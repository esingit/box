<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">设置</h2>
        <button class="close-button" @click="closeModal">
          <LucideX class="close-icon" />
        </button>
      </div>
      <div class="modal-body">
        <div class="settings-content">
          <div class="settings-menu">
            <nav class="menu-list">
              <button 
                @click="activeTab = 'profile'"
                :class="['menu-item', { active: activeTab === 'profile' }]"
              >
                <div class="menu-item-content">
                  <LucideUser class="menu-icon" :size="20" />
                  个人信息
                </div>
              </button>
              <button 
                @click="activeTab = 'security'"
                :class="['menu-item', { active: activeTab === 'security' }]"
              >
                <div class="menu-item-content">
                  <LucideLock class="menu-icon" :size="20" />
                  安全设置
                </div>
              </button>
            </nav>
          </div>
          
          <div class="content-area">
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

            <div v-if="activeTab === 'security'" class="security-panel">
              <form @submit.prevent="handleReset">
                <div class="form-group">
                  <label class="form-label">旧密码</label>
                  <input type="password" v-model="oldPassword" class="input" required />
                </div>
                <div class="form-group">
                  <label class="form-label">新密码</label>
                  <input type="password" v-model="newPassword" class="input" required />
                </div>
                <div class="form-group">
                  <label class="form-label">确认密码</label>
                  <input type="password" v-model="confirmPassword" class="input" required />
                </div>
                <div v-if="resetMsg" class="message-container">
                  <p :class="['message', resetSuccess ? 'success-text' : 'error-text']">
                    {{ resetMsg }}
                  </p>
                </div>
                <div class="form-group">
                  <div class="form-btn-container">
                    <button class="btn btn-primary" type="submit">确认修改</button>
                  </div>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { LucideX, LucideUser, LucideLock } from 'lucide-vue-next'
import { useUserStore } from '@/stores/userStore'
import axios from '@/utils/axios'

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
    const response = await axios.post('/api/user/reset-password', {
      username: user.value.username,
      oldPassword: oldPassword.value,
      newPassword: newPassword.value
    })
    
    if (response.data.success) {
      resetMsg.value = '密码重置成功！'
      resetSuccess.value = true
      oldPassword.value = ''
      newPassword.value = ''
      confirmPassword.value = ''
    } else {
      resetMsg.value = response.data.message || '重置失败'
    }
  } catch (error) {
    console.error('密码重置失败:', error)
    resetMsg.value = error.response?.data?.message || '请求失败，请稍后再试'
  }
}

defineExpose({
  openModal
})
</script>

