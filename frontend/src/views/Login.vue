<template>
  <div class="page-container">
    <div class="header">
      <h2>校园互助</h2>
      <p>登录你的账号</p>
    </div>

    <van-form @submit="handleLogin">
      <van-cell-group inset>
        <van-field
          v-model="form.studentId"
          label="学号"
          placeholder="请输入学号"
          :rules="[{ required: true, message: '请输入学号' }]"
        />
        <van-field
          v-model="form.password"
          label="密码"
          type="password"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>

      <div style="margin: 16px">
        <van-button block type="primary" native-type="submit" :loading="loading">
          登录
        </van-button>
      </div>
    </van-form>

    <div class="footer-link">
      还没有账号？<router-link to="/register">立即注册</router-link>
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
const form = reactive({
  studentId: '',
  password: ''
})

async function handleLogin() {
  loading.value = true
  try {
    const data = await login({
      studentId: form.studentId,
      password: form.password
    })
    authStore.setAuth(data)
    showToast('登录成功')
    router.push('/')
  } catch (e) {
    // Error toast already shown by interceptor
  } finally {
    loading.value = false
  }
}
</script>
