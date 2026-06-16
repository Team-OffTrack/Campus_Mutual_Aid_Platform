<template>
  <div class="page admin-page">
    <van-nav-bar left-arrow fixed placeholder class="admin-nav" @click-left="router.back()">
      <template #title><span class="nav-title">举报管理</span></template>
    </van-nav-bar>

    <!-- Status tabs -->
    <div class="tabs-wrap">
      <span v-for="t in statusTabs" :key="t.value"
        class="tab-chip" :class="{ 'tab-on': activeStatus === t.value }"
        @click="switchStatus(t.value)">
        {{ t.label }}
      </span>
    </div>

    <!-- Summary -->
    <div class="summary-row">
      <span class="summary-text">共 {{ totalCount }} 条举报</span>
    </div>

    <!-- Desktop table -->
    <div class="table-wrap">
      <table class="report-table">
        <thead>
          <tr>
            <th class="col-reporter">举报人</th>
            <th class="col-target">目标</th>
            <th class="col-reason">原因</th>
            <th class="col-status">状态</th>
            <th class="col-time">时间</th>
            <th class="col-act">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reports" :key="r.id" class="table-row">
            <td class="td-reporter">{{ r.reporterName }}</td>
            <td>
              <span class="target-tag" :class="'t-' + r.targetType.toLowerCase()">
                {{ targetLabel(r.targetType) }}
              </span>
              <span class="target-id">#{{ r.targetId }}</span>
            </td>
            <td>{{ reasonLabel(r.reason) }}</td>
            <td>
              <span class="status-tag" :class="'st-' + r.status.toLowerCase()">
                {{ statusLabel(r.status) }}
              </span>
            </td>
            <td class="td-time">{{ timeAgo(r.createTime) }}</td>
            <td>
              <button v-if="r.status === 'PENDING'"
                class="resolve-btn" @click="showActions(r)">
                处理
              </button>
              <span v-else class="resolved-text">{{ r.adminNote || '—' }}</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="table-loading">加载中…</div>
      <div v-if="finished && reports.length > 0" class="table-finished">已加载全部</div>
      <div v-if="!loading && reports.length === 0" class="table-empty">暂无举报</div>
    </div>

    <!-- Mobile card list -->
    <van-list v-model:loading="loading" :finished="finished"
      finished-text="已加载全部" class="mobile-list" @load="fetchReports">
      <div v-for="r in reports" :key="r.id" class="report-card card" @click="showActions(r)">
        <div class="card-top">
          <span class="target-tag" :class="'t-' + r.targetType.toLowerCase()">
            {{ targetLabel(r.targetType) }}
          </span>
          <span class="status-tag" :class="'st-' + r.status.toLowerCase()">
            {{ statusLabel(r.status) }}
          </span>
        </div>
        <div class="card-body">
          <span class="card-reason">{{ reasonLabel(r.reason) }}</span>
          <span class="card-meta">{{ r.reporterName }} · {{ timeAgo(r.createTime) }}</span>
        </div>
        <div v-if="r.adminNote" class="card-note">{{ r.adminNote }}</div>
      </div>
    </van-list>

    <van-action-sheet v-model:show="sheetVisible" :actions="sheetActions"
      cancel-text="取消" @select="onActionSelect" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listReports, resolveReport } from '@/api/report'

const router = useRouter()
const reports = ref([])
const totalCount = ref(0)
const loading = ref(false)
const finished = ref(false)
const activeStatus = ref('')
let pageNum = 1

const statusTabs = [
  { label: '全部', value: '' },
  { label: '待处理', value: 'PENDING' },
  { label: '已处理', value: 'RESOLVED' },
  { label: '已驳回', value: 'DISMISSED' }
]

const sheetVisible = ref(false)
const sheetActions = ref([])
let currentReport = null

const TARGET_LABELS = { DEMAND: '需求', USER: '用户', MESSAGE: '消息' }
const REASON_LABELS = {
  MISLEADING: '虚假信息', HARASSMENT: '骚扰/不当言论',
  ILLEGAL: '违禁品/违规内容', SPAM: '垃圾广告', OTHER: '其他'
}
const STATUS_LABELS = { PENDING: '待处理', RESOLVED: '已处理', DISMISSED: '已驳回' }

function targetLabel(v) { return TARGET_LABELS[v] || v }
function reasonLabel(v) { return REASON_LABELS[v] || v }
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

function switchStatus(status) {
  activeStatus.value = status
  pageNum = 1; reports.value = []; finished.value = false
  fetchReports()
}

async function fetchReports() {
  loading.value = true
  try {
    const params = { pageNum, pageSize: 20 }
    if (activeStatus.value) params.status = activeStatus.value
    const page = await listReports(params)
    reports.value = [...reports.value, ...(page.records || [])]
    totalCount.value = page.total
    finished.value = page.current >= page.pages
    pageNum++
  } catch {
    if (reports.value.length === 0) showToast('加载失败')
  } finally { loading.value = false }
}

onMounted(() => fetchReports())

function showActions(report) {
  if (report.status !== 'PENDING') return
  currentReport = report
  const actions = []
  if (report.targetType === 'DEMAND') {
    actions.push({ name: '下架需求', value: 'DELETE_DEMAND', color: '#ee0a24' })
  } else if (report.targetType === 'USER') {
    actions.push({ name: '封禁用户', value: 'BAN_USER', color: '#ee0a24' })
  }
  actions.push(
    { name: '标记已处理', value: 'RESOLVED' },
    { name: '驳回', value: 'DISMISSED' }
  )
  sheetActions.value = actions
  sheetVisible.value = true
}

async function onActionSelect(action) {
  if (!currentReport) return
  sheetVisible.value = false

  // Actions with value=DELETE_DEMAND or BAN_USER → resolved with action
  if (action.value === 'DELETE_DEMAND' || action.value === 'BAN_USER') {
    try {
      await resolveReport(currentReport.id, { status: 'RESOLVED', action: action.value })
      currentReport.status = 'RESOLVED'
      showToast(action.value === 'DELETE_DEMAND' ? '已下架需求' : '已封禁用户')
    } catch (e) {
      showToast(e.message || '操作失败')
    }
    return
  }

  // Simple RESOLVED / DISMISSED
  const status = action.value === 'RESOLVED' ? 'RESOLVED' : 'DISMISSED'
  try {
    await resolveReport(currentReport.id, { status })
    currentReport.status = status
    currentReport.adminNote = action.name
    showToast(status === 'RESOLVED' ? '已标记为已处理' : '已驳回')
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}
</script>

<style scoped>
.admin-page { background: var(--c-bg); padding-bottom: 32px; }
.admin-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.tabs-wrap { display: flex; gap: 8px; padding: 12px 16px; background: #fff; }
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
.target-tag {
  font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 4px;
}
.t-demand { background: #E3F2FD; color: #1565C0; }
.t-user { background: #F3E5F5; color: #7B1FA2; }
.t-message { background: #E8F5E9; color: #2E7D32; }

.status-tag {
  font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px;
}
.st-pending { background: #FFF3E0; color: #E65100; }
.st-resolved { background: #E8F5E9; color: #2E7D32; }
.st-dismissed { background: #F5F5F5; color: #757575; }

.target-id { font-size: 12px; color: var(--c-text-3); margin-left: 4px; }

/* Desktop table */
.table-wrap { display: none; }

@media (min-width: 768px) {
  .admin-page { background: var(--c-surface); }
  .tabs-wrap { max-width: var(--content-max); margin: 0 auto; padding: 12px 16px; }
  .summary-row { max-width: var(--content-max); margin: 0 auto; }
  .mobile-list { display: none; }
  .table-wrap {
    display: block;
    max-width: var(--content-max);
    margin: 0 auto;
    padding: 0 16px 32px;
  }
  .report-table { width: 100%; border-collapse: collapse; font-size: 14px; }
  .report-table thead { border-bottom: 2px solid var(--c-border); }
  .report-table th {
    text-align: left; padding: 12px 10px;
    font-size: 12px; font-weight: 700; color: var(--c-text-3);
    text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap;
  }
  .table-row { border-bottom: 1px solid var(--c-border); transition: background var(--ease); }
  .table-row:hover { background: var(--c-bg); }
  .table-row td { padding: 14px 10px; vertical-align: middle; }
  .td-reporter { font-weight: 600; color: var(--c-text-1); }
  .td-time { font-size: 12px; color: var(--c-text-3); white-space: nowrap; }
  .resolve-btn {
    padding: 4px 14px; border-radius: 6px; border: 1px solid var(--c-primary);
    background: transparent; color: var(--c-primary); font-size: 12px;
    font-weight: 600; cursor: pointer; transition: all var(--spring-fast-spatial);
  }
  .resolve-btn:hover { background: var(--c-primary); color: #fff; }
  .resolved-text { font-size: 12px; color: var(--c-text-3); }
  .table-loading, .table-finished, .table-empty { text-align: center; padding: 24px; font-size: 13px; color: var(--c-text-3); }
}

@media (min-width: 1024px) {
  .table-wrap { padding: 0 48px 32px; }
  .tabs-wrap { padding: 12px 32px; }
}

/* Mobile cards */
.mobile-list { padding: 0 16px; display: flex; flex-direction: column; gap: 10px; }
.report-card { padding: 14px; cursor: pointer; display: flex; flex-direction: column; gap: 8px; border-radius: var(--r-large); }
.report-card:active { background: var(--c-bg); }
.card-top { display: flex; justify-content: space-between; align-items: center; }
.card-body { display: flex; flex-direction: column; gap: 4px; }
.card-reason { font-size: 14px; font-weight: 600; color: var(--c-text-1); }
.card-meta { font-size: 12px; color: var(--c-text-3); }
.card-note { font-size: 12px; color: var(--c-text-2); padding: 6px 10px; background: var(--c-surface-variant); border-radius: 6px; }
</style>
