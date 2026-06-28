<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RefreshCcw } from 'lucide-vue-next'
import { listAiModelCallLogs, listAiToolCallLogs } from '@/api/mall'
import type { AiLog } from '@/types/api'

const modelLogs = ref<AiLog[]>([])
const toolLogs = ref<AiLog[]>([])
const loading = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [models, tools] = await Promise.all([listAiModelCallLogs(), listAiToolCallLogs()])
    modelLogs.value = models
    toolLogs.value = tools
  } finally {
    loading.value = false
  }
}

function statusText(status: number) {
  return status === 1 ? '成功' : '失败'
}
</script>

<template>
  <section class="shop-page admin-page">
    <section class="admin-hero">
      <div>
        <el-tag type="info" effect="plain">调用日志</el-tag>
        <h1>智能客服监控</h1>
        <p>查看模型调用和只读业务工具调用情况。</p>
      </div>
      <el-button :loading="loading" @click="load">
        <RefreshCcw :size="16" />
        刷新
      </el-button>
    </section>

    <section class="admin-panel admin-wide-panel">
      <h2>模型调用</h2>
      <el-table :data="modelLogs" v-loading="loading" class="admin-table">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="userId" label="用户" width="100" />
        <el-table-column prop="name" label="模型" min-width="160" />
        <el-table-column prop="elapsedMs" label="耗时(ms)" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
      </el-table>
    </section>

    <section class="admin-panel admin-wide-panel">
      <h2>工具调用</h2>
      <el-table :data="toolLogs" v-loading="loading" class="admin-table">
        <el-table-column prop="createdAt" label="时间" width="180" />
        <el-table-column prop="userId" label="用户" width="100" />
        <el-table-column prop="name" label="工具" min-width="160" />
        <el-table-column prop="elapsedMs" label="耗时(ms)" width="110" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="220" show-overflow-tooltip />
      </el-table>
    </section>
  </section>
</template>
