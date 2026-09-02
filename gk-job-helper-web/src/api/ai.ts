import { post } from './http'
import type { AiProviderConfig, AiTestResult } from '@/types/model'

export function testAiConnection(config: AiProviderConfig) {
  return post<AiTestResult>('/api/ai/test', config)
}
