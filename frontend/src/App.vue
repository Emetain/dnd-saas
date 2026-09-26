<script setup>
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppBanner from './components/AppBanner.vue'
import AppNavbar from './components/AppNavbar.vue'
import UpgradeModal from './components/UpgradeModal.vue'
import VerifyEmailBanner from './components/VerifyEmailBanner.vue'
import WelcomeView from './views/WelcomeView.vue'
import { useSession } from './stores/session'

const { state, restore } = useSession()
const route = useRoute()
const router = useRouter()

/** The sign-in pages; everything else needs a signed-in user. */
const isPublicPage = computed(() => route.meta.public === true)

/** The banner is only for the home page, and for visitors who are not signed in yet. */
const showBanner = computed(() => (!state.user && !isPublicPage.value) || route.name === 'dashboard')

// A session that ends mid-use goes to sign-in, and comes back here afterwards.
watch(() => state.expired, (expired) => {
  if (expired && !isPublicPage.value) router.push({ name: 'login', query: { redirect: route.fullPath } })
})

onMounted(restore)
</script>

<template>
  <div v-if="state.loading" class="boot"><span class="spinner" /></div>
  <template v-else>
    <!-- The navbar is sticky and stays at the top; the banner below it scrolls away. -->
    <AppNavbar />
    <VerifyEmailBanner v-if="state.user && !state.user.emailVerified && route.name !== 'verify-email'" />
    <AppBanner v-if="showBanner" />
    <main>
      <RouterView v-if="state.user || isPublicPage" :key="state.user?.id ?? 'signed-out'" />
      <WelcomeView v-else />
    </main>
    <UpgradeModal />
  </template>
</template>

<style scoped>
.boot {
  min-height: 100vh;
  display: grid;
  place-items: center;
  color: var(--primary);
}
</style>
