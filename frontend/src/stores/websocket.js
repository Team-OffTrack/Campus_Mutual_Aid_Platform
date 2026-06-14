import { defineStore } from 'pinia'
import { ref, watch, toRef } from 'vue'
import { Client } from '@stomp/stompjs'
import { useAuthStore } from './auth'

/**
 * WebSocket store — manages a single STOMP-over-WebSocket connection
 * and exposes reactive unread counters for the notification badge.
 * <p>
 * Uses native WebSocket (no SockJS) connected to {@code /ws-raw}.
 * Backend pushes:
 * <ul>
 *   <li>Chat messages → {@code /user/{id}/queue/chat}</li>
 *   <li>Notifications  → {@code /user/{id}/queue/notifications}</li>
 * </ul>
 * Connection is automatically established when the user logs in
 * and torn down on logout.
 */
export const useWebSocketStore = defineStore('websocket', () => {
  const authStore = useAuthStore()

  // ── reactive state ──
  const isConnected = ref(false)
  const notifUnread = ref(0)
  const chatUnread = ref(0)

  let stompClient = null
  let chatSub = null
  let notifSub = null

  // ── callback registries ──
  const chatCallbacks = []
  const notifCallbacks = []

  /** Register a callback invoked for every incoming chat message. */
  function onChatMessage(fn) {
    chatCallbacks.push(fn)
    return () => { const i = chatCallbacks.indexOf(fn); if (i >= 0) chatCallbacks.splice(i, 1) }
  }

  /** Register a callback invoked for every incoming notification. */
  function onNotification(fn) {
    notifCallbacks.push(fn)
    return () => { const i = notifCallbacks.indexOf(fn); if (i >= 0) notifCallbacks.splice(i, 1) }
  }

  // ── connection ──

  function brokerURL() {
    // Use same host as the page (works through Vite proxy in dev, same-origin in prod)
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    return `${protocol}//${window.location.host}/ws-raw`
  }

  function connect() {
    if (stompClient) return

    const token = authStore.token
    if (!token) return

    stompClient = new Client({
      brokerURL: brokerURL(),
      connectHeaders: { Authorization: 'Bearer ' + token },
      // Disable verbose console logging
      debug: () => {},
      reconnectDelay: 5000,
      onConnect: () => {
        isConnected.value = true

        // Subscribe to private chat push
        chatSub = stompClient.subscribe('/user/queue/chat', frame => {
          try {
            const msg = JSON.parse(frame.body)
            chatUnread.value++
            chatCallbacks.forEach(fn => { try { fn(msg) } catch { /* best-effort */ } })
          } catch { /* ignore malformed frames */ }
        })

        // Subscribe to private notification push
        notifSub = stompClient.subscribe('/user/queue/notifications', frame => {
          try {
            const notif = JSON.parse(frame.body)
            notifUnread.value++
            notifCallbacks.forEach(fn => { try { fn(notif) } catch { /* best-effort */ } })
          } catch { /* ignore malformed frames */ }
        })
      },
      onDisconnect: () => {
        isConnected.value = false
      },
      onStompError: () => {
        isConnected.value = false
      }
    })

    stompClient.activate()
  }

  function disconnect() {
    if (chatSub) { chatSub.unsubscribe(); chatSub = null }
    if (notifSub) { notifSub.unsubscribe(); notifSub = null }
    if (stompClient) {
      try { stompClient.deactivate() } catch { /* ignore */ }
      stompClient = null
    }
    isConnected.value = false
    notifUnread.value = 0
    chatUnread.value = 0
  }

  /** Reset unread counters (call after user reads them via REST). */
  function resetNotifUnread() { notifUnread.value = 0 }
  function resetChatUnread() { chatUnread.value = 0 }

  // ── watch auth token ──
  watch(toRef(authStore, 'token'), (newToken, oldToken) => {
    if (newToken && newToken !== oldToken) {
      disconnect()
      connect()
    } else if (!newToken) {
      disconnect()
    }
  }, { immediate: true })

  return {
    isConnected,
    notifUnread,
    chatUnread,
    connect,
    disconnect,
    resetNotifUnread,
    resetChatUnread,
    onChatMessage,
    onNotification
  }
})
