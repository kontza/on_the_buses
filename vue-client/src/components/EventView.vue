<template>
  <div class="grid">
    <div class="divider">Event Log</div>
    <textarea class="event-log" rows="25" cols="80" readonly v-model="events"></textarea>
    <button class="btn-primary" @click="register">Register to server</button>
    <div class="divider">Event to send</div>
    <input v-model="state.reason" @keyup.enter="send" />
    <button class="btn-primary" @click="send">Send update to server</button>
  </div>
</template>
<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { EventSourcePolyfill } from 'event-source-polyfill'

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
  es: null
})
logEvent('Welcome to the machine!')
const events = computed(() => {
  return state.eventArray.join('\n')
})
let es = ref(null)

watch(
  state.eventArray,
  (newArray) => {
    while (newArray.length > 24) {
      newArray.shift()
    }
  },
  { deep: true }
)

const register = () => {
  if (state.es) {
    logEvent('Already registered')
    return
  }
  logEvent('Registering...')
  let initialEs = new EventSourcePolyfill('/api/register/?clientId=Johnny')
  let listener = (event) => {
    switch (event.type) {
      case 'open':
        es.value = initialEs
        logEvent('Registered...')
        break
      case 'error':
        logEvent('Error...')
        state.es = null
        break
      case 'message':
        logEvent(`Message: ${event.data}`)
        break
    }
  }
  initialEs.addEventListener('open', listener)
  initialEs.addEventListener('message', listener)
  initialEs.addEventListener('error', listener)
}
const send = () => {
  logEvent(`Sending '${state.reason}'...`)
  fetch(`/api/update/?reason=${state.reason}`).then(() => {
    logEvent('... sent')
  })
}
</script>
<style scoped>
.event-log {
  font-family: monospace;
}
</style>
