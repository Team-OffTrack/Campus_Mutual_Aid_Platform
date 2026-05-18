<template>
  <div class="page chat-page">
    <!-- Header -->
    <van-nav-bar :title="otherUserName || '聊天'" left-arrow fixed placeholder
      class="chat-nav" @click-left="router.back()" />

    <!-- Message list -->
    <div ref="msgListRef" class="msg-list">
      <div v-if="loading" class="loading-hint">加载中…</div>

      <template v-else>
        <div v-for="m in messages" :key="m.messageId"
          class="msg-row" :class="{ 'msg-mine': m.senderId === userId }">
          <!-- Other user's avatar -->
          <div v-if="m.senderId !== userId" class="msg-avatar"
            :style="{ background: otherUserAvatar ? 'transparent' : avatarColor(otherUserName) }">
            <img v-if="otherUserAvatar" :src="otherUserAvatar" class="avatar-img" />
            <span v-else>{{ (otherUserName || '?').charAt(0).toUpperCase() }}</span>
          </div>

          <div class="msg-bubble" :class="{ 'bubble-mine': m.senderId === userId }">
            <p class="msg-text">{{ m.content }}</p>
            <span class="msg-time">{{ timeAgo(m.createTime) }}</span>
          </div>

          <!-- My avatar -->
          <div v-if="m.senderId === userId" class="msg-avatar my-avatar"
            :style="{ background: myAvatar ? 'transparent' : avatarColor(myName) }">
            <img v-if="myAvatar" :src="myAvatar" class="avatar-img" />
            <span v-else>{{ (myName || '?').charAt(0).toUpperCase() }}</span>
          </div>
        </div>

        <div v-if="messages.length === 0 && !loading" class="empty-hint">
          暂无消息，发送第一条消息吧
        </div>
      </template>
    </div>

    <!-- Input bar -->
    <div class="input-bar">
      <van-field v-model="inputText" type="textarea" rows="1" autosize
        placeholder="输入消息…" class="chat-input"
        @keydown.enter.exact.prevent="handleSend" />
      <van-button round type="primary" size="small"
        :loading="sending" :disabled="!inputText.trim()" @click="handleSend">
        发送
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getMessages, sendMessage } from '@/api/chat'
import { getProfile } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const conversationId = computed(() => Number(route.params.conversationId))
const userId = computed(() => Number(authStore.userId))
const myName = ref('')
const myAvatar = computed(() => authStore.avatar)
const otherUserName = ref(route.query.name || '')
const otherUserAvatar = ref(route.query.avatar || '')
const messages = ref([])
const inputText = ref('')
const loading = ref(false)
const sending = ref(false)
const msgListRef = ref(null)

let pollTimer = null

const AVATAR_COLORS = ['#5C6BF8', '#06B6D4', '#22C55E', '#EF4444', '#A855F7', '#EC4899', '#EAB308', '#F97316']
function avatarColor(n) { return AVATAR_COLORS[(n || '?').charCodeAt(0) % AVATAR_COLORS.length] }

function timeAgo(t) {
  if (!t) return ''
  const diff = Date.now() - new Date(t).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return mins + ' 分钟前'
  const hours = Math.floor(mins / 60)
  if (hours < 24) return hours + ' 小时前'
  const days = Math.floor(hours / 24)
  if (days < 30) return days + ' 天前'
  return formatTime(t)
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}

function scrollToBottom() {
  nextTick(() => {
    const el = msgListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function fetchMessages() {
  try {
    const msgs = await getMessages(conversationId.value)
    messages.value = msgs
    scrollToBottom()
  } catch { /* client.js handles errors */ }
}

async function loadProfileInfo() {
  try {
    const profile = await getProfile()
    myName.value = profile.name || ''
  } catch { /* skip */ }
}

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  sending.value = true
  try {
    const msg = await sendMessage(conversationId.value, text)
    messages.value = [...messages.value, msg]
    inputText.value = ''
    scrollToBottom()
  } catch { /* client.js handles errors */ }
  finally { sending.value = false }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    try {
      const msgs = await getMessages(conversationId.value)
      if (msgs.length !== messages.value.length) {
        messages.value = msgs
        scrollToBottom()
      }
    } catch { /* skip */ }
  }, 10000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// Restart polling if conversationId changes (e.g. switching chats without remount)
watch(conversationId, () => {
  loading.value = true
  fetchMessages().finally(() => { loading.value = false })
  startPolling()
})

onMounted(async () => {
  loading.value = true
  await loadProfileInfo()
  await fetchMessages()
  loading.value = false
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.chat-page {
  display: flex; flex-direction: column;
  height: 100dvh; background: var(--c-bg);
}
.chat-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }

/* Message list */
.msg-list {
  flex: 1; overflow-y: auto; padding: 12px 16px;
  display: flex; flex-direction: column; gap: 14px;
}

.loading-hint, .empty-hint {
  text-align: center; padding: 80px 16px; color: var(--c-text-3); font-size: 14px;
}

/* Message row */
.msg-row { display: flex; align-items: flex-end; gap: 8px; }
.msg-row.msg-mine { justify-content: flex-end; }

.msg-avatar {
  width: 34px; height: 34px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff; overflow: hidden;
}
.msg-avatar .avatar-img { width: 100%; height: 100%; object-fit: cover; }
.my-avatar { align-self: flex-end; }

/* Bubbles */
.msg-bubble {
  max-width: 72%; padding: 10px 14px; border-radius: 16px;
  background: var(--c-surface); box-shadow: var(--s-xs);
  display: flex; flex-direction: column; gap: 3px;
}
.bubble-mine {
  background: var(--c-primary); color: #fff;
  border-bottom-right-radius: 6px;
}
.msg-bubble:not(.bubble-mine) {
  border-bottom-left-radius: 6px;
}

.msg-text { font-size: 15px; line-height: 1.5; word-break: break-word; margin: 0; }
.bubble-mine .msg-text { color: #fff; }

.msg-time { font-size: 10px; color: var(--c-text-3); align-self: flex-end; }
.bubble-mine .msg-time { color: rgba(255,255,255,0.65); }

/* Input bar */
.input-bar {
  display: flex; align-items: flex-end; gap: 10px;
  padding: 10px 14px; padding-bottom: max(10px, env(safe-area-inset-bottom));
  background: #fff; border-top: 1px solid var(--c-border);
  position: sticky; bottom: 0;
}
.chat-input {
  flex: 1; background: var(--c-bg); border-radius: 22px;
  padding: 6px 14px !important; min-height: 40px;
}
.chat-input :deep(.van-field__control) { font-size: 15px; }
.input-bar .van-button { flex-shrink: 0; height: 40px !important; padding: 0 20px !important; }

/* Desktop */
@media (min-width: 768px) {
  .chat-page { background: var(--c-surface); }
  .msg-list { max-width: 720px; margin: 0 auto; width: 100%; padding: 20px 32px; }
  .input-bar {
    max-width: 720px; margin: 0 auto; width: 100%;
    background: transparent; border-top: none; padding: 12px 32px;
  }
  .chat-input { background: var(--c-bg); }
  .msg-bubble { max-width: 60%; }
}
</style>
