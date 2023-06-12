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
import { computed, reactive, watch } from 'vue'
import { EventSourcePolyfill } from 'event-source-polyfill'
import { uniqueNamesGenerator, colors, adjectives, names } from 'unique-names-generator'

const config = {
  dictionaries: [adjectives, colors, names],
  separator: ' ',
  length: 3,
  style: 'capital'
}

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
  reason: uniqueNamesGenerator(config)
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
let initialEs = null

const handlers = {
  open: () => logEvent('Joined'),
  error: (event) => {
    logEvent('Error... ' + JSON.stringify(event))
    initialEs && initialEs.close()
  },
  message: (event) => logEvent('Message: ' + event.data),
  UPDATE: (event) => logEvent('Update: ' + event.data),
  COMPLETE: (event) => logEvent('Complete: ' + event.data),
  HEARTBEAT: (event) => logEvent('Heartbeat: ' + event.data),
  REGISTERED: (event) => logEvent('Registered: ' + event.data)
}

const register = () => {
  logEvent('Registering...')
  if (initialEs) {
    logEvent('Closing previous EventSource')
    initialEs.close()
  }
  initialEs = new EventSourcePolyfill(`${API_BASE}/register/?clientId=${state.clientId}`)
  Object.keys(handlers).forEach((key) => initialEs.addEventListener(key, handlers[key]))
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
  initialEs && initialEs.close()
  fetch(`${API_BASE}/unregister/?clientId=${state.clientId}`)
    .then(() => {
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
