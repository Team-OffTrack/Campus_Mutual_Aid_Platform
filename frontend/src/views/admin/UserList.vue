<template>
  <div class="page admin-page">
    <van-nav-bar title="用户管理" left-arrow fixed placeholder
      class="admin-nav" @click-left="router.push('/')" />

    <!-- Search -->
    <div class="search-wrap">
      <van-search v-model="keyword" placeholder="搜索学号或姓名" shape="round"
        background="transparent" @search="onSearch" @clear="onSearch" />
    </div>

    <!-- Summary -->
    <div class="summary-row">
      <span class="summary-text">共 {{ totalCount }} 条结果</span>
    </div>

    <!-- ── Desktop table ── -->
    <div class="table-wrap">
      <table class="user-table">
        <thead>
          <tr>
            <th class="col-avatar"></th>
            <th class="col-name">姓名</th>
            <th class="col-id">学号</th>
            <th class="col-role">角色</th>
            <th class="col-status">状态</th>
            <th class="col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.userId" class="table-row">
            <td>
              <div class="td-avatar" :style="{ background: avatarColor(user.name) }">
                {{ (user.name || '?').charAt(0).toUpperCase() }}
              </div>
            </td>
            <td class="td-name">{{ user.name }}</td>
            <td class="td-id">{{ user.studentId }}</td>
            <td>
              <span class="role-tag" :class="user.role === 'ADMIN' ? 'role-admin' : 'role-user'">
                {{ user.role === 'ADMIN' ? '管理员' : '用户' }}
              </span>
            </td>
            <td>
              <span class="status-dot" :class="user.status === 1 ? 'dot-ok' : 'dot-ban'"></span>
              {{ user.status === 1 ? '正常' : '封禁' }}
            </td>
            <td>
              <button v-if="user.userId !== currentUserId"
                class="action-btn" :class="user.status === 1 ? 'btn-ban' : 'btn-ok'"
                @click="toggleUser(user)">
                {{ user.status === 1 ? '封禁' : '解封' }}
              </button>
              <span v-else class="self-tag">自己</span>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="loading" class="table-loading">加载中…</div>
      <div v-if="finished && users.length > 0" class="table-finished">已加载全部</div>
      <div v-if="!loading && users.length === 0" class="table-empty">暂无用户</div>
    </div>

    <!-- ── Mobile card list ── -->
    <van-list v-model:loading="loading" :finished="finished"
      finished-text="已加载全部" class="mobile-list" @load="fetchUsers">
      <div v-for="user in users" :key="user.userId" class="user-card" @click="showActions(user)">
        <div class="user-avatar" :style="{ background: avatarColor(user.name) }">
          {{ (user.name || '?').charAt(0).toUpperCase() }}
        </div>
        <div class="user-info">
          <span class="user-name">{{ user.name }}</span>
          <span class="user-id">学号：{{ user.studentId }}</span>
          <span class="user-role-tag" :class="user.role === 'ADMIN' ? 'r-admin' : 'r-user'">
            {{ user.role === 'ADMIN' ? '管理员' : '用户' }}
          </span>
        </div>
        <div class="status-badge" :class="user.status === 1 ? 'badge-ok' : 'badge-ban'">
          <span class="badge-dot"></span>
          {{ user.status === 1 ? '正常' : '已封禁' }}
        </div>
        <van-icon name="arrow" class="list-arrow" />
      </div>
    </van-list>

    <!-- Action sheet -->
    <van-action-sheet v-model:show="sheetVisible" :actions="sheetActions"
      cancel-text="取消" @select="onActionSelect" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listUsers, updateUserStatus } from '@/api/admin'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const currentUserId = computed(() => Number(authStore.userId))
const users = ref([])
const totalCount = ref(0)
const keyword = ref('')
const loading = ref(false)
const finished = ref(false)
let pageNum = 1

const sheetVisible = ref(false)
const sheetActions = ref([])
let currentUser = null

const AVATAR_COLORS = ['#5C6BF8','#06B6D4','#22C55E','#EF4444','#A855F7','#EC4899','#EAB308','#F97316']
function avatarColor(n) { return AVATAR_COLORS[(n || '?').charCodeAt(0) % AVATAR_COLORS.length] }

function onSearch() {
  pageNum = 1; users.value = []; finished.value = false; fetchUsers()
}

async function fetchUsers() {
  loading.value = true
  try {
    const page = await listUsers({ pageNum, pageSize: 20, keyword: keyword.value })
    users.value = [...users.value, ...(page.records || [])]
    totalCount.value = page.total
    finished.value = page.current >= page.pages
    pageNum++
  } catch (e) { /* skip */ }
  finally { loading.value = false }
}

onMounted(() => fetchUsers())

function showActions(user) {
  if (user.userId === currentUserId.value) {
    showToast('不能操作自己')
    return
  }
  currentUser = user
  const label = user.status === 1 ? '封禁该用户' : '解封该用户'
  const color = user.status === 1 ? '#ee0a24' : '#07c160'
  sheetActions.value = [{ name: label, color }]
  sheetVisible.value = true
}

async function toggleUser(user) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await updateUserStatus(user.userId, newStatus)
    user.status = newStatus
    showToast(newStatus === 1 ? '已解封' : '已封禁')
  } catch (e) { /* skip */ }
}

async function onActionSelect() {
  if (!currentUser) return
  await toggleUser(currentUser)
  sheetVisible.value = false
}
</script>

<style scoped>
.admin-page { background: var(--c-bg); }
.admin-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.admin-nav :deep(.van-nav-bar__title) { color: var(--c-text-1); }
.admin-nav :deep(.van-nav-bar__arrow) { color: var(--c-text-1) !important; }

/* Search */
.search-wrap { padding: 8px 0; background: #fff; }
.search-wrap :deep(.van-search__content) { background: var(--c-bg); border-radius: 18px; }
.search-wrap :deep(.van-cell) { padding: 0 !important; }
.search-wrap :deep(.van-field__control) { line-height: 36px; }

/* Summary */
.summary-row { padding: 12px 16px 4px; }
.summary-text { font-size: 13px; color: var(--c-text-3); }

/* ── Desktop table (hidden on mobile) ── */
.table-wrap { display: none; }

/* ── Mobile cards (hidden on desktop) ── */
.mobile-list { padding: 0 16px; }

.user-card {
  display: flex; align-items: center; gap: 12px;
  background: var(--c-surface);
  border-radius: var(--r-md);
  padding: 14px;
  margin-bottom: 8px;
  box-shadow: var(--s-xs);
  cursor: pointer;
  transition: background var(--ease);
}
.user-card:active { background: var(--c-bg); }

.user-avatar {
  width: 44px; height: 44px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff; flex-shrink: 0;
}

.user-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.user-name { font-size: 15px; font-weight: 600; color: var(--c-text-1); }
.user-id { font-size: 12px; color: var(--c-text-3); }

.user-role-tag {
  font-size: 10px; padding: 1px 6px; border-radius: 4px; align-self: flex-start; margin-top: 2px;
}
.r-admin { background: #EDE9FE; color: #7C3AED; }
.r-user { background: var(--c-bg); color: var(--c-text-3); }

.status-badge {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; font-weight: 600; flex-shrink: 0;
}
.badge-ok { color: var(--c-success); }
.badge-ban { color: var(--c-danger); }
.badge-dot { width: 6px; height: 6px; border-radius: 50%; }
.badge-ok .badge-dot { background: var(--c-success); }
.badge-ban .badge-dot { background: var(--c-danger); }

.list-arrow { flex-shrink: 0; color: var(--c-text-3); font-size: 14px; }

/* ════════════════════════════════════════
   Tablet (≥ 768px) → table visible
   ════════════════════════════════════════ */
@media (min-width: 768px) {
  .admin-page { background: var(--c-surface); }

  .search-wrap {
    max-width: var(--content-max); margin: 0 auto;
    padding: 16px 0 0; background: transparent;
  }
  .search-wrap :deep(.van-search) { max-width: 480px; padding: 0 0 0 16px; }

  .summary-row { max-width: var(--content-max); margin: 0 auto; padding: 12px 16px 4px; }

  /* Hide mobile cards */
  .mobile-list { display: none; }

  /* Show table */
  .table-wrap {
    display: block;
    max-width: var(--content-max);
    margin: 0 auto;
    padding: 0 16px 32px;
  }

  .user-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;
  }

  .user-table thead {
    border-bottom: 2px solid var(--c-border);
  }

  .user-table th {
    text-align: left;
    padding: 12px 10px;
    font-size: 12px;
    font-weight: 700;
    color: var(--c-text-3);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    white-space: nowrap;
  }

  .table-row {
    border-bottom: 1px solid var(--c-border);
    transition: background var(--ease);
  }
  .table-row:hover { background: var(--c-bg); }

  .table-row td {
    padding: 10px;
    vertical-align: middle;
  }

  .td-avatar {
    width: 38px; height: 38px; border-radius: 10px;
    display: flex; align-items: center; justify-content: center;
    font-size: 16px; font-weight: 700; color: #fff;
  }

  .td-name { font-weight: 600; color: var(--c-text-1); }
  .td-id { color: var(--c-text-2); font-size: 13px; }

  .role-tag {
    font-size: 11px; padding: 2px 10px; border-radius: 6px; font-weight: 600;
  }
  .role-admin { background: #EDE9FE; color: #7C3AED; }
  .role-user { background: var(--c-bg); color: var(--c-text-3); }

  .status-dot { display: inline-block; width: 7px; height: 7px; border-radius: 50%; margin-right: 5px; }
  .dot-ok { background: var(--c-success); }
  .dot-ban { background: var(--c-danger); }

  .action-btn {
    padding: 5px 14px; border-radius: 6px; font-size: 12px; font-weight: 600;
    border: 1.5px solid; cursor: pointer;
    transition: all var(--ease);
    background: #fff;
  }
  .btn-ban { color: var(--c-danger); border-color: #FECACA; }
  .btn-ban:hover { background: #FEF2F2; }
  .btn-ok { color: var(--c-success); border-color: #BBF7D0; }
  .btn-ok:hover { background: #F0FDF4; }
	  .self-tag { font-size: 12px; color: var(--c-text-3); }

  .table-loading, .table-finished, .table-empty {
    text-align: center; padding: 24px;
    font-size: 13px; color: var(--c-text-3);
  }
}

@media (min-width: 1024px) {
  .table-wrap { padding: 0 48px 32px; }
  .search-wrap :deep(.van-search) { padding: 0 0 0 32px; }
  .summary-row { padding: 12px 48px 4px; }
}
</style>
