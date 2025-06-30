// composables/useFormValidation.ts
import { computed, Ref } from 'vue'
import { RecognizedAssetItem } from '@/types/asset'

interface FormData {
    assetTypeId: string | null
    assetLocationId: string | null
    acquireTime: string
    unitId: string | null
}

export function useFormValidation(
    formData: FormData,
    recognizedItems: Ref<RecognizedAssetItem[]>
) {

    const validationErrors = computed(() => {
        const errors: string[] = []

        // 验证必填字段
        if (!formData.assetTypeId) {
            errors.push('请选择资产类型')
        }

        if (!formData.assetLocationId) {
            errors.push('请选择资产位置')
        }

        if (!formData.acquireTime) {
            errors.push('请选择登记日期')
        }

        if (!formData.unitId) {
            errors.push('请选择货币单位')
        }

        // 验证识别项目
        if (recognizedItems.value.length > 0) {
            const invalidItems = recognizedItems.value.filter(item =>
                !item.assetNameId || !item.amount || item.amount <= 0
            )

            if (invalidItems.length > 0) {
                errors.push(`有 ${invalidItems.length} 条记录缺少资产名称或金额无效`)
            }
        }

        return errors
    })

    const validItemsCount = computed(() => {
        return recognizedItems.value.filter(item =>
            item.assetNameId &&
            item.amount &&
            item.amount > 0
        ).length
    })

    const hasData = computed(() => {
        return recognizedItems.value.length > 0
    })

    const canSubmit = computed(() => {
        return hasData.value &&
            validationErrors.value.length === 0 &&
            validItemsCount.value > 0
    })

    const isFormComplete = computed(() => {
        return formData.assetTypeId &&
            formData.assetLocationId &&
            formData.acquireTime &&
            formData.unitId
    })

    const hasValidItems = computed(() => {
        return validItemsCount.value > 0
    })

    const invalidItemsCount = computed(() => {
        return recognizedItems.value.length - validItemsCount.value
    })

    const getItemValidationStatus = (item: RecognizedAssetItem) => {
        const issues: string[] = []

        if (!item.assetNameId) {
            issues.push('缺少资产名称')
        }

        if (!item.amount || item.amount <= 0) {
            issues.push('金额无效')
        }

        return {
            isValid: issues.length === 0,
            issues
        }
    }

    const getFormValidationSummary = () => {
        return {
            isFormComplete: isFormComplete.value,
            hasValidItems: hasValidItems.value,
            validItemsCount: validItemsCount.value,
            invalidItemsCount: invalidItemsCount.value,
            totalItems: recognizedItems.value.length,
            canSubmit: canSubmit.value,
            errors: validationErrors.value
        }
    }

    return {
        validationErrors,
        validItemsCount,
        invalidItemsCount,
        hasData,
        canSubmit,
        isFormComplete,
        hasValidItems,
        getItemValidationStatus,
        getFormValidationSummary
    }
}