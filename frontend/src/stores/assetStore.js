import { ref } from 'vue'
import { defineStore } from 'pinia'
import axios from '@/utils/axios'
import emitter from '@/utils/eventBus'

export const useAssetStore = defineStore('asset', () => {
  // 状态
  const assetNames = ref([])
  const types = ref([])
  const units = ref([])
  const locations = ref([])
  const loading = ref(false)

  // 获取资产名称列表
  async function fetchAssetNames() {
    try {
      console.log('开始获取资产名称列表...')
      const res = await axios.get('/api/asset-names/all')
      console.log('获取资产名称列表响应:', res)
      if (res.data?.success) {
        assetNames.value = res.data.data || []
        console.log('资产名称列表已更新:', assetNames.value)
      } else {
        console.error('获取资产名称列表失败，服务器返回:', res.data)
        emitter.emit('notify', `获取资产名称列表失败: ${res.data?.message || '未知错误'}`, 'error')
      }
    } catch (error) {
      console.error('获取资产名称列表失败:', {
        message: error.message,
        response: error.response?.data,
        config: {
          url: error.config?.url,
          method: error.config?.method,
          headers: error.config?.headers
        }
      })
      emitter.emit('notify', `获取资产名称列表失败: ${error.response?.data?.message || error.message}`, 'error')
    }
  }

  // 获取资产类型列表
  async function fetchTypes() {
    try {
      const res = await axios.get('/api/common-meta/by-type', {
        params: { typeCode: 'ASSET_TYPE' }
      })
      if (res.data?.success) {
        types.value = res.data.data || []
      }
    } catch (error) {
      console.error('获取资产类型列表失败:', error)
      emitter.emit('notify', '获取资产类型列表失败', 'error')
    }
  }

  // 获取货币单位列表
  async function fetchUnits() {
    try {
      const res = await axios.get('/api/common-meta/by-type', {
        params: { typeCode: 'UNIT' }
      })
      if (res.data?.success) {
        units.value = res.data.data || []
      }
    } catch (error) {
      console.error('获取货币单位列表失败:', error)
      emitter.emit('notify', '获取货币单位列表失败', 'error')
    }
  }

  // 获取资产位置列表
  async function fetchLocations() {
    try {
      const res = await axios.get('/api/common-meta/by-type', {
        params: { typeCode: 'ASSET_LOCATION' }
      })
      if (res.data?.success) {
        locations.value = res.data.data || []
      }
    } catch (error) {
      console.error('获取资产位置列表失败:', error)
      emitter.emit('notify', '获取资产位置列表失败', 'error')
    }
  }

  // 初始化数据
  async function initData() {
    loading.value = true
    try {
      await Promise.all([
        fetchAssetNames(),
        fetchTypes(),
        fetchUnits(),
        fetchLocations()
      ])
    } finally {
      loading.value = false
    }
  }

  return {
    assetNames,
    types,
    units,
    locations,
    loading,
    fetchAssetNames,
    fetchTypes,
    fetchUnits,
    fetchLocations,
    initData
  }
})
