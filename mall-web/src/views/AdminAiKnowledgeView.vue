<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DatabaseZap, Plus, RefreshCcw, Save } from 'lucide-vue-next'
import {
  createKnowledgeDoc,
  deleteKnowledgeDoc,
  indexKnowledgeDoc,
  listKnowledgeDocs,
  syncProductKnowledge,
  updateKnowledgeDoc
} from '@/api/mall'
import type { KnowledgeDoc } from '@/types/api'

const docs = ref<KnowledgeDoc[]>([])
const loading = ref(false)
const saving = ref(false)
const indexingId = ref<string>()
const syncingProducts = ref(false)
const defaultCategory = '\u5E38\u89C1\u95EE\u9898'
const form = reactive({ id: undefined as number | undefined, title: '', category: defaultCategory, content: '', status: 1 })
const categoryOptions = [
  { label: '\u5E38\u89C1\u95EE\u9898', value: '\u5E38\u89C1\u95EE\u9898' },
  { label: '\u9000\u6B3E\u552E\u540E', value: '\u9000\u6B3E\u552E\u540E' },
  { label: '\u7269\u6D41\u914D\u9001', value: '\u7269\u6D41\u914D\u9001' },
  { label: '\u79D2\u6740\u6D3B\u52A8', value: '\u79D2\u6740\u6D3B\u52A8' },
  { label: '\u652F\u4ED8\u8BF4\u660E', value: '\u652F\u4ED8\u8BF4\u660E' },
  { label: '\u5546\u54C1\u77E5\u8BC6', value: '\u5546\u54C1\u77E5\u8BC6' }
]

onMounted(loadDocs)

async function loadDocs() {
  loading.value = true
  try {
    docs.value = await listKnowledgeDocs()
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { id: undefined, title: '', category: defaultCategory, content: '', status: 1 })
}

function edit(row: KnowledgeDoc) {
  Object.assign(form, { id: row.id, title: row.title, category: row.category, content: row.content, status: row.status })
}

async function save() {
  saving.value = true
  try {
    if (form.id) {
      await updateKnowledgeDoc(form.id, form)
    } else {
      await createKnowledgeDoc(form)
    }
    ElMessage.success('\u4FDD\u5B58\u6210\u529F')
    resetForm()
    await loadDocs()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '\u4FDD\u5B58\u5931\u8D25')
  } finally {
    saving.value = false
  }
}

async function remove(row: KnowledgeDoc) {
  await ElMessageBox.confirm(`\u786E\u8BA4\u5220\u9664\u300C${row.title}\u300D\u5417\uFF1F`, '\u5220\u9664\u786E\u8BA4')
  await deleteKnowledgeDoc(row.id)
  await loadDocs()
}

async function indexDoc(row: KnowledgeDoc) {
  indexingId.value = row.id
  try {
    await indexKnowledgeDoc(row.id)
    ElMessage.success('\u5411\u91CF\u7D22\u5F15\u5B8C\u6210')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '\u7D22\u5F15\u5931\u8D25')
  } finally {
    indexingId.value = undefined
    await loadDocs()
  }
}

async function syncProducts() {
  syncingProducts.value = true
  try {
    const result = await syncProductKnowledge()
    const failedCount = result.filter((item) => item.embeddingStatus === 2).length
    if (failedCount > 0) {
      ElMessage.warning(`\u5546\u57CE\u5546\u54C1\u77E5\u8BC6\u5E93\u5DF2\u540C\u6B65\uFF0C${failedCount} \u6761\u5411\u91CF\u7D22\u5F15\u5931\u8D25`)
    } else {
      ElMessage.success('\u5546\u57CE\u5546\u54C1\u77E5\u8BC6\u5E93\u540C\u6B65\u5B8C\u6210')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '\u5546\u57CE\u5546\u54C1\u77E5\u8BC6\u5E93\u540C\u6B65\u5931\u8D25')
  } finally {
    syncingProducts.value = false
    await loadDocs()
  }
}

function embeddingText(status: number) {
  if (status === 1) return '\u5DF2\u7D22\u5F15'
  if (status === 2) return '\u5931\u8D25'
  return '\u5F85\u7D22\u5F15'
}
</script>

<template>
  <section class="shop-page admin-page">
    <section class="admin-hero">
      <div>
        <el-tag type="success" effect="plain">&#x77E5;&#x8BC6;&#x5E93;&#x7BA1;&#x7406;</el-tag>
        <h1>&#x79C1;&#x6709;&#x77E5;&#x8BC6;&#x5E93;</h1>
        <p>&#x7EF4;&#x62A4;&#x6587;&#x672C;&#x8D44;&#x6599;&#x5E76;&#x5199;&#x5165; Milvus &#x5411;&#x91CF;&#x5E93;&#xFF0C;&#x7528;&#x4E8E;&#x667A;&#x80FD;&#x5BA2;&#x670D;&#x68C0;&#x7D22;&#x589E;&#x5F3A;&#x56DE;&#x7B54;&#x3002;</p>
      </div>
    </section>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <h2>&#x77E5;&#x8BC6;&#x6587;&#x6863;</h2>
        <div class="admin-actions">
          <el-button :loading="syncingProducts" @click="syncProducts">
            <DatabaseZap :size="16" />
            &#x540C;&#x6B65;&#x5546;&#x57CE;&#x5546;&#x54C1;
          </el-button>
          <el-button @click="loadDocs">
            <RefreshCcw :size="16" />
            &#x5237;&#x65B0;
          </el-button>
        </div>
      </div>
      <el-table :data="docs" v-loading="loading" class="admin-table">
        <el-table-column prop="title" label="&#x6807;&#x9898;" min-width="180" />
        <el-table-column prop="category" label="&#x5206;&#x7C7B;" width="120" />
        <el-table-column label="&#x72B6;&#x6001;" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '\u542F\u7528' : '\u505C\u7528' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="&#x5411;&#x91CF;&#x72B6;&#x6001;" width="120">
          <template #default="{ row }">
            <el-tag :type="row.embeddingStatus === 1 ? 'success' : row.embeddingStatus === 2 ? 'danger' : 'warning'" effect="plain">
              {{ embeddingText(row.embeddingStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="&#x9519;&#x8BEF;&#x4FE1;&#x606F;" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.embeddingStatus === 2 ? row.lastEmbeddingError : '' }}
          </template>
        </el-table-column>
        <el-table-column width="230" align="right">
          <template #default="{ row }">
            <el-button text @click="edit(row)">&#x7F16;&#x8F91;</el-button>
            <el-button text :loading="indexingId === row.id" @click="indexDoc(row)">
              <DatabaseZap :size="15" />
              &#x7D22;&#x5F15;
            </el-button>
            <el-button text type="danger" @click="remove(row)">&#x5220;&#x9664;</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="admin-panel admin-wide-panel">
      <div class="admin-panel-head">
        <h2>{{ form.id ? '\u7F16\u8F91\u6587\u6863' : '\u65B0\u589E\u6587\u6863' }}</h2>
        <el-button @click="resetForm">
          <Plus :size="16" />
          &#x65B0;&#x589E;
        </el-button>
      </div>
      <el-form class="admin-form" label-position="top" @submit.prevent="save">
        <div class="admin-form-row three">
          <el-form-item label="&#x6807;&#x9898;">
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="&#x5206;&#x7C7B;">
            <el-select v-model="form.category">
              <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="&#x72B6;&#x6001;">
            <el-select v-model="form.status">
              <el-option label="&#x542F;&#x7528;" :value="1" />
              <el-option label="&#x505C;&#x7528;" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="&#x6B63;&#x6587;&#x5185;&#x5BB9;">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="save">
          <Save :size="16" />
          &#x4FDD;&#x5B58;
        </el-button>
      </el-form>
    </section>
  </section>
</template>
