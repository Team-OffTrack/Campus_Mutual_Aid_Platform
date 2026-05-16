<template>
  <div class="page-container">
    <van-nav-bar title="校园互助" fixed placeholder>
      <template #right>
        <van-icon name="manager" size="22" @click="router.push('/profile')" />
      </template>
    </van-nav-bar>

    <div class="welcome-card">
      <van-icon name="smile-comment" size="48" color="#1989fa" />
      <h3>欢迎回来，{{ authStore.name }}</h3>
      <p class="subtitle">校园互助平台，让校园生活更便捷</p>
    </div>

    <van-grid :column-num="3" :border="false">
      <van-grid-item icon="logistics" text="跑腿代取" />
      <van-grid-item icon="shop" text="二手交易" />
      <van-grid-item icon="friends" text="组队匹配" />
      <van-grid-item icon="search" text="失物招领" />
      <van-grid-item icon="chat" text="在线交流" />
      <van-grid-item icon="gold-coin" text="积分中心" />
    </van-grid>

    <!-- Admin entry -->
    <div v-if="authStore.isAdmin" class="admin-entry">
      <van-button type="warning" block @click="router.push('/admin/users')">
        管理后台
      </van-button>
    </div>

    <div class="logout-btn">
      <van-button type="default" block @click="handleLogout">退出登录</van-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

function handleLogout() {
  authStore.logout()
  showToast('已退出')
  router.push('/login')
}
</script>
