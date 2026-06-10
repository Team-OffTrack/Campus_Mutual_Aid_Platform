<template>
  <div class="page notif-page">
    <van-nav-bar left-arrow fixed placeholder class="notif-nav" @click-left="router.push('/')">
      <template #title><span class="nav-title">消息中心</span></template>
      <template #right><NavActions /></template>
    </van-nav-bar>

    <!-- Tab bar -->
    <div class="tab-bar">
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'system' }"
        @click="activeTab = 'system'">系统消息</span>
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'chat' }"
        @click="switchToChat">私信</span>
    </div>

    <div class="content-wrap">
      <!-- ═══ System notifications ═══ -->
      <template v-if="activeTab === 'system'">
        <div v-if="loadingNotif" class="loading-hint">加载中…</div>

        <div v-else-if="notifications.length === 0" class="empty-hint">
          <van-icon name="envelope-o" size="48" color="#C4C0CA" />
          <p>暂无系统消息</p>
        </div>

        <template v-else>
          <div v-for="n in notifications" :key="n.notificationId"
            class="notif-card card" :class="{ 'is-unread': !n.read }"
            @click="handleClick(n)">
            <div class="n-left">
              <div class="n-icon" :class="'icon-' + (n.type || '').toLowerCase()">
                <van-icon v-if="n.type === 'ACCEPT'" name="user-o" />
                <van-icon v-else-if="n.type === 'COMPLETE'" name="success" />
                <van-icon v-else-if="n.type === 'EVALUATION'" name="star-o" />
                <van-icon v-else name="info-o" />
              </div>
            </div>
            <div class="n-body">
              <div class="n-top">
                <span class="n-title">{{ n.title }}</span>
                <span v-if="!n.read" class="n-dot pulse-dot"></span>
              </div>
              <p class="n-content">{{ n.content }}</p>
              <span class="n-time">{{ timeAgo(n.createTime) }}</span>
            </div>
            <van-icon name="arrow" class="n-arrow" />
          </div>
        </template>
      </template>

      <!-- ═══ Private chat tab ═══ -->
      <template v-if="activeTab === 'chat'">
        <div v-if="loadingConvs" class="loading-hint">加载中…</div>

        <div v-else-if="conversations.length === 0" class="empty-hint">
          <van-icon name="chat-o" size="48" color="#C4C0CA" />
          <p>暂无私信</p>
        </div>

        <template v-else>
          <div v-for="c in conversations" :key="c.conversationId"
            class="conv-card card" :class="{ 'has-unread': c.unreadCount > 0 }"
            @click="router.push('/chat/' + c.conversationId + '?name=' + encodeURIComponent(c.otherUserName) + '&avatar=' + encodeURIComponent(c.otherUserAvatar || ''))">
            <div class="conv-avatar" :style="{ background: c.otherUserAvatar ? 'transparent' : avatarColor(c.otherUserName) }">
              <img v-if="c.otherUserAvatar" :src="c.otherUserAvatar" class="conv-avatar-img" />
              <span v-else>{{ c.otherUserName.charAt(0).toUpperCase() }}</span>
            </div>
            <div class="conv-body">
              <div class="conv-top">
                <span class="conv-name">{{ c.otherUserName }}</span>
                <span class="conv-time">{{ timeAgo(c.lastMessageAt) }}</span>
              </div>
              <div class="conv-bottom">
                <span class="conv-preview">{{ c.lastMessage || '暂无消息' }}</span>
                <span v-if="c.unreadCount > 0" class="conv-badge">{{ c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
              </div>
              <div class="conv-demand">
                <span class="conv-demand-tag">来自：{{ c.demandTitle }}</span>
              </div>
            </div>
          </div>
        </template>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listNotifications, markRead } from '@/api/notification'
import { getConversations } from '@/api/chat'
import NavActions from '@/components/NavActions.vue'

const router = useRouter()
const activeTab = ref('system')
const notifications = ref([])
const loadingNotif = ref(false)
const conversations = ref([])
const loadingConvs = ref(false)

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

const AVATAR_COLORS = ['#6750A4','#0097A7','#2E7D32','#D32F2F','#7B1FA2','#C62828','#ED6C02','#E65100']
function avatarColor(n) { return AVATAR_COLORS[(n || '?').charCodeAt(0) % AVATAR_COLORS.length] }

async function handleClick(n) {
  if (!n.read) {
    try { await markRead(n.notificationId); n.read = true }
    catch { /* ignore mark-read failure, still navigate */ }
  }
  if (n.relatedDemandId) { router.push('/demands/' + n.relatedDemandId) }
}

async function fetchNotifications() {
  loadingNotif.value = true
  try { notifications.value = await listNotifications() }
  catch (e) { showToast(e.message || '消息加载失败') }
  finally { loadingNotif.value = false }
}

async function fetchConversations() {
  loadingConvs.value = true
  try { conversations.value = await getConversations() }
  catch (e) { showToast(e.message || '私信列表加载失败') }
  finally { loadingConvs.value = false }
}

async function switchToChat() {
  activeTab.value = 'chat'
  if (conversations.value.length === 0) { await fetchConversations() }
}

onMounted(fetchNotifications)
</script>

<style scoped>
.notif-page { background: var(--c-bg); min-height: 100dvh; }
.notif-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.tab-bar {
  display: flex; gap: 8px; padding: 14px 16px;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  position: sticky; top: 52px; z-index: 10;
}
.tab-chip {
  flex: 1; text-align: center;
  padding: 11px 0; border-radius: var(--r-medium);
  font-size: 14px; font-weight: 600; color: var(--c-text-2);
  background: var(--c-surface-variant); cursor: pointer;
  transition: all var(--spring-fast-spatial);
}
.tab-on { background: var(--c-primary); color: #fff; box-shadow: 0 2px 8px rgba(103,80,164,0.3); }

.content-wrap { padding: 8px 16px; }
.loading-hint, .empty-hint { text-align: center; padding: 80px 16px; color: var(--c-text-3); font-size: 14px; }
.empty-hint p { margin-top: 12px; }

/* ═══════════════════════════════════════
   Notification cards
   ═══════════════════════════════════════ */
.notif-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px; margin-bottom: 8px;
  cursor: pointer;
  border-radius: var(--r-large);
}
.notif-card.is-unread {
  background: #F7F2FA;
  border-left: 3px solid var(--c-primary);
}
.notif-card:active { background: var(--c-bg); }

.n-left { flex-shrink: 0; }
.n-icon {
  width: 42px; height: 42px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; color: #fff;
}
.icon-accept     { background: #1565C0; }
.icon-complete   { background: #2E7D32; }
.icon-cancel     { background: #D32F2F; }
.icon-evaluation { background: #ED6C02; }

.n-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.n-top { display: flex; align-items: center; gap: 6px; }
.n-title { font-size: 15px; font-weight: 600; color: var(--c-text-1); }
.n-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: var(--c-primary); flex-shrink: 0;
}
.n-content { font-size: 13px; color: var(--c-text-3); line-height: 1.4; }
.n-time { font-size: 11px; color: var(--c-text-4); }
.n-arrow { flex-shrink: 0; color: var(--c-text-4); font-size: 14px; }

/* ═══════════════════════════════════════
   Conversation cards
   ═══════════════════════════════════════ */
.conv-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px; margin-bottom: 8px;
  cursor: pointer;
  border-radius: var(--r-large);
}
.conv-card.has-unread { background: #F7F2FA; }
.conv-card:active { background: var(--c-bg); }

.conv-avatar {
  width: 50px; height: 50px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 20px; font-weight: 700; color: #fff; overflow: hidden;
}
.conv-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.conv-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.conv-top { display: flex; justify-content: space-between; align-items: center; }
.conv-name { font-size: 15px; font-weight: 600; color: var(--c-text-1); }
.conv-time { font-size: 11px; color: var(--c-text-4); }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; }
.conv-preview {
  font-size: 13px; color: var(--c-text-3);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 220px;
}
.conv-badge {
  min-width: 20px; height: 20px; border-radius: 10px;
  background: var(--c-primary); color: #fff;
  font-size: 11px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 6px; flex-shrink: 0;
}
.conv-demand-tag { font-size: 11px; color: var(--c-text-4); }

@media (min-width: 768px) {
  .notif-page { background: var(--c-surface); }
  .tab-bar { max-width: var(--content-max); margin: 0 auto; padding: 14px 16px; background: transparent; position: static; backdrop-filter: none; }
  .tab-chip { flex: 0 0 auto; padding: 8px 24px; }
  .tab-chip:hover { opacity: 0.85; }
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 16px 32px; }
  .notif-card, .conv-card { max-width: 680px; }
  .notif-card:hover, .conv-card:hover { background: var(--c-bg); }
}
</style>
