// composables/useImageRecognition.ts
import { ref, nextTick } from 'vue'
import { useAssetStore } from '@/store/assetStore'
import emitter from '@/utils/eventBus'
import { RecognizedAssetItem } from '@/types/asset'

export function useImageRecognition(loadAssetNames: () => Promise<void>) {
    const assetStore = useAssetStore()

    const imageFile = ref<File | null>(null)
    const imagePreview = ref('')
    const isProcessing = ref(false)
    const recognizedItems = ref<RecognizedAssetItem[]>([])

    const handleImageUpload = (file: File) => {
        imageFile.value = file
        recognizedItems.value = []

        const reader = new FileReader()
        reader.onload = (e) => {
            imagePreview.value = e.target?.result as string
        }
        reader.readAsDataURL(file)
    }

    const recognizeImage = async () => {
        if (!imageFile.value) return

        isProcessing.value = true

        try {
            await loadAssetNames()

            const formData = new FormData()
            formData.append('file', imageFile.value)

            const result = await assetStore.recognizeAssetImage(formData)
            await nextTick()

            // 在获取到新数据后，再进行赋值，这会原子性地替换旧数据
            recognizedItems.value = processRecognitionResult(result)

            emitter.emit('notify', {
                type: recognizedItems.value.length ? 'success' : 'warning',
                message: recognizedItems.value.length
                    ? `成功识别 ${recognizedItems.value.length} 条数据`
                    : '未识别到有效数据'
            })
        } catch (err) {
            // 发生错误时，旧的数据仍然保留在界面上，用户体验更好
            emitter.emit('notify', {
                message: '图片识别失败，请重试',
                type: 'error'
            })
        } finally {
            isProcessing.value = false
        }
    }

    const processRecognitionResult = (result: any[] | null): RecognizedAssetItem[] => {
        const safeResult = result || []
        return safeResult.map(item => ({
            ...item,
            assetNameId: safeParseId(item.assetNameId),
            amount: item.amount || null,
            remark: item.remark || '',
            matchScore: item.matchScore || 0,
            matchedAssetName: item.matchedAssetName || '',
            originalAssetName: item.originalAssetName || item.assetName || ''
        }))
    }

    const safeParseId = (id: any): string | null => {
        return id === null || id === undefined || id === '' ? null : String(id)
    }

    return {
        imageFile,
        imagePreview,
        isProcessing,
        recognizedItems,
        handleImageUpload,
        recognizeImage
    }
}