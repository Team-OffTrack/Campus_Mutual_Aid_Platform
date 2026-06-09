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
        <p class="brand-tagline">加入我们，一起让校园更美好</p>
      </div>
    </div>

    <!-- ═══ Right form panel ═══ -->
    <div class="auth-body">
      <div class="auth-card">
        <div class="form-header">
          <h2 class="form-title">创建账号</h2>
          <p class="form-sub">填写以下信息完成注册</p>
        </div>

        <van-form @submit="handleRegister" class="auth-form">
          <div class="field-wrap" :class="{ focused: focusedField === 'id' }">
            <div class="field-icon-wrap">
              <van-icon name="manager-o" />
            </div>
            <van-field v-model="form.studentId" placeholder="学号"
              :rules="[{ required: true, message: '请输入学号' }]"
              class="bare-field"
              @focus="focusedField = 'id'" @blur="focusedField = null" />
          </div>

          <div class="field-wrap" :class="{ focused: focusedField === 'name' }">
            <div class="field-icon-wrap">
              <van-icon name="contact" />
            </div>
            <van-field v-model="form.name" placeholder="姓名"
              :rules="[{ required: true, message: '请输入姓名' }]"
              class="bare-field"
              @focus="focusedField = 'name'" @blur="focusedField = null" />
          </div>

          <div class="field-wrap" :class="{ focused: focusedField === 'pw' }">
            <div class="field-icon-wrap">
              <van-icon name="lock" />
            </div>
            <van-field v-model="form.password" type="password" placeholder="密码（至少6位）"
              :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }]"
              class="bare-field"
              @focus="focusedField = 'pw'" @blur="focusedField = null" />
          </div>

          <div class="field-wrap" :class="{ focused: focusedField === 'pw2' }">
            <div class="field-icon-wrap">
              <van-icon name="shield-o" />
            </div>
            <van-field v-model="form.confirmPassword" type="password" placeholder="再次输入密码"
              :rules="[{ required: true, message: '请确认密码' }, { validator: checkMatch, message: '两次密码不一致' }]"
              class="bare-field"
              @focus="focusedField = 'pw2'" @blur="focusedField = null" />
          </div>

          <van-button block native-type="submit" :loading="loading" class="submit-btn" round>
            注册
          </van-button>
        </van-form>

        <div class="auth-switch">
          已有账号？
          <router-link to="/login" class="switch-link">去登录 →</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { register } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const focusedField = ref(null)
const form = reactive({ studentId: '', name: '', password: '', confirmPassword: '' })

function checkMatch(val) { return val === form.password }

async function handleRegister() {
  loading.value = true
  try {
    await register({ studentId: form.studentId, name: form.name, password: form.password })
    showToast('注册成功，请登录')
    router.push('/login')
  } catch (e) { /* toast by interceptor */ }
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
  padding: 40px 16px 32px;
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

.blob-1 { width: 200px; height: 200px; top: -50px; right: -50px; }
.blob-2 { width: 100px; height: 100px; top: 60px; right: 80px; opacity: 0.25; }
.blob-3 { width: 140px; height: 140px; bottom: -50px; left: -30px; }

.ring {
  position: absolute;
  border: 2px solid rgba(255,255,255,0.08);
  border-radius: 50%;
}
.ring-1 { width: 260px; height: 260px; top: -100px; right: -90px; }
.ring-2 { width: 160px; height: 160px; top: 4px; right: 50px; }

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

  .blob-1 { width: 320px; height: 320px; top: -120px; right: -90px; }
  .blob-2 { width: 160px; height: 160px; top: 120px; right: 140px; }
  .blob-3 { width: 220px; height: 220px; bottom: -70px; left: -70px; }
  .ring-1 { width: 420px; height: 420px; top: -180px; right: -180px; }
  .ring-2 { width: 260px; height: 260px; top: 30px; right: 90px; }

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
