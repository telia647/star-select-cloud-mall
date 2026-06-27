<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import {
  BadgeCheck,
  Boxes,
  Flame,
  LogIn,
  LogOut,
  PackageSearch,
  Search,
  Settings,
  ShoppingBag,
  ShoppingCart,
  UserRound
} from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const navItems = [
  { path: '/products', label: '商品', icon: PackageSearch },
  { path: '/seckill', label: '秒杀', icon: Flame },
  { path: '/cart', label: '购物车', icon: ShoppingCart },
  { path: '/me', label: '我的', icon: UserRound }
]

const visibleNavItems = computed(() => {
  const items = [...navItems]
  if (authStore.isAdmin) {
    items.push({ path: '/admin/seckill', label: '运营', icon: Settings })
  }
  return items
})

const username = computed(() => authStore.user?.username || (authStore.isLoggedIn ? '会员' : '游客'))
const hotWords = ['秒杀爆品', '降噪耳机', '通勤背包', '温控手冲壶']

onMounted(() => {
  authStore.loadMe().catch(() => undefined)
  cartStore.load().catch(() => undefined)
})

function isActive(path: string) {
  return route.path === path || route.path.startsWith(`${path}/`)
}

function logout() {
  authStore.logout()
  router.push('/products')
}

function goSearch(keyword = '') {
  router.push({ path: '/products', query: keyword ? { keyword } : undefined })
}
</script>

<template>
  <div class="shop-shell">
    <div class="shop-utility-bar">
      <div>
        <span>欢迎来到星选商城</span>
        <button v-if="!authStore.isLoggedIn" type="button" @click="router.push('/login')">请登录</button>
        <button v-else type="button" @click="router.push('/me')">{{ username }}</button>
      </div>
      <div>
        <button type="button" @click="router.push('/me')">我的订单</button>
        <button type="button" @click="router.push('/cart')">购物车 {{ cartStore.totalQuantity }}</button>
        <button v-if="authStore.isAdmin" type="button" @click="router.push('/admin/seckill')">商家运营</button>
      </div>
    </div>

    <header class="shop-header">
      <RouterLink class="shop-brand" to="/products" aria-label="星选商城首页">
        <span class="brand-mark">
          <Boxes :size="22" />
        </span>
        <span class="brand-copy">
          <strong>星选商城</strong>
          <small>生产级商城</small>
        </span>
      </RouterLink>

      <div class="shop-search">
        <div class="shop-search-box" role="search" @click="goSearch()">
          <Search :size="18" />
          <span>搜索商品、品牌、秒杀会场</span>
          <button type="button">搜索</button>
        </div>
        <div class="shop-hotwords" aria-label="热门搜索">
          <button v-for="word in hotWords" :key="word" type="button" @click="goSearch(word)">
            {{ word }}
          </button>
        </div>
      </div>

      <div class="shop-header-actions">
        <el-tag class="gateway-tag" type="success" effect="plain">
          <BadgeCheck :size="14" />
          接口已连接
        </el-tag>

        <el-tooltip content="购物车" placement="bottom">
          <el-button class="header-icon-button" circle @click="router.push('/cart')">
            <el-badge :value="cartStore.totalQuantity" :hidden="cartStore.totalQuantity === 0" :max="99">
              <ShoppingBag :size="18" />
            </el-badge>
          </el-button>
        </el-tooltip>

        <el-dropdown trigger="click">
          <el-button class="user-button">
            <UserRound :size="17" />
            <span>{{ username }}</span>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/me')">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" @click="router.push('/admin/seckill')">
                <Settings :size="15" />
                秒杀运营
              </el-dropdown-item>
              <el-dropdown-item v-if="!authStore.isLoggedIn" @click="router.push('/login')">
                <LogIn :size="15" />
                登录
              </el-dropdown-item>
              <el-dropdown-item v-else divided @click="logout">
                <LogOut :size="15" />
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <nav class="shop-channel-bar" aria-label="商城频道">
      <RouterLink
        v-for="item in visibleNavItems"
        :key="item.path"
        :to="item.path"
        class="shop-nav-link"
        :class="{ active: isActive(item.path) }"
      >
        <component :is="item.icon" :size="17" />
        <span>{{ item.label }}</span>
      </RouterLink>
      <span class="channel-divider"></span>
      <RouterLink class="shop-nav-link highlight" to="/seckill">
        <Flame :size="17" />
        <span>今日限时抢</span>
      </RouterLink>
    </nav>

    <main class="shop-main">
      <RouterView />
    </main>
  </div>
</template>
