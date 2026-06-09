<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UserPlus } from 'lucide-vue-next'
import heroImage from '@/assets/mall-hero.png'
import { register } from '@/api/mall'

const router = useRouter()
const loading = ref(false)
const form = reactive({
  username: 'demo',
  password: '123456',
  phone: '13800000000'
})

async function submit() {
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功')
    router.push('/login')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-shell">
      <div class="auth-panel">
        <div class="auth-brand">
          <div class="brand-mark large">
            <UserPlus :size="28" />
          </div>
          <h1>创建账号</h1>
          <p>注册后可保存购物车、订单和个人信息。</p>
        </div>

        <el-form class="auth-form" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" autocomplete="tel" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" autocomplete="new-password" show-password />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="full-button">
            <UserPlus :size="18" />
            注册
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span>已有账号？</span>
          <el-button link type="primary" @click="router.push('/login')">去登录</el-button>
        </div>
      </div>

      <div class="auth-visual" :style="{ backgroundImage: `linear-gradient(180deg, rgba(14, 24, 34, 0.2), rgba(14, 24, 34, 0.72)), url(${heroImage})` }">
        <div>
          <span>Member</span>
          <strong>从选购到售后统一管理</strong>
        </div>
      </div>
    </section>
  </main>
</template>
