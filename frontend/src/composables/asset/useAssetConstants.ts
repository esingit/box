export const CHART_OPTIONS_STORAGE_KEY = 'asset_chart_options'

export const CHART_OPTIONS_CONFIG = [
    { key: 'showTotalTrend', label: '总金额趋势' },
    { key: 'showNameDimension', label: '按资产名称' },
    { key: 'showTypeDimension', label: '按资产类型' },
    { key: 'showLocationDimension', label: '按资产位置' }
] as const

export const CHART_COLORS = [
    '#6B7F96', '#8D9C8D', '#B19C7D', '#A88080', '#8C7BA8', '#9E8C9E', '#7B9E9E', '#B8936B',
    '#7B9DB8', '#9BB87B', '#B87B9D', '#7B7BB8', '#8B9B8B', '#B8898B', '#89B8B8', '#A8A87B',
    '#9E7B8C', '#7B8C9E', '#A8937B', '#8C8C7B'
]

export const ASSET_TYPE_KEYS = {
    SAVINGS: 'SAVINGS',
    FINANCE: 'FINANCE',
    FUND: 'FUND',
    DEBT: 'DEBT'
} as const

export const CURRENCY_SYMBOLS = ['￥', 'CNY', '人民币', 'RMB']
export const DEFAULT_CURRENCY = '¥'