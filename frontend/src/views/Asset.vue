<template>
  <div class="min-h-screen bg-gray-50 p-6 max-w-6xl mx-auto flex flex-col space-y-8 rounded-xl">
    <!-- 资产统计卡片 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <header class="flex justify-between items-center mb-6">
        <h2 class="text-xl font-semibold text-gray-900">
          {{ stats.formattedDate || '资产记录' }} 统计
        </h2>
        <button
            @click="refreshData"
            :class="{ 'animate-spin': retrying }"
            class="text-gray-500 hover:text-gray-900 transition"
            :title="retrying ? '正在重试...' : '刷新统计数据'"
        >
          <LucideRefreshCw class="w-6 h-6" />
        </button>
      </header>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="h-32 bg-gray-200 rounded-lg animate-pulse"></div>
        </template>
        <template v-else>
          <div
              class="p-4 rounded-lg border border-gray-200 hover:shadow-md transition"
          >
            <p class="text-sm text-gray-500">总资产</p>
            <p class="mt-2 text-3xl font-bold text-green-600">{{ formatAmount(stats.totalAssets) }}</p>
            <p :class="getChangeClass(stats.assetsChange)" class="mt-1 text-sm">
              {{ getChangePrefix(stats.assetsChange) }}{{ formatAmount(stats.assetsChange) }}
            </p>
          </div>

          <div
              class="p-4 rounded-lg border border-gray-200 hover:shadow-md transition"
          >
            <p class="text-sm text-gray-500">总负债</p>
            <p class="mt-2 text-3xl font-bold text-red-500">{{ formatAmount(stats.totalLiabilities) }}</p>
            <p :class="getChangeClass(stats.liabilitiesChange, true)" class="mt-1 text-sm">
              {{ getChangePrefix(stats.liabilitiesChange) }}{{ formatAmount(stats.liabilitiesChange) }}
            </p>
          </div>

          <div
              class="p-4 rounded-lg border border-gray-200 hover:shadow-md transition"
          >
            <p class="text-sm text-gray-500">净资产</p>
            <p
                class="mt-2 text-3xl font-bold"
                :class="netWorth > 0 ? 'text-green-600' : 'text-red-500'"
            >
              {{ formatAmount(netWorth) }}
            </p>
            <p :class="getChangeClass(netWorthChange)" class="mt-1 text-sm">
              {{ getChangePrefix(netWorthChange) }}{{ formatAmount(netWorthChange) }}
            </p>
          </div>
        </template>
      </div>

      <p v-if="retrying" class="text-xs text-gray-400 mt-2">
        正在重试 ({{ retryCount }}/{{ maxRetries }})...
      </p>
    </section>

    <!-- 搜索和操作 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
      <AssetSearch
          :query="query"
          :types="assetStore.types"
          :locations="assetStore.locations"
          :asset-names="assetStore.assetNames"
          @update:query="val => Object.assign(query, val)"
          @search="handleQuery"
          @reset="resetQuery"
          @refresh-names="refreshAssetNames"
          class="flex-grow"
      />

      <div class="flex items-center gap-3">
        <button
            @click="showModal"
            class="btn-primary rounded-full px-5 py-2 flex items-center justify-center space-x-2"
        >
          <LucidePlus class="w-5 h-5" />
          <span>添加记录</span>
        </button>
        <button
            @click="onCopyClick"
            class="btn-outline rounded-full px-5 py-2 flex items-center justify-center space-x-2"
        >
          <LucideCopy class="w-5 h-5" />
          <span>复制上回记录</span>
        </button>
      </div>
    </section>

    <!-- 记录列表 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <AssetList
          v-if="!loading"
          :records="records"
          :current="current"
          :total="total"
          :page-size="pageSize"
          @edit="editRecord"
          @delete="deleteRecord"
          @page-change="handlePageChange"
          @page-size-change="handlePageSizeChange"
      />
    </section>

    <!-- 添加/编辑弹窗 -->
    <AssetForm
        v-if="showAddModal"
        :show="showAddModal"
        :form="form"
        :asset-names="assetStore.assetNames"
        :types="assetStore.types"
        :units="assetStore.units"
        :locations="assetStore.locations"
        :loading="adding"
        title="添加记录"
        confirm-text="确定"
        remark-placeholder="备注（可选）"
        @submit="handleAddRecord"
        @cancel="cancelModal"
        @refresh-names="refreshAssetNames"
    />

    <AssetForm
        v-if="editingIdx !== null"
        :show="editingIdx !== null"
        :form="editForm"
        :asset-names="assetStore.assetNames"
        :types="assetStore.types"
        :units="assetStore.units"
        :locations="assetStore.locations"
        :loading="false"
        title="编辑记录"
        confirm-text="保存"
        remark-placeholder="备注"
        @submit="saveEdit"
        @cancel="cancelEdit"
        @refresh-names="refreshAssetNames"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { LucidePlus, LucideRefreshCw, LucideCopy } from 'lucide-vue-next'
import { useAssetStore } from '@/store/assetStore'
import emitter from '@/utils/eventBus'
import AssetList from '@/components/asset/AssetList.vue'
import AssetForm from '@/components/asset/AssetForm.vue'
import AssetSearch from '@/components/asset/AssetSearch.vue'
import axiosInstance from '@/utils/axios'

const assetStore = useAssetStore()

const loading = ref(false)
const adding = ref(false)
const showAddModal = ref(false)
const editingIdx = ref<number | null>(null)

const current = ref(1)
const pageSize = ref(10)
const total = ref(0)
const records = ref([])

const retrying = ref(false)
const retryCount = ref(0)
const maxRetries = 3

const stats = ref({
  totalAssets: 0,
  totalLiabilities: 0,
  assetsChange: 0,
  liabilitiesChange: 0,
  latestDate: '',
  formattedDate: ''
})

const netWorth = computed(() => stats.value.totalAssets - stats.value.totalLiabilities)
const netWorthChange = computed(() => stats.value.assetsChange - stats.value.liabilitiesChange)

const query = reactive({
  assetNameId: '',
  locationId: '',
  typeId: '',
  startDate: '',
  endDate: '',
  remark: ''
})

const form = reactive({
  assetNameId: '',
  assetTypeId: '',
  amount: '',
  unitId: '',
  assetLocationId: '',
  acquireTime: new Date().toISOString().slice(0, 10),
  remark: ''
})

const editForm = reactive({ ...form })

// 工具函数
function formatAmount(amount: number) {
  if (amount === null || amount === undefined || isNaN(amount)) return '0.00'
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(amount)
}
function getChangePrefix(change: number) {
  return change > 0 ? '+' : ''
}
function getChangeClass(change: number, isLiability = false) {
  if (isLiability) return change > 0 ? 'negative' : change < 0 ? 'positive' : ''
  return change > 0 ? 'positive' : change < 0 ? 'negative' : ''
}

// 请求和刷新逻辑
async function fetchStats() {
  try {
    const res = await axiosInstance.get('/api/asset-record/latest-stats')
    if (res.data?.success) {
      stats.value = res.data.data
      return true
    }
    throw new Error(res.data?.message || '获取资产统计失败')
  } catch (error) {
    emitter.emit('notify', error.message || '获取资产统计失败', 'error')
    return false
  }
}
async function tryFetchStats() {
  try {
    loading.value = true
    retrying.value = retryCount.value > 0
    const ok = await fetchStats()
    if (ok) {
      retryCount.value = 0
      retrying.value = false
      return true
    }
    throw new Error('统计数据获取失败')
  } catch {
    if (retryCount.value < maxRetries) {
      retryCount.value++
      await new Promise(r => setTimeout(r, 1000))
      return tryFetchStats()
    }
    retrying.value = false
    return false
  } finally {
    loading.value = false
  }
}
async function refreshData() {
  loading.value = true
  try {
    const [recordsRes] = await Promise.all([
      assetStore.fetchRecords({ ...query, page: current.value, pageSize: pageSize.value }),
      tryFetchStats()
    ])
    records.value = recordsRes.records || []
    total.value = recordsRes.total || 0
    if (total.value === 0) {
      emitter.emit('notify', { message: '未找到匹配的记录', type: 'info' })
    } else {
      emitter.emit('notify', { message: `查询到 ${total.value} 条记录`, type: 'success' })
    }
  } catch (error) {
    emitter.emit('notify', { message: '刷新失败：' + (error.message || '未知错误'), type: 'error' })
  } finally {
    loading.value = false
  }
}

async function handleQuery() {
  current.value = 1
  await refreshData()
}

function resetQuery() {
  Object.assign(query, {
    assetNameId: '',
    locationId: '',
    typeId: '',
    startDate: '',
    endDate: '',
    remark: ''
  })
  handleQuery()
}

function showModal() {
  showAddModal.value = true
}

function cancelModal() {
  showAddModal.value = false
  Object.assign(form, {
    assetNameId: '',
    assetTypeId: '',
    amount: '',
    unitId: '',
    assetLocationId: '',
    acquireTime: new Date().toISOString().slice(0, 10),
    remark: ''
  })
}

async function handleAddRecord(data: typeof form) {
  adding.value = true
  try {
    await assetStore.addRecord(data)
    showAddModal.value = false
    await refreshData()
  } finally {
    adding.value = false
  }
}

async function editRecord(idx: number) {
  editingIdx.value = idx
  Object.assign(editForm, records.value[idx])
}

async function saveEdit(data: typeof editForm) {
  if (editingIdx.value === null) return
  try {
    await assetStore.updateRecord({ id: records.value[editingIdx.value].id, ...data })
    editingIdx.value = null
    await refreshData()
  } catch (e) {
    emitter.emit('notify', '保存失败：' + e.message, 'error')
  }
}

function cancelEdit() {
  editingIdx.value = null
}

async function deleteRecord(idx: number) {
  const rec = records.value[idx]
  emitter.emit('confirm', {
    title: '删除确认',
    message: `确定删除该记录吗？此操作不可撤销。`,
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    async onConfirm() {
      try {
        await assetStore.deleteRecord(rec.id)
        await refreshData()
      } catch (e) {
        emitter.emit('notify', '删除失败：' + e.message, 'error')
      }
    }
  })
}

function onCopyClick() {
  emitter.emit('confirm', {
    title: '复制确认',
    message: '是否复制上回记录？',
    type: 'primary',
    confirmText: '复制',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await assetStore.copyLastRecords()
        await refreshData()
      } catch (e: any) {
        const msg = e.message || ''
        if (msg.includes('已有记录')) {
          emitter.emit('confirm', {
            title: '覆盖确认',
            message: '今日已有记录，是否强制覆盖？',
            confirmText: '覆盖',
            cancelText: '取消',
            type: 'primary',
            onConfirm: async () => {
              await assetStore.copyLastRecords(true)
              await refreshData()
            }
          })
        } else {
          emitter.emit('notify', `复制失败：${msg}`, 'error')
        }
      }
    }
  })
}

function handlePageChange(page: number) {
  current.value = page
  refreshData()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  current.value = 1
  refreshData()
}

async function refreshAssetNames() {
  await assetStore.fetchAssetNames()
}

// 初始化加载
onMounted(async () => {
  try {
    await Promise.all([
      assetStore.fetchTypes(),
      assetStore.fetchUnits(),
      assetStore.fetchLocations(),
      assetStore.fetchAssetNames(),
    ])
    await refreshData()
  } catch (e) {
    emitter.emit('notify', '加载失败，请刷新重试', 'error')
  }
})

watch(
    () => [assetStore.types, assetStore.units, assetStore.locations],
    ([types, units, locations]) => {
      if (Array.isArray(types) && types.length > 0 && !form.assetTypeId) {
        form.assetTypeId = types[0].id
      }
      if (Array.isArray(units) && units.length > 0 && !form.unitId) {
        form.unitId = units[0].id
      }
      if (Array.isArray(locations) && locations.length > 0 && !form.assetLocationId) {
        form.assetLocationId = locations[0].id
      }
    },
    { immediate: true, deep: true }
)
</script>

