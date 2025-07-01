import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { AssetScanImageDTO, RawAssetRecord } from '@/types/asset'
import {
    convertOcrResultsToAssetRecords,
    convertToRecognizedAssetItems,
    validateOcrResults
} from '@/utils/ocrUtils'
import axiosInstance from '@/api/axios'
import type { AxiosProgressEvent } from 'axios'

interface RecognitionOptions {
    timeout?: number
    retryCount?: number
    onUploadProgress?: (progressEvent: AxiosProgressEvent) => void
    onRetry?: (attempt: number, maxAttempts: number) => void
}

interface RecognitionResult {
    ocrResults: AssetScanImageDTO[]
    assetRecords: RawAssetRecord[]
    recognizedItems: any[]
    warnings: string[]
}

interface RecognitionState {
    isRecognizing: boolean
    progress: number
    currentStep: string
    lastResult: RecognitionResult | null
    lastError: string | null
}

const DEFAULT_OPTIONS: Required<RecognitionOptions> = {
    timeout: 60000,
    retryCount: 3,
    onUploadProgress: () => {},
    onRetry: () => {}
}

const RETRYABLE_CONFIG = {
    codes: ['ECONNABORTED', 'NETWORK_ERROR'],
    statusCodes: [408, 429, 502, 503, 504],
    statusRanges: [{ min: 500, max: 599 }]
}

const ERROR_MESSAGES = {
    413: '图片文件过大，请压缩后重试',
    422: '图片格式不支持或内容无法识别',
    429: '请求过于频繁，请稍后重试',
    timeout: '识别超时，请尝试使用更小的图片或检查网络连接',
    network: '网络连接失败，请检查网络后重试',
    server: '服务器处理错误，请稍后重试',
    default: '图片识别失败，请重试'
} as const

export const useRecognitionStore = defineStore('recognition', () => {
    const state = ref<RecognitionState>({
        isRecognizing: false,
        progress: 0,
        currentStep: '',
        lastResult: null,
        lastError: null
    })

    const isProcessing = computed(() => state.value.isRecognizing)
    const currentProgress = computed(() => state.value.progress)
    const processingStep = computed(() => state.value.currentStep)
    const hasResult = computed(() => state.value.lastResult !== null)
    const hasError = computed(() => state.value.lastError !== null)
    const lastRecognitionResult = computed(() => state.value.lastResult)

    const logger = {
        attempt: (attempt: number, isRetry: boolean) => {
            console.log(`🔄 [OCR识别] 第${attempt}次尝试${isRetry ? '(重试)' : ''}`)
        },
        progress: (percent: number) => {
            console.log(`📤 [上传进度] ${percent}%`)
        },
        retry: (attempt: number, maxAttempts: number, delay: number) => {
            console.log(`⏳ [OCR识别] ${delay / 1000}秒后进行第${attempt + 1}/${maxAttempts}次尝试`)
        },
        success: (results: AssetScanImageDTO[]) => {
            console.log('🟢 [OCR识别] 后端返回结果:', results)
        },
        warning: (invalid: any[]) => {
            if (invalid.length > 0) {
                console.warn('🟡 [OCR识别] 发现问题记录:', invalid)
            }
        },
        data: (assetRecords: RawAssetRecord[], recognizedItems: any[], warnings: string[]) => {
            console.log('🟢 [OCR识别] 转换后的资产记录:', assetRecords)
            console.log('🟢 [OCR识别] 前端显示数据:', recognizedItems)
            console.log('🟡 [OCR识别] 验证警告:', warnings)
        },
        error: (error: any) => {
            console.error('❌ [OCR识别] 错误:', error)
        }
    }

    function updateState(updates: Partial<RecognitionState>) {
        Object.assign(state.value, updates)
    }

    function resetState() {
        updateState({
            isRecognizing: false,
            progress: 0,
            currentStep: '',
            lastError: null
        })
    }

    function setProgress(progress: number, step: string = '') {
        updateState({ progress, currentStep: step })
    }

    async function withRetry<T>(
        operation: () => Promise<T>,
        options: {
            maxAttempts: number
            shouldRetry: (error: any) => boolean
            onRetry?: (attempt: number, error: any) => void
            getDelay?: (attempt: number) => number
        }
    ): Promise<T> {
        const {
            maxAttempts,
            shouldRetry,
            onRetry,
            getDelay = (attempt) => Math.min(1000 * Math.pow(2, attempt - 1), 8000)
        } = options

        let lastError: any

        for (let attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.attempt(attempt, attempt > 1)
                return await operation()
            } catch (error) {
                lastError = error
                logger.error(error)

                if (attempt === maxAttempts || !shouldRetry(error)) {
                    throw error
                }

                const delay = getDelay(attempt)
                logger.retry(attempt, maxAttempts, delay)
                onRetry?.(attempt, error)

                await new Promise(resolve => setTimeout(resolve, delay))
            }
        }

        throw lastError
    }

    function isRetryableError(error: any): boolean {
        if (RETRYABLE_CONFIG.codes.includes(error.code)) {
            return true
        }

        if (error.response?.status && RETRYABLE_CONFIG.statusCodes.includes(error.response.status)) {
            return true
        }

        if (error.response?.status) {
            return RETRYABLE_CONFIG.statusRanges.some(
                range => error.response.status >= range.min && error.response.status <= range.max
            )
        }

        return !error.response
    }

    function enhanceError(error: any): void {
        if (error.code === 'ECONNABORTED') {
            error.userMessage = ERROR_MESSAGES.timeout
        } else if (!error.response) {
            error.code = 'NETWORK_ERROR'
            error.userMessage = ERROR_MESSAGES.network
        } else {
            const status = error.response.status
            error.userMessage = ERROR_MESSAGES[status as keyof typeof ERROR_MESSAGES] ||
                (status >= 500 ? ERROR_MESSAGES.server : ERROR_MESSAGES.default)
        }
    }

    function createProgressHandler(onUploadProgress: (progressEvent: AxiosProgressEvent) => void) {
        return (progressEvent: AxiosProgressEvent) => {
            onUploadProgress(progressEvent)

            if (progressEvent.total) {
                const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
                logger.progress(percent)
                setProgress(20 + (percent * 0.3), `正在上传图片... ${percent}%`)
            }
        }
    }

    async function performOcrRequest(
        formData: FormData,
        timeout: number,
        onUploadProgress: (progressEvent: AxiosProgressEvent) => void
    ): Promise<any> {
        return axiosInstance.post('/api/asset/recognition/image', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            },
            timeout,
            onUploadProgress: createProgressHandler(onUploadProgress)
        })
    }

    function handleApiResponse<T>(response: any, operation: string): T | null {
        if (response?.data?.success) {
            return response.data.data as T
        } else {
            throw new Error(`${operation}失败: ${response?.data?.message || '未知错误'}`)
        }
    }

    function processOcrResults(ocrResults: AssetScanImageDTO[]): RecognitionResult {
        setProgress(90, '正在处理结果...')

        const validation = validateOcrResults(ocrResults)
        logger.warning(validation.invalid)

        const assetRecords = convertOcrResultsToAssetRecords(validation.valid)
        const recognizedItems = convertToRecognizedAssetItems(validation.valid)

        logger.data(assetRecords, recognizedItems, validation.warnings)

        return {
            ocrResults: validation.valid,
            assetRecords,
            recognizedItems,
            warnings: validation.warnings
        }
    }

    async function recognizeImage(
        formData: FormData,
        options: RecognitionOptions = {}
    ): Promise<RecognitionResult | null> {
        const config = { ...DEFAULT_OPTIONS, ...options }

        updateState({
            isRecognizing: true,
            progress: 0,
            currentStep: '准备识别图片...',
            lastError: null
        })

        try {
            setProgress(10, '正在上传图片...')

            const response = await withRetry(
                () => performOcrRequest(formData, config.timeout, config.onUploadProgress),
                {
                    maxAttempts: config.retryCount,
                    shouldRetry: isRetryableError,
                    onRetry: (attempt, error) => {
                        const delay = Math.min(1000 * Math.pow(2, attempt - 1), 8000)
                        setProgress(30 + (attempt * 10), `${delay / 1000}秒后重试...`)
                        config.onRetry(attempt, config.retryCount)
                    }
                }
            )

            setProgress(70, '正在解析结果...')

            const ocrResults = handleApiResponse<AssetScanImageDTO[]>(response, '图片识别')
            if (!ocrResults) {
                throw new Error('未获取到识别结果')
            }

            logger.success(ocrResults)

            const result = processOcrResults(ocrResults)

            setProgress(100, '识别完成')
            updateState({
                lastResult: result,
                currentStep: '识别完成'
            })

            return result

        } catch (error: any) {
            enhanceError(error)

            const errorMessage = error.userMessage || error.message || '图片识别失败'
            updateState({
                lastError: errorMessage,
                currentStep: '识别失败'
            })

            throw error

        } finally {
            setTimeout(() => {
                updateState({ isRecognizing: false })
            }, 1000)
        }
    }

    async function quickRecognize(formData: FormData): Promise<RecognitionResult | null> {
        return recognizeImage(formData, {
            timeout: 30000,
            retryCount: 2
        })
    }

    async function highQualityRecognize(formData: FormData): Promise<RecognitionResult | null> {
        return recognizeImage(formData, {
            timeout: 120000,
            retryCount: 5
        })
    }

    function clearResult() {
        updateState({
            lastResult: null,
            lastError: null
        })
    }

    function clearError() {
        updateState({ lastError: null })
    }

    return {
        isProcessing,
        currentProgress,
        processingStep,
        hasResult,
        hasError,
        lastRecognitionResult,
        recognizeImage,
        quickRecognize,
        highQualityRecognize,
        clearResult,
        clearError,
        resetState
    }
})

export type { RecognitionOptions, RecognitionResult, RecognitionState }
