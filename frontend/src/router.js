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
  { path: '/account', name: 'account', component: () => import('./views/AccountView.vue') },
  // Reachable while signed out; every other page needs a signed-in user (see App.vue).
  { path: '/login', name: 'login', component: () => import('./views/AuthView.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('./views/AuthView.vue'), meta: { public: true } },
  { path: '/forgot-password', name: 'forgot-password', component: () => import('./views/ForgotPasswordView.vue'), meta: { public: true } },
  { path: '/reset-password', name: 'reset-password', component: () => import('./views/ResetPasswordView.vue'), meta: { public: true } },
  { path: '/verify-email', name: 'verify-email', component: () => import('./views/VerifyEmailView.vue'), meta: { public: true } },
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
