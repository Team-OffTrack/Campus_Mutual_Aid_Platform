<template>
  <div class="page home-page">
    <!-- Top nav bar — transparent over hero -->
    <van-nav-bar title="" fixed placeholder class="home-nav">
      <template #right>
        <NavActions light />
      </template>
    </van-nav-bar>

    <!-- ═══ Hero banner ═══ -->
    <div class="hero-banner">
      <!-- Decorative shapes -->
      <div class="hero-deco" aria-hidden="true">
        <div class="deco-circle deco-1 float-slow"></div>
        <div class="deco-circle deco-2 float-fast"></div>
        <div class="deco-circle deco-3 float-slow"></div>
        <div class="deco-ring deco-r1"></div>
        <div class="deco-ring deco-r2"></div>
      </div>

      <div class="hero-content">
        <p class="hero-greeting">{{ greeting }}</p>
        <h2 class="hero-name">{{ authStore.name || '同学' }}</h2>
        <p class="hero-tip">有什么需要帮忙的吗？</p>
      </div>
    </div>

    <div class="content-wrap">
      <!-- ═══ Stats bar (glass morphism) ═══ -->
      <div class="stats-row glass">
        <div class="stat-chip points-chip" role="button" tabindex="0"
             @click="router.push('/points/history')">
          <div class="stat-icon-wrap" style="background:var(--c-primary-container)">
            <van-icon name="gold-coin-o" color="#6750A4" size="16" />
          </div>
          <div class="stat-text">
            <span class="stat-val">{{ stats.availablePoints != null ? stats.availablePoints : '—' }}</span>
            <span class="stat-label">可用积分</span>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-chip">
          <div class="stat-icon-wrap" style="background:#E8F5E9">
            <van-icon name="star-o" color="#2E7D32" size="16" />
          </div>
          <div class="stat-text">
            <span class="stat-val">{{ stats.reputationScore != null ? stats.reputationScore : '—' }}</span>
            <span class="stat-label">信誉评分</span>
          </div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-chip">
          <div class="stat-icon-wrap" style="background:#FFF3E0">
            <van-icon name="lock" color="#E65100" size="16" />
          </div>
          <div class="stat-text">
            <span class="stat-val">{{ stats.frozenPoints != null ? stats.frozenPoints : '—' }}</span>
            <span class="stat-label">冻结积分</span>
          </div>
        </div>
      </div>

      <!-- ═══ Check-in card ═══ -->
      <div class="checkin-card glass" :class="{ 'checked-in': checkinStatus.checkedIn }" v-if="!checkinLoading">
        <div class="checkin-left">
          <div class="checkin-icon-wrap">
            <van-icon :name="checkinStatus.checkedIn ? 'success' : 'gift-o'"
              :color="checkinStatus.checkedIn ? '#2E7D32' : '#6750A4'" size="22" />
          </div>
          <div class="checkin-text">
            <span class="checkin-title">
              {{ checkinStatus.checkedIn ? '今日已签到' : '每日签到' }}
            </span>
            <span class="checkin-sub">
              <template v-if="checkinStatus.checkedIn">
                已连续签到 {{ checkinStatus.currentStreak }} 天
              </template>
              <template v-else>
                签到领积分
                <span v-if="checkinStatus.currentStreak > 0"> · 已连续 {{ checkinStatus.currentStreak }} 天</span>
              </template>
            </span>
          </div>
        </div>
        <div class="checkin-right">
          <van-button v-if="!checkinStatus.checkedIn"
            size="small" round type="primary"
            :loading="checkinLoading" @click="handleCheckin"
            class="checkin-btn">
            签到
          </van-button>
          <div v-else class="checkin-done">
            <van-icon name="success" color="#2E7D32" size="22" />
          </div>
        </div>
      </div>

      <!-- ═══ Feature section ═══ -->
      <div class="section">
        <div class="section-header">
          <h3 class="section-title">功能入口</h3>
          <span class="section-sub">选择你需要帮助的类型</span>
        </div>
        <div class="feature-grid">
          <div v-for="feat in features" :key="feat.name"
               class="feat-card card"
               :style="{ '--fc-bg': feat.bg, '--fc-ic': feat.ic }"
               role="button" :aria-label="feat.name"
               @click="feat.type === 'browse' ? goBrowse() : goPublish(feat.type)">
            <div class="feat-icon-wrap">
              <van-icon :name="feat.icon" class="feat-icon" />
            </div>
            <div class="feat-text">
              <span class="feat-name">{{ feat.name }}</span>
              <span class="feat-desc">{{ feat.desc }}</span>
            </div>
            <van-icon name="arrow" class="feat-arrow" />
          </div>
        </div>
      </div>

      <!-- ═══ Quick actions ═══ -->
      <div class="section">
        <div class="section-header">
          <h3 class="section-title">快捷入口</h3>
        </div>
        <div class="quick-actions">
          <div class="action-card card" @click="router.push('/orders')" role="button">
            <div class="action-left">
              <div class="action-icon-wrap" style="background:#E3F2FD">
                <van-icon name="orders-o" color="#1565C0" size="20" />
              </div>
              <div>
                <p class="action-title">我的订单</p>
                <p class="action-sub">查看我发布和接取的需求</p>
              </div>
            </div>
            <van-icon name="arrow" class="action-arrow" />
          </div>

          <div class="action-card card" @click="router.push('/favorites')" role="button">
            <div class="action-left">
              <div class="action-icon-wrap" style="background:#FFF8E1">
                <van-icon name="star-o" color="#EAB308" size="20" />
              </div>
              <div>
                <p class="action-title">我的收藏</p>
                <p class="action-sub">查看收藏的需求</p>
              </div>
            </div>
            <van-icon name="arrow" class="action-arrow" />
          </div>

          <div v-if="authStore.isAdmin" class="action-card card admin-entry" @click="router.push('/admin')" role="button">
            <div class="action-left">
              <div class="action-icon-wrap" style="background:#FFF8E1">
                <van-icon name="setting-o" color="#F9A825" size="20" />
              </div>
              <div>
                <p class="action-title">
                  管理后台
                  <span v-if="pendingReportCount > 0" class="report-badge">{{ pendingReportCount > 99 ? '99+' : pendingReportCount }}</span>
                </p>
                <p class="action-sub">用户 · 需求 · 举报 · 数据</p>
              </div>
            </div>
            <van-icon name="arrow" class="action-arrow" />
          </div>
        </div>
      </div>

      <!-- ═══ Logout ═══ -->
      <div class="logout-section">
        <van-button type="default" block round class="logout-btn" @click="handleLogout">
          退出登录
        </van-button>
      </div>

      <!-- Bottom safe area spacer -->
      <div class="bottom-spacer"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getProfile } from '@/api/user'
import { checkin, getCheckinStatus } from '@/api/points'
import NavActions from '@/components/NavActions.vue'
import { useBadgeToastStore } from '@/stores/badgeToast'

const router = useRouter()
const authStore = useAuthStore()
const badgeToastStore = useBadgeToastStore()

const stats = ref({ availablePoints: null, reputationScore: null, frozenPoints: null })
const checkinStatus = ref({ checkedIn: false, currentStreak: 0, lastCheckinDate: null })
const checkinLoading = ref(false)
const pendingReportCount = ref(0)

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了 🌙'
  if (h < 12) return '早上好 ☀️'
  if (h < 14) return '中午好 🌤️'
  if (h < 18) return '下午好 🌈'
  return '晚上好 ✨'
})

const features = [
  { name: '跑腿代取', desc: '发布取件需求', icon: 'logistics',    bg: 'var(--feat-1-bg)', ic: 'var(--feat-1-ic)', type: 'errand' },
  { name: '二手交易', desc: '发布交易信息', icon: 'shop',        bg: 'var(--feat-2-bg)', ic: 'var(--feat-2-ic)', type: 'trade' },
  { name: '组队匹配', desc: '发布组队需求', icon: 'friends',      bg: 'var(--feat-3-bg)', ic: 'var(--feat-3-ic)', type: 'team' },
  { name: '失物招领', desc: '发布招领信息', icon: 'search',      bg: 'var(--feat-4-bg)', ic: 'var(--feat-4-ic)', type: 'lost_found' },
  { name: '学习互助', desc: '发布学习需求', icon: 'chat-o',       bg: 'var(--feat-5-bg)', ic: 'var(--feat-5-ic)', type: 'study' },
  { name: '需求广场', desc: '浏览所有需求', icon: 'gem-o',       bg: 'var(--feat-6-bg)', ic: 'var(--feat-6-ic)', type: 'browse' },
]

function goPublish(type) { router.push({ path: '/demands/publish', query: { type } }) }
function goBrowse() { router.push('/demands') }

async function handleLogout() {
  try { await showConfirmDialog({ title: '退出登录', message: '确定要退出当前账号吗？' }) }
  catch { return }
  authStore.logout()
  showToast('已退出')
  router.push('/login')
}

async function handleCheckin() {
  checkinLoading.value = true
  try {
    const result = await checkin()
    checkinStatus.value = {
      checkedIn: true,
      currentStreak: result.streak,
      lastCheckinDate: result.checkinDate
    }
    stats.value.availablePoints += result.pointsAwarded
    showToast(`签到成功 +${result.pointsAwarded} 积分`)
    await badgeToastStore.checkNewBadges()
  } catch (e) {
    showToast(e.message || '签到失败')
  } finally {
    checkinLoading.value = false
  }
}

async function fetchPendingReportCount() {
  if (!authStore.isAdmin) return
  try {
    const { listReports } = await import('@/api/report')
    const page = await listReports({ pageNum: 1, pageSize: 1, status: 'PENDING' })
    pendingReportCount.value = page.total || 0
  } catch { /* non-critical */ }
}

onMounted(async () => {
  try {
    const data = await getProfile()
    stats.value = {
      availablePoints: data.availablePoints ?? 0,
      reputationScore: data.reputationScore ?? 5.0,
      frozenPoints: data.frozenPoints ?? 0,
    }
  } catch { /* use fallback placeholders */ }

  try {
    const status = await getCheckinStatus()
    checkinStatus.value = status
  } catch { /* ignore — check-in will show default unchecked state */ }

  fetchPendingReportCount()
})
</script>

<style scoped>
.home-page { background: var(--c-bg); }

/* ═══════════════════════════════════════
   Nav — transparent over hero
   ═══════════════════════════════════════ */
.home-nav :deep(.van-nav-bar),
.home-nav :deep(.van-nav-bar__content) {
  background: transparent !important;
  transition: background var(--ease);
}

/* ═══════════════════════════════════════
   Hero
   ═══════════════════════════════════════ */
.hero-banner {
  position: relative;
  background: var(--g-hero);
  padding: 24px 24px 64px;
  overflow: hidden;
  isolation: isolate;
  border-radius: 0 0 var(--r-extra-large) var(--r-extra-large);
}

.hero-content {
  position: relative;
  z-index: 3;
  color: #fff;
}

.hero-greeting {
  font-size: 14px;
  color: rgba(255,255,255,0.78);
  margin-bottom: 6px;
  font-weight: 500;
}

.hero-name {
  font: var(--t-headline);
  color: #fff;
  margin-bottom: 6px;
  font-size: 28px;
}

.hero-tip {
  font-size: 14px;
  color: rgba(255,255,255,0.62);
  font-weight: 400;
}

/* Decorative animated shapes */
.hero-deco {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.12;
  background: #fff;
}

.deco-1 {
  width: 180px; height: 180px;
  top: -70px; right: -50px;
}

.deco-2 {
  width: 80px; height: 80px;
  top: 40px; right: 100px;
  opacity: 0.22;
}

.deco-3 {
  width: 120px; height: 120px;
  bottom: -30px; left: -30px;
  opacity: 0.15;
}

.deco-ring {
  position: absolute;
  border: 2px solid rgba(255,255,255,0.10);
  border-radius: 50%;
}

.deco-r1 {
  width: 260px; height: 260px;
  top: -100px; right: -100px;
}

.deco-r2 {
  width: 160px; height: 160px;
  top: 10px; right: 60px;
}

/* ═══════════════════════════════════════
   Stats — glass morphism card
   ═══════════════════════════════════════ */
.stats-row {
  display: flex;
  align-items: center;
  margin: -36px 0 0;
  padding: 18px 12px;
  position: relative;
  z-index: 10;
  box-shadow: var(--s-md), var(--s-glow);
}

.stat-chip {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  cursor: default;
  border-radius: var(--r-md);
  transition: background var(--ease);
}

.stat-icon-wrap {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.stat-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text-1);
  line-height: 1.2;
}

.stat-label {
  font-size: 11px;
  color: var(--c-text-3);
  font-weight: 500;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--c-border);
  flex-shrink: 0;
}

.points-chip {
  cursor: pointer;
  transition: opacity var(--ease);
}
.points-chip:hover { opacity: 0.78; }
.points-chip:active { opacity: 0.6; }

/* ═══════════════════════════════════════
   Check-in card
   ═══════════════════════════════════════ */
.checkin-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding: 14px 18px;
  box-shadow: var(--s-md), var(--s-glow);
  transition: border-color var(--ease), background var(--ease);
  border: 1.5px solid transparent;
}
.checkin-card.checked-in {
  border-color: #A5D6A7;
  background: linear-gradient(135deg, rgba(232,245,233,0.6), rgba(255,255,255,0.92));
}

.checkin-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.checkin-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: var(--c-primary-container);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.checked-in .checkin-icon-wrap {
  background: #E8F5E9;
}

.checkin-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.checkin-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--c-text-1);
}

.checkin-sub {
  font-size: 12px;
  color: var(--c-text-3);
}

.checkin-btn {
  height: 36px !important;
  padding: 0 20px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
}

.checkin-done {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ═══════════════════════════════════════
   Sections
   ═══════════════════════════════════════ */
.section { padding-top: 24px; }

.section-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text-1);
}

.section-sub {
  font-size: 12px;
  color: var(--c-text-3);
}

/* ═══════════════════════════════════════
   Feature grid
   ═══════════════════════════════════════ */
.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.feat-card {
  background: var(--fc-bg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: transform var(--spring-fast-spatial), box-shadow var(--spring-default-spatial);
  position: relative;
  border-radius: var(--r-large);
}
.feat-card:active { transform: scale(0.97); }

.feat-icon-wrap {
  width: 46px; height: 46px;
  border-radius: 14px;
  background: var(--fc-ic);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
}

.feat-icon { font-size: 22px; color: #fff !important; }

.feat-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.feat-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--c-text-1);
}

.feat-desc {
  font-size: 12px;
  color: var(--c-text-3);
}

.feat-arrow {
  position: absolute;
  top: 16px;
  right: 14px;
  font-size: 14px;
  color: var(--c-text-4);
  opacity: 0.6;
}

/* ═══════════════════════════════════════
   Quick action cards
   ═══════════════════════════════════════ */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.action-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  cursor: pointer;
  border-radius: var(--r-large);
  transition: transform var(--spring-fast-spatial), box-shadow var(--spring-default-spatial);
}
.action-card:active { transform: scale(0.985); }

.action-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.action-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-1);
  margin-bottom: 2px;
}

.action-sub {
  font-size: 12px;
  color: var(--c-text-3);
}

.action-arrow {
  color: var(--c-text-4);
  font-size: 16px;
}

.admin-entry {
  border: 1.5px solid #FFE082;
}

.report-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  background: #EF4444;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  margin-left: 6px;
  padding: 0 6px;
  vertical-align: middle;
}

/* ═══════════════════════════════════════
   Logout
   ═══════════════════════════════════════ */
.logout-section { padding-top: 28px; }

.logout-btn {
  color: var(--c-text-2) !important;
  border-color: var(--c-border) !important;
  background: var(--c-surface) !important;
  box-shadow: var(--s-xs) !important;
  height: 48px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  transition: all var(--ease) !important;
}
.logout-btn:active { background: var(--c-bg) !important; }

.bottom-spacer { height: calc(24px + var(--safe-bottom)); }

/* ═══════════════════════════════════════
   Tablet (≥ 768px)
   ═══════════════════════════════════════ */
@media (min-width: 768px) {
  .hero-banner {
    padding: 40px 32px 80px;
  }
  .hero-name { font-size: 34px; }
  .hero-greeting { font-size: 16px; }

  .feature-grid { grid-template-columns: repeat(3, 1fr); gap: 14px; }

  .stats-row {
    margin: -44px 0 0;
    padding: 20px 16px;
  }
  .stat-val { font-size: 22px; }

  .quick-actions {
    flex-direction: row;
  }
  .action-card { flex: 1; }

  .logout-section { max-width: 400px; margin: 0 auto; }

  .deco-1 { width: 300px; height: 300px; top: -120px; right: -80px; }
  .deco-r1 { width: 400px; height: 400px; top: -160px; right: -160px; }
}

/* ═══════════════════════════════════════
   Desktop (≥ 1024px) — hover effects + 4-col
   ═══════════════════════════════════════ */
@media (min-width: 1024px) {
  .hero-banner {
    padding: 48px 48px 92px;
  }
  .hero-name { font-size: 40px; }

  .feature-grid { grid-template-columns: repeat(3, 1fr); gap: 16px; }

  .feat-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--s-md), var(--s-glow);
  }
  .feat-card:active { transform: scale(0.98); }

  .action-card:hover {
    box-shadow: var(--s-md);
    border-color: transparent;
    transform: translateY(-2px);
  }

  .deco-1 { width: 400px; height: 400px; top: -160px; right: -120px; }
  .deco-2 { width: 120px; height: 120px; top: 80px; right: 200px; }
  .deco-r1 { width: 500px; height: 500px; top: -200px; right: -200px; }
  .deco-r2 { width: 260px; height: 260px; top: 20px; right: 140px; }
}
</style>
