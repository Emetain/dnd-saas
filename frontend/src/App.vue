<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppBanner from './components/AppBanner.vue'
import AppNavbar from './components/AppNavbar.vue'
import WelcomeView from './views/WelcomeView.vue'
import { useSession } from './stores/session'

const { state, restore } = useSession()
const route = useRoute()

/** The banner is only for the home page, and for visitors who are not signed in yet. */
const showBanner = computed(() => !state.user || route.name === 'dashboard')

onMounted(restore)
</script>

<template>
  <div v-if="state.loading" class="boot"><span class="spinner" /></div>
  <template v-else>
    <!-- The navbar is sticky and stays at the top; the banner below it scrolls away. -->
    <AppNavbar />
    <AppBanner v-if="showBanner" />
    <main>
      <RouterView v-if="state.user" :key="state.user.id" />
      <WelcomeView v-else />
    </main>
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
