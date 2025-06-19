// 修改后的 assetStore.ts（含 assetNameOptions）
import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import axiosInstance from '@/utils/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import { formatAssetRecord } from '@/utils/commonMeta'

export const useAssetStore = defineStore('asset', () => {
    const list = ref<any[]>([])
    const query = reactive({
        assetNameIdList: [],
        assetLocationIdList: [],
        assetTypeIdList: [],
        startDate: '',
        endDate: '',
        remark: ''
    })

    const pagination = reactive({
        pageNo: 1,
        pageSize: 10,
        total: 0
    })

    const stats = reactive({
        formattedDate: '-',
        totalAssets: 0,
        assetsChange: 0,
        totalLiabilities: 0,
        liabilitiesChange: 0,
    })

    const loadingList = ref(false)
    const loadingStats = ref(false)

    let recordController: AbortController | null = null
    let statsController: AbortController | null = null

    const hasRecords = computed(() => list.value.length > 0)
    const recordCount = computed(() => pagination.total)

    function buildParams() {
        return {
            page: pagination.pageNo,
            pageSize: pagination.pageSize,
            assetNameIdList: query.assetNameIdList.length ? query.assetNameIdList : undefined,
            assetLocationIdList: query.assetLocationIdList.length ? query.assetLocationIdList : undefined,
            assetTypeIdList: query.assetTypeIdList.length ? query.assetTypeIdList : undefined,
            startDate: query.startDate ? query.startDate + 'T00:00:00' : undefined,
            endDate: query.endDate ? query.endDate + 'T23:59:59' : undefined,
            remark: query.remark.trim() || undefined
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

    function formatTime(data: any) {
        const acquireTime = data?.acquireTime
        if (typeof acquireTime !== 'string' || acquireTime.length === 0) {
            return { ...data, acquireTime: '' }
        }
        return {
            ...data,
            acquireTime: acquireTime.includes('T') ? acquireTime : acquireTime + 'T00:00:00'
        }
    }

    async function loadList() {
        if (recordController) recordController.abort()
        recordController = new AbortController()
        loadingList.value = true

        try {
            const res = await axiosInstance.get('/api/asset-record/list', {
                params: buildParams(),
                signal: recordController.signal,
                paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
            })

            if (res.data.success) {
                const raw = res.data.data
                list.value = await Promise.all(raw.records.map(formatAssetRecord))
                pagination.total = Number(raw.total ?? 0)
                pagination.pageNo = Number(raw.current ?? pagination.pageNo)
                pagination.pageSize = Number(raw.size ?? pagination.pageSize)
            } else {
                emitter.emit('notify', { message: res.data.message || '获取列表失败', type: 'error' })
            }
        } catch (err) {
            await handleError(err, '获取资产记录')
        } finally {
            loadingList.value = false
            recordController = null
        }
    }

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
        query.assetNameIdList = []
        query.assetLocationIdList = []
        query.assetTypeIdList = []
        query.startDate = ''
        query.endDate = ''
        query.remark = ''
        pagination.pageNo = 1
    }

    async function loadStats() {
        if (statsController) statsController.abort()
        statsController = new AbortController()
        loadingStats.value = true

        try {
            const res = await axiosInstance.get('/api/asset-record/latest-stats', {
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

    async function addRecord(data: any) {
        try {
            const res = await axiosInstance.post('/api/asset-record/add', formatTime(data))
            if (res.data.success) {
                emitter.emit('notify', { message: '添加成功', type: 'success' })
                await loadList()
                return true
            } else {
                throw new Error(res.data.message || '添加失败')
            }
        } catch (err: any) {
            await handleError(err, '添加记录')
            throw err
        }
    }

    async function updateRecord(data: any) {
        try {
            const res = await axiosInstance.put('/api/asset-record/update', formatTime(data))
            if (res.data.success) {
                emitter.emit('notify', { message: '更新成功', type: 'success' })
                await loadList()
                return true
            } else {
                throw new Error(res.data.message || '更新失败')
            }
        } catch (err: any) {
            await handleError(err, '更新记录')
            throw err
        }
    }

    async function handleDelete(id: number | string) {
        try {
            const res = await axiosInstance.delete(`/api/asset-record/delete/${id}`)
            if (res.data.success) {
                emitter.emit('notify', { message: '删除成功', type: 'success' })
                await loadList()
            } else {
                throw new Error(res.data.message || '删除失败')
            }
        } catch (err) {
            await handleError(err, '删除记录')
            throw err
        }
    }

    async function copyLastRecords(force = false) {
        try {
            const res = await axiosInstance.post('/api/asset-record/copy-last' + (force ? '?force=true' : ''))
            if (res.data?.success) {
                emitter.emit('notify', {
                    message: '复制成功',
                    type: 'success'
                })
            } else {
                throw new Error(res.data?.message || '复制失败')
            }
        } catch (error: any) {
            emitter.emit('notify', {
                message: `复制失败: ${error.message || '未知错误'}`,
                type: 'error'
            })
            throw error
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
        handleDelete,
        copyLastRecords,
    }
})
