// composables/useBatchSubmit.ts
import { ref, Ref } from 'vue'
import { useAssetStore } from '@/store/assetStore'
import emitter from '@/utils/eventBus'
import { RawAssetRecord, RecognizedAssetItem } from '@/types/asset'

interface FormData {
    assetTypeId: string | null
    assetLocationId: string | null
    acquireTime: string
    unitId: string | null
}

export function useBatchSubmit(
    formData: FormData,
    recognizedItems: Ref<RecognizedAssetItem[]>,
    emit: any
) {
    const assetStore = useAssetStore()
    const isSubmitting = ref(false)

    const handleBatchAdd = async () => {
        if (!canSubmitData()) return

        isSubmitting.value = true

        try {
            const validItems = getValidItems()
            const records = createRecords(validItems)

            const hasRecordsToday = await assetStore.checkTodayRecords()

            if (hasRecordsToday) {
                await handleExistingRecords(records)
            } else {
                await handleNoRecords(records)
            }
        } catch (error: any) {
            emitter.emit('notify', {
                type: 'error',
                message: `操作失败：${error.message || '未知错误'}`
            })
        } finally {
            isSubmitting.value = false
        }
    }

    const canSubmitData = (): boolean => {
        if (!formData.assetTypeId || !formData.assetLocationId ||
            !formData.acquireTime || !formData.unitId) {
            return false
        }

        const validItems = getValidItems()
        return validItems.length > 0
    }

    const getValidItems = (): RecognizedAssetItem[] => {
        return recognizedItems.value.filter(item =>
            item.assetNameId && item.amount && item.amount > 0
        )
    }

    const createRecords = (validItems: RecognizedAssetItem[]): RawAssetRecord[] => {
        return validItems.map((item, index) => ({
            id: String(Date.now() + index),
            assetNameId: String(item.assetNameId!),
            assetLocationId: String(formData.assetLocationId!),
            assetTypeId: String(formData.assetTypeId!),
            unitId: String(formData.unitId!),
            amount: Number(item.amount!),
            acquireTime: formData.acquireTime,
            remark: item.remark || ''
        }))
    }

    const handleExistingRecords = async (records: RawAssetRecord[]) => {
        emitter.emit('confirm', {
            title: '今日已有记录',
            message: `检测到今日已有记录，请选择处理方式：\n\n• 智能合并：保留现有记录，更新相同资产名称的金额，添加新资产\n• 完全覆盖：删除今日所有记录后重新添加\n\n将处理 ${records.length} 条记录`,
            type: 'primary',
            confirmText: '智能合并',
            cancelText: '完全覆盖',
            onConfirm: async () => {
                const success = await executeBatchAdd(records, false, false)
                if (success) {
                    emit('close')
                    emit('submit', records)
                }
            },
            onCancel: async () => {
                showOverwriteConfirmation(records)
            }
        })
    }

    const showOverwriteConfirmation = (records: RawAssetRecord[]) => {
        emitter.emit('confirm', {
            title: '确认覆盖',
            message: '⚠️ 此操作将删除今日所有现有记录，是否确认？',
            type: 'danger',
            confirmText: '确认覆盖',
            cancelText: '取消',
            onConfirm: async () => {
                const success = await executeBatchAdd(records, true, false)
                if (success) {
                    emit('close')
                    emit('submit', records)
                }
            }
        })
    }

    const handleNoRecords = async (records: RawAssetRecord[]) => {
        emitter.emit('confirm', {
            title: '是否复制历史记录',
            message: `今日暂无记录，请选择操作方式：\n\n• 复制并添加：先复制上次记录作为基础，再添加 ${records.length} 条新记录\n• 仅添加新记录：直接添加 ${records.length} 条新记录`,
            type: 'primary',
            confirmText: '复制并添加',
            cancelText: '仅添加新记录',
            onConfirm: async () => {
                const success = await executeBatchAdd(records, false, true)
                if (success) {
                    emit('close')
                    emit('submit', records)
                }
            },
            onCancel: async () => {
                const success = await executeBatchAdd(records, false, false)
                if (success) {
                    emit('close')
                    emit('submit', records)
                }
            }
        })
    }

    const executeBatchAdd = async (
        records: RawAssetRecord[],
        forceOverwrite: boolean,
        copyLast: boolean = false
    ): Promise<boolean> => {
        try {
            const result = await assetStore.smartBatchAddRecords(records, forceOverwrite, copyLast)

            if (result) {
                const details: string[] = []
                if (result.copied) details.push('已复制历史记录')
                if (result.overwrote) details.push('已覆盖今日记录')
                if (result.updateCount > 0) details.push(`更新${result.updateCount}条`)
                if (result.addCount > 0) details.push(`新增${result.addCount}条`)

                emitter.emit('notify', {
                    type: 'success',
                    message: `批量操作完成：成功处理 ${result.successCount} 条记录${details.length ? `（${details.join('，')}）` : ''}`
                })

                return true
            }

            return false
        } catch (error: any) {
            emitter.emit('notify', {
                type: 'error',
                message: `批量添加失败：${error.message || '未知错误'}`
            })
            return false
        }
    }

    return {
        isSubmitting,
        handleBatchAdd
    }
}