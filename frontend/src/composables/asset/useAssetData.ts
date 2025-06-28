import {computed, Ref} from 'vue'
import {isIdInList} from '@/utils/common'
import type {AssetRecord, QueryConditions} from '@/types/asset'
import type {Option} from '@/types/common'
import {useAssetUtils} from './useAssetUtils'

interface UseAssetDataOptions {
    query: Ref<QueryConditions>
    allLoadedRecords: Ref<AssetRecord[]>
    assetNameOptions: Option[]
    assetTypeOptions: Option[]
    assetLocationOptions: Option[]
    unitOptions: Option[]
}

export function useAssetData(options: UseAssetDataOptions) {
    const { query, allLoadedRecords, assetNameOptions, assetTypeOptions, assetLocationOptions, unitOptions } = options
    const { createMapping, getDisplayName, safeParseAmount } = useAssetUtils()

    // 数据映射
    const nameMapping = computed(() => createMapping(assetNameOptions))
    const typeMapping = computed(() => createMapping(assetTypeOptions))
    const locationMapping = computed(() => createMapping(assetLocationOptions))
    const unitMapping = computed(() => createMapping(unitOptions, 'value1'))

    // 选项数据
    const processedAssetNameOptions = computed(() =>
        assetNameOptions?.map(option => ({
            label: option.value1 || option.label || `资产${option.value}`,
            value: option.value || option.id || ''
        })) || []
    )

    const processedAssetTypeOptions = computed(() =>
        assetTypeOptions?.map(option => ({
            label: option.value1 || option.label || `类型${option.value}`,
            value: option.value || option.id || ''
        })) || []
    )

    const processedAssetLocationOptions = computed(() =>
        assetLocationOptions?.map(option => ({
            label: option.value1 || option.label || `位置${option.value}`,
            value: option.value || option.id || ''
        })) || []
    )

    // 过滤后的记录
    const filteredRecords = computed<AssetRecord[]>(() => {
        let records = [...allLoadedRecords.value]

        if (query.value.assetTypeIdList?.length > 0) {
            records = records.filter(record =>
                isIdInList(record.assetTypeId, query.value.assetTypeIdList)
            )
        }

        if (query.value.assetNameIdList?.length > 0) {
            records = records.filter(record =>
                isIdInList(record.assetNameId, query.value.assetNameIdList)
            )
        }

        if (query.value.assetLocationIdList?.length > 0) {
            records = records.filter(record =>
                isIdInList(record.assetLocationId, query.value.assetLocationIdList)
            )
        }

        if (query.value.remark?.trim()) {
            const searchTerm = query.value.remark.trim().toLowerCase()
            records = records.filter(record =>
                record.remark?.toLowerCase().includes(searchTerm) ||
                record.assetName?.toLowerCase().includes(searchTerm) ||
                record.assetTypeName?.toLowerCase().includes(searchTerm) ||
                record.assetLocationName?.toLowerCase().includes(searchTerm)
            )
        }

        return records
    })

    // 日期相关数据
    const allDates = computed(() => {
        const dateSet = new Set<string>()
        filteredRecords.value.forEach(record => {
            if (record?.acquireTime) {
                const date = record.acquireTime.split('T')[0]
                if (date) dateSet.add(date)
            }
        })
        return Array.from(dateSet).sort()
    })

    const formattedDates = computed(() => {
        return allDates.value.map(date => {
            const [year, month, day] = date.split('-')
            return `${month}/${day}`
        })
    })


    // 基础状态
    const hasData = computed(() => filteredRecords.value.length > 0)

    const hasSearchConditions = computed(() =>
        query.value.assetTypeIdList.length > 0 ||
        query.value.assetNameIdList.length > 0 ||
        query.value.assetLocationIdList.length > 0 ||
        query.value.remark.trim() !== ''
    )

    const dateRangeDisplay = computed(() => {
        if (!query.value.startDate || !query.value.endDate) return ''
        return `${query.value.startDate} ~ ${query.value.endDate}`
    })

    const emptyStateDescription = computed(() => {
        if (!query.value.startDate || !query.value.endDate) {
            return '请选择日期范围查看资产数据'
        }
        if (hasSearchConditions.value) {
            return '当前筛选条件下没有找到资产记录，请尝试调整筛选条件'
        }
        return `${dateRangeDisplay.value}期间暂无资产记录`
    })

    // 按维度聚合的金额数据
    const amountByDimension = computed(() => {
        const byName: Record<string, Record<string, number>> = {}
        const byType: Record<string, Record<string, number>> = {}
        const byLocation: Record<string, Record<string, number>> = {}

        for (const record of filteredRecords.value) {
            if (!record?.acquireTime) continue

            const date = record.acquireTime.split('T')[0]
            const amount = safeParseAmount(record.amount)
            if (amount === 0) continue

            // 按维度聚合
            const nameKey = getDisplayName(String(record.assetNameId), nameMapping.value, record.assetName, '资产')
            if (!byName[nameKey]) byName[nameKey] = {}
            byName[nameKey][date] = (byName[nameKey][date] || 0) + amount

            const typeKey = getDisplayName(
                String(record.assetTypeId),
                typeMapping.value,
                record.assetTypeName || record.assetTypeValue,
                '类型'
            )
            if (!byType[typeKey]) byType[typeKey] = {}
            byType[typeKey][date] = (byType[typeKey][date] || 0) + amount

            const locationKey = getDisplayName(
                String(record.assetLocationId),
                locationMapping.value,
                record.assetLocationName || record.assetLocationValue,
                '位置'
            )
            if (!byLocation[locationKey]) byLocation[locationKey] = {}
            byLocation[locationKey][date] = (byLocation[locationKey][date] || 0) + amount
        }

        return { byName, byType, byLocation }
    })

    const totalAmountByDate = computed(() => {
        const map: Record<string, number> = {}
        for (const record of filteredRecords.value) {
            if (!record?.acquireTime) continue
            const date = record.acquireTime.split('T')[0]
            const amount = safeParseAmount(record.amount)
            if (amount > 0) {
                map[date] = (map[date] || 0) + amount
            }
        }
        return map
    })

    // 在现有的 lastDateWithRecords 计算属性中添加调试
    const lastDateWithRecords = computed(() => {
        const lastDate = allDates.value.length ? allDates.value[allDates.value.length - 1] : ''

        // 添加调试信息
        console.log('\n📅 日期分析:')
        console.log('  所有唯一日期:', allDates.value)
        console.log('  选择的最后日期:', lastDate)

        return lastDate
    })

    const lastDateRecords = computed(() => {
        if (!lastDateWithRecords.value) {
            console.log('❌ 没有最后日期，返回空数组')
            return []
        }

        const records = filteredRecords.value.filter(record =>
            record?.acquireTime?.startsWith(lastDateWithRecords.value)
        )

        // 添加调试信息
        console.log('\n🎯 最后日期记录筛选:')
        console.log(`  筛选条件: acquireTime 以 "${lastDateWithRecords.value}" 开头`)
        console.log(`  过滤前记录数: ${filteredRecords.value.length}`)
        console.log(`  过滤后记录数: ${records.length}`)
        console.log('  过滤后的记录:', records.map(record => ({
            id: record.id,
            acquireTime: record.acquireTime,
            assetName: record.assetName,
            amount: record.amount
        })))

        return records
    })

    return {
        nameMapping,
        typeMapping,
        locationMapping,
        unitMapping,
        processedAssetNameOptions,
        processedAssetTypeOptions,
        processedAssetLocationOptions,
        filteredRecords,
        allDates,
        formattedDates,
        lastDateWithRecords,
        lastDateRecords,
        hasData,
        hasSearchConditions,
        dateRangeDisplay,
        emptyStateDescription,
        amountByDimension,
        totalAmountByDate
    }
}