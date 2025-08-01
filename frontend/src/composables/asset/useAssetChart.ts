import { computed, ComputedRef } from 'vue'
import type { EChartsCoreOption } from 'echarts/core'
import type { AssetChartOptions } from '@/types/asset'
import { useAssetUtils } from './useAssetUtils'
import { CHART_COLORS } from './useAssetConstants'

interface UseAssetChartOptions {
    hasData: ComputedRef<boolean>
    allDates: ComputedRef<string[]>
    formattedDates: ComputedRef<string[]>
    amountByDimension: ComputedRef<{
        byName: Record<string, Record<string, number>>
        byType: Record<string, Record<string, number>>
        byLocation: Record<string, Record<string, number>>
    }>
    totalAmountByDate: ComputedRef<Record<string, number>>
    dateRangeDisplay: ComputedRef<string>
    lastDateWithRecords: ComputedRef<string>
    statisticsData: ComputedRef<{ unitSymbol: string }>
    chartOptions: AssetChartOptions
}

export function useAssetChart(options: UseAssetChartOptions) {
    const {
        hasData,
        allDates,
        formattedDates,
        amountByDimension,
        totalAmountByDate,
        dateRangeDisplay,
        lastDateWithRecords,
        statisticsData,
        chartOptions
    } = options

    const { formatRawAmountWithUnit, formatYAxisAmount } = useAssetUtils()

    function createSeriesData(dataMap: Record<string, Record<string, number>>, keys: string[]): Array<{ name: string; data: number[] }> {
        return keys.map(key => ({
            name: key,
            data: allDates.value.map(date => dataMap[key]?.[date] ?? 0)
        }))
    }

    // 计算合适的Y轴范围，让数据起伏更明显
    function calculateYAxisRange(allValues: number[]) {
        if (allValues.length === 0) return { min: 0, max: 100 }

        const validValues = allValues.filter(v => !isNaN(v) && v > 0)
        if (validValues.length === 0) return { min: 0, max: 100 }

        const minValue = Math.min(...validValues)
        const maxValue = Math.max(...validValues)

        // 如果最大值和最小值差距很小，扩大显示范围
        const range = maxValue - minValue
        const avgValue = (maxValue + minValue) / 2

        let yMin: number, yMax: number

        if (range === 0) {
            // 所有值相同的情况
            yMin = Math.max(0, minValue * 0.9)
            yMax = maxValue * 1.1
        } else if (range / avgValue < 0.1) {
            // 变化幅度小于平均值的10%，扩大显示范围
            const expandedRange = avgValue * 0.2 // 扩大到平均值的20%
            yMin = Math.max(0, minValue - expandedRange / 2)
            yMax = maxValue + expandedRange / 2
        } else {
            // 正常情况，适当留白
            const padding = range * 0.1
            yMin = Math.max(0, minValue - padding)
            yMax = maxValue + padding
        }

        return { min: yMin, max: yMax }
    }

    const chartSeries = computed(() => {
        if (!hasData.value || !allDates.value.length) return []

        const series: any[] = []
        let colorIndex = 0
        const { byName, byType, byLocation } = amountByDimension.value

        try {
            // 总金额趋势线
            if (chartOptions.showTotalTrend) {
                const totalData = allDates.value.map(date => totalAmountByDate.value[date] ?? 0)
                if (totalData.some(v => v > 0)) {
                    series.push({
                        name: '📈 总金额趋势',
                        type: 'line',
                        smooth: true,
                        symbol: 'circle',
                        symbolSize: 8,
                        data: totalData,
                        lineStyle: {
                            width: 4,
                            color: '#4A5568',
                            shadowColor: 'rgba(74, 85, 104, 0.3)',
                            shadowBlur: 4
                        },
                        itemStyle: {
                            color: '#4A5568',
                            borderWidth: 2,
                            borderColor: '#fff'
                        },
                        emphasis: {
                            focus: 'series',
                            scale: true
                        },
                        z: 10
                    })
                }
                colorIndex++
            }

            // 按维度添加系列
            const dimensionConfigs = [
                { condition: chartOptions.showNameDimension, data: byName, prefix: '💰', symbol: 'circle', lineType: 'solid' },
                { condition: chartOptions.showTypeDimension, data: byType, prefix: '🏷️', symbol: 'triangle', lineType: 'dashed' },
                { condition: chartOptions.showLocationDimension, data: byLocation, prefix: '📍', symbol: 'diamond', lineType: 'dotted' }
            ]

            dimensionConfigs.forEach(config => {
                if (config.condition && config.data) {
                    const keys = Object.keys(config.data)
                    const seriesData = createSeriesData(config.data, keys)
                    seriesData.forEach((item, index) => {
                        if (item.data.some(v => v > 0)) {
                            const color = CHART_COLORS[(colorIndex + index) % CHART_COLORS.length]
                            series.push({
                                name: `${config.prefix} ${item.name}`,
                                type: 'line',
                                smooth: true,
                                symbol: config.symbol,
                                symbolSize: 5,
                                data: item.data,
                                lineStyle: {
                                    width: 2,
                                    type: config.lineType,
                                    color,
                                    shadowColor: `${color}33`,
                                    shadowBlur: 2
                                },
                                itemStyle: {
                                    color,
                                    borderWidth: 1,
                                    borderColor: '#fff'
                                },
                                emphasis: {
                                    focus: 'series'
                                }
                            })
                        }
                    })
                    colorIndex += seriesData.length
                }
            })

            return series
        } catch (error) {
            console.error('Error generating chart series:', error)
            return []
        }
    })

    const echartConfig = computed(() => {
        if (!hasData.value || !chartSeries.value.length || !allDates.value.length) return null

        try {
            const hasMultipleDates = allDates.value.length > 7
            const allValues = chartSeries.value.flatMap(s => s.data || []).filter(v => !isNaN(v))
            const { min: yMin, max: yMax } = calculateYAxisRange(allValues)

            return {
                title: {
                    text: '资产金额趋势分析',
                    subtext: `统计期间: ${dateRangeDisplay.value} | 汇总基准: ${lastDateWithRecords.value}`, // 去掉了 | 前的符号
                    left: 'center',
                    top: 15,
                    textStyle: {
                        fontSize: 18,
                        fontWeight: 'bold',
                        color: '#2D3748'
                    },
                    subtextStyle: {
                        fontSize: 12,
                        color: '#718096'
                    }
                },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'cross',
                        label: {
                            backgroundColor: '#718096'
                        }
                    },
                    backgroundColor: 'rgba(255, 255, 255, 0.96)',
                    borderColor: '#E2E8F0',
                    borderWidth: 1,
                    borderRadius: 8,
                    textStyle: {
                        color: '#2D3748'
                    },
                    extraCssText: 'box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);',
                    formatter: (params: any[]) => {
                        if (!Array.isArray(params)) return ''

                        const dataIndex = params[0]?.dataIndex
                        const date = allDates.value[dataIndex] || ''
                        const unitSymbol = statisticsData.value.unitSymbol

                        let result = `<div style="font-weight: bold; margin-bottom: 8px; color: #1A202C">${date}</div>`

                        const groupedParams = {
                            total: params.filter(p => p.seriesName.includes('总金额')),
                            name: params.filter(p => p.seriesName.includes('💰')),
                            type: params.filter(p => p.seriesName.includes('🏷️')),
                            location: params.filter(p => p.seriesName.includes('📍'))
                        }

                        Object.entries(groupedParams).forEach(([key, series]) => {
                            if (series.length > 0) {
                                const titles = {
                                    total: '💰 总计',
                                    name: '📊 按资产名称',
                                    type: '🏷️ 按资产类型',
                                    location: '📍 按资产位置'
                                }
                                result += `<div style="margin-top: 8px; font-weight: 600; color: #4A5568; font-size: 13px">${titles[key as keyof typeof titles]}</div>`
                                series.forEach(item => {
                                    if (item.value > 0) {
                                        const formattedAmount = formatRawAmountWithUnit(item.value, unitSymbol)
                                        result += `<div style="display: flex; align-items: center; gap: 8px; margin-top: 4px">
                      <span style="display: inline-block; width: 8px; height: 8px; background: ${item.color}; border-radius: 50%"></span>
                      <span>${item.seriesName.replace(/[💰🏷️📍📈]/g, '').trim()}: <strong>${formattedAmount}</strong></span>
                    </div>`
                                    }
                                })
                            }
                        })

                        return result
                    }
                },
                legend: {
                    type: 'scroll',
                    orient: 'horizontal',
                    bottom: hasMultipleDates ? 60 : 15,
                    data: chartSeries.value.map(s => s.name),
                    textStyle: {
                        fontSize: 11,
                        color: '#4A5568'
                    }
                },
                grid: {
                    left: 100,
                    right: 50,
                    bottom: hasMultipleDates ? 120 : 80,
                    top: 80,
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: formattedDates.value,
                    boundaryGap: false,
                    axisLabel: {
                        fontSize: 11,
                        color: '#718096',
                        interval: 0,
                        rotate: hasMultipleDates ? 45 : 0
                    },
                    axisLine: {
                        lineStyle: {
                            color: '#CBD5E0'
                        }
                    },
                    axisTick: {
                        alignWithLabel: true,
                        lineStyle: {
                            color: '#CBD5E0'
                        }
                    }
                },
                yAxis: {
                    type: 'value',
                    name: '金额',
                    nameTextStyle: {
                        fontSize: 12,
                        color: '#718096'
                    },
                    min: yMin, // 使用计算的最小值
                    max: yMax, // 使用计算的最大值
                    axisLabel: {
                        fontSize: 11,
                        color: '#718096',
                        formatter: formatYAxisAmount
                    },
                    splitLine: {
                        lineStyle: {
                            type: 'dashed',
                            color: '#E2E8F0'
                        }
                    },
                    axisLine: {
                        show: false
                    },
                    axisTick: {
                        show: false
                    },
                    minInterval: 1
                },
                series: chartSeries.value,
                dataZoom: hasMultipleDates ? [
                    {
                        type: 'inside',
                        start: 0,
                        end: 100
                    },
                    {
                        type: 'slider',
                        show: true,
                        start: 0,
                        end: 100,
                        height: 20,
                        bottom: 25
                    }
                ] : undefined,
                animation: true,
                animationDuration: 600,
                animationEasing: 'cubicOut'
            } as EChartsCoreOption
        } catch (error) {
            console.error('Error generating chart config:', error)
            return null
        }
    })

    return {
        chartSeries,
        echartConfig
    }
}