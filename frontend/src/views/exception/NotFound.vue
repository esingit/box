<template>
  <n-space vertical align="center" justify="center" class="exception-container">
    <n-result
        :status="code"
        :title="code"
        :sub-title="message"
        class="exception-result"
    >
      <n-button type="primary" @click="goHome" class="exception-button">
        返回首页
      </n-button>
    </n-result>
  </n-space>
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

function goHome() {
  router.push('/home')
}
</script>

<style scoped>
.exception-container {
  height: 80vh;
  text-align: center;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.exception-result {
  max-width: 420px;
  margin: 0 auto;
}

.exception-button {
  margin-top: var(--space-md);
}
</style>
