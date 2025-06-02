<template>
  <div class="fitness-page">
    <div class="fitness-title">
      <LucideClipboardList class="title-icon" color="#222" size="24" />
      <span>记录</span>
    </div>
    <FitnessForm
      :form="form"
      :types="types"
      :units="units"
      :adding="adding"
      @submit="addRecord"
    />
    <FitnessList
      :records="records"
      @edit="editRecord"
      @delete="deleteRecord"
    />
    <FitnessEditModal
      :show="editingIdx !== null"
      :editForm="editForm"
      :types="types"
      :units="units"
      @save="saveEdit"
      @cancel="cancelEdit"
    />
  </div>
</template>

<script setup>
import '../assets/fitness.css'
import { ref, reactive, onMounted } from 'vue'
import { LucideClipboardList } from 'lucide-vue-next'
import emitter from '../utils/eventBus.js'
import axios from '../utils/axios.js'
import FitnessForm from '../components/FitnessForm.vue'
import FitnessList from '../components/FitnessList.vue'
import FitnessEditModal from '../components/FitnessEditModal.vue'

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
  const res = await axios.get('/api/fitness-record/list')
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
</script>
