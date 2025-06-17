<template>
  <div class="flex flex-col gap-6 p-6 bg-white text-gray-800 dark:bg-black dark:text-white">
    <!-- 统计信息卡片区域 -->
    <div class="w-full flex flex-col gap-4">
      <div class="flex items-center justify-between">
        <h2 class="text-lg font-medium" v-if="!loading && stats.formattedDate">
          {{ stats.formattedDate }} 资产记录统计
        </h2>
        <button
            class="hover:text-gray-500 dark:hover:text-gray-300 transition"
            :class="{ 'animate-spin': retrying }"
            @click="fetchStatsWithRetry"
            :title="retrying ? '正在重试...' : '刷新统计数据'"
        >
          <LucideRefreshCw class="w-5 h-5" />
        </button>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <template v-if="loading">
          <SkeletonCard v-for="i in 3" :key="i" class="h-[120px]" />
        </template>
        <template v-else>
          <div class="rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:shadow-md transition">
            <div class="text-sm text-gray-500">总资产</div>
            <div class="text-2xl font-semibold text-green-600">{{ formatAmount(stats.totalAssets) }}</div>
            <div :class="getChangeClass(stats.assetsChange)">
              {{ getChangePrefix(stats.assetsChange) }}{{ formatAmount(stats.assetsChange) }}
            </div>
          </div>

          <div class="rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:shadow-md transition">
            <div class="text-sm text-gray-500">总负债</div>
            <div class="text-2xl font-semibold text-red-500">{{ formatAmount(stats.totalLiabilities) }}</div>
            <div :class="getChangeClass(stats.liabilitiesChange, true)">
              {{ getChangePrefix(stats.liabilitiesChange) }}{{ formatAmount(stats.liabilitiesChange) }}
            </div>
          </div>

          <div class="rounded-xl border border-gray-200 dark:border-gray-700 p-4 hover:shadow-md transition">
            <div class="text-sm text-gray-500">净资产</div>
            <div class="text-2xl font-semibold" :class="netWorth > 0 ? 'text-green-600' : 'text-red-500'">
              {{ formatAmount(netWorth) }}
            </div>
            <div :class="getChangeClass(netWorthChange)">
              {{ getChangePrefix(netWorthChange) }}{{ formatAmount(netWorthChange) }}
            </div>
          </div>
        </template>
      </div>

      <div v-if="retrying" class="text-xs text-gray-400 mt-2">
        正在重试 ({{ retryCount }}/{{ maxRetries }})...
      </div>
    </div>

    <!-- 搜索栏和按钮操作区域 -->
    <div class="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
      <SearchPanel
          :query="query"
          :types="assetStore.types"
          :locations="assetStore.locations"
          :asset-names="assetStore.assetNames"
          @update:query="val => Object.assign(query, val)"
          @search="handleQuery"
          @reset="resetQuery"
          @refresh-names="refreshAssetNames"
      />

      <div class="flex items-center gap-2">
        <button class="px-4 py-2 rounded-lg bg-black text-white hover:opacity-90" @click="showModal">
          <LucidePlus class="w-4 h-4 inline mr-1" /> 添加记录
        </button>
        <button class="px-4 py-2 rounded-lg border border-gray-300 dark:border-gray-700 hover:bg-gray-100 dark:hover:bg-gray-800" @click="onCopyClick">
          <LucideCopy class="w-4 h-4 inline mr-1" /> 复制上回记录
        </button>
      </div>
    </div>

    <!-- 列表区域 -->
    <div>
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
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
        <SkeletonCard v-for="n in pageSize" :key="n" />
      </div>
    </div>

    <!-- 添加 / 编辑弹窗 -->
    <AssetModal
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
    <AssetModal
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

<script setup>
import {computed, onMounted, reactive, ref, watch} from 'vue'
import {LucideCopy, LucidePlus, LucideRefreshCw, LucideWallet} from 'lucide-vue-next'
import {useAssetStore} from '@/store/assetStore'
import {useAuth} from '@/composable/useAuth'
import emitter from '@/utils/eventBus.ts'
import AssetList from '@/components/asset/AssetList.vue'
import AssetModal from '@/components/asset/AssetModal.vue'
import SearchPanel from '@/components/asset/SearchPanel.vue'
import SkeletonCard from '@/components/base/SkeletonCard.vue'
import axiosInstance from '@/utils/axios';

// 外部依赖
const assetStore = useAssetStore()
const auth = useAuth()

// 图标
const WalletIcon = LucideWallet

// 状态
const showAddModal = ref(false)
const editingIdx = ref(null)
const adding = ref(false)
const loading = ref(false)
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)
const records = ref([])
const showCopyConfirm = ref(false)

// 统一的数据刷新函数
async function refreshData(options = {}) {
  const { showLoading = true, retryStats = false, refreshTable = true, refreshStats = true } = options
  
  try {
    if (showLoading) {
      loading.value = true
    }
    
    const promises = []
    
    // 根据需要添加相应的请求到并行任务中
    if (refreshTable) {
      promises.push(assetStore.fetchRecords({
        ...query,
        page: current.value,
        pageSize: pageSize.value
      }))
    }
    
    if (refreshStats) {
      promises.push(retryStats ? fetchStatsWithRetry() : fetchStats())
    }
    
    // 并行执行请求
    const results = await Promise.all(promises)
    
    // 处理表格数据结果
    if (refreshTable && results[0]) {
      const recordsResult = results[0]
      records.value = recordsResult.records || []
      total.value = recordsResult.total || 0
      return { success: true, total: total.value }
    }
    return { success: false }
    
  } catch (error) {
    console.error('获取数据失败：', error)
    emitter.emit('notify', '获取数据失败', 'error')
    return { success: false, error }
  } finally {
    if (showLoading) {
      loading.value = false
    }
  }
}

// 资产统计数据
const stats = ref({
  totalAssets: 0,
  totalLiabilities: 0,
  assetsChange: 0,
  liabilitiesChange: 0,
  latestDate: '',
  formattedDate: ''
})

// 计算净资产和净值变化
const netWorth = computed(() => stats.value.totalAssets - stats.value.totalLiabilities)
const netWorthChange = computed(() => stats.value.assetsChange - stats.value.liabilitiesChange)

// 查询条件
const query = reactive({
  assetNameId: '',
  locationId: '',
  typeId: '',
  startDate: '',
  endDate: '',
  remark: ''
})

// 表单数据
const form = reactive({
  assetNameId: '',
  assetTypeId: '',
  amount: '',
  unitId: '',
  assetLocationId: '',
  acquireTime: new Date().toISOString().split('T')[0],
  remark: ''
})

const editForm = reactive({...form})

// 重试相关状态
const retrying = ref(false)
const retryCount = ref(0)
const maxRetries = 3
const retryDelay = 1000 // 1秒后重试

// 延迟函数
function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// 带重试的获取资产统计
async function fetchStatsWithRetry() {
  retryCount.value = 0
  return tryFetchStats()
}

// 重试逻辑
async function tryFetchStats() {
  try {
    loading.value = true
    retrying.value = retryCount.value > 0
    const res = await axiosInstance.get('/api/asset-record/latest-stats')
    if (res.data?.success) {
      stats.value = res.data.data
      retryCount.value = 0
      retrying.value = false
      return true
    } else {
      throw new Error(res.data?.message || '获取资产统计失败')
    }
  } catch (error) {
    if (error.name === 'CanceledError') {
      console.log('请求已取消:', error.message)
      return false
    }

    console.error('获取资产统计失败:', error)
    emitter.emit('notify', error.message || '获取资产统计失败', 'error')
    return false
  } finally {
    if (retryCount.value === 0) {
      loading.value = false
      retrying.value = false
    }
  }
}

// 获取资产统计
async function fetchStats() {
  try {
    const res = await axiosInstance.get('/api/asset-record/latest-stats')
    if (res.data?.success) {
      stats.value = res.data.data
      return true
    } else {
      console.error('获取资产统计失败:', res.data)
      emitter.emit('notify', res.data?.message || '获取资产统计失败', 'error')
      return false
    }
  } catch (error) {
    if (error.name === 'CanceledError') {
      console.log('请求已取消:', error.message)
      return false
    }
    console.error('获取资产统计失败:', error)
    emitter.emit('notify', error.message || '获取资产统计失败', 'error')
    return false
  }
}

// 方法
async function handleQuery() {
  loading.value = true
  try {
    await refreshData({ refreshStats: false })
    if (Number(total.value) === 0) {
      emitter.emit('notify', { message: '未找到匹配的记录', type: 'info' })
    } else {
      emitter.emit('notify', { message: `查询到 ${total.value} 条记录`, type: 'success' })
    }
  } catch (error) {
    emitter.emit('notify', { message: '查询失败：' + (error.message || '未知错误'), type: 'error' })
  } finally {
    loading.value = false
  }
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
  const financeType = assetStore.types.find(type => type.key1 === 'FINANCE')
  const cnyUnit = assetStore.units.find(unit => unit.key1 === 'CNY')
  Object.assign(form, {
    assetNameId: '',
    assetTypeId: financeType?.id || assetStore.types[0]?.id || '',
    amount: '',
    unitId: cnyUnit?.id || assetStore.units[0]?.id || '',
    assetLocationId: assetStore.locations[0]?.id || '',
    acquireTime: new Date().toISOString().split('T')[0],
    remark: ''
  })
}

function formatAmount(amount) {
  if (amount === null || amount === undefined || isNaN(amount)) {
    return '0.00'
  }
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

function getChangePrefix(change) {
  return change > 0 ? '+' : ''
}

// 根据变化值获取样式类
function getChangeClass(change, isLiability = false) {
  if (isLiability) {
    return change > 0 ? 'negative' : change < 0 ? 'positive' : ''
  }
  return change > 0 ? 'positive' : change < 0 ? 'negative' : ''
}

async function handleAddRecord(formData) {
  try {
    adding.value = true
    await assetStore.addRecord(formData)
    showAddModal.value = false
    await refreshData({ retryStats: true })
  } catch (error) {
    console.error('添加记录失败：', error)
  } finally {
    adding.value = false
  }
}

async function editRecord(idx) {
  editingIdx.value = idx
  Object.assign(editForm, records.value[idx])
}

async function saveEdit(formData) {
  try {
    await assetStore.updateRecord({
      id: records.value[editingIdx.value].id,
      ...formData
    })
    editingIdx.value = null
    await refreshData({ retryStats: true })
  } catch (error) {
    console.error('更新记录失败：', error)
  }
}

function cancelEdit() {
  editingIdx.value = null
}

async function deleteRecord(idx) {
  const record = records.value[idx]
  try {
    await assetStore.deleteRecord(record.id)
    await refreshData({ retryStats: true })
  } catch (error) {
    console.error('删除记录失败：', error)
  }
}

async function copyLastRecords(force = false) {
  try {
    await assetStore.copyLastRecords(force)
    await refreshData({ retryStats: true })
  } catch (error) {
    // 错误处理逻辑在下方onCopyClick中实现
  }
}

function onCopyClick() {
  emitter.emit('confirm', {
    title: '复制确认',
    message: '是否复制上回记录？',
    confirmText: '复制',
    cancelText: '取消',
    type: 'primary',
    onConfirm: () => {
      assetStore.copyLastRecords().then(() => {
        refreshData({ retryStats: true })
      }).catch(error => {
        const msg = error?.message || ''
        if (msg.includes('今日已有记录') || msg.includes('强制模式')) {
          emitter.emit('confirm', {
            title: '覆盖确认',
            message: '今日已有记录，是否强制复制并覆盖今日所有记录？',
            confirmText: '覆盖',
            cancelText: '取消',
            type: 'primary',
            onConfirm: () => copyLastRecords(true)
          })
        } else if (msg.includes('没有找到可以复制的历史记录')) {
          emitter.emit('notify', '没有可复制的历史记录', 'info')
        } else {
          emitter.emit('notify', `复制失败: ${msg || '未知错误'}`, 'error')
        }
      })
    }
  })
}

function handlePageChange(page) {
  current.value = page
  refreshData()
}

function handlePageSizeChange(size) {
  pageSize.value = size
  current.value = 1
  refreshData()
}

async function refreshAssetNames() {
  await assetStore.fetchAssetNames()
}

// 监听元数据变化，设置默认值
watch([() => assetStore.types, () => assetStore.units, () => assetStore.locations], ([newTypes, newUnits, newLocations]) => {
  if (newTypes?.length > 0 && !form.assetTypeId) {
    // 默认选择理财类型
    const financeType = newTypes.find(type => type.key1 === 'FINANCE')
    form.assetTypeId = financeType?.id || newTypes[0].id
  }
  if (newUnits?.length > 0 && !form.unitId) {
    // 默认选择人民币单位
    const cnyUnit = newUnits.find(unit => unit.key1 === 'CNY')
    form.unitId = cnyUnit?.id || newUnits[0].id
  }
  if (newLocations?.length > 0 && !form.assetLocationId) {
    form.assetLocationId = newLocations[0].id
  }
}, { immediate: true })

// 生命周期钩子
onMounted(async () => {
  try {
    await Promise.all([
      assetStore.fetchTypes(),
      assetStore.fetchUnits(),
      assetStore.fetchLocations(),
      assetStore.fetchAssetNames(),
    ])
    console.log('类型列表:', assetStore.types)
    await refreshData({ retryStats: true })
  } catch (error) {
    if (error.name === 'CanceledError') {
      console.log('请求已取消:', error.message)
      return
    }
    console.error('初始化数据失败:', error)
    emitter.emit('notify', '加载数据失败：' + (error.message || '请刷新页面重试'), 'error')
  }
})
</script>


