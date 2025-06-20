// src/store/assetNameStore
import {defineStore} from 'pinia'
import {ref, reactive, computed} from 'vue'
import axiosInstance from '@/utils/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import {formatAssetNameRecord} from '@/utils/commonMeta'

export const useAssetNameStore = defineStore('assetName', () => {
    // --- 状态 ---
    const list = ref<any[]>([])
    const query = reactive<{
        name: string
        description: string
        remark: string
    }>({
        name: '',
        description: '',
        remark: ''
    })

    const pagination = reactive({
        pageNo: 1,
        pageSize: 10,
        total: 0
    })

    const loadingList = ref(false)

    let recordController: AbortController | null = null

    const hasRecords = computed(() => list.value.length > 0)
    const recordCount = computed(() => pagination.total)

    // --- 内部函数 ---
    function buildParams() {
        return {
            page: pagination.pageNo,
            pageSize: pagination.pageSize,
            name: query.name.length ? query.name : undefined,
            description: query.description.length ? query.description : undefined,
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

    // --- 列表操作 ---
    async function loadList() {
        if (recordController) recordController.abort()
        recordController = new AbortController()
        loadingList.value = true

        try {
            const res = await axiosInstance.get('/api/asset-name/list', {
                params: buildParams(),
                signal: recordController.signal,
                paramsSerializer: params => qs.stringify(params, {arrayFormat: 'repeat'})
            })

            if (res.data.success) {
                const raw = res.data.data
                list.value = await Promise.all(raw.records.map(formatAssetNameRecord))
                pagination.total = Number(raw.total ?? 0)
                pagination.pageNo = Number(raw.current ?? pagination.pageNo)
                pagination.pageSize = Number(raw.size ?? pagination.pageSize)
            } else {
                emitter.emit('notify', {message: res.data.message || '获取列表失败', type: 'error'})
            }
        } catch (err) {
            await handleError(err, '获取资产记录')
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
        query.name = ''
        query.description = ''
        query.remark = ''
        pagination.pageNo = 1
    }

    // --- 增删改 ---
    async function addRecord(data: any) {
        try {
            const res = await axiosInstance.post('/api/asset-name/add', data)
            if (res.data.success) {
                emitter.emit('notify', {message: '添加成功', type: 'success'})
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
            const res = await axiosInstance.put('/api/asset-name', data)
            if (res.data.success) {
                emitter.emit('notify', {message: '更新成功', type: 'success'})
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
            const res = await axiosInstance.delete(`/api/asset-name/${id}`)
            if (res.data.success) {
                emitter.emit('notify', {message: '删除成功', type: 'success'})
                await loadList()
            } else {
                throw new Error(res.data.message || '删除失败')
            }
        } catch (err) {
            await handleError(err, '删除记录')
            throw err
        }
    }

    // 获取全部数据
    async function fetchAssetName() {
        try {
            const res = await axiosInstance.get('/api/asset-name/all')
            if (res.data?.success) {
                assetName.value = res.data.data || []
            } else {
                emitter.emit('notify', {
                    message: `获取资产名称列表失败: ${res.data?.message || '未知错误'}`,
                    type: 'error'
                })
            }
        } catch (error: any) {
            emitter.emit('notify', {
                message: `获取资产名称列表失败: ${error.message || '未知错误'}`,
                type: 'error'
            })
        }
    }

    const assetName = ref<any[]>([])
    const assetNameOptions = computed(() =>
        assetName.value.map(i => ({ label: i.name || '', value: i.id }))
    )

    function getAssetNameOptionById(id: string | number) {
        return assetNameOptions.value.find(i => i.value === id)
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
        handleDelete,
        fetchAssetName,
        assetName,
        assetNameOptions,
        getAssetNameOptionById
    }
})
