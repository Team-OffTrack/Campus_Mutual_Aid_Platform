<template>
  <div class="nav-actions" :class="{ 'nav-light': light }">
    <div class="nav-bell" @click="$router.push('/notifications')" role="button" aria-label="消息通知">
      <van-icon name="comment-o" size="22" />
      <span v-if="unreadNum > 0" class="bell-badge">{{ unreadNum > 99 ? '99+' : unreadNum }}</span>
    </div>
    <div class="nav-avatar" @click="$router.push('/profile')" role="button" aria-label="个人资料">
      <img v-if="authStore.avatar" :src="authStore.avatar" class="avatar-img" />
      <span v-else>{{ nameInitial }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { unreadCount } from '@/api/notification'
import { unreadChatCount } from '@/api/chat'

defineProps({ light: Boolean })

const authStore = useAuthStore()
const nameInitial = computed(() => (authStore.name || '?').charAt(0).toUpperCase())
const unreadNum = ref(0)

async function fetchUnread() {
  try {
    const [notif, chat] = await Promise.all([
      unreadCount().catch(() => ({ count: 0 })),
      unreadChatCount().catch(() => ({ count: 0 }))
    ])
    unreadNum.value = (notif.count || 0) + (chat.count || 0)
  } catch (e) { /* skip */ }
}

onMounted(fetchUnread)
</script>

<style scoped>
.nav-actions { display: flex; align-items: center; gap: 14px; }

.nav-bell {
  position: relative; color: var(--c-text-1);
  cursor: pointer; padding: 4px;
  display: flex; align-items: center; justify-content: center;
}
.nav-light .nav-bell { color: rgba(255,255,255,0.85); }

.bell-badge {
  position: absolute; top: -2px; right: -4px;
  min-width: 17px; height: 17px; border-radius: 9px;
  background: #EF4444; color: #fff;
  font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 4px; line-height: 1;
}

.nav-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--c-primary);
  color: #fff; font-size: 14px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; overflow: hidden;
}
.nav-avatar .avatar-img { width: 100%; height: 100%; object-fit: cover; }
.nav-light .nav-avatar {
  background: rgba(255,255,255,0.28);
  border: 2px solid rgba(255,255,255,0.55);
}
</style>
