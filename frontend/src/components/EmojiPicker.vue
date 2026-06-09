<template>
  <transition name="emoji-fade">
    <div v-if="show" class="emoji-overlay" @click.self="close">
      <div class="emoji-panel">
        <!-- Category tabs -->
        <div class="emoji-tabs">
          <span v-for="cat in categories" :key="cat.key"
            class="emoji-tab" :class="{ 'tab-sel': activeCat === cat.key }"
            @click="activeCat = cat.key">{{ cat.icon }}</span>
        </div>

        <!-- Emoji grid -->
        <div class="emoji-grid">
          <span v-for="(e, i) in currentEmojis" :key="i" class="emoji-item"
            @click="pick(e)">{{ e }}</span>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed, ref } from 'vue'

defineProps({ show: Boolean })
const emit = defineEmits(['update:show', 'select'])

const activeCat = ref('faces')

const categories = [
  { key: 'faces',   icon: '😀' },
  { key: 'gestures', icon: '👍' },
  { key: 'hearts',  icon: '❤️' },
  { key: 'nature',  icon: '🌸' },
  { key: 'food',    icon: '🍕' },
  { key: 'animals', icon: '🐶' },
  { key: 'objects', icon: '💡' },
  { key: 'symbols', icon: '✅' },
]

const EMOJI_MAP = {
  faces:   ['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😍','🥰','😘','😗','😋','😛','😜','🤪','😝','🤗','🤔','😐','😑','😶','🙄','😏','😣','😥','😮','😯','😪','😫','😴','😌','😒','😓','😔','😕','🙃','🤑','😲','😨','😰','😱','🤯','😢','😭','😤','😡','🤬','💀','👻','🤖'],
  gestures: ['👍','👎','👌','✌️','🤞','🤟','🤘','👋','🤚','🖐️','✋','👆','👇','👉','👈','🙌','👏','🙏','💪','🤝','👊','✊','🤲','🫶'],
  hearts:  ['❤️','🧡','💛','💚','💙','💜','🖤','🤍','🤎','💔','💕','💞','💓','💗','💖','💘','💝','💟','♥️','💌','💋','🫀'],
  nature:  ['🌸','🌺','🌻','🌹','💐','🌱','🌿','🍀','🌵','🌴','🌳','🌈','⭐','✨','🔥','💧','🌊','☀️','🌙','🌍','❄️','☁️','⚡','🌪️'],
  food:    ['🍎','🍊','🍋','🍉','🍇','🍓','🍒','🍑','🥝','🍌','🍕','🍔','🍟','🍣','🍦','🍰','☕','🍺','🍩','🌮','🥗','🍜','🧋','🍿'],
  animals: ['🐶','🐱','🐭','🐹','🐰','🦊','🐻','🐼','🐨','🐯','🐮','🐷','🐸','🐵','🐔','🐧','🐦','🦄','🐝','🦋','🐙','🦀','🐬','🐳'],
  objects: ['📱','💻','⌚','📷','💡','💰','🔑','📦','🎁','🎉','🎈','🎄','📚','✏️','🎵','🎮','🛒','💊','🔮','🧲','🪄','🎀','🧸'],
  symbols: ['✅','❌','❓','❗','💯','🚫','💤','🔄','▶️','⏸️','⬆️','⬇️','➡️','⬅️','🔴','🟢','🔵','🟡','⚪','⚫','🟣','💮','♻️','©️','™️','🔞'],
}

const currentEmojis = computed(() => EMOJI_MAP[activeCat.value] || EMOJI_MAP.faces)

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
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: transparent;
}

.emoji-panel {
  position: fixed;
  bottom: 70px;
  left: 50%;
  transform: translateX(-50%);
  width: min(360px, 94vw);
  max-height: 340px;
  background: rgba(255,255,255,0.96);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-radius: 16px;
  border: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 8px 40px rgba(0,0,0,0.16), 0 2px 8px rgba(0,0,0,0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* Category tabs */
.emoji-tabs {
  display: flex;
  gap: 2px;
  padding: 8px 10px 4px;
  border-bottom: 1px solid var(--c-border);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
.emoji-tabs::-webkit-scrollbar { display: none; }

.emoji-tab {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  border-radius: 10px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background var(--ease), transform var(--ease);
}
.emoji-tab:active { transform: scale(0.9); }
.emoji-tab.tab-sel { background: var(--c-primary-container); }

/* Grid */
.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 2px;
  padding: 8px;
  overflow-y: auto;
  flex: 1;
}

.emoji-item {
  font-size: 26px;
  text-align: center;
  padding: 4px 0;
  cursor: pointer;
  border-radius: 8px;
  user-select: none;
  transition: background var(--dur-fast) var(--ease-std), transform var(--dur-fast) var(--ease-std);
}
.emoji-item:hover { background: var(--c-bg); }
.emoji-item:active {
  background: var(--c-primary-container);
  transform: scale(1.2);
}

/* Transition */
.emoji-fade-enter-active { transition: all 0.2s var(--ease-em); }
.emoji-fade-leave-active { transition: all 0.15s var(--ease-in); }
.emoji-fade-enter-from,
.emoji-fade-leave-to { opacity: 0; }
.emoji-fade-enter-from .emoji-panel { transform: translateX(-50%) translateY(12px) scale(0.95); }
.emoji-fade-leave-to .emoji-panel { transform: translateX(-50%) translateY(8px) scale(0.97); }

@media (min-width: 768px) {
  .emoji-panel {
    left: auto;
    right: calc(50% - 360px + 20px);
    transform: none;
  }
  .emoji-fade-enter-from .emoji-panel { transform: translateY(12px) scale(0.95); }
  .emoji-fade-leave-to .emoji-panel { transform: translateY(8px) scale(0.97); }
}
</style>
