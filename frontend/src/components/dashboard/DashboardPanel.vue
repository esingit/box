<template>
  <div class="dashboard-wrapper">
    <div class="grid-layout">
      <!-- 健身统计卡片 -->
      <section class="card stat-component animate-fade">
        <header class="card-header">
          <h2 class="card-title">健身统计</h2>
        </header>
        <div class="card-body">
          <div v-if="fitnessError" class="alert alert-error animate-slide-up">
            {{ fitnessError }}
          </div>
          <div class="control-group mb-lg">
            <select 
              v-model="selectedFitnessType" 
              class="form-select w-full"
            >
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
          <div class="chart-container">
            <Line
              v-if="fitnessChartData"
              :data="fitnessChartData"
              :options="chartOptions"
            />
            <div v-else-if="!fitnessError" class="empty-state">
              <span class="empty-text">请选择健身类型查看统计数据</span>
              <p class="empty-description">选择健身类型后将显示相关的统计图表</p>
            </div>
          </div>
        </div>
      </section>
      
      <!-- 资产统计卡片 -->
      <section class="card stat-component animate-fade">
        <header class="card-header">
          <h2 class="card-title">资产统计</h2>
        </header>
        <div class="card-body">
          <div v-if="assetError" class="alert alert-error animate-slide-up">
            {{ assetError }}
          </div>
          <div class="chart-container">
            <Line
              v-if="assetChartData"
              :data="assetChartData"
              :options="chartOptions"
            />
            <div v-else-if="!assetError" class="empty-state">
              <span class="empty-text">暂无资产统计数据</span>
              <p class="empty-description">资产数据将在这里以图表形式展示</p>
            </div>
          </div>
        </div>
      </section>
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
const fitnessError = ref('');
const assetError = ref('');

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

  if (filteredData.length === 0) {
    return null;
  }

  return {
    labels: filteredData.map(item => item.date),
    datasets: [{
      label: fitnessTypes.find(t => t.value === selectedFitnessType.value)?.label || '',
      data: filteredData.map(item => item.value),
      borderColor: 'var(--color-primary)',
      backgroundColor: 'var(--color-primary-light)',
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
      borderColor: 'var(--color-success)',
      backgroundColor: 'var(--color-success-light)',
      tension: 0.4
    }]
  };
});

// 获取健身数据
const fetchFitnessData = async () => {
  try {
    fitnessError.value = '';
    const response = await axios.get('/api/fitness/statistics');
    fitnessData.value = response.data;
    if (fitnessData.value.length > 0) {
      // 默认选择第一个有数据的类型
      const availableTypes = [...new Set(fitnessData.value.map(item => item.type))];
      if (availableTypes.length > 0) {
        selectedFitnessType.value = availableTypes[0];
      }
    }
  } catch (error) {
    console.error('获取健身数据失败:', error);
    fitnessError.value = '获取健身数据失败，请稍后重试';
  }
};

// 获取资产数据
const fetchAssetData = async () => {
  try {
    assetError.value = '';
    const response = await axios.get('/api/asset/statistics');
    assetData.value = response.data;
  } catch (error) {
    console.error('获取资产数据失败:', error);
    assetError.value = '获取资产数据失败，请稍后重试';
  }
};

// 组件挂载时获取数据
onMounted(async () => {
  if (userStore.isLoggedIn) {
    await Promise.all([
      fetchFitnessData(),
      fetchAssetData()
    ]);
  }
});
</script>


