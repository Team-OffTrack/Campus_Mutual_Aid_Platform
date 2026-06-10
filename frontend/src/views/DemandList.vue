<template>
  <div class="page list-page">
    <van-nav-bar left-arrow fixed placeholder class="list-nav" @click-left="router.push('/')">
      <template #title>
        <span class="nav-title">需求广场</span>
      </template>
      <template #right>
        <div class="nav-right">
          <van-icon name="plus" size="22" class="nav-plus" @click="router.push('/demands/publish')" />
          <NavActions />
        </div>
      </template>
    </van-nav-bar>

    <!-- ═══ Sticky toolbar: search + sort ═══ -->
    <div class="list-toolbar">
      <van-search v-model="keyword" placeholder="搜索需求…" shape="round"
        background="transparent" @search="onSearch" @clear="onSearch">
        <template #left-icon>
          <van-icon name="search" size="16" color="#787680" />
        </template>
      </van-search>

      <div class="sort-bar">
        <span v-for="s in sortOptions" :key="s.value"
          class="sort-chip" :class="{ 'sort-on': activeSort === s.value }"
          @click="switchSort(s.value)">
          {{ s.label }}
          <van-icon v-if="s.icon" :name="s.icon" size="10" />
        </span>
      </div>
    </div>

    <!-- ═══ Desktop table ═══ -->
    <div class="table-wrap">
      <table class="demand-table">
        <thead>
          <tr>
            <th class="col-type">类型</th>
            <th class="col-title">标题</th>
            <th class="col-pub">发布者</th>
            <th class="col-reward">报酬</th>
            <th class="col-loc">地点</th>
            <th class="col-time">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in demands" :key="d.demandId" class="table-row"
            @click="router.push('/demands/' + d.demandId)">
            <td><span class="type-badge" :style="typeStyle(d.type)">{{ typeLabel(d.type) }}</span></td>
            <td class="td-title">{{ d.title }}</td>
            <td class="td-pub">
              <van-icon v-if="d.isAnonymous" name="eye-o" size="14" />
              {{ d.publisherName }}
            </td>
            <td class="td-reward">{{ rewardText(d) }}</td>
            <td class="td-loc">{{ d.location || '—' }}</td>
            <td class="td-time">{{ timeAgo(d.createTime) }}</td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading && pageNum === 1" class="table-loading">加载中…</div>
      <div v-if="finished && demands.length > 0" class="table-finished">已加载全部</div>
      <div v-if="!loading && demands.length === 0" class="table-empty">
        <template v-if="fetchError">
          <van-icon name="failure" size="32" color="#EF4444" />
          <p style="margin-top:8px">加载失败</p>
          <van-button round size="small" type="primary" style="margin-top:12px" @click="fetchDemands">重试</van-button>
        </template>
        <template v-else>暂无需求</template>
      </div>
    </div>

    <!-- ═══ Mobile card feed ═══ -->
    <van-list v-model:loading="loading" :finished="finished" :error="loadError"
      finished-text="— 已加载全部 —" error-text="加载失败，点击重试" class="mobile-list" @load="fetchDemands">
      <div v-for="d in demands" :key="d.demandId" class="demand-card card"
        @click="router.push('/demands/' + d.demandId)">
        <div class="card-top">
          <span class="card-type" :style="typeStyle(d.type)">{{ typeLabel(d.type) }}</span>
          <span v-if="typeMeta(d)" class="card-type-meta">{{ typeMeta(d) }}</span>
          <span class="card-reward">{{ rewardText(d) }}</span>
        </div>
        <h3 class="card-title">{{ d.title }}</h3>
        <p class="card-desc">{{ d.description }}</p>
        <div class="card-meta">
          <span v-if="d.location" class="meta-item">
            <van-icon name="location-o" size="12" /> {{ d.location }}
          </span>
          <span v-if="d.deadline" class="meta-item">
            <van-icon name="clock-o" size="12" /> 截止 {{ formatDate(d.deadline) }}
          </span>
        </div>
        <div class="card-footer">
          <span class="footer-pub">
            <van-icon v-if="d.isAnonymous" name="eye-o" size="13" />
            {{ d.publisherName }}
          </span>
          <span class="footer-time">{{ timeAgo(d.createTime) }}</span>
        </div>
      </div>
    </van-list>

    <!-- ═══ Floating action button (mobile only) ═══ -->
    <div class="fab" role="button" aria-label="发布需求" @click="router.push('/demands/publish')">
      <van-icon name="add-o" size="24" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listDemands } from '@/api/demand'
import NavActions from '@/components/NavActions.vue'
import { TYPE_LABELS, TYPE_STYLES } from '@/constants/demandTypes'

const router = useRouter()
const demands = ref([])
const keyword = ref('')
const activeSort = ref('newest')
const loading = ref(false)
const finished = ref(false)
let pageNum = 1

const sortOptions = [
  { value: 'newest', label: '最新', icon: '' },
  { value: 'reward_high', label: '报酬最多', icon: 'arrow-down' },
  { value: 'reward_low', label: '报酬最少', icon: 'arrow-up' },
  { value: 'deadline', label: '即将截止', icon: '' }
]

function typeLabel(v) { return TYPE_LABELS[v] || v }
function typeStyle(v) { return TYPE_STYLES[v] || TYPE_STYLES.other }

function typeMeta(d) {
  if (!d.attributes) return ''
  const a = d.attributes
  if (d.type === 'errand') return a.pickup_location || ''
  if (d.type === 'trade' && a.item_price) return '¥' + a.item_price
  if (d.type === 'lost_found') return a.lf_type === 'LOST' ? '寻物' : a.lf_type === 'FOUND' ? '招领' : ''
  if (d.type === 'study') return a.subject || ''
  return ''
}

function rewardText(d) {
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

function formatDate(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function switchSort(sort) {
  activeSort.value = sort
  pageNum = 1; demands.value = []; finished.value = false
  fetchDemands()
}

function onSearch() {
  pageNum = 1; demands.value = []; finished.value = false
  fetchDemands()
}

const fetchError = ref(false)
const loadError = ref(false)

async function fetchDemands() {
  loading.value = true
  fetchError.value = false
  loadError.value = false
  try {
    const page = await listDemands({
      pageNum, pageSize: 20,
      keyword: keyword.value || undefined,
      sortBy: activeSort.value || undefined
    })
    demands.value = [...demands.value, ...(page.records || [])]
    finished.value = page.current >= page.pages
    pageNum++
  } catch (e) {
    if (demands.value.length === 0) {
      fetchError.value = true
    } else {
      loadError.value = true
    }
  }
  finally { loading.value = false }
}

onMounted(() => fetchDemands())
</script>

<style scoped>
.list-page { background: var(--c-bg); padding-bottom: 80px; }
.list-nav :deep(.van-nav-bar__content) {
  background: #fff !important;
  box-shadow: var(--s-xs);
}
.nav-title { font-weight: 600; }
.nav-right { display: flex; align-items: center; gap: 8px; }
.nav-plus { color: var(--c-primary); cursor: pointer; padding: 4px; border-radius: 50%; transition: background var(--ease); }
.nav-plus:active { background: var(--c-primary-container); }

/* ═══════════════════════════════════════
   Toolbar
   ═══════════════════════════════════════ */
.list-toolbar {
  position: sticky; top: 52px; z-index: 10;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  padding-bottom: 8px;
  border-bottom: 1px solid var(--c-border);
}
.list-toolbar :deep(.van-search) { padding: 8px 16px 0; }
.list-toolbar :deep(.van-search__content) {
  background: var(--c-surface-variant);
  border-radius: 20px;
}
.list-toolbar :deep(.van-cell) { padding: 0 !important; }
.list-toolbar :deep(.van-field__control) { line-height: 36px; }

.sort-bar { display: flex; gap: 8px; padding: 0 16px; }
.sort-chip {
  font-size: 12px; font-weight: 600; color: var(--c-text-3);
  padding: 5px 14px; border-radius: var(--r-full);
  cursor: pointer; transition: all var(--spring-fast-spatial);
  display: flex; align-items: center; gap: 3px;
  background: var(--c-surface-variant);
}
.sort-chip:active { transform: scale(0.95); }
.sort-on {
  background: var(--c-primary-container);
  color: var(--c-primary);
  font-weight: 700;
}

/* ═══════════════════════════════════════
   Mobile cards
   ═══════════════════════════════════════ */
.mobile-list { padding: 8px 16px 0; }

.demand-card {
  padding: 16px;
  margin-bottom: 10px;
  cursor: pointer;
  display: flex; flex-direction: column; gap: 10px;
  border-radius: var(--r-large);
}
.demand-card:active { background: var(--c-bg); }

.card-top { display: flex; justify-content: space-between; align-items: center; }
.card-type {
  font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 6px;
  letter-spacing: 0.3px;
}
.card-type-meta {
  font-size: 11px; color: var(--c-text-3);
  flex: 1; margin-left: 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.card-reward { font-size: 14px; font-weight: 700; color: var(--c-warning); flex-shrink: 0; }

.card-title { font-size: 16px; font-weight: 700; color: var(--c-text-1); line-height: 1.3; }
.card-desc {
  font-size: 13px; color: var(--c-text-3);
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; line-height: 1.5;
}

.card-meta { display: flex; gap: 12px; flex-wrap: wrap; }
.meta-item { font-size: 12px; color: var(--c-text-3); display: flex; align-items: center; gap: 4px; }

.card-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding-top: 6px; border-top: 1px solid var(--c-border);
}
.footer-pub { font-size: 12px; color: var(--c-text-2); display: flex; align-items: center; gap: 4px; font-weight: 500; }
.footer-time { font-size: 11px; color: var(--c-text-4); }

/* ═══════════════════════════════════════
   FAB
   ═══════════════════════════════════════ */
.fab {
  position: fixed; bottom: 28px; right: 24px;
  width: 56px; height: 56px; border-radius: 50%;
  background: var(--g-btn);
  box-shadow: var(--s-primary-lg);
  display: flex; align-items: center; justify-content: center;
  color: #fff; cursor: pointer; z-index: 20;
  transition: transform var(--spring-fast-spatial), box-shadow var(--spring-default-spatial);
}
.fab:active { transform: scale(0.9); }

/* ═══════════════════════════════════════
   Desktop table (hidden on mobile)
   ═══════════════════════════════════════ */
.table-wrap { display: none; }

@media (min-width: 768px) {
  .list-page { background: var(--c-surface); }
  .list-toolbar { position: static; backdrop-filter: none; background: transparent; border-bottom: none; }
  .list-toolbar :deep(.van-search) { max-width: 480px; padding: 12px 0 0 16px; }
  .sort-bar { padding: 4px 16px 8px; }
  .sort-chip:hover { background: var(--c-primary-container); }

  .mobile-list { display: none; }
  .fab { display: none; }

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
  .table-row { border-bottom: 1px solid var(--c-border); transition: background var(--ease); cursor: pointer; }
  .table-row:hover { background: var(--c-bg); }
  .table-row td { padding: 14px 10px; vertical-align: middle; }
  .type-badge { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 6px; }
  .td-title { font-weight: 600; color: var(--c-text-1); max-width: 260px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-pub { font-size: 13px; color: var(--c-text-2); }
  .td-reward { font-size: 13px; font-weight: 600; color: var(--c-warning); }
  .td-loc { font-size: 13px; color: var(--c-text-3); max-width: 120px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .td-time { font-size: 12px; color: var(--c-text-3); white-space: nowrap; }
  .table-loading, .table-finished, .table-empty { text-align: center; padding: 24px; font-size: 13px; color: var(--c-text-3); }
}

@media (min-width: 1024px) {
  .table-wrap { padding: 0 48px 32px; }
  .list-toolbar :deep(.van-search) { padding: 12px 0 0 32px; }
  .sort-bar { padding: 4px 32px 8px; }
}
</style>
