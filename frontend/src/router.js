import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'dashboard', component: () => import('./views/DashboardView.vue') },
  { path: '/campaigns', name: 'campaigns', component: () => import('./views/CampaignsView.vue') },
  { path: '/campaigns/:id', name: 'campaign', component: () => import('./views/CampaignDetailView.vue'), props: true },
  {
    path: '/generators',
    component: () => import('./views/GeneratorsView.vue'),
    children: [
      { path: '', name: 'generators', component: () => import('./views/generators/GeneratorHome.vue') },
      { path: ':slug', name: 'generator', component: () => import('./views/generators/GeneratorRunner.vue'), props: true },
    ],
  },
  { path: '/billing', name: 'billing', component: () => import('./views/BillingView.vue') },
  { path: '/earn', name: 'earn', component: () => import('./views/EarnView.vue') },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  // Wait a tick so lazily loaded pages have rendered the #anchor before scrolling to it.
  scrollBehavior: (to) =>
    to.hash
      ? new Promise((resolve) => setTimeout(() => resolve({ el: to.hash, behavior: 'smooth' }), 150))
      : { top: 0 },
})
