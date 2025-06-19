<template>
  <div class="border rounded-lg shadow p-6 animate-fade">
    <h2 class="text-lg font-semibold mb-4">资产统计</h2>
    <div class="flex flex-col space-y-4 mb-6">
      <select
          multiple
          v-model="selectedAssetType"
          @change="handleAssetTypeChange"
          class="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-400"
      >
        <option v-for="option in assetTypeOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>

      <select
          v-if="selectedAssetType.length"
          multiple
          v-model="selectedAssetName"
          @change="handleAssetNameChange"
          class="border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-400"
      >
        <option v-for="option in filteredAssetNameOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
    </div>

    <div class="min-h-[250px] flex items-center justify-center border border-gray-200 rounded">
      <template v-if="assetError">
        <div class="text-red-600 text-center px-4">
          {{ getEmptyDescription('资产') }}<br />
          <span class="font-semibold">{{ assetError }}</span>
        </div>
      </template>
      <template v-else-if="assetChartData">
        <Line :data="assetChartData" :options="chartOptions" class="w-full h-64" />
      </template>
      <template v-else>
        <div class="text-gray-400 text-center px-4">
          {{ getEmptyDescription('资产') }}
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Line } from 'vue-chartjs'
import axiosInstance from '@/utils/axios';
import { useUserStore } from '@/store/userStore'

const userStore = useUserStore()
const selectedAssetType = ref([])
const selectedAssetName = ref([])
const assetTypes = ref([])
const assetName = ref([])
const assetData = ref([])
const assetError = ref('')

const assetTypeOptions = computed(() =>
    assetTypes.value.map(type => ({ label: type.value1, value: type.id }))
)
const filteredAssetNameOptions = computed(() =>
    assetName.value.map(name => ({ label: name.name, value: name.id }))
)

function getEmptyDescription(type) {
  return `暂无相关${type}统计数据`
}

function formatAmount(value) {
  return value == null
      ? '-'
      : new Intl.NumberFormat('zh-CN', {
        style: 'decimal',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(value)
}

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      labels: {
        position: 'top',
        generateLabels: chart =>
            chart.data.datasets.map((dataset, i) => ({
              text: dataset.label,
              fillStyle: 'rgba(0, 0, 0, 0)',
              strokeStyle: '#d1d5db',
              lineWidth: 1,
              hidden: !chart.getDatasetMeta(i).visible,
              index: i,
              datasetIndex: i,
            })),
      },
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: ctx => {
          const label = ctx.dataset.label ? ctx.dataset.label + ': ' : ''
          return ctx.parsed.y != null ? label + `￥${formatAmount(ctx.parsed.y)}` : label
        },
      },
    },
  },
  scales: {
    x: { title: { display: true, text: '日期' }, grid: { color: 'rgba(0,0,0,0.1)' } },
    y: { title: { display: true, text: '金额' }, beginAtZero: true, grid: { color: 'rgba(0,0,0,0.1)' } },
  },
}

const assetChartData = computed(() => {
  if (!assetData.value.length) return null

  const dates = [...new Set(assetData.value.map(item => item.date))].sort()
  const groupedMap = new Map()

  const selectedNameSet = new Set(selectedAssetName.value)
  const nameMap = new Map(assetName.value.map(n => [n.id, n.name]))

  if (selectedAssetName.value.length > 0) {
    assetData.value.forEach(item => {
      if (!selectedNameSet.has(item.assetNameId)) return
      const label = nameMap.get(item.assetNameId) || '未知资产'
      if (!groupedMap.has(label)) groupedMap.set(label, {})
      groupedMap.get(label)[item.date] = item.amount
    })
  } else {
    const totalMap = {}
    dates.forEach(date => {
      totalMap[date] = assetData.value
          .filter(item => item.date === date)
          .reduce((sum, item) => sum + (Number(item.amount) || 0), 0)
    })
    groupedMap.set('总额', totalMap)
  }

  const datasets = [...groupedMap.entries()].map(([label, data]) => ({
    label,
    data: dates.map(date => data[date] || 0),
    borderColor: '#8e8ea0',
    backgroundColor: '#acacbe',
    pointRadius: 3,
    pointBackgroundColor: '#999',
    tension: 0.4,
    fill: false,
  }))

  const formattedDates = dates.map(date => {
    const d = new Date(date)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })

  return { labels: formattedDates, datasets }
})

const fetchAssetName = async () => {
  if (!selectedAssetType.value.length) {
    assetName.value = []
    return
  }
  const response = await axiosInstance.get('/api/asset-names/all', {
    params: { assetTypeId: selectedAssetType.value.join(',') },
  })
  if (response.data?.success && Array.isArray(response.data.data)) {
    assetName.value = response.data.data
  } else {
    assetName.value = []
  }
}

const fetchAssetData = async () => {
  try {
    assetError.value = ''
    assetData.value = []
    const params = {}
    if (selectedAssetType.value.length) params.assetTypeId = selectedAssetType.value.join(',')
    if (selectedAssetName.value.length) params.assetNameId = selectedAssetName.value.join(',')
    const response = await axiosInstance.get('/api/asset/statistics', { params })
    if (Array.isArray(response.data)) {
      assetData.value = response.data
    }
  } catch {
    assetError.value = '获取资产数据失败，请稍后重试'
  }
}

const handleAssetTypeChange = async () => {
  selectedAssetName.value = []
  await fetchAssetName()
  await fetchAssetData()
}
const handleAssetNameChange = async () => {
  await fetchAssetData()
}

onMounted(async () => {
  if (!userStore.isLoggedIn || !userStore.token) return
  const res = await axiosInstance.get('/api/common-meta/by-type', {
    params: { typeCode: 'ASSET_TYPE' },
  })
  if (res.data?.success) {
    assetTypes.value = res.data.data || []
    const finance = assetTypes.value.find(t => t.key1 === 'FINANCE')
    if (finance) {
      selectedAssetType.value = [finance.id]
      await fetchAssetName()
      await fetchAssetData()
    }
  }
})
</script>

<style scoped>
.animate-fade {
  animation: fadeIn 0.3s ease forwards;
}
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
