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
    ElMessage.error(error instanceof Error ? error.message : 'Activity load failed')
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
    ElMessage.warning('Enter an order number')
    return
  }
  orderLogLoading.value = true
  try {
    const [statusLogs, flows] = await Promise.all([listOrderStatusLogs(orderNo), listStockFlows(orderNo)])
    orderStatusLogs.value = statusLogs
    stockFlows.value = flows
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Order diagnostics load failed')
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
    ElMessage.success('Activity saved')
    await loadActivities()
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Activity save failed')
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
    ElMessage.warning('Select an activity first')
    return
  }
  saving.value = true
  try {
    const response = await saveAdminSession({ ...sessionForm, activityId: activeActivityId.value })
    activeSessionId.value = response.id
    ElMessage.success('Session saved')
    await loadSessions(activeActivityId.value)
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Session save failed')
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
    ElMessage.warning('Select an activity and a session first')
    return
  }
  saving.value = true
  try {
    await saveAdminItem({ ...itemForm, activityId: activeActivityId.value, sessionId: activeSessionId.value })
    ElMessage.success('Item saved')
    await loadItems(activeSessionId.value)
    await loadOperationLogs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Item save failed')
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
    ElMessage.success('Redis stock preheated')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : 'Stock preheat failed')
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
    productName: 'Mall Demo Phone',
    skuCode: 'PHONE-BLACK-128G',
    subtitle: 'Operator configured flash-sale item.',
    originalPrice: 1999,
    seckillPrice: 1599,
    totalStock: 100,
    availableStock: 100,
    limitPerUser: 1,
    badge: 'Live',
    sort: items.value.length + 1,
    status: 1
  }
}

function toLocalDateTimeInput(date: Date) {
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 19)
}

function statusText(status?: number) {
  return status === 1 ? 'Enabled' : 'Disabled'
}

function orderStatusText(status?: number | null) {
  if (status === 10) {
    return 'Pending'
  }
  if (status === 20) {
    return 'Paid'
  }
  if (status === 30) {
    return 'Canceled'
  }
  return '-'
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
          Seckill Operations
        </el-tag>
        <h1>Flash-sale control center</h1>
        <p>Configure activities, time sessions, activity SKUs, limit rules, and Redis stock preheat from one console.</p>
      </div>
      <div class="admin-metrics">
        <div>
          <strong>{{ activities.length }}</strong>
          <span>Activities</span>
        </div>
        <div>
          <strong>{{ sessions.length }}</strong>
          <span>Sessions</span>
        </div>
        <div>
          <strong>{{ enabledItemCount }}</strong>
          <span>Active SKUs</span>
        </div>
        <div>
          <strong>{{ totalAvailableStock }}</strong>
          <span>Available</span>
        </div>
      </div>
    </section>

    <div class="admin-layout">
      <section class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span class="section-eyebrow">Activity</span>
            <h2>Campaigns</h2>
          </div>
          <el-button @click="newActivity">
            <Wand2 :size="16" />
            New
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
          <el-table-column prop="name" label="Name" min-width="170" />
          <el-table-column label="Status" width="96">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column width="84" align="right">
            <template #default="{ row }">
              <el-button text @click.stop="editActivity(row)">Edit</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form class="admin-form" label-position="top" @submit.prevent="submitActivity">
          <el-form-item label="Name">
            <el-input v-model="activityForm.name" placeholder="Activity name" />
          </el-form-item>
          <el-form-item label="Title">
            <el-input v-model="activityForm.title" placeholder="Display title" />
          </el-form-item>
          <el-form-item label="Description">
            <el-input v-model="activityForm.description" type="textarea" :rows="2" placeholder="Internal description" />
          </el-form-item>
          <div class="admin-form-row">
            <el-form-item label="Status">
              <el-select v-model="activityForm.status">
                <el-option label="Enabled" :value="1" />
                <el-option label="Disabled" :value="0" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="saving" native-type="submit">
              <Save :size="16" />
              Save Activity
            </el-button>
          </div>
        </el-form>
      </section>

      <section class="admin-panel">
        <div class="admin-panel-head">
          <div>
            <span class="section-eyebrow">Session</span>
            <h2>{{ activeActivity?.name || 'No activity selected' }}</h2>
          </div>
          <el-button :disabled="!activeActivityId" @click="newSession">
            <Timer :size="16" />
            New
          </el-button>
        </div>

        <el-table
          :data="sessions"
          class="admin-table"
          height="220"
          highlight-current-row
          @current-change="(row: PromotionSessionAdmin | undefined) => row && loadItems(row.id)"
        >
          <el-table-column prop="name" label="Name" min-width="130" />
          <el-table-column prop="sort" label="Sort" width="72" />
          <el-table-column label="Status" width="96">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column width="84" align="right">
            <template #default="{ row }">
              <el-button text @click.stop="editSession(row)">Edit</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-form class="admin-form" label-position="top" @submit.prevent="submitSession">
          <div class="admin-form-row two">
            <el-form-item label="Name">
              <el-input v-model="sessionForm.name" placeholder="Session name" />
            </el-form-item>
            <el-form-item label="Sort">
              <el-input-number v-model="sessionForm.sort" :min="0" />
            </el-form-item>
          </div>
          <div class="admin-form-row two">
            <el-form-item label="Start">
              <el-date-picker v-model="sessionForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
            <el-form-item label="End">
              <el-date-picker v-model="sessionForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
            </el-form-item>
          </div>
          <div class="admin-form-row">
            <el-form-item label="Status">
              <el-select v-model="sessionForm.status">
                <el-option label="Enabled" :value="1" />
                <el-option label="Disabled" :value="0" />
              </el-select>
            </el-form-item>
            <el-button type="primary" :loading="saving" native-type="submit">
              <Save :size="16" />
              Save Session
            </el-button>
          </div>
        </el-form>
      </section>
    </div>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <div>
          <span class="section-eyebrow">Activity SKU</span>
          <h2>{{ activeSession?.name || 'No session selected' }}</h2>
        </div>
        <div class="admin-actions">
          <el-button :disabled="!activeSessionId" @click="newItem">
            <Flame :size="16" />
            New SKU
          </el-button>
          <el-button :disabled="!activeSessionId" @click="activeSessionId && loadItems(activeSessionId)">
            <RefreshCcw :size="16" />
            Refresh
          </el-button>
        </div>
      </div>

      <el-table :data="items" class="admin-table" row-key="id">
        <el-table-column prop="skuId" label="SKU" width="92" />
        <el-table-column prop="productName" label="Product" min-width="190" />
        <el-table-column prop="skuCode" label="Code" min-width="150" />
        <el-table-column label="Price" width="150">
          <template #default="{ row }">
            <div class="admin-price">
              <strong>{{ money(row.seckillPrice) }}</strong>
              <span>{{ money(row.originalPrice) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="availableStock" label="Available" width="104" />
        <el-table-column prop="totalStock" label="Total" width="88" />
        <el-table-column prop="limitPerUser" label="Limit" width="80" />
        <el-table-column label="Status" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column width="172" align="right">
          <template #default="{ row }">
            <el-button text @click="editItem(row)">Edit</el-button>
            <el-button text :loading="preheatingItemId === row.id" @click="preheatStock(row)">
              <DatabaseZap :size="15" />
              Preheat
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-form class="admin-sku-form" label-position="top" @submit.prevent="submitItem">
        <div class="admin-form-row four">
          <el-form-item label="SKU ID">
            <el-input-number v-model="itemForm.skuId" :min="1" />
          </el-form-item>
          <el-form-item label="Product ID">
            <el-input-number v-model="itemForm.productId" :min="1" />
          </el-form-item>
          <el-form-item label="SKU Code">
            <el-input v-model="itemForm.skuCode" />
          </el-form-item>
          <el-form-item label="Badge">
            <el-input v-model="itemForm.badge" />
          </el-form-item>
        </div>
        <div class="admin-form-row two">
          <el-form-item label="Product Name">
            <el-input v-model="itemForm.productName" />
          </el-form-item>
          <el-form-item label="Subtitle">
            <el-input v-model="itemForm.subtitle" />
          </el-form-item>
        </div>
        <div class="admin-form-row four">
          <el-form-item label="Original Price">
            <el-input-number v-model="itemForm.originalPrice" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item label="Seckill Price">
            <el-input-number v-model="itemForm.seckillPrice" :min="0.01" :precision="2" />
          </el-form-item>
          <el-form-item label="Total Stock">
            <el-input-number v-model="itemForm.totalStock" :min="0" />
          </el-form-item>
          <el-form-item label="Available Stock">
            <el-input-number v-model="itemForm.availableStock" :min="0" />
          </el-form-item>
        </div>
        <div class="admin-form-row four">
          <el-form-item label="Limit Per User">
            <el-input-number v-model="itemForm.limitPerUser" :min="1" />
          </el-form-item>
          <el-form-item label="Sort">
            <el-input-number v-model="itemForm.sort" :min="0" />
          </el-form-item>
          <el-form-item label="Status">
            <el-select v-model="itemForm.status">
              <el-option label="Enabled" :value="1" />
              <el-option label="Disabled" :value="0" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="saving" native-type="submit">
            <Save :size="16" />
            Save SKU
          </el-button>
        </div>
      </el-form>
    </section>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <div>
          <span class="section-eyebrow">Diagnostics</span>
          <h2>Order status timeline</h2>
        </div>
        <div class="admin-query-actions">
          <el-input
            v-model="orderLogQuery"
            clearable
            placeholder="Order number"
            @keyup.enter="queryOrderStatusLogs"
          />
          <el-button type="primary" :loading="orderLogLoading" @click="queryOrderStatusLogs">
            <Search :size="16" />
            Query
          </el-button>
        </div>
      </div>

      <el-table :data="orderStatusLogs" class="admin-table" empty-text="No order status logs">
        <el-table-column label="Time" width="150">
          <template #default="{ row }">
            {{ formatLogTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="eventType" label="Event" min-width="150" />
        <el-table-column label="Status" width="190">
          <template #default="{ row }">
            <div class="admin-status-flow">
              <el-tag effect="plain">{{ orderStatusText(row.fromStatus) }}</el-tag>
              <span>to</span>
              <el-tag :type="row.toStatus === 20 ? 'success' : row.toStatus === 30 ? 'info' : 'warning'" effect="plain">
                {{ orderStatusText(row.toStatus) }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="Biz No." min-width="150">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.bizNo || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="Remark" min-width="260">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-table :data="stockFlows" class="admin-table" empty-text="No stock flows">
        <el-table-column label="Time" width="150">
          <template #default="{ row }">
            {{ formatLogTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="skuId" label="SKU" width="100" />
        <el-table-column label="Operation" width="120">
          <template #default="{ row }">
            <el-tag :type="stockOperationType(row.operation)" effect="plain">{{ row.operation }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="Qty" width="80" />
        <el-table-column label="Available" min-width="160">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.beforeAvailableStock }} to {{ row.afterAvailableStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Locked" min-width="160">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.beforeLockedStock }} to {{ row.afterLockedStock }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <div>
          <span class="section-eyebrow">Audit</span>
          <h2>Recent operations</h2>
        </div>
        <el-button @click="loadOperationLogs">
          <RefreshCcw :size="16" />
          Refresh
        </el-button>
      </div>

      <el-table :data="operationLogs" class="admin-table" empty-text="No operation logs">
        <el-table-column label="Time" width="120">
          <template #default="{ row }">
            {{ formatLogTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="Operator" width="120" />
        <el-table-column prop="action" label="Action" min-width="170" />
        <el-table-column prop="resourceType" label="Resource" width="160" />
        <el-table-column prop="resourceId" label="ID" width="100" />
        <el-table-column prop="detail" label="Detail" min-width="280">
          <template #default="{ row }">
            <span class="admin-log-detail">{{ row.detail || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </section>
</template>
