import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import axiosInstance from '@/utils/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import { formatAssetRecord } from '@/utils/commonMeta'

export const useAssetStore = defineStore('asset', () => {
  // --- 状态 ---
  const list = ref<any[]>([])
  const query = reactive({
    assetNameId: undefined as number | undefined,
    locationId: undefined as number | undefined,
    typeId: undefined as number | undefined,
    startDate: '',
    endDate: '',
    remark: ''
  })

  const pagination = reactive({
    pageNo: 1,
    pageSize: 10,
    total: 0
  })

  const loadingList = ref(false)

  // --- 计算属性 ---
  const hasRecords = computed(() => list.value.length > 0)
  const recordCount = computed(() => pagination.total)

  // --- 内部工具函数 ---
  function buildParams() {
    return {
      page: pagination.pageNo,
      pageSize: pagination.pageSize,
      assetNameId: query.assetNameId,
      locationId: query.locationId,
      typeId: query.typeId,
      startDate: query.startDate ? query.startDate + 'T00:00:00' : undefined,
      endDate: query.endDate ? query.endDate + 'T23:59:59' : undefined,
      remark: query.remark?.trim() || undefined
    }
  }

  async function handleError(err: any, action: string) {
    if (err?.code === 'ERR_CANCELED') return
    console.error(`[${action}] 出错:`, err)
    emitter.emit('notify', {
      message: `${action}失败：${err?.message || '未知错误'}`,
      type: 'error'
    })
  }

  // --- 列表操作 ---
  let listController: AbortController | null = null
  async function loadList() {
    if (listController) listController.abort()
    listController = new AbortController()
    loadingList.value = true

    try {
      const res = await axiosInstance.get('/api/asset-record/list', {
        params: buildParams(),
        signal: listController.signal,
        paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
      })

      if (res.data.success) {
        const raw = res.data.data
        list.value = await Promise.all(raw.records.map(formatAssetRecord))
        pagination.total = Number(raw.total ?? 0)
        pagination.pageNo = Number(raw.current ?? pagination.pageNo)
        pagination.pageSize = Number(raw.size ?? pagination.pageSize)
      } else {
        emitter.emit('notify', { message: res.data.message || '获取资产记录失败', type: 'error' })
      }
    } catch (err) {
      await handleError(err, '获取资产记录列表')
    } finally {
      loadingList.value = false
      listController = null
    }
  }

  // --- 查询参数更新 ---
  function updateQuery(newQuery: Partial<typeof query>) {
    Object.assign(query, newQuery)
  }

  function setPageNo(page: number) {
    pagination.pageNo = page
  }

  function setPageSize(size: number) {
    pagination.pageSize = size
    pagination.pageNo = 1
  }

  function resetQuery() {
    query.assetNameId = undefined
    query.locationId = undefined
    query.typeId = undefined
    query.startDate = ''
    query.endDate = ''
    query.remark = ''
    pagination.pageNo = 1
  }

  // --- 增删改 ---
  async function addRecord(record: any) {
    try {
      const res = await axiosInstance.post('/api/asset-record/add', record)
      if (res.data.success) {
        emitter.emit('notify', { message: '添加成功', type: 'success' })
        await loadList()
        return true
      } else {
        throw new Error(res.data.message || '添加失败')
      }
    } catch (err: any) {
      emitter.emit('notify', { message: `添加失败: ${err.message || '未知错误'}`, type: 'error' })
      throw err
    }
  }

  async function updateRecord(record: any) {
    try {
      const res = await axiosInstance.put('/api/asset-record/update', record)
      if (res.data.success) {
        emitter.emit('notify', { message: '更新成功', type: 'success' })
        await loadList()
        return true
      } else {
        throw new Error(res.data.message || '更新失败')
      }
    } catch (err: any) {
      emitter.emit('notify', { message: `更新失败: ${err.message || '未知错误'}`, type: 'error' })
      throw err
    }
  }

  async function deleteRecord(id: number | string) {
    try {
      const res = await axiosInstance.delete(`/api/asset-record/delete/${id}`)
      if (res.data.success) {
        emitter.emit('notify', { message: '删除成功', type: 'success' })
        await loadList()
      } else {
        throw new Error(res.data.message || '删除失败')
      }
    } catch (err: any) {
      emitter.emit('notify', { message: `删除失败: ${err.message || '未知错误'}`, type: 'error' })
      throw err
    }
  }

  return {
    list,
    query,
    pagination,
    loadingList,
    hasRecords,
    recordCount,

    loadList,
    updateQuery,
    setPageNo,
    setPageSize,
    resetQuery,

    addRecord,
    updateRecord,
    deleteRecord
  }
})
