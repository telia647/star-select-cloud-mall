<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { LogIn, Store } from 'lucide-vue-next'
import heroImage from '@/assets/mall-hero.png'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: 'demo',
  password: '123456'
})

async function submit() {
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push((route.query.redirect as string) || '/products')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
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
            <Store :size="28" />
          </div>
          <h1>登录星选商城</h1>
          <p>使用演示账号可直接体验商品、购物车、订单和支付链路。</p>
        </div>

        <el-form class="auth-form" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" autocomplete="username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" autocomplete="current-password" show-password />
          </el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading" class="full-button">
            <LogIn :size="18" />
            登录
          </el-button>
        </el-form>

        <div class="auth-footer">
          <span>还没有账号？</span>
          <el-button link type="primary" @click="router.push('/register')">创建账号</el-button>
        </div>
      </div>

      <div class="auth-visual" :style="{ backgroundImage: `linear-gradient(180deg, rgba(14, 24, 34, 0.2), rgba(14, 24, 34, 0.72)), url(${heroImage})` }">
        <div>
          <span>原型已就绪</span>
          <strong>完整商城购物体验</strong>
        </div>
      </div>
    </section>
  </main>
</template>
