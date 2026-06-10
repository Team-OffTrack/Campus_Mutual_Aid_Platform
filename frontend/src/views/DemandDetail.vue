<template>
  <div class="page detail-page">
    <van-nav-bar left-arrow fixed placeholder class="detail-nav" @click-left="router.back()">
      <template #title><span class="nav-title">需求详情</span></template>
      <template #right><NavActions /></template>
    </van-nav-bar>

    <div v-if="demand" class="content-wrap">
      <!-- ═══ Header card ═══ -->
      <div class="detail-card card card-elevated">
        <div class="card-header">
          <span class="d-type" :style="typeStyle(demand.type)">{{ typeLabel(demand.type) }}</span>
          <span class="d-status" :class="statusClass(demand.status)">{{ statusLabel(demand.status) }}</span>
        </div>

        <h2 class="d-title">{{ demand.title }}</h2>

        <div class="d-publisher">
          <div class="pub-avatar" :style="{ background: demand.publisherAvatar ? 'transparent' : avatarColor(demand.publisherName) }">
            <img v-if="demand.publisherAvatar" :src="demand.publisherAvatar" class="avatar-img" />
            <span v-else>{{ demand.publisherName.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="pub-info">
            <span class="pub-name">
              <van-icon v-if="demand.isAnonymous" name="eye-o" size="14" />
              {{ demand.publisherName }}
            </span>
            <span class="pub-time">发布于 {{ formatTime(demand.createTime) }}</span>
          </div>
        </div>

        <div class="d-desc">{{ demand.description }}</div>

        <!-- Image gallery -->
        <div v-if="demandImages.length > 0" class="d-images">
          <img v-for="(url, idx) in demandImages" :key="idx"
            :src="url" class="d-image-thumb" @click="previewImage(idx)" />
        </div>

        <!-- Meta grid -->
        <div class="d-meta-grid">
          <div class="meta-item" v-if="demand.location">
            <van-icon name="location-o" size="20" />
            <div>
              <span class="meta-label">地点</span>
              <span class="meta-val">{{ demand.location }}</span>
            </div>
          </div>
          <div class="meta-item" v-if="demand.type !== 'lost_found' || (demand.rewardAmount && demand.rewardAmount > 0)">
            <van-icon name="gold-coin-o" size="20" />
            <div>
              <span class="meta-label">{{ rewardLabel(demand) }}</span>
              <span class="meta-val reward">{{ rewardText(demand) }}</span>
            </div>
          </div>
          <div class="meta-item" v-if="demand.deadline">
            <van-icon name="clock-o" size="20" />
            <div>
              <span class="meta-label">截止时间</span>
              <span class="meta-val">{{ formatTime(demand.deadline) }}</span>
            </div>
          </div>
        </div>

        <!-- Type-specific attributes -->
        <div v-if="demand.attributes && Object.keys(demand.attributes).length > 0" class="d-attrs-section">
          <div class="attrs-divider"></div>
          <!-- errand -->
          <template v-if="demand.type === 'errand'">
            <div class="attr-row"><span class="attr-label">取件地点</span><span class="attr-val">{{ demand.attributes.pickup_location || '未指定' }}</span></div>
            <div class="attr-row"><span class="attr-label">物品类别</span><span class="attr-val">{{ demand.attributes.item_category || '未指定' }}</span></div>
            <div class="attr-row"><span class="attr-label">紧急程度</span><span class="attr-val" :class="demand.attributes.urgency === 'urgent' ? 'urgency-urgent' : ''">{{ demand.attributes.urgency === 'urgent' ? '🏃 加急' : '普通' }}</span></div>
          </template>
          <!-- trade -->
          <template v-if="demand.type === 'trade'">
            <div class="attr-row"><span class="attr-label">商品成色</span><span class="attr-val">{{ demand.attributes.item_condition || '未指定' }}</span></div>
            <div class="attr-row"><span class="attr-label">商品类别</span><span class="attr-val">{{ demand.attributes.trade_category || '未指定' }}</span></div>
          </template>
          <!-- lost_found -->
          <template v-if="demand.type === 'lost_found'">
            <div class="attr-row"><span class="attr-label">类型</span><span class="attr-val lf-badge" :class="demand.attributes.lf_type === 'LOST' ? 'lf-lost' : 'lf-found'">{{ demand.attributes.lf_type === 'LOST' ? '🔍 寻物' : '📦 招领' }}</span></div>
            <div class="attr-row"><span class="attr-label">物品类别</span><span class="attr-val">{{ demand.attributes.item_category || '未指定' }}</span></div>
            <div class="attr-row" v-if="demand.attributes.lost_found_date"><span class="attr-label">日期</span><span class="attr-val">{{ demand.attributes.lost_found_date }}</span></div>
          </template>
          <!-- study -->
          <template v-if="demand.type === 'study'">
            <div class="attr-row"><span class="attr-label">科目</span><span class="attr-val">{{ demand.attributes.subject || '未指定' }}</span></div>
            <div class="attr-row"><span class="attr-label">方式</span><span class="attr-val">{{ demand.attributes.study_mode || '未指定' }}</span></div>
            <div class="attr-row"><span class="attr-label">难度</span><span class="attr-val">{{ demand.attributes.difficulty || '未指定' }}</span></div>
            <div class="attr-row" v-if="demand.attributes.preferred_time"><span class="attr-label">期望时间</span><span class="attr-val">{{ demand.attributes.preferred_time }}</span></div>
          </template>
        </div>
      </div>

      <!-- ═══ Acceptor info ═══ -->
      <div class="detail-card card" v-if="demand.acceptorId">
        <h3 class="card-subtitle">接单人</h3>
        <div class="d-publisher">
          <div class="pub-avatar" :style="{ background: demand.acceptorAvatar ? 'transparent' : avatarColor(demand.acceptorName || '?') }">
            <img v-if="demand.acceptorAvatar" :src="demand.acceptorAvatar" class="avatar-img" />
            <span v-else>{{ (demand.acceptorName || '?').charAt(0).toUpperCase() }}</span>
          </div>
          <div class="pub-info">
            <span class="pub-name">{{ demand.acceptorName }}</span>
            <span class="pub-time">已接单</span>
          </div>
        </div>
      </div>

      <!-- ═══ Action area ═══ -->
      <div class="action-section">
        <template v-if="demand.status === 'OPEN'">
          <van-button v-if="isOwner" block round type="danger"
            :loading="acting" class="action-btn" @click="handleCancel">
            取消需求
          </van-button>
          <van-button v-else block round type="primary"
            :loading="acting" class="action-btn" @click="handleAccept">
            我要接单
          </van-button>
        </template>

        <template v-if="demand.status === 'IN_PROGRESS'">
          <div v-if="isOwner" class="action-row">
            <van-button block round type="primary" class="action-btn"
              :loading="acting" @click="handleComplete">
              确认完成
            </van-button>
            <van-button block round type="default" class="cancel-btn-secondary action-btn"
              :loading="acting" @click="handleCancel">
              取消需求
            </van-button>
          </div>
          <div v-else-if="isAcceptor" class="action-hint">
            <van-icon name="clock-o" size="18" />
            <span>等待发布者确认完成…</span>
          </div>
        </template>

        <div v-if="demand.status === 'COMPLETED'" class="action-hint done">
          <van-icon name="success" size="18" />
          <span>该需求已完成</span>
        </div>
        <div v-if="demand.status === 'CANCELLED'" class="action-hint cancelled">
          <van-icon name="close" size="18" />
          <span>该需求已取消</span>
        </div>
      </div>

      <!-- ═══ Chat entry ═══ -->
      <div v-if="chatTargetId" class="action-section chat-section">
        <van-button block round plain type="primary" class="action-btn"
          :loading="startingChat" @click="handleStartChat">
          <van-icon name="chat-o" size="16" style="margin-right:6px" />
          {{ chatLabel }}
        </van-button>
      </div>

      <!-- ═══ Evaluation section ═══ -->
      <div v-if="demand.status === 'COMPLETED' || demand.status === 'CANCELLED'" class="detail-card card eval-section">
        <h3 class="card-subtitle">评价</h3>

        <div v-if="evaluations.length > 0" class="eval-list">
          <div v-for="e in evaluations" :key="e.evaluationId" class="eval-comment-card">
            <div class="eval-comment-avatar" :style="{ background: e.evaluatorAvatar ? 'transparent' : avatarColor(e.evaluatorName) }">
              <img v-if="e.evaluatorAvatar" :src="e.evaluatorAvatar" class="avatar-img" />
              <span v-else>{{ e.evaluatorName.charAt(0).toUpperCase() }}</span>
            </div>
            <div class="eval-comment-body">
              <div class="eval-comment-header">
                <span class="eval-comment-author">{{ e.evaluatorName }}</span>
                <span class="eval-comment-stars">{{ '★'.repeat(e.rating) }}{{ '☆'.repeat(5 - e.rating) }}</span>
              </div>
              <p v-if="e.comment" class="eval-comment-text">{{ e.comment }}</p>
              <span class="eval-comment-time">{{ timeAgo(e.createTime) }}</span>
            </div>
          </div>
        </div>
        <div v-else class="eval-empty">暂无评价</div>

        <!-- My evaluation form -->
        <div v-if="isParticipant && demand.status === 'COMPLETED'" class="eval-form-area">
          <div class="eval-divider"></div>

          <template v-if="!myEval">
            <h4 class="eval-form-title">提交评价</h4>
            <div class="star-row">
              <van-icon v-for="i in 5" :key="i"
                :name="i <= ratingValue ? 'star' : 'star-o'"
                :color="i <= ratingValue ? '#EAB308' : '#D1D5DB'"
                size="30" @click="ratingValue = i" />
            </div>
            <van-field v-model="ratingComment" type="textarea" rows="2"
              placeholder="说点什么…（选填）" class="eval-input" />
            <van-button block round type="primary" size="small"
              :loading="submittingEval" @click="submitEvaluation">
              提交评价
            </van-button>
          </template>

          <template v-else>
            <div class="my-eval-banner">
              <span class="my-eval-label">我的评价</span>
              <span class="my-eval-rating">{{ '★'.repeat(myEval.rating) }}{{ '☆'.repeat(5 - myEval.rating) }}</span>
            </div>
            <p v-if="myEval.comment" class="my-eval-comment">{{ myEval.comment }}</p>

            <template v-if="editingMyEval">
              <div class="star-row">
                <van-icon v-for="i in 5" :key="i"
                  :name="i <= ratingValue ? 'star' : 'star-o'"
                  :color="i <= ratingValue ? '#EAB308' : '#D1D5DB'"
                  size="30" @click="ratingValue = i" />
              </div>
              <van-field v-model="ratingComment" type="textarea" rows="2"
                placeholder="说点什么…（选填）" class="eval-input" />
              <div class="eval-edit-actions">
                <van-button round size="small" @click="editingMyEval = false">取消</van-button>
                <van-button round type="primary" size="small"
                  :loading="submittingEval" @click="submitUpdateEvaluation">
                  保存修改
                </van-button>
              </div>
            </template>
            <van-button v-else block round plain type="primary" size="small"
              @click="startEditMyEval">
              修改评价
            </van-button>
          </template>
        </div>
      </div>
    </div>

    <!-- States -->
    <div v-else-if="error" class="error-state">
      <van-icon name="failure" size="48" color="#EF4444" />
      <p>{{ error }}</p>
      <van-button round type="primary" @click="fetchDetail">重试</van-button>
    </div>
    <div v-else class="loading-state">
      <van-loading size="32" color="var(--c-primary)" />
      <p>加载中…</p>
    </div>

    <ImageViewer v-model:show="showPreview" :images="demandImages" :start-position="previewIndex" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import ImageViewer from '@/components/ImageViewer.vue'
import { getDemand, cancelDemand, acceptDemand, completeDemand } from '@/api/demand'
import { getEvaluationsByDemand, getMyEvaluation, createEvaluation, updateEvaluation } from '@/api/evaluation'
import { createConversation } from '@/api/chat'
import NavActions from '@/components/NavActions.vue'
import { useAuthStore } from '@/stores/auth'
import { TYPE_LABELS, TYPE_STYLES } from '@/constants/demandTypes'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const demand = ref(null)
const error = ref(null)
const acting = ref(false)
const evaluations = ref([])
const myEval = ref(null)
const ratingValue = ref(5)
const ratingComment = ref('')
const submittingEval = ref(false)
const editingMyEval = ref(false)
const showPreview = ref(false)
const previewIndex = ref(0)
const startingChat = ref(false)

const chatTargetId = computed(() => {
  if (!demand.value) return null
  if (!isOwner.value) return demand.value.publisherId
  if (demand.value.acceptorId) return demand.value.acceptorId
  return null
})

const chatLabel = computed(() => {
  if (!demand.value) return ''
  if (!isOwner.value) return '私信发布者'
  return '私信接单人'
})

const userId = computed(() => Number(authStore.userId))
const isOwner = computed(() => demand.value && userId.value === demand.value.publisherId)
const isAcceptor = computed(() => demand.value && userId.value === demand.value.acceptorId)
const isParticipant = computed(() => isOwner.value || isAcceptor.value)
const demandImages = computed(() => {
  if (!demand.value?.images) return []
  return demand.value.images.split(',').filter(Boolean)
})

function previewImage(index) { previewIndex.value = index; showPreview.value = true }

const STATUS_LABELS = { OPEN: '进行中', IN_PROGRESS: '已接单', COMPLETED: '已完成', CANCELLED: '已取消' }

function typeLabel(v) { return TYPE_LABELS[v] || v }
function typeStyle(v) { return TYPE_STYLES[v] || TYPE_STYLES.other }
function statusLabel(v) { return STATUS_LABELS[v] || v }
function statusClass(v) {
  if (v === 'OPEN' || v === 'IN_PROGRESS') return 's-active'
  if (v === 'COMPLETED') return 's-done'
  return 's-cancelled'
}

function rewardText(d) {
  if (!d.rewardAmount || d.rewardAmount === 0) return '免费'
  if (d.type === 'trade') return '¥' + d.rewardAmount
  if (d.rewardType === 'cash') return '¥' + d.rewardAmount
  if (d.rewardType === 'point') return d.rewardAmount + ' 积分'
  return '公益'
}

function rewardLabel(d) {
  if (d.type === 'trade') return '价格'
  if (d.type === 'lost_found') return '感谢金'
  return '报酬'
}

function formatTime(t) {
  if (!t) return ''
  const d = new Date(t)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

function timeAgo(t) {
  if (!t) return ''
  const diff = Date.now() - new Date(t).getTime()
  const mins = Math.floor(diff / 60000)
  if (mins < 1) return '刚刚'
  if (mins < 60) return mins + ' 分钟前'
  const hours = Math.floor(mins / 60)
  if (hours < 24) return hours + ' 小时前'
  const days = Math.floor(hours / 24)
  if (days < 30) return days + ' 天前'
  return formatTime(t)
}

const AVATAR_COLORS = ['#6750A4','#0097A7','#2E7D32','#D32F2F','#7B1FA2','#C62828','#ED6C02','#E65100']
function avatarColor(n) { return AVATAR_COLORS[(n || '?').charCodeAt(0) % AVATAR_COLORS.length] }

async function fetchDetail() {
  try {
    const id = route.params.demandId
    demand.value = await getDemand(id)
    error.value = null
    await fetchEvaluations()
  } catch { error.value = '加载失败，请重试' }
}

async function fetchEvaluations() {
  try {
    const id = route.params.demandId
    evaluations.value = await getEvaluationsByDemand(id)
    if (isParticipant.value) {
      myEval.value = await getMyEvaluation(id)
    }
  } catch { /* non-critical */ }
}

function startEditMyEval() {
  if (myEval.value) {
    ratingValue.value = myEval.value.rating
    ratingComment.value = myEval.value.comment || ''
  }
  editingMyEval.value = true
}

async function submitEvaluation() {
  if (submittingEval.value) return
  submittingEval.value = true
  try {
    const result = await createEvaluation({
      demandId: demand.value.demandId,
      rating: ratingValue.value,
      comment: ratingComment.value || null
    })
    showToast('评价成功')
    myEval.value = result
    evaluations.value = [...evaluations.value, result]
    ratingValue.value = 5; ratingComment.value = ''
    fetchEvaluations()
  } catch (e) { showToast(e.message || '提交评价失败，请重试') }
  finally { submittingEval.value = false }
}

async function submitUpdateEvaluation() {
  if (submittingEval.value || !myEval.value) return
  submittingEval.value = true
  try {
    const result = await updateEvaluation(myEval.value.evaluationId, {
      demandId: demand.value.demandId,
      rating: ratingValue.value,
      comment: ratingComment.value || null
    })
    showToast('评价已更新')
    myEval.value = result
    evaluations.value = evaluations.value.map(e => e.evaluationId === result.evaluationId ? result : e)
    editingMyEval.value = false
    ratingValue.value = 5; ratingComment.value = ''
    fetchEvaluations()
  } catch (e) { showToast(e.message || '修改失败，请重试') }
  finally { submittingEval.value = false }
}

async function handleStartChat() {
  if (startingChat.value || !chatTargetId.value) return
  startingChat.value = true
  try {
    const conv = await createConversation(demand.value.demandId, chatTargetId.value)
    const targetName = isOwner.value ? demand.value.acceptorName : demand.value.publisherName
    const targetAvatar = isOwner.value ? demand.value.acceptorAvatar : demand.value.publisherAvatar
    router.push('/chat/' + conv.conversationId + '?name=' + encodeURIComponent(targetName || '') + '&avatar=' + encodeURIComponent(targetAvatar || ''))
  } catch (e) { showToast(e.message || '创建会话失败，请重试') }
  finally { startingChat.value = false }
}

async function handleCancel() {
  try { await showConfirmDialog({ title: '确认取消', message: '取消后不可恢复，确定要取消这个需求吗？' }) }
  catch { return }
  acting.value = true
  try { await cancelDemand(demand.value.demandId); demand.value.status = 'CANCELLED'; showToast('已取消') }
  catch (e) { showToast(e.message || '取消失败，请重试') }
  finally { acting.value = false }
}

async function handleAccept() {
  try { await showConfirmDialog({ title: '确认接单', message: '接单后请按时完成需求，确定要接单吗？' }) }
  catch { return }
  acting.value = true
  try { demand.value = await acceptDemand(demand.value.demandId); showToast('接单成功') }
  catch (e) { showToast(e.message || '接单失败，请重试') }
  finally { acting.value = false }
}

async function handleComplete() {
  try { await showConfirmDialog({ title: '确认完成', message: '确认需求已完成？完成后将不可撤销。' }) }
  catch { return }
  acting.value = true
  try { demand.value = await completeDemand(demand.value.demandId); showToast('已完成'); await fetchEvaluations() }
  catch (e) { showToast(e.message || '操作失败，请重试') }
  finally { acting.value = false }
}

onMounted(fetchDetail)
</script>

<style scoped>
.detail-page { background: var(--c-bg); padding-bottom: 32px; }
.detail-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.content-wrap { padding: 16px; display: flex; flex-direction: column; gap: 16px; }

/* ═══════════════════════════════════════
   Detail card
   ═══════════════════════════════════════ */
.detail-card {
  padding: 24px 20px;
  display: flex; flex-direction: column; gap: 16px;
  border-radius: var(--r-extra-large);
}

.card-header { display: flex; justify-content: space-between; align-items: center; }

.d-type { font-size: 12px; font-weight: 700; padding: 4px 12px; border-radius: 6px; letter-spacing: 0.3px; }
.d-status { font-size: 12px; font-weight: 600; padding: 4px 12px; border-radius: 6px; }
.s-active { background: #E8F5E9; color: #2E7D32; }
.s-done { background: #E3F2FD; color: #1565C0; }
.s-cancelled { background: #F5F5F5; color: #9E9E9E; }

.d-title { font: var(--t-title); color: var(--c-text-1); line-height: 1.35; }

.d-publisher {
  display: flex; align-items: center; gap: 12px;
  padding: 14px; background: var(--c-surface-variant); border-radius: var(--r-medium);
  transition: background var(--spring-fast-spatial);
}
.pub-avatar {
  width: 42px; height: 42px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  font-size: 18px; font-weight: 700; color: #fff; flex-shrink: 0;
  overflow: hidden;
}
.pub-avatar .avatar-img { width: 100%; height: 100%; object-fit: cover; }
.pub-info { display: flex; flex-direction: column; gap: 2px; }
.pub-name { font-size: 15px; font-weight: 600; color: var(--c-text-1); display: flex; align-items: center; gap: 4px; }
.pub-time { font-size: 12px; color: var(--c-text-3); }

.d-desc { font-size: 15px; color: var(--c-text-2); line-height: 1.7; white-space: pre-wrap; }

.d-images { display: flex; flex-wrap: wrap; gap: 8px; }
.d-image-thumb {
  width: 100px; height: 100px;
  object-fit: cover; border-radius: var(--r-small);
  cursor: pointer; transition: opacity var(--spring-fast-effects), transform var(--spring-fast-spatial);
}
.d-image-thumb:active { opacity: 0.8; transform: scale(0.96); }

.d-meta-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px;
}

/* Type-specific attributes */
.d-attrs-section {
  display: flex; flex-direction: column; gap: 10px;
}
.attrs-divider {
  height: 1px; background: var(--c-border); margin: 2px 0;
}
.attr-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 6px 0;
}
.attr-label { font-size: 13px; color: var(--c-text-3); }
.attr-val { font-size: 14px; font-weight: 600; color: var(--c-text-1); }
.attr-val.urgency-urgent { color: var(--c-danger); }
.lf-badge {
  font-size: 13px; font-weight: 600; padding: 2px 10px; border-radius: 6px;
}
.lf-lost { background: #FFF0E5; color: #E65100; }
.lf-found { background: #E8F5E9; color: #2E7D32; }
.meta-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px; background: var(--c-surface-variant); border-radius: var(--r-medium);
  color: var(--c-text-3);
  transition: background var(--spring-fast-spatial);
}
.meta-item > div { display: flex; flex-direction: column; gap: 2px; }
.meta-label { font-size: 11px; color: var(--c-text-3); }
.meta-val { font-size: 14px; font-weight: 600; color: var(--c-text-1); }
.meta-val.reward { color: var(--c-warning); }

/* ═══════════════════════════════════════
   Actions
   ═══════════════════════════════════════ */
.action-btn { height: 50px !important; font-size: 16px !important; font-weight: 600 !important; }

.action-row { display: flex; flex-direction: column; gap: 10px; }
.cancel-btn-secondary {
  color: var(--c-text-2) !important; border-color: var(--c-border) !important;
  box-shadow: none !important;
}

.action-hint {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 18px; border-radius: var(--r-md);
  font-size: 14px; color: var(--c-text-3); background: var(--c-surface-variant);
}
.action-hint.done { background: #E8F5E9; color: #2E7D32; }
.action-hint.cancelled { background: #F5F5F5; color: #9E9E9E; }

.card-subtitle { font-size: 15px; font-weight: 700; color: var(--c-text-2); }

/* States */
.loading-state, .error-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 16px; padding: 80px 16px; text-align: center; color: var(--c-text-3);
}
.error-state p { font-size: 15px; }

/* ═══════════════════════════════════════
   Evaluation section
   ═══════════════════════════════════════ */
.eval-list { display: flex; flex-direction: column; gap: 14px; }
.eval-comment-card { display: flex; gap: 12px; }
.eval-comment-avatar {
  width: 38px; height: 38px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 15px; font-weight: 700; color: #fff; flex-shrink: 0;
  overflow: hidden;
}
.eval-comment-avatar .avatar-img { width: 100%; height: 100%; object-fit: cover; }
.eval-comment-body {
  flex: 1; min-width: 0;
  padding: 10px 14px; background: var(--c-surface-variant); border-radius: var(--r-md);
  display: flex; flex-direction: column; gap: 4px;
}
.eval-comment-header { display: flex; justify-content: space-between; align-items: center; }
.eval-comment-author { font-size: 13px; font-weight: 600; color: var(--c-text-1); }
.eval-comment-stars { color: #ED6C02; letter-spacing: 1px; font-size: 14px; }
.eval-comment-text { font-size: 14px; color: var(--c-text-2); line-height: 1.5; margin: 2px 0; }
.eval-comment-time { font-size: 11px; color: var(--c-text-3); }

.eval-empty { font-size: 13px; color: var(--c-text-3); text-align: center; padding: 8px; }

.eval-form-area { display: flex; flex-direction: column; gap: 12px; }
.eval-divider { height: 1px; background: var(--c-border); margin: 4px 0; }
.eval-form-title { font-size: 14px; font-weight: 600; color: var(--c-text-1); }
.star-row { display: flex; gap: 6px; justify-content: center; padding: 4px 0; }
.star-row .van-icon { cursor: pointer; transition: transform var(--spring-fast-effects); }
.star-row .van-icon:active { transform: scale(1.2); }
.eval-input { background: var(--c-surface-variant); border-radius: var(--r-sm); }

.my-eval-banner { display: flex; justify-content: space-between; align-items: center; }
.my-eval-label { font-size: 13px; font-weight: 600; color: var(--c-text-1); }
.my-eval-rating { color: #ED6C02; letter-spacing: 1px; font-size: 14px; }
.my-eval-comment { font-size: 13px; color: var(--c-text-2); line-height: 1.5; margin: 0; }

.eval-edit-actions { display: flex; gap: 10px; justify-content: flex-end; }

/* ═══════════════════════════════════════
   Desktop
   ═══════════════════════════════════════ */
@media (min-width: 768px) {
  .detail-page { background: var(--c-surface); }
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 24px 32px; }
  .detail-card { max-width: 720px; margin: 0 auto; padding: 32px; }
  .action-section { max-width: 720px; margin: 0 auto; width: 100%; }
  .d-meta-grid { grid-template-columns: repeat(3, 1fr); }
  .d-title { font-size: 26px; }
}
@media (min-width: 1024px) {
  .content-wrap { padding: 32px 48px; }
}
</style>
