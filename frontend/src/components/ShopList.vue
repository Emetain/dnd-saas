<script setup>
import { computed } from 'vue'
import { Store, UserRound } from 'lucide-vue-next'

/**
 * Shops in a campaign: SHOP locations, with the shopkeeper who lives there
 * and the items for sale there, all taken from Campaign Memory.
 */
const props = defineProps({ memory: { type: Object, required: true } })

const shops = computed(() =>
  props.memory.locations
    .filter((location) => location.type === 'SHOP')
    .map((shop) => ({
      ...shop,
      keepers: props.memory.npcs.filter((npc) => npc.location?.id === shop.id),
      stock: props.memory.items.filter((item) => item.location?.id === shop.id),
    })),
)

/** The generator writes "For sale at <shop>: <price>" into an item's notes. */
function price(item) {
  const match = /For sale at [^:]*:\s*(.+)$/.exec(item.notes || '')
  return match ? match[1] : '—'
}

function label(value) {
  return value ? String(value).replaceAll('_', ' ').toLowerCase() : ''
}
</script>

<template>
  <div v-if="!shops.length" class="empty">
    <p>No shops yet — generate one and it appears here with its shopkeeper and stock.</p>
  </div>
  <div v-else class="stack">
    <article v-for="shop in shops" :key="shop.id" class="card shop">
      <div class="row">
        <span class="icon"><Store :size="18" /></span>
        <div>
          <h3>{{ shop.name }}</h3>
          <span v-if="shop.parent" class="muted small">in {{ shop.parent.name }}</span>
        </div>
      </div>
      <p v-if="shop.description" class="body">{{ shop.description }}</p>
      <p v-if="shop.atmosphere" class="muted small">{{ shop.atmosphere }}</p>

      <div v-for="keeper in shop.keepers" :key="keeper.id" class="keeper">
        <UserRound :size="15" />
        <span><strong>{{ keeper.name }}</strong> · {{ keeper.race }} {{ keeper.occupation.toLowerCase() }}</span>
        <span v-if="keeper.personality" class="muted small">— {{ keeper.personality }}</span>
      </div>

      <table v-if="shop.stock.length" class="table stock">
        <thead><tr><th>Item</th><th>Type</th><th>Rarity</th><th style="text-align: right">Price</th></tr></thead>
        <tbody>
          <tr v-for="item in shop.stock" :key="item.id">
            <td>
              <strong>{{ item.name }}</strong>
              <div v-if="item.description" class="muted small">{{ item.description }}</div>
            </td>
            <td>{{ item.type }}</td>
            <td><span class="badge">{{ label(item.rarity) }}</span></td>
            <td class="price">{{ price(item) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="muted small">No stock recorded.</p>
    </article>
  </div>
</template>

<style scoped>
.shop { display: flex; flex-direction: column; gap: 10px; }
.shop h3 { margin: 0; }
.icon {
  width: 34px; height: 34px; border-radius: 8px; display: grid; place-items: center; flex-shrink: 0;
  background: var(--primary-soft); color: var(--primary-text);
}
.body { color: var(--text-secondary); }
.keeper {
  display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding: 8px 10px;
  background: var(--surface-sunken); border-radius: var(--radius-sm);
}
.stock { margin-top: 4px; }
.price { text-align: right; white-space: nowrap; font-weight: 600; }
</style>
