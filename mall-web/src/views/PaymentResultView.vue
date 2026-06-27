<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { BadgeCheck, FileText, ShoppingBag } from 'lucide-vue-next'
import { getPayment } from '@/api/mall'
import type { PaymentResponse } from '@/types/api'
import { money, paymentStatus } from '@/utils/format'

const props = defineProps<{ payNo: string }>()
const router = useRouter()
const payment = ref<PaymentResponse | null>(null)
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    payment.value = await getPayment(props.payNo)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '支付结果加载失败')
  } finally {
    loading.value = false
  }
}

function payChannelText(channel: string) {
  if (channel === 'MOCK') {
    return '模拟支付'
  }
  return channel || '-'
}
</script>

<template>
  <section class="shop-page centered-page">
    <el-skeleton :loading="loading" animated :rows="6">
      <section v-if="payment" class="result-panel">
        <div class="result-icon">
          <BadgeCheck :size="34" />
        </div>
        <h1>{{ paymentStatus(payment.status) }}</h1>
        <p>{{ payment.payNo }}</p>
        <div class="summary-band compact">
          <div>
            <span>订单号</span>
            <strong>{{ payment.orderNo }}</strong>
          </div>
          <div>
            <span>金额</span>
            <strong>{{ money(payment.amount) }}</strong>
          </div>
          <div>
            <span>渠道</span>
            <strong>{{ payChannelText(payment.payChannel) }}</strong>
          </div>
        </div>
        <div class="action-row centered">
          <el-button @click="router.push('/products')">
            <ShoppingBag :size="17" />
            继续购物
          </el-button>
          <el-button type="primary" @click="router.push(`/orders/${payment.orderNo}`)">
            <FileText :size="17" />
            查看订单
          </el-button>
        </div>
      </section>
    </el-skeleton>
  </section>
</template>
