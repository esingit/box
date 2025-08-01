// src/store/metaStore.ts
import { defineStore } from 'pinia'
import { reactive, ref, computed } from 'vue'
import axiosInstance from '@/api/axios'
import emitter from '@/utils/eventBus'
import { Option } from "@/types/common"
import { CommonMetaQueryDTO, CommonMetaVO } from "@/types/meta"
import { ApiResponse } from "@/types/api"

// 🔥 常量定义
const DEFAULT_DEBOUNCE_DELAY = 300

// 导入请求管理器
import { RequestManager } from '@/types/request'

export const useMetaStore = defineStore('meta', () => {
    // 缓存每类 typeCode 的元数据列表
    const typeMap = reactive<Record<string, CommonMetaVO[]>>({})

    /**
     * 将元数据映射为选项格式（包含所有字段）
     * @param typeCode 类型编码
     */
    function mapMetaToOptions(typeCode: string): Option[] {
        const metaList = typeMap[typeCode] || []
        return metaList.map(meta => ({
            label: meta.value1 || meta.label || String(meta.value),
            value: meta.id,
            id: meta.id,
            key1: meta.key1,
            key2: meta.key2,
            key3: meta.key3,
            key4: meta.key4,
            value1: meta.value1,
            value2: meta.value2,
            value3: meta.value3,
            value4: meta.value4
        }))
    }

    // 🔥 加载状态管理 - 改进版本
    const loadingState = reactive({
        query: false,
        init: false
    })

    // 添加独立的加载状态标识，便于模板中使用
    const loadingQuery = ref(false)
    const loadingInit = ref(false)

    // 🔥 请求管理
    const requestManager = new RequestManager()
    const isDev = import.meta.env.DEV

    // 防抖定时器
    let debounceTimer: ReturnType<typeof setTimeout> | null = null

    // 🔥 统一的加载状态管理函数
    function setLoadingState(type: 'query' | 'init', loading: boolean): void {
        switch (type) {
            case 'query':
                loadingQuery.value = loading
                loadingState.query = loading
                break
            case 'init':
                loadingInit.value = loading
                loadingState.init = loading
                break
        }
    }

    // 默认预加载元数据类型
    const defaultTypeCodes = ref<string[]>([
        'FITNESS_TYPE',
        'UNIT',
        'ASSET_TYPE',
        'ASSET_LOCATION'
    ])

    // 缓存查询参数，避免重复请求
    let lastQueryParams: string = ''

    function clearDebounceTimer(): void {
        if (debounceTimer) {
            clearTimeout(debounceTimer)
            debounceTimer = null
        }
    }

    function hasQueryParamsChanged(newParams: CommonMetaQueryDTO[]): boolean {
        const newParamsStr = JSON.stringify(newParams.sort((a, b) => a.typeCode.localeCompare(b.typeCode)))
        const changed = newParamsStr !== lastQueryParams
        lastQueryParams = newParamsStr
        return changed
    }

    // 🔥 计算属性
    const isLoading = computed(() => loadingQuery.value || loadingInit.value)
    const loadedTypes = computed(() => Object.keys(typeMap))
    const hasCache = computed(() => loadedTypes.value.length > 0)

    /**
     * 处理API响应
     * @param response API响应对象
     * @param operationName 操作名称
     * @returns 处理后的数据或null
     */
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

        return null
    }

    /**
     * 处理错误
     * @param operationName 操作名称
     * @param error 错误对象
     */
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

    /**
     * 判断是否是请求取消错误
     */
    function isRequestCancelled(error: unknown): boolean {
        const err = error as any
        return err?.code === 'ERR_CANCELED' ||
            err?.name === 'AbortError' ||
            err?.message?.includes('canceled')
    }

    /**
     * 判断是否是认证错误
     */
    function isAuthError(error: unknown): boolean {
        const err = error as any
        const authErrorMessages = [
            'AUTH_CANCELED',
            '用户未登录，请先登录',
            '请求已取消',
            '登录已过期，请重新登录'
        ]
        return authErrorMessages.includes(err?.message) || err?.response?.status === 401
    }

    /**
     * 获取错误消息
     */
    function getErrorMessage(error: unknown): string {
        if (error instanceof Error) {
            return error.message
        }
        return typeof error === 'string' ? error : '未知错误'
    }

    /**
     * 请求通用元数据（按 typeCode 合并查询，自动缓存）
     * @param dtoList 请求参数列表，只包含 typeCode
     * @param useCache 是否使用缓存，默认 true
     * @param force 是否强制刷新缓存，默认 false
     */
    async function queryMeta(
        dtoList: CommonMetaQueryDTO[],
        useCache = true,
        force = false
    ): Promise<CommonMetaVO[]> {
        // 验证参数
        if (!Array.isArray(dtoList) || dtoList.length === 0) {
            return []
        }

        // 去重并排序
        const uniqueDtoList = dtoList.filter((dto, index, arr) =>
            dto.typeCode && arr.findIndex(item => item.typeCode === dto.typeCode) === index
        )

        // 检查参数变化
        if (!force && !hasQueryParamsChanged(uniqueDtoList)) {
            if (isDev) {
                console.log('🟡 [获取元数据] 参数未变化，跳过重复请求')
            }
            return uniqueDtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        }

        const needQueryList = uniqueDtoList.filter(dto => {
            if (!dto.typeCode) return false
            return force || !useCache || !typeMap[dto.typeCode]
        })

        // 如果全部已缓存且不强制刷新，直接返回缓存数据
        if (needQueryList.length === 0) {
            if (isDev) {
                console.log('🟡 [获取元数据] 使用缓存数据')
            }
            return uniqueDtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        }

        clearDebounceTimer()
        const controller = requestManager.create('query')
        setLoadingState('query', true)

        try {
            if (isDev) {
                console.log('🟢 [获取元数据] 开始查询', needQueryList.map(d => d.typeCode))
            }

            const response = await axiosInstance.post('/api/common-meta/query', needQueryList, {
                signal: controller.signal
            })

            const data = handleApiResponse<CommonMetaVO[]>(response, '获取元数据')
            if (!data) return []

            // 按 typeCode 分类缓存
            for (const dto of needQueryList) {
                const code = dto.typeCode
                typeMap[code] = data.filter(item => item.typeCode === code)
            }

            if (isDev) {
                console.log(`🟢 [获取元数据] 查询成功`, {
                    types: needQueryList.map(d => d.typeCode),
                    totalCount: data.length
                })
            }

            return uniqueDtoList.flatMap(dto => typeMap[dto.typeCode] || [])
        } catch (error) {
            handleError('获取元数据', error)
            return []
        } finally {
            setLoadingState('query', false)
        }
    }

    /**
     * 防抖版本的查询函数
     * @param dtoList 请求参数列表
     * @param useCache 是否使用缓存
     * @param delay 防抖延迟时间
     */
    function queryMetaDebounced(
        dtoList: CommonMetaQueryDTO[],
        useCache = true,
        delay = DEFAULT_DEBOUNCE_DELAY
    ): Promise<CommonMetaVO[]> {
        return new Promise((resolve) => {
            clearDebounceTimer()
            debounceTimer = setTimeout(async () => {
                const result = await queryMeta(dtoList, useCache, true)
                resolve(result)
            }, delay)
        })
    }

    /**
     * 初始化默认类型的元数据
     * 通常在应用启动时调用
     */
    async function initAll(force = false): Promise<boolean> {
        if (!force && loadingInit.value) {
            if (isDev) {
                console.log('🟡 [初始化元数据] 正在初始化中，跳过重复请求')
            }
            return false
        }

        const controller = requestManager.create('init')
        setLoadingState('init', true)

        try {
            if (isDev) {
                console.log('🟢 [初始化元数据] 开始初始化', defaultTypeCodes.value)
            }

            const dtoList: CommonMetaQueryDTO[] = defaultTypeCodes.value.map(code => ({ typeCode: code }))
            const result = await queryMeta(dtoList, !force, force)

            const success = result.length > 0
            if (isDev) {
                console.log(`🟢 [初始化元数据] 初始化${success ? '成功' : '失败'}`, {
                    totalCount: result.length,
                    loadedTypes: loadedTypes.value
                })
            }

            return success
        } catch (error) {
            handleError('初始化元数据', error)
            return false
        } finally {
            setLoadingState('init', false)
        }
    }

    /**
     * 获取某类元数据选项列表
     * @param typeCode 类型编码
     * @param autoLoad 如果未缓存是否自动加载，默认 false
     */
    function getOptions(typeCode: string, autoLoad = false): CommonMetaVO[] {
        if (!typeCode) return []

        const cached = typeMap[typeCode]
        if (cached) {
            return cached
        }

        // 自动加载未缓存的元数据
        if (autoLoad && !loadingQuery.value) {
            queryMeta([{ typeCode }], true)
        }

        return []
    }

    /**
     * 根据typeCode和value获取具体的元数据项
     * @param typeCode 类型编码
     * @param value 值
     */
    function getOptionByValue(typeCode: string, value: string | number): CommonMetaVO | undefined {
        const options = getOptions(typeCode)
        return options.find(item => item.value === value)
    }

    /**
     * 手动设置某类元数据列表（用于前端动态修改）
     * @param typeCode 类型编码
     * @param list 元数据列表
     */
    function setTypeMap(typeCode: string, list: CommonMetaVO[]): void {
        if (!typeCode) return
        typeMap[typeCode] = Array.isArray(list) ? list : []

        if (isDev) {
            console.log(`🟡 [设置元数据] 已更新 ${typeCode}`, {
                count: typeMap[typeCode].length
            })
        }
    }

    /**
     * 清空指定类型的缓存
     * @param typeCode 类型编码，不传则清空所有
     */
    function clearCache(typeCode?: string): void {
        if (typeCode) {
            delete typeMap[typeCode]
            if (isDev) {
                console.log(`🧹 [清空缓存] 已清空 ${typeCode} 元数据缓存`)
            }
        } else {
            Object.keys(typeMap).forEach(code => delete typeMap[code])
            lastQueryParams = ''
            if (isDev) {
                console.log('🧹 [清空缓存] 已清空所有元数据缓存')
            }
        }
    }

    /**
     * 预加载指定类型的元数据
     * @param typeCodes 类型编码列表
     */
    async function preloadTypes(typeCodes: string[]): Promise<boolean> {
        if (!Array.isArray(typeCodes) || typeCodes.length === 0) {
            return false
        }

        const dtoList = typeCodes.map(code => ({ typeCode: code }))
        const result = await queryMeta(dtoList, true)
        return result.length > 0
    }

    /**
     * 刷新指定类型的元数据
     * @param typeCodes 类型编码列表，不传则刷新所有已加载的类型
     */
    async function refreshTypes(typeCodes?: string[]): Promise<boolean> {
        const targetTypes = typeCodes || loadedTypes.value
        if (targetTypes.length === 0) {
            return false
        }

        const dtoList = targetTypes.map(code => ({ typeCode: code }))
        const result = await queryMeta(dtoList, false, true)
        return result.length > 0
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
        typeMap,
        loadingState,
        defaultTypeCodes,

        loadingQuery,
        loadingInit,

        // 计算属性
        isLoading,
        loadedTypes,
        hasCache,

        // 核心方法
        queryMeta,
        queryMetaDebounced,
        initAll,
        mapMetaToOptions,

        // 工具方法
        getOptions,
        getOptionByValue,
        setTypeMap,
        clearCache,
        preloadTypes,
        refreshTypes,

        // 清理函数
        cleanup
    }
})