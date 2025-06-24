// src/store/assetStore.ts
import {defineStore} from 'pinia'
import {computed, reactive, ref} from 'vue'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import {formatAssetRecord} from '@/utils/commonMeta'
import {formatTime} from '@/utils/formatters'

// 添加本地存储的 key
const QUERY_STORAGE_KEY = 'asset_query_conditions'

export const useAssetStore = defineStore('asset', () => {
    // --- 状态 ---
    const list = ref<any[]>([])
    const allList = ref<any[]>([])

    // 从本地存储恢复查询条件
    const getSavedQuery = () => {
        try {
            const saved = localStorage.getItem(QUERY_STORAGE_KEY)
            return saved ? JSON.parse(saved) : {}
        } catch {
            return {}
        }
    }

    // 初始化查询条件，并从本地存储恢复
    const query = reactive<{
        assetNameIdList: number[]
        assetLocationIdList: number[]
        assetTypeIdList: number[]
        startDate: string
        endDate: string
        remark: string
    }>({
        assetNameIdList: [],
        assetLocationIdList: [],
        assetTypeIdList: [],
        startDate: '',
        endDate: '',
        remark: '',
        ...getSavedQuery() // 恢复保存的查询条件
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

    // 使用控制器管理请求取消
    let recordController: AbortController | null = null
    let statsController: AbortController | null = null

    const hasRecords = computed(() => list.value.length > 0)
    const recordCount = computed(() => pagination.total)

    // --- 内部函数 ---
    // 保存查询条件到本地存储
    function saveQueryToStorage() {
        try {
            localStorage.setItem(QUERY_STORAGE_KEY, JSON.stringify(query))
        } catch (error) {
            console.warn('Failed to save query to localStorage:', error)
        }
    }

    // 构建查询参数，并对 remark 进行 trim() 处理
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

    // 🔥 优化后的错误处理函数
    async function handleError(action: string, err: any) {
        // 忽略取消的请求
        if (err?.code === 'ERR_CANCELED') {
            console.log(`[${action}] 请求被取消`)
            return
        }

        // 🔥 忽略认证相关的错误，不显示给用户
        if (err?.message === 'AUTH_CANCELED' ||
            err?.message === '用户未登录，请先登录' ||
            err?.message === '请求已取消') {
            console.log(`[${action}] 认证相关错误，等待用户登录:`, err.message)
            return
        }

        // 其他错误正常处理
        console.error(`[${action}] 出错:`, err)
        emitter.emit('notify', {
            message: `${action} 失败：${err?.message || '未知错误'}`,
            type: 'error'
        })
    }

    // --- 列表分页查询 ---
    async function loadList() {
        // 如果有正在进行的列表请求，则取消它
        if (recordController) recordController.abort()
        recordController = new AbortController()
        loadingList.value = true

        try {
            const res = await axiosInstance.get('/api/asset-record/list', {
                params: buildParams(),
                signal: recordController.signal,
                paramsSerializer: params => qs.stringify(params, {arrayFormat: 'repeat'})
            })

            if (res.data.success) {
                const raw = res.data.data

                if (!raw.records || !Array.isArray(raw.records)) {
                    list.value = []
                    pagination.total = 0
                    return
                }

                list.value = await Promise.all(raw.records.map(formatAssetRecord))
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
            await handleError('获取资产记录', err)
        } finally {
            loadingList.value = false
            recordController = null
        }
    }

    // --- 查询全部记录（不分页） ---
    async function loadAllRecords() {
        // 如果有正在进行的列表请求，则取消它
        if (recordController) recordController.abort()
        recordController = new AbortController()
        loadingList.value = true

        try {
            const res = await axiosInstance.get('/api/asset-record/listAll', {
                params: {
                    assetNameIdList: query.assetNameIdList.length ? query.assetNameIdList : undefined,
                    assetLocationIdList: query.assetLocationIdList.length ? query.assetLocationIdList : undefined,
                    assetTypeIdList: query.assetTypeIdList.length ? query.assetTypeIdList : undefined,
                    startDate: query.startDate ? query.startDate + 'T00:00:00' : undefined,
                    endDate: query.endDate ? query.endDate + 'T23:59:59' : undefined,
                    remark: query.remark.trim() || undefined
                },
                signal: recordController.signal,
                paramsSerializer: params => qs.stringify(params, {arrayFormat: 'repeat'})
            })

            if (res.data.success) {
                const raw = res.data.data || [] // 确保数据为数组
                allList.value = await Promise.all(raw.map(formatAssetRecord))
                // 更新分页信息以反映全量数据
                pagination.total = raw.length
                pagination.pageNo = 1
                pagination.pageSize = raw.length || 10
            } else {
                emitter.emit('notify', {
                    message: res.data.message || '获取全部记录失败',
                    type: 'error'
                })
            }
        } catch (err) {
            await handleError('获取全部资产记录', err)
        } finally {
            loadingList.value = false
            recordController = null
        }
    }

    // --- 查询参数更新，增加持久化 ---
    function updateQuery(newQuery: Partial<typeof query>) {
        Object.assign(query, newQuery)
        saveQueryToStorage() // 保存到本地存储
    }

    function setPageNo(page: number) {
        pagination.pageNo = page
    }

    function setPageSize(size: number) {
        pagination.pageSize = size
        pagination.pageNo = 1
    }

    // --- 重置查询参数，增加持久化 ---
    function resetQuery() {
        query.assetNameIdList = []
        query.assetLocationIdList = []
        query.assetTypeIdList = []
        query.startDate = ''
        query.endDate = ''
        query.remark = ''
        pagination.pageNo = 1
        saveQueryToStorage() // 保存到本地存储
    }

    // --- 统计 ---
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
                emitter.emit('notify', {
                    message: res.data.message || '获取统计失败',
                    type: 'error'
                })
            }
        } catch (err) {
            await handleError('获取统计', err)
        } finally {
            loadingStats.value = false
            statsController = null
        }
    }

    // --- 增删改，调整逻辑使其与 fitnessStore 一致 ---
    async function addRecord(data: any) {
        try {
            const res = await axiosInstance.post('/api/asset-record/add', formatTime(data))
            if (res.data.success) {
                emitter.emit('notify', {message: '添加成功', type: 'success'})
                await loadList() // 添加后重新加载列表
                return true // 成功时返回 true
            } else {
                throw new Error(res.data.message || '添加失败') // 失败时抛出错误
            }
        } catch (err: any) {
            // 🔥 只有非认证错误才抛出，认证错误由 handleError 静默处理
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('添加记录', err)
                throw err // 重新抛出错误
            } else {
                await handleError('添加记录', err)
                // 认证错误不抛出，让组件可以正常处理
                return false
            }
        }
    }

    async function updateRecord(data: any) {
        try {
            const res = await axiosInstance.put('/api/asset-record/update', formatTime(data))
            if (res.data.success) {
                emitter.emit('notify', {message: '更新成功', type: 'success'})
                await loadList() // 更新后重新加载列表
                return true // 成功时返回 true
            } else {
                throw new Error(res.data.message || '更新失败') // 失败时抛出错误
            }
        } catch (err: any) {
            // 🔥 只有非认证错误才抛出，认证错误由 handleError 静默处理
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('更新记录', err)
                throw err // 重新抛出错误
            } else {
                await handleError('更新记录', err)
                // 认证错误不抛出，让组件可以正常处理
                return false
            }
        }
    }

    async function handleDelete(id: number | string) {
        try {
            const res = await axiosInstance.delete(`/api/asset-record/delete/${id}`)
            if (res.data.success) {
                emitter.emit('notify', {message: '删除成功', type: 'success'})
                await loadList() // 删除后重新加载列表
                return true // 🔥 删除成功也返回 true
            } else {
                throw new Error(res.data.message || '删除失败') // 失败时抛出错误
            }
        } catch (err: any) {
            // 🔥 只有非认证错误才抛出，认证错误由 handleError 静默处理
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('删除记录', err)
                throw err // 重新抛出错误
            } else {
                await handleError('删除记录', err)
                // 认证错误不抛出，让组件可以正常处理
                return false
            }
        }
    }

    async function copyLastRecords(force = false) {
        try {
            const res = await axiosInstance.post('/api/asset-record/copy-last' + (force ? '?force=true' : ''))
            if (res.data?.success) {
                emitter.emit('notify', {message: '复制成功', type: 'success'})
                await loadList() // 🔥 复制成功后重新加载列表
                return true // 🔥 复制成功返回 true
            } else {
                throw new Error(res.data?.message || '复制失败')
            }
        } catch (err: any) {
            // 🔥 只有非认证错误才抛出和显示错误，认证错误静默处理
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                emitter.emit('notify', {
                    message: `复制失败: ${err.message || '未知错误'}`,
                    type: 'error'
                })
                throw err
            } else {
                console.log('[复制记录] 认证相关错误，等待用户登录:', err.message)
                // 认证错误不抛出，让组件可以正常处理
                return false
            }
        }
    }

    // OCR识别图片
    async function recognizeAssetImage(formData: FormData) {
        try {
            const res = await axiosInstance.post('/api/asset-record/recognize-image', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            })

            if (res.data.success) {
                return res.data.data // 返回 AssetRecordDTO 数组
            } else {
                throw new Error(res.data.message || '图片识别失败')
            }
        } catch (err) {
            await handleError('图片识别', err)
            throw err
        }
    }

// 批量添加资产记录
    async function batchAddRecords(records: any[]) {
        try {
            const res = await axiosInstance.post('/api/asset-record/batch-add', records.map(item => formatTime(item)))

            if (res.data.success) {
                emitter.emit('notify', {
                    message: `成功添加 ${res.data.data} 条记录`,
                    type: 'success'
                })
                await loadList() // 添加后重新加载列表
                return true
            } else {
                throw new Error(res.data.message || '批量添加失败')
            }
        } catch (err: any) {
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('批量添加记录', err)
                throw err
            } else {
                await handleError('批量添加记录', err)
                return false
            }
        }
    }

    return {
        // 状态
        list,
        allList,
        query,
        pagination,
        loadingList,
        stats,
        loadingStats,
        hasRecords,
        recordCount,

        // 方法
        loadList,
        loadAllRecords,
        updateQuery,
        setPageNo,
        setPageSize,
        resetQuery,
        loadStats,
        addRecord,
        updateRecord,
        handleDelete,
        copyLastRecords,
        recognizeAssetImage,
        batchAddRecords,
    }
})