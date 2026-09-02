<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { testAiConnection } from '@/api/ai'
import { showError } from '@/api/http'
import type { AiProviderConfig } from '@/types/model'

const storageKey = 'ai-provider-config'
const router = useRouter()
const testing = ref(false)
const form = ref<AiProviderConfig>({
  provider: 'DEEPSEEK',
  model: 'deepseek-chat',
  baseUrl: 'https://api.deepseek.com',
  apiKey: ''
})

onMounted(() => {
  const saved = localStorage.getItem(storageKey)
  if (!saved) return
  try {
    const value = JSON.parse(saved) as Partial<AiProviderConfig>
    form.value = { ...form.value, ...value, provider: 'DEEPSEEK' }
  } catch {
    localStorage.removeItem(storageKey)
  }
})

function persist(): void {
  localStorage.setItem(storageKey, JSON.stringify(form.value))
}

function save(): void {
  persist()
  ElMessage.success('配置已保存在当前浏览器。')
  router.push('/')
}

async function test(): Promise<void> {
  if (!form.value.apiKey.trim()) {
    ElMessage.warning('请先填写 DeepSeek API Key。')
    return
  }
  persist()
  testing.value = true
  try {
    const result = await testAiConnection(form.value)
    if (result.success) {
      ElMessage.success(result.message)
    } else {
      ElMessage.error(result.message)
    }
  } catch (error) {
    showError(error, '测试连接失败。')
  } finally {
    testing.value = false
  }
}
</script>

<template>
  <section class="page-card settings-card">
    <div>
      <h1 class="page-title">AI模型设置</h1>
      <p class="page-subtitle">API Key 仅保存在当前浏览器 localStorage，不会上传或保存到服务器。</p>
    </div>
    <el-form label-width="120px" class="settings-form">
      <el-form-item label="服务商">
        <el-select v-model="form.provider" disabled><el-option label="DeepSeek" value="DEEPSEEK" /></el-select>
      </el-form-item>
      <el-form-item label="模型"><el-input v-model="form.model" /></el-form-item>
      <el-form-item label="Base URL"><el-input v-model="form.baseUrl" /></el-form-item>
      <el-form-item label="API Key">
        <el-input v-model="form.apiKey" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button @click="save">保存并返回首页</el-button>
        <el-button type="primary" :loading="testing" @click="test">测试连接</el-button>
      </el-form-item>
    </el-form>
  </section>
</template>

<style scoped>
.settings-card { max-width: 780px; margin: 28px auto; }
.settings-form { margin-top: 26px; max-width: 620px; }
</style>
