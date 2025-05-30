<template>
  <!-- 弹窗遮罩层 -->
  <div v-if="isOpen" class="modal-overlay" @click="closeModal"></div>

  <!-- 弹窗内容 -->
  <div v-if="isOpen" class="modal-container">
    <div class="modal-content">
      <!-- 弹窗头部 -->
      <div class="modal-header">
        <h2 class="modal-title">设置</h2>
        <button @click="closeModal" class="close-button">
          <svg class="close-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- 弹窗主体内容 -->
      <div class="modal-body">
        <!-- 左侧菜单 -->
        <div class="sidebar-menu">
          <nav class="menu-list">
            <button 
              @click="activeTab = 'profile'"
              :class="['menu-item', { active: activeTab === 'profile' }]"
            >
              <div class="menu-item-content">
                <svg class="menu-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                个人信息
              </div>
            </button>
            <button 
              @click="activeTab = 'security'"
              :class="['menu-item', { active: activeTab === 'security' }]"
            >
              <div class="menu-item-content">
                <svg class="menu-icon" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                安全设置
              </div>
            </button>
          </nav>
        </div>

        <!-- 右侧内容区 -->
        <div class="content-area">
          <!-- 个人信息面板 -->
          <div v-if="activeTab === 'profile'" class="profile-panel">
            <div class="info-group">
              <label class="info-label">用户名</label>
              <p class="info-value">{{ user?.username }}</p>
            </div>
            <div class="info-group">
              <label class="info-label">邮箱</label>
              <p class="info-value">{{ user?.email || 'N/A' }}</p>
            </div>
            <div class="info-group">
              <label class="info-label">上次登录时间</label>
              <p class="info-value">{{ formatDateTime(user?.lastLoginTime) }}</p>
            </div>
          </div>

          <!-- 安全设置面板 -->
          <div v-if="activeTab === 'security'">
            <form @submit.prevent="handleReset" class="reset-form">
              <div class="form-group">
                <label class="form-label">旧密码</label>
                <input 
                  type="password" 
                  v-model="oldPassword"
                  class="form-input"
                  required
                />
              </div>
              <div class="form-group">
                <label class="form-label">新密码</label>
                <input 
                  type="password" 
                  v-model="newPassword"
                  class="form-input"
                  required
                />
              </div>
              <div class="form-group">
                <label class="form-label">确认新密码</label>
                <input 
                  type="password" 
                  v-model="confirmPassword"
                  class="form-input"
                  required
                />
              </div>
              <div class="form-submit">
                <button type="submit" class="submit-button">
                  确认修改
                </button>
              </div>
              <p v-if="resetMsg" :class="['message', resetSuccess ? 'success' : 'error']">
                {{ resetMsg }}
              </p>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()
const user = computed(() => userStore.user)

const isOpen = ref(false)
const activeTab = ref('profile')
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const resetMsg = ref('')
const resetSuccess = ref(false)

const closeModal = () => {
  isOpen.value = false
  resetMsg.value = ''
  oldPassword.value = ''
  newPassword.value = ''
  confirmPassword.value = ''
}

const openModal = () => {
  isOpen.value = true
  activeTab.value = 'profile'
}

function formatDateTime(val) {
  if (!val) return 'N/A'
  const date = new Date(val)
  if (isNaN(date.getTime())) return val
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  const h = String(date.getHours()).padStart(2, '0')
  const min = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}:${s}`
}

async function handleReset() {
  resetMsg.value = ''
  resetSuccess.value = false
  
  if (newPassword.value !== confirmPassword.value) {
    resetMsg.value = '两次新密码输入不一致！'
    return
  }

  try {
    const res = await fetch('/api/user/reset-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: user.value.username,
        oldPassword: oldPassword.value,
        newPassword: newPassword.value
      })
    })
    const data = await res.json()
    if (data.success) {
      resetMsg.value = '密码重置成功！'
      resetSuccess.value = true
      oldPassword.value = ''
      newPassword.value = ''
      confirmPassword.value = ''
      setTimeout(() => {
        closeModal()
      }, 1500)
    } else {
      resetMsg.value = data.message || '重置失败'
    }
  } catch (e) {
    resetMsg.value = '请求失败，请稍后再试'
  }
}

defineExpose({
  openModal
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.3);
  z-index: 40;
}

.modal-container {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-content {
  background-color: white;
  border-radius: 0.5rem;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  width: 600px;
  max-height: 85vh;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 1.25rem 0.5rem;
  border-bottom: 1px solid rgba(229, 231, 235, 0.5);
  margin-bottom: 1rem;
}

.modal-title {
  font-weight: 600;
  color: #111827;
}

.close-button {
  color: #9ca3af;
  transition: color 0.2s;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.25rem;
}

.close-button:hover {
  color: #6b7280;
}

.close-icon {
  width: 1.5rem;
  height: 1.5rem;
}

.modal-body {
  display: flex;
  height: 450px;
}

.sidebar-menu {
  width: 160px;
}

.menu-item {
  margin: 0 0.5rem;
}
.menu-item.active {
  background-color: #f3f4f6;
  color: inherit;
  font-weight: normal;
  border-radius: 0.375rem;
  margin: 0 0.5rem;
  box-shadow: none;
  border: none;
}

.menu-list {
  display: flex;
  flex-direction: column;
  padding: 0.5rem 0;
  gap: 0.5rem;
}

.menu-item {
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  text-align: left;
  color: #4b5563;
  background: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.menu-item:hover {
  background-color: transparent;
}

.menu-item.active {
  background-color: #f3f4f6;
  color: inherit;
  font-weight: normal;
  border-radius: 0.375rem;
  margin: 0 0.5rem;
  box-shadow: none;
  border: none;
}

.menu-item-content {
  display: flex;
  align-items: center;
}

.menu-icon {
  width: 1rem;
  height: 1rem;
  margin-right: 0.5rem;
}

.content-area {
  flex: 1;
  padding: 1.25rem;
}

.profile-panel,
.security-panel {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.info-group {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  margin: 0;
}

.profile-panel .info-group:not(:last-child) {
  border-bottom: 1px solid rgba(229, 231, 235, 0.5);
  padding-bottom: 0;
  margin-bottom: 0;
}

.info-label {
  font-size: 0.813rem;
  font-weight: 500;
  color: #374151;
  letter-spacing: 0.03em;
  margin-bottom: 0.7rem;
}

.info-value {
  font-size: 0.875rem;
  font-weight: 400;
  color: #6b7280;
  padding: 0;
  margin: 0;
}

.modal-header {
  border-bottom: 1px solid rgba(229, 231, 235, 0.5);
}

.menu-item.active {
  border: none;
}

.profile-panel .info-group:not(:last-child),
.security-panel .form-group:not(:last-child) {
  border-bottom: 1px solid rgba(229, 231, 235, 0.5);
  padding-bottom: 0.75rem;
  margin-bottom: 0.75rem;
}

.reset-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.form-label {
  font-size: 0.813rem;
  font-weight: 500;
  color: #374151;
}

.form-input {
  width: 80%;
  padding: 0.375rem 0.625rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.submit-button {
  width: auto;
  margin: 0 auto;
  display: inline-flex;
  padding: 0.375rem 0.75rem;
  background-color: #2563eb;
  color: white;
  border: none;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2);
}

.form-submit {
  padding-top: 0.375rem;
}

.submit-button:hover {
  background-color: #1d4ed8;
}

.submit-button:focus {
  outline: none;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.5);
}

.message {
  margin-top: 0.5rem;
  font-size: 0.875rem;
}

.message.success {
  color: #059669;
}

.message.error {
  color: #dc2626;
}
</style>