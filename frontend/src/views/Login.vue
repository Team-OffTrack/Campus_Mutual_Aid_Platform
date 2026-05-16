<template>
  <div class="auth-page">
    <!-- Left hero panel (visible on desktop) -->
    <div class="auth-hero">
      <div class="hero-brand">
        <div class="brand-mark">互</div>
        <h1 class="brand-name">校园互助</h1>
        <p class="brand-tagline">让每一次互助都有温度</p>
      </div>
      <div class="blob blob-1" aria-hidden="true"></div>
      <div class="blob blob-2" aria-hidden="true"></div>
    </div>

    <!-- Right form panel -->
    <div class="auth-body">
      <div class="auth-card">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">请登录你的账号</p>

        <van-form @submit="handleLogin" class="auth-form">
          <div class="field-row" :class="{ focused: focusedField === 'id' }">
            <van-icon name="manager-o" class="field-icon" />
            <van-field
              v-model="form.studentId"
              placeholder="学号"
              :rules="[{ required: true, message: '请输入学号' }]"
              class="bare-field"
              @focus="focusedField = 'id'"
              @blur="focusedField = null"
            />
          </div>

          <div class="field-row" :class="{ focused: focusedField === 'pw' }">
            <van-icon name="lock" class="field-icon" />
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
/* ── Mobile: stacked layout ── */
.auth-page {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
}

.auth-hero {
  position: relative;
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 48px 16px 32px;
  background: var(--g-hero);
}

.hero-brand {
  position: relative;
  z-index: 2;
  text-align: center;
  color: #fff;
}

.brand-mark {
  width: 64px; height: 64px;
  margin: 0 auto 12px;
  background: rgba(255,255,255,0.22);
  border: 2.5px solid rgba(255,255,255,0.55);
  border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 800; color: #fff;
  backdrop-filter: blur(8px);
}

.brand-name {
  font-size: 24px; font-weight: 700;
  letter-spacing: 2px; margin-bottom: 4px;
}

.brand-tagline {
  font-size: 13px;
  color: rgba(255,255,255,0.72);
}

.blob {
  position: absolute; border-radius: 50%; opacity: 0.18; pointer-events: none;
}
.blob-1 { width: 200px; height: 200px; background: rgba(255,255,255,0.6); top: -60px; right: -60px; }
.blob-2 { width: 140px; height: 140px; background: rgba(255,255,255,0.4); bottom: -80px; left: -40px; }

/* Form area */
.auth-body {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: var(--c-bg);
  padding: 0 16px 32px;
  margin-top: -14px;
  position: relative; z-index: 2;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--c-surface);
  border-radius: 22px;
  padding: 28px 20px 24px;
  box-shadow: var(--s-md);
}

.form-title {
  font-size: 22px; font-weight: 700; color: var(--c-text-1); margin-bottom: 4px;
}
.form-sub {
  font-size: 13px; color: var(--c-text-3); margin-bottom: 24px;
}

.auth-form { display: flex; flex-direction: column; gap: 14px; }

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
.bare-field :deep(.van-field__control) {
  font-size: 15px; color: var(--c-text-1);
}

.submit-btn { height: 48px; font-size: 16px; margin-top: 4px; }

.auth-switch {
  text-align: center; margin-top: 20px;
  font-size: 13px; color: var(--c-text-3);
}
.switch-link { color: var(--c-primary); text-decoration: none; font-weight: 600; }

/* ── Desktop ≥ 768px: split-screen ── */
@media (min-width: 768px) {
  .auth-page {
    flex-direction: row;
  }

  .auth-hero {
    flex: 0 0 44%;
    min-height: 100dvh;
    padding: 60px;
    align-items: center;
  }

  .brand-mark {
    width: 80px; height: 80px;
    font-size: 36px; border-radius: 24px;
    margin-bottom: 16px;
  }

  .brand-name { font-size: 30px; }

  .blob-1 { width: 320px; height: 320px; top: -120px; right: -100px; }
  .blob-2 { width: 220px; height: 220px; bottom: -60px; left: -80px; }

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
  }
}

@media (min-width: 1024px) {
  .auth-hero { flex: 0 0 40%; }
  .auth-card { max-width: 400px; }
}
</style>
