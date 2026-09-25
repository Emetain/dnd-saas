<script setup>
defineProps({ campaign: { type: Object, required: true } })

function formatDate(iso) {
  return new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' })
}
</script>

<template>
  <RouterLink :to="`/campaigns/${campaign.id}`" class="card card-link campaign">
    <div class="row-between">
      <span class="badge" :class="campaign.kind === 'ONE_SHOT' ? 'badge-red' : 'badge-blue'">
        {{ campaign.kind === 'ONE_SHOT' ? 'One-shot' : 'Campaign' }}
      </span>
      <span class="muted small">{{ campaign.system }}</span>
    </div>
    <h3>{{ campaign.name }}</h3>
    <p class="muted description">{{ campaign.description || 'No description yet.' }}</p>
    <div class="meta muted small">
      <span>{{ campaign.sessionCount }} sessions</span>
      <span>{{ campaign.characterCount }} characters</span>
      <span>Updated {{ formatDate(campaign.updatedAt) }}</span>
    </div>
  </RouterLink>
</template>

<style scoped>
.campaign { display: flex; flex-direction: column; gap: 8px; }
.campaign h3 { margin: 4px 0 0; }
.description {
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 42px;
}
.meta { display: flex; gap: 12px; flex-wrap: wrap; margin-top: auto; }
</style>
