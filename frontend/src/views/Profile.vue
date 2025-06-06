<template>
  <div v-if="isOpen" class="modal-overlay" @click.self="closeModal">
    <div class="modal-container user-settings-modal">
      <div class="modal-content">
        <div class="modal-header">
          <h2 class="modal-title">设置</h2>
          <button class="close-button" @click="closeModal">
            <LucideX class="close-icon" />
          </button>
        </div>
        <div class="modal-body">
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
                  <input type="password" v-model="oldPassword" class="form-input" required />
                </div>
                <div class="form-group">
                  <label class="form-label">新密码</label>
                  <input type="password" v-model="newPassword" class="form-input" required />
                </div>
                <div class="form-group">
                  <label class="form-label">确认密码</label>
                  <input type="password" v-model="confirmPassword" class="form-input" required />
                </div>
                <div class="form-submit">
                  <p v-if="resetMsg" :class="['message', resetSuccess ? 'success' : 'error']">
                    {{ resetMsg }}
                  </p>
                  <button class="btn btn-black" type="submit">确认修改</button>
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
import { LucideX } from 'lucide-vue-next'
import { useUserStore } from '@/stores/userStore'

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

