<template>
  <div class="page publish-page">
    <van-nav-bar left-arrow fixed placeholder class="publish-nav" @click-left="router.back()">
      <template #title><span class="nav-title">发布需求</span></template>
    </van-nav-bar>

    <div class="content-wrap">
      <div class="publish-card card card-elevated">
        <van-form @submit="handlePublish" class="publish-form">
          <!-- Type selector -->
          <div class="form-section">
            <label class="section-label">需求类型</label>
            <div class="type-grid">
              <div v-for="t in demandTypes" :key="t.value"
                class="type-chip" :class="{ 'chip-active': form.type === t.value }"
                :style="form.type === t.value ? { background: t.color, borderColor: t.color, color: '#fff' } : {}"
                @click="form.type = t.value">
                <van-icon :name="t.icon" size="18" />
                <span>{{ t.label }}</span>
              </div>
            </div>
          </div>

          <div class="field-wrap" :class="{ focused: focusedField === 'title' }">
            <div class="field-icon-wrap"><van-icon name="label-o" /></div>
            <van-field v-model="form.title" placeholder="标题（一句话描述需求）"
              :rules="[{ required: true, message: '请输入标题' }]"
              class="bare-field"
              @focus="focusedField = 'title'" @blur="focusedField = null" />
          </div>

          <div class="field-wrap area-wrap" :class="{ focused: focusedField === 'desc' }">
            <div class="field-icon-wrap"><van-icon name="edit" /></div>
            <van-field v-model="form.description" type="textarea" rows="3" autosize
              placeholder="详细描述你的需求…"
              :rules="[{ required: true, message: '请输入描述' }]"
              class="bare-field"
              @focus="focusedField = 'desc'" @blur="focusedField = null" />
          </div>

          <!-- ═══ Type-specific fields ═══ -->

          <!-- errand -->
          <template v-if="form.type === 'errand'">
            <div class="field-wrap" :class="{ focused: focusedField === 'pickupLocation' }">
              <div class="field-icon-wrap"><van-icon name="location-o" /></div>
              <van-field v-model="attrs.errand.pickup_location" placeholder="取件地点（必填）"
                class="bare-field" :rules="[{ required: true, message: '请填写取件地点' }]"
                @focus="focusedField = 'pickupLocation'" @blur="focusedField = null" />
            </div>
            <div class="form-row-split">
              <div class="field-wrap split-field">
                <select v-model="attrs.errand.item_category" class="bare-select">
                  <option value="">物品类别</option>
                  <option value="快递">快递</option>
                  <option value="外卖">外卖</option>
                  <option value="文件">文件</option>
                  <option value="其他">其他</option>
                </select>
              </div>
              <div class="field-wrap split-field">
                <select v-model="attrs.errand.urgency" class="bare-select">
                  <option value="normal">普通</option>
                  <option value="urgent">加急 🏃</option>
                </select>
              </div>
            </div>
          </template>

          <!-- trade -->
          <template v-if="form.type === 'trade'">
            <div class="form-row-split">
              <div class="field-wrap split-field">
                <select v-model="attrs.trade.item_condition" class="bare-select">
                  <option value="">物品成色</option>
                  <option value="全新">全新</option>
                  <option value="几乎全新">几乎全新</option>
                  <option value="轻微使用">轻微使用</option>
                  <option value="明显使用">明显使用</option>
                </select>
              </div>
              <div class="field-wrap split-field">
                <van-field v-model.number="attrs.trade.item_price" type="number"
                  placeholder="价格（元）" class="bare-field" />
              </div>
            </div>
            <div class="field-wrap">
              <select v-model="attrs.trade.trade_category" class="bare-select">
                <option value="">商品类别</option>
                <option value="教材">教材</option>
                <option value="电子">电子</option>
                <option value="衣物">衣物</option>
                <option value="生活">生活</option>
                <option value="其他">其他</option>
              </select>
            </div>
          </template>

          <!-- lost_found -->
          <template v-if="form.type === 'lost_found'">
            <div class="lf-type-toggle">
              <span class="lf-toggle-chip" :class="{ active: attrs.lost_found.lf_type === 'LOST' }"
                @click="attrs.lost_found.lf_type = 'LOST'">🔍 寻物</span>
              <span class="lf-toggle-chip" :class="{ active: attrs.lost_found.lf_type === 'FOUND' }"
                @click="attrs.lost_found.lf_type = 'FOUND'">📦 招领</span>
            </div>
            <div class="form-row-split">
              <div class="field-wrap split-field">
                <select v-model="attrs.lost_found.item_category" class="bare-select">
                  <option value="">物品类别</option>
                  <option value="证件">证件</option>
                  <option value="电子">电子</option>
                  <option value="钥匙">钥匙</option>
                  <option value="衣物">衣物</option>
                  <option value="其他">其他</option>
                </select>
              </div>
              <div class="field-wrap split-field">
                <van-field v-model="attrs.lost_found.lost_found_date" type="date"
                  placeholder="日期" class="bare-field" />
              </div>
            </div>
          </template>

          <!-- study -->
          <template v-if="form.type === 'study'">
            <div class="form-row-split">
              <div class="field-wrap split-field">
                <select v-model="attrs.study.subject" class="bare-select">
                  <option value="">科目</option>
                  <option value="数学">数学</option>
                  <option value="英语">英语</option>
                  <option value="编程">编程</option>
                  <option value="物理">物理</option>
                  <option value="其他">其他</option>
                </select>
              </div>
              <div class="field-wrap split-field">
                <select v-model="attrs.study.study_mode" class="bare-select">
                  <option value="">方式</option>
                  <option value="线上">线上</option>
                  <option value="线下">线下</option>
                  <option value="均可">均可</option>
                </select>
              </div>
            </div>
            <div class="form-row-split">
              <div class="field-wrap split-field">
                <select v-model="attrs.study.difficulty" class="bare-select">
                  <option value="">难度</option>
                  <option value="入门">入门</option>
                  <option value="进阶">进阶</option>
                  <option value="高级">高级</option>
                </select>
              </div>
              <div class="field-wrap split-field">
                <van-field v-model="attrs.study.preferred_time" placeholder="期望时间（如：周二下午）"
                  class="bare-field" />
              </div>
            </div>
          </template>

          <!-- team -->
          <template v-if="form.type === 'team'">
            <div class="form-row-split">
              <div class="field-wrap split-field">
                <van-field v-model.number="attrs.team.team_size" type="number"
                  placeholder="队伍人数（至少2人）" class="bare-field" />
              </div>
              <div class="field-wrap split-field">
                <select v-model="attrs.team.team_type" class="bare-select">
                  <option value="">队伍类型</option>
                  <option value="course_project">课程项目</option>
                  <option value="competition">竞赛</option>
                  <option value="club">社团</option>
                  <option value="other">其他</option>
                </select>
              </div>
            </div>
            <div class="field-wrap" :class="{ focused: focusedField === 'teamTags' }">
              <div class="field-icon-wrap"><van-icon name="friends-o" /></div>
              <van-field v-model="attrs.team.team_tags" placeholder="技能标签（如：Python,机器学习,前端，逗号分隔）"
                class="bare-field"
                @focus="focusedField = 'teamTags'" @blur="focusedField = null" />
            </div>
            <div class="form-section">
              <label class="section-label">💡 发布后你将自动成为队长，队员可申请加入</label>
            </div>
          </template>

          <!-- Image upload -->
          <div class="form-section">
            <label class="section-label">添加图片（选填）</label>
            <van-uploader v-model="imageFiles"
              :max-count="9" :max-size="5 * 1024 * 1024"
              accept="image/*"
              :before-read="beforeReadImage"
              :after-read="afterReadImage"
              @oversize="showToast('图片不能超过5MB')" />
          </div>

          <div class="field-wrap" :class="{ focused: focusedField === 'location' }">
            <div class="field-icon-wrap"><van-icon name="location-o" /></div>
            <van-field v-model="form.location" :placeholder="rewardConfig.locationLabel + (rewardConfig.locationRequired ? '' : '（选填）')"
              class="bare-field"
              @focus="focusedField = 'location'" @blur="focusedField = null" />
          </div>

          <!-- Reward (type-aware) -->
          <template v-if="rewardConfig.showReward">
            <div class="form-section" v-if="form.type === 'trade'">
              <label class="section-label">💡 提示：建议上传实物图片，二手商品信息越详细越容易成交</label>
            </div>
            <div class="form-row-split">
              <div class="field-wrap split-field" :class="{ focused: focusedField === 'rewardType' }">
                <div class="field-icon-wrap"><van-icon name="gold-coin-o" /></div>
                <select v-model="form.rewardType" class="bare-select"
                  @focus="focusedField = 'rewardType'" @blur="focusedField = null">
                  <option v-if="rewardConfig.rewardTypes.includes('point')" value="point">积分</option>
                  <option v-if="rewardConfig.rewardTypes.includes('cash')" value="cash">现金</option>
                  <option v-if="rewardConfig.rewardTypes.includes('donation')" value="donation">公益</option>
                </select>
              </div>
              <div class="field-wrap split-field" :class="{ focused: focusedField === 'rewardAmount' }">
                <van-field v-model.number="form.rewardAmount" type="number"
                  :placeholder="rewardConfig.rewardLabel" class="bare-field"
                  @focus="focusedField = 'rewardAmount'" @blur="focusedField = null" />
              </div>
            </div>
          </template>
          <div v-if="form.type === 'lost_found'" class="form-section">
            <label class="section-label">💡 提示：上传物品照片能大大提高找回/归还几率</label>
          </div>

          <!-- Anonymous toggle -->
          <div class="anon-row">
            <div class="anon-left">
              <van-icon name="eye-o" size="18" color="var(--c-text-3)" />
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
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { publishDemand, uploadDemandImage } from '@/api/demand'
import { DEMAND_TYPES, TYPE_CONFIG } from '@/constants/demandTypes'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const focusedField = ref(null)
const imageFiles = ref([])
const uploadedImageUrls = ref([])

const initialType = route.query.type
const validTypes = ['errand', 'trade', 'team', 'lost_found', 'study', 'other']
const form = reactive({
  type: validTypes.includes(initialType) ? initialType : 'errand',
  title: '', description: '', location: '',
  rewardType: 'point', rewardAmount: 0, isAnonymous: false
})

const attrs = reactive({
  errand: { pickup_location: '', item_category: '', urgency: 'normal' },
  trade: { item_condition: '', item_price: null, trade_category: '' },
  lost_found: { lf_type: 'LOST', item_category: '', lost_found_date: '' },
  study: { subject: '', study_mode: '', difficulty: '', preferred_time: '' },
  team: { team_size: 2, team_tags: '', team_type: '' }
})

const demandTypes = Object.values(DEMAND_TYPES)
const rewardConfig = computed(() => TYPE_CONFIG[form.type] || TYPE_CONFIG.other)

function beforeReadImage(file) {
  if (!file.type.startsWith('image/')) { showToast('只支持图片文件'); return false }
  return true
}

async function afterReadImage(item) {
  try {
    const url = await uploadDemandImage(item.file)
    uploadedImageUrls.value.push(url)
  } catch {
    imageFiles.value = imageFiles.value.filter(f => f !== item)
    showToast('图片上传失败，请重试')
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
    // Build type-specific attributes
    const typeAttrs = attrs[form.type]
    if (typeAttrs) {
      const filtered = {}
      for (const [k, v] of Object.entries(typeAttrs)) {
        if (v !== '' && v !== null && v !== undefined) filtered[k] = v
      }
      if (Object.keys(filtered).length > 0) {
        payload.attributes = filtered
      }
    }
    // team: validate team_size
    if (form.type === 'team' && attrs.team.team_size < 2) {
      showToast('队伍人数至少为2人')
      loading.value = false
      return
    }
    // trade: sync item_price to rewardAmount
    if (form.type === 'trade' && attrs.trade.item_price != null) {
      payload.rewardAmount = attrs.trade.item_price
      payload.rewardType = 'cash'
    }
    await publishDemand(payload)
    showToast('发布成功')
    router.push('/demands')
  } catch (e) { showToast(e.message || '发布失败，请重试') }
  finally { loading.value = false }
}
</script>

<style scoped>
.publish-page { background: var(--c-bg); padding-bottom: 32px; }
.publish-nav :deep(.van-nav-bar__content) { background: #fff !important; box-shadow: var(--s-xs); }
.nav-title { font-weight: 600; }

.content-wrap { padding: 16px; }
.publish-card { padding: 24px 20px; border-radius: var(--r-extra-large); }
.publish-form { display: flex; flex-direction: column; gap: 18px; }

/* Type grid */
.form-section { display: flex; flex-direction: column; gap: 8px; }
.section-label { font-size: 14px; font-weight: 600; color: var(--c-text-1); }
.type-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }

.type-chip {
  display: flex; align-items: center; justify-content: center; gap: 6px;
  padding: 11px 8px;
  border-radius: var(--r-medium);
  border: 2px solid var(--c-border);
  font-size: 13px; font-weight: 600; color: var(--c-text-2);
  background: var(--c-surface-variant);
  cursor: pointer;
  transition: all var(--spring-fast-spatial);
}
.type-chip:active { opacity: 0.72; }
.chip-active { box-shadow: 0 2px 8px rgba(0,0,0,0.15); border-color: transparent !important; }

/* Field wraps */
.field-wrap {
  display: flex; align-items: center;
  background: var(--c-surface-variant);
  border: 2px solid transparent;
  border-radius: var(--r-medium);
  padding: 0 14px;
  min-height: 54px;
  transition: all var(--spring-default-spatial);
}
.field-wrap.focused {
  border-color: var(--c-primary);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(103,80,164,0.10);
}
.area-wrap { align-items: flex-start; padding-top: 12px; }

.field-icon-wrap {
  color: var(--c-text-3); font-size: 20px;
  margin-right: 12px; flex-shrink: 0;
  transition: color var(--spring-fast-spatial);
}
.field-wrap.focused .field-icon-wrap { color: var(--c-primary); }

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

/* Lost & Found type toggle */
.lf-type-toggle { display: flex; gap: 12px; }
.lf-toggle-chip {
  flex: 1; text-align: center; padding: 12px;
  border-radius: var(--r-medium);
  font-size: 15px; font-weight: 600;
  border: 2px solid var(--c-border);
  background: var(--c-surface-variant);
  color: var(--c-text-2);
  cursor: pointer; transition: all var(--spring-fast-spatial);
}
.lf-toggle-chip.active {
  background: #F3E5F5; border-color: #7B1FA2; color: #7B1FA2;
  box-shadow: 0 2px 8px rgba(123,31,162,0.15);
}
.lf-toggle-chip:active { transform: scale(0.96); }

/* Anonymous */
.anon-row {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 0;
}
.anon-left { display: flex; align-items: center; gap: 8px; color: var(--c-text-3); }
.anon-label { font-size: 14px; color: var(--c-text-1); font-weight: 600; }
.anon-hint { font-size: 12px; color: var(--c-text-3); }

/* Uploader */
.form-section :deep(.van-uploader__upload),
.form-section :deep(.van-uploader__preview-image) {
  width: 84px; height: 84px; border-radius: var(--r-md);
}

.submit-btn { height: 50px !important; font-size: 16px !important; font-weight: 600 !important; margin-top: 4px; }

@media (min-width: 768px) {
  .content-wrap { max-width: var(--content-max); margin: 0 auto; padding: 24px 32px; }
  .publish-card { max-width: 640px; margin: 0 auto; padding: 32px; }
  .type-grid { grid-template-columns: repeat(6, 1fr); }
}
</style>
