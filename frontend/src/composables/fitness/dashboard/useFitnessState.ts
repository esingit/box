import { ref, Ref } from 'vue'
import emitter from '@/utils/eventBus'

export interface FitnessState {
    errorMessage: Ref<string>
    isChartReady: Ref<boolean>
    isUpdatingChart: Ref<boolean>
    isSearching: Ref<boolean>
    hasInitialData: Ref<boolean>
    isFilterUpdating: Ref<boolean>
    isComponentUnmounted: Ref<boolean>
}

export function useFitnessState(): FitnessState & {
    showNotification: (message: string, type?: 'success' | 'error' | 'warning' | 'info') => void
} {
    const errorMessage = ref('')
    const isChartReady = ref(false)
    const isUpdatingChart = ref(false)
    const isSearching = ref(false)
    const hasInitialData = ref(false)
    const isFilterUpdating = ref(false)
    const isComponentUnmounted = ref(false)

    function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
        emitter.emit('notify', { message, type })
    }

    return {
        errorMessage,
        isChartReady,
        isUpdatingChart,
        isSearching,
        hasInitialData,
        isFilterUpdating,
        isComponentUnmounted,
        showNotification
    }
}