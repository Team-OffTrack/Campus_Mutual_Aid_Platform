<template>
  <teleport to="body">
    <transition name="badge-toast">
      <div v-if="store.visible && store.current" class="badge-toast-overlay"
        @click="store.dismiss()">
        <div class="badge-toast-card">
          <div class="badge-toast-emoji">{{ store.current.emoji }}</div>
          <div class="badge-toast-label">成就解锁</div>
          <div class="badge-toast-name">{{ store.current.displayName }}</div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { watch } from 'vue'
import { useBadgeToastStore } from '@/stores/badgeToast'

const store = useBadgeToastStore()

let timer = null

watch(() => store.visible, (v) => {
  clearTimeout(timer)
  if (v) {
    timer = setTimeout(() => store.dismiss(), 2500)
  }
})
</script>

<style scoped>
.badge-toast-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0, 0, 0, 0.82);
  backdrop-filter: blur(12px);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}

.badge-toast-card {
  text-align: center;
  user-select: none;
}

.badge-toast-emoji {
  font-size: 96px;
  line-height: 1;
  margin-bottom: 16px;
  animation: badge-pop-in 0.6s var(--ease-emphasized-decelerate) both,
             badge-pulse 2s 0.8s ease-in-out infinite;
}

.badge-toast-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.55);
  text-transform: uppercase;
  letter-spacing: 3px;
  margin-bottom: 8px;
  animation: badge-fade-up 0.5s 0.25s ease both;
}

.badge-toast-name {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  animation: badge-fade-up 0.5s 0.35s ease both;
}

/* ── Entrance: pop-in spring ── */
@keyframes badge-pop-in {
  0%   { transform: scale(0); opacity: 0; }
  60%  { transform: scale(1.15); opacity: 1; }
  100% { transform: scale(1); }
}

/* ── Subtle pulse ── */
@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50%      { transform: scale(1.06); }
}

/* ── Text fade-up ── */
@keyframes badge-fade-up {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── Overlay transition ── */
.badge-toast-enter-active { transition: opacity 0.25s ease; }
.badge-toast-leave-active { transition: opacity 0.3s ease; }
.badge-toast-enter-from,
.badge-toast-leave-to { opacity: 0; }
</style>
