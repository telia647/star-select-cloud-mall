<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, BadgeCheck, Heart, PackageCheck, ShieldCheck, ShoppingBag, ShoppingCart, Star, Truck } from 'lucide-vue-next'
import { getProduct } from '@/api/mall'
import { getProductPresentation } from '@/data/prototype'
import { useCartStore } from '@/stores/cart'
import type { ProductDetail, SkuResponse } from '@/types/api'
import { money, parseSpec } from '@/utils/format'

const props = defineProps<{ id: string }>()
const router = useRouter()
const cartStore = useCartStore()
const product = ref<ProductDetail | null>(null)
const selectedSkuId = ref<number>()
const quantity = ref(1)
const loading = ref(false)
const adding = ref(false)

const selectedSku = computed(() => product.value?.skus.find((item) => item.id === selectedSkuId.value))
const meta = computed(() => (product.value ? getProductPresentation(product.value) : null))
const selectedSpec = computed(() => parseSpec(selectedSku.value?.specJson))
const galleryImages = computed(() => {
  if (!product.value) {
    return []
  }
  const images = parseGalleryImages(product.value.galleryImages)
  if (product.value.mainImage && !images.includes(product.value.mainImage)) {
    images.unshift(product.value.mainImage)
  }
  return images
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    product.value = await getProduct(Number(props.id))
    selectedSkuId.value = product.value.skus[0]?.id
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败')
  } finally {
    loading.value = false
  }
}

async function addToCart(redirect = false) {
  if (!selectedSku.value) {
    ElMessage.warning('请选择 SKU')
    return
  }
  adding.value = true
  try {
    await cartStore.add(selectedSku.value.id, quantity.value)
    ElMessage.success('已加入购物车')
    if (redirect) {
      router.push('/cart')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加入购物车失败')
  } finally {
    adding.value = false
  }
}

function skuLabel(sku: SkuResponse) {
  const spec = parseSpec(sku.specJson)
  return spec ? `${sku.skuCode} · ${spec}` : sku.skuCode
}
function parseGalleryImages(value?: string | null) {
  if (!value) {
    return []
  }
  try {
    const parsed = JSON.parse(value) as unknown
    return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === 'string' && item.length > 0) : []
  } catch {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }
}
</script>

<template>
  <section class="shop-page">
    <el-button text class="back-button" @click="router.push('/products')">
      <ArrowLeft :size="17" />
      返回商品
    </el-button>

    <el-skeleton :loading="loading" animated :rows="10">
      <div v-if="product && meta" class="detail-layout">
        <section class="detail-gallery">
          <div class="detail-media product-media" :class="meta.tone">
            <span class="product-badge">{{ meta.badge }}</span>
            <img v-if="product.mainImage" class="product-image detail-image" :src="product.mainImage" :alt="product.name" />
            <div v-else class="product-object detail-object">
              <span>{{ meta.imageLabel }}</span>
            </div>
          </div>
          <div v-if="galleryImages.length > 0" class="gallery-thumbs gallery-image-thumbs">
            <button v-for="image in galleryImages" :key="image" type="button">
              <img :src="image" :alt="product.name" />
            </button>
          </div>
          <div v-else class="gallery-thumbs">
            <button v-for="feature in meta.highlights" :key="feature" type="button">
              {{ feature }}
            </button>
          </div>
        </section>

        <section class="detail-panel">
          <div class="detail-kicker">
            <el-tag effect="plain">{{ meta.marketTag }}</el-tag>
            <span>
              <Star :size="15" />
              {{ meta.rating }} 用户评分
            </span>
          </div>

          <h1>{{ product.name }}</h1>
          <div v-if="product.shopName" class="detail-shop-name">{{ product.shopName }}</div>
          <p class="detail-subtitle">{{ product.subtitle }}</p>

          <div class="price-panel">
            <span>到手价</span>
            <strong>{{ money(selectedSku?.price || meta.priceFrom) }}</strong>
            <small>{{ selectedSpec }}</small>
          </div>

          <div class="purchase-block">
            <div class="purchase-label">选择规格</div>
            <div class="sku-list">
              <button
                v-for="sku in product.skus"
                :key="sku.id"
                type="button"
                class="sku-option"
                :class="{ active: sku.id === selectedSkuId }"
                @click="selectedSkuId = sku.id"
              >
                <span>{{ skuLabel(sku) }}</span>
                <strong>{{ money(sku.price) }}</strong>
              </button>
            </div>
          </div>

          <div class="purchase-block">
            <div class="purchase-label">购买数量</div>
            <el-input-number v-model="quantity" :min="1" :max="99" />
          </div>

          <div class="detail-actions">
            <el-button :loading="adding" size="large" @click="addToCart(false)">
              <ShoppingCart :size="18" />
              加入购物车
            </el-button>
            <el-button type="primary" :loading="adding" size="large" @click="addToCart(true)">
              <ShoppingBag :size="18" />
              立即购买
            </el-button>
          </div>

          <div class="assurance-grid">
            <div>
              <ShieldCheck :size="19" />
              <span>正品保障</span>
            </div>
            <div>
              <Truck :size="19" />
              <span>{{ meta.delivery }}</span>
            </div>
            <div>
              <PackageCheck :size="19" />
              <span>七天无理由</span>
            </div>
            <div>
              <Heart :size="19" />
              <span>收藏好物</span>
            </div>
          </div>
        </section>
      </div>

      <section v-if="product && meta" class="story-band">
        <div>
          <BadgeCheck :size="22" />
          <strong>为什么值得买</strong>
        </div>
        <p>{{ meta.highlights.join('、') }}，覆盖从下单到售后的完整商城体验。</p>
      </section>
    </el-skeleton>
  </section>
</template>
