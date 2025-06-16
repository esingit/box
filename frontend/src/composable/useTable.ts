import { ref, watchEffect } from 'vue'
import { usePagination } from './usePagination'

export function useTable(fetchDataFn: (params: any) => Promise<any>) {
    const loading = ref(false)
    const dataSource = ref([])
    const pagination = usePagination()

    const getList = async () => {
        loading.value = true
        try {
            const res = await fetchDataFn({
                page: pagination.page.value,
                pageSize: pagination.pageSize.value
            })
            dataSource.value = res.records || []
            pagination.total.value = res.total || 0
        } finally {
            loading.value = false
        }
    }

    watchEffect(() => {
        getList()
    })

    return {
        loading,
        dataSource,
        ...pagination,
        refresh: getList
    }
}
