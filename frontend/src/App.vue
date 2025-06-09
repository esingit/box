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
          <nav class="user-menu-container">
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
      :show-login="isShowingLoginModal"
      :show-register="isShowingRegisterModal"
      @login-close="hideLogin"
      @login-success="handleLoginSuccess"
      @register-close="hideRegister"
      @register-success="handleRegisterSuccess"
    />
    
    <!-- 全局通知组件 -->
    <Notification />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useUserStore } from './stores/userStore';
import { useRouter } from 'vue-router';
import { useAuth, initializeAuth } from './composables/useAuth';
import { useLogout } from '@/composables/useLogout';
import Sidebar from './components/Sidebar.vue';
import Profile from './views/Profile.vue';
import GlobalModals from './components/GlobalModals.vue';
import Notification from './components/Notification.vue';
import ConfirmDialog from './components/ConfirmDialog.vue';
import UserMenuAuthenticated from './components/user/UserMenuAuthenticated.vue';
import UserMenuGuest from './components/user/UserMenuGuest.vue';
import emitter from './utils/eventBus';

// ===== 状态管理 =====
const userStore = useUserStore();
const router = useRouter();
const {
  isShowingLoginModal,
  isShowingRegisterModal,
  showLogin,
  hideLogin,
  showRegister,
  hideRegister
} = useAuth();

// ===== 计算属性 =====
const shouldShowSidebar = computed(() => 
  !['/', '/home'].includes(router.currentRoute.value.path) || isLoggedIn.value
);

// ===== 响应式数据 =====
const isLoggedIn = computed(() => userStore.isLoggedIn);
const user = computed(() => userStore.user);
const showMenu = ref(false);
const profileRef = ref(null);

// ===== 用户菜单相关函数 =====
function toggleMenu() {
  showMenu.value = !showMenu.value;
}

function closeMenu() {
  showMenu.value = false;
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

function openProfile() {
  if (profileRef.value?.openModal) {
    profileRef.value.openModal();
    closeMenu();
  }
}

async function logout() {
  closeMenu(); // 先关闭菜单，提升用户体验
  const { handleLogout } = useLogout();
  await handleLogout(true);
}

// ===== 认证相关函数 =====
function handleLoginSuccess() {
  closeMenu();
  hideLogin();
  emitter.emit('login-success');
  emitter.emit('notify', '登录成功', 'success');
}

function handleRegisterSuccess() {
  hideRegister();
  // 可选：注册成功后自动弹出登录弹窗
  // showLogin();
}

// ===== 生命周期钩子 =====
onMounted(() => {
  // 注册点击外部关闭菜单的事件
  document.addEventListener('mousedown', handleClickOutside);
  
  // 注册认证事件监听
  emitter.on('show-auth', (type, message) => {
    if (type === 'login') {
      showLogin(message);
    } else if (type === 'register') {
      showRegister();
    }
  });
  
  // 监听登录模态框显示事件
  emitter.on('show-login-modal', (message) => {
    showLogin(message);
  });

  // 初始化认证状态
  const initAuth = async () => {
    try {
      const success = await initializeAuth();
      if (!success && userStore.isLoggedIn) {
        // 如果初始化失败但状态显示已登录，清理登录状态
        userStore.logout(false);
      }
    } catch (error) {
      console.error('认证初始化失败:', error);
      userStore.logout(false);
    }
  };
  
  initAuth();
});

onBeforeUnmount(() => {
  // 移除事件监听
  document.removeEventListener('mousedown', handleClickOutside);
  emitter.off('show-auth');
  emitter.off('show-login-modal');
});
</script>
