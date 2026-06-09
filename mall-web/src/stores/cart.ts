import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { addCartItem, checkoutCart, clearCartItems, listCartItems, removeCartItem, updateCartItem } from '@/api/mall'
import type { CartItem, OrderCreateResponse } from '@/types/api'

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])
  const loading = ref(false)

  const totalAmount = computed(() => items.value.reduce((sum, item) => sum + Number(item.totalAmount), 0))
  const totalQuantity = computed(() => items.value.reduce((sum, item) => sum + Number(item.quantity), 0))

  async function load() {
    loading.value = true
    try {
      items.value = await listCartItems()
    } finally {
      loading.value = false
    }
  }

  async function add(skuId: number, quantity: number) {
    await addCartItem({ skuId, quantity })
    await load()
  }

  async function update(skuId: number, quantity: number) {
    await updateCartItem(skuId, { quantity })
    await load()
  }

  async function remove(skuId: number) {
    await removeCartItem(skuId)
    await load()
  }

  async function clear() {
    await clearCartItems()
    items.value = []
  }

  async function checkout(remark?: string): Promise<OrderCreateResponse> {
    const response = await checkoutCart({ remark, requestId: nextRequestId() })
    items.value = []
    return response
  }

  function nextRequestId() {
    if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
      return crypto.randomUUID()
    }
    return `cart-${Date.now()}-${Math.random().toString(16).slice(2)}`
  }

  return {
    items,
    loading,
    totalAmount,
    totalQuantity,
    load,
    add,
    update,
    remove,
    clear,
    checkout
  }
})
