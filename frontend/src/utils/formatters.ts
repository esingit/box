// src/utils/formatters.ts

/**
 * 金额格式化：保留两位小数
 */
export function formatAmount(amount: number | null | undefined): string {
    if (amount === null || amount === undefined || isNaN(amount)) return '0.00'
    return new Intl.NumberFormat('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(amount)
}

/**
 * 显示符号前缀：仅正数加“+”
 */
export function getChangePrefix(change: number): string {
    return change > 0 ? '+' : ''
}

/**
 * 改变颜色样式类名（资产为正绿负红，负债反转）
 */
export function getChangeClass(change: number, isLiability = false): string {
    if (isLiability) return change > 0 ? 'negative' : change < 0 ? 'positive' : ''
    return change > 0 ? 'positive' : change < 0 ? 'negative' : ''
}

/**
 * 日期格式化为 YYYY-MM-DD
 */
export function formatDate(dateStr: string | null | undefined): string {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return '-'
    return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`
}

/**
 * 日期格式化为 YYYY-MM-DDT00:00:00
 */
export function formatTime(data: any) {
    const acquireTime = data?.acquireTime
    if (typeof acquireTime !== 'string' || acquireTime.length === 0) {
        return { ...data, acquireTime: '' }
    }
    return {
        ...data,
        acquireTime: acquireTime.includes('T') ? acquireTime : acquireTime + 'T00:00:00'
    }
}
