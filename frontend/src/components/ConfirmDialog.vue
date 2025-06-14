<template>
  <transition name="fade">
    <div v-if="visible" class="modal-overlay" @click.self="onCancel">
      <div class="modal-container modal-sm">
        <div class="modal-header">
          <h3 class="modal-title">{{ title }}</h3>
          <button class="close-button" @click="onCancel" title="关闭">
            <LucideX :size="20" />
          </button>
        </div>
        <div class="modal-body">
          <div class="confirm-message" style="white-space: pre-line;">{{ message }}</div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-text" @click="onCancel">
            {{ cancelText }}
          </button>
          <button 
            class="btn" 
            :class="type === 'danger' ? 'btn-danger' : 'btn-primary'"
            @click="onConfirm"
          >
            {{ confirmText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { LucideX } from 'lucide-vue-next'
import emitter from '@/utils/eventBus.ts'

const visible = ref(false)
const title = ref('操作确认')
const message = ref('确定要执行此操作吗？')
const confirmText = ref('确定')
const cancelText = ref('取消')
const type = ref('primary')

let confirmCallback = null
let cancelCallback = null

function showConfirm(opts) {
  title.value = opts.title || '操作确认'
  message.value = opts.message || '确定要执行此操作吗？'
  confirmText.value = opts.confirmText || '确定'
  cancelText.value = opts.cancelText || '取消'
  type.value = opts.type || 'primary'
  confirmCallback = opts.onConfirm
  cancelCallback = opts.onCancel
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
