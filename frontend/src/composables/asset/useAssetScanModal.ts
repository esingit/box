// composables/useAssetScanModal.ts
import { reactive, computed } from 'vue'
import { useMetaStore } from '@/store/metaStore'
import { useAssetNameStore } from '@/store/assetNameStore'
import emitter from '@/utils/eventBus'

export function useAssetScanModal() {
    const metaStore = useMetaStore()
    const assetNameStore = useAssetNameStore()

    const TODAY = new Date().toISOString().slice(0, 10)

    const formData = reactive({
        assetTypeId: null as string | null,
        assetLocationId: null as string | null,
        acquireTime: TODAY,
        unitId: null as string | null
    })

    const modalSize = computed(() => ({
        width: '1250px',
        height: '900px'
    }))

    const options = computed(() => ({
        assetTypes: metaStore.typeMap?.ASSET_TYPE?.map(i => ({
            label: String(i.value1),
            value: i.id
        })) || [],
        assetLocations: metaStore.typeMap?.ASSET_LOCATION?.map(i => ({
            label: String(i.value1),
            value: i.id
        })) || [],
        units: metaStore.typeMap?.UNIT?.map(i => ({
            label: String(i.value1),
            value: i.id
        })) || []
    }))

    const loadAssetNames = async () => {
        try {
            await assetNameStore.fetchAssetName(true)
            if ((!assetNameStore.assetName || assetNameStore.assetName.length === 0) && assetNameStore.loadList) {
                await assetNameStore.loadList(true)
            }
        } catch (error) {
            console.error('加载资产名称数据失败:', error)
        }
    }

    const setDefaultUnit = async (typeId: string) => {
        const assetTypes = metaStore.typeMap?.ASSET_TYPE || []
        const unitList = metaStore.typeMap?.UNIT || []

        const selectedType = assetTypes.find(type => String(type.id) === typeId)
        if (!selectedType?.key3) return

        const defaultUnit = unitList.find(unit => unit.key1 === selectedType.key3)
        if (!defaultUnit) return

        const defaultUnitId = String(defaultUnit.id)
        if (!formData.unitId || formData.unitId !== defaultUnitId) {
            formData.unitId = defaultUnitId
        }
    }

    const initializeDefaults = async () => {
        // 设置默认资产类型
        const defaultAssetType = metaStore.typeMap?.ASSET_TYPE?.find(
            item => item.value1 === '理财'
        )
        if (defaultAssetType) {
            formData.assetTypeId = String(defaultAssetType.id)
            await setDefaultUnit(String(defaultAssetType.id))
        }

        // 设置默认资产位置
        const defaultAssetLocation = metaStore.typeMap?.ASSET_LOCATION?.find(
            item => item.value1 === '兴业银行'
        )
        if (defaultAssetLocation) {
            formData.assetLocationId = String(defaultAssetLocation.id)
        }
    }

    const resetForm = () => {
        Object.assign(formData, {
            assetTypeId: null,
            assetLocationId: null,
            acquireTime: TODAY,
            unitId: null
        })
    }

    // 确保返回所有需要的属性
    return {
        formData,
        modalSize,
        options,
        loadAssetNames,
        initializeDefaults,
        setDefaultUnit,
        resetForm,
        TODAY
    }
}