<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell, Clock3, Flame, Hourglass, PackageCheck, RotateCcw, Zap } from 'lucide-vue-next'
import { getSeckillResult, issueSeckillToken, listSeckillItems, listSeckillSessions, submitSeckill } from '@/api/mall'
import { useAuthStore } from '@/stores/auth'
import type { SeckillCreateResponse, SeckillItem, SeckillSession } from '@/types/api'
import { money } from '@/utils/format'

const router = useRouter()
const authStore = useAuthStore()
const sessions = ref<SeckillSession[]>([])
const items = ref<SeckillItem[]>([])
const activeSessionId = ref<number>()
const loading = ref(false)
const buyingItemId = ref<number>()
const result = ref<SeckillCreateResponse | null>(null)

const activeSession = computed(() => sessions.value.find((item) => item.id === activeSessionId.value))
const runningCount = computed(() => items.value.filter((item) => item.state === 'RUNNING' && item.availableStock > 0).length)

onMounted(loadSessions)

async function loadSessions() {
  loading.value = true
  try {
    sessions.value = await listSeckillSessions()
    activeSessionId.value = pickDefaultSession(sessions.value)?.id
    if (activeSessionId.value) {
      await loadItems(activeSessionId.value)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '秒杀场次加载失败')
  } finally {
    loading.value = false
  }
}

async function loadItems(sessionId: number) {
  activeSessionId.value = sessionId
  loading.value = true
  result.value = null
  try {
    items.value = await listSeckillItems(sessionId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '秒杀商品加载失败')
  } finally {
    loading.value = false
  }
}

async function buy(item: SeckillItem) {
  if (item.state !== 'RUNNING' || item.availableStock <= 0) {
    ElMessage.warning(item.state === 'UPCOMING' ? '该场次尚未开始' : '该商品已结束')
    return
  }
  if (!authStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: '/seckill' } })
    return
  }

  buyingItemId.value = item.id
  result.value = null
  try {
    const token = await issueSeckillToken({
      activityId: item.activityId,
      sessionId: item.sessionId,
      skuId: item.skuId,
      quantity: 1
    })
    const response = await submitSeckill({
      activityId: item.activityId,
      sessionId: item.sessionId,
      skuId: item.skuId,
      quantity: 1,
      token: token.token,
      requestId: nextRequestId()
    })
    result.value = response
    if (response.status === 'ACCEPTED') {
      await pollResult(response.requestId)
    }
    if (result.value?.status === 'CREATED' && result.value.orderNo) {
      ElMessage.success('抢购成功，订单已创建')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '抢购失败')
  } finally {
    buyingItemId.value = undefined
  }
}

async function pollResult(requestId: string) {
  for (let index = 0; index < 5; index += 1) {
    await wait(1200)
    const response = await getSeckillResult(requestId)
    result.value = response
    if (response.status !== 'ACCEPTED') {
      return
    }
  }
}

function pickDefaultSession(values: SeckillSession[]) {
  return values.find((item) => item.state === 'RUNNING') || values.find((item) => item.state === 'UPCOMING') || values[0]
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

function nextRequestId() {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `sk-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function stateLabel(state: SeckillSession['state']) {
  const map = {
    UPCOMING: '即将开始',
    RUNNING: '正在抢购',
    ENDED: '已结束'
  }
  return map[state]
}

function buttonLabel(item: SeckillItem) {
  if (item.availableStock <= 0) {
    return '已抢光'
  }
  if (item.state === 'UPCOMING') {
    return '提醒我'
  }
  if (item.state === 'ENDED') {
    return '已结束'
  }
  return '立即抢购'
}

function resultMessageText(message: string) {
  const map: Record<string, string> = {
    'seckill request accepted': '请求已接收，订单正在创建中。',
    'seckill order created': '秒杀成功，订单已创建。',
    'seckill order create failed': '秒杀订单创建失败。'
  }
  return map[message] || message
}

function seckillImage(item: SeckillItem) {
  const text = `${item.productName} ${item.skuCode}`.toLowerCase()
  if (text.includes('headset') || text.includes('ear') || item.skuId === 3001 || item.skuId === 3010) {
    return '/demo-products/headset.svg'
  }
  if (text.includes('runner') || item.skuId === 3031 || item.skuId === 3016) {
    return '/demo-products/running-shoes.svg'
  }
  if (text.includes('aroma') || item.skuId === 3041 || item.skuId === 3018) {
    return '/demo-products/aroma.svg'
  }
  if (text.includes('bag') || item.skuId === 3051 || item.skuId === 3020) {
    return '/demo-products/backpack.svg'
  }
  if (text.includes('speaker') || item.skuId === 3061 || item.skuId === 3022) {
    return '/demo-products/speaker.svg'
  }
  return '/demo-products/phone.svg'
}

function formatTime(value: string) {
  return new Date(value).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <section class="shop-page">
    <section class="seckill-hero">
      <div>
        <el-tag type="danger" effect="dark">
          <Zap :size="14" />
          整点开抢
        </el-tag>
        <h1>星选秒杀会场</h1>
        <p>按场次开抢，Redis 预扣库存、MQ 异步建单、订单支付倒计时，面向高并发 C 端抢购链路。</p>
        <div class="seckill-hero-stats">
          <div>
            <strong>{{ sessions.length }}</strong>
            <span>今日场次</span>
          </div>
          <div>
            <strong>{{ runningCount }}</strong>
            <span>可抢商品</span>
          </div>
          <div>
            <strong>1件</strong>
            <span>单用户限购</span>
          </div>
        </div>
      </div>
      <div class="seckill-hero-card">
        <Flame :size="42" />
        <span>当前热度</span>
        <strong>{{ runningCount > 0 ? '抢购中' : '等待开场' }}</strong>
      </div>
    </section>

    <section class="seckill-session-bar" aria-label="秒杀场次">
      <button
        v-for="session in sessions"
        :key="session.id"
        type="button"
        class="seckill-session"
        :class="{ active: session.id === activeSessionId, running: session.state === 'RUNNING' }"
        @click="loadItems(session.id)"
      >
        <strong>{{ session.name }}</strong>
        <span>{{ formatTime(session.startTime) }} - {{ formatTime(session.endTime) }}</span>
        <em>{{ stateLabel(session.state) }}</em>
      </button>
    </section>

    <section v-if="activeSession" class="seckill-notice">
      <Clock3 :size="18" />
      <span>{{ activeSession.name }} · {{ stateLabel(activeSession.state) }} · 抢购成功后进入订单准备支付页</span>
      <el-button text @click="loadItems(activeSession.id)">
        <RotateCcw :size="16" />
        刷新
      </el-button>
    </section>

    <el-skeleton :loading="loading" animated :rows="8">
      <el-empty v-if="items.length === 0" description="当前场次暂无秒杀商品" />

      <div v-else class="seckill-grid">
        <article v-for="item in items" :key="item.id" class="seckill-card">
          <div class="seckill-card-media">
            <span>{{ item.badge }}</span>
            <div>
              <img :src="seckillImage(item)" :alt="item.productName" />
              <strong>{{ item.productName.slice(0, 4) }}</strong>
            </div>
          </div>

          <div class="seckill-card-body">
            <div class="seckill-card-head">
              <el-tag :type="item.state === 'RUNNING' ? 'danger' : item.state === 'UPCOMING' ? 'warning' : 'info'" effect="plain">
                {{ stateLabel(item.state) }}
              </el-tag>
              <span>限购 {{ item.limitPerUser }} 件</span>
            </div>
            <h2>{{ item.productName }}</h2>
            <p>{{ item.subtitle }}</p>

            <div class="seckill-price-row">
              <div>
                <small>秒杀价</small>
                <strong>{{ money(item.seckillPrice) }}</strong>
              </div>
              <span>日常价 {{ money(item.originalPrice) }}</span>
            </div>

            <div class="seckill-stock">
              <div>
                <span>已抢 {{ item.soldPercent }}%</span>
                <span>剩余 {{ item.availableStock }}</span>
              </div>
              <el-progress :percentage="item.soldPercent" :show-text="false" :stroke-width="8" />
            </div>

            <el-button
              type="primary"
              size="large"
              :disabled="item.state !== 'RUNNING' || item.availableStock <= 0"
              :loading="buyingItemId === item.id"
              @click="buy(item)"
            >
              <PackageCheck v-if="item.state === 'RUNNING'" :size="18" />
              <Bell v-else-if="item.state === 'UPCOMING'" :size="18" />
              <Hourglass v-else :size="18" />
              {{ buttonLabel(item) }}
            </el-button>
          </div>
        </article>
      </div>
    </el-skeleton>

    <section v-if="result" class="seckill-result-panel">
      <el-tag :type="result.status === 'FAILED' ? 'danger' : result.status === 'CREATED' ? 'success' : 'warning'">
        {{ result.status === 'CREATED' ? '已创建订单' : result.status === 'FAILED' ? '抢购失败' : '处理中' }}
      </el-tag>
      <div>
        <strong>{{ resultMessageText(result.message) }}</strong>
        <span>{{ result.requestId }}</span>
      </div>
      <el-button v-if="result.orderNo" type="primary" @click="router.push(`/orders/${result.orderNo}`)">
        查看订单
      </el-button>
    </section>
  </section>
</template>
