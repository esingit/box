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

  // 获取资产记录列表
  async function fetchRecords({ page = 1, pageSize = 10, typeId, startDate, endDate, remark }) {
    try {
      const params = {
        page,
        pageSize
      }

      // 添加非空的查询参数
      if (typeId) params.typeId = typeId
      if (startDate) params.startDate = startDate + 'T00:00:00'
      if (endDate) params.endDate = endDate + 'T23:59:59'
      if (remark) params.remark = remark.trim()

      console.log('发送查询请求，参数：', params)

      const res = await axios.get('/api/asset-record/list', { params })
      console.log('查询响应：', res.data)
      
      if (res.data?.success) {
        return {
          records: res.data.data.records || [],
          total: res.data.data.total,
          current: res.data.data.current,
          size: res.data.data.size
        }
      } else {
        throw new Error(res.data?.message || '获取资产记录失败')
      }
    } catch (error) {
      console.error('获取资产记录失败:', error)
      emitter.emit('notify', `获取资产记录失败: ${error.message || '未知错误'}`, 'error')
      throw error
    }
  }

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
      console.error('获取资产名称列表失败:', error)
      emitter.emit('notify', `获取资产名称列表失败: ${error.message || '未知错误'}`, 'error')
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

  // 复制上次记录
  async function copyLastRecords() {
    try {
      const res = await axios.post('/api/asset-record/copy-last')
      if (res.data?.success) {
        emitter.emit('notify', '复制成功', 'success')
      } else {
        throw new Error(res.data?.message || '复制失败')
      }
    } catch (error) {
      console.error('复制上次记录失败:', error)
      emitter.emit('notify', `复制失败: ${error.message || '未知错误'}`, 'error')
      throw error
    }
  }

  // 删除记录
  async function deleteRecord(id) {
    try {
      const res = await axios.delete(`/api/asset-record/delete/${id}`)
      if (res.data?.success) {
        emitter.emit('notify', '删除成功', 'success')
      } else {
        throw new Error(res.data?.message || '删除失败')
      }
    } catch (error) {
      console.error('删除记录失败:', error)
      emitter.emit('notify', `删除失败: ${error.message || '未知错误'}`, 'error')
      throw error
    }
  }

  // 添加记录
  async function addRecord(record) {
    try {
      const res = await axios.post('/api/asset-record/add', record)
      if (res.data?.success) {
        emitter.emit('notify', '添加成功', 'success')
        return true
      } else {
        throw new Error(res.data?.message || '添加失败')
      }
    } catch (error) {
      console.error('添加记录失败:', error)
      emitter.emit('notify', `添加失败: ${error.message || '未知错误'}`, 'error')
      throw error
    }
  }

  // 更新记录
  async function updateRecord(record) {
    try {
      const res = await axios.put('/api/asset-record/update', record)
      if (res.data?.success) {
        emitter.emit('notify', '更新成功', 'success')
        return true
      } else {
        throw new Error(res.data?.message || '更新失败')
      }
    } catch (error) {
      console.error('更新记录失败:', error)
      emitter.emit('notify', `更新失败: ${error.message || '未知错误'}`, 'error')
      throw error
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
    fetchRecords,
    fetchAssetNames,
    fetchTypes,
    fetchUnits,
    fetchLocations,
    copyLastRecords,
    deleteRecord,
    addRecord,
    updateRecord,
    initData
  }
})
