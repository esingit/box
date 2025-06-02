<template>
  <div class="fitness-page">
    <div class="fitness-title">
      <LucideClipboardList class="title-icon" color="#222" size="24" />
      <span>记录</span>
    </div>
    <form class="fitness-form" @submit.prevent="addRecord">
      <select v-model="selectedType" class="type-select">
        <option v-for="type in types" :key="type" :value="type">{{ type }}</option>
      </select>
      <input type="number" v-model.number="count" min="1" placeholder="数量" class="count-input" />
      <select v-model="unit" class="unit-select">
        <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
      </select>
      <input type="date" v-model="finishDate" class="date-input" required />
      <input type="text" v-model="addRemark" class="remark-input" placeholder="备注" />
      <button type="submit" class="add-btn">添加</button>
    </form>
    <ul class="fitness-list">
      <li class="fitness-header">
        <span class="item-type center">类型</span>
        <span class="item-count">数量</span>
        <span class="item-unit">单位</span>
        <span class="item-time">日期</span>
        <span class="item-remark">备注</span>
        <span class="item-action center">操作</span>
      </li>
      <li v-for="(record, idx) in records" :key="record.id" class="fitness-item">
        <span class="item-type">{{ record.type }}</span>
        <span class="item-count">{{ record.count }}</span>
        <span class="item-unit">{{ record.unit }}</span>
        <span class="item-time">{{ record.finish_date }}</span>
        <span class="item-remark">{{ record.remark }}</span>
        <span class="item-action">
          <button @click="editRecord(idx)" class="edit-btn">编辑</button>
          <button @click="deleteRecord(idx)" class="delete-btn">删除</button>
        </span>
      </li>
    </ul>
    <div v-if="editingIdx !== null" class="edit-modal">
      <div class="modal-content">
        <h3>编辑记录</h3>
        <select v-model="editType" class="type-select fitness-select">
          <option v-for="type in types" :key="type" :value="type">{{ type }}</option>
        </select>
        <input type="number" v-model.number="editCount" min="1" class="count-input fitness-input" />
        <select v-model="editUnit" class="unit-select fitness-select">
          <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
        </select>
        <input type="date" v-model="editFinishDate" class="date-input fitness-input" required />
        <input type="text" v-model="editRemark" class="remark-input fitness-input" placeholder="备注" />
        <div class="modal-actions">
          <button @click="saveEdit" class="add-btn">保存</button>
          <button @click="cancelEdit" class="delete-btn">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.fitness-list {
  margin: 24px 0 0 0;
  padding: 0;
  list-style: none;
}
.fitness-header {
  font-weight: bold;
  background: #f5f6fa;
  border-radius: 8px 8px 0 0;
  padding: 16px 0 16px 0;
  display: flex;
  gap: 8px;
  font-size: 16px;
  letter-spacing: 1px;
}
.fitness-item {
  display: flex;
  gap: 8px;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 18px 0 18px 0;
  font-size: 15px;
  transition: background 0.2s;
}
.fitness-item:hover {
  background: #f8faff;
}
.item-type, .item-count, .item-unit, .item-time, .item-remark, .item-action {
  flex: 1;
  text-align: center;
}
.item-action {
  flex: 1.5;
}
.fitness-title {
  display: flex;
  align-items: center;
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 24px;
  letter-spacing: 2px;
  gap: 10px;
}
</style>

<script setup>
import { ref, onMounted } from 'vue'
import { LucideClipboardList } from 'lucide-vue-next'
import emitter from '../utils/eventBus.js'
import axios from '../utils/axios.js'

const types = ref([])
const units = ref([])

const selectedType = ref('')
const count = ref(1)
const unit = ref('')
const finishDate = ref(getTodayDate())
const records = ref([])
let nextId = 1

const editingIdx = ref(null)
const editType = ref('')
const editCount = ref(1)
const editUnit = ref('')
const editFinishDate = ref('')
const editRemark = ref('')
const addRemark = ref('')

async function fetchRecords() {
  const res = await axios.get('/api/fitness-record/list')
  if (res.data && res.data.success) {
    records.value = res.data.data || []
  }
}

onMounted(async () => {
  // 获取类型
  const typeRes = await axios.get('/api/common-meta/key1-value1-by-type', { params: { typeCode: 'FITNESS_TYPE' } })
  if (typeRes.data && typeRes.data.success) {
    types.value = typeRes.data.data.map(item => item.value1)
    selectedType.value = types.value[0] || ''
    editType.value = types.value[0] || ''
  }
  // 获取单位
  const unitRes = await axios.get('/api/common-meta/key1-value1-by-type', { params: { typeCode: 'UNIT' } })
  if (unitRes.data && unitRes.data.success) {
    units.value = unitRes.data.data.map(item => item.value1)
    unit.value = units.value[0] || ''
    editUnit.value = units.value[0] || ''
  }
  await fetchRecords()
})

function getTodayDate() {
  const now = new Date()
  return now.toISOString().slice(0, 10)
}

async function addRecord() {
  if (!count.value || count.value < 1 || !finishDate.value) return
  const finishTime = finishDate.value + 'T00:00:00'
  const payload = {
    type: selectedType.value,
    count: count.value,
    unit: unit.value,
    finishTime,
    remark: addRemark.value
  }
  const res = await axios.post('/api/fitness-record/add', payload)
  if (res.data && res.data.success) {
    await fetchRecords()
    count.value = 1
    selectedType.value = types.value[0]
    unit.value = units.value[0]
    finishDate.value = getTodayDate()
    addRemark.value = ''
    emitter.emit('notify', '添加成功', 'success')
  }
}

function editRecord(idx) {
  editingIdx.value = idx
  editType.value = records.value[idx].type
  editCount.value = records.value[idx].count
  editUnit.value = records.value[idx].unit
  editFinishDate.value = records.value[idx].finishTime ? records.value[idx].finishTime.slice(0, 10) : ''
  editRemark.value = records.value[idx].remark || ''
}

async function saveEdit() {
  if (editingIdx.value !== null && editFinishDate.value) {
    const record = records.value[editingIdx.value]
    const finishTime = editFinishDate.value + 'T00:00:00'
    const payload = {
      id: record.id,
      type: editType.value,
      count: editCount.value,
      unit: editUnit.value,
      finishTime,
      remark: editRemark.value
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
