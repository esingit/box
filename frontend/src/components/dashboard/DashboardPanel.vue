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
                v-for="type in metaTypes"
                :key="type.key1"
                :value="type.key1"
              >
                {{ type.value1 }}
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
import {useRouter} from 'vue-router';
import {Line} from 'vue-chartjs';
import {useMetaData} from '@/composables/useMetaData';
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
const router = useRouter();
const { types: metaTypes, fetchMetaData } = useMetaData();
const selectedFitnessType = ref('PUSH_UP'); // 默认选择俯卧撑
const fitnessData = ref([]);
const assetData = ref([]);
const fitnessError = ref('');
const assetError = ref('');

// 图表配置
const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top',
    },
    tooltip: {
      mode: 'index',
      intersect: false
    }
  },
  scales: {
    x: {
      title: {
        display: true,
        text: '日期'
      },
      grid: {
        color: 'rgba(0, 0, 0, 0.1)'
      }
    },
    y: {
      title: {
        display: true,
        text: '数量'
      },
      beginAtZero: true,
      grid: {
        color: 'rgba(0, 0, 0, 0.1)'
      }
    }
  }
};

// 健身数据图表
const fitnessChartData = computed(() => {
  if (!fitnessData.value.length || !metaTypes.value.length) return null;

  // 获取所有日期
  const dates = [...new Set(fitnessData.value.map(item => 
    item.finishTime.split('T')[0]
  ))].sort();
  
  // 只显示选中类型的数据
  const selectedType = metaTypes.value.find(type => type.key1 === selectedFitnessType.value);
  if (!selectedType) return null;

  const typeData = fitnessData.value.filter(item => 
    item.typeId === selectedType.id && item.count > 0
  );
  
  // 如果没有数据，返回null
  if (typeData.length === 0) return null;
  
  const dataByDate = dates.map(date => {
    const record = typeData.find(item => item.finishTime.split('T')[0] === date);
    return record ? Number(record.count) : 0;
  });

  return {
    labels: dates,
    datasets: [{
      label: selectedType.value1,
      data: dataByDate,
      borderColor: 'var(--color-primary)',
      backgroundColor: 'var(--color-primary-light)',
      tension: 0.4,
      fill: false
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
    const response = await axios.get('/api/fitness-record/list', {
      params: {
        page: 1,
        pageSize: 999, // 获取足够多的记录以支持统计
        startDate: new Date(new Date().setMonth(new Date().getMonth() - 1)).toISOString().split('T')[0], // 一个月前
        endDate: new Date().toISOString().split('T')[0] // 今天
      }
    });
    if (response.data?.success && response.data.data?.records) {
      fitnessData.value = response.data.data.records;
    } else {
      fitnessData.value = [];
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

// 获取健身类型元数据
const fetchFitnessTypes = async () => {
  try {
    await fetchMetaData();
  } catch (error) {
    console.error('获取健身类型失败:', error);
    fitnessError.value = '获取健身类型失败，请稍后重试';
  }
};

// 验证用户身份
const verifyUserAuth = async () => {
  if (!userStore.isLoggedIn || !userStore.token) {
    console.debug('用户未登录，跳转到登录页面');
    router.push('/login');
    return false;
  }
  return true;
};

// 组件挂载时获取数据
onMounted(async () => {
  if (await verifyUserAuth()) {
    try {
      await fetchFitnessTypes(); // 首先获取健身类型元数据
      await Promise.all([
        fetchFitnessData(),
        fetchAssetData()
      ]);
    } catch (error) {
      console.error('初始化数据失败:', error);
    }
  }
});
</script>


