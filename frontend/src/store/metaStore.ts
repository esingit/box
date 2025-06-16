import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import emitter from '@/utils/eventBus'
import { clearCommonMetaCache } from '@/utils/commonMeta'

export const useMetaStore = defineStore('meta', () => {
  const types = ref<any[]>([])
  const units = ref<any[]>([])
  const loading = ref(false)

  async function fetchMetaData() {
    loading.value = true
    try {
      const [typeRes, unitRes] = await Promise.all([
        axios.get('/api/common-meta/by-type', { params: { typeCode: 'FITNESS_TYPE' } }),
        axios.get('/api/common-meta/by-type', { params: { typeCode: 'UNIT' } })
      ])

      if (typeRes.data?.success) {
        types.value = typeRes.data.data || []
      } else {
        emitter.emit('notify', {
          message: '获取类型数据失败',
          type: 'error'
        })
      }

      if (unitRes.data?.success) {
        units.value = unitRes.data.data || []
      } else {
        emitter.emit('notify', {
          message: '获取单位数据失败',
          type: 'error'
        })
      }
    } catch (error) {
      if (!axios.isCancel?.(error)) {
        console.error('获取元数据失败:', error)
        emitter.emit('notify', {
          message: '获取元数据失败，请稍后重试',
          type: 'error'
        })
      }
    } finally {
      loading.value = false
    }
  }

  async function refreshMetaData() {
    clearCommonMetaCache()
    await fetchMetaData()
  }

  return {
    types,
    units,
    loading,
    fetchMetaData,
    refreshMetaData
  }
})
