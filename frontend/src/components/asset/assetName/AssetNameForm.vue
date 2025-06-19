<template>
  <BaseModal
      :visible="show"
      @update:visible="val => emit('update:show', val)"
      ariaLabelledby="asset-name-title"
  >
    <template #title>
      资产名称维护
      <span v-if="showAdd"> - 新增</span>
      <span v-else-if="showEdit"> - 编辑</span>
    </template>

    <div class="flex flex-col h-full">
      <!-- 搜索和新增按钮 -->
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
          >
            ✕
          </button>
        </div>

        <button
            v-if="!showAdd && !showEdit"
            @click="showAddForm"
            class="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-medium rounded-md px-4 py-2 shadow-sm transition"
            title="新增资产名称"
            type="button"
        >
          ＋ 新增
        </button>
      </div>

      <!-- 新增/编辑表单 -->
      <form
          v-if="showAdd || showEdit"
          @submit.prevent="handleSubmit"
          class="space-y-6 max-w-md"
          novalidate
      >
        <div>
          <label
              for="name"
              class="block text-gray-700 dark:text-gray-300 font-medium mb-1"
          >资产名称 <span class="text-red-500">*</span></label>
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
          <label
              for="description"
              class="block text-gray-700 dark:text-gray-300 font-medium mb-1"
          >描述</label>
         <BaseInput
              id="description"
              v-model="formData.description"
              type="text"
              clearable
              placeholder="请输入描述（可选）"
              class="w-full rounded-md border border-gray-300 dark:border-gray-700 px-3 py-2 text-gray-900 dark:text-gray-100 bg-gray-50 dark:bg-gray-800 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition"
              autocomplete="off"
          />
        </div>

        <div class="flex justify-end space-x-3">
          <button
              type="button"
              @click="cancelEditOrAdd"
              class="px-4 py-2 rounded-md text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition"
          >
            取消
          </button>
          <button
              type="submit"
              :disabled="loading || !!formError"
              :class="[
              'px-4 py-2 rounded-md text-white font-semibold transition',
              loading || formError ? 'bg-indigo-300 cursor-not-allowed' : 'bg-indigo-600 hover:bg-indigo-700',
            ]"
          >
            {{ loading ? '提交中...' : showEdit ? '保存' : '确定' }}
          </button>
        </div>
      </form>

      <!-- 资产名称列表 -->
      <div v-else class="flex-1 overflow-auto">
        <table class="min-w-full text-left text-gray-900 dark:text-gray-100">
          <thead class="border-b border-gray-300 dark:border-gray-700">
          <tr>
            <th class="px-4 py-2">名称</th>
            <th class="px-4 py-2">描述</th>
            <th class="px-4 py-2 w-32 text-center">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="item in pagedNames"
              :key="item.id"
              class="border-b border-gray-200 dark:border-gray-800 hover:bg-gray-100 dark:hover:bg-gray-800"
          >
            <td class="px-4 py-2 truncate" :title="item.name">{{ item.name }}</td>
            <td class="px-4 py-2 truncate" :title="item.description">{{ item.description }}</td>
            <td class="text-center">
              <div class="text-center">
                <BaseActions
                    :record="item"
                    type="asset"
                    @edit="$emit('edit', item.id)"
                    @delete="$emit('delete', item)"
                />
              </div>
            </td>
          </tr>
          <tr v-if="pagedNames.length === 0">
            <td :colspan="tableHeaders.length" class="py-8">
              <BaseEmptyState
                  icon="Wallet"
                  message="暂无资产名称"
                  description="点击上方的新增按钮添加资产名称"
              />
            </td>
          </tr>
          </tbody>
        </table>

        <!-- 分页 -->
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

const props = defineProps({
  show: Boolean,
})

const emit = defineEmits(['update:show', 'refresh', 'edit', 'delete'])

const searchTerm = ref('')
const names = ref([])
const loading = ref(false)
const error = ref(null)

const showAdd = ref(false)
const showEdit = ref(false)

const formError = ref('')
const formData = ref({ id: null, name: '', description: '' })

const currentPage = ref(1)
const pageSize = 10

// 这里定义表头，供 colspan 使用
const tableHeaders = ['名称', '描述', '操作']

const totalPages = computed(() => Math.ceil(names.value.length / pageSize))

const pagedNames = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return names.value.slice(start, start + pageSize)
})

// 监听 show 变化，同步到 BaseModal
watch(
    () => props.show,
    (val) => {
      if (val) {
        fetchNames()
      } else {
        resetAll()
      }
    }
)

watch(
    searchTerm,
    debounce(() => {
      fetchNames(1)
    }, 300)
)

function debounce(fn, delay = 300) {
  let timer = null
  return function (...args) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn.apply(this, args), delay)
  }
}

async function fetchNames(page = 1) {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/asset-names', {
      params: {
        current: page,
        size: pageSize,
        name: searchTerm.value,
      },
    })
    if (res.data?.success) {
      const data = res.data.data
      names.value = data.records || []
      currentPage.value = data.current || 1
      if (typeof data.total === 'number') {
        totalPages.value = Math.ceil(data.total / pageSize)
      }
      if (searchTerm.value) {
        if ((data.total || 0) === 0) {
          emitter.emit('notify', { message: '未找到匹配的资产名称', type: 'info' })
        } else {
          emitter.emit('notify', { message: `查询到 ${data.total} 条资产名称`, type: 'success' })
        }
      }
    } else {
      error.value = res.data?.message || '获取资产名称列表失败'
      emitter.emit('notify', { message: error.value, type: 'error' })
    }
  } catch (err) {
    error.value = err.message || '获取资产名称列表失败'
    emitter.emit('notify', { message: error.value, type: 'error' })
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
}

function startEdit(item) {
  formData.value = { id: item.id, name: item.name, description: item.description || '' }
  formError.value = ''
  showEdit.value = true
  showAdd.value = false
}

function cancelEditOrAdd() {
  formError.value = ''
  showAdd.value = false
  showEdit.value = false
}

async function handleSubmit() {
  if (!formData.value.name.trim()) {
    formError.value = '资产名称不能为空'
    return
  }

  loading.value = true
  formError.value = ''

  try {
    if (showAdd.value) {
      const res = await axios.post('/api/asset-names', {
        name: formData.value.name.trim(),
        description: formData.value.description.trim() || null,
      })
      if (res.data?.success) {
        emitter.emit('notify', { message: '添加成功', type: 'success' })
        await fetchNames(currentPage.value)
        emit('refresh')
        showAdd.value = false
      } else {
        handleApiError(res.data?.message)
      }
    } else if (showEdit.value) {
      const res = await axios.put(`/api/asset-names/${formData.value.id}`, {
        name: formData.value.name.trim(),
        description: formData.value.description.trim() || null,
      })
      if (res.data?.success) {
        emitter.emit('notify', { message: '保存成功', type: 'success' })
        await fetchNames(currentPage.value)
        emit('refresh')
        showEdit.value = false
      } else {
        handleApiError(res.data?.message)
      }
    }
  } catch (err) {
    handleApiError(err.message)
  } finally {
    loading.value = false
  }
}

function handleApiError(msg) {
  if (msg.includes('Duplicate entry') || msg.includes('uniq_name')) {
    formError.value = '该资产名称已存在'
  } else {
    error.value = msg
    emitter.emit('notify', { message: msg, type: 'error' })
  }
}

async function handleDelete(item) {
  if (!confirm(`确定删除资产名称 "${item.name}" 吗？`)) return
  loading.value = true
  error.value = null
  try {
    const res = await axios.delete(`/api/asset-names/${item.id}`)
    if (res.data?.success) {
      emitter.emit('notify', { message: '删除成功', type: 'success' })
      await fetchNames(currentPage.value)
      emit('refresh')
    } else {
      error.value = res.data?.message || '删除失败'
      emitter.emit('notify', { message: error.value, type: 'error' })
    }
  } catch (err) {
    error.value = err.message || '删除失败'
    emitter.emit('notify', { message: error.value, type: 'error' })
  } finally {
    loading.value = false
  }
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

function handleClose() {
  resetAll()
  emit('update:show', false)
}

function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchNames(page)
}
</script>
