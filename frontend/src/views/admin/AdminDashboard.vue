<template>
  <div class="page admin-page">
    <van-nav-bar left-arrow fixed placeholder class="admin-nav" @click-left="router.push('/')">
      <template #title><span class="nav-title">管理后台</span></template>
    </van-nav-bar>

    <div class="dash-content">
      <!-- Stats grid -->
      <div class="stats-grid">
        <!-- Users card -->
        <div class="stat-card card">
          <div class="stat-icon" style="background:#E3F2FD"><van-icon name="friends-o" color="#1565C0" size="22" /></div>
          <div class="stat-body">
            <span class="stat-value">{{ fmtNum(dashboard.totalUsers) }}</span>
            <span class="stat-label">用户总量</span>
            <span class="stat-sub">今日 +{{ dashboard.newUsersToday }}</span>
          </div>
          <div class="stat-extra">
            <span class="extra-item">7日活跃 {{ fmtNum(dashboard.activeUsers7d) }}</span>
            <span class="extra-item">30日活跃 {{ fmtNum(dashboard.activeUsers30d) }}</span>
          </div>
        </div>

        <!-- Demands card -->
        <div class="stat-card card">
          <div class="stat-icon" style="background:#E8F5E9"><van-icon name="orders-o" color="#2E7D32" size="22" /></div>
          <div class="stat-body">
            <span class="stat-value">{{ fmtNum(dashboard.totalDemands) }}</span>
            <span class="stat-label">需求总量</span>
            <span class="stat-sub">今日 +{{ dashboard.newDemandsToday }}</span>
          </div>
          <div class="stat-extra">
            <span v-for="(cnt, type) in dashboard.demandTypeDistribution" :key="type"
              class="type-chip" :class="'ty-' + type">
              {{ typeLabel(type) }} {{ cnt }}
            </span>
          </div>
        </div>

        <!-- Points card -->
        <div class="stat-card card">
          <div class="stat-icon" style="background:#FFF8E1"><van-icon name="gold-coin-o" color="#F9A825" size="22" /></div>
          <div class="stat-body">
            <span class="stat-value">{{ fmtNum(dashboard.totalPointsIssued) }}</span>
            <span class="stat-label">积分发放总量</span>
          </div>
          <div class="stat-extra">
            <span class="extra-item">签到率 {{ (dashboard.checkinRate * 100).toFixed(1) }}%</span>
          </div>
        </div>

        <!-- Reports card -->
        <div class="stat-card card" :class="{ 'has-pending': dashboard.pendingReportCount > 0 }">
          <div class="stat-icon" :style="{ background: dashboard.pendingReportCount > 0 ? '#FFEBEE' : '#F5F5F5' }">
            <van-icon name="warning-o" :color="dashboard.pendingReportCount > 0 ? '#D32F2F' : '#9E9E9E'" size="22" />
          </div>
          <div class="stat-body">
            <span class="stat-value" :class="{ 'text-danger': dashboard.pendingReportCount > 0 }">
              {{ dashboard.pendingReportCount }}
            </span>
            <span class="stat-label">待处理举报</span>
          </div>
        </div>
      </div>

      <!-- Quick links -->
      <h3 class="section-title">管理功能</h3>
      <div class="link-cards">
        <div class="link-card card" @click="router.push('/admin/users')">
          <div class="link-left">
            <div class="link-icon" style="background:#E3F2FD"><van-icon name="friends-o" color="#1565C0" size="20" /></div>
            <div>
              <p class="link-title">用户管理</p>
              <p class="link-sub">查看用户 · 封禁/解封</p>
            </div>
          </div>
          <van-icon name="arrow" class="link-arrow" />
        </div>
        <div class="link-card card" @click="router.push('/admin/demands')">
          <div class="link-left">
            <div class="link-icon" style="background:#E8F5E9"><van-icon name="orders-o" color="#2E7D32" size="20" /></div>
            <div>
              <p class="link-title">需求管理</p>
              <p class="link-sub">查看需求 · 直接删除</p>
            </div>
          </div>
          <van-icon name="arrow" class="link-arrow" />
        </div>
        <div class="link-card card" @click="router.push('/admin/reports')">
          <div class="link-left">
            <div class="link-icon" style="background:#FFEBEE"><van-icon name="warning-o" color="#D32F2F" size="20" /></div>
            <div>
              <p class="link-title">
                举报管理
                <span v-if="dashboard.pendingReportCount > 0" class="badge">{{ dashboard.pendingReportCount > 99 ? '99+' : dashboard.pendingReportCount }}</span>
              </p>
              <p class="link-sub">处理用户举报</p>
            </div>
          </div>
          <van-icon name="arrow" class="link-arrow" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getDashboard } from '@/api/admin'

const router = useRouter()
const dashboard = ref({
  totalUsers: 0, newUsersToday: 0, activeUsers7d: 0, activeUsers30d: 0,
  totalDemands: 0, newDemandsToday: 0, demandTypeDistribution: {},
  totalPointsIssued: 0, checkinRate: 0, pendingReportCount: 0
})

const TYPE_LABELS = {
  errand: '跑腿', trade: '交易', team: '组队',
  lost_found: '失物', study: '学习', other: '其他'
}
function typeLabel(v) { return TYPE_LABELS[v] || v }

function fmtNum(n) {
  if (n == null) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

onMounted(async () => {
  try {
    dashboard.value = await getDashboard()
  } catch {
    showToast('加载统计数据失败')
  }
})
</script>

<style scoped>
.admin-page { background: var(--c-bg); padding-bottom: 32px; }
.admin-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.dash-content { padding: 16px; display: flex; flex-direction: column; gap: 20px; }

/* Stats grid */
.stats-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
}
.stat-card {
  padding: 16px; border-radius: var(--r-large);
  display: flex; flex-direction: column; gap: 12px;
}
.stat-card.has-pending { border: 1px solid #FFCDD2; }
.stat-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
}
.stat-body { display: flex; flex-direction: column; gap: 2px; }
.stat-value { font-size: 24px; font-weight: 800; color: var(--c-text-1); line-height: 1.1; }
.stat-value.text-danger { color: #D32F2F; }
.stat-label { font-size: 12px; color: var(--c-text-3); font-weight: 500; }
.stat-sub { font-size: 11px; color: var(--c-primary); font-weight: 600; }
.stat-extra { display: flex; flex-wrap: wrap; gap: 6px; padding-top: 4px; border-top: 1px solid var(--c-border); }
.extra-item { font-size: 11px; color: var(--c-text-3); }

/* Type chips in demand card */
.type-chip {
  font-size: 10px; font-weight: 600; padding: 2px 8px; border-radius: 4px;
}
.ty-errand { background:#E3F2FD; color:#1565C0; }
.ty-trade { background:#FFF3E0; color:#E65100; }
.ty-team { background:#F3E5F5; color:#7B1FA2; }
.ty-lost_found { background:#E8F5E9; color:#2E7D32; }
.ty-study { background:#E8EAF6; color:#283593; }
.ty-other { background:#F5F5F5; color:#616161; }

/* Section title */
.section-title { font-size: 16px; font-weight: 700; color: var(--c-text-1); margin: 0; }

/* Quick link cards */
.link-cards { display: flex; flex-direction: column; gap: 10px; }
.link-card {
  padding: 16px; border-radius: var(--r-large);
  display: flex; align-items: center; justify-content: space-between;
  cursor: pointer; transition: background var(--spring-fast-spatial);
}
.link-card:active { background: var(--c-bg); }
.link-left { display: flex; align-items: center; gap: 12px; }
.link-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.link-title { font-size: 15px; font-weight: 600; color: var(--c-text-1); display: flex; align-items: center; gap: 6px; }
.link-sub { font-size: 12px; color: var(--c-text-3); margin-top: 2px; }
.link-arrow { color: var(--c-text-3); }
.badge {
  font-size: 11px; font-weight: 700; color: #fff; background: #D32F2F;
  padding: 1px 6px; border-radius: 10px; min-width: 18px; text-align: center;
}

/* Responsive */
@media (min-width: 768px) {
  .admin-page { background: var(--c-surface); }
  .dash-content { max-width: var(--content-max); margin: 0 auto; padding: 24px 32px; }
  .stats-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (min-width: 1024px) {
  .dash-content { padding: 32px 48px; }
}
</style>
