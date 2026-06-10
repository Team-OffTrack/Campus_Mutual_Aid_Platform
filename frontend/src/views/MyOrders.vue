<template>
  <div class="page orders-page">
    <van-nav-bar left-arrow fixed placeholder class="orders-nav" @click-left="router.push('/')">
      <template #title><span class="nav-title">我的订单</span></template>
      <template #right><NavActions /></template>
    </van-nav-bar>

    <!-- Tab bar -->
    <div class="tab-bar">
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'publisher' }"
        @click="switchTab('publisher')">我发布的</span>
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'acceptor' }"
        @click="switchTab('acceptor')">我接取的</span>
      <span class="tab-chip" :class="{ 'tab-on': activeTab === 'team' }"
        @click="switchTab('team')">我的队伍</span>
    </div>

    <div class="content-wrap">
      <div v-if="loading" class="loading-hint">加载中…</div>

      <div v-else-if="orders.length === 0" class="empty-hint">
        <template v-if="fetchError">
          <van-icon name="failure" size="48" color="#EF4444" />
          <p>加载失败</p>
          <van-button round size="small" type="primary" style="margin-top:12px" @click="fetchOrders">重试</van-button>
        </template>
        <template v-else>
          <van-icon name="notes-o" size="48" color="#C4C0CA" />
          <p>{{ activeTab === 'publisher' ? '还没有发布过需求' : activeTab === 'acceptor' ? '还没有接过单' : '还没有加入队伍' }}</p>
        </template>
      </div>

      <template v-else>
        <!-- Desktop table -->
        <div class="table-wrap">
          <table class="orders-table">
            <thead>
              <tr>
                <th class="col-type">类型</th>
                <th class="col-title">标题</th>
                <th class="col-other">{{ activeTab === 'publisher' ? '接单人' : activeTab === 'acceptor' ? '发布者' : '队长' }}</th>
                <th class="col-reward">报酬</th>
                <th class="col-status">状态</th>
                <th class="col-time">时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.demandId" class="table-row"
                @click="router.push('/demands/' + o.demandId)">
                <td><span class="type-badge" :style="typeStyle(o.type)">{{ typeLabel(o.type) }}</span></td>
                <td class="td-title">{{ o.title }}</td>
                <td class="td-other">{{ activeTab === 'publisher' ? (o.acceptorName || '—') : o.publisherName }}</td>
                <td class="td-reward">{{ rewardText(o) }}</td>
                <td><span class="status-tag" :class="'s-' + o.status.toLowerCase()">{{ statusLabel(o.status) }}</span></td>
                <td class="td-time">{{ timeAgo(o.createTime) }}</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Mobile cards -->
        <div class="mobile-list">
          <div v-for="o in orders" :key="o.demandId" class="order-card card"
            @click="router.push('/demands/' + o.demandId)">
            <div class="card-top">
              <span class="card-type" :style="typeStyle(o.type)">{{ typeLabel(o.type) }}</span>
              <span class="card-status" :class="'s-' + o.status.toLowerCase()">{{ statusLabel(o.status) }}</span>
            </div>
            <h3 class="card-title">{{ o.title }}</h3>
            <div class="card-meta">
              <span>{{ activeTab === 'publisher' ? '接单人' : activeTab === 'acceptor' ? '发布者' : '队长' }}：</span>
              <span class="meta-name">{{ activeTab === 'publisher' ? (o.acceptorName || '暂无') : o.publisherName }}</span>
              <span class="meta-divider">·</span>
              <span class="meta-reward">{{ rewardText(o) }}</span>
            </div>
            <div class="card-footer">
              <span class="footer-time">{{ timeAgo(o.createTime) }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { myOrders, myTeamOrders } from '@/api/demand'
import NavActions from '@/components/NavActions.vue'
import { TYPE_LABELS, TYPE_STYLES } from '@/constants/demandTypes'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const fetchError = ref(false)
const activeTab = ref('publisher')

const STATUS_LABELS = { OPEN: '待接单', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }

function typeLabel(v) { return TYPE_LABELS[v] || v }
function typeStyle(v) { return TYPE_STYLES[v] || TYPE_STYLES.other }
function statusLabel(v) { return STATUS_LABELS[v] || v }

function rewardText(d) {
  if (d.type === 'team') return '组队'
  if (!d.rewardAmount || d.rewardAmount === 0) return '免费'
  if (d.rewardType === 'cash') return '¥' + d.rewardAmount
  if (d.rewardType === 'point') return d.rewardAmount + ' 积分'
  return '公益'
}

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

async function switchTab(tab) { activeTab.value = tab; await fetchOrders() }

async function fetchOrders() {
  loading.value = true
  fetchError.value = false
  try {
    if (activeTab.value === 'team') {
      orders.value = await myTeamOrders()
    } else {
      orders.value = await myOrders(activeTab.value)
    }
  } catch (e) {
    orders.value = []
    fetchError.value = true
  }
  finally { loading.value = false }
}

onMounted(() => fetchOrders())
</script>

<style scoped>
.orders-page { background: var(--c-bg); padding-bottom: 32px; }
.orders-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
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

.content-wrap { padding: 16px; }
.loading-hint, .empty-hint { text-align: center; padding: 60px 16px; color: var(--c-text-3); font-size: 14px; }
.empty-hint p { margin-top: 12px; }

/* Cards */
.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.order-card { padding: 16px; cursor: pointer; display: flex; flex-direction: column; gap: 10px; border-radius: var(--r-large); }
.order-card:active { background: var(--c-bg); }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.card-type { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 6px; }
.card-status { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px; }
.s-open { background: #E3F2FD; color: #1565C0; }
.s-in_progress { background: #FFF3E0; color: #E65100; }
.s-completed { background: #E8F5E9; color: #2E7D32; }
.s-cancelled { background: #FFEBEE; color: #D32F2F; }
.card-title { font-size: 16px; font-weight: 700; color: var(--c-text-1); }
.card-meta { font-size: 13px; color: var(--c-text-3); display: flex; align-items: center; gap: 4px; }
.meta-name { font-weight: 600; color: var(--c-text-2); }
.meta-divider { color: var(--c-border); }
.meta-reward { font-weight: 600; color: var(--c-warning); }
.card-footer { display: flex; justify-content: flex-end; }
.footer-time { font-size: 11px; color: var(--c-text-4); }

/* Desktop table */
.table-wrap { display: none; }
@media (min-width: 768px) {
  .orders-page { background: var(--c-surface); }
  .tab-bar { max-width: var(--content-max); margin: 0 auto; padding: 14px 16px; background: transparent; position: static; backdrop-filter: none; }
  .tab-chip { flex: 0 0 auto; padding: 8px 24px; }
  .tab-chip:hover { opacity: 0.85; }
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 0 16px 32px; }
  .mobile-list { display: none; }
  .table-wrap { display: block; }
  .orders-table { width: 100%; border-collapse: collapse; font-size: 14px; }
  .orders-table thead { border-bottom: 2px solid var(--c-border); }
  .orders-table th {
    text-align: left; padding: 12px 10px;
    font-size: 12px; font-weight: 700; color: var(--c-text-3);
    text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap;
  }
  .table-row { border-bottom: 1px solid var(--c-border); transition: background var(--ease); cursor: pointer; }
  .table-row:hover { background: var(--c-bg); }
  .table-row td { padding: 14px 10px; vertical-align: middle; }
  .type-badge { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 6px; }
  .td-title { font-weight: 600; color: var(--c-text-1); max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-other { font-size: 13px; color: var(--c-text-2); }
  .td-reward { font-size: 13px; font-weight: 600; color: var(--c-warning); }
  .td-time { font-size: 12px; color: var(--c-text-3); white-space: nowrap; }
  .status-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px; }
}
@media (min-width: 1024px) {
  .content-wrap { padding: 0 48px 32px; }
  .tab-bar { padding: 14px 48px; }
}
</style>
