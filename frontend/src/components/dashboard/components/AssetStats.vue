<template>
  <n-card title="资产统计" bordered class="stat-component animate-fade">
    <n-space vertical>
        <n-select
            v-model:value="selectedAssetType"
            :options="assetTypeOptions"
            placeholder="请选择资产类型"
            @update:value="handleAssetTypeChange"
            clearable
            multiple
            class="select"
        />
        <n-select
            v-if="selectedAssetType.length"
            v-model:value="selectedAssetName"
            :options="filteredAssetNameOptions"
            placeholder="全部资产名称"
            @update:value="handleAssetNameChange"
            clearable
            multiple
            class="select"
        />
      <div class="chart-wrapper">
        <n-empty v-if="assetError" :description="assetError" status="error" class="empty-state">
          <template #description>{{ getEmptyDescription('资产') }}</template>
        </n-empty>
        <Line v-else-if="assetChartData" :data="assetChartData" :options="chartOptions"/>
        <n-empty v-else description="暂无数据" class="empty-state">
          <template #description>{{ getEmptyDescription('资产') }}</template>
        </n-empty>
      </div>
    </n-space>
  </n-card>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue';
import {Line} from 'vue-chartjs';
import axios from '@/utils/axios';
import {useUserStore} from '@/store/userStore';

const userStore = useUserStore();
const selectedAssetType = ref([]);
const selectedAssetName = ref([]);
const assetTypes = ref([]);
const assetNames = ref([]);
const assetData = ref([]);
const assetError = ref('');

const assetTypeOptions = computed(() =>
    assetTypes.value.map(type => ({label: type.value1, value: type.id}))
);
const filteredAssetNameOptions = computed(() =>
    assetNames.value.map(name => ({label: name.name, value: name.id}))
);

function getEmptyDescription(type) {
  return `暂无相关${type}统计数据`;
}

function formatAmount(value) {
  return value == null ? '-' : new Intl.NumberFormat('zh-CN', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
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
              datasetIndex: i
            }))
      }
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: ctx => {
          const label = ctx.dataset.label ? ctx.dataset.label + ': ' : '';
          return ctx.parsed.y != null ? label + `￥${formatAmount(ctx.parsed.y)}` : label;
        }
      }
    }
  },
  scales: {
    x: {title: {display: true, text: '日期'}, grid: {color: 'rgba(0,0,0,0.1)'}},
    y: {title: {display: true, text: '金额'}, beginAtZero: true, grid: {color: 'rgba(0,0,0,0.1)'}}
  }
};

const assetChartData = computed(() => {
  if (!assetData.value.length) return null;

  const dates = [...new Set(assetData.value.map(item => item.date))].sort();
  const groupedMap = new Map();

  const selectedNameSet = new Set(selectedAssetName.value);
  const nameMap = new Map(assetNames.value.map(n => [n.id, n.name]));

  if (selectedAssetName.value.length > 0) {
    assetData.value.forEach(item => {
      if (!selectedNameSet.has(item.assetNameId)) return;
      const label = nameMap.get(item.assetNameId) || '未知资产';
      if (!groupedMap.has(label)) groupedMap.set(label, {});
      groupedMap.get(label)[item.date] = item.amount;
    });
  } else {
    const totalMap = {};
    dates.forEach(date => {
      totalMap[date] = assetData.value
          .filter(item => item.date === date)
          .reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
    });
    groupedMap.set('总额', totalMap);
  }

  const datasets = [...groupedMap.entries()].map(([label, data]) => ({
    label,
    data: dates.map(date => data[date] || 0),
    borderColor: '#8e8ea0',
    backgroundColor: '#acacbe',
    pointRadius: 3,
    pointBackgroundColor: '#999',
    tension: 0.4,
    fill: false
  }));

  const formattedDates = dates.map(date => {
    const d = new Date(date);
    return `${d.getMonth() + 1}/${d.getDate()}`;
  });

  return {labels: formattedDates, datasets};
});

const fetchAssetNames = async () => {
  if (!selectedAssetType.value.length) {
    assetNames.value = [];
    return;
  }
  const response = await axios.get('/api/asset-names/all', {
    params: {assetTypeId: selectedAssetType.value.join(',')}
  });
  if (response.data?.success && Array.isArray(response.data.data)) {
    assetNames.value = response.data.data;
  } else {
    assetNames.value = [];
  }
};

const fetchAssetData = async () => {
  try {
    assetError.value = '';
    assetData.value = [];
    const params = {};
    if (selectedAssetType.value.length) params.assetTypeId = selectedAssetType.value.join(',');
    if (selectedAssetName.value.length) params.assetNameId = selectedAssetName.value.join(',');
    const response = await axios.get('/api/asset/statistics', {params});
    if (Array.isArray(response.data)) {
      assetData.value = response.data;
    }
  } catch {
    assetError.value = '获取资产数据失败，请稍后重试';
  }
};

const handleAssetTypeChange = async () => {
  selectedAssetName.value = [];
  await fetchAssetNames();
  await fetchAssetData();
};
const handleAssetNameChange = async () => {
  await fetchAssetData();
};

onMounted(async () => {
  if (!userStore.isLoggedIn || !userStore.token) return;
  const res = await axios.get('/api/common-meta/by-type', {
    params: {typeCode: 'ASSET_TYPE'}
  });
  if (res.data?.success) {
    assetTypes.value = res.data.data || [];
    const finance = assetTypes.value.find(t => t.key1 === 'FINANCE');
    if (finance) {
      selectedAssetType.value = [finance.id];
      await fetchAssetNames();
      await fetchAssetData();
    }
  }
});
</script>
