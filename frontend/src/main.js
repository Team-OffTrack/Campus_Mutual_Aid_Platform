import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

// Vant styles are auto-imported by unplugin-vue-components,
// but we still need the base CSS reset
import 'vant/lib/index.css'
import './styles/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
