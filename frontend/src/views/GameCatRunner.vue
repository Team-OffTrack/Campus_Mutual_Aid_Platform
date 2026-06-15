<template>
  <div class="game-page">
    <div id="game-container">
      <!-- Start overlay -->
      <div v-if="!gameStarted" id="overlay">
        <h1>CAT_RUNNER v1.0</h1>
        <p style="color:#555;font-size:12px;">SYSTEM_STATUS: READY</p>
        <button id="start-btn" @click="startGame">RUN PROGRAM</button>
        <div class="help-box">
          <p class="help-title">// CONTROLS & RULES</p>
          <p><kbd>A</kbd> <kbd>D</kbd> or ← → — Move left / right</p>
          <p><kbd>W</kbd> <kbd>Space</kbd> or ↑ — Jump</p>
          <p><kbd>Esc</kbd> — Return to previous page</p>
          <p class="help-desc">
            Dodge red obstacles. Your form randomly collapses between
            <span style="color:#00ffaa">HUMAN</span> (tall, slow, high jump)
            and <span style="color:#ffca28">CAT</span> (low, fast, short jump).
            Survive as long as you can!
          </p>
        </div>
      </div>

      <!-- HUD -->
      <div id="ui">
        <div class="stat-item">MODE: <span id="state-text" :style="{ color: stateColor }">{{ playerState }}</span></div>
        <div class="stat-item">SCORE: <span id="score-text">{{ score }}</span></div>
      </div>

      <!-- Emote log -->
      <div id="msg-log"></div>

      <!-- Canvas -->
      <canvas ref="canvasRef" id="gameCanvas" width="800" height="400"></canvas>

      <!-- Mobile controls -->
      <div class="mobile-ctrl">
        <div style="display:flex;gap:10px;">
          <div class="ctrl-btn" @touchstart.prevent="keys.a=true" @touchend.prevent="keys.a=false">A</div>
          <div class="ctrl-btn" @touchstart.prevent="keys.d=true" @touchend.prevent="keys.d=false">D</div>
        </div>
        <div class="ctrl-btn" style="width:100px;border-radius:15px;font-size:12px;"
          @touchstart.prevent="keys.space=true" @touchend.prevent="keys.space=false">JUMP</div>
      </div>

      <!-- Game over tap to restart (mobile) -->
      <div v-if="isGameOver && gameStarted" id="gameover-tap"
        @click="restartGame">
        <p>TAP TO REBOOT</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// ── Emote library ──
const EMOTES = {
  transform: ['(≡^∇^≡)', 'ฅ(⌯͒•̩̩̩́ ˑ̫ •̩̩̩̀⌯͒)ฅ', '(=｀ω´=)', 'ᶘ ᵒ㉨ᵒᶅ'],
  score: ['(๑•̀ㅂ•́)و✧', 'ヾ(=ﾟ･ﾟ=)ﾉ', '(^･ｪ･^)', 'o(^▽^)o'],
  death: ['_(:з」∠)_', '(=ＴェＴ=)', '(╯°Д°)╯', 'SYSTEM_HALT']
}

function pushLog(type, text) {
  const logBox = document.getElementById('msg-log')
  if (!logBox) return
  const emote = EMOTES[type][Math.floor(Math.random() * EMOTES[type].length)]
  const entry = document.createElement('div')
  entry.className = 'log-entry'
  entry.innerHTML = `[${new Date().toLocaleTimeString().split(' ')[0]}] ${emote}<br>${text}`
  logBox.prepend(entry)
  if (logBox.childNodes.length > 3) logBox.removeChild(logBox.lastChild)
}

// ── Game constants ──
const canvasRef = ref(null)
const GRAVITY = 0.7
const GROUND_Y = 350
const FRICTION = 0.85
const P_STATE = { HUMAN: 'HUMAN', CAT: 'CAT' }
const CONFIG = {
  [P_STATE.HUMAN]: { width: 30, height: 60, jump: -15, speed: 0.8, maxVx: 5, color: '#00ffaa' },
  [P_STATE.CAT]: { width: 40, height: 25, jump: -11.5, speed: 1.5, maxVx: 9, color: '#ffca28' }
}

// ── Reactive state ──
const gameStarted = ref(false)
const isGameOver = ref(false)
const score = ref(0)
const playerState = ref('HUMAN')
const stateColor = ref(CONFIG[P_STATE.HUMAN].color)
const keys = { a: false, d: false, space: false }

let player, obstacles, animFrameId, ctx

function initPlayer() {
  return {
    x: 100, y: GROUND_Y - CONFIG[P_STATE.HUMAN].height,
    vx: 0, vy: 0, state: P_STATE.HUMAN, isGrounded: false,
    update() {
      const conf = CONFIG[this.state]
      if (keys.a) this.vx -= conf.speed
      if (keys.d) this.vx += conf.speed
      this.vx *= FRICTION
      this.x += this.vx
      this.x = Math.max(0, Math.min(800 - conf.width, this.x))

      if (keys.space && this.isGrounded) {
        this.vy = conf.jump
        this.isGrounded = false
      }
      this.vy += GRAVITY
      this.y += this.vy

      if (this.y + conf.height >= GROUND_Y) {
        this.y = GROUND_Y - conf.height
        this.vy = 0
        this.isGrounded = true
      }
    },
    draw() {
      const conf = CONFIG[this.state]
      ctx.fillStyle = conf.color
      ctx.fillRect(this.x, this.y, conf.width, conf.height)
      // Draw eye
      ctx.fillStyle = '#000'
      const eyeX = (this.vx >= 0) ? this.x + conf.width - 10 : this.x + 5
      ctx.fillRect(eyeX, this.y + 5, 5, 5)
    }
  }
}

class Obstacle {
  constructor() {
    this.w = 25
    this.h = 30 + Math.random() * 30
    this.x = 800
    this.y = GROUND_Y - this.h
    this.speed = 5 + (score.value / 15)
  }
  update() { this.x -= this.speed }
  draw() { ctx.fillStyle = '#ff4444'; ctx.fillRect(this.x, this.y, this.w, this.h) }
}

function checkTransform() {
  if (Math.random() < 0.004) {
    const oldH = CONFIG[player.state].height
    player.state = (player.state === P_STATE.HUMAN) ? P_STATE.CAT : P_STATE.HUMAN
    player.y += (oldH - CONFIG[player.state].height)
    if (player.y + CONFIG[player.state].height > GROUND_Y) {
      player.y = GROUND_Y - CONFIG[player.state].height
    }
    playerState.value = player.state
    stateColor.value = CONFIG[player.state].color
    pushLog('transform', `Quantum collapse: ${player.state}`)
  }
}

function gameLoop() {
  if (isGameOver.value || !gameStarted.value) return

  ctx.fillStyle = '#0a0a0c'
  ctx.fillRect(0, 0, 800, 400)
  ctx.strokeStyle = '#222'
  ctx.beginPath()
  ctx.moveTo(0, GROUND_Y)
  ctx.lineTo(800, GROUND_Y)
  ctx.stroke()

  checkTransform()
  player.update()
  player.draw()

  // Spawn obstacles
  if (Math.random() < 0.015 && (obstacles.length === 0 || 800 - obstacles[obstacles.length - 1].x > 250)) {
    obstacles.push(new Obstacle())
  }

  // Update & collide obstacles
  for (let i = obstacles.length - 1; i >= 0; i--) {
    obstacles[i].update()
    obstacles[i].draw()
    const pC = CONFIG[player.state]
    if (player.x + 5 < obstacles[i].x + obstacles[i].w &&
        player.x + pC.width - 5 > obstacles[i].x &&
        player.y + 5 < obstacles[i].y + obstacles[i].h &&
        player.y + pC.height - 5 > obstacles[i].y) {
      isGameOver.value = true
      pushLog('death', 'FATAL: Logic overflow')
      drawGameOver()
      return
    }
    if (obstacles[i].x + obstacles[i].w < 0) {
      obstacles.splice(i, 1)
      score.value++
      if (score.value % 10 === 0) pushLog('score', `Optimization: ${score.value}pts`)
    }
  }

  animFrameId = requestAnimationFrame(gameLoop)
}

function drawGameOver() {
  ctx.fillStyle = 'rgba(255,0,0,0.2)'
  ctx.fillRect(0, 0, 800, 400)
  ctx.fillStyle = '#fff'
  ctx.font = '20px Consolas'
  ctx.textAlign = 'center'
  ctx.fillText('CRITICAL_ERROR: SYSTEM_HALTED', 400, 180)
  ctx.font = '14px Consolas'
  ctx.fillText('TAP OR SPACE TO REBOOT', 400, 220)
}

function startGame() {
  gameStarted.value = true
  isGameOver.value = false
  score.value = 0
  playerState.value = 'HUMAN'
  stateColor.value = CONFIG[P_STATE.HUMAN].color
  obstacles = []
  ctx = canvasRef.value.getContext('2d')
  player = initPlayer()
  pushLog('score', 'Core loaded')
  gameLoop()
}

function restartGame() {
  if (!isGameOver.value) return
  if (animFrameId) window.cancelAnimationFrame(animFrameId)
  startGame()
}

// ── Keyboard controls ──
function onKeyDown(e) {
  if (e.key === 'Escape') { router.back(); return }

  const k = e.key.toLowerCase()
  if (k === 'a' || k === 'arrowleft') keys.a = true
  if (k === 'd' || k === 'arrowright') keys.d = true
  if (k === ' ' || k === 'w' || k === 'arrowup') keys.space = true
  if (isGameOver.value && k === ' ') restartGame()
}

function onKeyUp(e) {
  const k = e.key.toLowerCase()
  if (k === 'a' || k === 'arrowleft') keys.a = false
  if (k === 'd' || k === 'arrowright') keys.d = false
  if (k === ' ' || k === 'w' || k === 'arrowup') keys.space = false
}

onMounted(() => {
  window.addEventListener('keydown', onKeyDown)
  window.addEventListener('keyup', onKeyUp)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeyDown)
  window.removeEventListener('keyup', onKeyUp)
  if (animFrameId) window.cancelAnimationFrame(animFrameId)
})
</script>

<style scoped>
.game-page {
  margin: 0; background: #0a0a0c; overflow: hidden;
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  height: 100dvh; color: #00ffaa;
  font-family: 'Consolas', 'Courier New', monospace;
  touch-action: none;
}

#game-container {
  position: relative; width: 100%; max-width: 800px; height: 400px;
  background: #000; border: 2px solid #333; overflow: hidden;
}

canvas { width: 100%; height: 100%; display: block; }

/* ── HUD ── */
#ui { position: absolute; top: 10px; left: 10px; pointer-events: none; z-index: 10; }
.stat-item {
  font-size: 14px; margin-bottom: 5px; background: rgba(0,0,0,0.7);
  padding: 4px 10px; border-left: 3px solid #00ffaa;
}

/* ── Emote log ── */
#msg-log {
  position: absolute; top: 10px; right: 10px; width: 180px; height: 100px;
  overflow: hidden; pointer-events: none; font-size: 11px;
  display: flex; flex-direction: column-reverse;
}
.log-entry {
  background: rgba(0,255,170,0.1); border-right: 2px solid #00ffaa;
  margin-bottom: 4px; padding: 4px; animation: slideIn 0.3s ease;
}
@keyframes slideIn { from { transform: translateX(100%); } to { transform: translateX(0); } }

/* ── Mobile controls ── */
.mobile-ctrl {
  position: absolute; bottom: 20px; display: none; width: 100%;
  justify-content: space-between; padding: 0 30px; box-sizing: border-box;
  pointer-events: auto; z-index: 50;
}
.ctrl-btn {
  width: 60px; height: 60px; background: rgba(255,255,255,0.05);
  border: 2px solid #00ffaa; border-radius: 50%;
  display: flex; justify-content: center; align-items: center;
  color: #00ffaa; font-weight: bold; user-select: none; -webkit-user-select: none;
}
.ctrl-btn:active { background: #00ffaa; color: #000; }
@media (max-width: 850px) {
  #game-container { height: 65vh; }
  .mobile-ctrl { display: flex; }
}

/* ── Overlay ── */
#overlay {
  position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 100;
  background: rgba(10,10,12,0.98);
  display: flex; flex-direction: column; justify-content: center; align-items: center;
  text-align: center;
}

/* ── Game over tap ── */
#gameover-tap {
  position: absolute; bottom: 80px; width: 100%; text-align: center;
  pointer-events: auto; z-index: 60; display: none;
  color: #fff; font-size: 14px; font-family: 'Consolas', monospace;
}
@media (max-width: 850px) {
  #gameover-tap { display: block; }
}

button {
  background: transparent; border: 1px solid #00ffaa; color: #00ffaa;
  padding: 10px 20px; margin: 10px; cursor: pointer; font-family: inherit;
}
button:hover { background: #00ffaa; color: #000; }

/* ── Help / instructions ── */
.help-box {
  margin-top: 16px; padding: 14px 18px;
  border: 1px solid #333; border-radius: 4px;
  text-align: left; font-size: 12px; line-height: 1.7;
  max-width: 400px; color: #888;
}
.help-title { color: #00ffaa; font-weight: bold; margin-bottom: 6px; font-size: 13px; }
.help-box p { margin: 2px 0; }
.help-box kbd {
  display: inline-block; padding: 1px 6px; border: 1px solid #555;
  border-radius: 3px; font-family: inherit; font-size: 11px; color: #00ffaa;
  background: rgba(0,255,170,0.08);
}
.help-desc { margin-top: 8px !important; color: #aaa; }
</style>
