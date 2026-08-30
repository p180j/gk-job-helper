import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/model'

/** 业务错误：后端返回 code != 0 */
export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

const http = axios.create({
  // 环境变量配置后端地址；默认空 = 相对路径，开发模式由 Vite 代理转发
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 120000
})

/** 后端业务错误码 -> 中文兜底文案（后端 message 优先） */
const CODE_MESSAGES: Record<number, string> = {
  40000: '请求参数有误，请检查后重试。',
  40401: '个人档案不存在，请先完善报考档案。',
  40402: '个人档案已存在。',
  40403: '职位表导入记录不存在。',
  40404: '岗位不存在或已删除。',
  40405: '该岗位尚未执行匹配，请先执行智能匹配。',
  50000: '系统内部错误，请稍后重试。'
}

function httpErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message || CODE_MESSAGES[error.code] || '请求失败，请稍后重试。'
  }
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError
    if (!axiosError.response) {
      return '网络异常，请检查网络连接后重试。'
    }
    const status = axiosError.response.status
    if (status === 404) {
      return '请求的接口不存在。'
    }
    if (status >= 500) {
      return '服务异常，请稍后重试。'
    }
  }
  return '请求失败，请稍后重试。'
}

/**
 * 统一 GET 请求：只处理业务成功(code=0)，业务失败/网络异常统一抛 ApiError
 * @param silent 不弹全局错误提示，由调用方自行处理
 */
export async function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request<T>('get', url, undefined, config)
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>('post', url, body, config)
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return request<T>('put', url, body, config)
}

export async function del<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return request<T>('delete', url, undefined, config)
}

async function request<T>(
  method: 'get' | 'post' | 'put' | 'delete',
  url: string,
  body: unknown,
  config?: AxiosRequestConfig
): Promise<T> {
  try {
    const merged: AxiosRequestConfig = { ...config, method, url }
    if (body !== undefined) {
      merged.data = body
    }
    const response = await http.request<ApiResponse<T>>(merged)
    const payload = response.data
    if (payload.code !== 0) {
      const message = payload.message || CODE_MESSAGES[payload.code] || '请求失败。'
      throw new ApiError(payload.code, message)
    }
    return payload.data as T
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<ApiResponse<T>>
      // 后端通过 HTTP 200 + code 返回业务错误；此处兜底非 200 且带响应体的情况
      const payload = axiosError.response?.data
      if (payload && typeof payload.code === 'number' && payload.code !== 0) {
        const message = payload.message || CODE_MESSAGES[payload.code] || '请求失败。'
        throw new ApiError(payload.code, message)
      }
    }
    throw error
  }
}

/** 页面级统一错误提示：把后端/网络异常转成中文，不暴露堆栈 */
export function showError(error: unknown, fallback: string): void {
  console.error('[api-error]', error)
  ElMessage.error(httpErrorMessage(error) || fallback)
}

export default http
