<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Coins, CreditCard, Gift, History, LayoutDashboard, Lock, LogOut, Plus, ScrollText, Castle, Library,
  Moon, Sun, Monitor,
} from 'lucide-vue-next'
import NavDropdown from './NavDropdown.vue'
import NarrionLogo from './NarrionLogo.vue'
import { useTheme } from '../stores/theme'
import { useWelcome } from '../stores/welcome'
import { generatorGroups } from '../data/generators'
import { tierLabels, useSession } from '../stores/session'

const { state, isFree, signOut } = useSession()
const { openAccountForm } = useWelcome()
const { preference, resolved, setTheme, toggleTheme } = useTheme()
const themeOptions = [
  { value: 'light', label: 'Light', icon: Sun },
  { value: 'dark', label: 'Dark', icon: Moon },
  { value: 'system', label: 'System', icon: Monitor },
]
const route = useRoute()
const router = useRouter()

const initials = computed(() =>
  (state.user?.displayName || '?').split(/\s+/).map((part) => part[0]).join('').slice(0, 2).toUpperCase(),
)
const section = computed(() => route.path.split('/')[1] || 'dashboard')

function logout() {
  signOut()
  router.push('/')
}
</script>

<template>
  <header class="nav">
    <div class="nav-inner">
      <RouterLink to="/" class="brand" aria-label="Narrion home">
        <NarrionLogo :size="46" />
      </RouterLink>

      <nav v-if="state.user" class="links">
        <RouterLink to="/" class="link" :class="{ active: section === 'dashboard' }">
          <LayoutDashboard :size="15" /> Dashboard
        </RouterLink>

        <NavDropdown label="Campaigns" :active="section === 'campaigns'">
          <RouterLink to="/campaigns" class="menu-item">
            <Library :size="16" />
            <span><strong>All campaigns</strong><small>Your campaigns and one-shots</small></span>
          </RouterLink>
          <RouterLink to="/generators/one-shot" class="menu-item">
            <ScrollText :size="16" />
            <span><strong>New one-shot</strong><small>A single-sitting adventure</small></span>
          </RouterLink>
          <RouterLink to="/generators/campaign" class="menu-item">
            <Castle :size="16" />
            <span>
              <strong>New campaign <span v-if="isFree" class="badge badge-red">Pro</span></strong>
              <small>A whole world, generated</small>
            </span>
          </RouterLink>
          <RouterLink to="/campaigns?new=1" class="menu-item">
            <Plus :size="16" />
            <span><strong>Start from scratch</strong><small>Create an empty campaign or one-shot</small></span>
          </RouterLink>
        </NavDropdown>

        <NavDropdown label="Generators" :active="section === 'generators'" wide>
          <div class="mega">
            <div v-for="group in generatorGroups" :key="group.id" class="mega-group">
              <div class="mega-title">{{ group.label }}</div>
              <RouterLink
                v-for="gen in group.generators"
                :key="gen.slug"
                :to="`/generators/${gen.slug}`"
                class="mega-item"
              >
                <component :is="gen.icon" :size="15" />
                <span>{{ gen.label }}</span>
                <Lock v-if="isFree && !gen.free" :size="12" class="lock" />
              </RouterLink>
            </div>
          </div>
          <RouterLink to="/generators" class="mega-footer">Browse all generators →</RouterLink>
        </NavDropdown>

        <NavDropdown label="Tokens" :active="section === 'billing' || section === 'earn'">
          <RouterLink to="/billing" class="menu-item">
            <History :size="16" />
            <span><strong>Balance &amp; history</strong><small>Where your tokens went</small></span>
          </RouterLink>
          <RouterLink to="/billing#plans" class="menu-item">
            <CreditCard :size="16" />
            <span><strong>Plans</strong><small>Free, Pro, Pro+ and Ultimate DM</small></span>
          </RouterLink>
          <RouterLink to="/billing#packs" class="menu-item">
            <Coins :size="16" />
            <span><strong>Buy tokens</strong><small>Never expire</small></span>
          </RouterLink>
          <RouterLink to="/earn" class="menu-item">
            <Gift :size="16" />
            <span><strong>Earn tokens</strong><small>Ads, referrals and daily streaks</small></span>
          </RouterLink>
        </NavDropdown>
      </nav>

      <div v-else class="links" />

      <div class="right">
        <RouterLink v-if="state.user" to="/billing" class="tokens" title="Your token balance">
          <Coins :size="15" />
          <strong>{{ state.balance?.currentTokens ?? '—' }}</strong>
          <span class="tokens-label">tokens</span>
        </RouterLink>

        <button
          class="icon-btn"
          :title="resolved === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'"
          :aria-label="resolved === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'"
          @click="toggleTheme"
        >
          <Sun v-if="resolved === 'dark'" :size="16" />
          <Moon v-else :size="16" />
        </button>

        <template v-if="!state.user">
          <button class="btn btn-sm btn-nav" @click="openAccountForm('existing')">Sign in</button>
          <button class="btn btn-sm btn-accent" @click="openAccountForm('register')">Register</button>
        </template>

        <NavDropdown v-if="state.user" :label="initials" align="right">
          <div class="account">
            <strong>{{ state.user.displayName }}</strong>
            <span class="muted small">{{ state.user.email }} · id {{ state.user.id }}</span>
            <span class="badge" :class="isFree ? 'badge' : 'badge-blue'">{{ tierLabels[state.user.subscriptionTier] }}</span>
          </div>
          <div class="theme-picker">
            <span class="theme-label">Theme</span>
            <div class="segmented">
              <button
                v-for="option in themeOptions" :key="option.value"
                :class="{ active: preference === option.value }" @click="setTheme(option.value)"
              >
                <component :is="option.icon" :size="13" /> {{ option.label }}
              </button>
            </div>
          </div>
          <button class="menu-item as-button" @click="logout">
            <LogOut :size="16" />
            <span><strong>Switch account</strong><small>Sign in as someone else</small></span>
          </button>
        </NavDropdown>
      </div>
    </div>
  </header>
</template>

<style scoped>
.nav {
  position: sticky; top: 0; z-index: 40;
  background: var(--nav-bg);
  border-bottom: 1px solid var(--nav-border);
}
/*
 * Full-width bar: brand at the far left, account controls at the far right,
 * and the links lined up with the left edge of the page content (.page is a
 * centred 1180px column with 24px padding). With 20px bar padding and a
 * 200px brand, the links' offset works out to 50% - 778px; on screens too
 * narrow for that it falls back to 24px after the brand.
 */
.nav-inner {
  height: var(--nav-height);
  padding: 0 20px; display: flex; align-items: center;
}

.brand { display: flex; align-items: center; color: var(--on-color); width: 200px; flex-shrink: 0; }
.btn-nav { background: rgba(255, 255, 255, 0.1); color: var(--on-color); }
.btn-nav:hover { background: rgba(255, 255, 255, 0.18); color: var(--on-color); }
.brand:hover { color: var(--on-color); }

.icon-btn {
  width: 32px; height: 32px; border-radius: 999px; border: none; cursor: pointer;
  display: grid; place-items: center;
  background: rgba(255, 255, 255, 0.1); color: rgba(255, 255, 255, 0.85);
}
.icon-btn:hover { background: rgba(255, 255, 255, 0.18); color: var(--on-color); }

.theme-picker { padding: 4px 10px 10px; border-bottom: 1px solid var(--border); margin-bottom: 6px; display: flex; flex-direction: column; gap: 6px; }
.theme-label { font-size: 12px; font-weight: 600; color: var(--text-muted); }
.theme-picker .segmented button { padding: 5px 9px; font-size: 12px; }

.links {
  display: flex; align-items: center; gap: 2px; flex: 1; min-width: 0;
  margin-left: max(24px, calc(50% - 778px));
}
.link {
  display: inline-flex; align-items: center; gap: 6px; height: 36px; padding: 0 12px;
  border-radius: var(--radius-sm); color: rgba(255, 255, 255, 0.78); font-weight: 560;
}
.link:hover { background: rgba(255, 255, 255, 0.08); color: var(--on-color); }
.link.active { color: var(--on-color); background: rgba(255, 255, 255, 0.1); }

.right { display: flex; align-items: center; gap: 10px; margin-left: 16px; }
.tokens {
  display: inline-flex; align-items: center; gap: 6px; height: 32px; padding: 0 12px;
  border-radius: 999px; background: rgba(255, 255, 255, 0.1); color: var(--on-color);
}
.tokens:hover { background: rgba(255, 255, 255, 0.16); color: var(--on-color); }
.tokens-label { opacity: 0.7; font-size: 12px; }

/* Dropdown content */
.menu-item {
  display: flex; align-items: flex-start; gap: 10px; padding: 8px 10px;
  border-radius: var(--radius-sm); color: var(--text-secondary);
}
.menu-item:hover { background: var(--primary-soft); color: var(--primary-text); }
.menu-item span { display: flex; flex-direction: column; }
.menu-item strong { font-weight: 600; color: var(--text-strong); display: inline-flex; gap: 6px; align-items: center; }
.menu-item small { color: var(--text-muted); font-size: 12px; }
.menu-item svg { margin-top: 2px; flex-shrink: 0; }
.as-button { width: 100%; border: none; background: none; font: inherit; text-align: left; cursor: pointer; }

.mega { display: grid; grid-template-columns: repeat(3, 1fr); gap: 4px 16px; padding: 6px; }
.mega-group { display: flex; flex-direction: column; margin-bottom: 8px; }
.mega-title {
  font-size: 11px; font-weight: 650; text-transform: uppercase; letter-spacing: 0.06em;
  color: var(--text-muted); padding: 6px 8px;
}
.mega-item {
  display: flex; align-items: center; gap: 8px; padding: 7px 8px; border-radius: var(--radius-sm);
  color: var(--text-strong); font-weight: 520;
}
.mega-item:hover { background: var(--primary-soft); color: var(--primary-text); }
.mega-item .lock { margin-left: auto; color: var(--accent); }
.mega-footer {
  display: block; border-top: 1px solid var(--border); margin-top: 4px; padding: 10px 14px 4px;
  font-weight: 560; font-size: 13px;
}

.account { display: flex; flex-direction: column; gap: 4px; padding: 8px 10px 12px; border-bottom: 1px solid var(--border); margin-bottom: 6px; align-items: flex-start; }

@media (max-width: 900px) {
  .brand :deep(.word), .tokens-label { display: none; }
  .brand { width: auto; }
  .nav-inner { padding: 0 12px; }
  .links { margin-left: 12px; }
  .link { padding: 0 8px; }
  .mega { grid-template-columns: 1fr 1fr; }
}
</style>
