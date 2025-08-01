import { ComputedRef } from 'vue'
import { FormattedFitnessRecord } from '@/types/fitness'
import { Option } from '@/types/common'

interface UseFitnessStatsOptions {
    filteredRecords: ComputedRef<FormattedFitnessRecord[]>
    fitnessTypeOptions: Option[]
    safeComputed: <T>(getter: () => T, defaultValue: T) => ComputedRef<T>
}

import { ID } from '@/types/base'

interface UseFitnessStatsReturn {
    isExerciseType: (typeId: ID | undefined) => boolean
    exerciseDaysCount: ComputedRef<number>
    pushUpCount: ComputedRef<number>
    proteinCount: ComputedRef<number>
}

export function useFitnessStats(options: UseFitnessStatsOptions): UseFitnessStatsReturn {
    const { filteredRecords, fitnessTypeOptions, safeComputed } = options

    // 添加调试代码
    console.log('🔍 useFitnessStats 接收到的参数:', {
        filteredRecordsLength: filteredRecords.value.length,
        fitnessTypeOptionsLength: fitnessTypeOptions?.length || 0
    })

    console.log('🔍 fitnessTypeOptions 详情:', fitnessTypeOptions?.map(option => ({
        id: option.id || option.value,
        value: option.value,
        label: option.label,
        key1: option.key1,
        key2: option.key2,
        value1: option.value1
    })))

    // 使用名称判断是否为运动类型
    function isExerciseType(typeId: ID | undefined): boolean {
        if (!typeId) return false

        console.log(`🔍 检查类型ID ${typeId} 是否为运动类型`)

        const fitnessType = fitnessTypeOptions?.find(type =>
            String(type.value) === String(typeId) ||
            String(type.id) === String(typeId)
        )

        if (!fitnessType) {
            console.log(`  未找到匹配的类型定义`)
            return false
        }

        console.log(`  找到类型: ${JSON.stringify(fitnessType)}`)

        // 首先尝试使用 key2 字段
        if (fitnessType.key2 === 'EXERCISE') {
            console.log(`  通过 key2='EXERCISE' 判定为运动类型`)
            return true
        }

        // 然后尝试使用名称判断
        const typeName = (fitnessType.value1 || fitnessType.label || '').toLowerCase()
        const exerciseKeywords = [
            '俯卧撑', '仰卧起坐', '深蹲', '跑步', '游泳', '骑行', '举重', '瑜伽',
            '健身', '运动', '锻炼', '训练', 'push', 'sit', 'squat', 'run', 'swim',
            'cycle', 'weight', 'yoga', 'exercise', 'workout', 'training'
        ]

        const isExercise = exerciseKeywords.some(keyword => typeName.includes(keyword))
        console.log(`  通过名称 "${typeName}" 判定${isExercise ? '为' : '不是'}运动类型`)
        return isExercise
    }

    // 使用名称判断是否为俯卧撑
    function isPushUpType(typeId: string | number | undefined): boolean {
        if (!typeId) return false

        console.log(`🔍 检查类型ID ${typeId} 是否为俯卧撑类型`)

        const fitnessType = fitnessTypeOptions?.find(type =>
            String(type.value) === String(typeId) ||
            String(type.id) === String(typeId)
        )

        if (!fitnessType) {
            console.log(`  未找到匹配的类型定义`)
            return false
        }

        console.log(`  找到类型: ${JSON.stringify(fitnessType)}`)

        // 首先尝试使用 key1 字段
        if (fitnessType.key1 === 'PUSH_UP') {
            console.log(`  通过 key1='PUSH_UP' 判定为俯卧撑类型`)
            return true
        }

        // 然后尝试使用名称判断
        const typeName = (fitnessType.value1 || fitnessType.label || '').toLowerCase()
        const isPushUp = typeName.includes('俯卧撑') || typeName.includes('push')
        console.log(`  通过名称 "${typeName}" 判定${isPushUp ? '为' : '不是'}俯卧撑类型`)
        return isPushUp
    }

    // 使用名称判断是否为蛋白质
    function isProteinType(typeId: string | number | undefined): boolean {
        if (!typeId) return false

        console.log(`🔍 检查类型ID ${typeId} 是否为蛋白质类型`)

        const fitnessType = fitnessTypeOptions?.find(type =>
            String(type.value) === String(typeId) ||
            String(type.id) === String(typeId)
        )

        if (!fitnessType) {
            console.log(`  未找到匹配的类型定义`)
            return false
        }

        console.log(`  找到类型: ${JSON.stringify(fitnessType)}`)

        // 首先尝试使用 key1 字段
        if (fitnessType.key1 === 'PROTEIN') {
            console.log(`  通过 key1='PROTEIN' 判定为蛋白质类型`)
            return true
        }

        // 然后尝试使用名称判断
        const typeName = (fitnessType.value1 || fitnessType.label || '').toLowerCase()
        const isProtein = typeName.includes('蛋白') || typeName.includes('protein')
        console.log(`  通过名称 "${typeName}" 判定${isProtein ? '为' : '不是'}蛋白质类型`)
        return isProtein
    }

    const exerciseDaysCount = safeComputed(() => {
        if (!filteredRecords.value.length) return 0

        console.log('🔍 计算运动天数...')
        console.log(`  过滤后记录数: ${filteredRecords.value.length}`)

        const exerciseDays = new Set<string>()
        for (const record of filteredRecords.value) {
            if (isExerciseType(record.typeId) && record.finishTime) {
                exerciseDays.add(record.finishTime.split('T')[0])
            }
        }

        console.log(`🔍 运动天数统计结果: ${exerciseDays.size}`)
        console.log(`  运动日期: ${Array.from(exerciseDays).join(', ')}`)

        return exerciseDays.size
    }, 0)

    const pushUpCount = safeComputed(() => {
        if (!filteredRecords.value.length || !fitnessTypeOptions?.length) return 0

        console.log('🔍 计算俯卧撑总数...')
        console.log(`  过滤后记录数: ${filteredRecords.value.length}`)

        let sum = 0
        let matchCount = 0
        for (const record of filteredRecords.value) {
            if (isPushUpType(record.typeId)) {
                const count = Number(record.count || 0)
                sum += count
                matchCount++
                console.log(`  找到俯卧撑记录: ID=${record.id}, 类型=${record.typeId}, 数量=${count}`)
            }
        }

        console.log(`🔍 俯卧撑统计结果: ${sum} (匹配记录数: ${matchCount})`)
        return sum
    }, 0)

    const proteinCount = safeComputed(() => {
        if (!filteredRecords.value.length || !fitnessTypeOptions?.length) return 0

        console.log('🔍 计算蛋白质汇总...')
        console.log(`  过滤后记录数: ${filteredRecords.value.length}`)

        let sum = 0
        let matchCount = 0
        for (const record of filteredRecords.value) {
            if (isProteinType(record.typeId)) {
                const count = Number(record.count || 0)
                sum += count
                matchCount++
                console.log(`  找到蛋白质记录: ID=${record.id}, 类型=${record.typeId}, 数量=${count}`)
            }
        }

        console.log(`🔍 蛋白质统计结果: ${sum} (匹配记录数: ${matchCount})`)
        return sum
    }, 0)

    return {
        isExerciseType,
        exerciseDaysCount,
        pushUpCount,
        proteinCount
    }
}