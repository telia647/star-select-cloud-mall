import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ShellLayout from '@/layouts/ShellLayout.vue'
import ProductListView from '@/views/ProductListView.vue'
import ProductDetailView from '@/views/ProductDetailView.vue'
import CartView from '@/views/CartView.vue'
import OrderDetailView from '@/views/OrderDetailView.vue'
import PaymentResultView from '@/views/PaymentResultView.vue'
import SeckillView from '@/views/SeckillView.vue'
import AdminSeckillView from '@/views/AdminSeckillView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AiAssistantView from '@/views/AiAssistantView.vue'
import AdminAiKnowledgeView from '@/views/AdminAiKnowledgeView.vue'
import AdminAiLogsView from '@/views/AdminAiLogsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/products' },
    { path: '/login', name: 'login', component: LoginView, meta: { guest: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { guest: true } },
    {
      path: '/',
      component: ShellLayout,
      children: [
        { path: 'products', name: 'products', component: ProductListView },
        { path: 'products/:id', name: 'product-detail', component: ProductDetailView, props: true },
        { path: 'cart', name: 'cart', component: CartView },
        { path: 'orders/:orderNo', name: 'order-detail', component: OrderDetailView, props: true },
        { path: 'payments/:payNo', name: 'payment-result', component: PaymentResultView, props: true },
        { path: 'seckill', name: 'seckill', component: SeckillView },
        { path: 'ai/assistant', name: 'ai-assistant', component: AiAssistantView, meta: { auth: true } },
        { path: 'admin/seckill', name: 'admin-seckill', component: AdminSeckillView, meta: { auth: true, admin: true } },
        { path: 'admin/ai/knowledge', name: 'admin-ai-knowledge', component: AdminAiKnowledgeView, meta: { auth: true, admin: true } },
        { path: 'admin/ai/logs', name: 'admin-ai-logs', component: AdminAiLogsView, meta: { auth: true, admin: true } },
        { path: 'me', name: 'profile', component: ProfileView }
      ]
    }
  ]
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (to.meta.auth && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if ((to.meta.auth || to.meta.admin) && authStore.isLoggedIn && !authStore.user) {
    await authStore.loadMe().catch(() => undefined)
  }
  if (to.meta.admin && !authStore.isAdmin) {
    return { name: 'products' }
  }
  if (to.meta.guest && authStore.isLoggedIn) {
    return { name: 'products' }
  }
  return true
})

export default router
