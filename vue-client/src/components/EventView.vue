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
import { uniqueNamesGenerator, colors, adjectives, names } from 'unique-names-generator'
import axios from 'axios'
import { CanceledError } from 'axios'

const axe = axios.create({
  baseURL: 'http://localhost:4040/'
})
const config = {
  dictionaries: [adjectives, colors, names],
  separator: ' ',
  length: 3,
  style: 'capital'
}

const API_BASE = '/api/track'
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

let abortController = null
const register = () => {
  logEvent('Registering...')
  if (abortController) {
    logEvent('Closing previous registering')
    abortController.abort()
  }
  abortController = new AbortController()
  axe
    .get(`${API_BASE}/register/?clientId=${state.clientId}`, {
      signal: abortController.signal
    })
    .then((data) => data.data)
    .then((data) => {
      let reboot = true
      if (data.reason === 'TIMEOUT') {
        logEvent('Timeout; re-register')
      } else if (data.reason === 'UNREGISTER') {
        logEvent('Unregistered, not going to re-register')
        reboot = false
      } else {
        if (data.reason !== 'ERROR') {
          logEvent('Register request completed: ' + JSON.stringify(data.reason))
        } else {
          logEvent('Error occurred: ' + JSON.stringify(data))
        }
      }
      if (reboot) {
        setTimeout(register, 0)
      }
    })
    .catch((error) => {
      console.error(error)
      if (error instanceof CanceledError) {
        logEvent('Register request canceled')
      } else {
        logEvent('Register failed: ' + error)
      }
    })
}
const send = () => {
  const payload = `${state.count++} ${state.reason}`
  logEvent(`Sending '${payload}'...`)
  axe
    .get(`${API_BASE}/update/?reason=${payload}`)
    .then((data) => logEvent('... sent ' + JSON.stringify(data)))
    .catch((error) => {
      logEvent('Update failed: ' + JSON.stringify(error))
    })
}
const unregister = () => {
  logEvent('Unregistering...')
  abortController && abortController.abort()
  logEvent('... abort signalled...')
  axe
    .get(`${API_BASE}/unregister/?clientId=${state.clientId}`)
    .then(() => {
      logEvent('... unregister done')
    })
    .catch((error) => {
      logEvent('... unregister failed: ' + JSON.stringify(error))
    })
  logEvent('... unregistered')
  abortController = null
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
