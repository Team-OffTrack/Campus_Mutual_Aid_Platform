import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserBadges } from '@/api/badge'

const STORAGE_KEY = 'earnedBadgeKeys'

/**
 * Manages the badge-earned fullscreen toast queue and tracking of
 * which badges the user has already seen the toast for.
 */
export const useBadgeToastStore = defineStore('badgeToast', () => {
  const queue = ref([])
  const current = ref(null)
  const visible = ref(false)
  const earnedKeys = ref(new Set(loadKeys()))

  function loadKeys() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]') }
    catch { return [] }
  }

  function persistKeys() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify([...earnedKeys.value]))
  }

  /** Fetch badges and enqueue any newly-earned ones for display. */
  async function checkNewBadges() {
    try {
      const badges = await getUserBadges()
      const earned = badges.filter(b => b.earned).map(b => b.badgeKey)
      const newKeys = earned.filter(k => !earnedKeys.value.has(k))
      if (newKeys.length === 0) return

      newKeys.forEach(k => earnedKeys.value.add(k))
      persistKeys()

      const newBadges = badges.filter(b => newKeys.includes(b.badgeKey))
      queue.value.push(...newBadges)
      if (!visible.value) showNext()
    } catch { /* silently fail — badge toast is non-critical */ }
  }

  /** Show the next badge in the queue. */
  function showNext() {
    if (queue.value.length === 0) {
      visible.value = false
      current.value = null
      return
    }
    current.value = queue.value.shift()
    visible.value = true
  }

  /** Dismiss the current toast and proceed to next in queue. */
  function dismiss() {
    visible.value = false
    setTimeout(() => showNext(), 350)
  }

  return { visible, current, checkNewBadges, dismiss }
})
