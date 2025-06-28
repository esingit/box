export interface QueryType {
    typeIdList: number[]
    startDate: string
    endDate: string
    remark: string
}

export function useFitnessQuery() {
    function createSafeQuery(inputQuery: any): QueryType {
        const defaultQuery: QueryType = {
            typeIdList: [],
            startDate: '',
            endDate: '',
            remark: ''
        }

        if (!inputQuery || typeof inputQuery !== 'object') {
            return defaultQuery
        }

        return {
            typeIdList: Array.isArray(inputQuery.typeIdList) ? inputQuery.typeIdList : [],
            startDate: typeof inputQuery.startDate === 'string' ? inputQuery.startDate : '',
            endDate: typeof inputQuery.endDate === 'string' ? inputQuery.endDate : '',
            remark: typeof inputQuery.remark === 'string' ? inputQuery.remark : ''
        }
    }

    function formatDateRange(startDate: string, endDate: string): string {
        if (!startDate || !endDate) return ''

        const formatDate = (dateStr: string) => {
            const date = new Date(dateStr)
            return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
        }

        const start = formatDate(startDate)
        const end = formatDate(endDate)

        return start === end ? start : `${start} ~ ${end}`
    }

    return {
        createSafeQuery,
        formatDateRange
    }
}