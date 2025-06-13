<template>
  <n-card title="健身统计" bordered class="stat-component animate-fade">
    <n-space vertical>
      <n-select
          v-model:value="selectedFitnessTypes"
          :options="fitnessTypeOptions"
          placeholder="请选择健身类型"
          @update:value="handleFitnessTypesChange"
          clearable
          multiple
          class="select"
      />
      <div class="chart-wrapper">
        <n-empty v-if="fitnessError" :description="fitnessError" status="error" class="empty-state">
          <template #description>{{ getEmptyDescription('健身') }}</template>
        </n-empty>
        <Line v-else-if="fitnessChartData" :data="fitnessChartData" :options="chartOptions" />
        <n-empty v-else description="暂无数据" class="empty-state">
          <template #description>{{ getEmptyDescription('健身') }}</template>
        </n-empty>
      </div>
    </n-space>
  </n-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { Line } from 'vue-chartjs';
import axios from '@/utils/axios';
import { useMetaData } from '@/composables/useMetaData';
import { NCard, NEmpty, NSelect, NSpace } from 'naive-ui';
import {
  Chart as ChartJS, Title, Tooltip, Legend, LineElement,
  CategoryScale, LinearScale, PointElement
} from 'chart.js';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend);

const selectedFitnessTypes = ref(['PUSH_UP']);
const fitnessData = ref([]);
const fitnessError = ref('');
const { types: metaTypes, fetchMetaData } = useMetaData();

const fitnessTypeOptions = computed(() =>
    metaTypes.value.map(item => ({ label: item.value1, value: item.key1 }))
);

function getEmptyDescription(type) {
  return `暂无相关${type}统计数据`;
}

function formatAmount(value) {
  if (value == null) return '-';
  return new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
}

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      labels: {
        generateLabels: (chart) => chart.data.datasets.map((dataset, i) => ({
          text: dataset.label, fillStyle: 'transparent', strokeStyle: '#d1d5db', lineWidth: 1,
          hidden: !chart.getDatasetMeta(i).visible, index: i
        }))
      }
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: context => `${context.dataset.label}: ￥${formatAmount(context.parsed.y)}`
      }
    }
  },
  scales: {
    x: { title: { display: true, text: '日期' }, grid: { color: 'rgba(0,0,0,0.1)' } },
    y: { title: { display: true, text: '次数' }, beginAtZero: true, grid: { color: 'rgba(0,0,0,0.1)' } }
  }
};

const fitnessChartData = computed(() => {
  if (!fitnessData.value.length || !selectedFitnessTypes.value.length || !metaTypes.value.length) return null;

  const dates = [...new Set(fitnessData.value.map(i => i.finishTime.split('T')[0]))].sort();
  const datasets = selectedFitnessTypes.value.map((typeKey, index) => {
    const typeMeta = metaTypes.value.find(type => type.key1 === typeKey);
    if (!typeMeta) return null;

    const typeData = dates.map(date => {
      const record = fitnessData.value.find(r => r.typeId === typeMeta.id && r.finishTime.startsWith(date));
      return record ? Number(record.count) : 0;
    });

    return {
      label: typeMeta.value1,
      data: typeData,
      borderColor: `hsl(${index * 60}, 70%, 50%)`,
      backgroundColor: `hsla(${index * 60}, 70%, 50%, 0.5)`,
      pointRadius: 3,
      pointBackgroundColor: `hsl(${index * 60}, 70%, 40%)`,
      tension: 0.4,
      fill: false
    };
  }).filter(Boolean);

  const formattedDates = dates.map(d => {
    const dateObj = new Date(d);
    return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
  });

  return { labels: formattedDates, datasets };
});

const fetchFitnessData = async () => {
  try {
    fitnessError.value = '';
    fitnessData.value = [];
    const now = new Date();
    const start = new Date(); start.setDate(now.getDate() - 30);

    const res = await axios.get('/api/fitness-record/list', {
      params: {
        page: 1,
        pageSize: 999,
        startDate: start.toISOString().split('T')[0],
        endDate: now.toISOString().split('T')[0],
        fitnessType: selectedFitnessTypes.value.join(',')
      }
    });

    fitnessData.value = res.data?.data?.records || [];
  } catch {
    fitnessError.value = '获取健身数据失败，请稍后重试';
  }
};

const handleFitnessTypesChange = async () => {
  await fetchFitnessData();
};

onMounted(async () => {
  await fetchMetaData();
  await fetchFitnessData();
});
</script>
