<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="route.meta.transition || 'page'" mode="out-in">
      <component :is="Component" :key="route.path" />
    </transition>
  </router-view>
</template>

<script setup>
// Root component with animated route transitions
</script>

<style>
/* Global page transition — M3E spring spatial */
.page-enter-active,
.page-leave-active {
  transition: opacity var(--dur-medium3) var(--ease-emphasized-decelerate),
              transform var(--dur-medium3) var(--ease-emphasized-decelerate);
}
.page-enter-from {
  opacity: 0;
  transform: translateY(14px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
