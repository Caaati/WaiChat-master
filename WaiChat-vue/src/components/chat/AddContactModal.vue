<template>
  <div v-if="isVisible" class="modal-overlay">
    <div class="modal">
      <div class="modal-header">
        <h3>查找用户</h3>
        <button class="close-btn" @click="closeModal">&times;</button>
      </div>
      <div class="modal-body">
        <input
            type="text"
            v-model="searchQuery"
            @keyup.enter="fetchSearchResults(searchQuery)"
            placeholder="输入用户名、昵称或ID，按Enter搜索"
            class="search-input"
            ref="searchInput"
        />
        <div class="search-results">
          <div v-if="searching">搜索中...</div>
          <div v-else-if="searchResults.length === 0">
            未找到匹配的用户
          </div>
          <div
              v-for="user in searchResults"
              :key="user.id"
              class="search-result-item"
              @click="selectUser(user)"
          >
            <div class="contact-avatar">
              <span>{{ user.nickname.charAt(0) }}</span>
            </div>
            <div class="contact-info">
              <div class="contact-name">
                <span class="nickname">{{ user.nickname }}</span>
                <span class="username">@{{ user.username }}</span>
              </div>
              <div class="user-id">ID: {{ user.id }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "AddContactModal",
  props: {
    isVisible: {
      type: Boolean,
      required: true,
      default: false
    },
    currentUserId: {
      type: [Number, String],
      required: true
    }
  },
  data() {
    return {
      searchQuery: '',
      searchResults: [],
      searching: false
    };
  },
  watch: {
    isVisible(newVal) {
      if (newVal) {
        this.$nextTick(() => {
          this.$refs.searchInput?.focus();
        });
      } else {
        this.searchQuery = '';
        this.searchResults = [];
      }
    }
  },
  methods: {
    async fetchSearchResults(query) {
      if (!query.trim()) {
        this.searchResults = [];
        return;
      }

      this.searching = true;
      try {
        const response = await axios.get('/api/user/search', {
          params: { key: query }
        });

        if (response.data.code === 1) {
          this.searchResults = response.data.data.filter(
              user => user.id !== this.currentUserId
          );
        } else {
          this.searchResults = [];
          // this.$emit('search-error', response.data.msg || '搜索失败');
        }
      } catch (error) {
        console.error('搜索用户时出错:', error);
        this.searchResults = [];
        // this.$emit('search-error', '网络错误，搜索失败');
      } finally {
        this.searching = false;
      }
    },

    selectUser(user) {
      this.$emit('user-selected', user);
      this.closeModal();
    },

    closeModal() {
      this.$emit('close');
    }
  }
};
</script>

<style scoped>
/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000; /* 确保在最上层 */
}

.modal {
  width: 90%;
  max-width: 400px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.modal-header {
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0 8px;
}

.close-btn:hover {
  color: #333;
}

.modal-body {
  padding: 16px;
}

/* 搜索框样式 */
.search-input {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 24px;
  font-size: 14px;
  margin-bottom: 16px;
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #42b983;
}

/* 搜索结果样式 */
.search-results {
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
  border-radius: 4px;
  margin-bottom: 8px;
}

.search-result-item:hover {
  background-color: #f5f5f5;
}

/* 联系人信息样式 (与主组件保持一致) */
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

.contact-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  min-width: 0;
}

.contact-name {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 4px;
}

.nickname {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.username, .user-id {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
</style>