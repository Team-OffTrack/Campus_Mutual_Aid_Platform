<template>
  <div class="page fav-page">
    <van-nav-bar left-arrow fixed placeholder class="fav-nav" @click-left="router.push('/')">
      <template #title><span class="nav-title">我的收藏</span></template>
      <template #right><NavActions /></template>
    </van-nav-bar>

    <div class="content-wrap">
      <div v-if="loading" class="loading-hint">加载中…</div>

      <div v-else-if="favorites.length === 0" class="empty-hint">
        <template v-if="fetchError">
          <van-icon name="failure" size="48" color="#EF4444" />
          <p>加载失败</p>
          <van-button round size="small" type="primary" style="margin-top:12px" @click="fetchFavorites">重试</van-button>
        </template>
        <template v-else>
          <van-icon name="star-o" size="48" color="#C4C0CA" />
          <p>还没有收藏任何需求</p>
        </template>
      </div>

      <template v-else>
        <!-- Desktop table -->
        <div class="table-wrap">
          <table class="fav-table">
            <thead>
              <tr>
                <th class="col-type">类型</th>
                <th class="col-title">标题</th>
                <th class="col-other">发布者</th>
                <th class="col-reward">报酬</th>
                <th class="col-status">状态</th>
                <th class="col-time">收藏时间</th>
                <th class="col-act">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="f in favorites" :key="f.demandId" class="table-row">
                <td>
                  <span class="type-badge" :style="typeStyle(f.type)" @click="router.push('/demands/' + f.demandId)">
                    {{ typeLabel(f.type) }}
                  </span>
                </td>
                <td class="td-title" @click="router.push('/demands/' + f.demandId)">{{ f.title }}</td>
                <td class="td-other">{{ f.publisherName }}</td>
                <td class="td-reward">{{ rewardText(f) }}</td>
                <td><span class="status-tag" :class="'s-' + f.status.toLowerCase()">{{ statusLabel(f.status) }}</span></td>
                <td class="td-time">{{ timeAgo(f.createTime) }}</td>
                <td>
                  <van-icon name="star" color="#EAB308" size="20" class="fav-icon"
                    @click.stop="handleUnfavorite(f)" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Mobile cards -->
        <van-list v-model:loading="loadingMore" :finished="finished" @load="onLoadMore" class="mobile-list">
          <div v-for="f in favorites" :key="f.demandId" class="fav-card card"
            @click="router.push('/demands/' + f.demandId)">
            <div class="card-top">
              <span class="card-type" :style="typeStyle(f.type)">{{ typeLabel(f.type) }}</span>
              <span class="card-status" :class="'s-' + f.status.toLowerCase()">{{ statusLabel(f.status) }}</span>
            </div>
            <h3 class="card-title">{{ f.title }}</h3>
            <div class="card-meta">
              <span>发布者：</span>
              <span class="meta-name">{{ f.publisherName }}</span>
              <span class="meta-divider">·</span>
              <span class="meta-reward">{{ rewardText(f) }}</span>
            </div>
            <div class="card-footer">
              <span class="footer-time">{{ timeAgo(f.createTime) }}</span>
              <van-icon name="star" color="#EAB308" size="20" class="fav-icon"
                @click.stop="handleUnfavorite(f)" />
            </div>
          </div>
        </van-list>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { myFavorites, unfavoriteDemand } from '@/api/demand'
import { useFavoritesStore } from '@/stores/favorites'
import NavActions from '@/components/NavActions.vue'
import { TYPE_LABELS, TYPE_STYLES, rewardText } from '@/constants/demandTypes'

const router = useRouter()
const favoritesStore = useFavoritesStore()

const favorites = ref([])
const loading = ref(false)
const fetchError = ref(false)
const loadingMore = ref(false)
const finished = ref(false)
const pageNum = ref(1)
const pageSize = 10

const STATUS_LABELS = { OPEN: '待接单', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }

function typeLabel(v) { return TYPE_LABELS[v] || v }
function typeStyle(v) { return TYPE_STYLES[v] || TYPE_STYLES.other }
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

async function fetchFavorites(reset = false) {
  if (reset) {
    pageNum.value = 1
    finished.value = false
  }
  loading.value = true
  fetchError.value = false
  try {
    const page = await myFavorites({ pageNum: pageNum.value, pageSize })
    if (reset) {
      favorites.value = page.records || []
    } else {
      favorites.value = [...favorites.value, ...(page.records || [])]
    }
    if (page.current >= page.pages) {
      finished.value = true
    }
  } catch (e) {
    if (reset) {
      favorites.value = []
      fetchError.value = true
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function handleUnfavorite(f) {
  try {
    await unfavoriteDemand(f.demandId)
    favoritesStore.removeOptimistic(f.demandId)
    favorites.value = favorites.value.filter(d => d.demandId !== f.demandId)
    showToast('已取消收藏')
  } catch (e) {
    showToast(e.message || '操作失败')
  }
}

async function onLoadMore() {
  pageNum.value++
  await fetchFavorites(false)
}

onMounted(() => fetchFavorites(true))
</script>

<style scoped>
.fav-page { background: var(--c-bg); padding-bottom: 32px; }
.fav-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.content-wrap { padding: 16px; }
.loading-hint, .empty-hint { text-align: center; padding: 60px 16px; color: var(--c-text-3); font-size: 14px; }
.empty-hint p { margin-top: 12px; }

/* Cards */
.mobile-list { display: flex; flex-direction: column; gap: 10px; }
.fav-card { padding: 16px; cursor: pointer; display: flex; flex-direction: column; gap: 10px; border-radius: var(--r-large); }
.fav-card:active { background: var(--c-bg); }
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
.card-footer { display: flex; justify-content: space-between; align-items: center; }
.footer-time { font-size: 11px; color: var(--c-text-4); }
.fav-icon { cursor: pointer; flex-shrink: 0; }
.fav-icon:hover { transform: scale(1.15); transition: transform 0.15s; }

/* Desktop table */
.table-wrap { display: none; }
@media (min-width: 768px) {
  .fav-page { background: var(--c-surface); }
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 0 16px 32px; }
  .mobile-list { display: none; }
  .table-wrap { display: block; }
  .fav-table { width: 100%; border-collapse: collapse; font-size: 14px; }
  .fav-table thead { border-bottom: 2px solid var(--c-border); }
  .fav-table th {
    text-align: left; padding: 12px 10px;
    font-size: 12px; font-weight: 700; color: var(--c-text-3);
    text-transform: uppercase; letter-spacing: 0.5px; white-space: nowrap;
  }
  .table-row { border-bottom: 1px solid var(--c-border); transition: background var(--ease); }
  .table-row:hover { background: var(--c-bg); }
  .table-row td { padding: 14px 10px; vertical-align: middle; }
  .type-badge { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 6px; cursor: pointer; }
  .td-title { font-weight: 600; color: var(--c-text-1); max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
  .td-other { font-size: 13px; color: var(--c-text-2); }
  .td-reward { font-size: 13px; font-weight: 600; color: var(--c-warning); }
  .td-time { font-size: 12px; color: var(--c-text-3); white-space: nowrap; }
  .status-tag { font-size: 11px; font-weight: 600; padding: 3px 10px; border-radius: 6px; }
  .col-act { width: 60px; }
}
@media (min-width: 1024px) {
  .content-wrap { padding: 0 48px 32px; }
}
</style>
