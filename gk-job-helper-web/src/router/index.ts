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
      path: '/results/:importId',
      name: 'results',
      component: () => import('@/views/ResultsView.vue')
    },
    {
      path: '/jobs/:id',
      name: 'jobDetail',
      component: () => import('@/views/JobDetailView.vue')
    }
  ]
})

export default router
