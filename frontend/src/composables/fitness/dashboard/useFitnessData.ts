import { computed, ComputedRef, Ref } from 'vue'
import { FormattedFitnessRecord } from '@/types/fitness'
import { Option } from '@/types/common'

type FitnessRecord = FormattedFitnessRecord

interface UseFitnessDataOptions {
    query: Ref<any>
    allLoadedRecords: Ref<FitnessRecord[]>
    fitnessTypeOptions: Option[]
    isComponentUnmounted: Ref<boolean>
}

export function useFitnessData(options: UseFitnessDataOptions) {
    const { query, allLoadedRecords, fitnessTypeOptions, isComponentUnmounted } = options

    // 安全的计算属性包装器
    function safeComputed<T>(getter: () => T, defaultValue: T): ComputedRef<T> {
        return computed(() => {
            if (isComponentUnmounted.value) return defaultValue
            try {
                return getter()
            } catch (error) {
                console.warn('Computed property error:', error)
                return defaultValue
            }
        })
    }

    const filteredRecords = safeComputed<FitnessRecord[]>(() => {
        let records = [...allLoadedRecords.value]

        if (query?.value?.typeIdList?.length > 0) {
            const typeIdStrings = query.value.typeIdList.map((id: number) => String(id))
            records = records.filter(record =>
                typeIdStrings.includes(String(record.typeId))
            )
        }

        if (query?.value?.remark?.trim()) {
            const searchTerm = query.value.remark.trim().toLowerCase()
            records = records.filter(record => {
                const typeOption = fitnessTypeOptions?.find(type =>
                    String(type.value) === String(record.typeId) ||
                    String(type.id) === String(record.typeId)
                )
                const typeName = typeOption?.value1 || typeOption?.label || ''

                return record.remark?.toLowerCase().includes(searchTerm) ||
                    typeName.toLowerCase().includes(searchTerm)
            })
        }

        return records
    }, [])

    const hasData = safeComputed(() => {
        return filteredRecords.value.length > 0
    }, false)

    const hasSearchConditions = safeComputed(() => {
        return query?.value?.typeIdList?.length > 0 || (query?.value?.remark || '').trim() !== ''
    }, false)

    const allDates = safeComputed(() => {
        const dateSet = new Set<string>()

        filteredRecords.value.forEach(record => {
            if (record?.finishTime) {
                const date = record.finishTime.split('T')[0]
                if (date) dateSet.add(date)
            }
        })

        return Array.from(dateSet).sort()
    }, [])

    const formattedDates = safeComputed(() => {
        return allDates.value.map(date => {
            const [year, month, day] = date.split('-')
            return `${month}/${day}`
        })
    }, [])

    const effectiveTypeIds = safeComputed(() => {
        if (!fitnessTypeOptions?.length) return []

        const dataTypeIds = new Set<string>()
        filteredRecords.value.forEach(record => {
            if (record?.typeId) {
                dataTypeIds.add(String(record.typeId))
            }
        })

        if (query?.value?.typeIdList?.length > 0) {
            return query.value.typeIdList.filter((id: number) =>
                dataTypeIds.has(String(id))
            )
        }

        return Array.from(dataTypeIds)
    }, [])

    return {
        safeComputed,
        filteredRecords,
        hasData,
        hasSearchConditions,
        allDates,
        formattedDates,
        effectiveTypeIds
    }
}