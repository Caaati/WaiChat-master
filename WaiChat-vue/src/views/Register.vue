// src/views/Register.vue

<template>
  <div class="register-container">
    <div class="register-box">
      <h2>WaiChat 注册</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <input type="text" v-model="username" required>
        </div>
        <div class="form-group">
          <label>密码</label>
          <input type="password" v-model="password" required>
        </div>
        <div class="form-group">
          <label>确认密码</label>
          <input type="password" v-model="confirmPassword" required>
        </div>
        <button type="submit" class="register-btn">注册</button>
        <p class="login-link">已有账号？<a href="/login">去登录</a></p>
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
      password: '',
      confirmPassword: ''
    };
  },
  methods: {
    async handleRegister() {
      if (this.password !== this.confirmPassword) {
        alert('两次输入的密码不一致！');
        return;
      }

      try {
        const res = await axios.post('/api/register', {
          username: this.username,
          password: this.password
        });
        if (res.data.code === 1) {
          alert('注册成功！请登录');
          this.$router.push('/login');
        } else {
          alert('注册失败：' + res.data.msg);
        }
      } catch (err) {
        console.error('注册请求失败：', err);
        alert('注册失败，请稍后重试');
      }
    }
  }
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.register-box {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 0 10px rgba(0,0,0,0.1);
  width: 300px;
}

.form-group {
  margin-bottom: 1rem;
}

label {
  display: block;
  margin-bottom: 0.3rem;
}

input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.register-btn {
  width: 100%;
  padding: 0.7rem;
  margin-top: 1rem;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.register-btn:hover {
  background: #36a47e;
}

.login-link {
  margin-top: 1rem;
  text-align: center;
  font-size: 0.9rem;
}

.login-link a {
  color: #2196f3;
  text-decoration: none;
}
</style>