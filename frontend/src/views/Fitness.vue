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
        <select v-model="editType" class="type-select">
          <option v-for="type in types" :key="type" :value="type">{{ type }}</option>
        </select>
        <input type="number" v-model.number="editCount" min="1" class="count-input" />
        <select v-model="editUnit" class="unit-select">
          <option v-for="u in units" :key="u" :value="u">{{ u }}</option>
        </select>
        <input type="date" v-model="editFinishDate" class="date-input" required />
        <button @click="saveEdit" class="add-btn">保存</button>
        <button @click="cancelEdit" class="delete-btn">取消</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { LucideClipboardList } from 'lucide-vue-next'

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
}

function deleteRecord(idx) {
  records.value.splice(idx, 1)
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
  }
}

function cancelEdit() {
  editingIdx.value = null
}
</script>

<style scoped>
.fitness-page {
  max-width: 600px;
  margin: 40px auto;
  padding: 32px 0 0 0;
}
.fitness-title {
  display: flex;
  align-items: center;
  font-size: 22px;
  font-weight: 500;
  color: #3a3a3a;
  margin-bottom: 18px;
  gap: 8px;
}
.title-icon {
  display: inline-block;
  vertical-align: middle;
}
.fitness-form {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  align-items: center;
}
.type-select {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  font-size: 16px;
}
.count-input, .unit-select, .date-input {
  width: 100px;
  padding: 6px 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  font-size: 16px;
}
.unit-select {
  width: 70px;
}
.date-input {
  width: 140px;
}
.add-btn {
  background: #4f8cff;
  color: #fff;
  border: none;
  border-radius: 6px;
  padding: 6px 18px;
  cursor: pointer;
  font-size: 16px;
  transition: background 0.2s;
}
.add-btn:hover {
  background: #2563eb;
}
.fitness-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.fitness-header {
  display: flex;
  align-items: center;
  gap: 16px;
  font-weight: 500;
  color: #888;
  border-bottom: 1px solid #eee;
  padding: 6px 0 2px 0;
  font-size: 15px;
  background: none;
}
.fitness-item {
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #f5f5f5;
  padding: 10px 0;
  font-size: 16px;
}
.item-type {
  width: 110px;
  min-width: 110px;
  max-width: 110px;
  flex-shrink: 0;
  text-align: left;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-count {
  width: 80px;
  text-align: center;
}
.item-unit {
  width: 70px;
  text-align: center;
}
.item-time {
  width: 140px;
  text-align: center;
  color: #888;
  font-size: 15px;
}
.item-action {
  width: 120px;
  text-align: left;
}
.center {
  text-align: center !important;
  justify-content: center;
  display: flex;
}
.edit-btn, .delete-btn {
  background: none;
  border: none;
  color: #4f8cff;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 15px;
  transition: background 0.2s, color 0.2s;
}
.edit-btn:hover {
  background: #e0e7ff;
  color: #2563eb;
}
.delete-btn {
  color: #ff7675;
}
.delete-btn:hover {
  background: #ffeaea;
  color: #d63031;
}
.edit-modal {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: #0005;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-content {
  background: #fff;
  border-radius: 10px;
  padding: 24px 32px;
  min-width: 320px;
  box-shadow: 0 2px 12px #0002;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
