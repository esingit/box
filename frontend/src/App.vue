<template>
  <div id="app" class="app-container">
    <Sidebar v-if="!['/', '/home'].includes($route.path) || isLoggedIn" :isLoggedIn="isLoggedIn" />
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
    <LoginForm v-if="showLoginModal" @close="handleLoginModalClose" @login-success="handleLoginSuccess" />
    <RegisterForm v-if="showRegisterModal" @close="showRegisterModal = false" @register-success="handleRegisterSuccess" />
    <Notification />
    <ConfirmDialog />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
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

const userStore = useUserStore();
const router = useRouter();
const isLoggedIn = computed(() => userStore.isLoggedIn);
const user = computed(() => userStore.user);
const showMenu = ref(false);
const profileRef = ref(null);
const showLoginModal = ref(false);
const showRegisterModal = ref(false);
const publicPaths = ['/login', '/register', '/home']; // 公共路径列表

// 检查是否需要显示登录框的函数
const checkAndShowLoginModal = () => {
  if (!isLoggedIn.value && !publicPaths.includes(router.currentRoute.value.path)) {
    showLoginModal.value = true;
  }
};

// 检查登录状态的函数
const checkLoginStatus = () => {
  if (!isLoggedIn.value && !publicPaths.includes(router.currentRoute.value.path)) {
    showLoginModal.value = true;
  }
};

// 监听登录状态变化
watch(isLoggedIn, (val) => {
  if (!val) {
    checkAndShowLoginModal();
  }
});

// 监听路由变化
watch(
  () => router.currentRoute.value.path,
  () => {
    checkAndShowLoginModal();
  }
);

// 检查需要登录的页面操作
const checkProtectedAction = (event) => {
  // 已登录或在公开页面时不需要处理
  if (isLoggedIn.value || publicPaths.includes(router.currentRoute.value.path)) {
    return false;
  }

  // 获取触发事件的元素及其所有父元素
  const path = event.composedPath();
  
  // 排除需要忽略的元素
  const excludeSelectors = [
    '.auth-form-modal',
    '.auth-form-modal-overlay',
    '.user-auth-link',
    '.auth-separator',
    '[data-modal-close]',
    '.modal-close-btn',
    '.modal-header',
    '.btn-close'
  ];
  
  // 检查事件路径上的所有元素是否包含需要排除的选择器
  const isExcluded = path.some(element => {
    if (!(element instanceof Element)) return false;
    return excludeSelectors.some(selector => element.matches(selector));
  });

  if (!isExcluded) {
    showLoginModal.value = true;
    return true;
  }

  return false;
};

// 用户操作处理函数
const handleUserAction = (event) => {
  checkProtectedAction(event);
};

onMounted(() => {
  document.addEventListener('click', handleUserAction);

  emitter.on('show-auth', (type, message) => {
    if (type === 'login') {
      showLoginModal.value = true;
      if (message) {
        emitter.emit('login-error', message);
      }
    } else if (type === 'register') {
      showRegisterModal.value = true;
    }
  });
});

onBeforeUnmount(() => {
  document.removeEventListener('click', handleUserAction);
  emitter.off('show-auth');
});

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

async function logout() {
  await userStore.logout();
  router.push('/');
  closeMenu();
  emitter.emit('notify', '已成功注销', 'success');
}

function showLogin() {
  showLoginModal.value = true;
}

function showRegister() {
  showRegisterModal.value = true;
}

function handleLoginModalClose() {
  showLoginModal.value = false;
}

function handleLoginSuccess() {
  closeMenu(); // 登录成功时自动关闭菜单
  showLoginModal.value = false;
  // 触发登录成功事件，以便其他组件可以更新状态
  emitter.emit('login-success');
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

onMounted(() => {
  document.addEventListener('mousedown', handleClickOutside);
  emitter.on('show-auth', (type, message) => {
    if (type === 'login') {
      showLoginModal.value = true;
      if (message) {
        emitter.emit('login-error', message);
      }
    } else if (type === 'register') {
      showRegisterModal.value = true;
    }
  });
});
onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleClickOutside);
  emitter.off('show-auth');
});
</script>