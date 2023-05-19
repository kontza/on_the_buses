<template>
  <div class="grid grid-cols-3">
    <div class="divider col-span-3">Event Log</div>
    <textarea class="event-log col-span-3" rows="25" cols="80" readonly v-model="events"></textarea>
    <div class="empty-row col-span-3"></div>
    <input v-model="state.clientId" />
    <button class="btn-primary" @click="register">Register to server</button>
    <button class="btn-secondary" @click="unregister">Unregister from server</button>
    <div class="divider col-span-3">Event to send</div>
    <input v-model="state.reason" @keyup.enter="send" />
    <button class="btn-primary" @click="send">Send update to server</button>
  </div>
</template>
<script setup>
import { computed, ref, reactive, watch } from 'vue'
import { EventSourcePolyfill } from 'event-source-polyfill'
import { fetchEventSource, EventStreamContentType } from '@microsoft/fetch-event-source'

class RetriableError extends Error {}
class FatalError extends Error {}

const logEvent = (message) => {
  const timestamp = new Date().toLocaleTimeString('fi', {
    hour: 'numeric',
    minute: 'numeric',
    second: 'numeric',
    fractionalSecondDigits: 3
  })
  state.eventArray.push(timestamp + ' ' + message)
}
const state = reactive({
  reason: 'Senkin huithapeli!',
  eventArray: [],
  clientId: import.meta.env.VITE_CLIENT_ID,
  retry: true
})
logEvent('Welcome to the machine!')
const events = computed(() => {
  return state.eventArray.join('\n')
})

watch(
  state.eventArray,
  (newArray) => {
    while (newArray.length > 24) {
      newArray.shift()
    }
  },
  { deep: true }
)
const MS = 'MS'
const mode = ref(null)

const register = () => {
  logEvent('Registering...')
  state.retry = true
  if (mode.value === MS) {
    fetchEventSource(`/api/register/?clientId=${state.clientId}`, {
      openWhenHidden: true,
      async onopen(response) {
        if (response.ok && response.headers.get('content-type') === EventStreamContentType) {
          logEvent('Registered... ' + JSON.stringify(response))
          return // everything's good
        } else if (response.status >= 400 && response.status < 500 && response.status !== 429) {
          // client-side errors are usually non-retriable:
          logEvent('Registration failed... ' + JSON.stringify(response))
          throw new FatalError()
        } else {
          logEvent('Registration failed, can be retried... ' + JSON.stringify(response))
          throw new RetriableError()
        }
      },
      onmessage(msg) {
        // if the server emits an error message, throw an exception
        // so it gets handled by the onerror callback below:
        if (msg.event === 'FatalError') {
          logEvent('Message error: ' + JSON.stringify(msg))
          throw new FatalError(msg.data)
        } else {
          switch (msg.event) {
            case 'REGISTERED':
              logEvent('Client registered')
              break
            case 'UPDATE':
              logEvent('Update: ' + JSON.stringify(msg.data))
              break
            case 'COMPLETE':
              logEvent('Completed')
              state.retry = false
              break
            default:
              logEvent('Message: ' + JSON.stringify(msg))
              break
          }
        }
      },
      onclose() {
        // if the server closes the connection unexpectedly, retry:
        if (state.retry) {
          logEvent('Server closed connection, retry')
          throw new RetriableError()
        } else {
          logEvent('Close on completed')
        }
      },
      onerror(err) {
        if (err instanceof FatalError) {
          logEvent('Non-retryable error: ' + JSON.stringify(err))
          throw err // rethrow to stop the operation
        } else {
          // do nothing to automatically retry. You can also
          // return a specific retry interval here.
          logEvent('Retryable error: ' + JSON.stringify(err))
        }
      }
    })
  } else {
    let initialEs = new EventSourcePolyfill(`/api/register/?clientId=${state.clientId}`, {
      heartbeatTimeout: 250000
    })
    let listener = (event) => {
      switch (event.type) {
        case 'open':
          if (state.retry) {
            logEvent('Opened')
          } else {
            logEvent('Closing the event source')
            initialEs.close()
          }
          break
        case 'error':
          if (event.status >= 500) {
            logEvent('Error... ' + JSON.stringify(event))
            initialEs.close()
          }
          break
        case 'message':
          logEvent(`Message: ${event.data}`)
          break
        default:
          logEvent(`Unknown: ${JSON.stringify(event)}`)
          break
      }
    }
    initialEs.addEventListener('open', listener)
    initialEs.addEventListener('message', listener)
    initialEs.addEventListener('error', listener)
    initialEs.addEventListener('REGISTERED', () => {
      logEvent('Client registered')
    })
    initialEs.addEventListener('UPDATE', (payload) => {
      logEvent('Update: ' + JSON.stringify(payload.data))
    })
    initialEs.addEventListener('COMPLETE', () => {
      logEvent('Client completed')
      initialEs.close()
    })
  }
}
const send = () => {
  logEvent(`Sending '${state.reason}'...`)
  fetch(`/api/update/?reason=${state.reason}`, { method: 'GET' })
    .then(() => logEvent('... sent'))
    .catch((error) => logEvent('Update failed: ' + JSON.stringify(error)))
}
const unregister = () => {
  logEvent('Unregistering...')
  fetch(`/api/unregister/?clientId=${state.clientId}`)
    .then(() => {
      state.retry = false
      logEvent('... done')
    })
    .catch((error) => logEvent('Unregister failed: ' + JSON.stringify(error)))
}
</script>
<style scoped>
.event-log {
  font-family: monospace;
}
.empty-row {
  height: 1rem;
}
</style>
