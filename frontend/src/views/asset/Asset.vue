<template>
  <div class="page-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <PageHeader title="资产" :icon="WalletIcon" />
    </div>

    <!-- 资产概览卡片 -->
    <div class="stats-container">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">总资产</div>
          <div class="stat-value positive">{{ formatAmount(totalAssets) }}</div>
          <div class="stat-change" :class="assetsChange > 0 ? 'positive' : assetsChange < 0 ? 'negative' : ''">
            {{ getChangePrefix(assetsChange) }}{{ formatAmount(assetsChange) }}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">总负债</div>
          <div class="stat-value negative">{{ formatAmount(totalLiabilities) }}</div>
          <div class="stat-change" :class="liabilitiesChange > 0 ? 'negative' : liabilitiesChange < 0 ? 'positive' : ''">
            {{ getChangePrefix(liabilitiesChange) }}{{ formatAmount(liabilitiesChange) }}
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-label">净资产</div>
          <div class="stat-value" :class="netWorth > 0 ? 'positive' : 'negative'">{{ formatAmount(netWorth) }}</div>
          <div class="stat-change" :class="netWorthChange > 0 ? 'positive' : netWorthChange < 0 ? 'negative' : ''">
            {{ getChangePrefix(netWorthChange) }}{{ formatAmount(netWorthChange) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索面板 -->
    <SearchPanel
      :query="query"
      :types="assetStore.types"
      @update:query="val => Object.assign(query, val)"
      @search="handleQuery"
      @reset="resetQuery"
    />

    <!-- 操作按钮区域 -->
    <div class="action-bar">
      <button class="btn btn-primary" @click="showModal">添加记录</button>
      <button class="btn btn-outline" @click="copyLastRecords">复制上回记录</button>
    </div>

    <!-- 记录列表 -->
    <AssetList
      :records="records"
      :current="current"
      :total="total"
      :page-size="pageSize"
      @edit="editRecord"
      @delete="deleteRecord"
      @page-change="handlePageChange"
      @page-size-change="handlePageSizeChange"
    />

    <!-- 添加记录弹窗 -->
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
    
    <!-- 编辑记录弹窗 -->
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
import { ref, reactive, onMounted, computed } from 'vue'
import { LucideWallet, LucideSearch, LucideRotateCcw } from 'lucide-vue-next'
import { useAssetStore } from '@/stores/assetStore'
import { useAuth } from '@/composables/useAuth'
import emitter from '@/utils/eventBus.js'
import PageHeader from '@/components/common/PageHeader.vue'
import AssetList from '@/components/asset/AssetList.vue'
import AssetModal from '@/components/asset/AssetModal.vue'
import SearchPanel from '@/components/asset/SearchPanel.vue'

// 外部依赖
const assetStore = useAssetStore()
const auth = useAuth()
const isLoggedIn = computed(() => auth.isLoggedIn)

// 图标
const WalletIcon = LucideWallet

// 状态
const showAddModal = ref(false)
const editingIdx = ref(null)
const adding = ref(false)
const current = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 查询条件
const query = reactive({
  typeId: '',
  startDate: '',
  endDate: '',
  remark: ''
})

// 表单数据
const form = reactive({
  assetName: '',
  typeId: '',
  amount: '',
  unitId: '',
  locationId: '',
  acquireTime: new Date().toISOString().split('T')[0],
  remark: ''
})

const editForm = reactive({...form})

// 计算属性
const records = ref([])

const getLatestDateRecords = () => {
  if (!records.value.length) {
    console.log('没有记录数据')
    return []
  }
  
  // 1. 获取最新的登记日期
  const latestDate = records.value.reduce((latest, record) => {
    const recordDate = new Date(record.acquireTime)
    return !latest || recordDate > latest ? recordDate : latest
  }, null)

  if (!latestDate) {
    console.log('未找到最新日期')
    return []
  }

  // 2. 筛选最新日期的记录
  const latestDateStr = latestDate.toISOString().split('T')[0]
  const filteredRecords = records.value.filter(record => record.acquireTime.startsWith(latestDateStr))
  console.log('最新日期:', latestDateStr, '记录数:', filteredRecords.length)
  return filteredRecords
}

const groupRecordsByType = (records) => {
  console.log('开始按类型分组，记录数:', records.length)
  const groups = {}
  records.forEach(record => {
    if (!record.typeId) {
      console.log('记录缺少typeId:', record)
      return
    }
    if (!groups[record.typeId]) {
      const type = assetStore.types.find(t => t.id === record.typeId)
      console.log('类型信息:', record.typeId, type)
      groups[record.typeId] = {
        type,
        amount: 0
      }
    }
    const amount = Number(record.amount || 0)
    groups[record.typeId].amount += amount
    console.log('累加金额:', record.typeId, amount, '=>', groups[record.typeId].amount)
  })
  console.log('分组结果:', groups)
  return groups
}

const totalAssets = computed(() => {
  console.log('计算总资产...')
  console.log('当前records:', records.value)
  console.log('资产类型列表:', assetStore.types)
  
  const latestRecords = getLatestDateRecords()
  if (!latestRecords.length) return 0

  const groupedByType = groupRecordsByType(latestRecords)
  
  const total = Object.values(groupedByType).reduce((sum, group) => {
    const isDebt = group.type?.typeCode === 'ASSET_LOCATION' && group.type?.key1 === 'DEBT'
    const amount = !isDebt ? group.amount : 0
    console.log('资产计算:', group.type?.typeName, isDebt ? '负债' : '资产', group.amount, '=>', amount)
    return sum + amount
  }, 0)
  
  console.log('总资产计算结果:', total)
  return total
})

const totalLiabilities = computed(() => {
  console.log('计算总负债...')
  const latestRecords = getLatestDateRecords()
  if (!latestRecords.length) return 0

  const groupedByType = groupRecordsByType(latestRecords)

  const total = Math.abs(Object.values(groupedByType).reduce((sum, group) => {
    const isDebt = group.type?.typeCode === 'ASSET_LOCATION' && group.type?.key1 === 'DEBT'
    const amount = isDebt ? group.amount : 0
    console.log('负债计算:', group.type?.typeName, isDebt ? '负债' : '资产', group.amount, '=>', amount)
    return sum + amount
  }, 0))
  
  console.log('总负债计算结果:', total)
  return total
})

const netWorth = computed(() => {
  const assets = totalAssets.value
  const liabilities = totalLiabilities.value
  console.log('计算净资产:', assets, '-', liabilities, '=', assets - liabilities)
  return assets - liabilities
})

// 变化率计算
const calculateAmountsByDate = (dateStr, calculateDebt = false) => {
  const dayRecords = records.value.filter(r => r.acquireTime.startsWith(dateStr))
  const groupedByType = groupRecordsByType(dayRecords)
  
  return Object.values(groupedByType).reduce((sum, group) => {
    const isDebt = group.type?.typeCode === 'ASSET_LOCATION' && group.type?.key1 === 'DEBT'
    if (calculateDebt ? isDebt : !isDebt) {
      return sum + group.amount
    }
    return sum
  }, 0)
}

const assetsChange = computed(() => {
  const uniqueDates = [...new Set(records.value.map(r => r.acquireTime.split('T')[0]))]
    .sort((a, b) => new Date(b) - new Date(a))

  if (uniqueDates.length < 2) return 0

  const latestAssets = calculateAmountsByDate(uniqueDates[0], false)
  const previousAssets = calculateAmountsByDate(uniqueDates[1], false)

  return latestAssets - previousAssets
})

const liabilitiesChange = computed(() => {
  const uniqueDates = [...new Set(records.value.map(r => r.acquireTime.split('T')[0]))]
    .sort((a, b) => new Date(b) - new Date(a))

  if (uniqueDates.length < 2) return 0

  const latestLiabilities = calculateAmountsByDate(uniqueDates[0], true)
  const previousLiabilities = calculateAmountsByDate(uniqueDates[1], true)

  return latestLiabilities - previousLiabilities
})

const netWorthChange = computed(() => assetsChange.value - liabilitiesChange.value)

// 方法
async function handleQuery() {
  try {
    const result = await assetStore.fetchRecords({
      ...query,
      page: current.value,
      pageSize: pageSize.value
    })
    if (result) {
      console.log('获取到记录:', result.records)
      records.value = result.records || []
      total.value = result.total || 0
    }
  } catch (error) {
    console.error('获取记录失败：', error)
    emitter.emit('notify', '获取记录失败', 'error')
  }
}

function resetQuery() {
  Object.assign(query, {
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
    assetName: '',
    typeId: '',
    amount: '',
    unitId: '',
    locationId: '',
    acquireTime: new Date().toISOString().split('T')[0],
    remark: ''
  })
}

function formatAmount(amount) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(amount)
}

function getChangePrefix(change) {
  return change > 0 ? '+' : ''
}

async function handleAddRecord(formData) {
  try {
    adding.value = true
    await assetStore.addRecord(formData)
    showAddModal.value = false
    await handleQuery()
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
    await handleQuery()
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
    await handleQuery()
  } catch (error) {
    console.error('删除记录失败：', error)
  }
}

async function copyLastRecords() {
  try {
    await assetStore.copyLastRecords()
    await handleQuery()
  } catch (error) {
    console.error('复制上回记录失败：', error)
  }
}

function handlePageChange(page) {
  current.value = page
  handleQuery()
}

function handlePageSizeChange(size) {
  pageSize.value = size
  current.value = 1
  handleQuery()
}

async function refreshAssetNames() {
  await assetStore.fetchAssetNames()
}

// 生命周期钩子
onMounted(async () => {
  if (!isLoggedIn.value) {
    auth.showLogin('请先登录')
    return
  }
  
  try {
    await Promise.all([
      assetStore.fetchTypes(),
      assetStore.fetchUnits(),
      assetStore.fetchLocations(),
      assetStore.fetchAssetNames()
    ])
    console.log('类型列表:', assetStore.types)
    await handleQuery()
  } catch (error) {
    // 如果是取消的请求，不显示错误提示
    if (error.name === 'CanceledError') {
      console.log('请求已取消:', error.message)
      return
    }
    // 如果是未授权错误，重定向到登录
    if (error.response?.status === 401) {
      const { showLogin } = useAuth()
      showLogin('登录已过期，请重新登录')
      return
    }
    console.error('初始化数据失败:', error)
    emitter.emit('notify', '加载数据失败：' + (error.message || '请刷新页面重试'), 'error')
  }
})
</script>

