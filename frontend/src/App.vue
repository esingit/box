<template>
  <div id="app" class="app-container">
    <Sidebar v-if="isLoggedIn" :isLoggedIn="isLoggedIn" />
    <div class="content-wrapper">
      <main class="main-content">
        <div class="main-header">
          <div class="user-menu">
            <template v-if="isLoggedIn">
              <button class="user-menu-btn" @click="toggleMenu" @mousedown.stop>
                <User :size="18" class="user-icon" /> {{ user?.username }}
              </button>
              <transition name="fade">
                <div v-if="showMenu" class="dropdown-menu" @mousedown.stop>
                  <button class="dropdown-item" @click="openProfile">
                    <UserCircle :size="16" class="dropdown-item-icon" />设置
                  </button>
                  <button class="dropdown-item" @click="logout">
                    <LogOut :size="16" class="dropdown-item-icon" />注销
                  </button>
                </div>
              </transition>
            </template>
            <template v-else>
              <button class="user-auth-link" @click="showLogin">
                <LogIn :size="18" />
                登录
              </button>
              <span class="auth-separator">|</span>
              <button class="user-auth-link" @click="showRegister">
                <UserPlus :size="18" />
                注册
              </button>
            </template>
          </div>
        </div>
        <router-view />
      </main>
    </div>
    <Profile ref="profileRef" />
    <LoginForm v-if="showLoginModal" @close="showLoginModal = false" @login-success="handleLoginSuccess" />
    <RegisterForm v-if="showRegisterModal" @close="showRegisterModal = false" @register-success="handleRegisterSuccess" />
    <Notification />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useUserStore } from '@/stores/userStore';
import { useRouter } from 'vue-router';
import Sidebar from '@/components/Sidebar.vue';
import Profile from '@/views/Profile.vue';
import Notification from '@/components/Notification.vue';
import ConfirmDialog from '@/components/ConfirmDialog.vue';
import LoginForm from '@/components/LoginForm.vue';
import RegisterForm from '@/components/RegisterForm.vue';
import { LogIn, UserPlus, User, UserCircle, LogOut } from 'lucide-vue-next';
import emitter from '@/utils/eventBus.js';
import { watch } from 'vue';

const userStore = useUserStore();
const router = useRouter();
const isLoggedIn = computed(() => userStore.isLoggedIn);
const user = computed(() => userStore.user);
const showMenu = ref(false);
const profileRef = ref(null);
const showLoginModal = ref(false);
const showRegisterModal = ref(false);

// 移除未登录自动跳转到 /login 的逻辑，弹窗模式下无需强制跳转
// watch(isLoggedIn, (val) => {
//   if (!val) {
//     router.push('/login'); // 如果用户未登录，跳转到登录页
//   }
// });

function toggleMenu() {
  showMenu.value = !showMenu.value;
}

function closeMenu() {
  showMenu.value = false;
}

function openProfile() {
  profileRef.value.openModal();
  closeMenu();
}

function logout() {
  userStore.logout();
  router.push('/');
  closeMenu();
}

function showLogin() {
  showLoginModal.value = true;
}

function showRegister() {
  showRegisterModal.value = true;
}

function handleLoginSuccess() {
  closeMenu(); // 登录成功时自动关闭菜单
  showLoginModal.value = false;
  // 用户已经自动重新登录，不需要额外处理
}

function handleRegisterSuccess() {
  showRegisterModal.value = false;
  // 可选：注册成功后自动弹出登录弹窗
  // showLoginModal.value = true;
}

function handleClickOutside(event) {
  const menu = document.querySelector('.dropdown-menu');
  const btn = document.querySelector('.user-menu-btn');
  if (
    showMenu.value &&
    menu &&
    !menu.contains(event.target) &&
    btn &&
    !btn.contains(event.target)
  ) {
    closeMenu();
  }
}

function handleShowAuth(type) {
  // 弹窗模式下只弹窗，不做路由跳转
  if (type === 'login') {
    showLogin();
  }
  if (type === 'register') {
    showRegister();
  }
}

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
  emitter.on('show-auth', handleShowAuth);
  emitter.on('login-success', handleLoginSuccess);
});
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside);
});
</script>