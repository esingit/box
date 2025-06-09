<template>
  <div v-if="show" class="modal-overlay" @click.self="handleClose">
    <div class="modal-container modal-lg">
      <ModalHeader 
        title="资产名称维护" 
        @close="handleClose" 
      />

      <div class="modal-body">
        <!-- 搜索工具栏 -->
        <div class="search-toolbar">
          <div class="search-wrapper">
            <input
              v-model="searchTerm"
              type="text"
              class="input search-input"
              placeholder="搜索资产名称"
            />
            <div v-if="searchTerm" class="clear-icon" @click="clearSearch">
              <LucideX :size="18" />
            </div>
          </div>
          <div class="search-actions">
            <button 
              v-if="!showAdd && !showEdit" 
              class="btn btn-primary" 
              title="新增资产名称"
              @click="showAddForm"
            >
              <LucidePlus class="btn-icon" :size="18" />
              <span>新增</span>
            </button>
          </div>
        </div>

        <!-- 资产名称表单弹窗 -->
        <AssetNameForm
          v-if="showAdd || showEdit"
          :show="showAdd || showEdit"
          :loading="loading"
          :edit-data="showEdit ? {
            id: editingId,
            name: editingName,
            description: editingDescription
          } : null"
          @close="() => showAdd ? (showAdd = false) : (showEdit = false)"
          @submit="handleFormSubmit"
        />

        <!-- 列表区域 -->
        <div v-else class="content-container">
          <div v-if="loading" class="empty-container">
            <EmptyState
              message="加载中..."
              icon="Loader"
            />
          </div>
          <template v-else>
            <div class="table-wrapper">
              <AssetNamesTable 
                :names="filteredNameList"
                @edit="startEdit"
                @delete="handleDelete"
              />
            </div>
            
            <div class="pagination-container mt-4">
              <PaginationBar
                :current="current"
                :total="total"
                :page-size="pageSize"
                @page-change="handlePageChange"
                @page-size-change="handlePageSizeChange"
              />
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { LucidePlus, LucideSearch, LucideX } from 'lucide-vue-next'
import EmptyState from '@/components/common/EmptyState.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import ModalHeader from './ModalHeader.vue'
import AssetNamesTable from './AssetNamesTable.vue'
import AssetNameForm from './AssetNameForm.vue'
import axios from '@/utils/axios.js'
import emitter from '@/utils/eventBus.js'

// 防抖函数
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

// 状态管理
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
const total = ref(0)
const pageSize = ref(10)
const formError = ref('')

// 处理关闭模态框
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

// 监听搜索词变化
watch(searchTerm, debounce(() => {
  fetchNames(1)
}, 300))

// 获取资产名称列表
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

// 清除搜索
function clearSearch() {
  searchTerm.value = ''
  fetchNames(1)
}

// 处理编辑
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

function handlePageChange(page) {
  const maxPage = Math.ceil(total.value / pageSize.value)
  const validPage = Math.min(Math.max(1, page), maxPage)
  if (validPage !== page) {
    emitter.emit('notify', '页码超出范围', 'warning')
  }
  fetchNames(validPage)
}

function handlePageSizeChange(size) {
  pageSize.value = size
  const maxPage = Math.ceil(total.value / size)
  const validPage = Math.min(current.value, maxPage)
  fetchNames(validPage)
}

// 处理表单提交
async function handleFormSubmit(data) {
  loading.value = true
  formError.value = ''
  try {
    if (showAdd.value) {
      // 处理新增
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
      // 处理编辑
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

// 处理删除
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

// 初始化加载数据
fetchNames()

// 监听显示状态变化
watch(() => props.show, (newVal) => {
  if (newVal) {
    fetchNames()
  } else {
    handleClose()
  }
})

// 过滤后的名称列表
const filteredNameList = computed(() => {
  if (!searchTerm.value) {
    return names.value
  }
  return names.value.filter(item => 
    item.name.toLowerCase().includes(searchTerm.value.toLowerCase()) ||
    item.description?.toLowerCase().includes(searchTerm.value.toLowerCase())
  )
})
</script>
