import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import axios from './utils/axios';

const app = createApp(App);
app.use(router);
app.config.globalProperties.$axios = axios; // 全局注册 Axios
app.mount('#app');