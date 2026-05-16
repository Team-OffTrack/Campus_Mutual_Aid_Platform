<template>
  <div class="page-container">
    <van-nav-bar title="个人资料" left-text="返回" left-arrow @click-left="router.back()" fixed placeholder />

    <van-cell-group inset>
      <van-cell title="头像">
        <van-image
          round
          width="48"
          height="48"
          :src="profile.avatar || 'https://via.placeholder.com/48'"
        />
      </van-cell>
      <van-cell title="学号" :value="profile.studentId" />
      <van-field
        v-model="form.name"
        label="姓名"
        :placeholder="profile.name"
      />
    </van-cell-group>

    <van-cell-group inset title="隐私设置">
      <van-cell title="匿名模式">
        <van-switch v-model="form.isAnonymous" />
      </van-cell>
      <van-field
        v-model="form.maskName"
        label="虚拟昵称"
        placeholder="匿名时对外显示的昵称"
      />
    </van-cell-group>

    <van-cell-group inset title="账户信息">
      <van-cell title="可用积分" :value="profile.availablePoints" />
      <van-cell title="冻结积分" :value="profile.frozenPoints" />
      <van-cell title="信誉评分" :value="profile.reputationScore" />
    </van-cell-group>

    <div style="margin: 16px">
      <van-button block type="primary" :loading="loading" @click="handleSave">保存修改</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getProfile, updateProfile } from '@/api/user'

const router = useRouter()
const loading = ref(false)
const profile = ref({})
const form = reactive({
  name: null,
  avatar: null,
  isAnonymous: false,
  maskName: ''
})

onMounted(async () => {
  try {
    const data = await getProfile()
    profile.value = data
    form.isAnonymous = data.isAnonymous || false
    form.maskName = data.maskName || ''
  } catch (e) {
    // ignore
  }
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
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}
</script>
