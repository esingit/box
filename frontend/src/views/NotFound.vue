<template>
  <div class="exception-container flex flex-col items-center justify-center h-[80vh] text-center px-4">
    <div class="max-w-md w-full">
      <div class="text-9xl font-extrabold mb-4 select-none text-gray-300">{{ codeDisplay }}</div>
      <h1 class="text-2xl font-semibold mb-2">{{ message }}</h1>
      <button
          @click="goHome"
          class="mt-6 bg-blue-600 hover:bg-blue-700 text-white py-2 px-6 rounded-md transition"
      >
        返回首页
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

type ResultStatus = 'error' | '404' | '500' | 'info' | 'success' | 'warning' | '403' | '418'

const router = useRouter()
const route = useRoute()

const code = ref<ResultStatus>('404')
const message = ref('页面未找到')

onMounted(() => {
  if (route.name === 'NotFound') {
    code.value = '404'
    message.value = '页面未找到'
  } else if (route.name === 'Error') {
    code.value = '500'
    message.value = '服务器错误'
  } else {
    code.value = 'error'
    message.value = '抱歉，出现了错误'
  }
})

const codeDisplay = computed(() => {
  // error 类型显示通用错误码 400，或者可以自定义
  return code.value === 'error' ? '400' : code.value
})

function goHome() {
  router.push('/home')
}
</script>

<style scoped>
.exception-container {
  /* tailwind 已覆盖 */
}
</style>
