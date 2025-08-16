<template>
  <div class="min-h-screen bg-gray-50 p-6 w-full mx-auto flex flex-col space-y-6 rounded-xl">
    <!-- 资产统计卡片 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <header class="flex justify-between items-center mb-3">
        <h2 class="text-xl font-semibold text-gray-800 ">
          {{ stats.formattedDate || '资产记录' }} 资产统计
        </h2>
        <BaseButton type="button" @click="refreshData" color="outline" :icon="LucideRefreshCw" class="w-7 h-7"/>
      </header>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <template v-if="loading">
          <div v-for="i in 3" :key="i" class="h-32 bg-gray-200 rounded-lg animate-pulse"></div>
        </template>
        <template v-else>
          <BaseStatCard
              title="净资产"
              :amount="formatAmount(stats.netAssets || 0)"
              :change="formatAmount(stats.netAssetsChange || 0)"
              :prefix="getChangePrefix(stats.netAssetsChange || 0)"
              :changeClass="getChangeClass(stats.netAssetsChange || 0)"
              highlightClass="msg-success"
          />
          <BaseStatCard
              title="理财资产"
              :amount="formatAmount(stats.investmentAssets || 0)"
              :change="formatAmount(stats.investmentAssetsChange || 0)"
              :prefix="getChangePrefix(stats.investmentAssetsChange || 0)"
              :changeClass="getChangeClass(stats.investmentAssetsChange || 0)"
              highlightClass="msg-info"
          />
          <BaseStatCard
              title="总负债"
              :amount="formatAmount(stats.totalLiabilities || 0)"
              :change="formatAmount(stats.liabilitiesChange || 0)"
              :prefix="getChangePrefix(stats.liabilitiesChange || 0)"
              :changeClass="getChangeClass(stats.liabilitiesChange || 0, true)"
              highlightClass="msg-error"
          />
        </template>
      </div>
    </section>

    <!-- 搜索和操作 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6 w-full space-y-4">
      <div class="flex justify-start gap-2">
        <BaseButton title="添加资产" @click="handleAdd" color="primary" :icon="LucidePlus"/>
        <BaseButton title="复制上回记录" @click="onCopyClick" color="outline" :icon="LucideCopy"/>
        <BaseButton title="扫图批量添加" @click="showAssetScanAddModal" color="outline" :icon="LucideScanText"/>
      </div>
      <AssetSearch
          :query="query"
          :assetNameOptions="assetNameOptions"
          :assetTypeOptions="assetTypeOptions"
          :assetLocationOptions="assetLocationOptions"
          :resultCount="resultCount"
          @search="handleQuery"
          @reset="resetQuery"
      />
    </section>

    <!-- 记录列表 -->
    <section class="bg-white rounded-xl hover:shadow-md p-6">
      <AssetList
          v-if="!loading"
          :list="list"
          :pageNo="pagination.pageNo"
          :pageSize="pagination.pageSize"
          @edit="editRecord"
          @delete="handleDelete"
          @page-change="handlePageChange"
      />
    </section>

    <!-- 弹窗 -->
    <AssetForm
        v-if="showAddModal"
        :visible="showAddModal"
        :form="form"
        title="添加记录"
        confirm-text="确定"
        @submit="handleAddRecord"
        @close="closeAddModal"
    />
    <AssetForm
        v-if="editingIdx !== null"
        :visible="true"
        :form="form"
        title="编辑记录"
        confirm-text="保存"
        @submit="saveEdit"
        @close="cancelEdit"
    />
    <AssetScanAddModal
        v-if="showAssetScanAddFlag"
        :visible="showAssetScanAddFlag"
        :form="form"
        title="扫图批量添加"
        confirm-text="确定"
        @submit="handleAssetScanAddRecord"
        @close="closeAssetScanAddModal"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LucideCopy, LucidePlus, LucideRefreshCw, LucideScanText } from 'lucide-vue-next'
import { storeToRefs } from 'pinia'
import { format } from 'date-fns'
import { useAssetStore } from '@/store/assetStore'
import { useAssetNameStore } from '@/store/assetNameStore'
import { useMetaStore } from '@/store/metaStore'
import emitter from '@/utils/eventBus'
import { formatAmount, getChangeClass, getChangePrefix, formatDate } from '@/utils/formatters'

import BaseButton from '@/components/base/BaseButton.vue'
import BaseStatCard from '@/components/base/BaseStatCard.vue'
import AssetList from '@/components/asset/AssetList.vue'
import AssetForm from '@/components/asset/AssetForm.vue'
import AssetSearch from '@/components/asset/AssetSearch.vue'
import AssetScanAddModal from '@/components/asset/AssetScanAddModal.vue'
import {clearCommonMetaCache} from "@/utils/commonMeta"
import {RawAssetRecord} from "@/types/asset"

const assetStore = useAssetStore()
const assetNameStore = useAssetNameStore()
const metaStore = useMetaStore()
const { list, stats, query, pagination } = storeToRefs(assetStore)

// route sync helpers
const route = useRoute()
const router = useRouter()

// 防止路由更新触发重复查询的标志
const isHandlingRouteUpdate = ref(false)

function toArrayParam(val: any): number[] {
  if (!val) return []
  const arr = Array.isArray(val) ? val : String(val).split(',')
  return arr
    .map((v: any) => {
      const n = Number(String(v).trim())
      return Number.isNaN(n) ? null : n
    })
    .filter((n: number | null): n is number => n !== null)
}

function parseRouteQuery(q: any): Partial<typeof query.value> {
  return {
    assetNameIdList: toArrayParam(q.assetNameIdList),
    assetTypeIdList: toArrayParam(q.assetTypeIdList),
    assetLocationIdList: toArrayParam(q.assetLocationIdList),
    startDate: q.startDate || '',
    endDate: q.endDate || '',
    remark: q.remark || ''
  }
}

function buildRouteQueryFromStore(): Record<string, any> {
  const out: Record<string, any> = {}
  if (query.value.assetNameIdList?.length) out.assetNameIdList = query.value.assetNameIdList
  if (query.value.assetTypeIdList?.length) out.assetTypeIdList = query.value.assetTypeIdList
  if (query.value.assetLocationIdList?.length) out.assetLocationIdList = query.value.assetLocationIdList
  if (query.value.startDate) out.startDate = query.value.startDate
  if (query.value.endDate) out.endDate = query.value.endDate
  if (query.value.remark) out.remark = query.value.remark
  return out
}

const loading = ref(false)
const showAddModal = ref(false)
const showAssetScanAddFlag = ref(false)
const editingIdx = ref<number | null>(null)
const resultCount = ref<number | null>(null)

const assetTypeOptions = computed(() =>
    (metaStore.typeMap?.ASSET_TYPE || []).map(i => ({
      label: i.value1 || '',
      value: i.id,
      id: i.id,
      key1: i.key1,
      key2: i.key2,
      key3: i.key3,
      value1: i.value1
    }))
)

const assetLocationOptions = computed(() =>
    (metaStore.typeMap?.ASSET_LOCATION || []).map(i => ({ label: i.value1 || '', value: i.id }))
)
const { assetNameOptions } = storeToRefs(assetNameStore)

function getDefaultTypeId() {
  const list = metaStore.typeMap?.ASSET_TYPE || []
  const item = list.find(i => i.value1 === '理财')
  return item?.id || list[0]?.id || ''
}

function getDefaultLocationId() {
  const list = metaStore.typeMap?.ASSET_LOCATION || []
  const item = list.find(i => i.value1 === '工商银行')
  return item?.id || list[0]?.id || ''
}

function getDefaultUnitId() {
  const list = metaStore.typeMap?.UNIT || []
  const item = list.find(i => i.value1 === '人民币')
  return item?.id || list[0]?.id || ''
}

const form = reactive({
  assetNameId: '',
  assetTypeId: '',
  assetLocationId: '',
  amount: '1',
  unitId: '',
  acquireTime: '',
  remark: ''
})

function initFormByRecord(rec: any) {
  Object.assign(form, {
    assetNameId: rec.assetNameId || '',
    assetTypeId: rec.assetTypeId || '',
    assetLocationId: rec.assetLocationId || '',
    amount: rec.amount?.toString() || '1',
    unitId: rec.unitId || '',
    acquireTime: rec.acquireTime || '',
    remark: rec.remark || ''
  })
}

function initEmptyForm() {
  Object.assign(form, {
    assetNameId: '',
    assetTypeId: getDefaultTypeId(),
    assetLocationId: getDefaultLocationId(),
    amount: '1',
    unitId: getDefaultUnitId(),
    acquireTime: format(new Date(), 'yyyy-MM-dd'),
    remark: ''
  })
}

watch(editingIdx, idx => {
  if (idx !== null && list.value?.[idx]) initFormByRecord(list.value[idx])
  else initEmptyForm()
})

watch(
    () => ({
      unit: metaStore.typeMap?.UNIT,
      type: metaStore.typeMap?.ASSET_TYPE,
      location: metaStore.typeMap?.ASSET_LOCATION
    }),
    ({ unit, type, location }) => {
      if (unit?.length && type?.length && location?.length && !form.unitId) {
        initEmptyForm()
      }
    },
    { immediate: true }
)

function notifyToast(message: string, type: 'success' | 'error' = 'success') {
  emitter.emit('notify', { message, type, duration: 3000 })
}

// 🔥 修改：添加 force 参数，支持强制刷新
async function refreshData(force = true) {
  loading.value = true
  try {
    // 🔥 修改：传入 force 参数进行强制刷新
    await Promise.all([
      assetStore.loadStats(),
      assetStore.loadList(force)
    ])
    resultCount.value = assetStore.pagination.total
    notifyToast(`成功查询出 ${resultCount.value} 条数据`)
  } catch (e: any) {
    notifyToast(e?.message || '刷新失败', 'error')
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  // 先重置 store 中的查询条件
  assetStore.resetQuery()
  
  // 设置标志，阻止 watch 响应这次路由更新
  isHandlingRouteUpdate.value = true
  
  // 清空路由 query（不会触发重复查询）
  router.replace({ query: {} }).finally(() => {
    // 确保标志被重置
    setTimeout(() => {
      isHandlingRouteUpdate.value = false
    }, 0)
  })
  
  // 刷新数据
  refreshData(true)
}

async function handleQuery(newQuery: Partial<typeof query.value>) {
  // 先更新 store 中的查询条件
  assetStore.updateQuery(newQuery)
  assetStore.setPageNo(1)
  
  // 加载新数据
  await refreshData(true)

  // 设置标志，阻止 watch 响应这次路由更新
  isHandlingRouteUpdate.value = true
  
  try {
    // 同步到路由 query
    await router.replace({ query: buildRouteQueryFromStore() })
  } catch (e) {
    // ignore router replace errors
  } finally {
    // 确保标志被重置
    setTimeout(() => {
      isHandlingRouteUpdate.value = false
    }, 0)
  }
}

function handlePageChange(page: number) {
  assetStore.setPageNo(page)
  refreshData(true)
}

function handleAdd() {
  initEmptyForm()
  showAddModal.value = true
}

function showAssetScanAddModal() {
  initEmptyForm()
  showAssetScanAddFlag.value = true
}

function closeAssetScanAddModal() {
  showAssetScanAddFlag.value = false
}

// 🔥 修复 handleAssetScanAddRecord 方法
function handleAssetScanAddRecord(records: RawAssetRecord[]) {
  console.log('=== handleAssetScanAddRecord ===')
  console.log('接收到的 records:', records)
  console.log('records 类型:', typeof records, Array.isArray(records))

  // 🔥 确保 records 是数组
  if (!Array.isArray(records)) {
    console.error('handleAssetScanAddRecord 接收到错误的数据格式:', records)
    emitter.emit('notify', {
      type: 'error',
      message: '数据格式错误，请重试'
    })
    return
  }

  console.log(`准备添加 ${records.length} 条扫描记录`)

  // 🔥 这里应该只是刷新列表，不要再次调用批量添加
  // 因为批量添加已经在 AssetScanAddModal 中完成了
  assetStore.loadList(true)
  assetStore.loadStats()

  emitter.emit('notify', {
    type: 'success',
    message: `扫描添加完成，共 ${records.length} 条记录`
  })
}

function closeAddModal() {
  showAddModal.value = false
}

async function handleAddRecord(data: typeof form) {
  try {
    await assetStore.addRecord({ ...data, amount: Number(data.amount) || 0 })
    showAddModal.value = false
    // 但为了更新统计数据，我们只刷新统计
    await assetStore.loadStats()
    resultCount.value = assetStore.pagination.total
  } catch (error) {
    // 错误处理已在 store 中完成
  }
}

function editRecord(id: number) {
  const idx = list.value.findIndex(r => String(r.id) === String(id))
  if (idx !== -1) {
    editingIdx.value = idx
    // 🔥 修改：编辑时填充表单数据
    const record = list.value[idx]
    initFormByRecord(record)
  }
}

function cancelEdit() {
  editingIdx.value = null
}

async function saveEdit(data: typeof form) {
  if (editingIdx.value === null) return

  try {
    const original = list.value[editingIdx.value]
    if (!original?.id) return

    await assetStore.updateRecord({
      ...data,
      id: original.id,
      amount: Number(data.amount) || 0
    })
    editingIdx.value = null
    // 🔥 修改：编辑后不需要再次刷新，因为 store 中的 updateRecord 已经调用了 loadList(true)
    // 但为了更新统计数据，我们只刷新统计
    await assetStore.loadStats()
    resultCount.value = assetStore.pagination.total
  } catch (error) {
    // 错误处理已在 store 中完成
  }
}

function handleDelete(record: any) {
  const dataInfo = `[${record.assetName},${record.assetTypeValue || '类型未知'},${record.amount}${record.unitValue},${record.assetLocationValue},${formatDate(record.acquireTime)}]`
  emitter.emit('confirm', {
    title: '删除确认',
    message: `确定要删除${dataInfo}这条记录吗？此操作无法撤销。`,
    type: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    async onConfirm() {
      try {
        await assetStore.handleDelete(record.id)
        // 🔥 修改：删除后不需要再次刷新，因为 store 中的 handleDelete 已经调用了 loadList(true)
        // 但为了更新统计数据，我们只刷新统计
        await assetStore.loadStats()
        resultCount.value = assetStore.pagination.total
      } catch (error) {
        // 错误处理已在 store 中完成
      }
    }
  })
}

function onCopyClick() {
  const PRIMARY_TYPE = 'primary'

  emitter.emit('confirm', {
    title: '复制确认',
    message: '是否复制上回记录？',
    type: PRIMARY_TYPE,
    confirmText: '复制',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await assetStore.copyLastRecords()
        await refreshData(true) // 🔥 复制后强制刷新
      } catch (e: any) {
        const msg = e.message || ''
        if (msg.includes('已有记录')) {
          setTimeout(() => {
            emitter.emit('confirm', {
              title: '覆盖确认',
              message: '今日已有记录，是否强制覆盖？',
              confirmText: '覆盖',
              cancelText: '取消',
              type: PRIMARY_TYPE,
              onConfirm: async () => {
                try {
                  await assetStore.copyLastRecords(true)
                  await refreshData(true) // 🔥 覆盖后强制刷新
                  notifyToast('复制成功', 'success')
                } catch (error: any) {
                  notifyToast(`覆盖失败：${error.message || '未知错误'}`, 'error')
                }
              }
            })
          }, 300)
        } else {
          notifyToast(`复制失败：${msg}`, 'error')
        }
      }
    }
  })
}

onMounted(async () => {
  // 初始化元数据与选项
  await Promise.all([
    metaStore.initAll(),
    assetNameStore.fetchAssetName()
  ])

  // 从路由 query 初始化查询条件（如果存在）
  const parsed = parseRouteQuery(route.query)
  assetStore.updateQuery(parsed)
  assetStore.setPageNo(1)
  await refreshData(true)
})

// 监听路由 query 的变化，但仅响应非程序触发的变化（如浏览器前进/后退）
watch(
  () => route.query,
  (q) => {
    // 如果是程序触发的路由更新，跳过处理
    if (isHandlingRouteUpdate.value) {
      return
    }

    // 否则（例如浏览器前进/后退）同步路由 query 到 store 并刷新数据
    const parsed = parseRouteQuery(q)
    const current = {
      assetNameIdList: query.value.assetNameIdList || [],
      assetTypeIdList: query.value.assetTypeIdList || [],
      assetLocationIdList: query.value.assetLocationIdList || [],
      startDate: query.value.startDate || '',
      endDate: query.value.endDate || '',
      remark: query.value.remark || ''
    }

    if (JSON.stringify(current) !== JSON.stringify(parsed)) {
      assetStore.updateQuery(parsed)
      assetStore.setPageNo(1)
      refreshData(true)
    }
  },
  { deep: true }
)

onBeforeUnmount(() => {
  clearCommonMetaCache()
})
</script>
