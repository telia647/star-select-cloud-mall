<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Ban, CreditCard, PackageCheck, RotateCcw } from 'lucide-vue-next'
import { cancelOrder, getOrder, payOrder } from '@/api/mall'
import type { OrderDetail } from '@/types/api'
import { money, orderStatus, parseSpec } from '@/utils/format'

const props = defineProps<{ orderNo: string }>()
const router = useRouter()
const order = ref<OrderDetail | null>(null)
const loading = ref(false)
const paying = ref(false)
const canceling = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    order.value = await getOrder(props.orderNo)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单加载失败')
  } finally {
    loading.value = false
  }
}

async function pay() {
  paying.value = true
  try {
    const payment = await payOrder({ orderNo: props.orderNo, payChannel: 'MOCK' })
    ElMessage.success('支付成功')
    router.push(`/payments/${payment.payNo}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '支付失败')
  } finally {
    paying.value = false
  }
}

async function cancel() {
  await ElMessageBox.confirm('确认取消该订单？', '取消订单', { type: 'warning' })
  canceling.value = true
  try {
    await cancelOrder(props.orderNo)
    ElMessage.success('订单已取消')
    await load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '取消失败')
  } finally {
    canceling.value = false
  }
}
</script>

<template>
  <section class="shop-page">
    <div class="page-header commerce-page-header">
      <div>
        <span class="section-eyebrow">订单</span>
        <h1>订单详情</h1>
        <p>订单号 {{ props.orderNo }}</p>
      </div>
      <el-button @click="load">
        <RotateCcw :size="17" />
        刷新
      </el-button>
    </div>

    <el-skeleton :loading="loading" animated :rows="8">
      <div v-if="order" class="order-layout">
        <section class="order-status-card">
          <div class="order-status-icon">
            <PackageCheck :size="30" />
          </div>
          <div>
            <span>订单状态</span>
            <strong>{{ orderStatus(order.status) }}</strong>
          </div>
          <div>
            <span>应付金额</span>
            <strong>{{ money(order.totalAmount) }}</strong>
          </div>
          <div>
            <span>支付单号</span>
            <strong>{{ order.payNo || '-' }}</strong>
          </div>
        </section>

        <section class="order-goods-card">
          <div class="cart-shop-title">
            <PackageCheck :size="17" />
            <strong>星选自营旗舰店</strong>
            <span>订单商品</span>
          </div>
          <article v-for="item in order.items" :key="item.skuId" class="order-good-row">
            <div>
              <strong>{{ item.productName }}</strong>
              <span>{{ item.skuCode }} · {{ parseSpec(item.specJson) }}</span>
            </div>
            <span>{{ money(item.price) }}</span>
            <span>x{{ item.quantity }}</span>
            <strong>{{ money(item.totalAmount) }}</strong>
          </article>
        </section>

        <div class="action-row">
          <el-button :disabled="order.status !== 10" :loading="canceling" @click="cancel">
            <Ban :size="17" />
            取消订单
          </el-button>
          <el-button type="primary" :disabled="order.status !== 10" :loading="paying" @click="pay">
            <CreditCard :size="17" />
            模拟支付
          </el-button>
        </div>
      </div>
    </el-skeleton>
  </section>
</template>
