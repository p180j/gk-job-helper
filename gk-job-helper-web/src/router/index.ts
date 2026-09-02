import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue')
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('@/views/ImportUploadView.vue')
    },
    {
      path: '/import/:id/mapping',
      name: 'mapping',
      component: () => import('@/views/MappingView.vue')
    },
    {
      path: '/import/:id/execute',
      name: 'execute',
      component: () => import('@/views/ExecuteView.vue')
    },
    {
      path: '/positions',
      name: 'results',
      component: () => import('@/views/ResultsView.vue')
    },
    { path: '/results/:importId', redirect: to => ({ path: '/positions', query: { importId: String(to.params.importId), result: String(to.query.result || '') } }) },
    {
      path: '/jobs/:id',
      name: 'jobDetail',
      component: () => import('@/views/JobDetailView.vue')
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoritesView.vue')
    },
    {
      path: '/compare',
      name: 'compare',
      component: () => import('@/views/CompareView.vue')
    },
    {
      path: '/recommend', name: 'recommend', component: () => import('@/views/RecommendView.vue')
    },
    {
      path: '/interview-scores/import', name: 'interviewScoreImport', component: () => import('@/views/InterviewScoreImportView.vue')
    },
    { path: '/recruitment', name: 'recruitment', component: () => import('@/views/RecruitmentDiscoveryView.vue') },
    { path: '/recruitment/notices/:id', name: 'recruitment-notice-detail', component: () => import('@/views/RecruitmentNoticeDetailView.vue')
    },
    { path: '/recruitment/notices/:id/positions', name: 'recruitment-position-list', component: () => import('@/views/RecruitmentPositionListView.vue') },
    { path: '/recruitment/positions/:id', name: 'recruitment-position-detail', component: () => import('@/views/RecruitmentPositionDetailView.vue') },
    { path: '/ai-settings', name: 'aiSettings', component: () => import('@/views/AiSettingsView.vue') }
  ]
})

export default router
