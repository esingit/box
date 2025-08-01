// 识别相关类型定义
import type { AxiosProgressEvent } from 'axios'
import type { AssetScanImageDTO, RawAssetRecord } from './asset'

// 识别选项接口
export interface RecognitionOptions {
    timeout?: number
    retryCount?: number
    onUploadProgress?: (progressEvent: AxiosProgressEvent) => void
    onRetry?: (attempt: number, maxAttempts: number) => void
}

// 识别结果接口
export interface RecognitionResult {
    ocrResults: AssetScanImageDTO[]
    assetRecords: RawAssetRecord[]
    recognizedItems: any[]
    warnings: string[]
}

// 识别状态接口
export interface RecognitionState {
    isRecognizing: boolean
    progress: number
    currentStep: string
    lastResult: RecognitionResult | null
    lastError: string | null
}