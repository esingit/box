import { ref, Ref, reactive } from 'vue'
import emitter from '@/utils/eventBus'
import type { ChartOptionsType } from '@/types/asset'
import { CHART_OPTIONS_STORAGE_KEY } from './useAssetConstants'

export interface AssetState {
    isLoading: Ref<boolean>
    errorMessage: Ref<string>
    isChartReady: Ref<boolean>
    isUpdatingChart: Ref<boolean>
    isSearching: Ref<boolean>
    hasInitialData: Ref<boolean>
    isFilterUpdating: Ref<boolean>
}

export function useAssetState(): AssetState & {
    showNotification: (message: string, type?: 'success' | 'error' | 'warning' | 'info') => void
    chartOptions: ChartOptionsType
    saveChartOptions: () => void
} {
    const isLoading = ref(false)
    const errorMessage = ref('')
    const isChartReady = ref(false)
    const isUpdatingChart = ref(false)
    const isSearching = ref(false)
    const hasInitialData = ref(false)
    const isFilterUpdating = ref(false)

    function showNotification(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info') {
        emitter.emit('notify', { message, type })
    }

    const getSavedChartOptions = (): Partial<ChartOptionsType> => {
        try {
            const saved = localStorage.getItem(CHART_OPTIONS_STORAGE_KEY)
            return saved ? JSON.parse(saved) : {}
        } catch {
            return {}
        }
    }

    const chartOptions = reactive<ChartOptionsType>({
        showTotalTrend: true,
        showNameDimension: true,
        showTypeDimension: true,
        showLocationDimension: true,
        ...getSavedChartOptions()
    })

    const saveChartOptions = () => {
        try {
            localStorage.setItem(CHART_OPTIONS_STORAGE_KEY, JSON.stringify(chartOptions))
        } catch (error) {
            console.warn('Failed to save chart options:', error)
        }
    }

    return {
        isLoading,
        errorMessage,
        isChartReady,
        isUpdatingChart,
        isSearching,
        hasInitialData,
        isFilterUpdating,
        showNotification,
        chartOptions,
        saveChartOptions
    }
}