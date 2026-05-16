<template>
  <div class="auth-page">
    <!-- Left hero panel -->
    <div class="auth-hero">
      <div class="hero-brand">
        <div class="brand-mark">互</div>
        <h1 class="brand-name">校园互助</h1>
        <p class="brand-tagline">加入我们，一起让校园更美好</p>
      </div>
      <div class="blob blob-1" aria-hidden="true"></div>
      <div class="blob blob-2" aria-hidden="true"></div>
    </div>

    <!-- Right form panel -->
    <div class="auth-body">
      <div class="auth-card">
        <h2 class="form-title">创建账号</h2>
        <p class="form-sub">填写以下信息完成注册</p>

        <van-form @submit="handleRegister" class="auth-form">
          <div class="field-row" :class="{ focused: focusedField === 'id' }">
            <van-icon name="manager-o" class="field-icon" />
            <van-field v-model="form.studentId" placeholder="学号"
              :rules="[{ required: true, message: '请输入学号' }]"
              class="bare-field"
              @focus="focusedField = 'id'" @blur="focusedField = null" />
          </div>

          <div class="field-row" :class="{ focused: focusedField === 'name' }">
            <van-icon name="contact" class="field-icon" />
            <van-field v-model="form.name" placeholder="姓名"
              :rules="[{ required: true, message: '请输入姓名' }]"
              class="bare-field"
              @focus="focusedField = 'name'" @blur="focusedField = null" />
          </div>

          <div class="field-row" :class="{ focused: focusedField === 'pw' }">
            <van-icon name="lock" class="field-icon" />
            <van-field v-model="form.password" type="password" placeholder="密码（至少6位）"
              :rules="[{ required: true, message: '请输入密码' }, { min: 6, message: '密码至少6位' }]"
              class="bare-field"
              @focus="focusedField = 'pw'" @blur="focusedField = null" />
          </div>

          <div class="field-row" :class="{ focused: focusedField === 'pw2' }">
            <van-icon name="shield-o" class="field-icon" />
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
/* ── Mobile: stacked ── */
.auth-page { display: flex; flex-direction: column; min-height: 100dvh; }

.auth-hero {
  position: relative;
  flex: 0 0 auto;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
  padding: 36px 16px 28px;
  background: var(--g-hero);
}

.hero-brand { position: relative; z-index: 2; text-align: center; color: #fff; }

.brand-mark {
  width: 64px; height: 64px; margin: 0 auto 12px;
  background: rgba(255,255,255,0.22);
  border: 2.5px solid rgba(255,255,255,0.55);
  border-radius: 20px;
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 800; color: #fff;
  backdrop-filter: blur(8px);
}
.brand-name { font-size: 24px; font-weight: 700; letter-spacing: 2px; margin-bottom: 4px; }
.brand-tagline { font-size: 13px; color: rgba(255,255,255,0.72); }

.blob { position: absolute; border-radius: 50%; opacity: 0.18; pointer-events: none; }
.blob-1 { width: 180px; height: 180px; background: rgba(255,255,255,0.6); top: -40px; right: -50px; }
.blob-2 { width: 120px; height: 120px; background: rgba(255,255,255,0.4); bottom: -60px; left: -30px; }

/* Form area */
.auth-body {
  flex: 1;
  display: flex; align-items: flex-start; justify-content: center;
  background: var(--c-bg);
  padding: 0 16px 32px;
  margin-top: -14px;
  position: relative; z-index: 2;
}

.auth-card {
  width: 100%; max-width: 420px;
  background: var(--c-surface);
  border-radius: 22px;
  padding: 28px 20px 24px;
  box-shadow: var(--s-md);
}

.form-title { font-size: 22px; font-weight: 700; color: var(--c-text-1); margin-bottom: 4px; }
.form-sub { font-size: 13px; color: var(--c-text-3); margin-bottom: 24px; }

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
.bare-field :deep(.van-field__control) { font-size: 15px; color: var(--c-text-1); }

.submit-btn { height: 48px; font-size: 16px; margin-top: 4px; }

.auth-switch { text-align: center; margin-top: 20px; font-size: 13px; color: var(--c-text-3); }
.switch-link { color: var(--c-primary); text-decoration: none; font-weight: 600; }

/* ── Desktop ≥ 768px: split-screen ── */
@media (min-width: 768px) {
  .auth-page { flex-direction: row; }

  .auth-hero {
    flex: 0 0 44%;
    min-height: 100dvh;
    padding: 60px;
    align-items: center;
  }
  .brand-mark { width: 80px; height: 80px; font-size: 36px; border-radius: 24px; margin-bottom: 16px; }
  .brand-name { font-size: 30px; }
  .blob-1 { width: 280px; height: 280px; top: -100px; right: -80px; }
  .blob-2 { width: 200px; height: 200px; bottom: -40px; left: -60px; }

  .auth-body {
    flex: 1;
    align-items: center; justify-content: center;
    background: var(--c-surface);
    margin-top: 0;
    padding: 40px;
  }
  .auth-card { box-shadow: none; border-radius: 0; max-width: 380px; }
}

@media (min-width: 1024px) {
  .auth-hero { flex: 0 0 40%; }
  .auth-card { max-width: 400px; }
}
</style>
