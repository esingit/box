<template>
  <transition name="fade">
    <div v-if="visible" class="modal-overlay">
      <div class="modal-container">
        <div class="modal-header">{{ title }}</div>
        <div class="modal-body">{{ message }}</div>
        <div class="modal-footer">
          <button class="btn btn-primary" @click="onConfirm">{{ confirmText || '确定' }}</button>
          <button class="btn btn-text" @click="onCancel">{{ cancelText || '取消' }}</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import emitter from '@/utils/eventBus.js'

const visible = ref(false)
const title = ref('操作确认')
const message = ref('确定要执行此操作吗？')
let confirmCallback = null
let cancelCallback = null

function showConfirm(opts) {
  title.value = opts.title || '操作确认'
  message.value = opts.message || '确定要执行此操作吗？'
  confirmCallback = opts.onConfirm || null
  cancelCallback = opts.onCancel || null
  visible.value = true
}

function onConfirm() {
  visible.value = false
  if (confirmCallback) confirmCallback()
}
function onCancel() {
  visible.value = false
  if (cancelCallback) cancelCallback()
}

onMounted(() => {
  emitter.on('confirm', showConfirm)
})
</script>
