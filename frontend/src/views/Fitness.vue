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
      <button type="submit" class="add-btn">添加</button>
    </form>
    <ul class="fitness-list">
      <li class="fitness-header">
        <span class="item-type center">类型</span>
        <span class="item-count">数量</span>
        <span class="item-unit">单位</span>
        <span class="item-time">日期</span>
        <span class="item-action center">操作</span>
      </li>
      <li v-for="(record, idx) in records" :key="record.id" class="fitness-item">
        <span class="item-type">{{ record.type }}</span>
        <span class="item-count">{{ record.count }}</span>
        <span class="item-unit">{{ record.unit }}</span>
        <span class="item-time">{{ record.finish_date }}</span>
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
        <div class="modal-actions">
          <button @click="saveEdit" class="add-btn">保存</button>
          <button @click="cancelEdit" class="delete-btn">取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { LucideClipboardList } from 'lucide-vue-next'
import mitt from '../utils/eventBus'

const types = [
  '俯卧撑',
  '仰卧起坐',
  '深蹲',
  '引体向上',
  '跑步',
]
const units = ['个', '米', '小时']
const selectedType = ref(types[0])
const count = ref(1)
const unit = ref(units[0])
const finishDate = ref(getTodayDate())
const records = ref([])
let nextId = 1

const editingIdx = ref(null)
const editType = ref('')
const editCount = ref(1)
const editUnit = ref(units[0])
const editFinishDate = ref('')

function getTodayDate() {
  const now = new Date()
  return now.toISOString().slice(0, 10)
}

function addRecord() {
  if (!count.value || count.value < 1 || !finishDate.value) return
  records.value.push({
    id: nextId++,
    type: selectedType.value,
    count: count.value,
    unit: unit.value,
    finish_date: finishDate.value,
  })
  count.value = 1
  selectedType.value = types[0]
  unit.value = units[0]
  finishDate.value = getTodayDate()
  mitt.emit('notify', '添加成功', 'success')
}

function deleteRecord(idx) {
  records.value.splice(idx, 1)
  mitt.emit('notify', '删除成功', 'success')
}

function editRecord(idx) {
  editingIdx.value = idx
  editType.value = records.value[idx].type
  editCount.value = records.value[idx].count
  editUnit.value = records.value[idx].unit
  editFinishDate.value = records.value[idx].finish_date
}

function saveEdit() {
  if (editingIdx.value !== null && editFinishDate.value) {
    records.value[editingIdx.value].type = editType.value
    records.value[editingIdx.value].count = editCount.value
    records.value[editingIdx.value].unit = editUnit.value
    records.value[editingIdx.value].finish_date = editFinishDate.value
    editingIdx.value = null
    mitt.emit('notify', '保存成功', 'success')
  }
}

function cancelEdit() {
  editingIdx.value = null
}
</script>
