<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MessageSquarePlus, SendHorizontal, Trash2 } from 'lucide-vue-next'
import { deleteAiConversation, listAiConversations, listAiMessages } from '@/api/mall'
import { useAuthStore } from '@/stores/auth'
import type { AiConversation, AiMessage } from '@/types/api'

const conversations = ref<AiConversation[]>([])
const messages = ref<AiMessage[]>([])
const activeConversationId = ref<string>()
const input = ref('')
const loading = ref(false)
const loadingConversations = ref(false)
const loadingMessages = ref(false)
const authStore = useAuthStore()

onMounted(loadConversations)

async function loadConversations(openFirst = true) {
  loadingConversations.value = true
  try {
    conversations.value = await listAiConversations()
    if (openFirst && !activeConversationId.value && conversations.value.length > 0) {
      await openConversation(conversations.value[0].id)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '\u4F1A\u8BDD\u5217\u8868\u52A0\u8F7D\u5931\u8D25')
  } finally {
    loadingConversations.value = false
  }
}

async function openConversation(id: string) {
  activeConversationId.value = id
  loadingMessages.value = true
  try {
    messages.value = await listAiMessages(id)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '\u4F1A\u8BDD\u6D88\u606F\u52A0\u8F7D\u5931\u8D25')
  } finally {
    loadingMessages.value = false
  }
}

function newConversation() {
  activeConversationId.value = undefined
  messages.value = []
  input.value = ''
}

async function send() {
  const message = input.value.trim()
  if (!message || loading.value) {
    return
  }
  input.value = ''
  messages.value.push({ id: Date.now(), roleCode: 'USER', content: message })
  loading.value = true
  try {
    await streamChat(message)
    void loadConversations(false)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '\u667A\u80FD\u5BA2\u670D\u6682\u65F6\u4E0D\u53EF\u7528')
  } finally {
    loading.value = false
  }
}

async function streamChat(message: string) {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  if (authStore.accessToken) {
    headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers,
    body: JSON.stringify({ conversationId: activeConversationId.value, message })
  })
  if (!response.ok || !response.body) {
    throw new Error('\u667A\u80FD\u5BA2\u670D\u6682\u65F6\u4E0D\u53EF\u7528')
  }

  const assistantMessage: AiMessage = { id: Date.now() + 1, roleCode: 'ASSISTANT', content: '' }
  messages.value.push(assistantMessage)
  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() || ''
    for (const event of events) {
      handleSseEvent(event, assistantMessage)
    }
  }
  if (buffer) {
    handleSseEvent(buffer, assistantMessage)
  }
  if (!assistantMessage.content) {
    throw new Error('\u667A\u80FD\u5BA2\u670D\u6682\u65F6\u4E0D\u53EF\u7528')
  }
}

function handleSseEvent(raw: string, message: AiMessage) {
  const lines = raw.split('\n')
  const event = lines.find((line) => line.startsWith('event:'))?.slice(6).trim() || 'message'
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart())
    .join('\n')
  if (event === 'delta') {
    message.content += data
    return
  }
  if (event === 'done') {
    if (data) {
      activeConversationId.value = data
    }
    return
  }
  if (event === 'error') {
    throw new Error(data || '\u667A\u80FD\u5BA2\u670D\u6682\u65F6\u4E0D\u53EF\u7528')
  }
}

async function removeConversation(item: AiConversation) {
  try {
    await ElMessageBox.confirm('\u5220\u9664\u540E\uFF0C\u8BE5\u4F1A\u8BDD\u4E2D\u7684\u6D88\u606F\u5C06\u65E0\u6CD5\u6062\u590D\u3002', '\u5220\u9664\u4F1A\u8BDD', {
      confirmButtonText: '\u5220\u9664',
      cancelButtonText: '\u53D6\u6D88',
      type: 'warning'
    })
    const wasActive = activeConversationId.value === item.id
    conversations.value = conversations.value.filter((conversation) => conversation.id !== item.id)
    if (wasActive) {
      newConversation()
    }
    await deleteAiConversation(item.id)
    ElMessage.success('\u5DF2\u5220\u9664\u4F1A\u8BDD')
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    await loadConversations(false)
    ElMessage.error(error instanceof Error ? error.message : '\u5220\u9664\u4F1A\u8BDD\u5931\u8D25')
  }
}

</script>

<template>
  <section class="shop-page ai-page">
    <aside class="ai-sidebar" v-loading="loadingConversations">
      <div class="ai-sidebar-head">
        <strong>&#x5386;&#x53F2;&#x4F1A;&#x8BDD;</strong>
        <el-button size="small" @click="newConversation">
          <MessageSquarePlus :size="15" />
          &#x65B0;&#x4F1A;&#x8BDD;
        </el-button>
      </div>
      <div v-if="conversations.length === 0" class="ai-empty">
        &#x6682;&#x65E0;&#x5386;&#x53F2;&#x4F1A;&#x8BDD;
      </div>
      <div
        v-for="item in conversations"
        :key="item.id"
        class="ai-session-row"
        :class="{ active: item.id === activeConversationId }"
      >
        <button class="ai-session-item" type="button" @click="openConversation(item.id)">
          <span>{{ item.title }}</span>
          <small>{{ item.updatedAt ? new Date(item.updatedAt).toLocaleString() : '' }}</small>
        </button>
        <el-tooltip content="&#x5220;&#x9664;&#x4F1A;&#x8BDD;" placement="right">
          <button class="ai-session-delete" type="button" @click.stop="removeConversation(item)">
            <Trash2 :size="15" />
          </button>
        </el-tooltip>
      </div>
    </aside>

    <section class="ai-chat-panel">
      <div class="ai-message-list" v-loading="loadingMessages">
        <div v-if="messages.length === 0" class="ai-empty">
          &#x6709;&#x4EC0;&#x4E48;&#x53EF;&#x4EE5;&#x5E2E;&#x4F60;&#xFF1F;
        </div>
        <article v-for="message in messages" :key="message.id" class="ai-message" :class="message.roleCode.toLowerCase()">
          <p>{{ message.content }}</p>
        </article>
      </div>

      <footer class="ai-input-row">
        <el-input
          v-model="input"
          :disabled="loading"
          placeholder="&#x8BF7;&#x8F93;&#x5165;&#x95EE;&#x9898;&#xFF0C;&#x4F8B;&#x5982;&#xFF1A;&#x6700;&#x8FD1;&#x4E00;&#x6B21;&#x7684;&#x8BA2;&#x5355;&#x4E70;&#x7684;&#x662F;&#x4EC0;&#x4E48;&#xFF1F;"
          @keyup.enter="send"
        />
        <el-button type="primary" :loading="loading" @click="send">
          <SendHorizontal :size="16" />
          &#x53D1;&#x9001;
        </el-button>
      </footer>
    </section>
  </section>
</template>
