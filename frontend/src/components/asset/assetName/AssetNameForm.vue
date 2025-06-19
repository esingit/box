<template>
  <BaseModal
      :visible="modalVisible"
      width="800px"
      @update:visible="val => (modalVisible = val)"
      ariaLabelledby="asset-name-title"
  >
    <template #title>
      资产名称维护
      <span v-if="showAdd">新增</span>
      <span v-else-if="showEdit">编辑</span>
    </template>

    <div class="flex flex-col h-full">
      <!-- 搜索与新增按钮 -->
      <div class="flex items-center justify-between mb-4 space-x-3">
        <div class="relative flex-1">
          <BaseInput
              v-model="searchTerm"
              type="text"
              required
              clearable
              placeholder="搜索资产名称"
              autocomplete="off"
          />
          <button
              v-if="searchTerm"
              @click="clearSearch"
              class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              aria-label="清除搜索"
              type="button"
          >✕</button>
        </div>

        <button
            v-if="!showAdd && !showEdit"
            @click="showAddForm"
            class="btn-primary"
            title="新增资产名称"
            type="button"
        >新增</button>
      </div>

      <!-- 表单区 -->
      <form
          v-if="showAdd || showEdit"
          @submit.prevent="handleSubmit"
          class="space-y-6 max-w-md"
          novalidate
      >
        <div>
          <label for="name" class="block text-gray-700 font-medium mb-1">资产名称 <span class="text-red-500">*</span></label>
          <BaseInput
              id="name"
              v-model.trim="formData.name"
              type="text"
              required
              clearable
              placeholder="请输入资产名称"
              @input="formError = ''"
              autocomplete="off"
          />
          <p v-if="formError" class="mt-1 text-sm text-red-600">{{ formError }}</p>
        </div>

        <div>
          <label for="description" class="block text-gray-700 font-medium mb-1">描述</label>
          <BaseInput
              id="description"
              v-model="formData.description"
              type="text"
              clearable
              placeholder="请输入描述（可选）"
              autocomplete="off"
          />
        </div>

        <div class="flex justify-end space-x-3">
          <button
              type="button"
              @click="cancelEditOrAdd"
              class="px-4 py-2 rounded-md text-gray-700 hover:bg-gray-100"
          >取消</button>
          <button
              type="submit"
              :disabled="loading || !!formError"
              :class="[
              'px-4 py-2 rounded-md text-white font-semibold transition',
              loading || formError ? 'bg-indigo-300 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700',
            ]"
          >{{ loading ? '提交中...' : showEdit ? '保存' : '确定' }}</button>
        </div>
      </form>

      <!-- 列表 -->
      <div v-else class="flex-1 overflow-auto">
        <table class="min-w-full text-left text-gray-900">
          <thead class="border-b border-gray-300">
          <tr>
            <th class="px-4 py-2">名称</th>
            <th class="px-4 py-2">描述</th>
            <th class="px-4 py-2 w-32 text-center">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in pagedNames" :key="item.id" class="border-b hover:bg-gray-100">
            <td class="px-4 py-2 truncate" :title="item.name">{{ item.name }}</td>
            <td class="px-4 py-2 truncate" :title="item.description">{{ item.description }}</td>
            <td class="text-center">
              <BaseActions
                  :record="item"
                  type="asset"
                  @edit="startEdit(item)"
                  @delete="$emit('delete', item)"
              />
            </td>
          </tr>
          <tr v-if="pagedNames.length === 0">
            <td :colspan="3" class="py-8">
              <BaseEmptyState
                  icon="Wallet"
                  message="暂无资产名称"
                  description="点击上方的新增按钮添加资产名称"
              />
            </td>
          </tr>
          </tbody>
        </table>

        <BasePagination
            :currentPage="currentPage"
            :totalPages="totalPages"
            @update:page="changePage"
        />
      </div>
    </div>
  </BaseModal>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import axios from 'axios'
import emitter from '@/utils/eventBus.ts'

import BaseModal from '@/components/base/BaseModal.vue'
import BasePagination from '@/components/base/BasePagination.vue'
import BaseActions from '@/components/base/BaseActions.vue'
import BaseEmptyState from '@/components/base/BaseEmptyState.vue'
import BaseInput from '@/components/base/BaseInput.vue'

const emit = defineEmits(['refresh', 'edit', 'delete'])

const modalVisible = ref(false)
const showAdd = ref(false)
const showEdit = ref(false)
const searchTerm = ref('')
const names = ref([])
const loading = ref(false)
const error = ref(null)
const formError = ref('')
const formData = ref({ id: null, name: '', description: '' })

const currentPage = ref(1)
const pageSize = 10

const pagedNames = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return names.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.ceil(names.value.length / pageSize))

watch(modalVisible, val => {
  if (val) fetchNames()
  else resetAll()
})

watch(searchTerm, debounce(() => fetchNames(1), 300))

function debounce(fn, delay = 300) {
  let timer = null
  return (...args) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

async function fetchNames(page = 1) {
  loading.value = true
  try {
    const res = await axios.get('/api/asset-names', {
      params: { current: page, size: pageSize, name: searchTerm.value },
    })
    if (res.data?.success) {
      const data = res.data.data
      names.value = data.records || []
      currentPage.value = data.current || 1
    }
  } catch (err) {
    error.value = err.message || '获取失败'
  } finally {
    loading.value = false
  }
}

function clearSearch() {
  searchTerm.value = ''
  fetchNames()
}

function showAddForm() {
  formData.value = { id: null, name: '', description: '' }
  formError.value = ''
  showAdd.value = true
  showEdit.value = false
  modalVisible.value = true
}

function startEdit(item) {
  formData.value = { id: item.id, name: item.name, description: item.description || '' }
  formError.value = ''
  showEdit.value = true
  showAdd.value = false
  modalVisible.value = true
}

function cancelEditOrAdd() {
  handleClose()
}

function handleClose() {
  resetAll()
  modalVisible.value = false
}

function resetAll() {
  searchTerm.value = ''
  names.value = []
  loading.value = false
  error.value = null
  showAdd.value = false
  showEdit.value = false
  formError.value = ''
  formData.value = { id: null, name: '', description: '' }
  currentPage.value = 1
}

async function handleSubmit() {
  if (!formData.value.name.trim()) {
    formError.value = '资产名称不能为空'
    return
  }

  loading.value = true
  try {
    const payload = {
      name: formData.value.name.trim(),
      description: formData.value.description.trim() || null,
    }
    let res
    if (showAdd.value) {
      res = await axios.post('/api/asset-names', payload)
    } else {
      res = await axios.put(`/api/asset-names/${formData.value.id}`, payload)
    }

    if (res.data?.success) {
      emitter.emit('notify', { message: '保存成功', type: 'success' })
      await fetchNames(currentPage.value)
      emit('refresh')
      handleClose()
    } else {
      formError.value = res.data?.message || '保存失败'
    }
  } catch (err) {
    formError.value = err.message || '请求失败'
  } finally {
    loading.value = false
  }
}

function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchNames(page)
}
</script>
