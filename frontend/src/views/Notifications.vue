<template>
  <div class="page notif-page">
    <van-nav-bar title="消息中心" left-arrow fixed placeholder
      class="notif-nav" @click-left="router.push('/')">
      <template #right>
        <NavActions />
      </template>
    </van-nav-bar>

    <!-- Tab bar: system notifications / private messages -->
    <div class="tab-bar">
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'system' }"
        @click="activeTab = 'system'">系统消息</span>
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'chat' }"
        @click="switchToChat">私信</span>
    </div>

    <div class="content-wrap">
      <!-- ═══ System notifications tab ═══ -->
      <template v-if="activeTab === 'system'">
        <div v-if="loadingNotif" class="loading-hint">加载中…</div>

        <div v-else-if="notifications.length === 0" class="empty-hint">
          <van-icon name="envelope-o" size="48" color="#94A3C8" />
          <p>暂无系统消息</p>
        </div>

        <template v-else>
          <div v-for="n in notifications" :key="n.notificationId"
            class="notif-card" :class="{ 'is-unread': !n.read }"
            @click="handleClick(n)">
            <div class="n-left">
              <div class="n-icon" :class="'icon-' + n.type.toLowerCase()">
                <van-icon v-if="n.type === 'ACCEPT'" name="user-o" />
                <van-icon v-else-if="n.type === 'COMPLETE'" name="success" />
                <van-icon v-else-if="n.type === 'EVALUATION'" name="star-o" />
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
      </template>

      <!-- ═══ Private chat tab ═══ -->
      <template v-if="activeTab === 'chat'">
        <div v-if="loadingConvs" class="loading-hint">加载中…</div>

        <div v-else-if="conversations.length === 0" class="empty-hint">
          <van-icon name="chat-o" size="48" color="#94A3C8" />
          <p>暂无私信</p>
        </div>

        <template v-else>
          <div v-for="c in conversations" :key="c.conversationId"
            class="conv-card" :class="{ 'has-unread': c.unreadCount > 0 }"
            @click="router.push('/chat/' + c.conversationId)">
            <div class="conv-avatar" :style="{ background: avatarColor(c.otherUserName) }">
              {{ c.otherUserName.charAt(0).toUpperCase() }}
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
import { listNotifications, markRead } from '@/api/notification'
import { getConversations } from '@/api/chat'
import NavActions from '@/components/NavActions.vue'

const router = useRouter()
const activeTab = ref('system')

// System notifications
const notifications = ref([])
const loadingNotif = ref(false)

// Private chat conversations
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

const AVATAR_COLORS = ['#5C6BF8', '#06B6D4', '#22C55E', '#EF4444', '#A855F7', '#EC4899', '#EAB308', '#F97316']
function avatarColor(n) { return AVATAR_COLORS[(n || '?').charCodeAt(0) % AVATAR_COLORS.length] }

async function handleClick(n) {
  if (!n.read) {
    try {
      await markRead(n.notificationId)
      n.read = true
    } catch { /* skip */ }
  }
  if (n.relatedDemandId) {
    router.push('/demands/' + n.relatedDemandId)
  }
}

async function fetchNotifications() {
  loadingNotif.value = true
  try {
    notifications.value = await listNotifications()
  } catch { /* skip */ }
  finally { loadingNotif.value = false }
}

async function fetchConversations() {
  loadingConvs.value = true
  try {
    conversations.value = await getConversations()
  } catch { /* skip */ }
  finally { loadingConvs.value = false }
}

async function switchToChat() {
  activeTab.value = 'chat'
  if (conversations.value.length === 0) {
    await fetchConversations()
  }
}

onMounted(fetchNotifications)
</script>

<style scoped>
.notif-page { background: var(--c-bg); min-height: 100dvh; }
.notif-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }

/* Tab bar */
.tab-bar {
  display: flex; gap: 8px; padding: 14px 16px;
  background: #fff; position: sticky; top: 52px; z-index: 10;
}
.tab-chip {
  flex: 1; text-align: center;
  padding: 10px 0; border-radius: var(--r-md);
  font-size: 14px; font-weight: 600; color: var(--c-text-2);
  background: var(--c-bg); cursor: pointer;
  transition: all var(--ease);
}
.tab-on { background: var(--c-primary); color: #fff; }

.content-wrap { padding: 8px 16px; }

.loading-hint, .empty-hint { text-align: center; padding: 80px 16px; color: var(--c-text-3); font-size: 14px; }
.empty-hint p { margin-top: 12px; }

/* System notification cards */
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
.icon-accept     { background: #3B82F6; }
.icon-complete   { background: #22C55E; }
.icon-cancel     { background: #EF4444; }
.icon-evaluation { background: #EAB308; }

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

/* Conversation cards */
.conv-card {
  display: flex; align-items: center; gap: 12px;
  background: var(--c-surface);
  border-radius: var(--r-md);
  padding: 14px;
  margin-bottom: 8px;
  box-shadow: var(--s-xs);
  cursor: pointer;
  transition: background var(--ease);
}
.conv-card.has-unread { background: #F8F9FF; }
.conv-card:active { background: var(--c-bg); }

.conv-avatar {
  width: 48px; height: 48px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff;
}
.conv-body { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.conv-top { display: flex; justify-content: space-between; align-items: center; }
.conv-name { font-size: 15px; font-weight: 600; color: var(--c-text-1); }
.conv-time { font-size: 11px; color: var(--c-text-3); }
.conv-bottom { display: flex; justify-content: space-between; align-items: center; }
.conv-preview {
  font-size: 13px; color: var(--c-text-3);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 220px;
}
.conv-badge {
  min-width: 18px; height: 18px; border-radius: 9px;
  background: var(--c-primary); color: #fff;
  font-size: 10px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 5px; flex-shrink: 0;
}
.conv-demand-tag { font-size: 11px; color: var(--c-text-3); }

@media (min-width: 768px) {
  .notif-page { background: var(--c-surface); }
  .tab-bar { max-width: var(--content-max); margin: 0 auto; padding: 14px 16px; background: transparent; position: static; }
  .tab-chip { flex: 0 0 auto; padding: 8px 24px; }
  .tab-chip:hover { opacity: 0.85; }
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 16px 32px; }
  .notif-card, .conv-card { max-width: 680px; }
  .notif-card:hover, .conv-card:hover { background: var(--c-bg); }
}
</style>
