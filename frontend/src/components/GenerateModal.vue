<script setup>
import { computed } from 'vue'
import AppModal from './AppModal.vue'
import UpgradePrompt from './UpgradePrompt.vue'
import ModuleGenerator from '../views/generators/ModuleGenerator.vue'
import { findGenerator } from '../data/generators'
import { useSession } from '../stores/session'

/** Runs one generator for one campaign in a pop-up, without leaving the campaign page. */
const props = defineProps({
  slug: { type: String, required: true },
  campaignId: { type: Number, required: true },
})
const emit = defineEmits(['close', 'generated'])
const { isFree } = useSession()

const generator = computed(() => findGenerator(props.slug))
</script>

<template>
  <AppModal
    :title="`Generate ${generator.label}`"
    :subtitle="`${generator.description} · ${generator.cost} tokens`"
    wide
    @close="emit('close')"
  >
    <UpgradePrompt v-if="isFree && !generator.free" :feature="generator.label" />
    <ModuleGenerator v-else :generator="generator" :fixed-campaign-id="campaignId" @generated="emit('generated', $event)" />
  </AppModal>
</template>
