import { ref } from 'vue'
import { useMessage } from 'naive-ui'

export function useRequest<T = any>(requestFn: () => Promise<T>) {
    const loading = ref(false)
    const data = ref<T | null>(null)
    const error = ref<any>(null)
    const message = useMessage()

    const run = async () => {
        loading.value = true
        error.value = null
        try {
            const res = await requestFn()
            data.value = res
        } catch (err: any) {
            error.value = err
            message.error(err?.message || '请求失败')
        } finally {
            loading.value = false
        }
    }

    return {
        loading,
        data,
        error,
        run
    }
}
