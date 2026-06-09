<template>
  <div class="auth-page">
    <!-- ═══ Left hero panel ═══ -->
    <div class="auth-hero">
      <div class="hero-deco" aria-hidden="true">
        <div class="blob blob-1 float-slow"></div>
        <div class="blob blob-2 float-fast"></div>
        <div class="blob blob-3 float-slow"></div>
        <div class="ring ring-1"></div>
        <div class="ring ring-2"></div>
      </div>

      <div class="hero-brand">
        <div class="brand-mark">
          <span>互</span>
        </div>
        <h1 class="brand-name">校园互助</h1>
        <p class="brand-tagline">让每一次互助都有温度</p>
      </div>
    </div>

    <!-- ═══ Right form panel ═══ -->
    <div class="auth-body">
      <div class="auth-card">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-sub">请登录你的账号</p>
        </div>

        <van-form @submit="handleLogin" class="auth-form">
          <div class="field-wrap" :class="{ focused: focusedField === 'id' }">
            <div class="field-icon-wrap">
              <van-icon name="manager-o" />
            </div>
            <van-field
              v-model="form.studentId"
              placeholder="学号"
              :rules="[{ required: true, message: '请输入学号' }]"
              class="bare-field"
              @focus="focusedField = 'id'"
              @blur="focusedField = null"
            />
          </div>

          <div class="field-wrap" :class="{ focused: focusedField === 'pw' }">
            <div class="field-icon-wrap">
              <van-icon name="lock" />
            </div>
            <van-field
              v-model="form.password"
              type="password"
              placeholder="密码"
              :rules="[{ required: true, message: '请输入密码' }]"
              class="bare-field"
              @focus="focusedField = 'pw'"
              @blur="focusedField = null"
            />
          </div>

          <van-button block native-type="submit" :loading="loading" class="submit-btn" round>
            登录
          </van-button>
        </van-form>

        <div class="auth-switch">
          还没有账号？
          <router-link to="/register" class="switch-link">立即注册 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { login } from '@/api/user'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const focusedField = ref(null)
const form = reactive({ studentId: '', password: '' })

async function handleLogin() {
  loading.value = true
  try {
    const data = await login({ studentId: form.studentId, password: form.password })
    authStore.setAuth(data)
    showToast('登录成功')
    router.push('/')
  } catch (e) { /* toast handled by interceptor */ }
  finally { loading.value = false }
}
</script>

<style scoped>
/* ═══════════════════════════════════════
   Layout
   ═══════════════════════════════════════ */
.auth-page {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
}

/* ═══════════════════════════════════════
   Hero panel
   ═══════════════════════════════════════ */
.auth-hero {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 52px 16px 36px;
  background: var(--g-hero);
  isolation: isolate;
}

.hero-deco { position: absolute; inset: 0; z-index: 0; pointer-events: none; }

.blob {
  position: absolute;
  border-radius: 50%;
  opacity: 0.16;
  background: #fff;
}

.blob-1 { width: 220px; height: 220px; top: -70px; right: -60px; }
.blob-2 { width: 120px; height: 120px; top: 80px; right: 90px; opacity: 0.25; }
.blob-3 { width: 160px; height: 160px; bottom: -60px; left: -40px; }

.ring {
  position: absolute;
  border: 2px solid rgba(255,255,255,0.08);
  border-radius: 50%;
}
.ring-1 { width: 300px; height: 300px; top: -120px; right: -120px; }
.ring-2 { width: 180px; height: 180px; top: 4px; right: 60px; }

.hero-brand {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
}

.brand-mark {
  width: 72px; height: 72px;
  margin: 0 auto 16px;
  background: rgba(255,255,255,0.18);
  border: 2.5px solid rgba(255,255,255,0.5);
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
  color: #fff;
  backdrop-filter: blur(12px);
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
}

.brand-name {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 3px;
  margin-bottom: 6px;
}

.brand-tagline {
  font-size: 14px;
  color: rgba(255,255,255,0.7);
  font-weight: 400;
}

/* ═══════════════════════════════════════
   Form panel
   ═══════════════════════════════════════ */
.auth-body {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: var(--c-bg);
  padding: 0 16px 32px;
  margin-top: -16px;
  position: relative;
  z-index: 2;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--c-surface);
  border-radius: 24px;
  padding: 32px 22px 24px;
  box-shadow: var(--s-md);
}

.form-header {
  margin-bottom: 28px;
}

.form-title {
  font: var(--t-title);
  color: var(--c-text-1);
  margin-bottom: 4px;
}

.form-sub {
  font-size: 14px;
  color: var(--c-text-3);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ═══════════════════════════════════════
   Field wraps
   ═══════════════════════════════════════ */
.field-wrap {
  display: flex;
  align-items: center;
  background: var(--c-surface-variant);
  border: 2px solid transparent;
  border-radius: var(--r-md);
  padding: 0 14px;
  min-height: 54px;
  transition: all var(--ease);
}

.field-wrap.focused {
  border-color: var(--c-primary);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(103, 80, 164, 0.10);
}

.field-icon-wrap {
  color: var(--c-text-3);
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
  transition: color var(--ease);
}

.field-wrap.focused .field-icon-wrap { color: var(--c-primary); }

.bare-field { flex: 1; }
.bare-field :deep(.van-cell) {
  padding: 0 !important;
  background: transparent !important;
  min-height: 46px;
  display: flex;
  align-items: center;
}
.bare-field :deep(.van-field__body) { padding: 0; }
.bare-field :deep(.van-field__control) {
  font-size: 16px;
  color: var(--c-text-1);
}

/* ═══════════════════════════════════════
   Submit
   ═══════════════════════════════════════ */
.submit-btn {
  height: 50px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  margin-top: 6px;
  border-radius: var(--r-full) !important;
}

/* ═══════════════════════════════════════
   Switch link
   ═══════════════════════════════════════ */
.auth-switch {
  text-align: center;
  margin-top: 22px;
  font-size: 14px;
  color: var(--c-text-3);
}

.switch-link {
  color: var(--c-primary);
  text-decoration: none;
  font-weight: 700;
  transition: opacity var(--ease);
}
.switch-link:hover { opacity: 0.8; }

/* ═══════════════════════════════════════
   Desktop (≥ 768px) — split-screen
   ═══════════════════════════════════════ */
@media (min-width: 768px) {
  .auth-page { flex-direction: row; }

  .auth-hero {
    flex: 0 0 44%;
    min-height: 100dvh;
    padding: 60px;
    align-items: center;
  }

  .brand-mark {
    width: 88px; height: 88px;
    font-size: 38px;
    border-radius: 26px;
    margin-bottom: 20px;
  }

  .brand-name { font-size: 32px; }

  .blob-1 { width: 360px; height: 360px; top: -140px; right: -100px; }
  .blob-2 { width: 180px; height: 180px; top: 140px; right: 140px; }
  .blob-3 { width: 240px; height: 240px; bottom: -80px; left: -80px; }
  .ring-1 { width: 460px; height: 460px; top: -200px; right: -200px; }
  .ring-2 { width: 280px; height: 280px; top: 40px; right: 100px; }

  .auth-body {
    flex: 1;
    align-items: center;
    justify-content: center;
    background: var(--c-surface);
    margin-top: 0;
    padding: 40px;
  }

  .auth-card {
    box-shadow: none;
    border-radius: 0;
    max-width: 380px;
    padding: 0;
  }
}

@media (min-width: 1024px) {
  .auth-hero { flex: 0 0 40%; }
  .auth-card { max-width: 400px; }
}
</style>
