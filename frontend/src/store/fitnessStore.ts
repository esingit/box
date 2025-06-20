import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import axiosInstance from '@/utils/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import { formatFitnessRecord } from '@/utils/commonMeta'
import { formatTime } from '@/utils/formatters'

export const useFitnessStore = defineStore('fitness', () => {
  // --- 状态 ---
  const list = ref<any[]>([])
  const query = reactive<{
    typeIdList: number[]
    startDate: string
    endDate: string
    remark: string
  }>({
    typeIdList: [],
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

  const stats = reactive({
    monthlyCount: 0,
    weeklyCount: 0,
    lastWorkoutDays: 0,
    nextWorkoutDay: '-',
    proteinIntake: 0,
    carbsIntake: 0
  })
  const loadingStats = ref(false)

  let recordController: AbortController | null = null
  let statsController: AbortController | null = null

  const hasRecords = computed(() => list.value.length > 0)
  const recordCount = computed(() => pagination.total)

  // --- 内部函数 ---
  function buildParams() {
    return {
      page: pagination.pageNo,
      pageSize: pagination.pageSize,
      typeIdList: query.typeIdList.length > 0 ? query.typeIdList : undefined,
      startDate: query.startDate ? query.startDate + 'T00:00:00' : undefined,
      endDate: query.endDate ? query.endDate + 'T23:59:59' : undefined,
      remark: query.remark || undefined
    }
  }

  async function handleError(err: any, action: string) {
    if (err?.code === 'ERR_CANCELED') return
    console.error(`[${action}] 出错:`, err)
    emitter.emit('notify', {
      message: `${action} 失败：${err?.message || '未知错误'}`,
      type: 'error'
    })
  }

  // --- 列表操作 ---
  async function loadList() {
    if (recordController) recordController.abort()
    recordController = new AbortController()
    loadingList.value = true

    try {
      const res = await axiosInstance.get('/api/fitness-record/list', {
        params: buildParams(),
        signal: recordController.signal,
        paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
      })

      if (res.data.success) {
        const raw = res.data.data // 注意这里是 data 里的 data

        if (!raw.records || !Array.isArray(raw.records)) {
          list.value = []
          pagination.total = 0
          return
        }

        list.value = await Promise.all(raw.records.map(formatFitnessRecord))
        pagination.total = Number(raw.total ?? 0)
        pagination.pageNo = Number(raw.current ?? pagination.pageNo)
        pagination.pageSize = Number(raw.size ?? pagination.pageSize)
      } else {
        emitter.emit('notify', {
          message: res.data.message || '获取列表失败',
          type: 'error'
        })
      }
    } catch (err) {
      await handleError(err, '获取健身记录')
    } finally {
      loadingList.value = false
      recordController = null
    }
  }


  // --- 更新查询参数 ---
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
    query.typeIdList = []
    query.startDate = ''
    query.endDate = ''
    query.remark = ''
    pagination.pageNo = 1
  }

  // --- 统计操作 ---
  async function loadStats() {
    if (statsController) statsController.abort()
    statsController = new AbortController()
    loadingStats.value = true

    try {
      const res = await axiosInstance.get('/api/fitness-record/stats', {
        signal: statsController.signal
      })

      if (res.data.success) {
        Object.assign(stats, res.data.data)
      } else {
        emitter.emit('notify', { message: res.data.message || '获取统计失败', type: 'error' })
      }
    } catch (err) {
      await handleError(err, '获取统计')
    } finally {
      loadingStats.value = false
      statsController = null
    }
  }

  // --- 增删改 ---
  async function addRecord(data: any) {
    try {
      const res = await axiosInstance.post('/api/fitness-record/add', formatTime(data))
      if (res.data.success) {
        emitter.emit('notify', { message: '添加成功', type: 'success' })
        await loadList()
      } else {
        emitter.emit('notify', { message: res.data.message || '添加失败', type: 'error' })
      }
    } catch (err) {
      await handleError(err, '添加记录')
    }
  }

  async function updateRecord(data: any) {
    try {
      const res = await axiosInstance.put('/api/fitness-record/update', formatTime(data))
      if (res.data.success) {
        emitter.emit('notify', { message: '更新成功', type: 'success' })
        await loadList()
      } else {
        emitter.emit('notify', { message: res.data.message || '更新失败', type: 'error' })
      }
    } catch (err) {
      await handleError(err, '更新记录')
    }
  }

  async function deleteRecord(id: number | string) {
    try {
      const res = await axiosInstance.delete(`/api/fitness-record/delete/${id}`)
      if (res.data.success) {
        emitter.emit('notify', { message: '删除成功', type: 'success' })
        await loadList()
      } else {
        emitter.emit('notify', { message: res.data.message || '删除失败', type: 'error' })
      }
    } catch (err) {
      await handleError(err, '删除记录')
    }
  }

  return {
    list,
    query,
    pagination,
    loadingList,
    stats,
    loadingStats,
    hasRecords,
    recordCount,
    loadList,
    updateQuery,
    setPageNo,
    setPageSize,
    resetQuery,
    loadStats,
    addRecord,
    updateRecord,
    deleteRecord
  }
})
