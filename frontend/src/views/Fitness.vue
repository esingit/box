<template>
  <div class="min-h-screen bg-gray-50 p-6 max-w-6xl mx-auto flex flex-col space-y-8 rounded-xl">
    <!-- 统计卡片 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <header class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-900">健身记录统计</h2>
        <button
            @click="refreshData"
            class="text-gray-500 hover:text-gray-900 transition"
            title="刷新数据"
        >
          <LucideRefreshCw class="w-6 h-6"/>
        </button>
      </header>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="h-32 bg-gray-200 rounded-lg animate-pulse"/>
        </template>

        <template v-else>
          <div class="p-4 rounded-lg border border-gray-200 hover:shadow-md">
            <p class="text-sm text-gray-500">本月运动</p>
            <p class="mt-2 text-3xl font-bold text-gray-900">{{ stats.monthlyCount }} 次</p>
            <p class="mt-1 text-sm text-gray-400">本周运动 {{ stats.weeklyCount }} 次</p>
          </div>

          <div class="p-4 rounded-lg border border-gray-200 hover:shadow-md">
            <p class="text-sm text-gray-500">上次运动</p>
            <p
                class="mt-2 text-3xl font-bold"
                :class="{ 'text-red-500': isWorkoutOverdue, 'text-gray-900': !isWorkoutOverdue }"
            >
              {{ stats.lastWorkoutDays }} 天前
            </p>
            <p
                class="mt-1 text-sm"
                :class="isNextWorkoutOverdue ? 'text-red-500' : 'text-gray-400'"
            >
              下次运动日 {{ formatDate(stats.nextWorkoutDay) }}
            </p>
          </div>

          <div class="p-4 rounded-lg border border-gray-200 hover:shadow-md">
            <p class="text-sm text-gray-500">今日蛋白</p>
            <p
                class="mt-2 text-3xl font-bold flex items-center"
                :class="stats.proteinIntake >= 80 ? 'text-green-600' : 'text-gray-900'"
            >
              {{ stats.proteinIntake }} 克
              <span
                  class="ml-3 text-sm font-normal"
                  :class="stats.proteinIntake >= 80 ? 'text-green-500' : 'text-yellow-500'"
              >
                {{ stats.proteinIntake >= 80 ? '✓' : `差 ${80 - stats.proteinIntake} 克` }}
              </span>
            </p>
            <p class="mt-1 text-sm text-gray-400 flex items-center">
              今日碳水 {{ stats.carbsIntake }} 克
              <span
                  v-if="stats.carbsIntake < 120"
                  class="ml-2 text-yellow-500"
              >
                差 {{ 120 - stats.carbsIntake }} 克
              </span>
              <span v-else class="ml-2 text-green-500">✓</span>
            </p>
          </div>
        </template>
      </div>
    </section>

    <!-- 搜索和操作 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6 space-y-4">
      <div class="flex justify-start">
        <button
            @click="handleAdd"
            class="btn-primary rounded-full px-5 py-2 flex items-center justify-center space-x-2"
        >
          <LucidePlus class="w-5 h-5" />
          <span>添加记录</span>
        </button>
      </div>
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <FitnessSearch
            :query="query"
            :fitnessTypeOptions="fitnessTypeOptions"
            @search="handleQuery"
            @reset="resetQuery"
            class="flex-grow"
        />
      </div>
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

    <!-- 添加和编辑弹窗 -->
    <FitnessForm
        v-if="showAddModal"
        :visible="showAddModal"
        :form="form"
        title="添加记录"
        confirm-text="确定"
        remark-placeholder="备注（可选）"
        @submit="handleAddRecord"
        @close="closeAddModal"
    />

    <FitnessForm
        v-if="editingIdx !== null"
        :visible="true"
        :form="form"
        :loading="false"
        title="编辑记录"
        confirm-text="保存"
        remark-placeholder="备注（可选）"
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

import FitnessList from '@/components/fitness/FitnessList.vue'
import FitnessForm from '@/components/fitness/FitnessForm.vue'
import FitnessSearch from '@/components/fitness/FitnessSearch.vue'

const fitnessStore = useFitnessStore()
const metaStore = useMetaStore()

const { list, stats, query, pagination } = storeToRefs(fitnessStore)

const loading = ref(false)
const showAddModal = ref(false)
const editingIdx = ref<null | number>(null)

const fitnessTypeOptions = computed(() =>
    (metaStore.typeMap?.FITNESS_TYPE || []).map(item => ({
      label: item.value1 || '',
      value: item.id
    }))
)

const form = reactive({
  typeId: '',
  count: '1',
  unitId: '',
  finishTime: '',
  remark: ''
})

function formatDate(dateStr: string | null | undefined) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${(d.getMonth() + 1)
      .toString()
      .padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`
}

function initFormDefaults() {
  const types = metaStore.typeMap?.FITNESS_TYPE || []
  if (types.length === 0) return

  const now = new Date()
  now.setSeconds(0, 0)
  form.finishTime = now.toISOString().slice(0, 10)
  form.count = '1'
  form.remark = ''

  const first = types[0]
  form.typeId = String(first.id) || ''

  if (first.key3 && metaStore.typeMap.UNIT) {
    const unit = metaStore.typeMap.UNIT.find((u: any) => u.key1 === first.key3)
    form.unitId = unit ? String(unit.id) : ''
  } else {
    form.unitId = ''
  }
}

watch(editingIdx, (idx) => {
  if (idx !== null && list.value?.[idx]) {
    const rec = list.value[idx]
    form.typeId = rec.typeId || ''
    form.count = rec.count?.toString() || '1'
    form.unitId = rec.unitId || ''
    form.finishTime = rec.finishTime || ''
    form.remark = rec.remark || ''
  } else {
    form.typeId = ''
    form.count = '1'
    form.unitId = ''
    form.finishTime = ''
    form.remark = ''
  }
})

const isWorkoutOverdue = computed(() => stats.value?.lastWorkoutDays > 3)
const isNextWorkoutOverdue = computed(() => {
  const t = new Date(stats.value?.nextWorkoutDay).getTime()
  return !isNaN(t) && t < Date.now()
})

async function refreshData() {
  loading.value = true
  try {
    await metaStore.initAll()
    await fitnessStore.loadStats()
    await fitnessStore.loadList()
  } catch (e) {
    console.error('刷新数据失败', e)
  } finally {
    loading.value = false
  }
}

function handleQuery(newQuery: Partial<typeof query.value>) {
  fitnessStore.updateQuery(newQuery)
  fitnessStore.setPageNo(1)
  refreshData()
}

function resetQuery() {
  fitnessStore.resetQuery()
  refreshData()
}

function handlePageChange(newPage: number) {
  fitnessStore.setPageNo(newPage)
  refreshData()
}

function handleAdd() {
  initFormDefaults()
  showAddModal.value = true
}
function closeAddModal() {
  showAddModal.value = false
}
async function handleAddRecord(data: typeof form) {
  const payload = { ...data, count: Number(data.count) || 0 }
  await fitnessStore.addRecord(payload)
  showAddModal.value = false
  await refreshData()
}

function editRecord(recordId: number) {
  const idx = list.value.findIndex((r) => r.id === recordId)
  if (idx !== -1) editingIdx.value = idx
}
function cancelEdit() {
  editingIdx.value = null
}
async function saveEdit(data: typeof form) {
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

// 这里是改造点，接收完整 record，拼接提示信息传递给确认弹窗
function handleDelete(record: any) {
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

onMounted(async () => {
  await metaStore.initAll()
  await refreshData()
})
</script>
