import type { AssetScanImageDTO, RawAssetRecord } from '@/types/asset'

/**
 * 金额解析：字符串支持千分位处理
 */
function parseAmount(input: unknown): number {
    if (typeof input === 'string') {
        const parsed = parseFloat(input.replace(/,/g, ''))
        return isNaN(parsed) ? 0 : parsed
    } else if (typeof input === 'number') {
        return input
    }
    return 0
}

/**
 * 匹配度 / 置信度解析
 */
function parseScore(input: unknown): number | null {
    if (typeof input === 'string') {
        const parsed = parseFloat(input)
        return isNaN(parsed) ? null : parsed
    } else if (typeof input === 'number') {
        return input
    }
    return null
}

/**
 * OCR → 资产记录 (移除过滤逻辑)
 */
export function convertOcrResultToAssetRecord(ocrResult: AssetScanImageDTO): RawAssetRecord {
    return {
        assetNameId: ocrResult.assetNameId != null ? String(ocrResult.assetNameId) : '0',
        assetTypeId: ocrResult.assetTypeId || 1,
        unitId: ocrResult.unitId || 1,
        assetLocationId: ocrResult.assetLocationId || 1,
        amount: parseAmount(ocrResult.amount),
        acquireTime: ocrResult.acquireTime || new Date().toISOString()
    }
}

/**
 * 批量转换 OCR → 资产记录 (不过滤任何数据)
 */
export function convertOcrResultsToAssetRecords(ocrResults: AssetScanImageDTO[]): RawAssetRecord[] {
    return Array.isArray(ocrResults)
        ? ocrResults.map(convertOcrResultToAssetRecord)
        : []
}

/**
 * OCR → 识别列表展示项 (保留所有数据)
 */
export function convertToRecognizedAssetItems(ocrResults: AssetScanImageDTO[]): any[] {
    return Array.isArray(ocrResults)
        ? ocrResults.map((result, index) => {
            const amount = parseAmount(result.amount)
            const matchScore = parseScore(result.matchScore)
            const confidence = parseScore(result.confidence)

            return {
                // 基本信息
                id: index + 1,
                assetName: result.originalAssetName || result.matchedText || result.assetName || '未识别',
                assetNameId: result.assetNameId ? String(result.assetNameId) : null,
                amount,

                // OCR 附加信息
                originalAssetName: result.originalAssetName,
                matchedAssetName: result.matchedAssetName || result.assetName,
                matchScore,
                confidence,
                isMatched: result.isMatched,
                boundingBox: result.boundingBox,

                // 计算的质量信息
                qualityScore: calculateQualityScore(matchScore, confidence),
                hasWarnings: hasQualityWarnings(matchScore, confidence, amount),

                // 扩展信息
                cleanedAssetName: result.cleanedAssetName,
                recognitionTime: result.recognitionTime,
                acquireTime: result.acquireTime
            }
        })
        : []
}

/**
 * 评分：平均值
 */
function calculateQualityScore(
    matchScore: number | null | undefined,
    confidence: number | null | undefined
): number {
    const scores: number[] = []

    if (typeof matchScore === 'number') scores.push(matchScore)
    if (typeof confidence === 'number') scores.push(confidence)

    return scores.length > 0 ? scores.reduce((a, b) => a + b, 0) / scores.length : 0
}

/**
 * 判断是否有质量问题 (仅用于显示提示，不过滤数据)
 */
function hasQualityWarnings(
    matchScore: number | null | undefined,
    confidence: number | null | undefined,
    amount: number | null | undefined
): boolean {
    if (typeof matchScore === 'number' && matchScore < 0.7) return true
    if (typeof confidence === 'number' && confidence < 0.8) return true
    if (typeof amount !== 'number' || amount <= 0) return true
    return false
}

/**
 * 数据转换和警告收集 (不过滤任何数据)
 */
export function validateOcrResults(ocrResults: any[]): {
    valid: any[]
    invalid: any[]
    warnings: string[]
} {
    const valid: any[] = []
    const warnings: string[] = []

    ocrResults.forEach((result, index) => {
        const itemNumber = index + 1

        // 生成提示信息，但不过滤数据
        if (!result.assetNameId) {
            warnings.push(`第${itemNumber}条记录未匹配到资产名称`)
        }

        const matchScore = parseScore(result.matchScore)
        if (typeof matchScore === 'number') {
            if (matchScore < 0.3) {
                warnings.push(`第${itemNumber}条记录匹配度很低 (${(matchScore * 100).toFixed(1)}%)`)
            } else if (matchScore < 0.7) {
                warnings.push(`第${itemNumber}条记录匹配度较低 (${(matchScore * 100).toFixed(1)}%)`)
            }
        }

        const amount = parseAmount(result.amount)
        if (!amount || amount <= 0) {
            warnings.push(`第${itemNumber}条记录金额为空或无效`)
        }

        const confidence = parseScore(result.confidence)
        if (typeof confidence === 'number' && confidence < 0.8) {
            warnings.push(`第${itemNumber}条记录识别置信度较低 (${(confidence * 100).toFixed(1)}%)`)
        }

        // 所有数据都加入valid数组，不过滤
        valid.push(result)
    })

    return {
        valid,
        invalid: [], // 不再有无效数据
        warnings
    }
}

/**
 * 获取有效项目数量 (用于UI显示)
 * 有效定义：有资产名称ID 或 有金额
 */
export function getValidItemsCount(items: any[]): number {
    return items.filter(item => {
        const hasAssetNameId = item.assetNameId && item.assetNameId !== null
        const hasValidAmount = item.amount && item.amount > 0
        return hasAssetNameId || hasValidAmount
    }).length
}

/**
 * 获取可提交的项目 (用于批量添加)
 * 可提交条件：必须有资产名称ID
 */
export function getSubmittableItems(items: any[]): any[] {
    return items.filter(item => {
        return item.assetNameId && item.assetNameId !== null
    })
}

/**
 * 检查是否可以提交
 */
export function canSubmitItems(items: any[]): boolean {
    return getSubmittableItems(items).length > 0
}

/**
 * 获取高质量项目 (匹配度 >= 0.8)
 */
export function getHighQualityItems(items: any[]): any[] {
    return items.filter(item => {
        const matchScore = parseScore(item.matchScore)
        return typeof matchScore === 'number' && matchScore >= 0.8
    })
}

/**
 * 获取有金额的项目
 */
export function getItemsWithAmount(items: any[]): any[] {
    return items.filter(item => {
        const amount = parseAmount(item.amount)
        return amount > 0
    })
}

/**
 * 获取完整项目 (既有资产名称又有金额)
 */
export function getCompleteItems(items: any[]): any[] {
    return items.filter(item => {
        const hasAssetNameId = item.assetNameId && item.assetNameId !== null
        const hasValidAmount = item.amount && item.amount > 0
        return hasAssetNameId && hasValidAmount
    })
}

/**
 * 生成质量报告
 */
export function generateQualityReport(items: any[]): {
    total: number
    withAssetName: number
    withAmount: number
    withBoth: number
    highQuality: number
    mediumQuality: number
    lowQuality: number
    submittable: number
} {
    const total = items.length
    const withAssetName = items.filter(item => item.assetNameId).length
    const withAmount = getItemsWithAmount(items).length
    const withBoth = getCompleteItems(items).length
    const submittable = getSubmittableItems(items).length

    // 按质量分级
    const highQuality = items.filter(item => {
        const qualityScore = item.qualityScore || 0
        return qualityScore >= 0.8
    }).length

    const mediumQuality = items.filter(item => {
        const qualityScore = item.qualityScore || 0
        return qualityScore >= 0.5 && qualityScore < 0.8
    }).length

    const lowQuality = items.filter(item => {
        const qualityScore = item.qualityScore || 0
        return qualityScore < 0.5
    }).length

    return {
        total,
        withAssetName,
        withAmount,
        withBoth,
        highQuality,
        mediumQuality,
        lowQuality,
        submittable
    }
}

/**
 * 获取质量等级描述
 */
export function getQualityLevelDescription(qualityScore: number): {
    level: 'high' | 'medium' | 'low'
    text: string
    color: string
} {
    if (qualityScore >= 0.8) {
        return {
            level: 'high',
            text: '优秀',
            color: 'success'
        }
    } else if (qualityScore >= 0.5) {
        return {
            level: 'medium',
            text: '良好',
            color: 'warning'
        }
    } else {
        return {
            level: 'low',
            text: '较差',
            color: 'danger'
        }
    }
}

/**
 * 获取匹配度描述
 */
export function getMatchScoreDescription(matchScore: number | null): {
    text: string
    color: string
} {
    if (matchScore === null || matchScore === undefined) {
        return {
            text: '未匹配',
            color: 'secondary'
        }
    }

    if (matchScore >= 0.9) {
        return {
            text: '完美匹配',
            color: 'success'
        }
    } else if (matchScore >= 0.7) {
        return {
            text: '良好匹配',
            color: 'primary'
        }
    } else if (matchScore >= 0.5) {
        return {
            text: '一般匹配',
            color: 'warning'
        }
    } else {
        return {
            text: '匹配度低',
            color: 'danger'
        }
    }
}

/**
 * 格式化金额显示
 */
export function formatAmount(amount: number | null | undefined): string {
    if (typeof amount !== 'number' || amount <= 0) {
        return '-'
    }

    return amount.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    })
}

/**
 * 格式化百分比显示
 */
export function formatPercentage(value: number | null | undefined): string {
    if (typeof value !== 'number') {
        return '-'
    }

    return `${(value * 100).toFixed(1)}%`
}

/**
 * 导出数据到CSV (用于调试和数据分析)
 */
export function exportToCSV(items: any[], filename: string = 'ocr_results.csv'): void {
    const headers = [
        'ID', '资产名称', '金额', '匹配度', '置信度',
        '是否匹配', '质量分数', '原始名称', '匹配的资产名称'
    ]

    const rows = items.map(item => [
        item.id,
        item.assetName || '',
        formatAmount(item.amount),
        formatPercentage(item.matchScore),
        formatPercentage(item.confidence),
        item.isMatched ? '是' : '否',
        item.qualityScore?.toFixed(3) || '0',
        item.originalAssetName || '',
        item.matchedAssetName || ''
    ])

    const csvContent = [headers, ...rows]
        .map(row => row.map(cell => `"${cell}"`).join(','))
        .join('\n')

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)

    link.setAttribute('href', url)
    link.setAttribute('download', filename)
    link.style.visibility = 'hidden'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
}

/**
 * 统计警告类型
 */
export function categorizeWarnings(warnings: string[]): {
    noAssetName: number
    lowMatchScore: number
    noAmount: number
    lowConfidence: number
    total: number
} {
    const stats = {
        noAssetName: 0,
        lowMatchScore: 0,
        noAmount: 0,
        lowConfidence: 0,
        total: warnings.length
    }

    warnings.forEach(warning => {
        if (warning.includes('未匹配到资产名称')) {
            stats.noAssetName++
        } else if (warning.includes('匹配度')) {
            stats.lowMatchScore++
        } else if (warning.includes('金额为空')) {
            stats.noAmount++
        } else if (warning.includes('置信度较低')) {
            stats.lowConfidence++
        }
    })

    return stats
}

/**
 * 生成处理建议
 */
export function generateProcessingSuggestions(items: any[]): string[] {
    const suggestions: string[] = []
    const report = generateQualityReport(items)

    if (report.total === 0) {
        suggestions.push('没有识别到任何数据，请检查图片质量或重新上传')
        return suggestions
    }

    if (report.submittable === 0) {
        suggestions.push('没有可提交的数据，建议手动匹配资产名称或重新识别')
    } else if (report.submittable < report.total * 0.5) {
        suggestions.push('可提交数据较少，建议检查资产名称库是否完整')
    }

    if (report.withAmount < report.total * 0.8) {
        suggestions.push('部分记录缺少金额信息，建议检查图片中的金额区域')
    }

    if (report.lowQuality > report.total * 0.3) {
        suggestions.push('识别质量较低，建议使用更清晰的图片或调整图片角度')
    }

    if (report.highQuality > report.total * 0.8) {
        suggestions.push('识别质量优秀，可以直接提交数据')
    }

    return suggestions
}

// 默认导出所有主要函数
export default {
    convertOcrResultToAssetRecord,
    convertOcrResultsToAssetRecords,
    convertToRecognizedAssetItems,
    validateOcrResults,
    getValidItemsCount,
    getSubmittableItems,
    canSubmitItems,
    getHighQualityItems,
    getItemsWithAmount,
    getCompleteItems,
    generateQualityReport,
    getQualityLevelDescription,
    getMatchScoreDescription,
    formatAmount,
    formatPercentage,
    exportToCSV,
    categorizeWarnings,
    generateProcessingSuggestions
}