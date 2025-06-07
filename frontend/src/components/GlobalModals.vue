<template>
  <div>
    <Profile ref="profileRef" />
    
    <LoginForm 
      v-if="showLogin" 
      @close="$emit('login-close')" 
      @login-success="$emit('login-success')" 
    />
    
    <RegisterForm 
      v-if="showRegister" 
      @close="$emit('register-close')" 
      @register-success="$emit('register-success')" 
    />
  </div>
</template>

<script setup>
import { ref } from 'vue';
import Profile from '../views/Profile.vue';
import LoginForm from './LoginForm.vue';
import RegisterForm from './RegisterForm.vue';

defineProps({
  showLogin: Boolean,
  showRegister: Boolean
});

defineEmits(['login-close', 'login-success', 'register-close', 'register-success']);

const profileRef = ref(null);

defineExpose({
  openModal: () => profileRef.value?.openModal()
});
</script>
