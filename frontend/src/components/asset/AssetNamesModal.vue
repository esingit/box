<template>
  <div v-if="show" class="modal-overlay">
    <div class="modal-content names-modal">
      <div class="modal-header">
        <h3 class="modal-title">资产名称维护</h3>
        <button class="close-button" @click="handleClose">
          <LucideX class="close-icon" />
        </button>
      </div>
      
      <!-- 搜索和操作栏 -->
      <div class="toolbar">
        <button v-if="!showAdd && !showEdit" class="btn btn-primary" @click="showAddForm">
          <LucidePlus :size="16" class="btn-icon" />
          新增
        </button>
        <div class="search-wrapper">
          <input 
            v-model="searchTerm" 
            type="text" 
            placeholder="搜索资产名称..."
            class="search-input" 
          />
        </div>
      </div>

      <!-- 新增表单 -->
      <div v-if="showAdd" class="add-form">
        <div class="input-group">
          <input 
            v-model="newName" 
            type="text" 
            placeholder="输入资产名称"
            class="form-input"
          />
          <input 
            v-model="newDescription" 
            type="text" 
            placeholder="输入描述（可选）"
            class="form-input"
          />
          <div class="form-actions">
            <button class="btn btn-primary" @click="addName">确定</button>
            <button class="btn btn-text" @click="cancelAdd">取消</button>
          </div>
        </div>
      </div>

      <!-- 编辑表单 -->
      <div v-if="showEdit" class="edit-form">
        <div class="input-group">
          <input 
            v-model="editingName" 
            type="text" 
            placeholder="输入资产名称"
            class="form-input"
          />
          <input 
            v-model="editingDescription" 
            type="text" 
            placeholder="输入描述（可选）"
            class="form-input"
          />
          <div class="form-actions">
            <button class="btn btn-primary" @click="saveEdit">保存</button>
            <button class="btn btn-text" @click="cancelEdit">取消</button>
          </div>
        </div>
      </div>
      
      <!-- 资产名称列表 -->
      <div class="names-list">
        <div v-if="loading" class="loading-state">
          加载中...
        </div>
        <div v-else-if="error" class="error-state">
          {{ error }}
        </div>
        <div v-else-if="names.length === 0" class="empty-state">
          暂无资产名称
        </div>
        <template v-else>
          <div v-for="name in names" :key="name.id" class="name-item">
            <div class="name-info">
              <span class="name-text">{{ name.name }}</span>
              <span v-if="name.description" class="name-desc">{{ name.description }}</span>
            </div>
            <div class="name-actions">
              <button class="action-btn" @click="startEdit(name)" title="编辑">
                <LucidePencil :size="16" />
              </button>
              <button class="action-btn delete" @click="confirmDelete(name)" title="删除">
                <LucideTrash2 :size="16" />
              </button>
            </div>
          </div>
        </template>
      </div>

      <!-- 分页器 -->
      <div class="pagination-wrapper">
        <PaginationBar
          :current="current"
          :total="total"
          :page-size="pageSize"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { LucideX, LucideCheck, LucideEdit2, LucideTrash2, LucidePencil, LucidePlus } from 'lucide-vue-next'
import PaginationBar from '../../components/PaginationBar.vue'
import emitter from '../../utils/eventBus.js'
import axios from '../../utils/axios.js'

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

// 处理关闭模态框
function handleClose() {
  // 重置状态
  showAdd.value = false
  showEdit.value = false
  searchTerm.value = ''
  newName.value = ''
  newDescription.value = ''
  editingId.value = null
  editingName.value = ''
  editingDescription.value = ''
  error.value = null
  
  emit('close')
}

const searchTerm = ref('')
const names = ref([])
const showAdd = ref(false)
const showEdit = ref(false) // 新增编辑模态框显示状态
const loading = ref(false) // 加载状态
const error = ref(null) // 错误状态
const newName = ref('')
const newDescription = ref('') // 新增描述字段
const editingDescription = ref('') // 编辑时的描述字段
const current = ref(1)
const total = ref(0)
const pageSize = ref(10)
const editingId = ref(null)
const editingName = ref('')

// 监听搜索词变化
watch(searchTerm, debounce(() => {
  fetchNames(1) // 搜索时重置到第一页
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

// 添加资产名称
async function addName() {
  if (!newName.value.trim()) return
  
  loading.value = true
  error.value = null
  try {
    const res = await axios.post('/api/asset-names', {
      name: newName.value.trim(),
      description: newDescription.value.trim() || null
    })
    if (res.data?.success) {
      emitter.emit('notify', '添加成功', 'success')
      await fetchNames()
      emit('refresh')
      newName.value = ''
      newDescription.value = ''
      showAdd.value = false
    } else {
      error.value = res.data?.message || '添加失败'
      emitter.emit('notify', error.value, 'error')
    }
  } catch (error) {
    console.error('添加资产名称失败:', error)
    error.value = error.message || '添加失败'
    emitter.emit('notify', error.value, 'error')
  } finally {
    loading.value = false
  }
}

// 编辑资产名称
function startEdit(name) {
  editingId.value = name.id
  editingName.value = name.name
  editingDescription.value = name.description || ''
  showEdit.value = true
}

async function saveEdit() {
  if (!editingName.value.trim() || !editingId.value) return
  
  loading.value = true
  error.value = null
  try {
    const res = await axios.put(`/api/asset-names/${editingId.value}`, {
      name: editingName.value.trim(),
      description: editingDescription.value.trim() || null
    })
    if (res.data?.success) {
      emitter.emit('notify', '保存成功', 'success')
      await fetchNames()
      emit('refresh')
      cancelEdit()
    } else {
      error.value = res.data?.message || '保存失败'
      emitter.emit('notify', error.value, 'error')
    }
  } catch (error) {
    console.error('更新资产名称失败:', error)
    error.value = error.message || '保存失败'
    emitter.emit('notify', error.value, 'error')
  } finally {
    loading.value = false
  }
}

function cancelEdit() {
  editingId.value = null
  editingName.value = ''
  editingDescription.value = ''
  showEdit.value = false
}

// 删除资产名称
async function confirmDelete(name) {
  if (!window.confirm(`确定要删除资产名称"${name.name}"吗？`)) return
  
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

function showAddForm() {
  showAdd.value = true
}

function cancelAdd() {
  showAdd.value = false
  newName.value = ''
}

function handlePageChange(page) {
  // 确保页码在有效范围内
  const validPage = Math.min(Math.max(1, page), Math.ceil(total.value / pageSize.value))
  if (validPage !== page) {
    emitter.emit('notify', '页码超出范围', 'warning')
  }
  fetchNames(validPage)
}

function handlePageSizeChange(size) {
  pageSize.value = size
  // 切换每页条数时,重新计算当前页码
  const maxPage = Math.ceil(total.value / size)
  const validPage = Math.min(current.value, maxPage)
  fetchNames(validPage)
}

// 初始化加载数据
fetchNames()

watch(() => props.show, (newVal) => {
  if (newVal) {
    // 模态框打开时，加载数据
    fetchNames()
  } else {
    // 模态框关闭时，重置状态
    handleClose()
  }
})
</script>
