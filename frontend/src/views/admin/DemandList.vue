<template>
  <div class="page admin-page">
    <van-nav-bar left-arrow fixed placeholder class="admin-nav" @click-left="router.back()">
      <template #title><span class="nav-title">需求管理</span></template>
    </van-nav-bar>

    <!-- Search bar -->
    <div class="search-wrap">
      <van-search v-model="keyword" shape="round" placeholder="搜索需求标题"
        @search="onSearch" @clear="onSearch" />
    </div>

    <!-- Type filter tabs -->
    <div class="tabs-wrap">
      <span v-for="t in typeTabs" :key="t.value"
        class="tab-chip" :class="{ 'tab-on': activeType === t.value }"
        @click="switchType(t.value)">
        {{ t.label }}
      </span>
    </div>

    <!-- Status filter tabs -->
    <div class="tabs-wrap">
      <span v-for="s in statusTabs" :key="s.value"
        class="tab-chip" :class="{ 'tab-on': activeStatus === s.value }"
        @click="switchStatus(s.value)">
        {{ s.label }}
      </span>
    </div>

    <!-- Summary -->
    <div class="summary-row">
      <span class="summary-text">共 {{ totalCount }} 条需求</span>
    </div>

    <!-- Desktop table -->
    <div class="table-wrap">
      <table class="demand-table">
        <thead>
          <tr>
            <th class="col-title">标题</th>
            <th class="col-type">类型</th>
            <th class="col-status">状态</th>
            <th class="col-user">发布者</th>
            <th class="col-user">接单者</th>
            <th class="col-time">时间</th>
            <th class="col-act">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in demands" :key="d.demandId" class="table-row">
            <td class="td-title">{{ d.title }}</td>
            <td><span class="type-tag" :class="'ty-' + d.type">{{ typeLabel(d.type) }}</span></td>
            <td><span class="status-tag" :class="'st-' + d.status.toLowerCase()">{{ statusLabel(d.status) }}</span></td>
            <td class="td-user">{{ d.publisherName }}</td>
            <td class="td-user">{{ d.acceptorName || '—' }}</td>
            <td class="td-time">{{ timeAgo(d.createTime) }}</td>
            <td>
              <button class="delete-btn" @click="confirmDelete(d)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="table-loading">加载中…</div>
      <div v-if="finished && demands.length > 0" class="table-finished">已加载全部</div>
      <div v-if="!loading && demands.length === 0" class="table-empty">暂无需求</div>
    </div>

    <!-- Mobile card list -->
    <van-list v-model:loading="loading" :finished="finished"
      finished-text="已加载全部" class="mobile-list" @load="fetchDemands">
      <div v-for="d in demands" :key="d.demandId" class="demand-card card">
        <div class="card-top">
          <span class="card-title">{{ d.title }}</span>
          <span class="type-tag" :class="'ty-' + d.type">{{ typeLabel(d.type) }}</span>
        </div>
        <div class="card-mid">
          <span class="status-tag" :class="'st-' + d.status.toLowerCase()">{{ statusLabel(d.status) }}</span>
          <span class="card-meta">{{ timeAgo(d.createTime) }}</span>
        </div>
        <div class="card-bottom">
          <span class="card-user">{{ d.publisherName }}</span>
          <span v-if="d.acceptorName" class="card-acceptor">→ {{ d.acceptorName }}</span>
          <button class="delete-btn" @click.stop="confirmDelete(d)">删除</button>
        </div>
      </div>
    </van-list>

    <!-- Delete confirmation dialog -->
    <van-dialog v-model:show="dialogVisible" title="确认删除"
      :message="'确定要删除需求「' + (targetDemand?.title || '') + '」吗？该操作不可撤销。'"
      show-cancel-button confirm-button-text="删除" confirm-button-color="#ee0a24"
      @confirm="doDelete" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listDemands, deleteDemand } from '@/api/admin'

const router = useRouter()
const demands = ref([])
const totalCount = ref(0)
const loading = ref(false)
const finished = ref(false)
const keyword = ref('')
const activeType = ref('')
const activeStatus = ref('')
const dialogVisible = ref(false)
const targetDemand = ref(null)
let pageNum = 1

const typeTabs = [
  { label: '全部', value: '' },
  { label: '跑腿', value: 'errand' },
  { label: '交易', value: 'trade' },
  { label: '组队', value: 'team' },
  { label: '失物', value: 'lost_found' },
  { label: '学习', value: 'study' },
  { label: '其他', value: 'other' }
]

const statusTabs = [
  { label: '全部', value: '' },
  { label: '进行中', value: 'OPEN' },
  { label: '已接单', value: 'IN_PROGRESS' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' }
]

const TYPE_LABELS = {
  errand: '跑腿', trade: '交易', team: '组队',
  lost_found: '失物', study: '学习', other: '其他'
}
const STATUS_LABELS = {
  OPEN: '进行中', IN_PROGRESS: '已接单', COMPLETED: '已完成', CANCELLED: '已取消'
}

function typeLabel(v) { return TYPE_LABELS[v] || v }
function statusLabel(v) { return STATUS_LABELS[v] || v }

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

function switchType(type) {
  activeType.value = type
  pageNum = 1; demands.value = []; finished.value = false
  fetchDemands()
}

function switchStatus(status) {
  activeStatus.value = status
  pageNum = 1; demands.value = []; finished.value = false
  fetchDemands()
}

function onSearch() {
  pageNum = 1; demands.value = []; finished.value = false
  fetchDemands()
}

async function fetchDemands() {
  loading.value = true
  try {
    const params = { pageNum, pageSize: 20 }
    if (activeType.value) params.type = activeType.value
    if (activeStatus.value) params.status = activeStatus.value
    if (keyword.value) params.keyword = keyword.value
    const page = await listDemands(params)
    demands.value = [...demands.value, ...(page.records || [])]
    totalCount.value = page.total
    finished.value = page.current >= page.pages
    pageNum++
  } catch {
    if (demands.value.length === 0) showToast('加载失败')
  } finally { loading.value = false }
}

onMounted(() => fetchDemands())

function confirmDelete(demand) {
  targetDemand.value = demand
  dialogVisible.value = true
}

async function doDelete() {
  if (!targetDemand.value) return
  try {
    await deleteDemand(targetDemand.value.demandId)
    demands.value = demands.value.filter(d => d.demandId !== targetDemand.value.demandId)
    totalCount.value--
    showToast('已删除')
  } catch (e) {
    showToast(e.message || '删除失败')
  } finally {
    targetDemand.value = null
  }
}
</script>

<style scoped>
.admin-page { background: var(--c-bg); padding-bottom: 32px; }
.admin-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.search-wrap { background: #fff; padding: 0 8px; }
.search-wrap :deep(.van-search) { padding: 8px 0; }

.tabs-wrap { display: flex; gap: 8px; padding: 6px 16px; background: #fff; flex-wrap: wrap; }
.tab-chip {
  font-size: 12px; font-weight: 600; color: var(--c-text-3);
  padding: 4px 14px; border-radius: var(--r-full);
  cursor: pointer; transition: all var(--spring-fast-spatial);
  background: var(--c-surface-variant);
}
.tab-on { background: var(--c-primary-container); color: var(--c-primary); font-weight: 700; }

.summary-row { padding: 12px 16px 4px; }
.summary-text { font-size: 13px; color: var(--c-text-3); font-weight: 500; }

/* Tags */
.type-tag {
  font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 4px;
}
.ty-errand { background: #E3F2FD; color: #1565C0; }
.ty-trade { background: #FFF3E0; color: #E65100; }
.ty-team { background: #F3E5F5; color: #7B1FA2; }
.ty-lost_found { background: #E8F5E9; color: #2E7D32; }
.ty-study { background: #E8EAF6; color: #283593; }
.ty-other { background: #F5F5F5; color: #616161; }

.status-tag {
  font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px;
}
.st-open { background: #E3F2FD; color: #1565C0; }
.st-in_progress { background: #FFF3E0; color: #E65100; }
.st-completed { background: #E8F5E9; color: #2E7D32; }
.st-cancelled { background: #F5F5F5; color: #9E9E9E; }

/* Desktop table */
.table-wrap { display: none; }

@media (min-width: 768px) {
  .admin-page { background: var(--c-surface); }
  .search-wrap { max-width: var(--content-max); margin: 0 auto; padding: 0 16px; }
  .tabs-wrap { max-width: var(--content-max); margin: 0 auto; padding: 6px 16px; }
  .summary-row { max-width: var(--content-max); margin: 0 auto; }
  .mobile-list { display: none; }
  .table-wrap {
    display: block;
    max-width: var(--content-max);
    margin: 0 auto;
    padding: 0 16px 32px;
  }
  .demand-table { width: 100%; border-collapse: collapse; font-size: 14px; }
  .demand-table thead { border-bottom: 2px solid var(--c-border); }
  .demand-table th {
    text-align: left; padding: 12px 10px;
    font-size: 12px; font-weight: 700; color: var(--c-text-3);
    text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap;
  }
  .table-row { border-bottom: 1px solid var(--c-border); transition: background var(--ease); }
  .table-row:hover { background: var(--c-bg); }
  .table-row td { padding: 14px 10px; vertical-align: middle; }
  .td-title { font-weight: 600; color: var(--c-text-1); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-user { font-size: 13px; color: var(--c-text-2); }
  .td-time { font-size: 12px; color: var(--c-text-3); white-space: nowrap; }
  .delete-btn {
    padding: 4px 14px; border-radius: 6px; border: 1px solid #ee0a24;
    background: transparent; color: #ee0a24; font-size: 12px;
    font-weight: 600; cursor: pointer; transition: all var(--spring-fast-spatial);
  }
  .delete-btn:hover { background: #ee0a24; color: #fff; }
  .table-loading, .table-finished, .table-empty { text-align: center; padding: 24px; font-size: 13px; color: var(--c-text-3); }
}

@media (min-width: 1024px) {
  .table-wrap { padding: 0 48px 32px; }
  .tabs-wrap { padding: 6px 32px; }
  .search-wrap { padding: 0 32px; }
}

/* Mobile cards */
.mobile-list { padding: 0 16px; display: flex; flex-direction: column; gap: 10px; }
.demand-card { padding: 14px; display: flex; flex-direction: column; gap: 8px; border-radius: var(--r-large); }
.card-top { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
.card-title { font-size: 14px; font-weight: 600; color: var(--c-text-1); flex: 1; }
.card-mid { display: flex; justify-content: space-between; align-items: center; }
.card-meta { font-size: 12px; color: var(--c-text-3); }
.card-bottom { display: flex; align-items: center; gap: 6px; }
.card-user { font-size: 13px; color: var(--c-text-2); }
.card-acceptor { font-size: 13px; color: var(--c-primary); }
.card-bottom .delete-btn { margin-left: auto; }
</style>
