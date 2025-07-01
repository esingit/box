// composables/useImageRecognition.ts
import { ref, nextTick, computed } from 'vue'
import { useRecognitionStore } from '@/store/recognitionStore'
import emitter from '@/utils/eventBus'
import { RecognizedAssetItem } from '@/types/asset'

export interface ImageRecognitionOptions {
    maxFileSize?: number // 最大文件大小 (MB)
    maxWidth?: number    // 最大宽度
    maxHeight?: number   // 最大高度
    quality?: number     // 压缩质量 0-1
    timeout?: number     // 超时时间 (ms)
    retryCount?: number  // 重试次数
}

export function useImageRecognition(loadAssetNames: () => Promise<void>) {
    const recognitionStore = useRecognitionStore()

    const imageFile = ref<File | null>(null)
    const imagePreview = ref('')
    const isProcessing = ref(false)
    const recognizedItems = ref<RecognizedAssetItem[]>([])

    // 新增状态
    const progress = ref(0)
    const currentStep = ref('')
    const processingOptions = ref<ImageRecognitionOptions>({
        maxFileSize: 5,
        maxWidth: 1920,
        maxHeight: 1080,
        quality: 0.8,
        timeout: 60000,
        retryCount: 3
    })

    /**
     * 文件验证
     */
    const validateFile = (file: File): string | null => {
        const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
        if (!allowedTypes.includes(file.type)) {
            return '请选择 JPG、PNG、GIF 或 WebP 格式的图片'
        }

        const maxSize = processingOptions.value.maxFileSize! * 1024 * 1024
        if (file.size > maxSize) {
            return `图片大小不能超过 ${processingOptions.value.maxFileSize}MB`
        }

        return null
    }

    /**
     * 图片压缩
     */
    const compressImage = async (file: File): Promise<File> => {
        return new Promise((resolve, reject) => {
            const canvas = document.createElement('canvas')
            const ctx = canvas.getContext('2d')
            const img = new Image()

            img.onload = () => {
                let { width, height } = img
                const maxWidth = processingOptions.value.maxWidth!
                const maxHeight = processingOptions.value.maxHeight!

                if (width > maxWidth || height > maxHeight) {
                    const ratio = Math.min(maxWidth / width, maxHeight / height)
                    width *= ratio
                    height *= ratio
                }

                canvas.width = width
                canvas.height = height
                ctx?.drawImage(img, 0, 0, width, height)

                canvas.toBlob(
                    (blob) => {
                        if (blob) {
                            const compressedFile = new File([blob], file.name, {
                                type: 'image/jpeg',
                                lastModified: Date.now()
                            })

                            console.log(`图片压缩: ${(file.size / 1024 / 1024).toFixed(2)}MB -> ${(compressedFile.size / 1024 / 1024).toFixed(2)}MB`)
                            resolve(compressedFile)
                        } else {
                            reject(new Error('图片压缩失败'))
                        }
                    },
                    'image/jpeg',
                    processingOptions.value.quality
                )
            }

            img.onerror = () => reject(new Error('图片加载失败'))
            img.src = URL.createObjectURL(file)
        })
    }

    /**
     * 带重试的识别请求
     */
    const recognizeWithRetry = async (formData: FormData, retryCount = 0): Promise<any> => {
        try {
            currentStep.value = retryCount > 0
                ? `重试识别 (${retryCount}/${processingOptions.value.retryCount})`
                : '正在识别图片...'
            progress.value = 30 + (retryCount * 15)

            const result = await recognitionStore.recognizeImage(formData, {
                timeout: processingOptions.value.timeout
            })

            return result
        } catch (error: any) {
            console.error(`识别失败 (尝试 ${retryCount + 1}):`, error)

            const shouldRetry = retryCount < processingOptions.value.retryCount! &&
                (error.code === 'ECONNABORTED' ||
                    error.code === 'NETWORK_ERROR' ||
                    (error.response && error.response.status >= 500))

            if (shouldRetry) {
                const delay = Math.min(1000 * Math.pow(2, retryCount), 8000)
                currentStep.value = `${delay / 1000}秒后重试...`

                await new Promise(resolve => setTimeout(resolve, delay))
                return recognizeWithRetry(formData, retryCount + 1)
            }

            throw error
        }
    }

    const handleImageUpload = (file: File) => {
        // 文件验证
        const validationError = validateFile(file)
        if (validationError) {
            emitter.emit('notify', {
                type: 'error',
                message: validationError
            })
            return
        }

        imageFile.value = file
        recognizedItems.value = []

        const reader = new FileReader()
        reader.onload = (e) => {
            imagePreview.value = e.target?.result as string
        }
        reader.readAsDataURL(file)
    }

    const recognizeImage = async (options?: Partial<ImageRecognitionOptions>) => {
        if (!imageFile.value) return

        // 合并配置
        if (options) {
            processingOptions.value = { ...processingOptions.value, ...options }
        }

        if (isProcessing.value) {
            emitter.emit('notify', {
                type: 'warning',
                message: '正在识别中，请稍候...'
            })
            return
        }

        isProcessing.value = true
        progress.value = 0
        currentStep.value = ''

        try {
            // 步骤1: 加载资产名称
            currentStep.value = '正在加载资产数据...'
            progress.value = 5
            await loadAssetNames()

            // 步骤2: 图片压缩
            currentStep.value = '正在压缩图片...'
            progress.value = 10

            const compressedFile = await compressImage(imageFile.value)

            // 步骤3: 准备上传
            currentStep.value = '正在准备上传...'
            progress.value = 20

            const formData = new FormData()
            formData.append('file', compressedFile)

            // 步骤4: 识别（含重试） - 修改这里的调用
            currentStep.value = '正在识别图片...'

            const result = await recognitionStore.recognizeImage(formData, {
                timeout: processingOptions.value.timeout,
                retryCount: processingOptions.value.retryCount,
                onUploadProgress: (progressEvent) => {
                    if (progressEvent.total) {
                        const uploadPercent = Math.round(
                            (progressEvent.loaded * 100) / progressEvent.total
                        )
                        progress.value = 20 + (uploadPercent * 0.3) // 20-50% 为上传进度
                        currentStep.value = `正在上传图片... ${uploadPercent}%`
                    }
                }
            })

            // 步骤5: 处理结果
            currentStep.value = '正在处理结果...'
            progress.value = 90

            await nextTick()

            // 处理返回结果（保持原有逻辑）
            if (result && result.recognizedItems) {
                recognizedItems.value = processRecognitionResult(result.recognizedItems)

                // 显示警告信息
                if (result.warnings && result.warnings.length > 0) {
                    console.warn('识别警告:', result.warnings)
                    const warningMessage = result.warnings.length > 3
                        ? `发现 ${result.warnings.length} 个问题，请检查识别结果`
                        : result.warnings.join('; ')

                    emitter.emit('notify', {
                        type: 'warning',
                        message: warningMessage
                    })
                }

                // 完成
                currentStep.value = '识别完成'
                progress.value = 100

                emitter.emit('notify', {
                    type: recognizedItems.value.length ? 'success' : 'warning',
                    message: recognizedItems.value.length
                        ? `成功识别 ${recognizedItems.value.length} 条有效数据`
                        : '未识别到有效数据'
                })
            } else {
                recognizedItems.value = []
                emitter.emit('notify', {
                    message: '未识别到有效数据',
                    type: 'warning'
                })
            }
        } catch (err: any) {
            console.error('图片识别失败:', err)

            // 使用增强的错误信息
            const errorMessage = err.userMessage || err.message || '图片识别失败，请重试'

            emitter.emit('notify', {
                message: errorMessage,
                type: 'error'
            })
        } finally {
            isProcessing.value = false
            progress.value = 0
            currentStep.value = ''
        }
    }

    const processRecognitionResult = (items: any[]): RecognizedAssetItem[] => {
        if (!Array.isArray(items)) {
            console.error('识别结果不是数组格式:', items)
            return []
        }

        return items.map((item, index) => ({
            // 基本信息
            id: item.id || index + 1,
            assetName: item.assetName || '未识别',
            assetNameId: safeParseId(item.assetNameId),
            amount: safeParseAmount(item.amount),
            remark: item.remark || '',

            // OCR相关信息
            originalAssetName: item.originalAssetName || '',
            matchedAssetName: item.matchedAssetName || '',
            matchScore: safeParseFloat(item.matchScore),
            confidence: safeParseFloat(item.confidence),
            isMatched: Boolean(item.isMatched),
            boundingBox: item.boundingBox || '',

            // 质量信息
            qualityScore: item.qualityScore || 0,
            hasWarnings: Boolean(item.hasWarnings)
        }))
    }

    const safeParseId = (id: any): string | null => {
        return id === null || id === undefined || id === '' ? null : String(id)
    }

    const safeParseAmount = (amount: any): number | null => {
        if (amount === null || amount === undefined || amount === '') {
            return null
        }

        if (typeof amount === 'string') {
            const parsed = parseFloat(amount.replace(/,/g, ''))
            return isNaN(parsed) ? null : parsed
        }

        const num = Number(amount)
        return isNaN(num) ? null : num
    }

    const safeParseFloat = (value: any): number => {
        if (value === null || value === undefined || value === '') {
            return 0
        }

        if (typeof value === 'string') {
            const parsed = parseFloat(value)
            return isNaN(parsed) ? 0 : parsed
        }

        const num = Number(value)
        return isNaN(num) ? 0 : num
    }

    return {
        imageFile,
        imagePreview,
        isProcessing: computed(() => isProcessing.value),
        recognizedItems,
        // 新增返回值
        progress: computed(() => progress.value),
        currentStep: computed(() => currentStep.value),
        processingOptions: computed(() => processingOptions.value),
        // 方法
        handleImageUpload,
        recognizeImage,
        validateFile,
        compressImage
    }
}