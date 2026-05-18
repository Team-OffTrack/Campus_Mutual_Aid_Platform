<template>
  <div class="page home-page">
    <!-- Top nav bar -->
    <van-nav-bar title="" fixed placeholder class="home-nav">
      <template #right>
        <NavActions light />
      </template>
    </van-nav-bar>

    <!-- Hero banner -->
    <div class="hero-banner">
      <div class="hero-content">
        <p class="hero-greeting">{{ greeting }}</p>
        <h2 class="hero-name">{{ authStore.name || '同学' }}</h2>
        <p class="hero-tip">有什么需要帮忙的吗？</p>
      </div>
      <div class="hero-deco" aria-hidden="true">
        <div class="deco-ring deco-ring-1"></div>
        <div class="deco-ring deco-ring-2"></div>
      </div>
    </div>

    <div class="content-wrap">
      <!-- Stats bar -->
      <div class="stats-row">
        <div class="stat-chip">
          <span class="stat-val">0</span>
          <span class="stat-label">可用积分</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-chip">
          <span class="stat-val">5.0</span>
          <span class="stat-label">信誉评分</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-chip">
          <span class="stat-val">0</span>
          <span class="stat-label">互助次数</span>
        </div>
      </div>

      <!-- Feature section -->
      <div class="section">
        <h3 class="section-title">功能入口</h3>
        <div class="feature-grid">
          <div v-for="feat in features" :key="feat.name"
               class="feat-card"
               :style="{ '--fc-bg': feat.bg, '--fc-ic': feat.ic }"
               role="button" :aria-label="feat.name"
               @click="feat.type === 'browse' ? goBrowse() : goPublish(feat.type)">
            <div class="feat-icon-wrap">
              <van-icon :name="feat.icon" class="feat-icon" />
            </div>
            <span class="feat-name">{{ feat.name }}</span>
            <span class="feat-desc">{{ feat.desc }}</span>
          </div>
        </div>
      </div>

      <!-- My orders -->
      <div class="section">
        <div class="orders-card" @click="router.push('/orders')" role="button">
          <div class="orders-card-left">
            <div class="orders-icon-wrap"><van-icon name="orders-o" /></div>
            <div>
              <p class="orders-title">我的订单</p>
              <p class="orders-sub">查看我发布和接取的需求</p>
            </div>
          </div>
          <van-icon name="arrow" class="orders-arrow" />
        </div>
      </div>

      <!-- Admin entry -->
      <div v-if="authStore.isAdmin" class="section">
        <div class="admin-card" @click="router.push('/admin/users')" role="button">
          <div class="admin-card-left">
            <div class="admin-icon-wrap"><van-icon name="setting-o" /></div>
            <div>
              <p class="admin-title">管理后台</p>
              <p class="admin-sub">用户管理 · 数据监控</p>
            </div>
          </div>
          <van-icon name="arrow" class="admin-arrow" />
        </div>
      </div>

      <!-- Logout -->
      <div class="logout-section">
        <van-button type="default" block round class="logout-btn" @click="handleLogout">
          退出登录
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import NavActions from '@/components/NavActions.vue'

const router = useRouter()
const authStore = useAuthStore()

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6)  return '夜深了'
  if (h < 12) return '早上好 ☀️'
  if (h < 14) return '中午好 🌤'
  if (h < 18) return '下午好 🌈'
  return '晚上好 🌙'
})

const features = [
  { name: '跑腿代取', desc: '发布取件需求', icon: 'logistics',    bg: 'var(--feat-1-bg)', ic: 'var(--feat-1-ic)', type: 'errand' },
  { name: '二手交易', desc: '发布交易信息', icon: 'shop',        bg: 'var(--feat-2-bg)', ic: 'var(--feat-2-ic)', type: 'trade' },
  { name: '组队匹配', desc: '发布组队需求', icon: 'friends',      bg: 'var(--feat-3-bg)', ic: 'var(--feat-3-ic)', type: 'team' },
  { name: '失物招领', desc: '发布招领信息', icon: 'search',      bg: 'var(--feat-4-bg)', ic: 'var(--feat-4-ic)', type: 'lost_found' },
  { name: '学习互助', desc: '发布学习需求', icon: 'chat-o',       bg: 'var(--feat-5-bg)', ic: 'var(--feat-5-ic)', type: 'study' },
  { name: '需求广场', desc: '浏览所有需求', icon: 'gem-o',       bg: 'var(--feat-5-bg)', ic: 'var(--feat-5-ic)', type: 'browse' },
]

function goPublish(type) {
  router.push({ path: '/demands/publish', query: { type } })
}

function goBrowse() {
  router.push('/demands')
}

function handleLogout() {
  authStore.logout()
  showToast('已退出')
  router.push('/login')
}
</script>

<style scoped>
.home-page { background: var(--c-bg); }

/* ── Nav ── */
.home-nav :deep(.van-nav-bar),
.home-nav :deep(.van-nav-bar__content) { background: var(--g-hero) !important; }

/* ── Hero ── */
.hero-banner {
  position: relative;
  background: var(--g-hero);
  padding: 20px 24px 52px;
  overflow: hidden;
}

.hero-content { position: relative; z-index: 2; color: #fff; }

.hero-greeting { font-size: 13px; color: rgba(255,255,255,0.75); margin-bottom: 4px; }
.hero-name { font-size: 24px; font-weight: 700; margin-bottom: 4px; }
.hero-tip { font-size: 13px; color: rgba(255,255,255,0.65); }

.hero-deco { position: absolute; inset: 0; pointer-events: none; z-index: 1; }
.deco-ring { position: absolute; border: 2px solid rgba(255,255,255,0.10); border-radius: 50%; }
.deco-ring-1 { width: 220px; height: 220px; top: -80px; right: -60px; }
.deco-ring-2 { width: 140px; height: 140px; top: -20px; right: 30px; }

/* ── Stats ── */
.stats-row {
  display: flex; align-items: center;
  background: var(--c-surface);
  border-radius: var(--r-lg);
  margin: -28px 0 0;
  padding: 16px 0;
  position: relative; z-index: 10;
  box-shadow: var(--s-md);
}
.stat-chip { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 3px; }
.stat-val { font-size: 18px; font-weight: 700; color: var(--c-primary); }
.stat-label { font-size: 11px; color: var(--c-text-3); }
.stat-divider { width: 1px; height: 28px; background: var(--c-border); }

/* ── Sections ── */
.section { padding-top: 28px; }
.section-title { font-size: 15px; font-weight: 700; color: var(--c-text-1); margin-bottom: 14px; }

/* ── Feature grid ── */
.feature-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.feat-card {
  background: var(--fc-bg);
  border-radius: var(--r-md);
  padding: 18px 16px;
  display: flex; flex-direction: column; gap: 6px;
  cursor: pointer;
  transition: transform var(--ease), box-shadow var(--ease);
}
.feat-card:active { transform: scale(0.97); box-shadow: var(--s-sm); }

.feat-icon-wrap {
  width: 44px; height: 44px; border-radius: 14px;
  background: var(--fc-ic);
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 6px;
}
.feat-icon { font-size: 22px; color: #fff !important; }
.feat-name { font-size: 15px; font-weight: 600; color: var(--c-text-1); }
.feat-desc { font-size: 12px; color: var(--c-text-3); }

/* ── Orders card ── */
.orders-card {
  display: flex; align-items: center; justify-content: space-between;
  background: #EDF4FF;
  border: 1.5px solid #BFDBFE;
  border-radius: var(--r-md);
  padding: 16px;
  cursor: pointer;
  transition: background var(--ease);
}
.orders-card:active { background: #DBEAFE; }
.orders-card-left { display: flex; align-items: center; gap: 14px; }
.orders-icon-wrap {
  width: 44px; height: 44px; border-radius: 14px;
  background: #3B82F6;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: #fff;
}
.orders-title { font-size: 15px; font-weight: 600; color: var(--c-text-1); margin-bottom: 2px; }
.orders-sub { font-size: 12px; color: var(--c-text-3); }
.orders-arrow { color: var(--c-text-3); font-size: 16px; }

/* ── Admin card ── */
.admin-card {
  display: flex; align-items: center; justify-content: space-between;
  background: #FFF8EC;
  border: 1.5px solid #FFE5A3;
  border-radius: var(--r-md);
  padding: 16px;
  cursor: pointer;
  transition: background var(--ease);
}
.admin-card:active { background: #FFF0CC; }
.admin-card-left { display: flex; align-items: center; gap: 14px; }
.admin-icon-wrap {
  width: 44px; height: 44px; border-radius: 14px;
  background: #EAB308;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; color: #fff;
}
.admin-title { font-size: 15px; font-weight: 600; color: var(--c-text-1); margin-bottom: 2px; }
.admin-sub { font-size: 12px; color: var(--c-text-3); }
.admin-arrow { color: var(--c-text-3); font-size: 16px; }

/* ── Logout ── */
.logout-section { padding-top: 28px; }
.logout-btn {
  color: var(--c-text-2) !important; border-color: var(--c-border) !important;
  background: var(--c-surface) !important; box-shadow: none !important;
  height: 48px !important; font-size: 14px !important;
}

/* ════════════════════════════════════════
   Tablet (≥ 768px) — 3-col grid, wider stats
   ════════════════════════════════════════ */
@media (min-width: 768px) {
  .hero-banner {
    padding: 32px var(--content-pad, 32px) 60px;
  }
  .hero-name { font-size: 28px; }

  .feature-grid { grid-template-columns: repeat(3, 1fr); gap: 14px; }

  .stats-row { margin: -28px 0 0; padding: 20px 0; }
  .stat-val { font-size: 22px; }

  .logout-section { max-width: 400px; margin: 0 auto; }
}

/* ════════════════════════════════════════
   Desktop (≥ 1024px) — 4-col grid, hover effects
   ════════════════════════════════════════ */
@media (min-width: 1024px) {
  .hero-banner {
    padding: 40px 48px 80px;
  }
  .hero-name { font-size: 32px; }
  .deco-ring-1 { width: 360px; height: 360px; top: -140px; right: -100px; }
  .deco-ring-2 { width: 220px; height: 220px; top: -30px; right: 80px; }

  .feature-grid { grid-template-columns: repeat(4, 1fr); gap: 16px; }

  .feat-card:hover {
    transform: translateY(-4px);
    box-shadow: var(--s-md);
  }

	  .orders-card:hover { background: #DBEAFE; }
  .admin-card:hover { background: #FFF0CC; }
}
</style>
