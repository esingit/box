<template>
  <div class="dashboard-panel mt-8">
    <div class="grid grid-cols-2 gap-6">
      <div class="chart-container p-4 bg-white rounded-lg shadow-md">
        <h2 class="text-xl font-semibold mb-4">健身统计</h2>
        <div class="mb-4">
          <select v-model="selectedFitnessType" class="form-select w-full p-2 border rounded">
            <option value="" disabled>请选择健身类型</option>
            <option
                v-for="type in fitnessTypes"
                :key="type.value"
                :value="type.value"
            >
              {{ type.label }}
            </option>
          </select>
        </div>
        <Line
            v-if="fitnessChartData"
            :data="fitnessChartData"
            :options="chartOptions"
        />
      </div>
      <div class="chart-container p-4 bg-white rounded-lg shadow-md">
        <h2 class="text-xl font-semibold mb-4">资产统计</h2>
        <Line
            v-if="assetChartData"
            :data="assetChartData"
            :options="chartOptions"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue';
import {Line} from 'vue-chartjs';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend
} from 'chart.js';
import axios from '@/utils/axios';
import {useUserStore} from '@/stores/userStore';

// 注册 Chart.js 组件
ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    Title,
    Tooltip,
    Legend
);

const userStore = useUserStore();
const selectedFitnessType = ref('');
const fitnessData = ref([]);
const assetData = ref([]);

// 健身类型选项
const fitnessTypes = [
  {label: '跑步', value: 'running'},
  {label: '游泳', value: 'swimming'},
  {label: '骑行', value: 'cycling'},
  {label: '健身', value: 'workout'}
];

// 图表配置
const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
    }
  }
};

// 健身数据图表
const fitnessChartData = computed(() => {
  if (!fitnessData.value.length || !selectedFitnessType.value) return null;

  const filteredData = fitnessData.value.filter(item =>
      item.type === selectedFitnessType.value
  );

  return {
    labels: filteredData.map(item => item.date),
    datasets: [{
      label: fitnessTypes.find(t => t.value === selectedFitnessType.value)?.label || '',
      data: filteredData.map(item => item.value),
      borderColor: '#4F46E5',
      backgroundColor: '#4F46E533',
      tension: 0.4
    }]
  };
});

// 资产数据图表
const assetChartData = computed(() => {
  if (!assetData.value.length) return null;

  return {
    labels: assetData.value.map(item => item.date),
    datasets: [{
      label: '总资产',
      data: assetData.value.map(item => item.totalAmount),
      borderColor: '#059669',
      backgroundColor: '#05966933',
      tension: 0.4
    }]
  };
});

// 获取健身数据
const fetchFitnessData = async () => {
  try {
    const response = await axios.get('/api/fitness/statistics');
    fitnessData.value = response.data;
  } catch (error) {
    console.error('获取健身数据失败:', error);
  }
};

// 获取资产数据
const fetchAssetData = async () => {
  try {
    const response = await axios.get('/api/asset/statistics');
    assetData.value = response.data;
  } catch (error) {
    console.error('获取资产数据失败:', error);
  }
};

// 组件挂载时获取数据
onMounted(async () => {
  if (userStore.isAuthenticated) {
    await Promise.all([
      fetchFitnessData(),
      fetchAssetData()
    ]);
  }
});
</script>

<style scoped>
.chart-container {
  height: 400px;
}
</style>
