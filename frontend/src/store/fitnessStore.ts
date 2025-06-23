// src/store/fitnessStore.ts
import {defineStore} from 'pinia'
import {ref, reactive, computed} from 'vue'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'
import qs from 'qs'
import {formatFitnessRecord} from '@/utils/commonMeta'
import {formatTime} from '@/utils/formatters'

// 添加本地存储的 key，用于持久化查询条件
const QUERY_STORAGE_KEY = 'fitness_query_conditions'

export const useFitnessStore = defineStore('fitness', () => {
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
        typeIdList: number[]
        startDate: string
        endDate: string
        remark: string
    }>({
        typeIdList: [],
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

    // 统一列表加载状态，移除 loadingAll
    const loadingList = ref(false)
    const loadingStats = ref(false) // 修复了原始代码中的重复声明

    const stats = reactive({
        monthlyCount: 0,
        weeklyCount: 0,
        lastWorkoutDays: 0,
        nextWorkoutDay: '-',
        proteinIntake: 0,
        carbsIntake: 0
    })

    // 🔥 优化请求控制器管理
    let recordController: AbortController | null = null
    let statsController: AbortController | null = null

    // 🔥 添加请求防抖机制
    let loadRecordsTimeout: ReturnType<typeof setTimeout> | null = null
    let lastRequestParams: string = ''

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
    function buildParams(includePageInfo = true) {
        const baseParams = {
            typeIdList: query.typeIdList.length > 0 ? query.typeIdList : undefined,
            startDate: query.startDate ? query.startDate + 'T00:00:00' : undefined,
            endDate: query.endDate ? query.endDate + 'T23:59:59' : undefined,
            remark: query.remark.trim() || undefined
        }

        if (includePageInfo) {
            return {
                ...baseParams,
                page: pagination.pageNo,
                pageSize: pagination.pageSize
            }
        }

        return baseParams
    }

    // 🔥 优化错误处理函数
    async function handleError(action: string, err: any, isManualCancel = false) {
        // 🔥 区分不同类型的取消
        if (err?.code === 'ERR_CANCELED' || err?.name === 'AbortError') {
            if (isManualCancel) {
                console.log(`🟡 [${action}] 请求被主动取消（切换查询条件）`)
            } else {
                console.log(`🟡 [${action}] 请求被取消`)
            }
            return
        }

        // 🔥 忽略认证相关的错误，不显示给用户
        if (err?.message === 'AUTH_CANCELED' ||
            err?.message === '用户未登录，请先登录' ||
            err?.message === '请求已取消') {
            console.log(`🟡 [${action}] 认证相关错误，等待用户登录:`, err.message)
            return
        }

        // 其他错误正常处理
        console.error(`🔴 [${action}] 出错:`, err)
        emitter.emit('notify', {
            message: `${action} 失败：${err?.message || '未知错误'}`,
            type: 'error'
        })
    }

    // 🔥 安全取消请求的函数
    function cancelRecordRequest(reason = '新请求开始') {
        if (recordController) {
            console.log(`🟡 [请求管理] ${reason}，取消之前的记录请求`)
            recordController.abort()
            recordController = null
        }
    }

    // 🔥 检查参数是否发生变化
    function hasParamsChanged(newParams: any): boolean {
        const newParamsStr = JSON.stringify(newParams)
        const changed = newParamsStr !== lastRequestParams
        lastRequestParams = newParamsStr
        return changed
    }

    // --- 列表分页查询 ---
    async function loadList(force = false) {
        const params = buildParams()

        // 🔥 如果参数没有变化且不是强制加载，跳过请求
        if (!force && !hasParamsChanged(params) && list.value.length > 0) {
            console.log('🟡 [获取健身记录] 参数未变化，跳过重复请求')
            return
        }

        // 🔥 清除防抖定时器
        if (loadRecordsTimeout) {
            clearTimeout(loadRecordsTimeout)
            loadRecordsTimeout = null
        }

        // 🔥 如果有正在进行的请求，标记为主动取消
        const isManualCancel = recordController !== null
        cancelRecordRequest('分页查询开始')

        recordController = new AbortController()
        loadingList.value = true

        try {
            console.log('🟢 [获取健身记录] 开始分页查询', params)

            const res = await axiosInstance.get('/api/fitness-record/list', {
                params,
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

                list.value = await Promise.all(raw.records.map(formatFitnessRecord))
                pagination.total = Number(raw.total ?? 0)
                pagination.pageNo = Number(raw.current ?? pagination.pageNo)
                pagination.pageSize = Number(raw.size ?? pagination.pageSize)

                console.log('🟢 [获取健身记录] 分页查询成功', {
                    count: list.value.length,
                    total: pagination.total
                })
            } else {
                emitter.emit('notify', {
                    message: res.data.message || '获取列表失败',
                    type: 'error'
                })
            }
        } catch (err) {
            await handleError('获取健身记录', err, isManualCancel)
        } finally {
            loadingList.value = false
            recordController = null
        }
    }

    // --- 查询全部记录（不分页）---
    async function loadAllRecords(force = false) {
        const params = buildParams(false)

        // 🔥 如果参数没有变化且不是强制加载，跳过请求
        if (!force && !hasParamsChanged({...params, type: 'all'}) && allList.value.length > 0) {
            console.log('🟡 [获取全部记录] 参数未变化，跳过重复请求')
            return
        }

        // 🔥 清除防抖定时器
        if (loadRecordsTimeout) {
            clearTimeout(loadRecordsTimeout)
            loadRecordsTimeout = null
        }

        // 🔥 如果有正在进行的请求，标记为主动取消
        const isManualCancel = recordController !== null
        cancelRecordRequest('全量查询开始')

        recordController = new AbortController()
        loadingList.value = true

        try {
            console.log('🟢 [获取全部记录] 开始全量查询', params)

            const res = await axiosInstance.get('/api/fitness-record/listAll', {
                params,
                signal: recordController.signal,
                paramsSerializer: params => qs.stringify(params, {arrayFormat: 'repeat'})
            })

            if (res.data.success) {
                const raw = res.data.data || []
                allList.value = await Promise.all(raw.map(formatFitnessRecord))

                // 更新分页信息以反映全量数据
                pagination.total = raw.length
                pagination.pageNo = 1
                pagination.pageSize = raw.length || 10

                console.log('🟢 [获取全部记录] 全量查询成功', {
                    count: allList.value.length
                })
            } else {
                emitter.emit('notify', {
                    message: res.data.message || '获取全部记录失败',
                    type: 'error'
                })
            }
        } catch (err) {
            await handleError('获取全部记录', err, isManualCancel)
        } finally {
            loadingList.value = false
            recordController = null
        }
    }

    // 🔥 添加防抖版本的加载函数
    function loadAllRecordsDebounced(delay = 300) {
        if (loadRecordsTimeout) {
            clearTimeout(loadRecordsTimeout)
        }

        loadRecordsTimeout = setTimeout(() => {
            loadAllRecords(true)
        }, delay)
    }

    function loadListDebounced(delay = 300) {
        if (loadRecordsTimeout) {
            clearTimeout(loadRecordsTimeout)
        }

        loadRecordsTimeout = setTimeout(() => {
            loadList(true)
        }, delay)
    }

    // --- 查询参数更新，增加持久化 ---
    function updateQuery(newQuery: Partial<typeof query>) {
        const hasChanged = Object.keys(newQuery).some(key => {
            return (query as any)[key] !== (newQuery as any)[key]
        })

        if (hasChanged) {
            Object.assign(query, newQuery)
            saveQueryToStorage()
            console.log('🟡 [查询条件] 已更新', query)
        }
    }

    function setPageNo(page: number) {
        if (pagination.pageNo !== page) {
            pagination.pageNo = page
        }
    }

    function setPageSize(size: number) {
        if (pagination.pageSize !== size) {
            pagination.pageSize = size
            pagination.pageNo = 1
        }
    }

    // --- 重置查询参数，增加持久化 ---
    function resetQuery() {
        query.typeIdList = []
        query.startDate = ''
        query.endDate = ''
        query.remark = ''
        pagination.pageNo = 1
        saveQueryToStorage()

        // 🔥 重置时清除参数缓存，确保下次查询会执行
        lastRequestParams = ''
        console.log('🟡 [查询条件] 已重置')
    }

    // --- 统计 ---
    async function loadStats() {
        if (statsController) {
            console.log('🟡 [获取统计] 取消之前的统计请求')
            statsController.abort()
        }

        statsController = new AbortController()
        loadingStats.value = true

        try {
            const res = await axiosInstance.get('/api/fitness-record/stats', {
                signal: statsController.signal
            })

            if (res.data.success) {
                Object.assign(stats, res.data.data)
                console.log('🟢 [获取统计] 统计查询成功')
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

    // --- 增删改 ---
    async function addRecord(data: any) {
        try {
            const res = await axiosInstance.post('/api/fitness-record/add', formatTime(data))
            if (res.data.success) {
                emitter.emit('notify', {message: '添加成功', type: 'success'})
                // 🔥 添加后强制重新加载列表
                await loadList(true)
                return true
            } else {
                throw new Error(res.data.message || '添加失败')
            }
        } catch (err: any) {
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('添加记录', err)
                throw err
            } else {
                await handleError('添加记录', err)
                return false
            }
        }
    }

    async function updateRecord(data: any) {
        try {
            const res = await axiosInstance.put('/api/fitness-record/update', formatTime(data))
            if (res.data.success) {
                emitter.emit('notify', {message: '更新成功', type: 'success'})
                // 🔥 更新后强制重新加载列表
                await loadList(true)
                return true
            } else {
                throw new Error(res.data.message || '更新失败')
            }
        } catch (err: any) {
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('更新记录', err)
                throw err
            } else {
                await handleError('更新记录', err)
                return false
            }
        }
    }

    async function deleteRecord(id: number | string) {
        try {
            const res = await axiosInstance.delete(`/api/fitness-record/delete/${id}`)
            if (res.data.success) {
                emitter.emit('notify', {message: '删除成功', type: 'success'})
                // 🔥 删除后强制重新加载列表
                await loadList(true)
                return true
            } else {
                throw new Error(res.data.message || '删除失败')
            }
        } catch (err: any) {
            if (err?.message !== 'AUTH_CANCELED' &&
                err?.message !== '用户未登录，请先登录' &&
                err?.message !== '请求已取消') {
                await handleError('删除记录', err)
                throw err
            } else {
                await handleError('删除记录', err)
                return false
            }
        }
    }

    // 🔥 添加清理函数
    function cleanup() {
        cancelRecordRequest('组件销毁')

        if (statsController) {
            statsController.abort()
            statsController = null
        }

        if (loadRecordsTimeout) {
            clearTimeout(loadRecordsTimeout)
            loadRecordsTimeout = null
        }

        console.log('🟡 [Store清理] 已清理所有请求和定时器')
    }

    return {
        list,
        allList,
        query,
        pagination,
        loadingList,
        stats,
        loadingStats,
        hasRecords,
        recordCount,
        loadList,
        loadAllRecords,
        loadAllRecordsDebounced,
        loadListDebounced,
        updateQuery,
        setPageNo,
        setPageSize,
        resetQuery,
        loadStats,
        addRecord,
        updateRecord,
        deleteRecord,
        cleanup
    }
})