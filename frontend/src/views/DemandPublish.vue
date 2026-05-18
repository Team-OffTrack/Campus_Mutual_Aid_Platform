<template>
  <div class="page publish-page">
    <van-nav-bar title="发布需求" left-arrow fixed placeholder
      class="publish-nav" @click-left="router.back()" />

    <div class="content-wrap">
      <div class="publish-card">
        <van-form @submit="handlePublish" class="publish-form">
          <!-- Type selector as a grid of chips -->
          <div class="form-section">
            <label class="section-label">需求类型</label>
            <div class="type-grid">
              <div v-for="t in demandTypes" :key="t.value"
                class="type-chip"
                :class="{ 'chip-active': form.type === t.value }"
                :style="form.type === t.value ? { background: t.color, borderColor: t.color, color: '#fff' } : {}"
                @click="form.type = t.value">
                <van-icon :name="t.icon" size="18" />
                <span>{{ t.label }}</span>
              </div>
            </div>
          </div>

          <div class="field-row" :class="{ focused: focusedField === 'title' }">
            <van-icon name="label-o" class="field-icon" />
            <van-field v-model="form.title" placeholder="标题（一句话描述需求）"
              :rules="[{ required: true, message: '请输入标题' }]"
              class="bare-field"
              @focus="focusedField = 'title'" @blur="focusedField = null" />
          </div>

          <div class="field-row area-row" :class="{ focused: focusedField === 'desc' }">
            <van-icon name="edit" class="field-icon" />
            <van-field v-model="form.description" type="textarea" rows="3" autosize
              placeholder="详细描述你的需求…"
              :rules="[{ required: true, message: '请输入描述' }]"
              class="bare-field"
              @focus="focusedField = 'desc'" @blur="focusedField = null" />
          </div>

          <!-- Image upload -->
          <div class="form-section">
            <label class="section-label">添加图片（选填）</label>
            <van-uploader
              v-model="imageFiles"
              :max-count="9"
              :max-size="5 * 1024 * 1024"
              accept="image/*"
              :before-read="beforeReadImage"
              :after-read="afterReadImage"
              @oversize="showToast('图片不能超过5MB')"
            />
          </div>

          <div class="field-row" :class="{ focused: focusedField === 'location' }">
            <van-icon name="location-o" class="field-icon" />
            <van-field v-model="form.location" placeholder="地点（选填）"
              class="bare-field"
              @focus="focusedField = 'location'" @blur="focusedField = null" />
          </div>

          <!-- Reward settings -->
          <div class="form-row-split">
            <div class="field-row split-field" :class="{ focused: focusedField === 'rewardType' }">
              <van-icon name="gold-coin-o" class="field-icon" />
              <select v-model="form.rewardType" class="bare-select"
                @focus="focusedField = 'rewardType'" @blur="focusedField = null">
                <option value="point">积分</option>
                <option value="cash">现金</option>
                <option value="donation">公益</option>
              </select>
            </div>
            <div class="field-row split-field" :class="{ focused: focusedField === 'rewardAmount' }">
              <van-field v-model.number="form.rewardAmount" type="number"
                placeholder="报酬数量" class="bare-field"
                @focus="focusedField = 'rewardAmount'" @blur="focusedField = null" />
            </div>
          </div>

          <!-- Anonymous toggle -->
          <div class="anon-row">
            <div class="anon-left">
              <van-icon name="eye-o" size="18" />
              <span class="anon-label">匿名发布</span>
              <span class="anon-hint">其他人看不到你的真实姓名</span>
            </div>
            <van-switch v-model="form.isAnonymous" size="22px" />
          </div>

          <van-button block native-type="submit" :loading="loading" class="submit-btn" round>
            发布需求
          </van-button>
        </van-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { publishDemand, uploadDemandImage } from '@/api/demand'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const focusedField = ref(null)
const imageFiles = ref([])
const uploadedImageUrls = ref([])

// Pre-select type from query param (e.g. /demands/publish?type=errand)
const initialType = route.query.type
const validTypes = ['errand', 'trade', 'team', 'lost_found', 'study', 'other']
const form = reactive({
  type: validTypes.includes(initialType) ? initialType : 'errand',
  title: '',
  description: '',
  location: '',
  rewardType: 'point',
  rewardAmount: 0,
  isAnonymous: false
})

const demandTypes = [
  { value: 'errand', label: '跑腿代取', icon: 'logistics', color: '#FF7849' },
  { value: 'trade', label: '二手交易', icon: 'shop', color: '#22C55E' },
  { value: 'team', label: '组队匹配', icon: 'friends', color: '#3B82F6' },
  { value: 'lost_found', label: '失物招领', icon: 'search', color: '#A855F7' },
  { value: 'study', label: '学习互助', icon: 'bookmark-o', color: '#EC4899' },
  { value: 'other', label: '其他', icon: 'ellipsis', color: '#94A3C8' }
]

function beforeReadImage(file) {
  if (!file.type.startsWith('image/')) {
    showToast('只支持图片文件')
    return false
  }
  return true
}

async function afterReadImage(item) {
  try {
    const url = await uploadDemandImage(item.file)
    uploadedImageUrls.value.push(url)
  } catch {
    // Remove the failed file from the uploader list
    imageFiles.value = imageFiles.value.filter(f => f !== item)
    /* toast handled by interceptor */
  }
}

async function handlePublish() {
  loading.value = true
  try {
    const payload = { ...form }
    if (!payload.location) payload.location = null
    if (!payload.rewardAmount) payload.rewardAmount = 0
    if (uploadedImageUrls.value.length > 0) {
      payload.images = uploadedImageUrls.value.join(',')
    }
    await publishDemand(payload)
    showToast('发布成功')
    router.push('/demands')
  } catch (e) { /* toast handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.publish-page { background: var(--c-bg); padding-bottom: 32px; }
.publish-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }

.content-wrap { padding: 16px; }
.publish-card {
  background: var(--c-surface);
  border-radius: var(--r-lg);
  padding: 24px 20px;
  box-shadow: var(--s-sm);
}

.publish-form { display: flex; flex-direction: column; gap: 16px; }

/* Type grid */
.form-section { display: flex; flex-direction: column; gap: 8px; }
.section-label { font-size: 14px; font-weight: 600; color: var(--c-text-1); }
.type-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }

.type-chip {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 10px 8px;
  border-radius: var(--r-sm);
  border: 1.5px solid var(--c-border);
  font-size: 13px; font-weight: 600; color: var(--c-text-2);
  background: var(--c-bg);
  cursor: pointer;
  transition: all var(--ease);
}
.type-chip:active { transform: scale(0.96); }
.chip-active { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }

/* Field rows */
.field-row {
  display: flex; align-items: center;
  background: var(--c-bg);
  border: 1.5px solid var(--c-border);
  border-radius: var(--r-md);
  padding: 0 14px;
  min-height: 52px;
  transition: border-color var(--ease), box-shadow var(--ease);
}
.field-row.focused {
  border-color: var(--c-primary);
  box-shadow: 0 0 0 3px rgba(92,107,248,0.12);
  background: #fff;
}
.area-row { align-items: flex-start; padding-top: 12px; }
.field-icon {
  color: var(--c-text-3); font-size: 18px; margin-right: 10px; flex-shrink: 0;
  transition: color var(--ease);
}
.field-row.focused .field-icon { color: var(--c-primary); }

.bare-field { flex: 1; }
.bare-field :deep(.van-cell) {
  padding: 0 !important; background: transparent !important;
  min-height: 46px; display: flex; align-items: center;
}
.bare-field :deep(.van-field__body) { padding: 0; }
.bare-field :deep(.van-field__control) { font-size: 15px; color: var(--c-text-1); }

/* Reward split */
.form-row-split { display: flex; gap: 10px; }
.split-field { flex: 1; }
.bare-select {
  flex: 1; border: none; background: transparent;
  font-size: 15px; color: var(--c-text-1); outline: none;
  padding: 14px 0; cursor: pointer;
}

/* Anonymous row */
.anon-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 0;
}
.anon-left { display: flex; align-items: center; gap: 8px; color: var(--c-text-3); }
.anon-label { font-size: 14px; color: var(--c-text-1); font-weight: 600; }
.anon-hint { font-size: 12px; color: var(--c-text-3); }

/* Uploader */
.form-section :deep(.van-uploader__upload) {
  width: 80px; height: 80px; border-radius: var(--r-md);
}
.form-section :deep(.van-uploader__preview-image) {
  width: 80px; height: 80px; border-radius: var(--r-md);
}

.submit-btn { height: 48px; font-size: 16px; margin-top: 4px; }

@media (min-width: 768px) {
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 24px 32px; }
  .publish-card { max-width: 640px; margin: 0 auto; padding: 32px; }
  .type-grid { grid-template-columns: repeat(6, 1fr); }
}
</style>
