<template>
  <div class="min-h-screen bg-gray-50 p-6 max-w-6xl mx-auto flex flex-col space-y-8 rounded-xl">
    <!-- 统计卡片 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <header class="flex justify-between items-center mb-3">
        <h2 class="text-xl font-semibold text-gray-800">健身统计</h2>
        <BaseButton type="button" @click="refreshData" color="outline" :icon="LucideRefreshCw" class="w-7 h-7"/>
      </header>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="h-32 bg-gray-200 rounded-lg animate-pulse" />
        </template>
        <template v-else>
          <BaseStatCard
              title="本月运动"
              :amount="`${stats.monthlyCount} 次`"
              :change="`本周 ${stats.weeklyCount} 次`"
              change-class="text-gray-400"
          />
          <BaseStatCard
              title="上次运动"
              :amount="`${stats.lastWorkoutDays} 天前`"
              :highlight-class="isWorkoutOverdue ? 'msg-error' : 'text-gray-800 '"
              :change="`下次 ${formatDate(stats.nextWorkoutDay)}`"
              :change-class="isNextWorkoutOverdue ? 'msg-error' : 'text-gray-400'"
          />
          <BaseStatCard
              title="今日蛋白"
              :amount="`${stats.proteinIntake} 克`"
              :highlight-class="stats.proteinIntake >= 80 ? 'text-green-600' : 'text-gray-800 '"
              :change="stats.proteinIntake >= 80 ? '✓' : `差 ${80 - stats.proteinIntake} 克`"
              :change-class="stats.proteinIntake >= 80 ? 'text-green-500' : 'text-yellow-500'"
          />
        </template>
      </div>
    </section>

    <!-- 搜索和操作 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6 space-y-4">
      <div class="flex justify-start">
        <BaseButton title="添加记录" @click="handleAdd" color="primary" :icon="LucidePlus"/>
      </div>
      <!-- 这里使用 v-model:query 实现双向绑定 -->
      <FitnessSearch
          v-model:query="query"
          :fitnessTypeOptions="fitnessTypeOptions"
          :resultCount="resultCount"
          @search="handleQuery"
          @reset="resetQuery"
          class="flex-grow"
      />
    </section>

    <!-- 记录列表 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <FitnessList
          v-if="!loading"
          :list="list"
          :pageNo="pagination.pageNo"
          :pageSize="pagination.pageSize"
          @edit="editRecord"
          @delete="handleDelete"
          @page-change="handlePageChange"
      />
    </section>

    <!-- 添加弹窗 -->
    <FitnessForm
        v-if="showAddModal"
        :visible="showAddModal"
        :form="form"
        title="添加记录"
        confirm-text="确定"
        remark-placeholder="备注（可选）"
        :fitnessTypeOptions="fitnessTypeOptions"
        :fitnessUnitOptions="fitnessUnitOptions"
        @submit="handleAddRecord"
        @close="closeAddModal"
    />
    <!-- 编辑弹窗 -->
    <FitnessForm
        v-if="editingIdx !== null"
        :visible="true"
        :form="form"
        :loading="false"
        title="编辑记录"
        confirm-text="保存"
        remark-placeholder="备注（可选）"
        :fitnessTypeOptions="fitnessTypeOptions"
        :fitnessUnitOptions="fitnessUnitOptions"
        @submit="saveEdit"
        @close="cancelEdit"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { LucidePlus, LucideRefreshCw } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { useFitnessStore } from '@/store/fitnessStore'
import { useMetaStore } from '@/store/metaStore'
import emitter from '@/utils/eventBus'
import { formatDate } from '@/utils/formatters'

import BaseStatCard from '@/components/base/BaseStatCard.vue'
import FitnessList from '@/components/fitness/FitnessList.vue'
import FitnessForm from '@/components/fitness/FitnessForm.vue'
import FitnessSearch from '@/components/fitness/FitnessSearch.vue'

const fitnessStore = useFitnessStore()
const metaStore = useMetaStore()
const { list, stats, query: storeQuery, pagination } = storeToRefs(fitnessStore)

const loading = ref(false)
const showAddModal = ref(false)
const editingIdx = ref<null | number>(null)
const resultCount = ref<number | null>(null)

// 类型选项
const fitnessTypeOptions = computed(() =>
    (metaStore.typeMap?.FITNESS_TYPE || []).map(item => ({
      label: item.value1 || '',
      value: item.id
    }))
)

// 单位选项
const fitnessUnitOptions = computed(() =>
    (metaStore.typeMap?.UNIT || []).map(item => ({
      label: item.value1 || '',
      value: item.id
    }))
)

// 初始化时，确保query.typeIdList是空数组（避免默认全选）
const query = reactive({
  typeIdList: Array.isArray(storeQuery.value.typeIdList) && storeQuery.value.typeIdList.length > 0
      ? [...storeQuery.value.typeIdList]
      : [],
  startDate: storeQuery.value.startDate || '',
  endDate: storeQuery.value.endDate || '',
  remark: storeQuery.value.remark || ''
})

// 保持 pinia 的 query 和本地 query 同步，防止页面切换后残留旧值
watch(query, (newVal) => {
  storeQuery.value.typeIdList = Array.isArray(newVal.typeIdList) ? [...newVal.typeIdList] : []
  storeQuery.value.startDate = newVal.startDate
  storeQuery.value.endDate = newVal.endDate
  storeQuery.value.remark = newVal.remark
}, { deep: true })

// 父组件接收子组件的 query 变更，合并更新 pinia 的 query 状态
function handleQuery(newQuery) {
  Object.assign(query, newQuery)
  fitnessStore.setPageNo(1)
  refreshData()
}

function resetQuery() {
  // 这里重置所有查询条件，并同步 Pinia
  query.typeIdList = []
  query.startDate = ''
  query.endDate = ''
  query.remark = ''
  fitnessStore.setPageNo(1)
  refreshData()
}

async function refreshData() {
  loading.value = true
  try {
    // 切换页面回来时，强制确保 typeIdList 不是全选状态
    if (!query.typeIdList.length) {
      storeQuery.value.typeIdList = []
    } else {
      storeQuery.value.typeIdList = [...query.typeIdList]
    }

    await Promise.all([fitnessStore.loadStats(), fitnessStore.loadList()])
    resultCount.value = fitnessStore.pagination.total
    emitter.emit('notify', { message: `成功查询出 ${resultCount.value} 条数据`, type: 'success' })
  } catch (e: any) {
    emitter.emit('notify', { message: e?.message || '刷新数据失败', type: 'error' })
  } finally {
    loading.value = false
  }
}

function handlePageChange(newPage) {
  fitnessStore.setPageNo(newPage)
  refreshData()
}

function handleAdd() {
  initEmptyForm()
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
}

async function handleAddRecord(data) {
  const payload = { ...data, count: Number(data.count) || 0 }
  await fitnessStore.addRecord(payload)
  showAddModal.value = false
  await refreshData()
}

function editRecord(recordId) {
  const idx = list.value.findIndex((r) => r.id === recordId)
  if (idx !== -1) editingIdx.value = idx
}

function cancelEdit() {
  editingIdx.value = null
}

async function saveEdit(data) {
  if (editingIdx.value === null) return
  const original = list.value[editingIdx.value]
  if (!original || !original.id) return
  const payload = {
    id: original.id,
    typeId: data.typeId,
    count: Number(data.count) || 0,
    unitId: data.unitId,
    finishTime: data.finishTime,
    remark: data.remark
  }
  await fitnessStore.updateRecord(payload)
  editingIdx.value = null
  await refreshData()
}

function handleDelete(record) {
  const dataInfo = `[${record.typeValue || '类型未知'},${record.count}${record.unitValue},${formatDate(record.finishTime)}]`
  emitter.emit('confirm', {
    title: '删除确认',
    message: `确定要删除${dataInfo}这条记录吗？此操作无法撤销。`,
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    async onConfirm() {
      await fitnessStore.deleteRecord(record.id)
      await refreshData()
    }
  })
}

function initEmptyForm() {
  // 默认类型“俯卧撑”
  const defaultType = fitnessTypeOptions.value.find(item => item.label === '俯卧撑')
  form.typeId = defaultType ? String(defaultType.value) : String(fitnessTypeOptions.value[0]?.value || '')

  // 默认单位“个”
  const defaultUnit = fitnessUnitOptions.value.find(item => item.label === '个')
  form.unitId = defaultUnit ? String(defaultUnit.value) : String(fitnessUnitOptions.value[0]?.value || '')

  form.count = '1'
  form.finishTime = formatDate(new Date())
  form.remark = ''
}

const form = reactive({
  typeId: '',
  count: '1',
  unitId: '',
  finishTime: '',
  remark: ''
})

const isWorkoutOverdue = computed(() => stats.value?.lastWorkoutDays > 3)
const isNextWorkoutOverdue = computed(() => {
  const t = new Date(stats.value?.nextWorkoutDay).getTime()
  return !isNaN(t) && t < Date.now()
})

onMounted(async () => {
  await metaStore.initAll()
  // 切换页面回来，重置typeIdList为空，避免全选
  query.typeIdList = []
  storeQuery.value.typeIdList = []
  await refreshData()
})
</script>
