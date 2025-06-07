<template>
  <div id="app" class="app-container">
    <!-- 侧边栏 -->
    <Sidebar 
      v-if="shouldShowSidebar" 
      :isLoggedIn="isLoggedIn" 
    />
    
    <div class="content-wrapper">
      <main class="main-content">
        <!-- 页面顶部用户菜单 -->
        <div class="main-header">
          <nav class="user-menu">
            <!-- 登录后菜单 -->
            <UserMenuAuthenticated 
              v-if="isLoggedIn" 
              :username="user?.username"
              :showMenu="showMenu"
              @toggle="toggleMenu"
              @open-profile="openProfile"
              @logout="logout"
            />
            
            <!-- 未登录菜单 -->
            <UserMenuGuest 
              v-else
              @login="showLogin"
              @register="showRegister"
            />
          </nav>
        </div>
        
        <!-- 主要内容区 -->
        <router-view />
      </main>
    </div>
    
    <!-- 全局弹窗组件 -->
    <GlobalModals 
      ref="profileRef"
      :show-login="showLoginModal"
      :show-register="showRegisterModal"
      @login-close="handleLoginModalClose"
      @login-success="handleLoginSuccess"
      @register-close="showRegisterModal = false"
      @register-success="handleRegisterSuccess"
    />
    
    <!-- 全局通知组件 -->
    <Notification />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useUserStore } from './stores/userStore';
import { useRouter } from 'vue-router';
import Sidebar from './components/Sidebar.vue';
import Profile from './views/Profile.vue';
import GlobalModals from './components/GlobalModals.vue';
import Notification from './components/Notification.vue';
import ConfirmDialog from './components/ConfirmDialog.vue';
import LoginForm from './components/LoginForm.vue';
import RegisterForm from './components/RegisterForm.vue';
import emitter from './utils/eventBus.js';
import UserMenuAuthenticated from './components/user/UserMenuAuthenticated.vue';
import UserMenuGuest from './components/user/UserMenuGuest.vue';

// ===== 状态管理 =====
const userStore = useUserStore();
const router = useRouter();

// ===== 计算属性 =====
const shouldShowSidebar = computed(() => 
  !['/', '/home'].includes(router.currentRoute.value.path) || isLoggedIn.value
);

// ===== 响应式数据 =====
const isLoggedIn = computed(() => userStore.isLoggedIn);
const user = computed(() => userStore.user);
const showMenu = ref(false);
const profileRef = ref(null);
const showLoginModal = ref(false);
const showRegisterModal = ref(false);

// ===== 常量 =====
const publicPaths = ['/login', '/register', '/home']; // 公共路径列表

// ===== 登录状态检查 =====
/**
 * 检查是否需要显示登录框
 */
const checkAndShowLoginModal = () => {
  // 如果已经显示登录框，不重复检查
  if (showLoginModal.value) return;

  const currentPath = router.currentRoute.value.path;
  // 如果是公共路径，不需要显示登录框
  if (publicPaths.includes(currentPath)) return;
  
  // 只在页面刷新时检查登录状态
  if (!isLoggedIn.value && !router.currentRoute.value.redirectedFrom) {
    showLoginModal.value = true;
  }
};

// 只在路由变化时检查登录状态，不再监听登录状态变化
watch(() => router.currentRoute.value.path, () => {
  nextTick(checkAndShowLoginModal);
});

// ===== 用户菜单相关函数 =====
/**
 * 切换用户菜单显示状态
 */
function toggleMenu() {
  showMenu.value = !showMenu.value;
}

/**
 * 关闭用户菜单
 */
function closeMenu() {
  showMenu.value = false;
}

/**
 * 处理菜单外部点击事件
 */
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

/**
 * 打开用户资料设置
 */
function openProfile() {
  profileRef.value.openModal();
  closeMenu();
}

/**
 * 用户登出
 */
async function logout() {
  await userStore.logout();
  router.push('/');
  closeMenu();
  emitter.emit('notify', '已成功注销', 'success');
}

// ===== 认证相关函数 =====
/**
 * 显示登录模态框
 */
function showLogin() {
  showLoginModal.value = true;
}

/**
 * 显示注册模态框
 */
function showRegister() {
  showRegisterModal.value = true;
}

/**
 * 处理登录模态框关闭
 */
function handleLoginModalClose() {
  showLoginModal.value = false;
}

/**
 * 处理登录成功
 */
function handleLoginSuccess() {
  closeMenu();
  showLoginModal.value = false;
  emitter.emit('notify', '登录成功', 'success');
}

/**
 * 处理注册成功
 */
function handleRegisterSuccess() {
  showRegisterModal.value = false;
  // 可选：注册成功后自动弹出登录弹窗
  // showLoginModal.value = true;
}

// ===== 生命周期钩子 =====
onMounted(() => {
  // 注册点击外部关闭菜单的事件
  document.addEventListener('mousedown', handleClickOutside);
  
  // 注册认证事件监听
  emitter.on('show-auth', (type, message) => {
    if (type === 'login') {
      // 如果登录框已经显示，不再重复显示
      if (!showLoginModal.value) {
        showLoginModal.value = true;
        if (message) {
          emitter.emit('login-error', message);
        }
      }
    } else if (type === 'register' && !showRegisterModal.value) {
      showRegisterModal.value = true;
    }
  });
});

onBeforeUnmount(() => {
  // 移除事件监听
  document.removeEventListener('mousedown', handleClickOutside);
  emitter.off('show-auth');
});
</script>
