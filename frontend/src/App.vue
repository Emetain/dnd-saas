<script setup>
import { onMounted } from 'vue'
import AppNavbar from './components/AppNavbar.vue'
import WelcomeView from './views/WelcomeView.vue'
import { useSession } from './stores/session'

const { state, restore } = useSession()

onMounted(restore)
</script>

<template>
  <div v-if="state.loading" class="boot"><span class="spinner" /></div>
  <WelcomeView v-else-if="!state.user" />
  <template v-else>
    <AppNavbar />
    <main>
      <RouterView :key="state.user.id" />
    </main>
  </template>
</template>

<style scoped>
.boot {
  min-height: 100vh;
  display: grid;
  place-items: center;
  color: var(--blue-600);
}
</style>
