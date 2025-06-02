<template>
  <transition name="fade">
    <div v-if="visible" class="confirm-modal-overlay">
      <div class="confirm-modal">
        <div class="confirm-title">{{ title }}</div>
        <div class="confirm-message">{{ message }}</div>
        <div class="confirm-actions">
          <button class="btn confirm-btn" @click="onConfirm">确定</button>
          <button class="btn cancel-btn" @click="onCancel">取消</button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import emitter from '../utils/eventBus.js'

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

<style scoped>
.confirm-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.18);
  z-index: 4000;
  display: flex;
  align-items: center;
  justify-content: center;
}
.confirm-modal {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.12);
  padding: 32px 28px 20px 28px;
  min-width: 280px;
  max-width: 90vw;
  text-align: center;
  border: 1.5px solid #e0e0e0;
}
.confirm-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #222;
}
.confirm-message {
  font-size: 15px;
  color: #666;
  margin-bottom: 24px;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 18px;
}
.confirm-btn {
  background: #222;
  color: #fff;
  border: none;
  border-radius: 999px;
  padding: 8px 32px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s;
}
.confirm-btn:hover {
  background: #444;
}
.cancel-btn {
  background: #f7f7f8;
  color: #222;
  border: 1.5px solid #e0e0e0;
  border-radius: 999px;
  padding: 8px 32px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.18s;
}
.cancel-btn:hover {
  background: #ededed;
}
</style>
