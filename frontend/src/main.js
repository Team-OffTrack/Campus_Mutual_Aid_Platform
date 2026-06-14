import { createApp, watch } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Vant styles are auto-imported by unplugin-vue-components,
// but we still need the base CSS reset
import 'vant/lib/index.css'
import './styles/main.css'

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.mount('#app')

// Load favorites when user logs in, clear on logout
import { useAuthStore } from '@/stores/auth'
import { useFavoritesStore } from '@/stores/favorites'

const authStore = useAuthStore()
const favoritesStore = useFavoritesStore()

watch(() => authStore.isLoggedIn, (loggedIn) => {
  if (loggedIn) {
    favoritesStore.loadFavorites()
  } else {
    favoritesStore.clearFavorites()
  }
}, { immediate: true })
