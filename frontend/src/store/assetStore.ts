// src/store/assetStore.ts
import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import type {BatchAddResult, RawAssetRecord} from '@/types/asset'
import { formatAssetRecord } from '@/utils/commonMeta'
import { formatTime } from '@/utils/formatters'
import type { Pagination } from '@/types/common'
import type { AssetRecord, QueryConditions, StatsData } from '@/types/asset'

// 🔥 常量定义
const DEFAULT_DEBOUNCE_DELAY = 300
const DEFAULT_PAGE_SIZE = 10

// 🔥 请求管理器类
class RequestManager {
    private controllers = new Map<string, AbortController>()
    private isDev = import.meta.env.DEV

    abort(key: string, reason = '新请求开始'): void {
        const controller = this.controllers.get(key)
        if (controller) {
            if (this.isDev) {
                console.log(`🟡 [请求管理] ${reason}，取消 ${key} 请求`)
            }
            controller.abort(reason)
            this.controllers.delete(key)
        }
    }

    create(key: string): AbortController {
        this.abort(key)
        const controller = new AbortController()
        this.controllers.set(key, controller)
        return controller
    }

    cleanup(): void {
        this.controllers.forEach((controller, key) => {
            controller.abort('Store cleanup')
        })
        this.controllers.clear()
        if (this.isDev) {
            console.log('🟡 [请求管理] 已清理所有请求')
        }
    }
}

export const useAssetStore = defineStore('asset', () => {
    // 🔥 状态定义
    const list = ref<AssetRecord[]>([])
    const allList = ref<AssetRecord[]>([])

    const query = reactive<QueryConditions>({
        assetNameIdList: [],
        assetLocationIdList: [],
        assetTypeIdList: [],
        startDate: '',
        endDate: '',
        remark: ''
    })

    const pagination = reactive<Pagination<any>>({
        pageNo: 1,
        pageSize: DEFAULT_PAGE_SIZE,
        total: 0,
        records: []
    })

    const stats = reactive<StatsData>({
        formattedDate: '-',
        totalAssets: 0,
        assetsChange: 0,
        totalLiabilities: 0,
        liabilitiesChange: 0,
        netAssets: 0,
        netAssetsChange: 0,
        investmentAssets: 0,
        investmentAssetsChange: 0,
    })

    // 🔥 加载状态管理 - 改进版本
    const loadingState = reactive({
        list: false,
        stats: false,
        operation: false,
        recognition: false
    })

    // 添加独立的加载状态标识，便于模板中使用
    const loadingList = ref(false)
    const loadingStats = ref(false)
    const loadingOperation = ref(false)
    const loadingRecognition = ref(false)

    // 🔥 请求管理
    const requestManager = new RequestManager()
    const isDev = import.meta.env.DEV

    // 防抖定时器
    let debounceTimer: ReturnType<typeof setTimeout> | null = null

    // 🔥 修复：参数缓存用于去重 - 分别缓存分页和全量查询的参数
    let lastListRequestParams: string = ''
    let lastAllRequestParams: string = ''

    // 🔥 统一的加载状态管理函数
    function setLoadingState(type: 'list' | 'stats' | 'operation' | 'recognition', loading: boolean): void {
        switch (type) {
            case 'list':
                loadingList.value = loading
                loadingState.list = loading
                break
            case 'stats':
                loadingStats.value = loading
                loadingState.stats = loading
                break
            case 'operation':
                loadingOperation.value = loading
                loadingState.operation = loading
                break
            case 'recognition':
                loadingRecognition.value = loading
                loadingState.recognition = loading
                break
        }
    }

    // 🔥 计算属性
    const hasRecords = computed(() => list.value.length > 0)
    const recordCount = computed(() => pagination.total)
    const isLoading = computed(() => loadingList.value || loadingStats.value || loadingOperation.value || loadingRecognition.value)

    function buildParams(includePageInfo = true): Record<string, any> {
        const baseParams: Record<string, any> = {
            assetNameIdList: query.assetNameIdList.length > 0 ? query.assetNameIdList : undefined,
            assetLocationIdList: query.assetLocationIdList.length > 0 ? query.assetLocationIdList : undefined,
            assetTypeIdList: query.assetTypeIdList.length > 0 ? query.assetTypeIdList : undefined,
            startDate: query.startDate ? `${query.startDate}T00:00:00` : undefined,
            endDate: query.endDate ? `${query.endDate}T23:59:59` : undefined,
            remark: query.remark.trim() || undefined
        }

        if (includePageInfo) {
            baseParams.page = pagination.pageNo
            baseParams.pageSize = pagination.pageSize
        }

        // 移除 undefined 值
        return Object.fromEntries(
            Object.entries(baseParams).filter(([_, value]) => value !== undefined)
        )
    }

    // 🔥 修复：改为纯函数，不产生副作用
    function checkParamsChanged(newParams: Record<string, any>, lastParams: string): boolean {
        const newParamsStr = JSON.stringify(newParams)
        return newParamsStr !== lastParams
    }

    // 🔥 修复：单独的函数更新缓存参数
    function updateCachedParams(newParams: Record<string, any>, type: 'list' | 'all'): void {
        const newParamsStr = JSON.stringify(newParams)
        if (type === 'list') {
            lastListRequestParams = newParamsStr
        } else {
            lastAllRequestParams = newParamsStr
        }
    }

    function clearDebounceTimer(): void {
        if (debounceTimer) {
            clearTimeout(debounceTimer)
            debounceTimer = null
        }
    }

    // 🔥 修复：统一的 API 响应处理 - 修复空返回值问题
    function handleApiResponse<T>(response: any, operationName: string): T | null {
        // 检查是否需要重新登录
        if (response?.data?.code === 'AUTH_REQUIRED') {
            if (isDev) {
                console.info(`🔐 [${operationName}] 检测到需要重新登录，已静默处理`)
            }
            return null
        }

        if (response?.data?.success) {
            // 🔥 修复：对于成功但无数据的情况（如删除操作），返回空对象而不是 null/undefined
            // 这样确保 result !== null 的判断能正确工作
            const data = response.data.data
            return data !== null && data !== undefined ? data : ({} as T)
        }

        // 业务逻辑错误
        const errorMessage = response?.data?.message || `${operationName}失败`
        emitter.emit('notify', {
            message: errorMessage,
            type: 'error'
        })

        throw new Error(errorMessage)
    }

    // 🔥 统一的错误处理
    function handleError(operationName: string, error: unknown): void {
        // 忽略取消相关的错误
        if (isRequestCancelled(error)) {
            if (isDev) {
                console.log(`🟡 [${operationName}] 请求被取消`)
            }
            return
        }

        // 忽略认证相关错误，这些会由全局处理
        if (isAuthError(error)) {
            if (isDev) {
                console.log(`🟡 [${operationName}] 认证错误，等待用户登录`)
            }
            return
        }

        // 记录并显示其他错误
        const errorMessage = getErrorMessage(error)
        if (isDev) {
            console.error(`🔴 [${operationName}] 出错:`, error)
        }

        emitter.emit('notify', {
            message: `${operationName}失败：${errorMessage}`,
            type: 'error'
        })
    }

    function isRequestCancelled(error: unknown): boolean {
        const err = error as any
        return err?.code === 'ERR_CANCELED' ||
            err?.name === 'AbortError' ||
            err?.message?.includes('canceled')
    }

    function isAuthError(error: unknown): boolean {
        const err = error as any
        const authErrorMessages = [
            'AUTH_CANCELED',
            '用户未登录，请先登录',
            '请求已取消',
            '登录已过期，请重新登录'
        ]
        return authErrorMessages.includes(err?.message)
    }

    function getErrorMessage(error: unknown): string {
        if (error instanceof Error) {
            return error.message
        }
        return typeof error === 'string' ? error : '未知错误'
    }

    // 🔥 修复：API 调用函数 - 修复缓存逻辑
    async function loadList(force = false): Promise<void> {
        const params = buildParams()

        // 🔥 修复：force = true 时直接跳过缓存检查
        if (!force) {
            const hasChanged = checkParamsChanged(params, lastListRequestParams)
            if (!hasChanged && list.value.length > 0) {
                if (isDev) {
                    console.log('🟡 [获取资产记录] 参数未变化，跳过重复请求')
                }
                return
            }
        }

        clearDebounceTimer()
        const controller = requestManager.create('list')
        setLoadingState('list', true)

        try {
            if (isDev) {
                console.log(`🟢 [获取资产记录] 开始分页查询${force ? ' (强制刷新)' : ''}`, params)
            }

            const response = await axiosInstance.get('/api/asset-record/list', {
                params,
                signal: controller.signal,
                paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
            })

            const data = handleApiResponse<Pagination<RawAssetRecord>>(response, '获取资产记录')
            if (!data) return // 需要重新登录

            if (!data.records || !Array.isArray(data.records)) {
                list.value = []
                pagination.total = 0
                // 🔥 修复：请求成功后更新缓存参数
                updateCachedParams(params, 'list')
                return
            }

            // 🔥 明确类型转换
            list.value = await Promise.all(
                data.records.map((record: RawAssetRecord) => formatAssetRecord(record))
            ) as unknown as AssetRecord[]

            pagination.total = Number(data.total ?? 0)
            pagination.pageNo = Number(data.pageNo ?? pagination.pageNo)
            pagination.pageSize = Number(data.pageSize ?? pagination.pageSize)

            // 🔥 修复：请求成功后更新缓存参数
            updateCachedParams(params, 'list')

            if (isDev) {
                console.log('🟢 [获取资产记录] 分页查询成功', {
                    count: list.value.length,
                    total: pagination.total
                })
            }
        } catch (error) {
            handleError('获取资产记录', error)
        } finally {
            setLoadingState('list', false)
        }
    }

    // 🔥 修复：同样修复 loadAllRecords 函数
    async function loadAllRecords(force = false): Promise<void> {
        const params = buildParams(false)

        // 🔥 修复：force = true 时直接跳过缓存检查
        if (!force) {
            const hasChanged = checkParamsChanged(params, lastAllRequestParams)
            if (!hasChanged && allList.value.length > 0) {
                if (isDev) {
                    console.log('🟡 [获取全部资产记录] 参数未变化，跳过重复请求')
                }
                return
            }
        }

        clearDebounceTimer()
        const controller = requestManager.create('allRecords')
        setLoadingState('list', true)

        try {
            if (isDev) {
                console.log(`🟢 [获取全部资产记录] 开始全量查询${force ? ' (强制刷新)' : ''}`, params)
            }

            const response = await axiosInstance.get('/api/asset-record/listAll', {
                params,
                signal: controller.signal,
                paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
            })

            const data = handleApiResponse<RawAssetRecord[]>(response, '获取全部资产记录')
            if (!data) return // 需要重新登录

            // 🔥 修复类型警告，确保data是数组类型
            const records = Array.isArray(data) ? data : []
            allList.value = await Promise.all(
                records.map((record: RawAssetRecord) => formatAssetRecord(record))
            ) as unknown as AssetRecord[]

            // 更新分页信息
            pagination.total = records.length
            pagination.pageNo = 1

            // 🔥 修复：请求成功后更新缓存参数
            updateCachedParams(params, 'all')

            if (isDev) {
                console.log('🟢 [获取全部资产记录] 全量查询成功', {
                    count: allList.value.length
                })
            }
        } catch (error) {
            handleError('获取全部资产记录', error)
        } finally {
            setLoadingState('list', false)
        }
    }

    async function loadStats(): Promise<void> {
        const controller = requestManager.create('stats')
        setLoadingState('stats', true)

        try {
            const response = await axiosInstance.get('/api/asset-record/latest-stats', {
                signal: controller.signal
            })

            const data = handleApiResponse<StatsData>(response, '获取统计')
            if (!data) return // 需要重新登录

            Object.assign(stats, data)

            if (isDev) {
                console.log('🟢 [获取统计] 统计查询成功')
            }
        } catch (error) {
            handleError('获取统计', error)
        } finally {
            setLoadingState('stats', false)
        }
    }

    // 🔥 防抖版本的加载函数
    function loadListDebounced(delay = DEFAULT_DEBOUNCE_DELAY): void {
        clearDebounceTimer()
        debounceTimer = setTimeout(() => loadList(true), delay)
    }

    function loadAllRecordsDebounced(delay = DEFAULT_DEBOUNCE_DELAY): void {
        clearDebounceTimer()
        debounceTimer = setTimeout(() => loadAllRecords(true), delay)
    }

    // 🔥 数据操作函数
    async function addRecord(data: any): Promise<boolean> {
        setLoadingState('operation', true)

        try {
            const response = await axiosInstance.post('/api/asset-record/add', formatTime(data))
            const result = handleApiResponse(response, '添加记录')

            if (result !== null) {
                emitter.emit('notify', { message: '添加成功', type: 'success' })
                await loadList(true)
                return true
            }
            return false
        } catch (error) {
            if (!isAuthError(error)) {
                handleError('添加记录', error)
                throw error
            }
            return false
        } finally {
            setLoadingState('operation', false)
        }
    }

    async function updateRecord(data: any): Promise<boolean> {
        setLoadingState('operation', true)

        try {
            const response = await axiosInstance.put('/api/asset-record/update', formatTime(data))
            const result = handleApiResponse(response, '更新记录')

            if (result !== null) {
                emitter.emit('notify', { message: '更新成功', type: 'success' })
                await loadList(true)
                return true
            }
            return false
        } catch (error) {
            if (!isAuthError(error)) {
                handleError('更新记录', error)
                throw error
            }
            return false
        } finally {
            setLoadingState('operation', false)
        }
    }

    async function handleDelete(id: number | string): Promise<boolean> {
        setLoadingState('operation', true)

        try {
            const response = await axiosInstance.delete(`/api/asset-record/delete/${id}`)
            const result = handleApiResponse(response, '删除记录')

            if (isDev) {
                console.log('🟢 [删除记录] 响应结果:', { response: response?.data, result })
            }

            if (result !== null) {
                emitter.emit('notify', { message: '删除成功', type: 'success' })
                await loadList(true)
                return true
            }
            return false
        } catch (error) {
            if (!isAuthError(error)) {
                handleError('删除记录', error)
                throw error
            }
            return false
        } finally {
            setLoadingState('operation', false)
        }
    }

    async function copyLastRecords(force = false): Promise<boolean> {
        setLoadingState('operation', true)

        try {
            const response = await axiosInstance.post(`/api/asset-record/copy-last${force ? '?force=true' : ''}`)
            const result = handleApiResponse(response, '复制记录')

            if (result !== null) {
                emitter.emit('notify', { message: '复制成功', type: 'success' })
                await loadList(true)
                return true
            }
            return false
        } catch (error) {
            if (!isAuthError(error)) {
                handleError('复制记录', error)
                throw error
            }
            return false
        } finally {
            setLoadingState('operation', false)
        }
    }

    // 🔥 OCR识别功能
    async function recognizeAssetImage(formData: FormData): Promise<RawAssetRecord[] | null> {
        setLoadingState('recognition', true)

        try {
            const response = await axiosInstance.post('/api/asset/recognition/image', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data'
                }
            })

            return handleApiResponse<RawAssetRecord[]>(response, '图片识别')
        } catch (error) {
            if (!isAuthError(error)) {
                handleError('图片识别', error)
                throw error
            }
            return null
        } finally {
            setLoadingState('recognition', false)
        }
    }


    // 🔥 检查今日是否有记录
    async function checkTodayRecords(): Promise<boolean> {
        try {
            const response = await axiosInstance.get('/api/asset-record/check-today')
            return handleApiResponse<boolean>(response, '检查今日记录') || false
        } catch (error) {
            if (!isAuthError(error)) {
                handleError('检查今日记录', error)
                throw error
            }
            return false
        }
    }

    // assetStore.ts 中修复 smartBatchAddRecords 方法
    async function smartBatchAddRecords(
        records: any[],
        forceOverwrite = false,
        copyLast = false
    ): Promise<BatchAddResult | null> {
        setLoadingState('operation', true)

        try {
            // 🔥 添加参数验证
            if (!records) {
                throw new Error('records 参数不能为空')
            }

            if (!Array.isArray(records)) {
                console.error('records 参数类型错误:', typeof records, records)
                throw new Error('records 必须是数组类型')
            }

            if (records.length === 0) {
                throw new Error('records 数组不能为空')
            }

            console.log('收到的 records 参数:', records)
            console.log('records 类型:', typeof records, 'isArray:', Array.isArray(records))

            // 🔥 确保ID字段保持为字符串
            const formattedRecords = records.map((item, index) => {
                if (!item || typeof item !== 'object') {
                    console.error(`记录 ${index} 格式错误:`, item)
                    throw new Error(`第 ${index + 1} 条记录格式错误`)
                }

                const formatted = formatTime(item)

                // 确保关键ID字段为字符串
                return {
                    ...formatted,
                    assetNameId: String(formatted.assetNameId || item.assetNameId),
                    assetTypeId: String(formatted.assetTypeId || item.assetTypeId),
                    assetLocationId: String(formatted.assetLocationId || item.assetLocationId),
                    unitId: String(formatted.unitId || item.unitId)
                }
            })

            console.log('格式化后的数据:', formattedRecords)

            const response = await axiosInstance.post('/api/asset-record/batch-add', {
                records: formattedRecords,
                forceOverwrite,
                copyLast
            })

            const result = handleApiResponse<BatchAddResult>(response, '批量添加记录')

            if (result !== null) {
                console.log('后端返回结果:', result)

                emitter.emit('notify', {
                    message: result.message,
                    type: 'success'
                })

                await Promise.all([
                    loadList(true),
                    loadStats()
                ])

                return result
            }
            return null
        } catch (error: any) {
            console.error('smartBatchAddRecords 错误详情:', error)
            if (!isAuthError(error)) {
                handleError('批量添加记录', error)
                throw error
            }
            return null
        } finally {
            setLoadingState('operation', false)
        }
    }

    // 保留原方法以保持兼容性
    async function batchAddRecords(records: any[], forceOverwrite = false): Promise<boolean> {
        const result = await smartBatchAddRecords(records, forceOverwrite, false)
        return result !== null
    }

    // 🔥 查询参数管理
    function updateQuery(newQuery: Partial<QueryConditions>): void {
        const hasChanged = Object.keys(newQuery).some(key => {
            return (query as any)[key] !== (newQuery as any)[key]
        })

        if (hasChanged) {
            Object.assign(query, newQuery)

            if (isDev) {
                console.log('🟡 [查询条件] 已更新', query)
            }
        }
    }

    function setPageNo(page: number): void {
        if (pagination.pageNo !== page) {
            pagination.pageNo = page
        }
    }

    function setPageSize(size: number): void {
        if (pagination.pageSize !== size) {
            pagination.pageSize = size
            pagination.pageNo = 1
        }
    }

    function resetQuery(): void {
        Object.assign(query, {
            assetNameIdList: [],
            assetLocationIdList: [],
            assetTypeIdList: [],
            startDate: '',
            endDate: '',
            remark: ''
        })
        pagination.pageNo = 1
        // 🔥 修复：清除所有缓存参数
        lastListRequestParams = ''
        lastAllRequestParams = ''

        if (isDev) {
            console.log('🟡 [查询条件] 已重置')
        }
    }

    // 🔥 清理函数
    function cleanup(): void {
        requestManager.cleanup()
        clearDebounceTimer()

        if (isDev) {
            console.log('🟡 [Store清理] 已清理所有请求和定时器')
        }
    }

    return {
        // 状态
        list,
        allList,
        query,
        pagination,
        stats,
        loadingState,

        // 👈 新增：独立的加载状态，便于模板使用
        loadingList,
        loadingStats,
        loadingOperation,
        loadingRecognition,

        // 计算属性
        hasRecords,
        recordCount,
        isLoading,

        // 加载函数
        loadList,
        loadAllRecords,
        loadStats,
        loadListDebounced,
        loadAllRecordsDebounced,

        // 数据操作
        addRecord,
        updateRecord,
        handleDelete,
        copyLastRecords,
        recognizeAssetImage,
        batchAddRecords,
        smartBatchAddRecords,
        checkTodayRecords,

        // 查询管理
        updateQuery,
        setPageNo,
        setPageSize,
        resetQuery,

        // 工具函数
        cleanup
    }
})