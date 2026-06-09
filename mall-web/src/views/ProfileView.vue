<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { BadgeCheck, RefreshCw, ShieldCheck, UserRound } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    await authStore.loadMe()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户信息加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="shop-page">
    <div class="page-header">
      <div>
        <span class="section-eyebrow">Account</span>
        <h1>个人中心</h1>
        <p>查看当前会员信息和商城服务状态。</p>
      </div>
      <el-button :loading="loading" @click="load">
        <RefreshCw :size="17" />
        刷新
      </el-button>
    </div>

    <section class="profile-panel">
      <div class="profile-avatar">
        <UserRound :size="36" />
      </div>
      <div class="profile-main">
        <el-tag type="success" effect="plain">
          <BadgeCheck :size="14" />
          NovaMall 会员
        </el-tag>
        <h2>{{ authStore.user?.username || '游客用户' }}</h2>
        <p>{{ authStore.user?.phone || '登录后同步手机号和订单信息' }}</p>
      </div>
      <div class="profile-service">
        <ShieldCheck :size="22" />
        <div>
          <strong>服务保障</strong>
          <span>正品、发货、售后一站式管理</span>
        </div>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户 ID">{{ authStore.user?.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">{{ authStore.user?.status === 1 ? '正常' : authStore.user?.status || '-' }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </section>
</template>
