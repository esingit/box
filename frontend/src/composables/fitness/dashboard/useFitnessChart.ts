import {ComputedRef, reactive, Ref} from 'vue'
import {FormattedFitnessRecord} from '@/types/fitness'
import {Option} from '@/types/common'

const CHART_COLORS = [
    '#6B7F96', '#8D9C8D', '#B19C7D', '#A88080', '#8C7BA8', '#9E8C9E',
    '#7B9E9E', '#B8936B', '#7B9DB8', '#9BB87B', '#B87B9D', '#7B7BB8',
    '#8B9B8B', '#B8898B', '#89B8B8', '#A8A87B', '#9E7B8C', '#7B8C9E'
]

interface ChartOptions {
    showDataLabels: boolean
    showAreaFill: boolean
    smoothCurve: boolean
}

interface UseFitnessChartOptions {
    filteredRecords: ComputedRef<FormattedFitnessRecord[]>
    fitnessTypeOptions: Option[]
    unitOptions: Option[]
    query: Ref<any>
    allDates: ComputedRef<string[]>
    formattedDates: ComputedRef<string[]>
    effectiveTypeIds: ComputedRef<(string | number)[]>
    dateRangeDisplay: ComputedRef<string>
    hasData: ComputedRef<boolean>
    safeComputed: <T>(getter: () => T, defaultValue: T) => ComputedRef<T>
}

export function useFitnessChart(options: UseFitnessChartOptions) {
    const {
        filteredRecords,
        fitnessTypeOptions,
        unitOptions,
        query,
        allDates,
        formattedDates,
        dateRangeDisplay,
        hasData,
        safeComputed
    } = options

    const chartOptions = reactive<ChartOptions>({
        showDataLabels: false,
        showAreaFill: true,
        smoothCurve: true
    })

    // 单位映射
    const unitMapping = safeComputed(() => {
        const map: Record<string, string> = {}
        if (!unitOptions?.length) return map

        for (const option of unitOptions) {
            if (option) {
                if (option.id && option.value1) {
                    map[String(option.id)] = option.value1
                }
                if (option.value && option.value1) {
                    map[String(option.value)] = option.value1
                }
            }
        }
        return map
    }, {})

    function getDefaultUnitForType(typeId: string | number): string {
        const fitnessType = fitnessTypeOptions?.find(type =>
            String(type.value) === String(typeId) || String(type.id) === String(typeId)
        )

        if (!fitnessType?.key3) {
            return ''
        }

        const defaultUnit = unitOptions?.find(unit => unit.key1 === fitnessType.key3)
        if (!defaultUnit) {
            return ''
        }

        return defaultUnit.value1 || ''
    }

    function getRecordUnit(typeId: string | number, date: string): string {
        const records = filteredRecords.value.filter(record =>
            record &&
            String(record.typeId) === String(typeId) &&
            record.finishTime?.startsWith(date)
        )

        if (records.length > 0 && records[0].unitId) {
            const unitId = String(records[0].unitId)
            const unitName = unitMapping.value[unitId]
            if (unitName) {
                return unitName
            }
        }

        return getDefaultUnitForType(typeId)
    }

    function formatValue(value: number): string {
        if (value === 0) return '0'

        if (value >= 1000) {
            return `${(value / 1000).toFixed(1)}k`
        } else if (value >= 100) {
            return value.toFixed(0)
        } else if (value >= 10) {
            return value.toFixed(1)
        } else {
            return value.toFixed(2)
        }
    }

    function formatValueWithUnit(value: number, typeId: string | number, date: string): string {
        if (value === 0) return '0'

        const unit = getRecordUnit(typeId, date)

        let formattedValue: string
        if (value >= 1000) {
            formattedValue = `${(value / 1000).toFixed(1)}k`
        } else if (value >= 100) {
            formattedValue = value.toFixed(0)
        } else if (value >= 10) {
            formattedValue = value.toFixed(1)
        } else {
            formattedValue = value.toFixed(2)
        }

        return unit ? `${formattedValue}${unit}` : formattedValue
    }

    // 图表系列数据
    const chartSeries = safeComputed(() => {
        if (!hasData.value || !allDates.value.length) return []

        try {
            const dateDataCache = new Map<string, Map<string, number>>()
            const actualTypeIds = new Set<string>()

            for (const record of filteredRecords.value) {
                if (record?.finishTime && record?.typeId) {
                    const date = record.finishTime.split('T')[0]
                    const typeId = String(record.typeId)

                    actualTypeIds.add(typeId)

                    if (!dateDataCache.has(date)) {
                        dateDataCache.set(date, new Map())
                    }

                    const typeMap = dateDataCache.get(date)!
                    const currentValue = typeMap.get(typeId) || 0
                    typeMap.set(typeId, currentValue + Number(record.count || 0))
                }
            }

            let typeIdsToShow: (string | number)[]

            if (query?.value?.typeIdList?.length > 0) {
                typeIdsToShow = query.value.typeIdList.filter((id: number) =>
                    actualTypeIds.has(String(id))
                )
            } else {
                typeIdsToShow = Array.from(actualTypeIds)
            }

            return typeIdsToShow
                .map((typeId, index) => {
                    const typeOption = fitnessTypeOptions?.find(item =>
                        String(item.value) === String(typeId) ||
                        String(item.id) === String(typeId)
                    )
                    const typeName = typeOption?.value1 || typeOption?.label || `类型${typeId}`

                    const data = allDates.value.map(date => {
                        const typeMap = dateDataCache.get(date)
                        return typeMap?.get(String(typeId)) || 0
                    })

                    if (!data.some(value => value > 0)) {
                        return null
                    }

                    const color = CHART_COLORS[index % CHART_COLORS.length]

                    return {
                        name: typeName,
                        type: 'line',
                        data,
                        typeId,
                        smooth: chartOptions.smoothCurve,
                        symbol: 'circle',
                        symbolSize: 6,
                        lineStyle: {
                            width: 2,
                            color,
                            shadowColor: `${color}33`,
                            shadowBlur: 2
                        },
                        itemStyle: {
                            color,
                            borderWidth: 1,
                            borderColor: '#fff'
                        },
                        areaStyle: chartOptions.showAreaFill ? {
                            color: `${color}26`
                        } : undefined,
                        label: chartOptions.showDataLabels ? {
                            show: true,
                            fontSize: 10,
                            color: '#666',
                            position: 'top',
                            formatter: (params: any) => {
                                const {value, dataIndex} = params
                                if (value <= 0) return ''
                                const date = allDates.value[dataIndex]
                                return formatValueWithUnit(value, typeId, date)
                            }
                        } : undefined,
                        emphasis: {
                            focus: 'series',
                            scale: true
                        }
                    }
                })
                .filter(Boolean)
        } catch (error) {
            console.error('Error generating chart series:', error)
            return []
        }
    }, [])

    // ECharts 配置
    const echartConfig = safeComputed(() => {
        if (!hasData.value || !chartSeries.value.length || !allDates.value.length) {
            return null
        }

        try {
            const hasMultipleDates = allDates.value.length > 7

            return {
                title: {
                    text: '健身统计趋势',
                    subtext: `统计期间: ${dateRangeDisplay.value}`,
                    left: 'center',
                    top: 15,
                    textStyle: {
                        fontSize: 16,
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
                        if (!Array.isArray(params) || params.length === 0) return ''

                        const dataIndex = params[0]?.dataIndex
                        const date = allDates.value[dataIndex] || ''
                        let result = `<div style="font-weight: bold; margin-bottom: 8px; color: #1A202C">${date}</div>`

                        params.forEach((param) => {
                            if (param.value > 0) {
                                const series = chartSeries.value.find((s: any) => s && s.name === param.seriesName)
                                if (series && series.typeId !== undefined) {
                                    const typeId = series.typeId as string | number
                                    const unit = getRecordUnit(typeId, date)
                                    const formattedValue = formatValue(param.value)
                                    const displayValue = unit ? `${formattedValue}${unit}` : formattedValue

                                    result += `<div style="display: flex; align-items: center; gap: 6px; margin-top: 3px">
                    <span style="display: inline-block; width: 8px; height: 8px; background: ${param.color}; border-radius: 50%"></span>
                    <span>${param.seriesName}: <strong>${displayValue}</strong></span>
                  </div>`
                                } else {
                                    const formattedValue = formatValue(param.value)
                                    result += `<div style="display: flex; align-items: center; gap: 6px; margin-top: 3px">
                    <span style="display: inline-block; width: 8px; height: 8px; background: ${param.color}; border-radius: 50%"></span>
                    <span>${param.seriesName}: <strong>${formattedValue}</strong></span>
                  </div>`
                                }
                            }
                        })

                        return result
                    }
                },
                legend: {
                    type: 'scroll',
                    orient: 'horizontal',
                    bottom: hasMultipleDates ? 60 : 15,
                    data: chartSeries.value.map((s: any) => s?.name || '').filter(Boolean),
                    textStyle: {
                        fontSize: 12,
                        color: '#4A5568'
                    }
                },
                grid: {
                    left: 80,
                    right: 40,
                    top: 80,
                    bottom: hasMultipleDates ? 120 : 80,
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
                    name: '数值',
                    nameTextStyle: {
                        fontSize: 12,
                        color: '#718096'
                    },
                    axisLabel: {
                        fontSize: 11,
                        color: '#718096',
                        formatter: (value: number) => {
                            if (value >= 1000) {
                                return `${(value / 1000).toFixed(1)}k`
                            } else if (value >= 100) {
                                return value.toFixed(0)
                            } else {
                                return value.toFixed(1)
                            }
                        }
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
                    minInterval: 1,
                    min: 0
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
            }
        } catch (error) {
            console.error('Error generating chart config:', error)
            return null
        }
    }, null)

    return {
        chartOptions,
        echartConfig,
        chartSeries
    }
}