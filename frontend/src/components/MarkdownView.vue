<script setup>
import { computed } from 'vue'
import { marked } from 'marked'
import DOMPurify from 'dompurify'

/**
 * Renders Markdown (session plans, debriefs) as formatted text.
 * The HTML is sanitised first: the text comes from the AI and from users.
 */
const props = defineProps({ source: { type: String, default: '' } })

const html = computed(() => DOMPurify.sanitize(marked.parse(props.source || '', { breaks: true })))
</script>

<template>
  <!-- eslint-disable-next-line vue/no-v-html -- sanitised above -->
  <div class="markdown" v-html="html" />
</template>

<style scoped>
.markdown { color: var(--text); line-height: 1.6; }
.markdown :deep(h1), .markdown :deep(h2), .markdown :deep(h3) {
  font-size: 13px; font-weight: 650; text-transform: uppercase; letter-spacing: 0.05em;
  color: var(--text-muted); margin: 18px 0 6px;
}
.markdown :deep(> :first-child) { margin-top: 0; }
.markdown :deep(p) { margin: 0 0 8px; }
.markdown :deep(ul), .markdown :deep(ol) { margin: 0 0 8px; padding-left: 20px; display: grid; gap: 3px; }
.markdown :deep(strong) { color: var(--text-strong); }
.markdown :deep(blockquote) {
  margin: 0 0 8px; padding: 8px 12px; border-left: 3px solid var(--primary);
  background: var(--primary-soft); border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}
</style>
