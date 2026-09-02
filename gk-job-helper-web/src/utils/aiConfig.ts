import type { AiProviderConfig } from '@/types/model'

const STORAGE_KEY = 'ai-provider-config'

/** 仅从当前浏览器读取；未配置或损坏时返回 null，业务流程必须无 AI 继续运行。 */
export function loadAiProviderConfig(): AiProviderConfig | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    const value = JSON.parse(raw) as Partial<AiProviderConfig>
    if (!value.apiKey?.trim() || !value.provider?.trim() || !value.model?.trim() || !value.baseUrl?.trim()) return null
    return { provider: value.provider, model: value.model, baseUrl: value.baseUrl, apiKey: value.apiKey }
  } catch {
    return null
  }
}
