<!--src/views/Fitness.vue-->
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
                {{
                  stats.proteinIntake >= 80
                      ? '✓'
                      : `差 ${80 - stats.proteinIntake} 克`
                }}
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
    <section
        class="bg-white rounded-xl hover:shadow-md p-6 space-y-4"
    >
      <!-- 第一行：按钮栏 -->
      <div class="flex justify-start">
        <button
            @click="handleAdd"
            class="btn-primary rounded-full px-5 py-2 flex items-center justify-center space-x-2"
        >
          <LucidePlus class="w-5 h-5"/>
          <span>添加记录</span>
        </button>
      </div>
      <!-- 第一行：搜索栏 -->
      <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <SearchPanel
            :query="query"
            :types="types"
            @update:query="val => Object.assign(query, val)"
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
          @edit="editRecord"
          @delete="handleDelete"
      />
      <div v-else class="space-y-4">
        <SkeletonCard v-for="n in pageSize" :key="n"/>
      </div>
    </section>

    <!-- 弹窗：添加 -->
    <FitnessModal
        v-if="showAddModal"
        :visible="showAddModal"
        :form="form"
        title="添加记录"
        confirm-text="确定"
        remark-placeholder="备注（可选）"
        @submit="handleAddRecord"
        @close="closeAddModal"
    />

    <!-- 弹窗：编辑 -->
    <FitnessModal
        v-if="editingIdx !== null"
        :visible="true"
        :form="form"
        :loading="false"
        title="编辑记录"
        confirm-text="保存"
        remark-placeholder="备注"
        @submit="saveEdit"
        @close="cancelEdit"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {LucidePlus, LucideRefreshCw} from 'lucide-vue-next'
import {storeToRefs} from 'pinia'
import {useFitnessStore} from '@/store/fitnessStore'
import {useMetaStore} from '@/store/metaStore'

import FitnessList from '@/components/fitness/FitnessList.vue'
import FitnessModal from '@/components/fitness/FitnessModal.vue'
import SearchPanel from '@/components/fitness/SearchPanel.vue'
import SkeletonCard from '@/components/base/SkeletonCard.vue'

const fitnessStore = useFitnessStore()
const metaStore = useMetaStore()

const {list, stats: statsRef} = storeToRefs(fitnessStore)
const query = ref<Record<string, any>>({})
const loading = ref(false)
const showAddModal = ref(false)
const editingIdx = ref<null | number>(null)
const pageSize = ref(10)
const types = ref<any[]>([])

const form = reactive({
  typeId: '',
  count: '1',
  unitId: '',
  finishTime: '',
  remark: ''
})

// 控制 meta 是否加载完成
const metaLoaded = ref(false)

// 日期格式化
function formatDate(dateStr: string | null | undefined) {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`
}

function initFormDefaults() {
  if (types.value.length === 0) return
  const now = new Date()
  now.setSeconds(0, 0)
  form.finishTime = now.toISOString().slice(0, 10)
  form.count = '1'
  form.remark = ''

  const first = types.value[0]
  form.typeId = String(first.id || '')

  if (first.key3 && metaStore.typeMap.UNIT) {
    const unit = metaStore.typeMap.UNIT.find((u: any) => u.key1 === first.key3)
    form.unitId = unit ? String(unit.id) : ''
  } else {
    form.unitId = ''
  }
}

// 监听 meta 加载
watch(
    () => metaStore.typeMap,
    (newVal) => {
      if (newVal?.FITNESS_TYPE?.length > 0) {
        types.value = newVal.FITNESS_TYPE
        initFormDefaults()
      }
    },
    {immediate: true}
)

// 编辑表单
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

// 统计卡片
const stats = computed(() => statsRef.value || {
  monthlyCount: 0,
  weeklyCount: 0,
  lastWorkoutDays: 0,
  nextWorkoutDay: '',
  proteinIntake: 0,
  carbsIntake: 0
})

const isWorkoutOverdue = computed(() => stats.value.lastWorkoutDays > 3)
const isNextWorkoutOverdue = computed(() => {
  const t = new Date(stats.value.nextWorkoutDay).getTime()
  return !isNaN(t) && t < Date.now()
})

// 主动刷新（初次加载 + 手动刷新）
async function refreshData() {
  loading.value = true
  try {
    await metaStore.initAll()
    await fitnessStore.loadStats()
    await fitnessStore.loadList()
    metaLoaded.value = true
  } catch (e) {
    console.error('刷新数据失败', e)
  } finally {
    loading.value = false
  }
}

// 打开/关闭弹窗
function handleAdd() {
  initFormDefaults()
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
}

// 添加记录
async function handleAddRecord(data: typeof form) {
  const payload = {...data, count: Number(data.count) || 0}
  await fitnessStore.addRecord(payload)
  showAddModal.value = false
  await refreshData()
}

// 编辑记录
function editRecord(recordId: number) {
  const idx = list.value?.findIndex(r => r.id === recordId)
  if (idx !== -1) editingIdx.value = idx
}

function cancelEdit() {
  editingIdx.value = null
}

// 保存编辑
async function saveEdit(data: typeof form) {
  if (editingIdx.value === null) return
  const payload = {...data, count: Number(data.count) || 0}
  await fitnessStore.updateRecord(payload)
  editingIdx.value = null
  await refreshData()
}

// 删除
async function handleDelete(id: number) {
  await fitnessStore.deleteRecord(id)
  await refreshData()
}

// 搜索、重置
async function handleQuery() {
  await refreshData()
}

function resetQuery() {
  query.value = {}
  refreshData()
}

onMounted(async () => {
  await metaStore.initAll()
  await fitnessStore.loadList()
})
</script>

