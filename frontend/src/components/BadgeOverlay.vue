<template>
  <span v-if="badgeKey" class="badge-overlay">{{ emoji }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  badgeKey: { type: String, default: null }
})

const EMOJI_MAP = {
  FIRST_PUBLISH: '🎉', FIRST_ACCEPT: '🤝', TEN_COMPLETES: '🏆',
  FIRST_FIVE_STAR: '⭐', HUNDRED_STARS: '💯', CHECKIN_30: '🔥',
  HELPER: '💝', FIRST_REPORT_SUCCESS: '🦸', EASTER_EGG: '🐱'
}

const emoji = computed(() => EMOJI_MAP[props.badgeKey] || '🏅')
</script>

<style scoped>
.badge-overlay {
  position: absolute;
  bottom: -4px;
  right: -5px;
  font-size: 0.85em;
  line-height: 1;
  filter: drop-shadow(0 1.5px 2.5px rgba(0,0,0,0.35));
  z-index: 2;
  pointer-events: none;
}
</style>
