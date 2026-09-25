/**
 * Rewarded ads: "watch an ad, earn tokens".
 *
 * Providers:
 *  - "google"    — Google Ad Manager rewarded ads for the web (GPT). Switched on by
 *                  setting VITE_GAM_REWARDED_AD_UNIT to your ad unit path, e.g.
 *                  "/1234567/narrion_rewarded". Ads only fill on an approved, public
 *                  domain (not on localhost), and in the EU only after cookie consent.
 *  - "simulated" — a countdown in place of an ad, for local development only.
 *
 * Outside development, without an ad unit, ads are simply unavailable.
 */
const googleAdUnit = import.meta.env.VITE_GAM_REWARDED_AD_UNIT

export const adProvider = googleAdUnit ? 'google' : import.meta.env.DEV ? 'simulated' : null

let gptLoading = null

/** Loads Google Publisher Tag once. Fails if an ad blocker stops it. */
function loadGpt() {
  if (!gptLoading) {
    gptLoading = new Promise((resolve, reject) => {
      window.googletag = window.googletag || { cmd: [] }
      const script = document.createElement('script')
      script.async = true
      script.src = 'https://securepubads.g.doubleclick.net/tag/js/gpt.js'
      script.onload = resolve
      script.onerror = () => {
        gptLoading = null
        reject(new Error('The ad could not be loaded — is an ad blocker active?'))
      }
      document.head.appendChild(script)
    })
  }
  return gptLoading
}

/**
 * Shows one Google rewarded ad. Resolves with:
 *  'granted'     — watched to the end, the reward may be claimed
 *  'dismissed'   — closed early, no reward
 *  'no-fill'     — the network had no ad to show right now
 *  'unsupported' — this page or device cannot show rewarded ads
 */
export async function showGoogleRewardedAd() {
  await loadGpt()
  return new Promise((resolve) => {
    const { googletag } = window
    googletag.cmd.push(() => {
      const slot = googletag.defineOutOfPageSlot(googleAdUnit, googletag.enums.OutOfPageFormat.REWARDED)
      if (!slot) {
        resolve('unsupported')
        return
      }
      const pubads = googletag.pubads()
      let granted = false

      const listeners = {
        rewardedSlotReady: (event) => { if (event.slot === slot) event.makeRewardedVisible() },
        rewardedSlotGranted: (event) => { if (event.slot === slot) granted = true },
        rewardedSlotClosed: (event) => { if (event.slot === slot) finish(granted ? 'granted' : 'dismissed') },
        slotRenderEnded: (event) => { if (event.slot === slot && event.isEmpty) finish('no-fill') },
      }
      function finish(result) {
        Object.entries(listeners).forEach(([name, fn]) => pubads.removeEventListener(name, fn))
        googletag.destroySlots([slot])
        resolve(result)
      }

      slot.addService(pubads)
      Object.entries(listeners).forEach(([name, fn]) => pubads.addEventListener(name, fn))
      googletag.enableServices()
      googletag.display(slot)
    })
  })
}
