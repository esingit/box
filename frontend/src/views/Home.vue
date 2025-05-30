<template>
  <div>
    <h1>
      <Box class="rotating-box" />
      BOX
    </h1>
    <transition name="slide-fade">
      <div v-if="showAuth" class="auth-container">
        <div class="auth-header">
          <button class="close-btn" @click="showAuth = false">×</button>
          <div class="auth-tabs">
            <button 
              :class="['tab-btn', { active: activeTab === 'login' }]"
              @click="activeTab = 'login'"
            >
              登录
            </button>
            <button 
              :class="['tab-btn', { active: activeTab === 'register' }]"
              @click="activeTab = 'register'"
            >
              注册
            </button>
          </div>
        </div>
        <div class="auth-form">
          <LoginForm v-if="activeTab === 'login'" />
          <RegisterForm v-else />
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Box } from 'lucide-vue-next'
import { useUserStore } from '../stores/userStore'
import LoginForm from '../components/LoginForm.vue'
import RegisterForm from '../components/RegisterForm.vue'
import { emitter } from '../utils/eventBus'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const activeTab = ref('login')
const showAuth = ref(false)

// 监听事件
onMounted(() => {
  emitter.on('show-auth', (type) => {
    activeTab.value = type
    showAuth.value = true
  })
  
  emitter.on('login-success', () => {
    showAuth.value = false
  })
})

// 清理事件监听
onUnmounted(() => {
  emitter.off('show-auth')
  emitter.off('login-success')
})
</script>

<style scoped>
h1 {
  text-align: center;
  font-size: 2rem;
  color: #333;
  margin: 40px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.rotating-box {
  animation: rotate 3s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.auth-container {
  max-width: 400px;
  margin: 20px auto;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 0 12px #ccc;
}

.auth-header {
  position: relative;
  margin-bottom: 20px;
}

.close-btn {
  position: absolute;
  right: 0;
  top: -10px;
  background: none;
  border: none;
  font-size: 24px;
  color: #666;
  cursor: pointer;
  padding: 0;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.auth-tabs {
  display: flex;
  border-bottom: 1px solid #eee;
}

.tab-btn {
  flex: 1;
  padding: 12px;
  border: none;
  background: none;
  font-size: 1rem;
  font-weight: 600;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn.active {
  color: #3b82f6;
  border-bottom: 2px solid #3b82f6;
}

.tab-btn:hover:not(.active) {
  color: #3b82f6;
  background: #f0f7ff;
}

.auth-form {
  padding: 0 10px;
}

/* 动画效果 */
.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.3s ease-in;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}
</style> 