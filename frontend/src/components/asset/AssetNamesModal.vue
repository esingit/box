<template>
  <div v-if="show" class="names-modal">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">资产名称管理</h3>
        <button class="close-button" @click="$emit('close')">
          <LucideX class="close-icon" />
        </button>
      </div>
      <div class="modal-controls">
        <input
          v-model="searchTerm"
          type="text"
          class="search-input"
          placeholder="搜索资产名称..."
        />
        <button class="btn btn-black" @click="showAddForm">添加名称</button>
      </div>
      <div class="names-table-container">
        <div v-if="loading" class="loading-overlay">
          <span>加载中...</span>
        </div>
        <div v-if="error" class="error-message">
          {{ error }}
        </div>
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>描述</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="!loading && !names.length">
              <td colspan="3" class="text-center">暂无数据</td>
            </tr>
            <tr v-for="name in names" :key="name.id">
              <td>{{ name.name }}</td>
              <td>{{ name.description || '-' }}</td>
              <td class="operations">
                <button class="btn btn-link" @click="startEdit(name)" title="编辑">
                  <LucideEdit2 size="16" />
                </button>
                <button class="btn btn-link" @click="confirmDelete(name)" title="删除">
                  <LucideTrash2 size="16" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination-container">
        <PaginationBar
          :current="current"
          :total="total"
          :page-size="pageSize"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
        />
      </div>
      <!-- 添加名称表单 -->
      <div v-if="showAdd" class="add-form">
        <div class="modal-content">
          <div class="modal-header">
            <h3 class="modal-title">添加资产名称</h3>
            <button class="close-button" @click="cancelAdd">
              <LucideX class="close-icon" />
            </button>
          </div>
          <div class="form-content">
            <div class="form-group">
              <label>名称</label>
              <input
                v-model="newName"
                type="text"
                class="input"
                placeholder="请输入资产名称"
              />
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea
                v-model="newDescription"
                class="input textarea"
                placeholder="请输入描述信息（可选）"
                rows="3"
              ></textarea>
            </div>
            <div class="form-actions">
              <button class="btn btn-gray" @click="cancelAdd">取消</button>
              <button
                class="btn btn-black"
                :disabled="!newName.trim()"
                @click="addName"
              >
                确定
              </button>
            </div>
          </div>
        </div>
      </div>
      <!-- 编辑名称表单 -->
      <div v-if="showEdit" class="edit-form">
        <div class="modal-content">
          <div class="modal-header">
            <h3 class="modal-title">编辑资产名称</h3>
            <button class="close-button" @click="cancelEdit">
              <LucideX class="close-icon" />
            </button>
          </div>
          <div class="form-content">
            <div class="form-group">
              <label>名称</label>
              <input
                v-model="editingName"
                type="text"
                class="input"
                placeholder="请输入资产名称"
              />
            </div>
            <div class="form-group">
              <label>描述</label>
              <textarea
                v-model="editingDescription"
                class="input textarea"
                placeholder="请输入描述信息（可选）"
                rows="3"
              ></textarea>
            </div>
            <div class="form-actions">
              <button class="btn btn-gray" @click="cancelEdit">取消</button>
              <button
                class="btn btn-black"
                :disabled="!editingName.trim()"
                @click="saveEdit"
              >
                保存
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import '@/assets/asset.css'
import { ref, watch, computed } from 'vue'
import { LucideX, LucideCheck, LucideEdit2, LucideTrash2 } from 'lucide-vue-next'
import PaginationBar from '@/components/PaginationBar.vue'
import emitter from '@/utils/eventBus.js'
import axios from '@/utils/axios.js'

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
</script>
