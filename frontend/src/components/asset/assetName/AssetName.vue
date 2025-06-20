<template>
  <!-- 主弹窗，显示搜索和列表 -->
  <BaseModal
      :visible="visible"
      title="资产名称维护"
      width="1000px"
      @update:visible="close"
  >
    <div class="flex flex-col h-full">
      <AssetNameSearch
          :searchTerm="searchTerm"
          :loading="loadingList"
          @update:searchTerm="onSearchTermChange"
          @addNew="openAddModal"
      />
      <AssetNameList
        :list="list"
        :loadingList="loadingList"
        :pagination="pagination"
        :totalPages="totalPages"
        @edit="openEditModal"
        @delete="$emit('delete', $event)"
        @changePage="changePage"
    />
    </div>
  </BaseModal>

  <!-- 新增弹窗 -->
  <BaseModal
      :visible="addModalVisible"
      title="新增资产名称"
      width="600px"
      @update:visible="val => (addModalVisible = val)"
  >
    <AssetNameForm
        :formData="newFormData"
        :loadingSubmit="loadingSubmit"
        :formError="formError"
        :isEdit="false"
        @submit="handleAddSubmit"
        @closeModal="closeAddModal"
        @update:formData="val => (newFormData = val)"
        @clearError="() => (formError = '')"
    />
  </BaseModal>

  <!-- 编辑弹窗 -->
  <BaseModal
      :visible="editModalVisible"
      title="编辑资产名称"
      width="600px"
      @update:visible="val => (editModalVisible = val)"
  >
    <AssetNameForm
        :formData="editFormData"
        :loadingSubmit="loadingSubmit"
        :formError="formError"
        :isEdit="true"
        @submit="handleEditSubmit"
        @closeModal="closeEditModal"
        @update:formData="val => (editFormData = val)"
        @clearError="() => (formError = '')"
    />
  </BaseModal>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useAssetNameStore } from '@/store/assetNameStore'

import BaseModal from '@/components/base/BaseModal.vue'
import AssetNameSearch from './AssetNameSearch.vue'
import AssetNameForm from './AssetNameForm.vue'
import AssetNameList from './AssetNameList.vue'

const emit = defineEmits(['refresh', 'delete'])

const visible = ref(false)           // 主弹窗显示
const addModalVisible = ref(false)   // 新增弹窗显示
const editModalVisible = ref(false)  // 编辑弹窗显示

const searchTerm = ref('')
const formError = ref('')
const loadingSubmit = ref(false)

const newFormData = reactive({ id: null, name: '', description: '' })
const editFormData = reactive({ id: null, name: '', description: '' })

const assetNameStore = useAssetNameStore()
const { list, pagination, loadingList } = storeToRefs(assetNameStore)
const { loadList, updateQuery, setPageNo, resetQuery, addRecord, updateRecord } = assetNameStore

const totalPages = computed(() => Math.ceil(pagination.value.total / pagination.value.pageSize))

defineExpose({ open, close })

function open() {
  visible.value = true
  resetQuery()
  setPageNo(1)
  loadList()
}

function close() {
  visible.value = false
  searchTerm.value = ''
  updateQuery({ name: '' })
  closeAddModal()
  closeEditModal()
}

// 防抖搜索
let timer: number | null = null
watch(searchTerm, val => {
  if (timer) clearTimeout(timer)
  timer = window.setTimeout(() => {
    updateQuery({ name: val })
    setPageNo(1)
    loadList()
  }, 300)
})

function onSearchTermChange(val: string) {
  searchTerm.value = val
}

// 新增弹窗打开
function openAddModal() {
  newFormData.id = null
  newFormData.name = ''
  newFormData.description = ''
  formError.value = ''
  addModalVisible.value = true
}

// 新增弹窗关闭
function closeAddModal() {
  addModalVisible.value = false
  formError.value = ''
}

// 编辑弹窗打开
function openEditModal(item: any) {
  editFormData.id = item.id
  editFormData.name = item.name
  editFormData.description = item.description || ''
  formError.value = ''
  editModalVisible.value = true
}

// 编辑弹窗关闭
function closeEditModal() {
  editModalVisible.value = false
  formError.value = ''
}

// 新增提交
async function handleAddSubmit() {
  if (!newFormData.name.trim()) {
    formError.value = '资产名称不能为空'
    return
  }
  try {
    loadingSubmit.value = true
    await addRecord({
      id: null,
      name: newFormData.name.trim(),
      description: newFormData.description.trim()
    })
    addModalVisible.value = false
    emit('refresh')
    await loadList()
  } catch (err: any) {
    formError.value = err.message || '新增失败'
  } finally {
    loadingSubmit.value = false
  }
}

// 编辑提交
async function handleEditSubmit() {
  if (!editFormData.name.trim()) {
    formError.value = '资产名称不能为空'
    return
  }
  try {
    loadingSubmit.value = true
    await updateRecord({
      id: editFormData.id,
      name: editFormData.name.trim(),
      description: editFormData.description.trim()
    })
    editModalVisible.value = false
    emit('refresh')
    await loadList()
  } catch (err: any) {
    formError.value = err.message || '更新失败'
  } finally {
    loadingSubmit.value = false
  }
}

function changePage(page: number) {
  if (page < 1 || page > totalPages.value) return
  setPageNo(page)
  loadList()
}
</script>
