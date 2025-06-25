// src/store/fitnessStore.ts
import { defineStore } from 'pinia'
import { ref, reactive, computed } from 'vue'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import { formatFitnessRecord } from '@/utils/commonMeta'
import { formatTime } from '@/utils/formatters'
import type { Pagination } from '@/types/common'
import type { FormattedFitnessRecord, StatsData, RawFitnessRecord, QueryConditions } from '@/types/fitness'


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

export const useFitnessStore = defineStore('fitness', () => {
    // 🔥 状态定义
    const list = ref<FormattedFitnessRecord[]>([])
    const allList = ref<FormattedFitnessRecord[]>([])

    const query = reactive<QueryConditions>({
        typeIdList: [],
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
        monthlyCount: 0,
        weeklyCount: 0,
        lastWorkoutDays: 0,
        nextWorkoutDay: '-',
        proteinIntake: 0,
        carbsIntake: 0
    })

    // 🔥 加载状态管理 - 改进版本
    const loadingState = reactive({
        list: false,
        stats: false,
        operation: false
    })

    // 添加独立的加载状态标识，便于模板中使用
    const loadingList = ref(false)
    const loadingStats = ref(false)
    const loadingOperation = ref(false)

    // 🔥 请求管理
    const requestManager = new RequestManager()
    const isDev = import.meta.env.DEV

    // 防抖定时器
    let debounceTimer: ReturnType<typeof setTimeout> | null = null

    // 参数缓存用于去重
    let lastRequestParams: string = ''

    // 🔥 统一的加载状态管理函数
    function setLoadingState(type: 'list' | 'stats' | 'operation', loading: boolean): void {
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
        }
    }

    // 🔥 计算属性
    const hasRecords = computed(() => list.value.length > 0)
    const recordCount = computed(() => pagination.total)
    const isLoading = computed(() => loadingList.value || loadingStats.value || loadingOperation.value)

    function buildParams(includePageInfo = true): Record<string, any> {
        const baseParams: Record<string, any> = {
            typeIdList: query.typeIdList.length > 0 ? query.typeIdList : undefined,
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

    function hasParamsChanged(newParams: Record<string, any>): boolean {
        const newParamsStr = JSON.stringify(newParams)
        const changed = newParamsStr !== lastRequestParams
        lastRequestParams = newParamsStr
        return changed
    }

    function clearDebounceTimer(): void {
        if (debounceTimer) {
            clearTimeout(debounceTimer)
            debounceTimer = null
        }
    }

    // 🔥 统一的 API 响应处理
    function handleApiResponse<T>(response: any, operationName: string): T | null {
        // 检查是否需要重新登录
        if (response?.data?.code === 'AUTH_REQUIRED') {
            if (isDev) {
                console.info(`🔐 [${operationName}] 检测到需要重新登录，已静默处理`)
            }
            return null
        }

        if (response?.data?.success) {
            return response.data.data
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

    // 🔥 API 调用函数
    async function loadList(force = false): Promise<void> {
        const params = buildParams()

        if (!force && !hasParamsChanged(params) && list.value.length > 0) {
            if (isDev) {
                console.log('🟡 [获取健身记录] 参数未变化，跳过重复请求')
            }
            return
        }

        clearDebounceTimer()
        const controller = requestManager.create('list')
        setLoadingState('list', true)

        try {
            if (isDev) {
                console.log('🟢 [获取健身记录] 开始分页查询', params)
            }

            const response = await axiosInstance.get('/api/fitness-record/list', {
                params,
                signal: controller.signal,
                paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
            })

            const data = handleApiResponse<Pagination<RawFitnessRecord>>(response, '获取健身记录')
            if (!data) return // 需要重新登录

            if (!data.records || !Array.isArray(data.records)) {
                list.value = []
                pagination.total = 0
                return
            }

            // 🔥 明确类型转换
            list.value = await Promise.all(
                data.records.map((record: RawFitnessRecord) => formatFitnessRecord(record))
            ) as FormattedFitnessRecord[]

            pagination.total = Number(data.total ?? 0)
            pagination.pageNo = Number(data.pageNo ?? pagination.pageNo)
            pagination.pageSize = Number(data.pageSize ?? pagination.pageSize)

            if (isDev) {
                console.log('🟢 [获取健身记录] 分页查询成功', {
                    count: list.value.length,
                    total: pagination.total
                })
            }
        } catch (error) {
            handleError('获取健身记录', error)
        } finally {
            setLoadingState('list', false)
        }
    }

    async function loadAllRecords(force = false): Promise<void> {
        const params = buildParams(false)
        const paramKey = { ...params, type: 'all' }

        if (!force && !hasParamsChanged(paramKey) && allList.value.length > 0) {
            if (isDev) {
                console.log('🟡 [获取全部记录] 参数未变化，跳过重复请求')
            }
            return
        }

        clearDebounceTimer()
        const controller = requestManager.create('allRecords')
        setLoadingState('list', true)

        try {
            if (isDev) {
                console.log('🟢 [获取全部记录] 开始全量查询', params)
            }

            const response = await axiosInstance.get('/api/fitness-record/listAll', {
                params,
                signal: controller.signal,
                paramsSerializer: params => qs.stringify(params, { arrayFormat: 'repeat' })
            })

            const data = handleApiResponse<RawFitnessRecord[]>(response, '获取全部记录')
            if (!data) return // 需要重新登录

            // 🔥 修复类型警告，确保data是数组类型
            const records = Array.isArray(data) ? data : []
            allList.value = await Promise.all(
                records.map((record: RawFitnessRecord) => formatFitnessRecord(record))
            ) as FormattedFitnessRecord[]

            // 更新分页信息
            pagination.total = records.length
            pagination.pageNo = 1

            if (isDev) {
                console.log('🟢 [获取全部记录] 全量查询成功', {
                    count: allList.value.length
                })
            }
        } catch (error) {
            handleError('获取全部记录', error)
        } finally {
            setLoadingState('list', false)
        }
    }

    async function loadStats(): Promise<void> {
        const controller = requestManager.create('stats')
        setLoadingState('stats', true)

        try {
            const response = await axiosInstance.get('/api/fitness-record/stats', {
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
            const response = await axiosInstance.post('/api/fitness-record/add', formatTime(data))
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
            const response = await axiosInstance.put('/api/fitness-record/update', formatTime(data))
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

    async function deleteRecord(id: number | string): Promise<boolean> {
        setLoadingState('operation', true)

        try {
            const response = await axiosInstance.delete(`/api/fitness-record/delete/${id}`)
            const result = handleApiResponse(response, '删除记录')

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
            typeIdList: [],
            startDate: '',
            endDate: '',
            remark: ''
        })
        pagination.pageNo = 1
        lastRequestParams = '' // 清除参数缓存

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
        deleteRecord,

        // 查询管理
        updateQuery,
        setPageNo,
        setPageSize,
        resetQuery,

        // 工具函数
        cleanup
    }
})