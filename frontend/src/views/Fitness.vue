<template>
  <div class="fitness-page">
    <div class="fitness-title">
      <LucideClipboardList class="title-icon" color="#222" size="24" />
      <span>记录</span>
    </div>
    <div class="fitness-query-bar">
      <div class="query-fields">
        <select v-model="query.type">
          <option value="">全部类型</option>
          <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
        </select>
        <input type="date" v-model="query.startDate" />
        <span class="to-text">至</span>
        <input type="date" v-model="query.endDate" />
        <input v-model="query.remark" placeholder="备注关键词" type="text" />
      </div>
      <div class="query-btns">
        <button class="btn" @click="handleQuery">查询</button>
        <button class="btn reset-btn" @click="resetQuery">重置</button>
      </div>
    </div>
    <button class="fitness-add-btn btn" @click="showAddModal = true">添加单据</button>
    <FitnessModal
      :show="showAddModal"
      :form="form"
      :types="types"
      :units="units"
      :loading="adding"
      title="添加单据"
      confirmText="确定"
      remarkPlaceholder="备注（可选）"
      @submit="handleAddRecord"
      @cancel="showAddModal = false"
    />
    <FitnessList
      :records="records"
      @edit="editRecord"
      @delete="deleteRecord"
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
import '../assets/base.css'
import '../assets/fitness.css'
import { ref, reactive, onMounted } from 'vue'
import { LucideClipboardList } from 'lucide-vue-next'
import emitter from '../utils/eventBus.js'
import axios from '../utils/axios.js'
import FitnessModal from '../components/FitnessModal.vue'
import FitnessList from '../components/FitnessList.vue'

const types = ref([])
const units = ref([])
const records = ref([])
const adding = ref(false)

// 添加表单和编辑表单统一用 reactive 管理
const form = reactive({
  type: '',
  count: 1,
  unit: '',
  finishTime: getTodayDate(),
  remark: ''
})
const editForm = reactive({
  type: '',
  count: 1,
  unit: '',
  finishTime: getTodayDate(),
  remark: ''
})
const editingIdx = ref(null)
const showAddModal = ref(false)
const query = reactive({
  type: '',
  startDate: '',
  endDate: '',
  remark: ''
})

async function initSelectOptions() {
  const [typeRes, unitRes] = await Promise.all([
    axios.get('/api/common-meta/key1-value1-by-type', { params: { typeCode: 'FITNESS_TYPE' } }),
    axios.get('/api/common-meta/key1-value1-by-type', { params: { typeCode: 'UNIT' } })
  ])
  if (typeRes.data?.success) {
    types.value = typeRes.data.data.map(item => item.value1)
    form.type = types.value[0] || ''
    editForm.type = types.value[0] || ''
  }
  if (unitRes.data?.success) {
    units.value = unitRes.data.data.map(item => item.value1)
    form.unit = units.value[0] || ''
    editForm.unit = units.value[0] || ''
  }
}

async function fetchRecords() {
  const params = {}
  if (query.type) params.type = query.type
  if (query.startDate) params.startDate = query.startDate
  if (query.endDate) params.endDate = query.endDate
  if (query.remark) params.remark = query.remark
  const res = await axios.get('/api/fitness-record/list', { params })
  if (res.data && res.data.success) {
    records.value = res.data.data || []
  }
}

onMounted(async () => {
  await initSelectOptions()
  await fetchRecords()
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
      type: form.type,
      count: form.count,
      unit: form.unit,
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
  editForm.type = record.type
  editForm.count = record.count
  editForm.unit = record.unit
  // finishTime 容错处理
  editForm.finishTime = record.finishTime ? record.finishTime.slice(0, 10) : getTodayDate()
  editForm.remark = record.remark || ''
}

async function saveEdit() {
  if (editingIdx.value !== null && editForm.finishTime) {
    const record = records.value[editingIdx.value]
    const payload = {
      id: record.id,
      type: editForm.type,
      count: editForm.count,
      unit: editForm.unit,
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
  query.type = ''
  query.startDate = ''
  query.endDate = ''
  query.remark = ''
  fetchRecords()
}
</script>
