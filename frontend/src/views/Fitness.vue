<template>
  <div class="min-h-screen bg-gray-50 p-6 max-w-6xl mx-auto flex flex-col space-y-8">
    <!-- 统计卡片 -->
    <section class="bg-white rounded-xl shadow-md p-6">
      <header class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-900">健身记录统计</h2>
        <button
            @click="refreshData"
            class="text-gray-500 hover:text-gray-900 transition"
            title="刷新数据"
        >
          <LucideRefreshCw class="w-6 h-6" />
        </button>
      </header>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="h-32 bg-gray-200 rounded-lg animate-pulse" />
        </template>

        <template v-else>
          <div class="p-4 rounded-lg border border-gray-200">
            <p class="text-sm text-gray-500">本月运动</p>
            <p class="mt-2 text-3xl font-bold text-gray-900">{{ displayStats.monthlyCount }} 次</p>
            <p class="mt-1 text-sm text-gray-400">本周运动 {{ displayStats.weeklyCount }} 次</p>
          </div>

          <div class="p-4 rounded-lg border border-gray-200">
            <p class="text-sm text-gray-500">上次运动</p>
            <p
                class="mt-2 text-3xl font-bold"
                :class="{ 'text-red-500': isWorkoutOverdue, 'text-gray-900': !isWorkoutOverdue }"
            >
              {{ displayStats.lastWorkoutDays }} 天前
            </p>
            <p
                class="mt-1 text-sm"
                :class="isNextWorkoutOverdue ? 'text-red-500' : 'text-gray-400'"
            >
              下次运动日 {{ displayStats.nextWorkoutDay }}
            </p>
          </div>

          <div class="p-4 rounded-lg border border-gray-200">
            <p class="text-sm text-gray-500">今日蛋白</p>
            <p
                class="mt-2 text-3xl font-bold flex items-center"
                :class="displayStats.proteinIntake >= 80 ? 'text-green-600' : 'text-gray-900'"
            >
              {{ displayStats.proteinIntake }} 克
              <span
                  class="ml-3 text-sm font-normal"
                  :class="displayStats.proteinIntake >= 80 ? 'text-green-500' : 'text-yellow-500'"
              >
                {{
                  displayStats.proteinIntake >= 80
                      ? '✓'
                      : `差 ${80 - displayStats.proteinIntake} 克`
                }}
              </span>
            </p>
            <p class="mt-1 text-sm text-gray-400 flex items-center">
              今日碳水 {{ displayStats.carbsIntake }} 克
              <span
                  v-if="displayStats.carbsIntake < 120"
                  class="ml-2 text-yellow-500"
              >
                差 {{ 120 - displayStats.carbsIntake }} 克
              </span>
              <span v-else class="ml-2 text-green-500">✓</span>
            </p>
          </div>
        </template>
      </div>
    </section>

    <!-- 搜索和操作 -->
    <section
        class="bg-white rounded-xl shadow-md p-6 flex flex-col sm:flex-row sm:items-center sm:justify-between space-y-4 sm:space-y-0"
    >
      <SearchPanel
          :query="query"
          :types="types"
          @update:query="val => Object.assign(query, val)"
          @search="handleQuery"
          @reset="resetQuery"
          class="flex-grow"
      />
      <button
          @click="handleAdd"
          class="btn-primary"
      >
        <LucidePlus class="w-5 h-5 mr-2" />
        添加记录
      </button>
    </section>

    <!-- 记录列表 -->
    <section class="bg-white rounded-xl shadow-md p-6">
      <FitnessList
          v-if="!loading"
          :records="records"
          :current="current"
          :total="total"
          :page-size="pageSize"
          @edit="editRecord"
          @delete="handleDelete"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
      />
      <div v-else class="space-y-4">
        <SkeletonCard v-for="n in pageSize" :key="n" />
      </div>
    </section>

    <!-- 弹窗：添加 -->
    <FitnessModal
        v-if="showAddModal"
        :show="showAddModal"
        :form="form"
        :types="types"
        :units="units"
        :loading="adding"
        title="添加记录"
        confirm-text="确定"
        remark-placeholder="备注（可选）"
        @submit="handleAddRecord"
        @cancel="closeAddModal"
    />

    <!-- 弹窗：编辑 -->
    <FitnessModal
        v-if="editingIdx !== null"
        :show="editingIdx !== null"
        :form="editForm"
        :types="types"
        :units="units"
        :loading="false"
        title="编辑记录"
        confirm-text="保存"
        remark-placeholder="备注"
        @submit="saveEdit"
        @cancel="cancelEdit"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { LucideRefreshCw } from 'lucide-vue-next'
import { fitnessStore } from '@/store/fitnessStore'

const fitness = fitnessStore()

// 表单默认值常量
const DEFAULT_FORM = {
  typeId: '',
  count: 1,
  unitId: '',
  finishTime: '',
  remark: ''
}

// 状态
const stats = ref({
  monthlyCount: 0,
  weeklyCount: 0,
  lastWorkoutDays: 0,
  nextWorkoutDay: '-',
  proteinIntake: 0,
  carbsIntake: 0
})

const loading = ref(false)

async function refreshData() {
  loading.value = true
  try {
    const result = await fitness.fetchStats()
    if (result) stats.value = result
  } finally {
    loading.value = false
  }
}

// 状态逻辑封装
const isWorkoutOverdue = computed(() => stats.value.lastWorkoutDays > 3)
const isNextWorkoutOverdue = computed(() => {
  const next = new Date(stats.value.nextWorkoutDay).getTime()
  return !isNaN(next) && next < Date.now()
})

// 展示用数据，默认值处理
const displayStats = computed(() => ({
  monthlyCount: stats.value.monthlyCount || 0,
  weeklyCount: stats.value.weeklyCount || 0,
  lastWorkoutDays: stats.value.lastWorkoutDays || 0,
  nextWorkoutDay: stats.value.nextWorkoutDay || '-',
  proteinIntake: stats.value.proteinIntake || 0,
  carbsIntake: stats.value.carbsIntake || 0
}))

// 弹窗控制
const showAddModal = ref(false)
const editingIdx = ref<null | number>(null)

const form = ref({ ...DEFAULT_FORM })
const editForm = ref<typeof form.value | null>(null)

function handleAdd() {
  form.value = { ...DEFAULT_FORM }
  showAddModal.value = true
}

function closeAddModal() {
  showAddModal.value = false
}

function cancelEdit() {
  editingIdx.value = null
}

onMounted(() => {
  refreshData()
})
</script>
