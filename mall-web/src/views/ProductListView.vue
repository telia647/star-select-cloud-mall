<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  BadgePercent,
  ChevronRight,
  Crown,
  Gift,
  Search,
  ShieldCheck,
  ShoppingCart,
  SlidersHorizontal,
  Sparkles,
  Star,
  UserRound,
  Zap
} from 'lucide-vue-next'
import { listCategories, pageProducts } from '@/api/mall'
import heroImage from '@/assets/mall-hero.png'
import { getProductPresentation, showcaseStats } from '@/data/prototype'
import type { CategoryResponse, ProductListItem } from '@/types/api'
import { money } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const categories = ref<CategoryResponse[]>([])
const products = ref<ProductListItem[]>([])
const total = ref(0)
const sortMode = ref('recommend')
const query = reactive({
  pageNo: 1,
  pageSize: 8,
  categoryId: undefined as number | undefined,
  keyword: (route.query.keyword as string) || ''
})

const categoryTabs = computed(() => [{ id: undefined, name: '全部' }, ...categories.value])
const channelCards = computed(() => [
  { title: '限时秒杀', text: '整点开抢', action: '去抢购', path: '/seckill' },
  { title: '品质数码', text: '通勤办公精选', action: '看数码', categoryId: categories.value[0]?.id },
  { title: '居家焕新', text: '好物直降', action: '逛居家', categoryId: categories.value[1]?.id }
])
const displayProducts = computed(() => {
  const mapped = products.value.map((product, index) => ({
    product,
    meta: getProductPresentation(product, index)
  }))

  if (sortMode.value === 'price') {
    return [...mapped].sort((a, b) => a.meta.priceFrom - b.meta.priceFrom)
  }
  if (sortMode.value === 'new') {
    return [...mapped].reverse()
  }
  return mapped
})

onMounted(() => {
  loadCategories()
  loadProducts()
})

watch(
  () => route.query.keyword,
  (keyword) => {
    const nextKeyword = typeof keyword === 'string' ? keyword : ''
    if (query.keyword !== nextKeyword) {
      query.keyword = nextKeyword
      search()
    }
  }
)

async function loadCategories() {
  categories.value = await listCategories()
}

async function loadProducts() {
  loading.value = true
  try {
    const page = await pageProducts(query)
    products.value = page.records
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品加载失败')
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNo = 1
  loadProducts()
}

function selectCategory(categoryId?: number) {
  query.categoryId = categoryId
  search()
}

function goDetail(id: number) {
  router.push(`/products/${id}`)
}

function productImage(product: ProductListItem) {
  return product.mainImage || ''
}

function openChannel(card: { path?: string; categoryId?: number }) {
  if (card.path) {
    router.push(card.path)
    return
  }
  selectCategory(card.categoryId)
}
</script>

<template>
  <section class="shop-page">
    <section class="mall-hero">
      <div class="mall-hero-copy">
        <el-tag class="hero-kicker" effect="plain">
          <Sparkles :size="14" />
          星选好物节
        </el-tag>
        <h1>今天值得买的品质好物</h1>
        <p>秒杀、频道、商品流和购物车链路已经打通，适合展示真实 C 端商城的下单体验。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="selectCategory(undefined)">
            <ShoppingCart :size="18" />
            逛全部商品
          </el-button>
          <el-button class="hero-secondary" size="large" @click="router.push('/seckill')">
            <Zap :size="18" />
            限时秒杀
          </el-button>
        </div>
        <div class="hero-stats" aria-label="商城指标">
          <div v-for="item in showcaseStats" :key="item.label">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>

      <div class="mall-hero-visual">
        <img :src="heroImage" alt="精选商品组合展示" />
        <div class="hero-floating-panel">
          <span>今日会场</span>
          <strong>爆品直降</strong>
        </div>
      </div>
    </section>

    <section class="commerce-board" aria-label="商城频道">
      <aside class="category-rail">
        <div class="rail-title">主题频道</div>
        <button
          v-for="item in categoryTabs"
          :key="item.id ?? 'all-rail'"
          type="button"
          :class="{ active: query.categoryId === item.id }"
          @click="selectCategory(item.id)"
        >
          <span>{{ item.name }}</span>
          <ChevronRight :size="15" />
        </button>
      </aside>

      <div class="promo-tiles">
        <article v-for="card in channelCards" :key="card.title" class="promo-tile" @click="openChannel(card)">
          <span>{{ card.text }}</span>
          <strong>{{ card.title }}</strong>
          <button type="button">
            {{ card.action }}
            <ArrowRight :size="15" />
          </button>
        </article>
      </div>

      <aside class="member-card">
        <div class="member-avatar">
          <UserRound :size="26" />
        </div>
        <strong>星选会员权益</strong>
        <span>品质保障、极速履约、秒杀提醒</span>
        <div class="member-actions">
          <button type="button" @click="router.push('/login')">登录领取</button>
          <button type="button" @click="router.push('/me')">会员中心</button>
        </div>
        <div class="member-perks">
          <div>
            <Gift :size="17" />
            <span>专享券</span>
          </div>
          <div>
            <Crown :size="17" />
            <span>优先购</span>
          </div>
          <div>
            <ShieldCheck :size="17" />
            <span>正品保障</span>
          </div>
        </div>
      </aside>
    </section>

    <section class="catalog-section">
      <div class="catalog-heading">
        <div>
          <span class="section-eyebrow">猜你喜欢</span>
          <h2>为你推荐</h2>
        </div>
        <el-segmented
          v-model="sortMode"
          :options="[
            { label: '推荐', value: 'recommend' },
            { label: '新品', value: 'new' },
            { label: '价格', value: 'price' }
          ]"
        />
      </div>

      <div class="catalog-toolbar">
        <el-input v-model="query.keyword" clearable placeholder="搜索商品名称、卖点或场景" @keyup.enter="search">
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <el-button type="primary" @click="search">
          <SlidersHorizontal :size="17" />
          筛选
        </el-button>
      </div>

      <el-skeleton :loading="loading" animated :rows="8">
        <el-empty v-if="displayProducts.length === 0" description="没有找到匹配商品" />
        <div v-else class="product-grid">
          <article
            v-for="{ product, meta } in displayProducts"
            :key="product.id"
            class="product-card"
            @click="goDetail(product.id)"
          >
            <div class="product-media" :class="meta.tone">
              <span class="product-badge">{{ meta.accent }}</span>
              <img v-if="productImage(product)" class="product-image" :src="productImage(product)" :alt="product.name" />
              <div v-else class="product-object">
                <span>{{ meta.imageLabel }}</span>
              </div>
            </div>

            <div class="product-info">
              <div class="product-meta-line">
                <span>{{ product.shopName || meta.scene }}</span>
                <span class="rating">
                  <Star :size="14" />
                  {{ meta.rating }}
                </span>
              </div>
              <h3>{{ product.name }}</h3>
              <p>{{ product.subtitle }}</p>
              <div class="product-tags">
                <span class="coupon-tag">
                  <BadgePercent :size="13" />
                  满减
                </span>
                <span v-for="feature in meta.highlights.slice(0, 2)" :key="feature">{{ feature }}</span>
              </div>
              <div class="product-footer">
                <div>
                  <strong>{{ money(meta.priceFrom) }}</strong>
                  <span>{{ meta.delivery }} · {{ meta.marketTag }}</span>
                </div>
                <el-tooltip content="查看详情" placement="top">
                  <el-button circle type="primary" @click.stop="goDetail(product.id)">
                    <ArrowRight :size="17" />
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </article>
        </div>
      </el-skeleton>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="loadProducts"
        />
      </div>
    </section>
  </section>
</template>
