<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BadgeCheck, CreditCard, Gift, PackageCheck, RefreshCw, ShieldCheck, ShoppingBag, UserRound } from 'lucide-vue-next'
import { listMemberBenefits, listMemberCoupons, listMyOrders } from '@/api/mall'
import { useAuthStore } from '@/stores/auth'
import type { MemberBenefit, MemberCoupon, OrderListItem } from '@/types/api'
import { money, orderStatus } from '@/utils/format'

const authStore = useAuthStore()
const router = useRouter()
const loading = ref(false)
const benefits = ref<MemberBenefit[]>([])
const coupons = ref<MemberCoupon[]>([])
const orders = ref<OrderListItem[]>([])

onMounted(load)

async function load() {
  loading.value = true
  try {
    await authStore.loadMe()
    const [benefitValues, couponValues, orderPage] = await Promise.all([
      listMemberBenefits(),
      listMemberCoupons(),
      listMyOrders({ pageNo: 1, pageSize: 3 })
    ])
    benefits.value = benefitValues
    coupons.value = couponValues
    orders.value = orderPage.records
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户信息加载失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="shop-page">
    <div class="page-header commerce-page-header">
      <div>
        <span class="section-eyebrow">账户</span>
        <h1>会员中心</h1>
        <p>管理账号、订单和星选商城服务。</p>
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
          星选商城会员
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
      <div class="profile-quick-actions">
        <button type="button" @click="router.push('/products')">
          <ShoppingBag :size="18" />
          <span>继续购物</span>
        </button>
        <button type="button" @click="router.push('/cart')">
          <CreditCard :size="18" />
          <span>购物车</span>
        </button>
        <button type="button" @click="router.push('/seckill')">
          <PackageCheck :size="18" />
          <span>秒杀订单</span>
        </button>
        <button type="button" @click="router.push('/seckill')">
          <Gift :size="18" />
          <span>会员券</span>
        </button>
      </div>

      <section class="member-dashboard">
        <div class="member-dashboard-section">
          <div class="member-section-title">
            <strong>会员权益</strong>
            <span>{{ benefits.length }} 项</span>
          </div>
          <div class="member-benefit-list">
            <article v-for="item in benefits" :key="item.code">
              <strong>{{ item.title }}</strong>
              <span>{{ item.description }}</span>
            </article>
          </div>
        </div>

        <div class="member-dashboard-section">
          <div class="member-section-title">
            <strong>我的券包</strong>
            <span>{{ coupons.length }} 张可用</span>
          </div>
          <div class="coupon-list">
            <article v-for="item in coupons" :key="item.id">
              <strong>{{ money(item.discountAmount) }}</strong>
              <span>{{ item.couponName }}</span>
              <small>{{ item.thresholdAmount > 0 ? `满 ${money(item.thresholdAmount)} 可用` : '无门槛' }}</small>
            </article>
          </div>
        </div>
      </section>

      <section class="member-order-panel">
        <div class="member-section-title">
          <strong>最近订单</strong>
          <button type="button" @click="router.push('/cart')">去结算</button>
        </div>
        <el-empty v-if="orders.length === 0" description="暂无订单" />
        <article v-for="item in orders" v-else :key="item.orderNo" class="member-order-row" @click="router.push(`/orders/${item.orderNo}`)">
          <div>
            <strong>{{ item.firstProductName || item.orderNo }}</strong>
            <span>{{ item.orderNo }} · {{ item.itemCount }} 件 · {{ orderStatus(item.status) }}</span>
          </div>
          <strong>{{ money(item.totalAmount) }}</strong>
        </article>
      </section>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户 ID">{{ authStore.user?.id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">{{ authStore.user?.status === 1 ? '正常' : authStore.user?.status || '-' }}</el-descriptions-item>
      </el-descriptions>
    </section>
  </section>
</template>
