<template>
  <div class="page-container">
    <div class="page-title">
      <LucideClipboardList class="title-icon" color="#222" size="24" />
      <span>记录</span>
    </div>
    <div class="fitness-query-bar">
      <div class="query-fields query-bar-fields">
        <select v-model="query.typeId">
          <option value="">全部类型</option>
          <option v-for="t in types" :key="t.id" :value="t.id">{{ t.value1 }}</option>
        </select>
        <input type="date" v-model="query.startDate" />
        <span class="to-text">至</span>
        <input type="date" v-model="query.endDate" />
        <input v-model="query.remark" placeholder="备注关键词" type="text" />
      </div>
      <div class="query-btns">
        <button class="btn btn-white" title="查询" @click="handleQuery">
          <LucideSearch size="18" style="vertical-align: middle;" />
        </button>
        <button class="btn btn-gray" title="重置" @click="resetQuery">
          <LucideRotateCcw size="18" style="vertical-align: middle;" />
        </button>
      </div>
    </div>
    <button class="btn btn-black" @click="showAddModal = true">添加记录</button>
    <FitnessModal
      :show="showAddModal"
      :form="form"
      :types="types"
      :units="units"
      :loading="adding"
      title="添加记录"
      confirmText="确定"
      remarkPlaceholder="备注（可选）"
      @submit="handleAddRecord"
      @cancel="showAddModal = false"
    />
    <FitnessList
      :records="records"
      :current="current"
      :total="total"
      :page-size="pageSize"
      @edit="editRecord"
      @delete="deleteRecord"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />
    <FitnessModal
      :show="editingIdx !== null"
      :form="editForm"
      :types="types"
      :units="units"
      :loading="false"
      title="编辑记录"
      confirmText="保存"
      remarkPlaceholder="备注"
      @submit="saveEdit"
      @cancel="cancelEdit"
    />
  </div>
</template>

<script setup>
import '@/assets/base.css'
import '@/assets/fitness.css'
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { LucideClipboardList, LucideSearch, LucideRotateCcw } from 'lucide-vue-next'
import emitter from '@/utils/eventBus.js'
import axios from '@/utils/axios.js'
import { formatFitnessRecord, clearCommonMetaCache } from '@/utils/commonMeta'
import FitnessModal from '@/components/fitness/FitnessModal.vue'
import FitnessList from '@/components/fitness/FitnessList.vue'

const types = ref([])
const units = ref([])
const records = ref([])
const adding = ref(false)

// 添加表单和编辑表单统一用 reactive 管理
const form = reactive({
  typeId: null,
  count: 1,
  unitId: null,
  finishTime: getTodayDate(),
  remark: ''
})
const editForm = reactive({
  typeId: null,
  count: 1,
  unitId: null,
  finishTime: getTodayDate(),
  remark: ''
})
const editingIdx = ref(null)
const showAddModal = ref(false)
const query = reactive({
  typeId: '',
  startDate: '',
  endDate: '',
  remark: ''
})
const current = ref(1)
const total = ref(0)
const pageSize = ref(7)

async function initSelectOptions() {
  const [typeRes, unitRes] = await Promise.all([
    axios.get('/api/common-meta/by-type', { params: { typeCode: 'FITNESS_TYPE' } }),
    axios.get('/api/common-meta/by-type', { params: { typeCode: 'UNIT' } })
  ])
  if (typeRes.data?.success) {
    types.value = typeRes.data.data
    if (types.value.length > 0) {
      form.typeId = types.value[0].id
      editForm.typeId = types.value[0].id
    }
  }
  if (unitRes.data?.success) {
    units.value = unitRes.data.data
    if (units.value.length > 0) {
      form.unitId = units.value[0].id
      editForm.unitId = units.value[0].id
    }
  }
}

async function fetchRecords(page = 1, size = pageSize.value) {
  try {
    const params = {
      page,
      pageSize: size
    }
    if (query.typeId) params.typeId = query.typeId
    if (query.startDate) params.startDate = query.startDate
    if (query.endDate) params.endDate = query.endDate
    if (query.remark) params.remark = query.remark
    const res = await axios.get('/api/fitness-record/list', { params })
    if (res.data?.success) {
      const rawRecords = (res.data.data?.records) || []
      // 格式化每条记录的类型和单位
      records.value = await Promise.all(rawRecords.map(record => formatFitnessRecord(record)))
      total.value = res.data.data ? Number(res.data.data.total) : 0
      current.value = res.data.data ? Number(res.data.data.current) : 1
      pageSize.value = res.data.data ? Number(res.data.data.size) : pageSize.value
    } else {
      emitter.emit('notify', '获取数据失败: ' + (res.data?.message || '未知错误'), 'error')
    }
  } catch (err) {
    console.error('获取健身记录失败:', err)
    emitter.emit('notify', '获取数据失败: ' + (err.message || '未知错误'), 'error')
  }
}

onMounted(async () => {
  await initSelectOptions()
  await fetchRecords()
  
  // 监听刷新数据事件
  emitter.on('refresh-data', async () => {
    await initSelectOptions()
    await fetchRecords()
  })
})

// 在组件销毁时移除事件监听
onUnmounted(() => {
  emitter.off('refresh-data')
})

function getTodayDate() {
  const now = new Date()
  return now.toISOString().slice(0, 10)
}

async function addRecord() {
  if (!form.count || form.count < 1 || !form.finishTime) return
  adding.value = true
  try {
    const res = await axios.post('/api/fitness-record/add', {
      typeId: form.typeId,
      count: form.count,
      unitId: form.unitId,
      finishTime: form.finishTime + 'T00:00:00',
      remark: form.remark
    })
    if (res.data?.success) {
      await fetchRecords()
      form.type = types.value[0]
      form.unit = units.value[0]
      form.count = 1
      form.finishTime = getTodayDate()
      form.remark = ''
      emitter.emit('notify', '添加成功', 'success')
    }
  } finally {
    adding.value = false
  }
}

function handleAddRecord() {
  addRecord().then(() => {
    showAddModal.value = false
  })
}

function editRecord(idx) {
  editingIdx.value = idx
  const record = records.value[idx]
  editForm.typeId = record.typeId
  editForm.count = record.count
  editForm.unitId = record.unitId
  // finishTime 容错处理
  editForm.finishTime = record.finishTime ? record.finishTime.slice(0, 10) : getTodayDate()
  editForm.remark = record.remark || ''
}

async function saveEdit() {
  if (editingIdx.value !== null && editForm.finishTime) {
    const record = records.value[editingIdx.value]
    const payload = {
      id: record.id,
      typeId: editForm.typeId,
      count: editForm.count,
      unitId: editForm.unitId,
      finishTime: editForm.finishTime + 'T00:00:00',
      remark: editForm.remark
    }
    const res = await axios.put('/api/fitness-record/update', payload)
    if (res.data && res.data.success) {
      await fetchRecords()
      editingIdx.value = null
      emitter.emit('notify', '保存成功', 'success')
    }
  }
}

function cancelEdit() {
  editingIdx.value = null
}

async function deleteRecord(idx) {
  emitter.emit('confirm', {
    title: '删除确认',
    message: '确定要删除该记录吗？',
    onConfirm: async () => {
      const id = records.value[idx].id
      const res = await axios.delete(`/api/fitness-record/delete/${id}`)
      if (res.data && res.data.success) {
        await fetchRecords()
        emitter.emit('notify', '删除成功', 'success')
      }
    }
  })
}

function handleQuery() {
  fetchRecords()
}

function resetQuery() {
  query.typeId = ''
  query.startDate = ''
  query.endDate = ''
  query.remark = ''
  fetchRecords()
}

function handlePageChange(page) {
  fetchRecords(page, pageSize.value)
}

function handlePageSizeChange(size) {
  pageSize.value = size
  fetchRecords(1, size)
}
</script>
