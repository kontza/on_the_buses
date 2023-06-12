<template>
  <div class="grid grid-cols-3">
    <div class="divider col-span-3">Event Log</div>
    <textarea class="event-log col-span-3" rows="25" cols="80" readonly v-model="events"></textarea>
    <div class="empty-row col-span-3"></div>
    <input v-model="state.clientId" />
    <button class="btn-primary" @click="register">Join chat</button>
    <button class="btn-secondary" @click="unregister">{{ state.leaveTitle }}</button>
    <div class="divider col-span-3">Event to send</div>
    <input v-model="state.reason" @keyup.enter="send" />
    <button class="btn-primary" @click="send">Send update to server</button>
  </div>
</template>
<script setup>
import { computed, ref, reactive, watch } from 'vue'
import { EventSourcePolyfill } from 'event-source-polyfill'
import { uniqueNamesGenerator, colors, adjectives, names } from 'unique-names-generator'

const config = {
  dictionaries: [adjectives, colors, names],
  separator: ' ',
  length: 3,
  style: 'capital'
}

class RetriableError extends Error {}
class FatalError extends Error {}

const API_BASE = '/api/sse'
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
  clientId: 'Chat member #',
  count: 1,
  eventArray: [],
  leaveTitle: 'Leave chat',
  reason: uniqueNamesGenerator(config),
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
let controller = null
let initialEs = null

const register = () => {
  logEvent('Registering...')
  state.retry = true
  if (controller !== null) {
    controller.abort()
  }
  controller = new AbortController()
  if (initialEs) {
    logEvent('Closing previous EventSource')
    initialEs.close()
  }
  initialEs = new EventSourcePolyfill(`${API_BASE}/register/?clientId=${state.clientId}`)
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
  initialEs.addEventListener('REGISTERED', (payload) => {
    let parsed = JSON.parse(payload.data)
    logEvent('Client registered ' + JSON.stringify(parsed))
  })
  initialEs.addEventListener('UPDATE', (payload) => {
    logEvent('Update: ' + JSON.stringify(payload.data))
  })
  initialEs.addEventListener('COMPLETE', () => {
    logEvent('Client completed')
    initialEs.close()
  })
}
const send = () => {
  const payload = `${state.count++} ${state.reason}`
  logEvent(`Sending '${payload}'...`)
  fetch(`${API_BASE}/update/?reason=${payload}`, { method: 'GET' })
    .then(() => logEvent('... sent'))
    .catch((error) => {
      logEvent('Update failed: ' + JSON.stringify(error))
    })
}
const unregister = () => {
  logEvent('Unregistering...')
  fetch(`${API_BASE}/unregister/?clientId=${state.clientId}`)
    .then(() => {
      controller && controller.abort()
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
