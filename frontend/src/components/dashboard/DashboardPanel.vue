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
              class="form-select fitness-type-select"
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
          <div class="chart-wrapper">
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
          <div class="chart-wrapper">
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
          <div class="control-group mb-lg">
            <div class="control-group">
              <select 
                v-model="selectedAssetType" 
                class="input control-select"
                @change="handleAssetTypeChange"
              >
                <option value="" disabled>资产类型</option>
                <option
                  v-for="type in assetTypes"
                  :key="type.id"
                  :value="type.id"
                >
                  {{ type.value1 }}
                </option>
              </select>
              <select 
                v-if="selectedAssetType"
                v-model="selectedAssetName" 
                class="input control-select"
                @change="handleAssetNameChange"
              >
                <option value="">全部资产名称</option>
                <option
                  v-for="name in filteredAssetNames"
                  :key="name.id"
                  :value="name.id"
                >
                  {{ name.name }}
                </option>
              </select>
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

// 资产类型和名称相关的状态
const selectedAssetType = ref('');
const selectedAssetName = ref('');
const assetTypes = ref([]);
const assetNames = ref([]);

const filteredAssetNames = computed(() => {
  if (!selectedAssetType.value) return [];
  return assetNames.value.filter(name => name.typeId === selectedAssetType.value);
});

// 格式化金额
function formatAmount(value) {
  if (value == null) return '-';
  return new Intl.NumberFormat('zh-CN', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(value);
}

// 图表配置
const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'top'
    },
    tooltip: {
      mode: 'index',
      intersect: false,
      callbacks: {
        label: (context) => {
          let label = context.dataset.label || '';
          if (label) {
            label += ': ';
          }
          if (context.parsed.y !== null) {
            label += `￥${formatAmount(context.parsed.y)}`;
          }
          return label;
        }
      }
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
        text: '金额（人民币）'
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

  // 按资产名称或类型分组数据
  const groupedData = {};
  const dates = [...new Set(assetData.value.map(item => item.date))].sort();

  assetData.value.forEach(item => {
    // 如果选择了具体资产名称，则按名称分组，否则按类型分组
    const key = selectedAssetName.value ? item.assetName : item.assetTypeName;
    if (!groupedData[key]) {
      groupedData[key] = {
        label: key,
        data: {},
        typeId: item.assetTypeId
      };
    }
    groupedData[key].data[item.date] = item.amount;
  });

  // 创建数据集
  const sortedKeys = Object.keys(groupedData).sort((a, b) => {
    // 首先按类型ID排序，然后按名称排序
    const typeIdA = groupedData[a].typeId;
    const typeIdB = groupedData[b].typeId;
    if (typeIdA === typeIdB) {
      return a.localeCompare(b);
    }
    return typeIdA - typeIdB;
  });

  const datasets = sortedKeys.map((key, index) => ({
    label: groupedData[key].label,
    data: dates.map(date => groupedData[key].data[date] || 0),
    borderColor: `hsl(${index * 137.5}, 70%, 50%)`,
    backgroundColor: `hsla(${index * 137.5}, 70%, 50%, 0.1)`,
    tension: 0.4,
    fill: false
  }));

  return {
    labels: dates,
    datasets
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
    assetData.value = [];

    const params = {};
    if (selectedAssetType.value) {
      params.assetTypeId = selectedAssetType.value;
    }
    if (selectedAssetName.value) {
      params.assetNameId = selectedAssetName.value;
    }
    const response = await axios.get('/api/asset/statistics', { params });
    if (response.data?.length > 0) {
      assetData.value = response.data;
    } else {
      assetError.value = selectedAssetType.value ? '暂无相关资产数据' : '请选择资产类型查看统计';
    }
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

// 处理资产类型变化
async function handleAssetTypeChange() {
  selectedAssetName.value = '';
  await fetchAssetData();
}

// 处理资产名称变化
async function handleAssetNameChange() {
  await fetchAssetData();
}

// 组件挂载时获取数据
onMounted(async () => {
  if (await verifyUserAuth()) {
    try {
      await fetchFitnessTypes(); // 首先获取健身类型元数据
      await Promise.all([
        fetchFitnessData(),
        fetchAssetData()
      ]);

      // 获取资产类型和名称
      const [typesRes, namesRes] = await Promise.all([
        axios.get('/api/common-meta/by-type', { params: { typeCode: 'ASSET_TYPE' }}),
        axios.get('/api/asset-names/all')
      ]);
      
      if (typesRes.data?.success) {
        assetTypes.value = typesRes.data.data || [];
        // 默认选择理财类型
        const financeType = assetTypes.value.find(type => type.key1 === 'FINANCE');
        if (financeType) {
          selectedAssetType.value = financeType.id;
        }
      }
      
      if (namesRes.data?.success) {
        assetNames.value = namesRes.data.data || [];
      }
      
      await fetchAssetData();
    } catch (error) {
      console.error('初始化数据失败:', error);
    }
  }
});
</script>

<style scoped>
.ml-md {
  margin-left: 1rem;
}

.control-group {
  display: flex;
  gap: var(--spacing-md);
  align-items: center;
}

.control-select {
  flex: 1;
  min-width: 160px;
  margin: 0;
}

.input {
  appearance: none;
  background-color: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  color: var(--color-text);
  font-size: 0.9rem;
  padding: 0.5rem 2rem 0.5rem 0.75rem;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%236b7280' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 0.5rem center;
  background-repeat: no-repeat;
  background-size: 1.5em 1.5em;
}

.form-select:focus {
  border-color: var(--color-primary);
  outline: none;
  box-shadow: 0 0 0 2px var(--color-primary-light);
}
</style>






