<template>
  <div class="page profile-page">
    <!-- Nav -->
    <van-nav-bar left-arrow fixed placeholder class="profile-nav" @click-left="router.back()">
      <template #title>
        <span class="nav-title-text">个人资料</span>
      </template>
      <template #right><NavActions /></template>
    </van-nav-bar>

    <!-- ═══ Hero header ═══ -->
    <div class="profile-hero">
      <div class="hero-deco" aria-hidden="true">
        <div class="deco-circle deco-1 float-slow"></div>
        <div class="deco-circle deco-2 float-fast"></div>
      </div>

      <div class="avatar-wrap" role="button" aria-label="编辑头像" @click="triggerAvatar">
        <div class="avatar-circle" :class="{ uploading: uploadingAvatar }">
          <img v-if="profile.avatar" :src="profile.avatar" class="avatar-img" />
          <span v-else class="avatar-letter">{{ nameInitial }}</span>
          <div v-if="uploadingAvatar" class="avatar-overlay">
            <van-loading color="#fff" size="22" />
          </div>
        </div>
        <div class="avatar-badge" title="编辑头像">
          <van-icon name="photograph" />
        </div>
        <input ref="avatarInput" type="file" accept="image/*" hidden @change="handleAvatarChange" />
      </div>

      <h2 class="profile-name">{{ profile.name || '—' }}</h2>
      <p class="profile-id">学号 {{ profile.studentId || '—' }}</p>
    </div>

    <div class="content-wrap">
      <!-- ═══ Stats bar ═══ -->
      <div class="stats-bar glass">
        <div class="stat-item points-stat" role="button" tabindex="0"
             @click="router.push('/points/history')">
          <span class="stat-num" style="color:var(--c-primary)">
            {{ profile.availablePoints ?? '—' }}
            <van-icon name="arrow" class="points-arrow" />
          </span>
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

      <!-- ═══ Two-column layout ═══ -->
      <div class="profile-columns">
        <!-- Basic info -->
        <div class="column">
          <div class="section">
            <h3 class="section-title">基本信息</h3>
            <div class="info-card card">
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
            <div class="info-card card">
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

      <!-- Password -->
      <div class="section password-section">
        <h3 class="section-title">修改密码</h3>
        <div class="info-card card">
          <div class="info-row">
            <span class="info-label">旧密码</span>
            <div class="info-field">
              <van-field v-model="passwordForm.oldPassword" type="password"
                placeholder="输入旧密码" class="inline-field" />
            </div>
          </div>
          <div class="info-row">
            <span class="info-label">新密码</span>
            <div class="info-field">
              <van-field v-model="passwordForm.newPassword" type="password"
                placeholder="6-64位新密码" class="inline-field" />
            </div>
          </div>
        </div>
        <div class="save-section">
          <van-button block round :loading="changingPassword" class="save-btn"
            @click="handleChangePassword">
            修改密码
          </van-button>
        </div>
      </div>

      <!-- Logout -->
      <div class="logout-section">
        <van-button block round class="logout-btn" @click="handleLogout">
          退出登录
        </van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { getProfile, updateProfile, changePassword, uploadAvatar } from '@/api/user'
import NavActions from '@/components/NavActions.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const focusedField = ref(null)
const profile = ref({})
const form = reactive({ name: null, avatar: null, isAnonymous: false, maskName: '' })

const changingPassword = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '' })

const avatarInput = ref(null)
const uploadingAvatar = ref(false)

function triggerAvatar() { avatarInput.value?.click() }

async function handleAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    const avatarUrl = await uploadAvatar(file)
    profile.value.avatar = avatarUrl
    form.avatar = avatarUrl
    authStore.avatar = avatarUrl
    localStorage.setItem('avatar', avatarUrl)
    showToast('头像已更新')
  } catch (e) { showToast(e.message || '头像上传失败') }
  finally { uploadingAvatar.value = false }
  e.target.value = ''
}

const nameInitial = computed(() => ((profile.value.name || form.name || '?').charAt(0)).toUpperCase())

onMounted(async () => {
  try {
    const data = await getProfile()
    profile.value = data
    form.name = data.name || ''
    form.isAnonymous = data.isAnonymous || false
    form.maskName = data.maskName || ''
  } catch (e) { showToast(e.message || '个人资料加载失败') }
})

async function handleSave() {
  loading.value = true
  try {
    const payload = {}
    if (form.name && form.name !== profile.value.name) payload.name = form.name
    if (form.avatar) payload.avatar = form.avatar
    if (form.isAnonymous !== profile.value.isAnonymous) payload.isAnonymous = form.isAnonymous
    if (form.maskName !== (profile.value.maskName || '')) payload.maskName = form.maskName
    const data = await updateProfile(payload)
    profile.value = data
    showToast('保存成功')
  } catch (e) { showToast(e.message || '保存失败，请重试') }
  finally { loading.value = false }
}

async function handleChangePassword() {
  if (!passwordForm.oldPassword) { showToast('请输入旧密码'); return }
  if (!passwordForm.newPassword || passwordForm.newPassword.length < 6) { showToast('新密码至少6位'); return }
  changingPassword.value = true
  try {
    await changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    showToast('密码已修改')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
  } catch (e) { showToast(e.message || '密码修改失败，请检查旧密码是否正确') }
  finally { changingPassword.value = false }
}

async function handleLogout() {
  try { await showConfirmDialog({ title: '退出登录', message: '确定要退出当前账号吗？' }) }
  catch { return }
  authStore.logout()
  showToast('已退出')
  router.push('/login')
}
</script>

<style scoped>
.profile-page { background: var(--c-bg); }

/* ═══════════════════════════════════════
   Nav
   ═══════════════════════════════════════ */
.profile-nav :deep(.van-nav-bar) { background: var(--g-hero) !important; }
.profile-nav :deep(.van-nav-bar__title),
.profile-nav :deep(.van-nav-bar__arrow) { color: #fff !important; }
.nav-title-text { color: #fff; font-weight: 600; }

/* ═══════════════════════════════════════
   Hero
   ═══════════════════════════════════════ */
.profile-hero {
  position: relative;
  background: var(--g-hero);
  padding: 20px 24px 64px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  overflow: hidden;
  isolation: isolate;
  border-radius: 0 0 var(--r-extra-large) var(--r-extra-large);
}

.hero-deco { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  background: #fff;
}
.deco-1 { width: 140px; height: 140px; top: -40px; right: -30px; }
.deco-2 { width: 90px; height: 90px; bottom: 20px; left: -20px; opacity: 0.18; }

/* Avatar */
.avatar-wrap { position: relative; margin-bottom: 16px; z-index: 1; }

.avatar-circle {
  width: 92px; height: 92px; border-radius: 50%;
  background: rgba(255,255,255,0.24);
  border: 3px solid rgba(255,255,255,0.55);
  display: flex; align-items: center; justify-content: center;
  font-size: 38px; font-weight: 800; color: #fff;
  backdrop-filter: blur(10px);
  overflow: hidden; position: relative; cursor: pointer;
  transition: transform var(--spring-fast-spatial), box-shadow var(--spring-default-spatial);
  box-shadow: 0 8px 32px rgba(0,0,0,0.15);
}
.avatar-circle:active { transform: scale(0.95); }
.avatar-circle.uploading { pointer-events: none; }
.avatar-img { width: 100%; height: 100%; object-fit: cover; }
.avatar-letter { line-height: 1; }

.avatar-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.35);
  display: flex; align-items: center; justify-content: center;
}

.avatar-badge {
  position: absolute; bottom: 2px; right: 2px;
  width: 28px; height: 28px;
  background: #fff; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 14px; color: var(--c-primary);
  box-shadow: var(--s-sm); cursor: pointer;
  transition: transform var(--ease);
}
.avatar-badge:active { transform: scale(0.9); }

.profile-name {
  font-size: 22px; font-weight: 700; color: #fff;
  margin-bottom: 4px; position: relative; z-index: 1;
}
.profile-id {
  font-size: 13px; color: rgba(255,255,255,0.65);
  position: relative; z-index: 1;
}

/* ═══════════════════════════════════════
   Stats
   ═══════════════════════════════════════ */
.stats-bar {
  display: flex; align-items: center;
  margin: -36px 0 0;
  padding: 18px 0;
  position: relative; z-index: 10;
  box-shadow: var(--s-md), var(--s-glow);
}
.stat-item { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.stat-num { font-size: 22px; font-weight: 700; }
.stat-lbl { font-size: 11px; color: var(--c-text-3); font-weight: 500; }
.stat-sep { width: 1px; height: 32px; background: var(--c-border); }
.points-stat { cursor: pointer; border-radius: var(--r-md); transition: background var(--ease); }
.points-stat:hover { background: rgba(103,80,164,0.06); }
.points-arrow { font-size: 14px; margin-left: 4px; opacity: 0.45; vertical-align: middle; }

/* ═══════════════════════════════════════
   Sections
   ═══════════════════════════════════════ */
.section { padding-top: 24px; }
.section-title {
  font-size: 14px; font-weight: 700; color: var(--c-text-2);
  text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px;
}

/* Info card */
.info-card { overflow: hidden; }
.info-row {
  display: flex; align-items: center;
  padding: 4px 16px; min-height: 56px; gap: 12px;
}
.info-row.border-bottom { border-bottom: 1px solid var(--c-border); }
.info-row.row-disabled { opacity: 0.4; }
.info-row-left { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.info-label { font-size: 14px; font-weight: 600; color: var(--c-text-1); min-width: 60px; }
.info-hint { font-size: 12px; color: var(--c-text-3); }

.info-field {
  flex: 1; border-radius: var(--r-sm);
  transition: background var(--ease);
}
.info-field.focused { background: var(--c-primary-container); }

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

/* Logout */
.logout-section { padding-top: 16px; padding-bottom: calc(24px + var(--safe-bottom)); }
.logout-btn {
  color: var(--c-text-2) !important; border-color: var(--c-border) !important;
  background: var(--c-surface) !important; box-shadow: var(--s-xs) !important;
  height: 44px !important; font-size: 14px !important;
}

/* ═══════════════════════════════════════
   Tablet+ (≥ 768px)
   ═══════════════════════════════════════ */
@media (min-width: 768px) {
  .profile-hero { padding: 32px 32px 76px; }
  .avatar-circle { width: 104px; height: 104px; font-size: 42px; }

  .stats-bar {
    max-width: 640px;
    margin-left: auto; margin-right: auto;
    margin-top: -44px;
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
