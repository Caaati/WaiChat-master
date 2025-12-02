<template>
  <div class="app-container">
    <!-- 左侧联系人列表 -->
    <div class="contacts-sidebar">
      <div class="sidebar-header">
        <h3>联系人</h3>
      </div>
      <ul class="contacts-list">
        <li class="add-chat-item" @click="showAddContactModal = true">
          <div class="add-chat-icon">+</div>
          <div class="add-chat-text">新增聊天</div>
        </li>
        <!-- 注意：这里的循环变量是 contact，我们将使用它的 nickname, username, 和 lastMessage 属性 -->
        <li
            v-for="contact in contacts"
            :key="contact.id"
            :class="{ 'active': selectedContactId === contact.id }"
            @click="selectContact(contact)"
        >
          <div class="contact-avatar">
            <!-- 使用昵称的第一个字符作为头像 -->
            <span>{{ contact.nickname.charAt(0) }}</span>
          </div>
          <div class="contact-info"> <!-- 新增一个容器来包裹文本信息 -->
            <div class="contact-name">
              <span class="nickname">{{ contact.nickname }}</span>
              <span class="username">@{{ contact.username }}</span>
            </div>
            <!-- 显示最后一条消息的预览 -->
            <div class="last-message">{{ contact.lastMessage }}</div>
          </div>
        </li>
      </ul>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-container">
      <!-- 聊天头部：显示当前聊天对象 -->
      <div class="chat-header">
        <div class="contact-avatar">
          <span>{{ currentContactName.charAt(0) }}</span>
        </div>
        <h2>{{ currentContactName || '选择一个联系人开始聊天' }}</h2>
        <!-- 当前登录用户信息 -->
        <div class="current-user-item">
          <div class="contact-avatar">
            <!-- 添加空值判断，避免username为null时出错 -->
            <span>{{ nickname ? nickname.charAt(0) : '' }}</span>
          </div>
          <div class="contact-info">
            <div class="contact-name">
              <span class="nickname">{{ nickname || '未登录' }}</span>
            </div>
            <button class="logout-btn" @click="handleLogout">退出登录</button>
          </div>
        </div>
      </div>

      <!-- 聊天消息区域 -->
      <div class="chat-messages">
        <div v-if="messages.length === 0 && selectedContactId" class="empty-chat-hint">
          <p>这里是空的，发送一条消息开始对话吧！</p>
        </div>
        <div v-else-if="!selectedContactId" class="empty-chat-hint">
          <p>请在左侧选择一个联系人。</p>
        </div>
        <div class="message-list">
          <div
              v-for="msg in filteredMessages"
              :key="msg.id"
              :class="['message-item', msg.senderId === userId ? 'self-message' : 'other-message']"
          >
            <div class="message-bubble">
              <div class="message-sender">{{ msg.senderName || (msg.senderId === userId ? '我' : '未知用户') }}</div>
              <div class="message-content">{{ msg.content }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <input
            type="text"
            v-model="message"
            @keyup.enter="sendMessage"
            placeholder="输入消息..."
            class="message-input"
        />
        <button @click="sendMessage" class="send-button">发送</button>
      </div>
    </div>
    <!-- 引入新增联系人的模态框组件 -->
    <AddContactModal
        :is-visible="showAddContactModal"
        :current-user-id="userId"
        @close="showAddContactModal = false"
        @user-selected="handleUserSelected"
        @search-error="showNotification"
    />
    <!-- 动态提示框的容器 -->
    <div id="notification-container" ref="notificationContainer" class="notification-wrapper"></div>
  </div>
</template>

<script>
import axios from "axios";
import AddContactModal from '../components/chat/AddContactModal.vue';

export default {
  components: {
    AddContactModal
  },
  data() {
    return {
      messages: [], // 存储当前聊天窗口的消息
      message: '',
      ws: null,
      userId: null,
      username: null,
      nickname: null,
      selectedContactId: null, // 当前选中的联系人ID
      currentContactName: '', // 当前选中的联系人名称
      contacts: [], // 存储联系人列表
      showAddContactModal: false,
    };
  },
  computed: {
    // 计算属性：只显示与当前选中联系人相关的消息
    filteredMessages() {
      if (!this.selectedContactId || !this.userId) return [];
      return this.messages
          .filter(msg =>
              (msg.senderId == this.userId && msg.targetId == this.selectedContactId) ||
              (msg.senderId == this.selectedContactId && msg.targetId == this.userId)
          )
          .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
    },
  },

  methods: {
    // 退出登录
    handleLogout() {
      if (confirm('确定要退出登录吗？')) {
        // 关闭WebSocket连接
        if (this.ws) {
          this.ws.close();
        }
        // 清除本地存储的用户信息
        localStorage.removeItem('userId');
        localStorage.removeItem('username');
        localStorage.removeItem('nickname');
        // 跳转到登录页
        this.$router.push('/login');
      }
    },
    // 获取联系人列表
    getContactList() {
      // 清空旧数据，防止重复或闪烁
      this.contacts = [];
      axios.get('/api/chat/getContactList', {
        params: {
          userId: this.userId,
        }
      }).then(res => {
        if (res.data.code === 1) {
          const _data = Array.isArray(res.data.data) ? res.data.data : [];

          // 【重要修正】将数据赋值给 contacts，而不是 messages
          this.contacts = _data.map(item => {
            return {
              id: item.id,
              username: item.username,
              nickname: item.nickname,
              lastMessage: item.content || '无消息' // 使用 content 作为最后一条消息
            };
          });
        } else {
          console.error('加载联系人失败:', res.data.msg);
        }
      }).catch(err => {
        console.error('请求联系人列表异常:', err);
      });
    },

    // 当用户在子组件中选择了一个用户时触发
    handleUserSelected(user) {
      console.log('从子组件收到选中的用户:', user);
      // 1. 检查该用户是否已在联系人列表中
      const existingContactIndex = this.contacts.findIndex(c => c.id == user.id);

      if (existingContactIndex !== -1) {
        // 2. 如果已存在，则将其移到列表顶部
        const contact = this.contacts.splice(existingContactIndex, 1)[0];
        this.contacts.unshift(contact);
      } else {
        // 3. 如果不存在，则添加到联系人列表顶部
        this.contacts.unshift({
          id: user.id,
          username: user.username,
          nickname: user.nickname,
          lastMessage: '无消息'
        });
      }
      this.selectContact({
        id: user.id,
        nickname: user.nickname,
        username: user.username
      });
    },

    // 选择联系人
    selectContact(contact) {
      this.selectedContactId = contact.id;
      // 显示昵称
      this.currentContactName = contact.nickname;
      this.messages = []; // 清空旧消息

      axios.get('/api/chat/history', {
        params: {
          userId: this.userId,
          targetId: contact.id
        }
      }).then(res => {
        if (res.data.code === 1) {
          const historyData = Array.isArray(res.data.data) ? res.data.data : [];

          this.messages = historyData.map(msg => {
            const isSelf = msg.userId == this.userId;
            return {
              id: msg.id || Date.now() + Math.random(),
              senderId: isSelf ? this.userId : msg.userId,
              targetId: isSelf ? contact.id : msg.targetId,
              content: msg.content,
              // 聊天窗口中显示昵称
              senderName: isSelf ? '我' : contact.nickname,
              timestamp: msg.timestamp || msg.createTime || new Date()
            };
          });

          this.scrollToBottom();
        } else {
          console.error('加载聊天记录失败:', res.data.msg);
        }
      }).catch(err => {
        console.error('请求聊天记录异常:', err);
      });
    },

    async sendMessage() {
      if (!this.message.trim() || !this.selectedContactId) return;

      const newMessage = {
        id: Date.now(),
        senderId: this.userId,
        senderName: '我',
        targetId: this.selectedContactId,
        targetName: this.currentContactName,
        content: this.message,
        status: 'sending',
      };
      this.messages.push(newMessage);
      this.scrollToBottom();

      const messageContent = this.message;
      this.message = '';

      try {
        const response = await fetch('/api/chat/send', {
          method: 'POST',
          headers: {'Content-Type': 'application/json'},
          body: JSON.stringify({
            userId: this.userId,
            targetId: this.selectedContactId,
            content: messageContent,
          }),
        });

        const data = await response.json();

        if (data.code === 1) {
          newMessage.status = 'sent';

          // --- 新增代码开始 ---
          // 1. 找到当前联系人在 contacts 数组中的索引
          const contactIndex = this.contacts.findIndex(c => c.id == this.selectedContactId);

          // 2. 如果找到了（通常情况下都能找到）
          if (contactIndex !== -1) {
            // 3. 更新最后一条消息
            this.contacts[contactIndex].lastMessage = messageContent;

            // 4. 将该联系人移动到列表顶部，提升用户体验
            const updatedContact = this.contacts.splice(contactIndex, 1)[0];
            this.contacts.unshift(updatedContact);
          }
          // --- 新增代码结束 ---

        } else {
          newMessage.status = 'offline';
          this.showNotification(data.msg || `对方"${this.currentContactName}"当前不在线，消息将在对方上线后送达`, 'error');
        }
      } catch (error) {
        console.error('发送消息时出错:', error);
        newMessage.status = 'error';
        this.showNotification('消息发送失败，请检查网络连接。', 'error');
      }
    },

    // 显示通知
    showNotification(message, type = 'info') {
      const container = this.$refs.notificationContainer;
      if (!container) {
        console.error('通知容器未找到');
        return;
      }

      const notification = document.createElement('div');
      notification.className = `notification notification-${type}`;
      notification.innerHTML = `
    <div class="notification-icon">
      ${type === 'error' ? '⚠️' : 'ℹ️'}
    </div>
    <div class="notification-content">${message}</div>
  `;

      container.appendChild(notification);
      // 触发显示动画
      setTimeout(() => notification.classList.add('show'), 10);
      // 3秒后自动消失
      setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
      }, 3000);
    },

    scrollToBottom() {
      this.$nextTick(() => {
        const messagesEl = this.$el.querySelector('.chat-messages');
        if (messagesEl) {
          messagesEl.scrollTop = messagesEl.scrollHeight;
        }
      });
    }
  },
  mounted() {
    this.userId = localStorage.getItem('userId');
    this.username = localStorage.getItem('username');
    this.nickname = localStorage.getItem('nickname');
    if (!this.username) {
      this.$router.push('/login');
      return;
    }

    // 页面加载时获取联系人列表
    this.getContactList();

    if (this.userId) {
      this.ws = new WebSocket(`ws://localhost:8080/ws/${this.userId}`);

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          const senderId = data.userId || data.senderId;
          const message = {
            id: data.id || Date.now() + Math.random(),
            senderId: senderId, // 使用临时变量
            targetId: data.targetId,
            content: data.content,
            senderName: this.contacts.find(c => c.id === senderId)?.nickname || '未知用户',
            timestamp: data.createTime || new Date()
          };
          const contactIndex = this.contacts.findIndex(c => c.id == senderId);
          if (contactIndex !== -1) {
            // 1. 更新最后一条消息内容
            this.contacts[contactIndex].lastMessage = message.content;
            // 2. 将该联系人移到列表顶部（最新消息优先展示）
            const updatedContact = this.contacts.splice(contactIndex, 1)[0];
            this.contacts.unshift(updatedContact);
          }
          // 如果是当前选中的联系人消息，添加到聊天窗口
          if (this.selectedContactId == senderId) {
            this.messages.push(message);
            this.scrollToBottom();
          } else {
            // 显示新消息通知
            this.showNotification(`收到来自 "${message.senderName}" 的新消息`);
          }
        } catch (e) {
          console.warn('无法解析 WebSocket 消息:', e);
          this.showNotification('收到一条系统通知。', 'info');
        }
      };

      this.ws.onclose = (event) => {
        console.log('WebSocket 连接已关闭:', event);
        this.showNotification('与服务器连接已断开。', 'error');
        this.$router.push('/login');
      };

      this.ws.onerror = (error) => {
        console.error('WebSocket 发生错误:', error);
        this.showNotification('WebSocket 发生错误。', 'error');
      };
    }
  },
  beforeUnmount() {
    if (this.ws) {
      this.ws.close();
    }
  },
};
</script>

<style scoped>
/* --- 整体布局 --- */
.app-container {
  display: flex;
  height: 97vh; /* 设置为视口高度 */
  background-color: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 新增聊天按钮样式 (从子组件移过来) */
.add-chat-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
  color: #42b983;
  font-weight: 500;
}

.add-chat-item:hover {
  background-color: #f0fdf4;
}

.add-chat-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #ecfdf5;
  color: #42b983;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}

.add-chat-text {
  font-size: 15px;
}

/* --- 左侧联系人侧边栏 --- */
.contacts-sidebar {
  width: 260px;
  background-color: #ffffff;
  border-right: 1px solid #e9e9eb;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e9e9eb;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.contacts-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
}

.contacts-list li {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-bottom: 1px solid #f0f0f0; /* 添加分隔线 */
}

.contacts-list li:hover {
  background-color: #f5f5f5;
}

.contacts-list li.active {
  background-color: #e8f0fe;
  border-left: 3px solid #42b983;
}

.contact-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #42b983;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 12px;
  flex-shrink: 0;
}

/* --- 新增的联系人信息样式 --- */
.contact-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1; /* 让文本区域占据剩余空间 */
  min-width: 0; /* 防止内容过长时 flex 布局异常 */
}

.contact-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.nickname {
  font-size: 15px;
  font-weight: 600; /* 昵称加粗 */
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.username {
  font-size: 12px;
  color: #999; /* 用户名灰色 */
  white-space: nowrap;
}

.last-message {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* --- 右侧聊天区域 (样式未变) --- */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f7f8fa;
}

/* 当前用户信息样式 */
.current-user-item {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  cursor: default;
  margin-left: auto; /* 右对齐 */
}

/* 退出按钮样式 */
.logout-btn {
  background: none;
  border: none;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s ease;
}

.logout-btn:hover {
  background-color: #f5f5f5;
  color: #ff4d4f; /*  hover时变红 */
}

.chat-header {
  padding: 14px 16px;
  border-bottom: 1px solid #e9e9eb;
  background-color: #ffffff;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.chat-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f7f8fa;
  background-size: cover;
  background-position: center;
}

.empty-chat-hint {
  text-align: center;
  color: #999;
  padding-top: 30%;
  font-size: 14px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  max-width: 80%;
}

.self-message {
  align-self: flex-end;
}

.other-message {
  align-self: flex-start;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  position: relative;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.self-message .message-bubble {
  background-color: #42b983;
  color: white;
  border-top-right-radius: 4px;
}

.other-message .message-bubble {
  background-color: #ffffff;
  color: #333;
  border: 1px solid #e0e0e0;
  border-top-left-radius: 4px;
}

.message-sender {
  font-size: 12px;
  margin-bottom: 4px;
  opacity: 0.8;
  font-weight: 500;
}

.message-content {
  font-size: 15px;
  line-height: 1.4;
  word-break: break-word;
}

.chat-input-area {
  display: flex;
  padding: 12px 16px;
  background-color: #ffffff;
  border-top: 1px solid #e9e9eb;
  gap: 10px;
  align-items: center;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 24px;
  font-size: 14px;
  transition: border-color 0.2s ease;
  outline: none;
}

.message-input:focus {
  border-color: #42b983;
}

.send-button {
  padding: 12px 24px;
  background-color: #42b983;
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.send-button:hover {
  background-color: #36a47e;
}

.send-button:disabled {
  background-color: #a0e5c1;
  cursor: not-allowed;
}

/* 通知容器 - 固定定位在主窗口右上角 */
.notification-wrapper {
  position: fixed; /* 浮在所有内容上方 */
  top: 20px;
  right: 20px;
  z-index: 9999; /* 确保在最上层 */
  display: flex;
  flex-direction: column;
  gap: 12px; /* 通知之间的间距 */
}

/* --- 通知样式 --- */
:deep(.notification) {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateX(120%);
  opacity: 0;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  max-width: 300px;
}

:deep(.notification.show) {
  transform: translateX(0);
  opacity: 1;
}

:deep(.notification-icon) {
  font-size: 20px;
  margin-right: 12px;
  flex-shrink: 0;
}

:deep(.notification-content) {
  font-size: 14px;
  line-height: 1.4;
  color: #333;
}

:deep(.notification-error) {
  border-left: 4px solid #ff4d4f;
  background-color: #fff2f0;
}

:deep(.notification-error .notification-content) {
  color: #ff4d4f;
}

:deep(.notification-info) {
  border-left: 4px solid #1890ff;
  background-color: #e6f7ff;
}

:deep(.notification-info .notification-content) {
  color: #1890ff;
}
</style>
