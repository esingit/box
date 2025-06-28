import {CURRENCY_SYMBOLS, DEFAULT_CURRENCY} from './useAssetConstants'

export function useAssetUtils() {
    function debounce<T extends (...args: any[]) => any>(func: T, wait: number): (...args: Parameters<T>) => void {
        let timeout: ReturnType<typeof setTimeout>
        return (...args: Parameters<T>) => {
            clearTimeout(timeout)
            timeout = setTimeout(() => func(...args), wait)
        }
    }

    function createMapping(options: any[], valueKey = 'value1', fallbackKey = 'label'): Record<string, string> {
        const map: Record<string, string> = {}
        if (!options?.length) return map

        options.forEach(option => {
            if (option) {
                const id = option.id || option.value
                const name = option[valueKey] || option.name || option[fallbackKey]
                if (id && name) {
                    map[String(id)] = String(name)
                }
            }
        })
        return map
    }

    function normalizeUnitSymbol(unitSymbol: string): string {
        // 添加调试
        console.log('🔍 原始货币符号:', unitSymbol)

        // 如果是空字符串、undefined、null或者只是一个"-"，返回默认货币
        if (!unitSymbol || unitSymbol.trim() === '' || unitSymbol === '-') {
            console.log('🔍 使用默认货币符号:', DEFAULT_CURRENCY)
            return DEFAULT_CURRENCY
        }

        const normalized = CURRENCY_SYMBOLS.includes(unitSymbol) ? DEFAULT_CURRENCY : unitSymbol
        console.log('🔍 标准化后的货币符号:', normalized)
        return normalized
    }

    function formatRawAmountWithUnit(amount: number, unitSymbol = DEFAULT_CURRENCY): string {
        if (amount === 0) return `${normalizeUnitSymbol(unitSymbol)}0.00`

        const normalizedSymbol = normalizeUnitSymbol(unitSymbol)
        const formattedAmount = amount.toLocaleString('zh-CN', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2
        })

        return `${normalizedSymbol}${formattedAmount}`
    }

    function formatYAxisAmount(value: number): string {
        if (value === 0) return '¥0'
        if (value >= 100000000) {
            return `¥${(value / 100000000).toFixed(1)}亿`
        } else if (value >= 10000) {
            return `¥${(value / 10000).toFixed(1)}万`
        } else {
            return `¥${value.toLocaleString('zh-CN')}`
        }
    }

    function getDisplayName(id: string, mapping: Record<string, string>, fallback?: string | null, prefix = '未知'): string {
        return mapping[id] || fallback || `${prefix}${id}`
    }

    function safeParseAmount(amount: string | number | null | undefined): number {
        const parsed = parseFloat(String(amount || '0'))
        return isNaN(parsed) ? 0 : parsed
    }

    return {
        debounce,
        createMapping,
        normalizeUnitSymbol,
        formatRawAmountWithUnit,
        formatYAxisAmount,
        getDisplayName,
        safeParseAmount
    }
}