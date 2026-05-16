<template>
  <div class="page profile-page">
    <!-- Nav -->
    <van-nav-bar title="个人资料" left-arrow fixed placeholder
      class="profile-nav" @click-left="router.back()">
      <template #right><NavActions /></template>
    </van-nav-bar>

    <!-- Hero -->
    <div class="profile-hero">
      <div class="avatar-wrap">
        <div class="avatar-circle">{{ nameInitial }}</div>
        <div class="avatar-badge" title="编辑头像"><van-icon name="photograph" /></div>
      </div>
      <h2 class="profile-name">{{ profile.name || '—' }}</h2>
      <p class="profile-id">学号 {{ profile.studentId || '—' }}</p>
    </div>

    <div class="content-wrap">
      <!-- Stats bar -->
      <div class="stats-bar">
        <div class="stat-item">
          <span class="stat-num" style="color:var(--c-primary)">{{ profile.availablePoints ?? '—' }}</span>
          <span class="stat-lbl">可用积分</span>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <span class="stat-num" style="color:var(--c-warning)">{{ profile.frozenPoints ?? '—' }}</span>
          <span class="stat-lbl">冻结积分</span>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <span class="stat-num" style="color:var(--c-success)">{{ profile.reputationScore ?? '—' }}</span>
          <span class="stat-lbl">信誉评分</span>
        </div>
      </div>

      <!-- Desktop: two-column layout -->
      <div class="profile-columns">
        <!-- Basic info -->
        <div class="column">
          <div class="section">
            <h3 class="section-title">基本信息</h3>
            <div class="info-card">
              <div class="info-row">
                <span class="info-label">姓名</span>
                <div class="info-field" :class="{ focused: focusedField === 'name' }">
                  <van-field v-model="form.name" :placeholder="profile.name || '修改姓名'"
                    class="inline-field"
                    @focus="focusedField = 'name'" @blur="focusedField = null" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Privacy settings -->
        <div class="column">
          <div class="section">
            <h3 class="section-title">隐私设置</h3>
            <div class="info-card">
              <div class="info-row border-bottom">
                <div class="info-row-left">
                  <span class="info-label">匿名模式</span>
                  <span class="info-hint">对其他人隐藏真实身份</span>
                </div>
                <van-switch v-model="form.isAnonymous" size="22px" />
              </div>
              <div class="info-row" :class="{ 'row-disabled': !form.isAnonymous }">
                <span class="info-label">虚拟昵称</span>
                <div class="info-field" :class="{ focused: focusedField === 'mask' }">
                  <van-field v-model="form.maskName" placeholder="匿名时对外显示的昵称"
                    :disabled="!form.isAnonymous" class="inline-field"
                    @focus="focusedField = 'mask'" @blur="focusedField = null" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Save -->
      <div class="save-section">
        <van-button block round :loading="loading" class="save-btn" @click="handleSave">
          保存修改
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getProfile, updateProfile } from '@/api/user'
import NavActions from '@/components/NavActions.vue'

const router = useRouter()
const loading = ref(false)
const focusedField = ref(null)
const profile = ref({})
const form = reactive({ name: null, avatar: null, isAnonymous: false, maskName: '' })

const nameInitial = computed(() => ((profile.value.name || form.name || '?').charAt(0)).toUpperCase())

onMounted(async () => {
  try {
    const data = await getProfile()
    profile.value = data
    form.isAnonymous = data.isAnonymous || false
    form.maskName = data.maskName || ''
  } catch (e) { /* skip */ }
})

async function handleSave() {
  loading.value = true
  try {
    const payload = {}
    if (form.name) payload.name = form.name
    if (form.avatar) payload.avatar = form.avatar
    if (form.isAnonymous !== profile.value.isAnonymous) payload.isAnonymous = form.isAnonymous
    if (form.maskName !== (profile.value.maskName || '')) payload.maskName = form.maskName
    const data = await updateProfile(payload)
    profile.value = data
    showToast('保存成功')
  } catch (e) { /* skip */ }
  finally { loading.value = false }
}
</script>

<style scoped>
.profile-page { background: var(--c-bg); }

/* Nav */
.profile-nav :deep(.van-nav-bar) { background: var(--g-hero) !important; }

/* Hero */
.profile-hero {
  background: var(--g-hero);
  padding: 28px 24px 60px;
  display: flex; flex-direction: column; align-items: center; text-align: center;
}

.avatar-wrap { position: relative; margin-bottom: 14px; }

.avatar-circle {
  width: 84px; height: 84px; border-radius: 50%;
  background: rgba(255,255,255,0.26);
  border: 3px solid rgba(255,255,255,0.6);
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; font-weight: 800; color: #fff;
  backdrop-filter: blur(8px);
}

.avatar-badge {
  position: absolute; bottom: 2px; right: 2px;
  width: 26px; height: 26px;
  background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; color: var(--c-primary);
  box-shadow: var(--s-sm); cursor: pointer;
}

.profile-name { font-size: 20px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.profile-id { font-size: 13px; color: rgba(255,255,255,0.65); }

/* Stats */
.stats-bar {
  display: flex; align-items: center;
  background: var(--c-surface);
  border-radius: var(--r-lg);
  margin: -28px 0 0;
  padding: 16px 0;
  position: relative; z-index: 10;
  box-shadow: var(--s-md);
}
.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.stat-num { font-size: 20px; font-weight: 700; }
.stat-lbl { font-size: 11px; color: var(--c-text-3); }
.stat-sep { width: 1px; height: 30px; background: var(--c-border); }

/* Sections */
.section { padding-top: 24px; }
.section-title {
  font-size: 14px; font-weight: 700; color: var(--c-text-2);
  text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px;
}

/* Info card */
.info-card {
  background: var(--c-surface);
  border-radius: var(--r-md);
  box-shadow: var(--s-xs);
  overflow: hidden;
}
.info-row {
  display: flex; align-items: center;
  padding: 4px 16px; min-height: 56px; gap: 12px;
}
.info-row.border-bottom { border-bottom: 1px solid var(--c-border); }
.info-row.row-disabled { opacity: 0.45; }
.info-row-left { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.info-label { font-size: 14px; font-weight: 600; color: var(--c-text-1); min-width: 60px; }
.info-hint { font-size: 12px; color: var(--c-text-3); }

.info-field {
  flex: 1; border-radius: var(--r-sm);
  transition: background var(--ease);
}
.info-field.focused { background: var(--c-primary-soft); }

.inline-field :deep(.van-cell) { padding: 8px 0 !important; background: transparent !important; }
.inline-field :deep(.van-field__control) {
  font-size: 14px; color: var(--c-text-1); text-align: right;
}

/* Save */
.save-section { padding-top: 28px; }
.save-btn {
  height: 50px !important; font-size: 16px !important;
  font-weight: 600 !important; letter-spacing: 1px;
}

/* ════════════════════════════════════════
   Tablet+ (≥ 768px) — two-column form layout
   ════════════════════════════════════════ */
@media (min-width: 768px) {
  .profile-hero {
    padding: 36px 32px 72px;
  }
  .avatar-circle { width: 96px; height: 96px; font-size: 40px; }

  .stats-bar {
    max-width: 640px;
    margin-left: auto; margin-right: auto;
    margin-top: -36px;
  }

  .profile-columns {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
    padding-top: 8px;
  }
  .column { min-width: 0; }

  .save-section { max-width: 400px; margin: 0 auto; }
}

@media (min-width: 1024px) {
  .stats-bar { max-width: 720px; }
  .profile-columns { gap: 32px; }
}
</style>
