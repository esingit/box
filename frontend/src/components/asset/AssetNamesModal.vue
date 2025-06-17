<template>
  <BaseModal
      :visible="show"
      :widthClass="'w-full max-w-4xl max-h-[90vh]'"
      @update:visible="handleVisibleChange"
  >
    <!-- 头部 -->
    <template #header>
      <ModalHeader
          title="资产名称维护"
          @close="handleClose"
          class="border-b border-gray-200 dark:border-gray-700"
      />
    </template>

    <!-- 内容 -->
    <div class="flex-1 overflow-auto p-6 flex flex-col">
      <!-- 搜索工具栏 -->
      <div class="flex items-center justify-between mb-4 space-x-3">
        <div class="relative flex-1">
          <input
              v-model="searchTerm"
              type="text"
              placeholder="搜索资产名称"
              class="w-full rounded-md border border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-100 placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
          />
          <button
              v-if="searchTerm"
              @click="clearSearch"
              class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              aria-label="清除搜索"
          >
            <LucideX size="18" />
          </button>
        </div>

        <button
            v-if="!showAdd && !showEdit"
            @click="showAddForm"
            class="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-md px-4 py-2 shadow-sm transition"
            title="新增资产名称"
        >
          <LucidePlus size="18" />
          <span>新增</span>
        </button>
      </div>

      <!-- 资产名称表单 -->
      <AssetNameForm
          v-if="showAdd || showEdit"
          :show="showAdd || showEdit"
          :loading="loading"
          :form-error="formError"
          :edit-data="showEdit ? { id: editingId, name: editingName, description: editingDescription } : null"
          @close="() => {
          formError = ''
          showAdd ? (showAdd = false) : (showEdit = false)
        }"
          @submit="handleFormSubmit"
          @update:form-error="val => formError = val"
      />

      <!-- 列表区域 -->
      <div v-else class="flex-1 overflow-auto">
        <template v-if="loading">
          <div class="space-y-3">
            <SkeletonCard v-for="n in 5" :key="n" />
          </div>
        </template>
        <template v-else>
          <AssetNamesList
              :names="filteredNameList"
              :current="current"
              :total="total"
              :page-size="pageSize"
              @edit="startEdit"
              @delete="handleDelete"
              @page-change="handlePageChange"
              @page-size-change="handlePageSizeChange"
          />
        </template>
      </div>
    </div>
  </BaseModal>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { LucidePlus, LucideX } from 'lucide-vue-next'
import BaseModal from '@/components/base/BaseModal.vue'
import ModalHeader from './ModalHeader.vue'
import AssetNamesList from './AssetNamesList.vue'
import AssetNameForm from './AssetNameForm.vue'
import SkeletonCard from '@/components/base/SkeletonCard.vue'
import axios from 'axios'
import emitter from '@/utils/eventBus.ts'

// 你的防抖函数保持不变
function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['close', 'refresh'])

const searchTerm = ref('')
const names = ref([])
const loading = ref(false)
const error = ref(null)
const showAdd = ref(false)
const showEdit = ref(false)
const editingId = ref(null)
const editingName = ref('')
const editingDescription = ref('')
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)
const formError = ref('')

function handleClose() {
  resetState()
  emit('close')
}

function resetState() {
  showAdd.value = false
  showEdit.value = false
  searchTerm.value = ''
  editingId.value = null
  editingName.value = ''
  editingDescription.value = ''
  error.value = null
}

watch(searchTerm, debounce(() => {
  fetchNames(1)
}, 300))

async function fetchNames(page = current.value) {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/asset-names', {
      params: {
        current: page,
        size: pageSize.value,
        name: searchTerm.value
      }
    })
    if (res.data?.success) {
      const data = res.data.data
      names.value = data.records || []
      total.value = data.total || 0
      current.value = data.current || 1
      pageSize.value = data.size || 10
      if (searchTerm.value) {
        if (Number(total.value) === 0) {
          emitter.emit('notify', { message: '未找到匹配的资产名称', type: 'info' })
        } else {
          emitter.emit('notify', { message: `查询到 ${total.value} 条资产名称`, type: 'success' })
        }
      }
    } else {
      error.value = res.data?.message || '获取资产名称列表失败'
      emitter.emit('notify', error.value, 'error')
    }
  } catch (error) {
    console.error('获取资产名称列表失败:', error)
    error.value = error.message || '获取资产名称列表失败'
    emitter.emit('notify', error.value, 'error')
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchTerm.value = ''
  fetchNames(1)
}

function startEdit(name) {
  editingId.value = name.id
  editingName.value = name.name
  editingDescription.value = name.description || ''
  showEdit.value = true
}

function cancelEdit() {
  editingId.value = null
  editingName.value = ''
  editingDescription.value = ''
  showEdit.value = false
}

function showAddForm() {
  showAdd.value = true
}

const filteredNameList = computed(() => {
  const start = (current.value - 1) * pageSize.value
  const end = start + pageSize.value
  return names.value.slice(start, end)
})

watch(names, (newList) => {
  total.value = newList.length
})

function handlePageChange(page) {
  current.value = page
}

function handlePageSizeChange(size) {
  pageSize.value = size
  current.value = 1
}

async function handleFormSubmit(data) {
  loading.value = true
  formError.value = ''
  try {
    if (showAdd.value) {
      const { name, description } = data
      const res = await axios.post('/api/asset-names', {
        name: name.trim(),
        description: description.trim() || null
      })
      if (res.data?.success) {
        emitter.emit('notify', '添加成功', 'success')
        await fetchNames()
        emit('refresh')
        showAdd.value = false
      } else {
        const errMsg = res.data?.message || '添加失败'
        if (errMsg.includes('Duplicate entry') || errMsg.includes('uniq_name')) {
          formError.value = '该资产名称已存在'
        } else {
          error.value = errMsg
          emitter.emit('notify', errMsg, 'error')
        }
      }
    } else if (showEdit.value) {
      const { id, name, description } = data
      const res = await axios.put(`/api/asset-names/${id}`, {
        name: name.trim(),
        description: description.trim() || null
      })
      if (res.data?.success) {
        emitter.emit('notify', '保存成功', 'success')
        await fetchNames()
        emit('refresh')
        showEdit.value = false
      } else {
        const errMsg = res.data?.message || '保存失败'
        if (errMsg.includes('Duplicate entry') || errMsg.includes('uniq_name')) {
          formError.value = '该资产名称已存在'
        } else {
          error.value = errMsg
          emitter.emit('notify', errMsg, 'error')
        }
      }
    }
  } catch (err) {
    console.error('操作失败:', err)
    error.value = err.message || '操作失败'
    emitter.emit('notify', error.value, 'error')
  } finally {
    loading.value = false
  }
}

async function handleDelete(name) {
  loading.value = true
  error.value = null
  try {
    const res = await axios.delete(`/api/asset-names/${name.id}`)
    if (res.data?.success) {
      emitter.emit('notify', '删除成功', 'success')
      await fetchNames()
      emit('refresh')
    } else {
      error.value = res.data?.message || '删除失败'
      emitter.emit('notify', error.value, 'error')
    }
  } catch (error) {
    console.error('删除资产名称失败:', error)
    error.value = error.message || '删除失败'
    emitter.emit('notify', error.value, 'error')
  } finally {
    loading.value = false
  }
}

fetchNames()

watch(() => props.show, (newVal) => {
  if (newVal) {
    fetchNames()
  } else {
    handleClose()
  }
})

// 这里要同步外部 v-model:visible 关闭弹窗
function handleVisibleChange(val) {
  if (!val) handleClose()
  emit('close', val)
}
</script>
