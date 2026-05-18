<template>
  <Teleport to="body">
    <Transition name="iv-fade">
      <div v-if="show" class="iv-root" @click.self="emitClose">
        <!-- Close button -->
        <div class="iv-close" @click="emitClose" @click.stop>
          <van-icon name="cross" size="24" color="#fff" />
        </div>

        <!-- Image counter -->
        <div class="iv-index">{{ current + 1 }} / {{ images.length }}</div>

        <!-- Swipe viewport -->
        <div
          ref="swipeEl"
          class="iv-viewport"
          @mousedown="onMouseDown"
          @mousemove="onMouseMove"
          @mouseup="onMouseUp"
          @mouseleave="onMouseUp"
          @touchstart="onTouchStart"
          @touchmove="onTouchMove"
          @touchend="onTouchEnd"
        >
          <img
            :src="images[current]"
            :key="current"
            class="iv-image"
            draggable="false"
            @click.stop
          />
        </div>

        <!-- Prev / Next buttons (desktop) -->
        <button v-if="images.length > 1" class="iv-arrow iv-arrow--left" @click.stop="prev">
          <van-icon name="arrow-left" size="20" color="#fff" />
        </button>
        <button v-if="images.length > 1" class="iv-arrow iv-arrow--right" @click.stop="next">
          <van-icon name="arrow" size="20" color="#fff" />
        </button>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: Boolean,
  images: { type: Array, default: () => [] },
  startPosition: { type: Number, default: 0 }
})

const emit = defineEmits(['close', 'update:show'])

const current = ref(props.startPosition)
const swipeEl = ref(null)

// Keep current in sync with startPosition
watch(() => props.startPosition, (v) => { current.value = v })
watch(() => props.show, (v) => {
  if (v) current.value = props.startPosition
})

function emitClose() {
  emit('update:show', false)
  emit('close')
}

function prev() {
  if (current.value > 0) current.value--
}
function next() {
  if (current.value < props.images.length - 1) current.value++
}

// ── Touch swipe ──
let touchStartX = 0
let touchStartY = 0
let touchMoved = false

function onTouchStart(e) {
  if (e.touches.length === 1) {
    touchStartX = e.touches[0].clientX
    touchStartY = e.touches[0].clientY
    touchMoved = false
  }
}

function onTouchMove(e) {
  if (e.touches.length === 1 && !touchMoved) {
    const dx = Math.abs(e.touches[0].clientX - touchStartX)
    const dy = Math.abs(e.touches[0].clientY - touchStartY)
    if (dx > 10 || dy > 10) touchMoved = true
  }
}

function onTouchEnd(e) {
  if (!touchMoved) return
  const dx = (e.changedTouches[0]?.clientX || touchStartX) - touchStartX
  if (Math.abs(dx) > 60) {
    if (dx < -30 && current.value < props.images.length - 1) current.value++
    else if (dx > 30 && current.value > 0) current.value--
  }
}

// ── Mouse drag swipe ──
let mouseDown = false
let mouseStartX = 0
let mouseMoved = false

function onMouseDown(e) {
  if (e.button === 0) {
    mouseDown = true
    mouseStartX = e.clientX
    mouseMoved = false
    e.preventDefault()
  }
}

function onMouseMove(e) {
  if (!mouseDown) return
  if (Math.abs(e.clientX - mouseStartX) > 5) mouseMoved = true
}

function onMouseUp(e) {
  if (!mouseDown) return
  mouseDown = false
  if (!mouseMoved) return
  const dx = e.clientX - mouseStartX
  if (Math.abs(dx) > 60) {
    if (dx < -30 && current.value < props.images.length - 1) current.value++
    else if (dx > 30 && current.value > 0) current.value--
  }
}

// ── Keyboard ──
function onKeydown(e) {
  if (!props.show) return
  if (e.key === 'Escape') emitClose()
  if (e.key === 'ArrowLeft') prev()
  if (e.key === 'ArrowRight') next()
}

import { onMounted, onUnmounted } from 'vue'
onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.iv-root {
  position: fixed;
  inset: 0;
  z-index: 3000;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
  -webkit-user-select: none;
}

/* Transition */
.iv-fade-enter-active, .iv-fade-leave-active {
  transition: opacity 0.3s ease;
}
.iv-fade-enter-from, .iv-fade-leave-to {
  opacity: 0;
}

/* Close */
.iv-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.iv-close:active { background: rgba(255,255,255,0.3); }

/* Index */
.iv-index {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 10;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(0,0,0,0.5);
}

/* Viewport */
.iv-viewport {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
}
.iv-viewport:active { cursor: grabbing; }

.iv-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  pointer-events: none;
}

/* Arrows */
.iv-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 44px; height: 44px;
  border: none;
  border-radius: 50%;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}
.iv-arrow:active { background: rgba(255,255,255,0.3); }
.iv-arrow--left  { left: 12px; }
.iv-arrow--right { right: 12px; }

@media (min-width: 768px) {
  .iv-arrow { width: 48px; height: 48px; }
  .iv-arrow--left  { left: 24px; }
  .iv-arrow--right { right: 24px; }
  .iv-close { top: 20px; right: 20px; }
  .iv-index { top: 24px; }
}
</style>
