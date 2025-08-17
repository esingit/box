<template>
  <div class="min-h-screen bg-gray-50 p-6 w-full mx-auto flex flex-col space-y-6 rounded-xl">
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
              title="30天内运动次数"
              :amount="`${stats.monthlyCount} 次`"
              :change="`本周 ${stats.weeklyCount} 次`"
              change-class="text-gray-400"
          />
          <BaseStatCard
                title="上次运动"
                :amount="stats.lastWorkoutDays != null ? `${stats.lastWorkoutDays} 天前` : '未记录'"
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
    <section class="bg-white rounded-xl hover:shadow-md p-4 space-y-4">
      <div class="flex justify-start">
        <BaseButton title="添加记录" @click="handleAdd" color="primary" :icon="LucidePlus"/>
      </div>
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
        @submit="saveEdit"
        @close="cancelEdit"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { LucidePlus, LucideRefreshCw } from 'lucide-vue-next'
import { useFitnessStore } from '@/store/fitnessStore'
import { useMetaStore } from '@/store/metaStore'
import {clearCommonMetaCache} from '@/utils/commonMeta'
import emitter from '@/utils/eventBus'
import { formatDate } from '@/utils/formatters'

import type { FitnessFormData, FitnessRecord } from '@/types/fitness'
import BaseStatCard from '@/components/base/BaseStatCard.vue'
import FitnessList from '@/components/fitness/FitnessList.vue'
import FitnessForm from '@/components/fitness/FitnessForm.vue'
import FitnessSearch from '@/components/fitness/FitnessSearch.vue'

const fitnessStore = useFitnessStore()
const metaStore = useMetaStore()

// 解构响应式引用
const { query, pagination, list, stats } = storeToRefs(fitnessStore)

const loading = ref(false)
const showAddModal = ref(false)
const editingIdx = ref<null | number>(null)
const resultCount = ref<number | null>(null)

const fitnessTypeOptions = computed(() =>
    (metaStore.typeMap?.FITNESS_TYPE || []).map(i => ({
      label: i.value1 || '',
      value: i.id,
      id: i.id,
      key1: i.key1,
      key2: i.key2,
      key3: i.key3,
      value1: i.value1
    }))
)

const fitnessUnitOptions = computed(() =>
    (metaStore.typeMap?.UNIT || []).map(item => ({
      label: item.value1 || '',
      value: item.id
    }))
)

function handleQuery() {
  fitnessStore.setPageNo(1)
  refreshData(true) // 🔥 搜索时强制刷新
}

function resetQuery() {
  fitnessStore.resetQuery()
  fitnessStore.setPageNo(1)
  refreshData(true) // 🔥 重置搜索时强制刷新
}

// 🔥 修改：添加 force 参数，支持强制刷新
async function refreshData(force = true) {
  loading.value = true
  try {
    // 🔥 修改：传入 force 参数进行强制刷新
    await Promise.all([
      fitnessStore.loadStats(),
      fitnessStore.loadList(force)
    ])
    resultCount.value = fitnessStore.pagination.total
    emitter.emit('notify', {
      message: `成功查询出 ${resultCount.value} 条数据`,
      type: 'success'
    })
  } catch (e: any) {
    emitter.emit('notify', {
      message: e?.message || '刷新数据失败',
      type: 'error'
    })
  } finally {
    loading.value = false
  }
}

function handlePageChange(newPage: number) {
  fitnessStore.setPageNo(newPage)
  refreshData(true) // 🔥 翻页时强制刷新确保数据准确性
}

function handleAdd() {
  initEmptyForm()
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
}

async function handleAddRecord(data: FitnessFormData) {
  try {
    const payload = { ...data, count: Number(data.count) || 0 }
    await fitnessStore.addRecord(payload)
    showAddModal.value = false
    // 🔥 修改：添加后不需要再次刷新，因为 store 中的 addRecord 已经调用了 loadList(true)
    // 但为了更新统计数据，我们只刷新统计
    await fitnessStore.loadStats()
    resultCount.value = fitnessStore.pagination.total
  } catch (error) {
    // 错误处理已在 store 中完成
  }
}

function editRecord(recordId: number) {
  const idx = list.value.findIndex((r) => r.id === recordId)
  if (idx !== -1) {
    editingIdx.value = idx
    // 🔥 修改：编辑时填充表单数据
    const record = list.value[idx]
    form.typeId = String(record.typeId)
    form.count = String(record.count)
    form.unitId = String(record.unitId)
    form.finishTime = formatDate(record.finishTime)
    form.remark = record.remark || ''
  }
}

function cancelEdit() {
  editingIdx.value = null
}

async function saveEdit(data: FitnessFormData) {
  if (editingIdx.value === null) return

  try {
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
    // 🔥 修改：编辑后不需要再次刷新，因为 store 中的 updateRecord 已经调用了 loadList(true)
    // 但为了更新统计数据，我们只刷新统计
    await fitnessStore.loadStats()
    resultCount.value = fitnessStore.pagination.total
  } catch (error) {
    // 错误处理已在 store 中完成
  }
}

function handleDelete(record: FitnessRecord) {
  const dataInfo = `[${record.typeValue || '类型未知'},${record.count}${record.unitValue},${formatDate(record.finishTime)}]`
  emitter.emit('confirm', {
    title: '删除确认',
    message: `确定要删除${dataInfo}这条记录吗？此操作无法撤销。`,
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    async onConfirm() {
      try {
        await fitnessStore.deleteRecord(record.id)
        // 🔥 修改：删除后不需要再次刷新，因为 store 中的 deleteRecord 已经调用了 loadList(true)
        // 但为了更新统计数据，我们只刷新统计
        await fitnessStore.loadStats()
        resultCount.value = fitnessStore.pagination.total
      } catch (error) {
        // 错误处理已在 store 中完成
      }
    }
  })
}

function initEmptyForm() {
  const defaultType = fitnessTypeOptions.value.find(item => item.label === '俯卧撑')
  const defaultUnit = fitnessUnitOptions.value.find(item => item.label === '个')
  form.typeId = defaultType ? String(defaultType.value) : String(fitnessTypeOptions.value[0]?.value || '')
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
  // 🔥 修改：初始加载时强制刷新
  await refreshData(true)
})

onBeforeUnmount(() => {
  clearCommonMetaCache()
})
</script>