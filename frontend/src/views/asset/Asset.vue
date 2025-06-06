<template>
  <div class="page-container">
    <div class="page-title">
      <LucideWallet class="title-icon" color="#222" size="24" />
      <span>资产记录</span>
    </div>
    <div class="asset-overview">
      <div class="overview-title">今日资产总计</div>
      <div class="asset-statistics">
        <div class="stat-item">
          <div class="stat-label">总资产</div>
          <div class="stat-value positive">{{ formatAmount(totalAssets) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">总负债</div>
          <div class="stat-value negative">{{ formatAmount(totalLiabilities) }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">净资产</div>
          <div class="stat-value" :class="getNetWorthClass">{{ formatAmount(netWorth) }}</div>
        </div>
      </div>
    </div>
    <div class="asset-query-bar">
      <div class="query-fields">
        <select v-model="query.typeId">
          <option value="">全部类型</option>
          <option v-for="t in assetStore.types" :key="t.id" :value="t.id">{{ t.value1 }}</option>
        </select>
        <input type="date" v-model="query.startDate" />
        <span class="to-text">至</span>
        <input type="date" v-model="query.endDate" />
        <input v-model="query.remark" placeholder="备注关键词" type="text" />
      </div>
      <div class="query-btns">
        <button class="btn btn-white" title="查询" @click="handleQuery">
          <LucideSearch size="18" style="vertical-align: middle;" />
        </button>
        <button class="btn btn-gray" title="重置" @click="resetQuery">
          <LucideRotateCcw size="18" style="vertical-align: middle;" />
        </button>
      </div>
    </div>
    <div class="button-group">
      <button class="btn btn-black" @click="showModal">添加记录</button>
      <button class="btn btn-white" @click="copyLastRecords">复制上回记录</button>
    </div>
    <AssetModal
      :show="showAddModal"
      :form="form"
      :asset-names="assetStore.assetNames"
      :types="assetStore.types"
      :units="assetStore.units"
      :locations="assetStore.locations"
      :loading="adding"
      title="添加记录"
      confirmText="确定"
      remarkPlaceholder="备注（可选）"
      @submit="handleAddRecord"
      @cancel="cancelModal"
      @refresh-names="refreshAssetNames"
    />
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
    <AssetModal
      :show="editingIdx !== null"
      :form="editForm"
      :asset-names="assetStore.assetNames"
      :types="assetStore.types"
      :units="assetStore.units"
      :locations="assetStore.locations"
      :loading="false"
      title="编辑记录"
      confirmText="保存"
      remarkPlaceholder="备注"
      @submit="saveEdit"
      @cancel="cancelEdit"
      @refresh-names="refreshAssetNames"
    />
  </div>
</template>

<script setup>
import '@/assets/base.css'
import '@/assets/asset.css'
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { LucideWallet, LucideSearch, LucideRotateCcw } from 'lucide-vue-next'
import emitter from '@/utils/eventBus.js'
import axios from '@/utils/axios.js'
import { formatAssetRecord } from '@/utils/commonMeta'
import { useAssetStore } from '@/stores/assetStore'
import { useUserStore } from '@/stores/userStore'
import AssetModal from '@/components/asset/AssetModal.vue'
import AssetList from '@/components/asset/AssetList.vue'

const assetStore = useAssetStore()
const userStore = useUserStore()

const records = ref([])
const adding = ref(false)

// 计算总资产（非负债）
// 最近日期的资产统计
const totalAssets = ref(0)
const totalLiabilities = ref(0)

// 获取最近日期的资产统计
async function fetchLatestAssetStats() {
  try {
    const res = await axios.get('/api/asset-record/latest-stats')
    if (res.data?.success) {
      totalAssets.value = res.data.data.totalAssets || 0
      totalLiabilities.value = res.data.data.totalLiabilities || 0
    }
  } catch (err) {
    console.error('获取最近资产统计失败:', err)
    emitter.emit('notify', '获取资产统计失败：' + (err.message || '未知错误'), 'error')
  }
}

// 计算净资产
const netWorth = computed(() => {
  return totalAssets.value - totalLiabilities.value
})

// 根据净资产值确定显示的样式类
const getNetWorthClass = computed(() => {
  if (netWorth.value > 0) return 'positive'
  if (netWorth.value < 0) return 'negative'
  return 'neutral'
})

// 格式化金额显示
function formatAmount(amount) {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    minimumFractionDigits: 2
  }).format(amount)
}

// 确保用户已登录
async function checkAndInitData() {
  if (!userStore.token) {
    await userStore.autoLogin()
  }
  return assetStore.initData()
}  // 添加表单和编辑表单统一用 reactive 管理
const form = reactive({
  assetNameId: null,
  assetTypeId: null,
  amount: 0,
  unitId: null,
  assetLocationId: null,
  acquireTime: getTodayDate(),
  remark: ''
})

const editForm = reactive({
  assetNameId: null,
  assetTypeId: null,
  amount: 0,
  unitId: null,
  assetLocationId: null,
  acquireTime: getTodayDate(),
  remark: ''
})

const editingIdx = ref(null)
const showAddModal = ref(false)
const query = reactive({
  typeId: '',
  startDate: '',
  endDate: '',
  remark: ''
})
const current = ref(1)
const total = ref(0)
const pageSize = ref(7)

async function initSelectOptions() {
  await assetStore.initData()
  // 等待数据加载完成后再初始化表单默认值
  if (assetStore.types.length > 0) {
    form.assetTypeId = assetStore.types[0].id
    editForm.assetTypeId = assetStore.types[0].id
  }
  if (assetStore.units.length > 0) {
    form.unitId = assetStore.units[0].id
    editForm.unitId = assetStore.units[0].id
  }    if (assetStore.locations.length > 0) {
    form.assetLocationId = assetStore.locations[0].id
    editForm.assetLocationId = assetStore.locations[0].id
  }
  if (assetStore.assetNames.length > 0) {
    form.assetNameId = assetStore.assetNames[0].id
    editForm.assetNameId = assetStore.assetNames[0].id
  }
}

async function fetchRecords(page = 1, size = pageSize.value) {
  try {
    const params = {
      page,
      pageSize: size
    }
    if (query.typeId) params.typeId = query.typeId
    if (query.startDate) params.startDate = query.startDate
    if (query.endDate) params.endDate = query.endDate
    if (query.remark) params.remark = query.remark
    
    // 并行获取记录列表和最新统计
    const [listRes, statsRes] = await Promise.all([
      axios.get('/api/asset-record/list', { params }),
      axios.get('/api/asset-record/latest-stats')
    ])
    
    if (listRes.data?.success) {
      const rawRecords = (listRes.data.data?.records) || []
      // 格式化每条记录的类型和单位
      records.value = await Promise.all(rawRecords.map(record => formatAssetRecord(record)))
      total.value = listRes.data.data ? Number(listRes.data.data.total) : 0
      current.value = listRes.data.data ? Number(listRes.data.data.current) : 1
      pageSize.value = listRes.data.data ? Number(listRes.data.data.size) : pageSize.value
      
      // 更新资产统计
      if (statsRes.data?.success) {
        totalAssets.value = statsRes.data.data.totalAssets || 0
        totalLiabilities.value = statsRes.data.data.totalLiabilities || 0
      }
    } else {
      emitter.emit('notify', '获取数据失败: ' + (listRes.data?.message || '未知错误'), 'error')
    }
  } catch (err) {
    console.error('获取资产记录失败:', err)
    emitter.emit('notify', '获取数据失败: ' + (err.message || '未知错误'), 'error')
  }
}

onMounted(async () => {
  try {
    await checkAndInitData()
    await fetchRecords()
    
    // 监听刷新数据事件
    emitter.on('refresh-data', async () => {
      await checkAndInitData()
      await fetchRecords()
    })
  } catch (error) {
    console.error('初始化数据失败:', error)
    emitter.emit('notify', '初始化数据失败', 'error')
  }
})

// 在组件销毁时移除事件监听
onUnmounted(() => {
  emitter.off('refresh-data')
})

function getTodayDate() {
  const now = new Date()
  return now.toISOString().slice(0, 10)
}

async function addRecord() {
  // 表单验证
  if (!form.assetNameId) {
    emitter.emit('notify', '请选择资产名称', 'error')
    return
  }
  if (!form.assetTypeId) {
    emitter.emit('notify', '请选择资产分类', 'error')
    return
  }
  if (!form.amount || form.amount <= 0) {
    emitter.emit('notify', '请输入正确的金额', 'error')
    return
  }
  if (!form.unitId) {
    emitter.emit('notify', '请选择货币单位', 'error')
    return
  }
  if (!form.assetLocationId) {
    emitter.emit('notify', '请选择资产位置', 'error')
    return
  }
  if (!form.acquireTime) {
    emitter.emit('notify', '请选择日期', 'error')
    return
  }

  adding.value = true
  try {
    const res = await axios.post('/api/asset-record/add', {
      assetNameId: form.assetNameId,
      assetTypeId: form.assetTypeId,
      amount: form.amount,
      unitId: form.unitId,
      assetLocationId: form.assetLocationId,
      acquireTime: form.acquireTime + 'T00:00:00',
      remark: form.remark
    })
    if (res.data?.success) {
      await fetchRecords()
      // 重置表单时使用 store 中的第一个选项
      form.assetNameId = assetStore.assetNames[0]?.id
      form.typeId = assetStore.types[0]?.id
      form.unitId = assetStore.units[0]?.id
      form.locationId = assetStore.locations[0]?.id
      form.amount = 0
      form.acquireTime = getTodayDate()
      form.remark = ''
      emitter.emit('notify', '添加成功', 'success')
    } else {
      emitter.emit('notify', res.data?.message || '添加失败', 'error')
    }
  } catch (error) {
    console.error('添加资产记录失败:', error)
    emitter.emit('notify', error.message || '添加失败', 'error')
  } finally {
    adding.value = false
  }
}

function handleAddRecord() {
  addRecord().then(() => {
    showAddModal.value = false
  })
}

function editRecord(idx) {
  editingIdx.value = idx
  const record = records.value[idx]
  editForm.assetNameId = record.assetNameId
  editForm.assetTypeId = record.assetTypeId
  editForm.amount = record.amount
  editForm.unitId = record.unitId
  editForm.assetLocationId = record.assetLocationId
  editForm.acquireTime = record.acquireTime ? record.acquireTime.slice(0, 10) : getTodayDate()
  editForm.remark = record.remark || ''
}

async function saveEdit() {
  // 表单验证
  if (!editForm.assetNameId) {
    emitter.emit('notify', '请选择资产名称', 'error')
    return
  }
  if (!editForm.assetTypeId) {
    emitter.emit('notify', '请选择资产分类', 'error')
    return
  }
  if (!editForm.amount || editForm.amount <= 0) {
    emitter.emit('notify', '请输入正确的金额', 'error')
    return
  }
  if (!editForm.unitId) {
    emitter.emit('notify', '请选择货币单位', 'error')
    return
  }
  if (!editForm.assetLocationId) {
    emitter.emit('notify', '请选择资产位置', 'error')
    return
  }
  if (!editForm.acquireTime) {
    emitter.emit('notify', '请选择日期', 'error')
    return
  }
  
  if (editingIdx.value !== null) {
    const record = records.value[editingIdx.value]
    const payload = {
      id: record.id,
      assetNameId: editForm.assetNameId,
      assetTypeId: editForm.assetTypeId,
      amount: editForm.amount,
      unitId: editForm.unitId,
      assetLocationId: editForm.assetLocationId,
      acquireTime: editForm.acquireTime + 'T00:00:00',
      remark: editForm.remark
    }
    try {
      const res = await axios.put('/api/asset-record/update', payload)
      if (res.data && res.data.success) {
        await fetchRecords()
        editingIdx.value = null
        emitter.emit('notify', '保存成功', 'success')
      } else {
        emitter.emit('notify', res.data?.message || '保存失败', 'error')
      }
    } catch (error) {
      console.error('更新资产记录失败:', error)
      emitter.emit('notify', error.message || '保存失败', 'error')
    }
  }
}

function cancelEdit() {
  editingIdx.value = null
}

async function deleteRecord(idx) {
  emitter.emit('confirm', {
    title: '删除确认',
    message: '确定要删除该记录吗？',
    onConfirm: async () => {
      const id = records.value[idx].id
      const res = await axios.delete(`/api/asset-record/delete/${id}`)
      if (res.data && res.data.success) {
        await fetchRecords()
        emitter.emit('notify', '删除成功', 'success')
      }
    }
  })
}

function handleQuery() {
  fetchRecords()
}

function resetQuery() {
  query.typeId = ''
  query.startDate = ''
  query.endDate = ''
  query.remark = ''
  fetchRecords()
}

function handlePageChange(page) {
  fetchRecords(page, pageSize.value)
}

function handlePageSizeChange(size) {
  pageSize.value = size
  fetchRecords(1, size)
}

async function copyLastRecords() {
  emitter.emit('confirm', {
    title: '复制确认',
    message: '确定要复制上次的资产记录吗？',
    onConfirm: async () => {
      try {
        const res = await axios.post('/api/asset-record/copy-last')
        if (res.data?.success) {
          await fetchRecords()
          emitter.emit('notify', '复制上次记录成功', 'success')
        } else {
          // 如果是今日已有记录的情况
          if (res.data?.message?.includes('今日已有记录')) {
            emitter.emit('confirm', {
              title: '提示',
              message: '今日已有资产记录，是否仍要复制？',
              onConfirm: async () => {
                try {
                  const forceRes = await axios.post('/api/asset-record/copy-last?force=true')
                  if (forceRes.data?.success) {
                    await fetchRecords()
                    emitter.emit('notify', '复制上次记录成功', 'success')
                  } else {
                    emitter.emit('notify', forceRes.data?.message || '复制失败', 'error')
                  }
                } catch (err) {
                  console.error('强制复制记录失败:', err)
                  emitter.emit('notify', err.message || '复制失败', 'error')
                }
              }
            })
          } else {
            emitter.emit('notify', res.data?.message || '复制失败', 'error')
          }
        }
      } catch (error) {
        console.error('复制上次记录失败:', error)
        emitter.emit('notify', error.message || '复制失败', 'error')
      }
    }
  })
}

async function showModal() {
  try {
    console.log('开始打开模态框流程...')
    console.log('当前用户Token:', localStorage.getItem('token'))
    console.log('正在检查并初始化数据...')
    await checkAndInitData() // 确保数据已加载
    console.log('数据初始化完成，开始获取资产名称列表...')
    await assetStore.fetchAssetNames() // 刷新资产名称列表
    console.log('获取到的资产名称列表:', assetStore.assetNames)
    showAddModal.value = true
    console.log('模态框已打开')
  } catch (error) {
    console.error('打开模态框失败:', {
      error,
      message: error.message,
      response: error.response?.data,
      stack: error.stack
    })
    emitter.emit('notify', `加载数据失败: ${error.response?.data?.message || error.message}`, 'error')
    // 如果是 token 相关错误，尝试重新登录
    if (error.response?.status === 401 || 
        error.response?.status === 403 || 
        error.response?.data?.message?.toLowerCase().includes('token')) {
      console.log('检测到认证问题，尝试重新登录...')
      await userStore.autoLogin()
    }
  }
}

function cancelModal() {
  showAddModal.value = false
  // 重置表单
  Object.assign(form, {
    assetNameId: assetStore.assetNames[0]?.id || null,
    assetTypeId: assetStore.types[0]?.id || null,
    amount: 0,
    unitId: assetStore.units[0]?.id || null,
    assetLocationId: assetStore.locations[0]?.id || null,
    acquireTime: getTodayDate(),
    remark: ''
  })
}

// 添加刷新资产名称的处理函数
async function refreshAssetNames() {
  await assetStore.fetchAssetNames()
  await fetchRecords() // 同时刷新资产记录列表以显示更新后的名称
}
</script>


