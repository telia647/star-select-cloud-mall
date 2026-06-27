<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { DatabaseZap, Flame, RefreshCcw, Save, Search, Settings, Timer, Wand2 } from 'lucide-vue-next'
import {
  initSeckillStock,
  listAdminActivities,
  listAdminItems,
  listAdminOperationLogs,
  listAdminSessions,
  listOrderStatusLogs,
  listStockFlows,
  saveAdminActivity,
  saveAdminItem,
  saveAdminSession
} from '@/api/mall'
import type {
  OrderStatusLogResponse,
  PromotionActivityAdmin,
  PromotionOperationLog,
  PromotionSeckillSkuAdmin,
  PromotionSessionAdmin,
  StockFlowResponse
} from '@/types/api'
import { money } from '@/utils/format'

const activities = ref<PromotionActivityAdmin[]>([])
const sessions = ref<PromotionSessionAdmin[]>([])
const items = ref<PromotionSeckillSkuAdmin[]>([])
const operationLogs = ref<PromotionOperationLog[]>([])
const orderStatusLogs = ref<OrderStatusLogResponse[]>([])
const stockFlows = ref<StockFlowResponse[]>([])
const activeActivityId = ref<number>()
const activeSessionId = ref<number>()
const loading = ref(false)
const saving = ref(false)
const orderLogLoading = ref(false)
const preheatingItemId = ref<number>()
const orderLogQuery = ref('')

const activityForm = reactive<Partial<PromotionActivityAdmin>>(emptyActivity())
const sessionForm = reactive<Partial<PromotionSessionAdmin>>(emptySession())
const itemForm = reactive<Partial<PromotionSeckillSkuAdmin>>(emptyItem())

const activeActivity = computed(() => activities.value.find((item) => item.id === activeActivityId.value))
const activeSession = computed(() => sessions.value.find((item) => item.id === activeSessionId.value))
const enabledItemCount = computed(() => items.value.filter((item) => item.status === 1).length)
const totalAvailableStock = computed(() => items.value.reduce((sum, item) => sum + Number(item.availableStock || 0), 0))

onMounted(loadActivities)

async function loadActivities() {
  loading.value = true
  try {
    activities.value = await listAdminActivities()
    if (!activeActivityId.value && activities.value.length > 0) {
      activeActivityId.value = activities.value[0].id
    }
    if (activeActivityId.value) {
      await loadSessions(activeActivityId.value)
    }
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '活动加载失败')
  } finally {
    loading.value = false
  }
}

async function loadOperationLogs() {
  operationLogs.value = await listAdminOperationLogs()
}

async function queryOrderStatusLogs() {
  const orderNo = orderLogQuery.value.trim()
  if (!orderNo) {
    ElMessage.warning('请输入订单号')
    return
  }
  orderLogLoading.value = true
  try {
    const [statusLogs, flows] = await Promise.all([listOrderStatusLogs(orderNo), listStockFlows(orderNo)])
    orderStatusLogs.value = statusLogs
    stockFlows.value = flows
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '订单诊断加载失败')
  } finally {
    orderLogLoading.value = false
  }
}

async function loadSessions(activityId: number) {
  activeActivityId.value = activityId
  sessions.value = await listAdminSessions(activityId)
  activeSessionId.value = sessions.value[0]?.id
  Object.assign(sessionForm, emptySession(activityId))
  Object.assign(itemForm, emptyItem(activityId, activeSessionId.value))
  if (activeSessionId.value) {
    await loadItems(activeSessionId.value)
  } else {
    items.value = []
  }
}

async function loadItems(sessionId: number) {
  activeSessionId.value = sessionId
  items.value = await listAdminItems(sessionId)
  Object.assign(itemForm, emptyItem(activeActivityId.value, sessionId))
}

function editActivity(row: PromotionActivityAdmin) {
  Object.assign(activityForm, row)
}

function newActivity() {
  Object.assign(activityForm, emptyActivity())
}

async function submitActivity() {
  saving.value = true
  try {
    const response = await saveAdminActivity(activityForm)
    activeActivityId.value = response.id
    ElMessage.success('活动已保存')
    await loadActivities()
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '活动保存失败')
  } finally {
    saving.value = false
  }
}

function editSession(row: PromotionSessionAdmin) {
  Object.assign(sessionForm, row)
}

function newSession() {
  Object.assign(sessionForm, emptySession(activeActivityId.value))
}

async function submitSession() {
  if (!activeActivityId.value) {
    ElMessage.warning('请先选择活动')
    return
  }
  saving.value = true
  try {
    const response = await saveAdminSession({ ...sessionForm, activityId: activeActivityId.value })
    activeSessionId.value = response.id
    ElMessage.success('场次已保存')
    await loadSessions(activeActivityId.value)
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '场次保存失败')
  } finally {
    saving.value = false
  }
}

function editItem(row: PromotionSeckillSkuAdmin) {
  Object.assign(itemForm, row)
}

function newItem() {
  Object.assign(itemForm, emptyItem(activeActivityId.value, activeSessionId.value))
}

async function submitItem() {
  if (!activeActivityId.value || !activeSessionId.value) {
    ElMessage.warning('请先选择活动和场次')
    return
  }
  saving.value = true
  try {
    await saveAdminItem({ ...itemForm, activityId: activeActivityId.value, sessionId: activeSessionId.value })
    ElMessage.success('商品已保存')
    await loadItems(activeSessionId.value)
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品保存失败')
  } finally {
    saving.value = false
  }
}

async function preheatStock(row: PromotionSeckillSkuAdmin) {
  preheatingItemId.value = row.id
  try {
    await initSeckillStock({
      activityId: row.activityId,
      sessionId: row.sessionId,
      skuId: row.skuId,
      quantity: row.availableStock
    })
    ElMessage.success('Redis 库存已预热')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '库存预热失败')
  } finally {
    preheatingItemId.value = undefined
  }
}

function emptyActivity(): Partial<PromotionActivityAdmin> {
  return {
    name: '',
    title: '',
    description: '',
    status: 1
  }
}

function emptySession(activityId = activeActivityId.value): Partial<PromotionSessionAdmin> {
  return {
    activityId,
    name: '',
    startTime: toLocalDateTimeInput(new Date(Date.now() + 60 * 60 * 1000)),
    endTime: toLocalDateTimeInput(new Date(Date.now() + 3 * 60 * 60 * 1000)),
    status: 1,
    sort: sessions.value.length + 1
  }
}

function emptyItem(activityId = activeActivityId.value, sessionId = activeSessionId.value): Partial<PromotionSeckillSkuAdmin> {
  return {
    activityId,
    sessionId,
    skuId: 3001,
    productId: 2001,
    productName: '商城演示手机',
    skuCode: '手机-黑色-128G',
    subtitle: '运营配置的秒杀商品。',
    originalPrice: 1999,
    seckillPrice: 1599,
    totalStock: 100,
    availableStock: 100,
    limitPerUser: 1,
    badge: '进行中',
    sort: items.value.length + 1,
    status: 1
  }
}

function toLocalDateTimeInput(date: Date) {
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 19)
}

function statusText(status?: number) {
  return status === 1 ? '启用' : '停用'
}

function orderStatusText(status?: number | null) {
  if (status === 10) {
    return '待支付'
  }
  if (status === 20) {
    return '已支付'
  }
  if (status === 30) {
    return '已取消'
  }
  return '-'
}

function orderEventText(eventType?: string) {
  const map: Record<string, string> = {
    CREATE: '创建订单',
    SECKILL_CREATE: '秒杀建单',
    PAY_SUCCESS: '支付成功',
    USER_CANCEL: '用户取消',
    ORDER_EXPIRE: '订单超时',
    CANCEL: '取消订单'
  }
  return eventType ? map[eventType] || eventType : '-'
}

function stockOperationType(operation?: string) {
  if (operation === 'LOCK') {
    return 'warning'
  }
  if (operation === 'DEDUCT') {
    return 'success'
  }
  if (operation === 'RELEASE') {
    return 'info'
  }
  return ''
}

function stockOperationText(operation?: string) {
  if (operation === 'LOCK') {
    return '锁定'
  }
  if (operation === 'DEDUCT') {
    return '扣减'
  }
  if (operation === 'RELEASE') {
    return '释放'
  }
  return operation || '-'
}

function auditActionText(action?: string) {
  const map: Record<string, string> = {
    CREATE_ACTIVITY: '创建活动',
    UPDATE_ACTIVITY: '更新活动',
    CREATE_SESSION: '创建场次',
    UPDATE_SESSION: '更新场次',
    CREATE_SECKILL_SKU: '创建秒杀 SKU',
    UPDATE_SECKILL_SKU: '更新秒杀 SKU'
  }
  return action ? map[action] || action : '-'
}

function resourceTypeText(resourceType?: string) {
  const map: Record<string, string> = {
    PROMO_ACTIVITY: '秒杀活动',
    PROMO_SESSION: '秒杀场次',
    PROMO_SECKILL_SKU: '秒杀商品'
  }
  return resourceType ? map[resourceType] || resourceType : '-'
}

function formatLogTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <section class="shop-page admin-page">
    <section class="admin-hero">
      <div>
        <el-tag type="warning" effect="plain">
          <Settings :size="14" />
          秒杀运营
        </el-tag>
        <h1>秒杀运营控制台</h1>
        <p>统一配置活动、场次、活动 SKU、限购规则和 Redis 库存预热。</p>
      </div>
      <div class="admin-metrics">
        <div>
          <strong>{{ activities.length }}</strong>
          <span>活动数</span>
        </div>
        <div>
          <strong>{{ sessions.length }}</strong>
          <span>场次数</span>
        </div>
        <div>
          <strong>{{ enabledItemCount }}</strong>
          <span>启用 SKU</span>
        </div>
        <div>
          <strong>{{ totalAvailableStock }}</strong>
          <span>可售库存</span>
        </div>
      </div>
    </section>

    <div class="admin-layout">
      <section class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span class="section-eyebrow">活动</span>
            <h2>营销活动</h2>
          </div>
          <el-button @click="newActivity">
            <Wand2 :size="16" />
            新建
          </el-button>
        </div>

        <el-table
          :data="activities"
          v-loading="loading"
          class="admin-table"
          height="220"
          highlight-current-row
          @current-change="(row: PromotionActivityAdmin | undefined) => row && loadSessions(row.id)"
        >
          <el-table-column prop="id" label="ID" width="86" />
          <el-table-column prop="name" label="名称" min-width="170" />
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column width="84" align="right">
            <template #default="{ row }">
              <el-button text @click.stop="editActivity(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form class="admin-form" label-position="top" @submit.prevent="submitActivity">
          <el-form-item label="名称">
            <el-input v-model="activityForm.name" placeholder="活动名称" />
          </el-form-item>
          <el-form-item label="标题">
            <el-input v-model="activityForm.title" placeholder="展示标题" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="activityForm.description" type="textarea" :rows="2" placeholder="内部描述" />
          </el-form-item>
          <div class="admin-form-row">
            <el-form-item label="状态">
              <el-select v-model="activityForm.status">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="saving" native-type="submit">
              <Save :size="16" />
              保存活动
            </el-button>
          </div>
        </el-form>
      </section>

      <section class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span class="section-eyebrow">场次</span>
            <h2>{{ activeActivity?.name || '未选择活动' }}</h2>
          </div>
          <el-button :disabled="!activeActivityId" @click="newSession">
            <Timer :size="16" />
            新建
          </el-button>
        </div>

        <el-table
          :data="sessions"
          class="admin-table"
          height="220"
          highlight-current-row
          @current-change="(row: PromotionSessionAdmin | undefined) => row && loadItems(row.id)"
        >
          <el-table-column prop="name" label="名称" min-width="130" />
          <el-table-column prop="sort" label="排序" width="72" />
          <el-table-column label="状态" width="96">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column width="84" align="right">
            <template #default="{ row }">
              <el-button text @click.stop="editSession(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form class="admin-form" label-position="top" @submit.prevent="submitSession">
          <div class="admin-form-row two">
            <el-form-item label="名称">
              <el-input v-model="sessionForm.name" placeholder="场次名称" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="sessionForm.sort" :min="0" />
            </el-form-item>
          </div>
          <div class="admin-form-row two">
            <el-form-item label="开始时间">
              <el-date-picker v-model="sessionForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
            <el-form-item label="结束时间">
              <el-date-picker v-model="sessionForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </div>
          <div class="admin-form-row">
            <el-form-item label="状态">
              <el-select v-model="sessionForm.status">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="saving" native-type="submit">
              <Save :size="16" />
              保存场次
            </el-button>
          </div>
        </el-form>
      </section>
    </div>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <div>
          <span class="section-eyebrow">活动 SKU</span>
          <h2>{{ activeSession?.name || '未选择场次' }}</h2>
        </div>
        <div class="admin-actions">
          <el-button :disabled="!activeSessionId" @click="newItem">
            <Flame :size="16" />
            新建 SKU
          </el-button>
          <el-button :disabled="!activeSessionId" @click="activeSessionId && loadItems(activeSessionId)">
            <RefreshCcw :size="16" />
            刷新
          </el-button>
        </div>
      </div>

      <el-table :data="items" class="admin-table" row-key="id">
        <el-table-column prop="skuId" label="SKU" width="92" />
        <el-table-column prop="productName" label="商品" min-width="190" />
        <el-table-column prop="skuCode" label="编码" min-width="150" />
        <el-table-column label="价格" width="150">
          <template #default="{ row }">
            <div class="admin-price">
              <strong>{{ money(row.seckillPrice) }}</strong>
              <span>{{ money(row.originalPrice) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="availableStock" label="可售" width="104" />
        <el-table-column prop="totalStock" label="总量" width="88" />
        <el-table-column prop="limitPerUser" label="限购" width="80" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="172" align="right">
          <template #default="{ row }">
            <el-button text @click="editItem(row)">编辑</el-button>
            <el-button text :loading="preheatingItemId === row.id" @click="preheatStock(row)">
              <DatabaseZap :size="15" />
              预热
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-form class="admin-sku-form" label-position="top" @submit.prevent="submitItem">
        <div class="admin-form-row four">
          <el-form-item label="SKU ID">
            <el-input-number v-model="itemForm.skuId" :min="1" />
          </el-form-item>
          <el-form-item label="商品 ID">
            <el-input-number v-model="itemForm.productId" :min="1" />
          </el-form-item>
          <el-form-item label="SKU 编码">
            <el-input v-model="itemForm.skuCode" />
          </el-form-item>
          <el-form-item label="角标">
            <el-input v-model="itemForm.badge" />
          </el-form-item>
        </div>
        <div class="admin-form-row two">
          <el-form-item label="商品名称">
            <el-input v-model="itemForm.productName" />
          </el-form-item>
          <el-form-item label="副标题">
            <el-input v-model="itemForm.subtitle" />
          </el-form-item>
        </div>
        <div class="admin-form-row four">
          <el-form-item label="原价">
            <el-input-number v-model="itemForm.originalPrice" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item label="秒杀价">
            <el-input-number v-model="itemForm.seckillPrice" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item label="总库存">
            <el-input-number v-model="itemForm.totalStock" :min="0" />
          </el-form-item>
          <el-form-item label="可售库存">
            <el-input-number v-model="itemForm.availableStock" :min="0" />
          </el-form-item>
        </div>
        <div class="admin-form-row four">
          <el-form-item label="单人限购">
            <el-input-number v-model="itemForm.limitPerUser" :min="1" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="itemForm.sort" :min="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="itemForm.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="saving" native-type="submit">
            <Save :size="16" />
            保存 SKU
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <div>
          <span class="section-eyebrow">诊断</span>
          <h2>订单状态时间线</h2>
        </div>
        <div class="admin-query-actions">
          <el-input
            v-model="orderLogQuery"
            clearable
            placeholder="订单号"
            @keyup.enter="queryOrderStatusLogs"
          />
          <el-button type="primary" :loading="orderLogLoading" @click="queryOrderStatusLogs">
            <Search :size="16" />
            查询
          </el-button>
        </div>
      </div>

      <el-table :data="orderStatusLogs" class="admin-table" empty-text="暂无订单状态日志">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            {{ formatLogTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="事件" min-width="150">
          <template #default="{ row }">{{ orderEventText(row.eventType) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="190">
          <template #default="{ row }">
            <div class="admin-status-flow">
              <el-tag effect="plain">{{ orderStatusText(row.fromStatus) }}</el-tag>
              <span>至</span>
              <el-tag :type="row.toStatus === 20 ? 'success' : row.toStatus === 30 ? 'info' : 'warning'" effect="plain">
                {{ orderStatusText(row.toStatus) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="业务单号" min-width="150">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.bizNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="260">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-table :data="stockFlows" class="admin-table" empty-text="暂无库存流水">
        <el-table-column label="时间" width="150">
          <template #default="{ row }">
            {{ formatLogTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="skuId" label="SKU" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-tag :type="stockOperationType(row.operation)" effect="plain">{{ stockOperationText(row.operation) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="可售库存" min-width="160">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.beforeAvailableStock }} 至 {{ row.afterAvailableStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="锁定库存" min-width="160">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.beforeLockedStock }} 至 {{ row.afterLockedStock }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <div>
          <span class="section-eyebrow">审计</span>
          <h2>最近操作</h2>
        </div>
        <el-button @click="loadOperationLogs">
          <RefreshCcw :size="16" />
          刷新
        </el-button>
      </div>

      <el-table :data="operationLogs" class="admin-table" empty-text="暂无操作日志">
        <el-table-column label="时间" width="120">
          <template #default="{ row }">
            {{ formatLogTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column label="动作" min-width="170">
          <template #default="{ row }">{{ auditActionText(row.action) }}</template>
        </el-table-column>
        <el-table-column label="资源" width="160">
          <template #default="{ row }">{{ resourceTypeText(row.resourceType) }}</template>
        </el-table-column>
        <el-table-column prop="resourceId" label="ID" width="100" />
        <el-table-column prop="detail" label="详情" min-width="280">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.detail || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>
