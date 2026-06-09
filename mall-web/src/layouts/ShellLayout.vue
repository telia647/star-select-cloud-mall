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
  { path: '/products', label: 'Catalog', icon: PackageSearch },
  { path: '/seckill', label: 'Flash Sale', icon: Flame },
  { path: '/cart', label: 'Cart', icon: ShoppingCart },
  { path: '/me', label: 'Account', icon: UserRound }
]

const visibleNavItems = computed(() => {
  const items = [...navItems]
  if (authStore.isAdmin) {
    items.push({ path: '/admin/seckill', label: 'Ops', icon: Settings })
  }
  return items
})

const username = computed(() => authStore.user?.username || (authStore.isLoggedIn ? 'Member' : 'Guest'))

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
</script>

<template>
  <div class="shop-shell">
    <header class="shop-header">
      <RouterLink class="shop-brand" to="/products" aria-label="NovaMall home">
        <span class="brand-mark">
          <Boxes :size="22" />
        </span>
        <span class="brand-copy">
          <strong>NovaMall</strong>
          <small>Production Mall</small>
        </span>
      </RouterLink>

      <nav class="shop-nav" aria-label="Mall navigation">
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
      </nav>

      <div class="shop-header-actions">
        <el-tag class="gateway-tag" type="success" effect="plain">
          <BadgeCheck :size="14" />
          /api connected
        </el-tag>

        <el-tooltip content="Cart" placement="bottom">
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
              <el-dropdown-item @click="router.push('/me')">Account Center</el-dropdown-item>
              <el-dropdown-item v-if="authStore.isAdmin" @click="router.push('/admin/seckill')">
                <Settings :size="15" />
                Seckill Ops
              </el-dropdown-item>
              <el-dropdown-item v-if="!authStore.isLoggedIn" @click="router.push('/login')">
                <LogIn :size="15" />
                Login
              </el-dropdown-item>
              <el-dropdown-item v-else divided @click="logout">
                <LogOut :size="15" />
                Logout
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="shop-main">
      <RouterView />
    </main>
  </div>
</template>
