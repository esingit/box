<template>
  <div class="bg-white border rounded-md p-4 shadow animate-fade max-w-3xl mx-auto">
    <h2 class="text-lg font-semibold mb-4">健身统计</h2>
    <div class="mb-4">
      <select
          multiple
          v-model="selectedFitnessTypes"
          @change="handleFitnessTypesChange"
          class="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-400"
          size="4"
      >
        <option
            v-for="option in fitnessTypeOptions"
            :key="option.value"
            :value="option.value"
        >{{ option.label }}</option>
      </select>
    </div>
    <div class="relative h-72">
      <template v-if="fitnessError">
        <div class="flex flex-col items-center justify-center h-full text-red-600">
          <p class="mb-2 font-semibold">{{ fitnessError }}</p>
          <p class="text-sm text-gray-500">{{ getEmptyDescription('健身') }}</p>
        </div>
      </template>
      <template v-else-if="fitnessChartData">
        <canvas ref="chartRef"></canvas>
      </template>
      <template v-else>
        <div class="flex flex-col items-center justify-center h-full text-gray-400">
          <p class="mb-2">暂无数据</p>
          <p class="text-sm">{{ getEmptyDescription('健身') }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue';
import Chart from 'chart.js/auto';
import axiosInstance from '@/utils/axios';

const selectedFitnessTypes = ref(['PUSH_UP']);
const fitnessData = ref([]);
const fitnessError = ref('');
const chartInstance = ref(null);
const chartRef = ref(null);

const metaTypes = ref([]); // 你从接口拿到的元数据

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
        generateLabels: (chart) =>
            chart.data.datasets.map((dataset, i) => ({
              text: dataset.label,
              fillStyle: 'transparent',
              strokeStyle: '#d1d5db',
              lineWidth: 1,
              hidden: !chart.getDatasetMeta(i).hidden,
              index: i,
            })),
      },
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: (context) => `${context.dataset.label}: ${formatAmount(context.parsed.y)}`,
      },
    },
  },
  scales: {
    x: { title: { display: true, text: '日期' }, grid: { color: 'rgba(0,0,0,0.1)' } },
    y: { title: { display: true, text: '次数' }, beginAtZero: true, grid: { color: 'rgba(0,0,0,0.1)' } },
  },
};

const fitnessChartData = computed(() => {
  if (!fitnessData.value.length || !selectedFitnessTypes.value.length || !metaTypes.value.length) return null;

  const dates = [...new Set(fitnessData.value.map(i => i.finishTime.split('T')[0]))].sort();
  const datasets = selectedFitnessTypes.value
      .map((typeKey, index) => {
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
          fill: false,
        };
      })
      .filter(Boolean);

  const formattedDates = dates.map((d) => {
    const dateObj = new Date(d);
    return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
  });

  return { labels: formattedDates, datasets };
});

async function fetchMetaData() {
  try {
    const res = await axiosInstance.get('/api/meta/types', { params: { typeCode: 'FITNESS_TYPE' } });
    if (res.data?.success) {
      metaTypes.value = res.data.data || [];
    }
  } catch {
    metaTypes.value = [];
  }
}

async function fetchFitnessData() {
  try {
    fitnessError.value = '';
    fitnessData.value = [];
    const now = new Date();
    const start = new Date();
    start.setDate(now.getDate() - 30);

    const res = await axiosInstance.get('/api/fitness-record/list', {
      params: {
        page: 1,
        pageSize: 999,
        startDate: start.toISOString().split('T')[0],
        endDate: now.toISOString().split('T')[0],
        fitnessType: selectedFitnessTypes.value.join(','),
      },
    });

    fitnessData.value = res.data?.data?.records || [];
  } catch {
    fitnessError.value = '获取健身数据失败，请稍后重试';
  }
}

function renderChart() {
  if (chartInstance.value) {
    chartInstance.value.destroy();
    chartInstance.value = null;
  }
  if (!fitnessChartData.value) return;
  const ctx = chartRef.value.getContext('2d');
  chartInstance.value = new Chart(ctx, {
    type: 'line',
    data: fitnessChartData.value,
    options: chartOptions,
  });
}

async function handleFitnessTypesChange() {
  await fetchFitnessData();
}

watch([fitnessChartData], () => {
  renderChart();
});

onMounted(async () => {
  await fetchMetaData();
  await fetchFitnessData();
});
</script>

<style scoped>
.animate-fade {
  animation: fadeIn 0.3s ease forwards;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
.select {
  /* 你可以根据需要自定义select样式 */
}
.chart-wrapper {
  position: relative;
  height: 18rem; /* 72 */
}
.empty-state {
  padding: 2rem;
  text-align: center;
}
</style>
