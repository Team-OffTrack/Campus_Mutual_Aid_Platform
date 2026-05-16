<template>
  <div class="admin-container">
    <van-nav-bar title="用户管理" left-text="返回" left-arrow @click-left="router.push('/')" fixed placeholder />

    <van-search v-model="keyword" placeholder="搜索学号或姓名" @search="onSearch" @clear="onSearch" />

    <van-list
      v-model:loading="loading"
      :finished="finished"
      @load="fetchUsers"
    >
      <van-cell
        v-for="user in users"
        :key="user.userId"
        :title="user.name"
        :label="'学号: ' + user.studentId"
        :value="user.status === 1 ? '正常' : '已封禁'"
        @click="showActions(user)"
      />
    </van-list>

    <!-- Action sheet for ban/unban (component-based API in Vant 4) -->
    <van-action-sheet
      v-model:show="sheetVisible"
      :actions="sheetActions"
      cancel-text="取消"
      @select="onActionSelect"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { listUsers, updateUserStatus } from '@/api/admin'

const router = useRouter()
const users = ref([])
const keyword = ref('')
const loading = ref(false)
const finished = ref(false)
let pageNum = 1

// Action sheet state
const sheetVisible = ref(false)
const sheetActions = ref([])
let currentUser = null

function onSearch() {
  pageNum = 1
  users.value = []
  finished.value = false
  fetchUsers()
}

async function fetchUsers() {
  loading.value = true
  try {
    const page = await listUsers({
      pageNum: pageNum,
      pageSize: 20,
      keyword: keyword.value
    })
    users.value = [...users.value, ...(page.records || [])]
    finished.value = page.current >= page.pages
    pageNum++
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function showActions(user) {
  currentUser = user
  const label = user.status === 1 ? '封禁该用户' : '解封该用户'
  const color = user.status === 1 ? '#ee0a24' : '#07c160'
  sheetActions.value = [{ name: label, color }]
  sheetVisible.value = true
}

async function onActionSelect() {
  if (!currentUser) return
  const newStatus = currentUser.status === 1 ? 0 : 1
  try {
    await updateUserStatus(currentUser.userId, newStatus)
    currentUser.status = newStatus
    showToast(newStatus === 1 ? '已解封' : '已封禁')
  } catch (e) {
    // ignore
  } finally {
    sheetVisible.value = false
  }
}
</script>
