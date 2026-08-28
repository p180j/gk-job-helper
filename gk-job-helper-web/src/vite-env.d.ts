/// <reference types="vite/client" />

declare module 'element-plus' {
  const ElementPlus: any
  export default ElementPlus
  export const ElMessage: any
}

declare module 'element-plus/es/locale/lang/zh-cn' {
  const zhCn: any
  export default zhCn
}
/// <reference types="element-plus/global" />

interface ImportMetaEnv {
  /** API 基础地址，空则使用相对路径(开发代理) */
  readonly VITE_API_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
