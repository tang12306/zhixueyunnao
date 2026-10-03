import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 导入动画库
import 'animate.css'
import { MotionPlugin } from '@vueuse/motion'
import AOS from 'aos'
import 'aos/dist/aos.css'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 导入图标库
import { Icon } from '@iconify/vue'

// 导入加载组件
import { LoadingPlugin } from 'vue-loading-overlay'
import 'vue-loading-overlay/dist/css/index.css'

// 导入所需的Element Plus组件
import { ElLoading } from 'element-plus'

// 导入错误处理工具
import { initErrorHandler, createVueErrorHandler } from './utils/errorHandler'

// 初始化全局错误处理
initErrorHandler()

const app = createApp(App)
const pinia = createPinia()

// 设置Vue错误处理器
app.config.errorHandler = createVueErrorHandler()

// 注册全局组件
app.component('Icon', Icon)
app.component('el-loading', ElLoading)

// 使用插件
app.use(router)
app.use(pinia)
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default'
})
app.use(MotionPlugin)
app.use(LoadingPlugin)

// 配置 NProgress
NProgress.configure({
  easing: 'ease',
  speed: 500,
  showSpinner: false,
  trickleSpeed: 200,
  minimum: 0.3
})

// 初始化 AOS
AOS.init({
  duration: 800,
  easing: 'ease-in-out',
  once: true,
  mirror: false
})

app.mount('#app')