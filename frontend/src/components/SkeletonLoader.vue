<template>
  <div class="skeleton-wrap" :style="{ padding }">
    <!-- Card skeleton -->
    <template v-if="variant === 'card'">
      <div v-for="i in count" :key="i" class="skeleton-card-item">
        <div class="sk-row">
          <div class="skeleton skeleton-avatar" :style="{ width: avatarSize, height: avatarSize }"></div>
          <div class="sk-body">
            <div class="skeleton skeleton-title" style="width:55%"></div>
            <div class="skeleton skeleton-text" style="width:85%"></div>
            <div class="skeleton skeleton-text" style="width:40%"></div>
          </div>
        </div>
      </div>
    </template>

    <!-- List skeleton -->
    <template v-else-if="variant === 'list'">
      <div v-for="i in count" :key="i" class="skeleton-list-item">
        <div class="skeleton skeleton-title" :style="{ width: titleWidth }"></div>
        <div class="skeleton skeleton-text" style="width:90%"></div>
        <div class="skeleton skeleton-text" style="width:70%"></div>
      </div>
    </template>

    <!-- Detail skeleton -->
    <template v-else-if="variant === 'detail'">
      <div class="skeleton skeleton-title" style="width:70%"></div>
      <div class="skeleton skeleton-text" style="width:100%"></div>
      <div class="skeleton skeleton-text" style="width:100%"></div>
      <div class="skeleton skeleton-text" style="width:60%"></div>
      <div style="height:16px"></div>
      <div class="skeleton skeleton-card" style="height:100px"></div>
    </template>

    <!-- Grid skeleton (for feature tiles) -->
    <template v-else-if="variant === 'grid'">
      <div class="sk-grid" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
        <div v-for="i in count" :key="i" class="skeleton skeleton-card" style="height:100px"></div>
      </div>
    </template>

    <!-- Inline text skeleton -->
    <template v-else>
      <div v-for="i in count" :key="i" class="skeleton skeleton-text" :style="{ width: textWidth }"></div>
    </template>
  </div>
</template>

<script setup>
defineProps({
  variant: { type: String, default: 'text' },  // card | list | detail | grid | text
  count: { type: Number, default: 3 },
  padding: { type: String, default: '16px' },
  avatarSize: { type: String, default: '44px' },
  titleWidth: { type: String, default: '50%' },
  textWidth: { type: String, default: '80%' },
  columns: { type: Number, default: 2 },
})
</script>

<style scoped>
.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-card-item {
  background: var(--c-surface);
  border-radius: var(--r-large);
  padding: 16px;
  box-shadow: var(--s-xs);
}

.sk-row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.sk-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.skeleton-list-item {
  background: var(--c-surface);
  border-radius: var(--r-large);
  padding: 16px;
  box-shadow: var(--s-xs);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sk-grid {
  display: grid;
  gap: 12px;
}
</style>
