<template>
  <div v-if="show" class="emoji-overlay" @click.self="close">
    <div class="emoji-panel">
      <div class="emoji-grid">
        <span v-for="(e, i) in EMOJIS" :key="i" class="emoji-item"
          @click="pick(e)">{{ e }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({ show: Boolean })
const emit = defineEmits(['update:show', 'select'])

const EMOJIS = [
  '😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😍','🥰','😘','😗','😋',
  '😛','😜','🤪','😝','🤗','🤔','🤐','😐','😑','😶','🙄','😏','😣','😥','😮','😯',
  '😪','😫','😴','😌','😒','😓','😔','😕','🙃','🤑','😲','😨','😰','😱','🤯','😢',
  '😭','😩','😤','😡','🤬','💀','👻','🤖',
  '👍','👎','👌','✌','🤞','🤟','🤘','👋','🤚','🖐','✋','👆','👇','👉','👈',
  '🙌','👏','🙏','💪','🤝',
  '❤️','🧡','💛','💚','💙','💜','🖤','💔','💕','💞','💓','💗','💖','💘','💝',
  '🍎','🍊','🍋','🍉','🍇','🍓','🍒','🍑','🍕','🍔','🍟','🍣','🍦','🍰','☕','🍺',
  '🌞','🌝','🌍','🔥','⭐','✨','🌈','☀️','🌸','🌺','🌻','💐','🌱',
  '🎁','🎉','🎈','🎄','📱','💻','⌚','📷','💡','💰','🔑','📦',
  '🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🐮','🐷','🐸','🐵','🐔','🐧',
  '🐦','🦄','🐝','🦋',
  '✅','❌','❓','❗','💯','🚫','💤','🔄','▶️','⏸️','⬆️','⬇️','➡️','⬅️',
]

function pick(emoji) {
  emit('select', emoji)
  close()
}

function close() {
  emit('update:show', false)
}
</script>

<style scoped>
.emoji-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: transparent;
}
.emoji-panel {
  position: fixed; bottom: 60px; left: 50%; transform: translateX(-50%);
  width: min(340px, 94vw); max-height: 280px;
  background: #fff; border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.15);
  overflow-y: auto; padding: 10px;
}
.emoji-grid {
  display: grid; grid-template-columns: repeat(10, 1fr); gap: 4px;
}
.emoji-item {
  font-size: 24px; text-align: center; padding: 4px 0;
  cursor: pointer; border-radius: 6px; user-select: none;
}
.emoji-item:active { background: var(--c-border, #ebedf0); }

@media (min-width: 768px) {
  .emoji-panel { left: auto; right: calc(50% - 360px + 10px); transform: none; }
}
</style>
