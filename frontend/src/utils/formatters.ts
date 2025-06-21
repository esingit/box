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
 * 支持 Date、字符串、时间戳
 */
export function formatDate(dateInput: string | number | Date | null | undefined): string {
    if (!dateInput) return '-'

    const d = new Date(dateInput)
    if (isNaN(d.getTime())) return '-'

    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}


/**
 * 日期格式化为 YYYY-MM-DDT00:00:00
 */
export function formatTime(data: any) {
    if (!data || typeof data !== 'object') return data;

    const result = { ...data };

    Object.keys(result).forEach((key) => {
        if (key.toLowerCase().includes('time')) {
            const val = result[key];
            if (typeof val !== 'string' || val.length === 0) {
                result[key] = '';
            } else if (!val.includes('T')) {
                result[key] = val + 'T00:00:00';
            }
        }
    });
    return result;
}

// 日期范围查询格式化
export function joinRangeDates(start: string, end: string): string {
    if (start && end) {
        return `${start} ~ ${end}`
    } else if (start) {
        return start
    } else if (end) {
        return end
    }
    return ''
}

export function splitRangeDates(rangeStr: string): { start: string; end: string } {
    if (!rangeStr) return { start: '', end: '' }
    const parts = rangeStr.split('~').map(v => v.trim())
    if (parts.length === 2) {
        return { start: parts[0], end: parts[1] }
    } else if (parts.length === 1) {
        return { start: parts[0], end: parts[0] }
    }
    return { start: '', end: '' }
}


