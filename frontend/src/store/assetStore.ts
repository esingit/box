import { ref } from 'vue'
import { defineStore } from 'pinia'
import axiosInstance from '@/utils/axios';
import emitter from '@/utils/eventBus'
import { formatAssetRecord, clearCommonMetaCache } from '@/utils/commonMeta'
import { AssetRecordRaw } from '@/types/asset'

interface AssetRecord {
  id?: number
  typeId?: number
  unitId?: number
  assetTypeId?: number
  assetLocationId?: number
  [key: string]: any
}

interface RecordQuery {
  page?: number
  pageSize?: number
  assetNameId?: number
  locationId?: number
  typeId?: number
  startDate?: string
  endDate?: string
  remark?: string
}

export const useAssetStore = defineStore('asset', () => {
  const assetNames = ref<any[]>([])
  const types = ref<any[]>([])
  const units = ref<any[]>([])
  const locations = ref<any[]>([])
  const loading = ref(false)

  async function fetchRecords(query: RecordQuery) {
    loading.value = true
    try {
      const params: Record<string, any> = {
        page: query.page || 1,
        pageSize: query.pageSize || 10,
      }

      if (query.assetNameId) params.assetNameId = query.assetNameId
      if (query.locationId) params.locationId = query.locationId
      if (query.typeId) params.typeId = query.typeId
      if (query.startDate) params.startDate = query.startDate + 'T00:00:00'
      if (query.endDate) params.endDate = query.endDate + 'T23:59:59'
      if (query.remark) params.remark = query.remark.trim()

      const res = await axios.get('/api/asset-record/list', { params })
      if (res.data?.success) {
        const rawRecords = res.data.data.records || []
        const records = await Promise.all(
            rawRecords.map((record: AssetRecordRaw) => formatAssetRecord(record))
        )
        return {
          records,
          total: res.data.data.total,
          current: res.data.data.current,
          size: res.data.data.size
        }
      } else {
        emitter.emit('notify', {
          message: res.data?.message || '获取资产记录失败',
          type: 'error'
        })
        return null
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: `获取记录失败: ${error.message || '未知错误'}`,
        type: 'error'
      })
      return null
    } finally {
      loading.value = false
    }
  }

  async function fetchAssetNames() {
    try {
      const res = await axios.get('/api/asset-names/all')
      if (res.data?.success) {
        assetNames.value = res.data.data || []
      } else {
        emitter.emit('notify', {
          message: `获取资产名称列表失败: ${res.data?.message || '未知错误'}`,
          type: 'error'
        })
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: `获取资产名称列表失败: ${error.message || '未知错误'}`,
        type: 'error'
      })
    }
  }

  async function fetchTypes() {
    try {
      const res = await axios.get('/api/common-meta/by-type', {
        params: { typeCode: 'ASSET_TYPE' }
      })
      if (res.data?.success) {
        types.value = res.data.data || []
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: '获取资产类型列表失败',
        type: 'error'
      })
    }
  }

  async function fetchUnits() {
    try {
      const res = await axios.get('/api/common-meta/by-type', {
        params: { typeCode: 'UNIT' }
      })
      if (res.data?.success) {
        units.value = res.data.data || []
        clearCommonMetaCache()
      } else {
        emitter.emit('notify', {
          message: '获取货币单位列表失败',
          type: 'error'
        })
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: '获取货币单位列表失败',
        type: 'error'
      })
    }
  }

  async function fetchLocations() {
    try {
      const res = await axios.get('/api/common-meta/by-type', {
        params: { typeCode: 'ASSET_LOCATION' }
      })
      if (res.data?.success) {
        locations.value = res.data.data || []
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: '获取资产位置列表失败',
        type: 'error'
      })
    }
  }

  async function copyLastRecords(force = false) {
    try {
      const res = await axios.post('/api/asset-record/copy-last' + (force ? '?force=true' : ''))
      if (res.data?.success) {
        emitter.emit('notify', {
          message: '复制成功',
          type: 'success'
        })
      } else {
        throw new Error(res.data?.message || '复制失败')
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: `复制失败: ${error.message || '未知错误'}`,
        type: 'error'
      })
      throw error
    }
  }

  async function deleteRecord(id: number) {
    try {
      const res = await axios.delete(`/api/asset-record/delete/${id}`)
      if (res.data?.success) {
        emitter.emit('notify', {
          message: '删除成功',
          type: 'success'
        })
      } else {
        throw new Error(res.data?.message || '删除失败')
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: `删除失败: ${error.message || '未知错误'}`,
        type: 'error'
      })
      throw error
    }
  }

  async function addRecord(record: AssetRecord) {
    try {
      const res = await axios.post('/api/asset-record/add', record)
      if (res.data?.success) {
        emitter.emit('notify', {
          message: '添加成功',
          type: 'success'
        })
        return true
      } else {
        throw new Error(res.data?.message || '添加失败')
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: `添加失败: ${error.message || '未知错误'}`,
        type: 'error'
      })
      throw error
    }
  }

  async function updateRecord(record: AssetRecord) {
    try {
      const res = await axios.put('/api/asset-record/update', record)
      if (res.data?.success) {
        emitter.emit('notify', {
          message: '更新成功',
          type: 'success'
        })
        return true
      } else {
        throw new Error(res.data?.message || '更新失败')
      }
    } catch (error: any) {
      emitter.emit('notify', {
        message: `更新失败: ${error.message || '未知错误'}`,
        type: 'error'
      })
      throw error
    }
  }

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