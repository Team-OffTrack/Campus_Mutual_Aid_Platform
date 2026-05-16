<template>
  <div class="page-container">
    <div class="header">
      <h2>校园互助</h2>
      <p>注册新账号</p>
    </div>

    <van-form @submit="handleRegister">
      <van-cell-group inset>
        <van-field
          v-model="form.studentId"
          label="学号"
          placeholder="请输入学号"
          :rules="[{ required: true, message: '请输入学号' }]"
        />
        <van-field
          v-model="form.name"
          label="姓名"
          placeholder="请输入姓名"
          :rules="[{ required: true, message: '请输入姓名' }]"
        />
        <van-field
          v-model="form.password"
          label="密码"
          type="password"
          placeholder="至少6位"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 6, message: '密码至少6位' }
          ]"
        />
        <van-field
          v-model="form.confirmPassword"
          label="确认密码"
          type="password"
          placeholder="再次输入密码"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: checkPasswordMatch, message: '两次密码不一致' }
          ]"
        />
      </van-cell-group>

      <div style="margin: 16px">
        <van-button block type="primary" native-type="submit" :loading="loading">
          注册
        </van-button>
      </div>
    </van-form>

    <div class="footer-link">
      已有账号？<router-link to="/login">去登录</router-link>
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
const form = reactive({
  studentId: '',
  name: '',
  password: '',
  confirmPassword: ''
})

function checkPasswordMatch(val) {
  return val === form.password
}

async function handleRegister() {
  loading.value = true
  try {
    await register({
      studentId: form.studentId,
      name: form.name,
      password: form.password
    })
    showToast('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    // Error toast already shown by interceptor
  } finally {
    loading.value = false
  }
}
</script>
