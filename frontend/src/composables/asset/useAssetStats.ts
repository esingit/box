import { computed, ComputedRef } from 'vue'
import type { AssetRecord } from '@/types/asset'
import type { Option } from '@/types/common'
import { useAssetUtils } from './useAssetUtils'
import { DEFAULT_CURRENCY } from './useAssetConstants'

interface UseAssetStatsOptions {
    lastDateRecords: ComputedRef<AssetRecord[]>
    assetTypeOptions: Option[]
    unitOptions: Option[]
}

interface UseAssetStatsReturn {
    statisticsData: ComputedRef<any>
    statisticsCards: ComputedRef<any[]>
}

export function useAssetStats(options: UseAssetStatsOptions): UseAssetStatsReturn {
    const { lastDateRecords, assetTypeOptions, unitOptions } = options
    const { safeParseAmount, normalizeUnitSymbol, formatRawAmountWithUnit } = useAssetUtils()

    // 将 unitMapping 提取为计算属性
    const unitMapping = computed<Record<string, string>>(() => {
        const mapping: Record<string, string> = {}
        unitOptions?.forEach(option => {
            if (option) {
                const id = option.id || option.value
                const name = option.value1
                if (id && name) {
                    mapping[String(id)] = String(name)
                }
            }
        })
        return mapping
    })

    function getDefaultUnitForAssetType(typeId: string | number): string {
        const assetType = assetTypeOptions?.find(type =>
            String(type.value) === String(typeId) || String(type.id) === String(typeId)
        )

        if (!assetType?.key3) return DEFAULT_CURRENCY

        const defaultUnit = unitOptions?.find(unit => unit.key1 === assetType.key3)
        return defaultUnit ? normalizeUnitSymbol(defaultUnit.value1 || DEFAULT_CURRENCY) : DEFAULT_CURRENCY
    }

    function getUnitSymbol(record: AssetRecord): string {
        if (record.unitId && unitMapping.value[String(record.unitId)]) {
            return normalizeUnitSymbol(unitMapping.value[String(record.unitId)])
        }
        if (record.unitValue) {
            return normalizeUnitSymbol(record.unitValue)
        }
        return getDefaultUnitForAssetType(record.assetTypeId)
    }

    // 在 statisticsData 计算属性开始处添加
    console.log('🔍 调试 assetTypeOptions:', assetTypeOptions.map(type => ({
        id: type.id || type.value,
        key1: type.key1,
        value1: type.value1 || type.label
    })));

    const getTypeTotal = (typeKeys: string[]) => {
        console.log(`\n📊 计算类型总额: ${typeKeys.join(', ')}`);

        // 类型名称映射
        const typeNameMap: Record<string, string[]> = {
            'SAVINGS': ['储蓄'],
            'FIXED_DEPOSIT': ['定期'],
            'CASH': ['现金'],
            'FINANCE': ['理财'],
            'STOCK': ['股票'],
            'FUND': ['基金'],
            'PROVIDENT_FUND': ['公积金'],
            'PENSION': ['养老金'],
            'MEDICAL_INSURANCE': ['医保'],
            'DEBT': ['负债']
        };

        // 获取所有匹配的中文名称
        const valueNames: string[] = [];
        typeKeys.forEach(key => {
            if (typeNameMap[key]) {
                valueNames.push(...typeNameMap[key]);
            }
        });

        console.log(`  匹配的中文名称: [${valueNames.join(', ')}]`);

        // 使用中文名称匹配类型
        const typeIds = assetTypeOptions
            ?.filter(type => {
                const typeName = (type.value1 || type.label || '').toLowerCase();
                return valueNames.some(value => typeName.includes(value.toLowerCase()));
            })
            ?.map(type => String(type.id || type.value)) || [];

        console.log(`  匹配的类型ID: [${typeIds.join(', ')}]`);
        console.log(`  匹配的类型详情:`, assetTypeOptions?.filter(type =>
            typeIds.includes(String(type.id || type.value))
        ).map(type => ({
            id: type.id || type.value,
            name: type.value1 || type.label
        })));

        const matchingRecords = lastDateRecords.value
            .filter(record => typeIds.includes(String(record.assetTypeId)));

        console.log(`  匹配的记录数量: ${matchingRecords.length}`);
        console.log(`  匹配的记录详情:`, matchingRecords.map(record => ({
            id: record.id,
            assetName: record.assetName,
            assetTypeId: record.assetTypeId,
            amount: record.amount,
            parsedAmount: safeParseAmount(record.amount)
        })));

        const total = matchingRecords.reduce((sum, record) => {
            const amount = safeParseAmount(record.amount);
            console.log(`    累加: ${sum} + ${amount} = ${sum + amount}`);
            return sum + amount;
        }, 0);

        console.log(`  ${typeKeys.join(', ')} 类型总额: ${total}`);
        return total;
    };

    const statisticsData = computed(() => {
        console.log('\n🔍 ===== 资产统计计算流程开始 =====')
        console.log('输入的 lastDateRecords 数量:', lastDateRecords.value.length)
        console.log('所有记录详情:', lastDateRecords.value.map(record => ({
            id: record.id,
            assetName: record.assetName || record.assetNameValue,
            assetTypeName: record.assetTypeName || record.assetTypeValue,
            assetTypeId: record.assetTypeId,
            assetLocationName: record.assetLocationName || record.assetLocationValue,
            amount: record.amount,
            parsedAmount: safeParseAmount(record.amount),
            acquireTime: record.acquireTime,
            unitId: record.unitId,
            unitValue: record.unitValue
        })))

        // 🔥 修改：使用 key1 判断负债类型
        const debtTypeIds = assetTypeOptions
            ?.filter(type => {
                const typeName = (type.value1 || type.label || '').toLowerCase();
                return typeName.includes('负债');
            })
            ?.map(type => String(type.id || type.value)) || [];

        console.log('\n💰 分离资产和负债:')
        console.log('  负债类型ID:', debtTypeIds)

        // 分离资产和负债记录
        const assetRecords = lastDateRecords.value.filter(record =>
            !debtTypeIds.includes(String(record.assetTypeId))
        )
        const debtRecords = lastDateRecords.value.filter(record =>
            debtTypeIds.includes(String(record.assetTypeId))
        )

        console.log(`  资产记录数量: ${assetRecords.length}`)
        console.log(`  负债记录数量: ${debtRecords.length}`)

        // 计算资产总额（不包含负债）
        console.log('\n💰 计算净资产（资产总额，不含负债）:')
        const totalAmount = assetRecords.reduce((sum, record, index) => {
            const amount = safeParseAmount(record.amount)
            const newSum = sum + amount
            console.log(`  资产记录${index + 1}: ${record.assetName || '未知'} - ${record.amount} -> 解析为: ${amount}, 累计: ${sum} + ${amount} = ${newSum}`)
            return newSum
        }, 0)

        // 计算负债总额（用于显示）
        const totalDebts = debtRecords.reduce((sum, record, index) => {
            const amount = safeParseAmount(record.amount)
            const newSum = sum + amount
            console.log(`  负债记录${index + 1}: ${record.assetName || '未知'} - ${record.amount} -> 解析为: ${amount}, 累计: ${sum} + ${amount} = ${newSum}`)
            return newSum
        }, 0)

        console.log(`净资产计算结果: ${totalAmount}`)
        console.log(`负债总额: ${totalDebts}`)

        // 🔥 修改：使用 key1 字段计算各类型总额
        const savingsTotal = getTypeTotal(['SAVINGS', 'FIXED_DEPOSIT', 'CASH'])
        const financeTotal = getTypeTotal(['FINANCE', 'STOCK'])
        const fundTotal = getTypeTotal(['FUND', 'PROVIDENT_FUND', 'PENSION', 'MEDICAL_INSURANCE'])
        const debtTotal = getTypeTotal(['DEBT'])

        console.log('\n📋 各类型总额汇总:')
        console.log(`  储蓄类型: ${savingsTotal}`)
        console.log(`  理财类型: ${financeTotal}`)
        console.log(`  基金类型: ${fundTotal}`)
        console.log(`  负债类型: ${debtTotal}`)

        const assetsTypesSum = savingsTotal + financeTotal + fundTotal
        console.log(`  资产类型总和（不含负债）: ${assetsTypesSum}`)
        console.log(`  与净资产差异: ${totalAmount - assetsTypesSum}`)

        if (Math.abs(totalAmount - assetsTypesSum) > 0.01) {
            console.warn('⚠️  净资产与各资产类型总和不一致，可能存在未分类的资产类型')

            // 查找未分类的资产记录（不包括负债）
            const categorizedKeys = ['SAVINGS', 'FIXED_DEPOSIT', 'CASH', 'FINANCE', 'STOCK', 'FUND', 'PROVIDENT_FUND', 'PENSION', 'MEDICAL_INSURANCE']
            const allCategorizedTypeIds = assetTypeOptions
                ?.filter(type => categorizedKeys.includes(type.key1 || ''))
                ?.map(type => String(type.id || type.value)) || []

            const uncategorizedAssetRecords = assetRecords.filter(record =>
                !allCategorizedTypeIds.includes(String(record.assetTypeId))
            )

            console.log(`  未分类的资产记录数量: ${uncategorizedAssetRecords.length}`)
            console.log(`  未分类的资产记录:`, uncategorizedAssetRecords.map(record => ({
                id: record.id,
                assetName: record.assetName,
                assetTypeId: record.assetTypeId,
                amount: record.amount,
                parsedAmount: safeParseAmount(record.amount)
            })))
        }

        const unitSymbol = lastDateRecords.value.length > 0
            ? getUnitSymbol(lastDateRecords.value[0])
            : DEFAULT_CURRENCY

        console.log(`\n💱 使用的货币符号: ${unitSymbol}`)
        console.log('🔍 ===== 资产统计计算流程结束 =====\n')

        return {
            totalAmount,
            totalAssets: totalAmount,
            totalDebts,
            netAssets: totalAmount - totalDebts,
            savingsTotal,
            financeTotal,
            fundTotal,
            debtTotal,
            unitSymbol
        }
    })

    const statisticsCards = computed(() => [
        {
            title: '净资产',
            value: formatRawAmountWithUnit(statisticsData.value.totalAmount, statisticsData.value.unitSymbol),
            cardClass: 'bg-blue-50 p-3 rounded-lg',
            titleClass: 'text-blue-600 font-medium',
            valueClass: 'text-lg font-bold text-blue-800'
        },
        {
            title: '储蓄类型总额',
            value: formatRawAmountWithUnit(statisticsData.value.savingsTotal, statisticsData.value.unitSymbol),
            cardClass: 'bg-green-50 p-3 rounded-lg',
            titleClass: 'text-green-600 font-medium',
            valueClass: 'text-lg font-bold text-green-800'
        },
        {
            title: '理财类型总额',
            value: formatRawAmountWithUnit(statisticsData.value.financeTotal, statisticsData.value.unitSymbol),
            cardClass: 'bg-yellow-50 p-3 rounded-lg',
            titleClass: 'text-yellow-600 font-medium',
            valueClass: 'text-lg font-bold text-yellow-800'
        },
        {
            title: '基金类型总额',
            value: formatRawAmountWithUnit(statisticsData.value.fundTotal, statisticsData.value.unitSymbol),
            cardClass: 'bg-purple-50 p-3 rounded-lg',
            titleClass: 'text-purple-600 font-medium',
            valueClass: 'text-lg font-bold text-purple-800'
        },
        {
            title: '负债总额',
            value: formatRawAmountWithUnit(statisticsData.value.debtTotal, statisticsData.value.unitSymbol),
            cardClass: 'bg-red-50 p-3 rounded-lg',
            titleClass: 'text-red-600 font-medium',
            valueClass: 'text-lg font-bold text-red-800'
        }
    ])

    return {
        statisticsData,
        statisticsCards
    }
}