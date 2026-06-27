<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CreditCard, PackageOpen, ShieldCheck, ShoppingBag, Trash2 } from 'lucide-vue-next'
import { getProductPresentation } from '@/data/prototype'
import { useCartStore } from '@/stores/cart'
import { money, parseSpec } from '@/utils/format'

const router = useRouter()
const cartStore = useCartStore()
const checkoutLoading = ref(false)
const remark = ref('请尽快发货')

const shippingFee = computed(() => (cartStore.totalAmount > 0 ? 0 : 0))
const payableAmount = computed(() => cartStore.totalAmount + shippingFee.value)

onMounted(() => {
  cartStore.load().catch((error) => {
    ElMessage.error(error instanceof Error ? error.message : '购物车加载失败')
  })
})

async function updateQuantity(skuId: number, quantity: number | undefined) {
  if (!quantity) {
    return
  }
  try {
    await cartStore.update(skuId, quantity)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新失败')
  }
}

async function remove(skuId: number) {
  try {
    await cartStore.remove(skuId)
    ElMessage.success('已移除商品')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

async function clear() {
  await ElMessageBox.confirm('确认清空购物车？', '清空购物车', { type: 'warning' })
  await cartStore.clear()
  ElMessage.success('购物车已清空')
}

async function checkout() {
  checkoutLoading.value = true
  try {
    const order = await cartStore.checkout(remark.value)
    ElMessage.success('订单已创建')
    router.push(`/orders/${order.orderNo}`)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '结算失败')
  } finally {
    checkoutLoading.value = false
  }
}
</script>

<template>
  <section class="shop-page">
    <div class="page-header commerce-page-header">
      <div>
        <span class="section-eyebrow">购物车</span>
        <h1>购物车</h1>
      </div>
      <div class="checkout-steps" aria-label="交易步骤">
        <span class="active">购物车</span>
        <span>确认订单</span>
        <span>准备支付</span>
      </div>
      <el-button :disabled="cartStore.items.length === 0" @click="clear">
        <Trash2 :size="17" />
        清空
      </el-button>
    </div>

    <el-skeleton :loading="cartStore.loading" animated :rows="8">
      <section v-if="cartStore.items.length === 0" class="empty-panel">
        <PackageOpen :size="48" />
        <h2>购物车还是空的</h2>
        <p>先去挑选几件商品，再回来完成结算。</p>
        <el-button type="primary" @click="router.push('/products')">
          <ShoppingBag :size="17" />
          去选购
        </el-button>
      </section>

      <div v-else class="cart-layout">
        <section class="cart-list" aria-label="购物车商品">
          <div class="cart-shop-title">
            <ShieldCheck :size="17" />
            <strong>星选自营旗舰店</strong>
            <span>正品保障 · 免运费</span>
          </div>
          <article v-for="item in cartStore.items" :key="item.skuId" class="cart-item">
            <div class="cart-thumb product-media" :class="getProductPresentation({ id: item.productId, name: item.productName }).tone">
              <div class="product-object">
                <span>{{ getProductPresentation({ id: item.productId, name: item.productName }).imageLabel }}</span>
              </div>
            </div>

            <div class="cart-item-main">
              <div>
                <h3>{{ item.productName }}</h3>
                <p>{{ item.skuCode }} · {{ parseSpec(item.specJson) }}</p>
              </div>
              <div class="cart-item-meta">
                <span>{{ money(item.price) }}</span>
                <el-input-number
                  :model-value="item.quantity"
                  :min="1"
                  :max="99"
                  size="small"
                  @change="(value: number | undefined) => updateQuantity(item.skuId, value)"
                />
                <strong>{{ money(item.totalAmount) }}</strong>
              </div>
            </div>

            <el-tooltip content="移除商品" placement="top">
              <el-button text type="danger" class="remove-button" @click="remove(item.skuId)">
                <Trash2 :size="16" />
              </el-button>
            </el-tooltip>
          </article>
        </section>

        <aside class="checkout-panel">
          <h2>订单汇总</h2>
          <div class="summary-row">
            <span>商品数量</span>
            <strong>{{ cartStore.totalQuantity }} 件</strong>
          </div>
          <div class="summary-row">
            <span>商品金额</span>
            <strong>{{ money(cartStore.totalAmount) }}</strong>
          </div>
          <div class="summary-row">
            <span>配送费</span>
            <strong>免运费</strong>
          </div>
          <el-input v-model="remark" type="textarea" :rows="3" placeholder="订单备注" />
          <div class="checkout-total">
            <span>应付合计</span>
            <strong>{{ money(payableAmount) }}</strong>
          </div>
          <el-button type="primary" size="large" :loading="checkoutLoading" @click="checkout">
            <CreditCard :size="18" />
            提交订单
          </el-button>
        </aside>
      </div>
    </el-skeleton>
  </section>
</template>
