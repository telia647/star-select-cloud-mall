<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Heart,
  Search,
  ShieldCheck,
  ShoppingCart,
  SlidersHorizontal,
  Sparkles,
  Star,
  Truck,
  Zap
} from 'lucide-vue-next'
import { listCategories, pageProducts } from '@/api/mall'
import heroImage from '@/assets/mall-hero.png'
import { getProductPresentation, servicePromises, showcaseStats } from '@/data/prototype'
import type { CategoryResponse, ProductListItem } from '@/types/api'
import { money } from '@/utils/format'

const router = useRouter()
const loading = ref(false)
const categories = ref<CategoryResponse[]>([])
const products = ref<ProductListItem[]>([])
const total = ref(0)
const sortMode = ref('recommend')
const query = reactive({
  pageNo: 1,
  pageSize: 8,
  categoryId: undefined as number | undefined,
  keyword: ''
})

const categoryTabs = computed(() => [{ id: undefined, name: '全部' }, ...categories.value])
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
</script>

<template>
  <section class="shop-page">
    <section class="mall-hero">
      <div class="mall-hero-copy">
        <el-tag class="hero-kicker" effect="plain">
          <Sparkles :size="14" />
          今日严选
        </el-tag>
        <h1>NovaMall 精选好物</h1>
        <p>把数码、居家、美妆、运动和通勤单品放进一个清爽高效的购物体验里。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="selectCategory(undefined)">
            <ShoppingCart :size="18" />
            立即选购
          </el-button>
          <el-button size="large" @click="router.push('/seckill')">
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
          <span>本周热度</span>
          <strong>98%</strong>
        </div>
      </div>
    </section>

    <section class="service-strip" aria-label="服务承诺">
      <div v-for="(item, index) in servicePromises" :key="item.title" class="service-item">
        <component :is="[ShieldCheck, Truck, Heart][index]" :size="19" />
        <div>
          <strong>{{ item.title }}</strong>
          <span>{{ item.text }}</span>
        </div>
      </div>
    </section>

    <section class="catalog-section">
      <div class="catalog-heading">
        <div>
          <span class="section-eyebrow">Catalog</span>
          <h2>正在上新的商品</h2>
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

      <div class="category-tabs">
        <button
          v-for="item in categoryTabs"
          :key="item.id ?? 'all'"
          type="button"
          class="category-pill"
          :class="{ active: query.categoryId === item.id }"
          @click="selectCategory(item.id)"
        >
          {{ item.name }}
        </button>
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
              <div class="product-object">
                <span>{{ meta.imageLabel }}</span>
              </div>
            </div>

            <div class="product-info">
              <div class="product-meta-line">
                <span>{{ meta.scene }}</span>
                <span class="rating">
                  <Star :size="14" />
                  {{ meta.rating }}
                </span>
              </div>
              <h3>{{ product.name }}</h3>
              <p>{{ product.subtitle }}</p>
              <div class="product-tags">
                <span v-for="feature in meta.highlights.slice(0, 2)" :key="feature">{{ feature }}</span>
              </div>
              <div class="product-footer">
                <div>
                  <strong>{{ money(meta.priceFrom) }}</strong>
                  <span>{{ meta.delivery }}</span>
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
