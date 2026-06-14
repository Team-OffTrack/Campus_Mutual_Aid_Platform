import { defineStore } from 'pinia'
import { ref } from 'vue'
import { myFavorites } from '@/api/demand'

export const useFavoritesStore = defineStore('favorites', () => {
  // Set of favorited demand IDs for quick lookup
  const favoritedIds = ref(new Set())

  /** Load all favorited demand IDs from the server. */
  async function loadFavorites() {
    try {
      const page = await myFavorites({ pageNum: 1, pageSize: 500 })
      favoritedIds.value = new Set((page.records || []).map(d => d.demandId))
    } catch {
      // Non-critical — store stays empty, components show default unfavorited state
    }
  }

  /** Clear all favorited IDs (on logout). */
  function clearFavorites() {
    favoritedIds.value = new Set()
  }

  /** Optimistic add: update local state before server response. */
  function addOptimistic(demandId) {
    const s = new Set(favoritedIds.value)
    s.add(demandId)
    favoritedIds.value = s
  }

  /** Optimistic remove: update local state before server response. */
  function removeOptimistic(demandId) {
    const s = new Set(favoritedIds.value)
    s.delete(demandId)
    favoritedIds.value = s
  }

  /** Check if a demand is favorited. */
  function isFavorited(demandId) {
    return favoritedIds.value.has(demandId)
  }

  return { favoritedIds, loadFavorites, clearFavorites, addOptimistic, removeOptimistic, isFavorited }
})
