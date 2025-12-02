<template>
  <div class="login-container">
    <div class="login-box">
      <h2>WaiChat登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="username" required>
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="password" required>
        </div>
        <button type="submit" class="login-btn">登录</button>
        <button type="button" class="register-btn" @click="toRegister">注册</button>
      </form>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { useRouter } from 'vue-router';

export default {
  data() {
    return {
      username: '',
      password: ''
    };
  },
  methods: {
    async handleLogin() {
      try {
        const formData = new FormData();
        formData.append('username', this.username);
        formData.append('password', this.password);

        const res = await axios.post('/api/login', formData);

        if (res.data.code === 1) {
          // 假设后端返回的数据结构是 { code: 1, msg: "success", data: { id: 1, username: "admin" } }
          // 请根据你实际的返回结构进行调整
          if (res.data.data && res.data.data.id) {
            localStorage.setItem('userId', res.data.data.id);
            localStorage.setItem('username', res.data.data.username);
            localStorage.setItem('nickname', res.data.data.nickname);
          }
          this.$router.push('/chat');
        } else {
          alert(res.data.msg || '登录失败');
        }
      } catch (err) {
        console.error(err);
        alert('网络错误或服务器异常');
      }
    },
    toRegister() {
      this.$router.push('/register');
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.login-box {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0,0,0,0.1);
}

.form-group {
  margin-bottom: 1rem;
}

input {
  width: 100%;
  padding: 0.5rem;
  margin-top: 0.3rem;
}

.login-btn, .register-btn {
  width: 100%;
  padding: 0.5rem;
  margin-top: 1rem;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.register-btn {
  background: #2196f3;
}
</style>