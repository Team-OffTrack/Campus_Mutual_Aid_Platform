<template>
  <div class="page notif-page">
    <van-nav-bar title="消息通知" left-arrow fixed placeholder
      class="notif-nav" @click-left="router.push('/')">
      <template #right>
        <NavActions />
        <span v-if="notifications.length > 0" class="nav-mark-all" @click="handleMarkAll">
          全部已读
        </span>
      </template>
    </van-nav-bar>

    <div class="content-wrap">
      <div v-if="loading" class="loading-hint">加载中…</div>

      <div v-else-if="notifications.length === 0" class="empty-hint">
        <van-icon name="envelope-o" size="48" color="#94A3C8" />
        <p>暂无消息</p>
      </div>

      <template v-else>
        <div v-for="n in notifications" :key="n.notificationId"
          class="notif-card" :class="{ 'is-unread': !n.read }"
          @click="handleClick(n)">
          <div class="n-left">
            <div class="n-icon" :class="'icon-' + n.type.toLowerCase()">
              <van-icon v-if="n.type === 'ACCEPT'" name="user-o" />
              <van-icon v-else-if="n.type === 'COMPLETE'" name="success" />
              <van-icon v-else name="close" />
            </div>
          </div>
          <div class="n-body">
            <div class="n-top">
              <span class="n-title">{{ n.title }}</span>
              <span v-if="!n.read" class="n-dot"></span>
            </div>
            <p class="n-content">{{ n.content }}</p>
            <span class="n-time">{{ timeAgo(n.createTime) }}</span>
          </div>
          <van-icon name="arrow" class="n-arrow" />
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications, markRead, markAllRead } from '@/api/notification'
import NavActions from '@/components/NavActions.vue'

const router = useRouter()
const notifications = ref([])
const loading = ref(false)

function timeAgo(t) {
  if (!t) return ''
  const diff = Date.now() - new Date(t).getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return min + ' 分钟前'
  const h = Math.floor(min / 60)
  if (h < 24) return h + ' 小时前'
  return Math.floor(h / 24) + ' 天前'
}

async function handleClick(n) {
  if (!n.read) {
    try {
      await markRead(n.notificationId)
      n.read = true
    } catch (e) { /* skip */ }
  }
  // Navigate to related demand if available
  if (n.relatedDemandId) {
    router.push('/demands/' + n.relatedDemandId)
  }
}

async function handleMarkAll() {
  try {
    await markAllRead()
    notifications.value.forEach(n => n.read = true)
  } catch (e) { /* skip */ }
}

async function fetchNotifications() {
  loading.value = true
  try {
    notifications.value = await listNotifications()
    // Auto-mark all as read when viewing the list
    if (notifications.value.some(n => !n.read)) {
      await markAllRead()
      notifications.value.forEach(n => n.read = true)
    }
  } catch (e) { /* skip */ }
  finally { loading.value = false }
}

onMounted(fetchNotifications)
</script>

<style scoped>
.notif-page { background: var(--c-bg); min-height: 100dvh; }
.notif-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-mark-all { font-size: 13px; color: var(--c-primary); cursor: pointer; }

.content-wrap { padding: 8px 16px; }

.loading-hint, .empty-hint { text-align: center; padding: 80px 16px; color: var(--c-text-3); font-size: 14px; }
.empty-hint p { margin-top: 12px; }

.notif-card {
  display: flex; align-items: center; gap: 12px;
  background: var(--c-surface);
  border-radius: var(--r-md);
  padding: 14px;
  margin-bottom: 8px;
  box-shadow: var(--s-xs);
  cursor: pointer;
  transition: background var(--ease);
}
.notif-card.is-unread { background: #F8F9FF; border-left: 3px solid var(--c-primary); }
.notif-card:active { background: var(--c-bg); }

.n-left { flex-shrink: 0; }
.n-icon {
  width: 40px; height: 40px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff;
}
.icon-accept    { background: #3B82F6; }
.icon-complete  { background: #22C55E; }
.icon-cancel    { background: #EF4444; }

.n-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.n-top { display: flex; align-items: center; gap: 6px; }
.n-title { font-size: 15px; font-weight: 600; color: var(--c-text-1); }
.n-dot {
  width: 7px; height: 7px; border-radius: 50%;
  background: var(--c-primary); flex-shrink: 0;
}
.n-content { font-size: 13px; color: var(--c-text-3); line-height: 1.4; }
.n-time { font-size: 11px; color: var(--c-text-3); }
.n-arrow { flex-shrink: 0; color: var(--c-text-3); font-size: 14px; }

@media (min-width: 768px) {
  .notif-page { background: var(--c-surface); }
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 16px 32px; }
  .notif-card { max-width: 680px; }
  .notif-card:hover { background: var(--c-bg); }
}
</style>
