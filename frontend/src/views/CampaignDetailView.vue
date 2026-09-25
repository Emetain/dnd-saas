<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  BookOpen, Flag, Gem, Lock, MapPin, Plus, ScrollText, Sparkles, Store, Swords, Trash2, UserRound, Users, CalendarDays,
} from 'lucide-vue-next'
import { campaignApi, characterApi } from '../services/api'
import { useSession } from '../stores/session'
import MemoryList from '../components/MemoryList.vue'
import CharacterForm from '../components/CharacterForm.vue'
import NavDropdown from '../components/NavDropdown.vue'
import GenerateModal from '../components/GenerateModal.vue'
import SessionsPanel from '../components/SessionsPanel.vue'
import ShopList from '../components/ShopList.vue'
import { findGenerator, generatorGroups } from '../data/generators'

const props = defineProps({ id: { type: String, required: true } })
const { isFree } = useSession()
const router = useRouter()
const route = useRoute()

const memory = ref(null)
const error = ref('')
/** The open tab is kept in the URL (?tab=sessions), so it survives a reload and can be linked to. */
const tab = ref(route.query.tab || 'overview')
watch(tab, (value) => router.replace({ query: { ...route.query, tab: value === 'overview' ? undefined : value } }))
const addingCharacter = ref(false)
/** The generator open in the pop-up, by slug; null when closed. */
const generating = ref(null)

/** Which generators each tab offers, so the world can grow right where you are looking. */
const tabGenerators = {
  overview: ['npc', 'quest', 'ask'],
  npcs: ['npc', 'boss'],
  locations: ['shop'],
  quests: ['quest'],
  encounters: ['encounter', 'random-encounter'],
  items: ['loot'],
  shops: ['shop'],
  characters: ['backstory'],
}
const currentGenerators = computed(() => (tabGenerators[tab.value] || []).map(findGenerator))

const moduleGroups = generatorGroups
  .map((group) => ({ ...group, generators: group.generators.filter((g) => g.kind !== 'world') }))
  .filter((group) => group.generators.length)

const tabs = computed(() => {
  const m = memory.value
  if (!m) return []
  return [
    { id: 'overview', label: 'Overview', icon: BookOpen },
    { id: 'npcs', label: 'NPCs', icon: UserRound, count: m.npcs.length },
    { id: 'locations', label: 'Locations', icon: MapPin, count: m.locations.length },
    { id: 'factions', label: 'Factions', icon: Flag, count: m.factions.length, locked: isFree.value },
    { id: 'quests', label: 'Quests', icon: ScrollText, count: m.quests.length },
    { id: 'encounters', label: 'Encounters', icon: Swords, count: m.encounters.length },
    { id: 'items', label: 'Items', icon: Gem, count: m.items.length },
    { id: 'shops', label: 'Shops', icon: Store, count: m.locations.filter((l) => l.type === 'SHOP').length },
    { id: 'sessions', label: 'Sessions', icon: CalendarDays, count: m.sessions.length },
    { id: 'characters', label: 'Party', icon: Users, count: m.characters.length },
  ]
})

async function load() {
  try {
    memory.value = await campaignApi.memory(props.id)
  } catch (e) {
    error.value = e.message
  }
}

async function addCharacter(character) {
  await characterApi.create(props.id, character)
  addingCharacter.value = false
  await load()
}

async function removeCampaign() {
  if (!window.confirm(`Delete "${memory.value.campaign.name}" and everything it remembers? This cannot be undone.`)) return
  await campaignApi.remove(props.id)
  router.push('/campaigns')
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div v-if="error" class="alert alert-error">{{ error }}</div>
    <div v-else-if="!memory" class="muted">Loading…</div>

    <template v-else>
      <div class="page-header">
        <div>
          <div class="row" style="margin-bottom: 8px">
            <span class="badge" :class="memory.campaign.kind === 'ONE_SHOT' ? 'badge-red' : 'badge-blue'">
              {{ memory.campaign.kind === 'ONE_SHOT' ? 'One-shot' : 'Campaign' }}
            </span>
            <span class="muted small">{{ memory.campaign.system }}</span>
          </div>
          <h1>{{ memory.campaign.name }}</h1>
          <p>{{ memory.campaign.description || 'No pitch yet.' }}</p>
        </div>
        <div class="row">
          <NavDropdown label="Generate here" align="right" class="generate">
            <div v-for="group in moduleGroups" :key="group.id" class="gen-group">
              <div class="gen-title">{{ group.label }}</div>
              <button
                v-for="gen in group.generators"
                :key="gen.slug"
                class="gen-item"
                @click="generating = gen.slug"
              >
                <component :is="gen.icon" :size="15" /> {{ gen.label }}
                <span class="muted small cost">{{ gen.cost }}</span>
                <Lock v-if="isFree && !gen.free" :size="12" class="lock" />
              </button>
            </div>
          </NavDropdown>
          <button class="btn btn-ghost" title="Delete campaign" @click="removeCampaign"><Trash2 :size="16" /></button>
        </div>
      </div>

      <div class="tabs">
        <button v-for="t in tabs" :key="t.id" :class="{ active: tab === t.id }" @click="tab = t.id">
          <component :is="t.icon" :size="15" />
          {{ t.label }}
          <span v-if="t.count != null" class="badge">{{ t.count }}</span>
          <Lock v-if="t.locked" :size="12" class="lock" />
        </button>
      </div>

      <div v-if="currentGenerators.length" class="tab-actions">
        <button
          v-for="gen in currentGenerators" :key="gen.slug"
          class="btn btn-sm" :class="gen === currentGenerators[0] ? 'btn-ai' : 'btn-secondary'"
          @click="generating = gen.slug"
        >
          <Sparkles :size="14" /> Generate {{ gen.label }}
          <span class="cost-chip">{{ gen.cost }}</span>
          <Lock v-if="isFree && !gen.free" :size="12" />
        </button>
      </div>

      <section v-if="tab === 'overview'" class="overview">
        <div class="card lore">
          <h2>World lore</h2>
          <p v-if="memory.campaign.worldLore" class="prose">{{ memory.campaign.worldLore }}</p>
          <p v-else class="muted">No lore yet. Generate NPCs, locations and quests and this world will start to fill in.</p>
        </div>
        <aside class="stack">
          <div class="card">
            <h3>Campaign memory</h3>
            <p class="muted small">Everything the AI knows about this world when it generates.</p>
            <ul class="memory-stats">
              <li><span>Locations</span><strong>{{ memory.stats.locations }}</strong></li>
              <li><span>Factions</span><strong>{{ memory.stats.factions }}</strong></li>
              <li><span>NPCs</span><strong>{{ memory.stats.npcs }}</strong></li>
              <li><span>Quests</span><strong>{{ memory.stats.quests }}</strong></li>
              <li><span>Items</span><strong>{{ memory.stats.items }}</strong></li>
              <li><span>Encounters</span><strong>{{ memory.stats.encounters }}</strong></li>
              <li><span>Relationships</span><strong>{{ memory.stats.relationships }}</strong></li>
            </ul>
          </div>
        </aside>
      </section>

      <MemoryList
        v-else-if="tab === 'npcs'" :items="memory.npcs" :tag-keys="['role', 'race', 'occupation', 'status']"
        :fields="[['personality', 'Personality'], ['motivation', 'Motivation'], ['secret', 'Secret'], ['voice', 'Voice'], ['location', 'Lives in'], ['faction', 'Faction']]"
        empty-text="No NPCs yet — try the NPC generator."
      />
      <MemoryList
        v-else-if="tab === 'locations'" :items="memory.locations" :tag-keys="['type']"
        :fields="[['atmosphere', 'Atmosphere'], ['parent', 'Part of']]" empty-text="No locations yet."
      />
      <template v-else-if="tab === 'factions'">
        <div v-if="isFree" class="alert alert-info locked">
          <Lock :size="16" />
          <span>Factions are part of Pro — they make large worlds hang together. <RouterLink to="/billing#plans">Compare plans</RouterLink></span>
        </div>
        <MemoryList
          :items="memory.factions" :tag-keys="['type', 'status']"
          :fields="[['goals', 'Goals'], ['ideology', 'Ideology'], ['headquarters', 'Headquarters']]" empty-text="No factions yet."
        />
      </template>
      <MemoryList
        v-else-if="tab === 'quests'" :items="memory.quests" title-key="title" :tag-keys="['status']"
        :fields="[['hook', 'Hook'], ['objective', 'Objective'], ['reward', 'Reward'], ['questGiver', 'Quest giver'], ['location', 'Location'], ['involvedNpcs', 'Involved']]"
        empty-text="No quests yet — try the Quest generator."
      />
      <MemoryList
        v-else-if="tab === 'encounters'" :items="memory.encounters" title-key="title" :tag-keys="['calculatedDifficulty']"
        :fields="[['objective', 'Objective'], ['terrain', 'Terrain'], ['creatures', 'Creatures'], ['adjustedExperience', 'Adjusted XP']]"
        empty-text="No encounters yet — try the Encounter generator."
      />
      <MemoryList
        v-else-if="tab === 'items'" :items="memory.items" :tag-keys="['type', 'rarity']"
        :fields="[['properties', 'Properties'], ['history', 'History'], ['location', 'Location']]" empty-text="No items yet — try the Loot generator."
      />
      <ShopList v-else-if="tab === 'shops'" :memory="memory" />
      <SessionsPanel v-else-if="tab === 'sessions'" :memory="memory" @changed="load" />
      <section v-else-if="tab === 'characters'" class="stack">
        <CharacterForm v-if="addingCharacter" @create="addCharacter" @cancel="addingCharacter = false" />
        <button v-else class="btn btn-secondary add" @click="addingCharacter = true"><Plus :size="16" /> Add player character</button>
        <MemoryList
          :items="memory.characters" :tag-keys="['characterClass', 'race']"
          body-key="backstory"
          :fields="[['playerName', 'Player'], ['level', 'Level'], ['armorClass', 'AC'], ['passivePerception', 'Passive Perception']]"
          empty-text="No player characters yet."
        />
      </section>
    </template>

    <GenerateModal
      v-if="generating && memory"
      :slug="generating"
      :campaign-id="memory.campaign.id"
      @generated="load"
      @close="generating = null"
    />
  </div>
</template>

<style scoped>
.overview { display: grid; grid-template-columns: 1fr 300px; gap: 16px; align-items: start; }
@media (max-width: 900px) { .overview { grid-template-columns: 1fr; } }
.prose { white-space: pre-line; color: var(--text-secondary); line-height: 1.65; }
.memory-stats { list-style: none; padding: 0; margin: 12px 0 0; display: grid; gap: 6px; }
.memory-stats li { display: flex; justify-content: space-between; font-size: 13px; }
.memory-stats span { color: var(--text-muted); }
.lock { color: var(--accent); }
.locked { margin-bottom: 16px; }
.add { align-self: flex-start; }

.generate :deep(.trigger) { background: var(--primary); color: var(--on-color); }
.generate :deep(.trigger:hover), .generate :deep(.trigger.open) { background: var(--primary-hover); color: var(--on-color); }
.gen-group { margin-bottom: 6px; }
.gen-title { font-size: 11px; font-weight: 650; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-muted); padding: 6px 8px; }
.gen-item {
  display: flex; align-items: center; gap: 8px; padding: 6px 8px; border-radius: var(--radius-sm); color: var(--text-strong);
  width: 100%; border: none; background: none; font: inherit; cursor: pointer; text-align: left;
}
.gen-item:hover { background: var(--primary-soft); color: var(--primary-text); }
.gen-item .cost { margin-left: auto; }

.tab-actions { display: flex; justify-content: flex-end; gap: 8px; flex-wrap: wrap; margin: -6px 0 16px; }
.btn-ai { background: linear-gradient(135deg, var(--primary), var(--accent)); color: var(--on-color); }
.btn-ai:hover:not(:disabled) { filter: brightness(1.08); color: var(--on-color); }
.cost-chip { font-size: 11px; padding: 1px 6px; border-radius: 999px; background: rgba(0, 0, 0, 0.12); }
</style>
