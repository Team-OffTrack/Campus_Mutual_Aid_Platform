<template>
  <div class="nav-actions" :class="{ 'nav-light': light }">
    <div class="nav-bell" @click="$router.push('/notifications')" role="button" aria-label="消息通知">
      <van-icon name="comment-o" size="21" />
      <span v-if="unreadNum > 0" class="bell-badge">{{ unreadNum > 99 ? '99+' : unreadNum }}</span>
    </div>
    <div class="nav-avatar" @click="$router.push('/profile')" role="button" aria-label="个人资料">
      <img v-if="authStore.avatar" :src="authStore.avatar" class="avatar-img" />
      <span v-else class="avatar-letter">{{ nameInitial }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useWebSocketStore } from '@/stores/websocket'
import { unreadCount } from '@/api/notification'
import { unreadChatCount } from '@/api/chat'

defineProps({ light: Boolean })

const authStore = useAuthStore()
const wsStore = useWebSocketStore()
const nameInitial = computed(() => (authStore.name || '?').charAt(0).toUpperCase())

// Reactive sum of notification + chat unread counters pushed via WebSocket
const unreadNum = computed(() => wsStore.notifUnread + wsStore.chatUnread)

async function fetchUnread() {
  const [notif, chat] = await Promise.all([
    unreadCount().catch(() => ({ count: 0 })),
    unreadChatCount().catch(() => ({ count: 0 }))
  ])
  // Set baseline counts; WebSocket pushes will increment from here
  wsStore.notifUnread = notif.count || 0
  wsStore.chatUnread = chat.count || 0
}

onMounted(fetchUnread)
</script>

<style scoped>
.nav-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ── Bell ── */
.nav-bell {
  position: relative;
  color: var(--c-text-1);
  cursor: pointer;
  padding: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: background var(--spring-fast-spatial), transform var(--spring-fast-spatial);
}
.nav-bell:active { transform: scale(0.9); background: rgba(0,0,0,0.06); }

.nav-light .nav-bell { color: rgba(255,255,255,0.9); }
.nav-light .nav-bell:active { background: rgba(255,255,255,0.15); }

.bell-badge {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #EF4444;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  line-height: 1;
  box-shadow: var(--s-danger);
  animation: badge-in 0.3s var(--ease-emphasized-decelerate);
}

@keyframes badge-in {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

/* ── Avatar ── */
.nav-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--c-primary) 0%, #9A4DB5 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  transition: transform var(--spring-fast-spatial), box-shadow var(--spring-fast-spatial);
  box-shadow: var(--s-primary);
}
.nav-avatar:active { transform: scale(0.92); }
.nav-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.nav-light .nav-avatar {
  background: rgba(255,255,255,0.38);
  border: 2px solid rgba(255,255,255,0.55);
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
}
.nav-light .avatar-letter {
  color: var(--c-primary);
}
.avatar-letter { line-height: 1; }
</style>
