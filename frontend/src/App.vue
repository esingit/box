<template>
  <div id="app" class="app-container">
    <div class="content-wrapper">
      <Sidebar v-if="isLoggedIn" :isLoggedIn="isLoggedIn" />
      <main class="main-content">
        <div class="main-header">
          <div class="user-menu">
            <template v-if="isLoggedIn">
              <button class="user-menu-btn" @click="toggleMenu">
                <User :size="18" class="user-icon" /> {{ user?.username }}
              </button>
              <transition name="fade">
                <div v-if="showMenu" class="dropdown-menu">
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
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useUserStore } from './stores/userStore';
import { useRouter } from 'vue-router';
import Sidebar from './components/Sidebar.vue';
import Profile from './views/Profile.vue';
import { LogIn, UserPlus, User, UserCircle, LogOut } from 'lucide-vue-next';
import { emitter } from './utils/eventBus';

const userStore = useUserStore();
const router = useRouter();
const isLoggedIn = computed(() => userStore.isLoggedIn);
const user = computed(() => userStore.user);
const showMenu = ref(false);
const profileRef = ref(null);

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
  emitter.emit('show-auth', 'login');
}

function showRegister() {
  emitter.emit('show-auth', 'register');
}
</script>

<style>
.app-container {
  display: flex;
  height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  background: #f9f9f9;
  border-radius: 12px;
  overflow: hidden;
}

.content-wrapper {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.main-content {
  flex: 1;
  background: #fff;
  padding: 20px;
  box-sizing: border-box;
  overflow-y: auto;
}

.main-header {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 20px;
  align-items: center;
}

.user-menu {
  position: relative;
  display: flex;
  align-items: center;
}

.user-menu-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-weight: 500;
  color: #666;
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.3s;
  display: flex;
  align-items: center;
}

.user-icon {
  margin-right: 8px;
}

.user-menu-btn:hover {
  color: #000;
  background: #f0f0f0;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
  overflow: hidden;
  z-index: 100;
  min-width: 160px;
  margin-top: 8px;
}

.dropdown-item {
  display: block;
  padding: 10px 15px;
  text-decoration: none;
  color: #333;
  transition: background-color 0.2s;
  border: none;
  background: none;
  width: 100%;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.dropdown-item-icon {
  margin-right: 8px;
}

.dropdown-item:hover {
  background-color: #f0f0f0;
}

.user-auth-link {
  padding: 8px 16px;
  text-decoration: none;
  color: #666;
  transition: color 0.3s;
  display: flex;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
}

.user-auth-link svg {
  margin-right: 5px;
}

.user-auth-link:hover {
  color: #000;
}

.auth-separator {
  color: #ccc;
  margin: 0 5px;
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style> 