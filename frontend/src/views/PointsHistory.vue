<template>
  <div class="page history-page">
    <van-nav-bar left-arrow fixed placeholder @click-left="router.back()">
      <template #title><span class="nav-title">积分明细</span></template>
    </van-nav-bar>

    <div class="content-wrap">
      <!-- Balance overview -->
      <div class="balance-card glass">
        <div class="balance-row">
          <div class="balance-item">
            <span class="balance-val primary">{{ profile.availablePoints ?? 0 }}</span>
            <span class="balance-label">可用积分</span>
          </div>
          <div class="balance-divider"></div>
          <div class="balance-item">
            <span class="balance-val warn">{{ profile.frozenPoints ?? 0 }}</span>
            <span class="balance-label">冻结积分</span>
          </div>
        </div>
      </div>

      <!-- Tab filter -->
      <van-tabs v-model:active="activeTab" sticky offset-top="46" class="tx-tabs"
        @change="onTabChange">
        <van-tab title="全部" name="" />
        <van-tab title="签到" name="DAILY_CHECKIN" />
        <van-tab title="发布冻结" name="PUBLISH" />
        <van-tab title="取消退款" name="CANCEL_REFUND" />
        <van-tab title="完成获得" name="COMPLETE_EARN" />
      </van-tabs>

      <!-- Transaction list -->
      <van-list v-model:loading="loading" :finished="finished"
        finished-text="没有更多了" @load="onLoad">
        <div v-for="tx in transactions" :key="tx.transactionId" class="tx-item card">
          <div class="tx-left">
            <div class="tx-icon-wrap" :style="{ background: typeStyle(tx.type).bg }">
              <van-icon :name="typeStyle(tx.type).icon"
                :color="typeStyle(tx.type).color" size="18" />
            </div>
            <div class="tx-info">
              <span class="tx-desc">{{ tx.description || typeLabel(tx.type) }}</span>
              <span class="tx-time">{{ formatTime(tx.createTime) }}</span>
            </div>
          </div>
          <div class="tx-right">
            <span class="tx-amount" :class="tx.amount >= 0 ? 'plus' : 'minus'">
              {{ tx.amount >= 0 ? '+' : '' }}{{ tx.amount }}
            </span>
            <span class="tx-balance">余额 {{ tx.balanceAfter }}</span>
          </div>
        </div>
      </van-list>

      <!-- Empty state -->
      <div v-if="!loading && transactions.length === 0" class="empty-state">
        <van-icon name="notes-o" size="48" color="var(--c-text-4)" />
        <p>暂无积分记录</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile } from '@/api/user'
import { getTransactions } from '@/api/points'

const router = useRouter()

const profile = reactive({ availablePoints: 0, frozenPoints: 0 })
const transactions = ref([])
const loading = ref(false)
const finished = ref(false)
const activeTab = ref('')
const page = ref(1)

const TYPE_META = {
  SIGNUP_BONUS:    { label: '注册赠送', icon: 'gift-o',       color: '#6750A4', bg: '#F3E5F5' },
  DAILY_CHECKIN:   { label: '每日签到', icon: 'clock-o',      color: '#2E7D32', bg: '#E8F5E9' },
  PUBLISH:         { label: '发布冻结', icon: 'lock',         color: '#E65100', bg: '#FFF3E0' },
  CANCEL_REFUND:   { label: '取消退款', icon: 'refund-o',     color: '#1565C0', bg: '#E3F2FD' },
  COMPLETE_EARN:   { label: '完成获得', icon: 'gold-coin-o',  color: '#2E7D32', bg: '#E8F5E9' },
  ADMIN_ADJUST:    { label: '管理员调整', icon: 'setting-o',  color: '#616161', bg: '#F5F5F5' }
}

function typeLabel(type) {
  return TYPE_META[type]?.label || type
}

function typeStyle(type) {
  return TYPE_META[type] || { icon: 'exchange', color: '#616161', bg: '#F5F5F5' }
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function onLoad() {
  loading.value = true
  try {
    const type = activeTab.value || undefined
    const result = await getTransactions({ pageNum: page.value, pageSize: 20, type })
    const records = result.records || []
    if (page.value === 1) {
      transactions.value = records
    } else {
      transactions.value.push(...records)
    }
    finished.value = records.length < 20
    if (records.length > 0) page.value++
  } catch {
    finished.value = true
  } finally {
    loading.value = false
  }
}

function onTabChange() {
  page.value = 1
  transactions.value = []
  finished.value = false
  onLoad()
}

onMounted(async () => {
  try {
    const data = await getProfile()
    profile.availablePoints = data.availablePoints ?? 0
    profile.frozenPoints = data.frozenPoints ?? 0
  } catch { /* ignore */ }
})
</script>

<style scoped>
.history-page { background: var(--c-bg); min-height: 100vh; }
.nav-title { font-weight: 600; }

.content-wrap { padding: 16px; }

/* Balance card */
.balance-card {
  padding: 20px 16px;
  margin-bottom: 12px;
}
.balance-row {
  display: flex;
  align-items: center;
}
.balance-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.balance-divider {
  width: 1px;
  height: 36px;
  background: var(--c-border);
  flex-shrink: 0;
}
.balance-val {
  font-size: 24px;
  font-weight: 700;
}
.balance-val.primary { color: var(--c-primary); }
.balance-val.warn { color: var(--c-warning); }
.balance-label {
  font-size: 12px;
  color: var(--c-text-3);
}

/* Tabs */
.tx-tabs { margin-bottom: 8px; }

/* Transaction items */
.tx-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 8px;
  border-radius: var(--r-large);
}
.tx-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}
.tx-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tx-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.tx-desc {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
}
.tx-time {
  font-size: 12px;
  color: var(--c-text-3);
}
.tx-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
  margin-left: 12px;
}
.tx-amount {
  font-size: 16px;
  font-weight: 700;
}
.tx-amount.plus { color: #2E7D32; }
.tx-amount.minus { color: #C62828; }
.tx-balance {
  font-size: 11px;
  color: var(--c-text-3);
}

/* Empty */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 64px 0;
  color: var(--c-text-3);
  font-size: 14px;
}
</style>
