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
                class="form-select"
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
              <p class="empty-description">暂无健身统计数据</p>
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
          <div class="control-group mb-lg">
            <select
                v-model="selectedAssetType"
                class="form-select"
                @change="handleAssetTypeChange"
            >
              <option value="" disabled>请选择资产类型</option>
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
                class="form-select"
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
          <div class="chart-wrapper">
            <Line
                v-if="assetChartData"
                :data="assetChartData"
                :options="chartOptions"
            />
            <div v-else-if="!assetError" class="empty-state">
              <span class="empty-text">请选择资产类型查看统计数据</span>
              <p class="empty-description">暂无资产统计数据</p>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue';
import {useRouter} from 'vue-router';
import {Line} from 'vue-chartjs';
import {useMetaData} from '@/composables/useMetaData';
import {
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip
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
const {types: metaTypes, fetchMetaData} = useMetaData();
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
  if (!Array.isArray(assetNames.value)) {
    console.debug('资产名称不是数组:', assetNames.value);
    return [];
  }
  return assetNames.value;
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

  // 格式化日期，只显示月和日
  const formattedDates = dates.map(date => {
    const dateObj = new Date(date);
    return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
  });

  return {
    labels: formattedDates, // 使用格式化后的日期
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

  // 获取所有日期
  const dates = [...new Set(assetData.value.map(item => item.date))].sort();
  
  // 按资产名称分组数据
  const groupedData = {};
  
  // 如果选择了具体资产名称，则只显示该资产名称的数据
  // 否则，按资产名称分组显示所有数据
  if (selectedAssetName.value) {
    // 找到选中的资产名称
    const selectedName = assetNames.value.find(name => name.id === selectedAssetName.value);
    if (selectedName) {
      const nameData = assetData.value.filter(item => item.assetNameId === selectedAssetName.value);
      
      if (nameData.length > 0) {
        groupedData[selectedName.name] = {
          label: selectedName.name,
          data: {}
        };
        
        // 填充日期数据
        nameData.forEach(item => {
          groupedData[selectedName.name].data[item.date] = item.amount;
        });
      }
    }
  } else {
    // 未选择具体资产名称时，按日期汇总所有理财资产的总额
    groupedData['理财总额'] = {
      label: '理财总额',
      data: {}
    };
    
    // 按日期汇总金额
    dates.forEach(date => {
      const dayTotal = assetData.value
        .filter(item => item.date === date)
        .reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
      
      groupedData['理财总额'].data[date] = dayTotal;
    });
  }

  // 创建数据集
  const datasets = Object.keys(groupedData).map((key, index) => ({
    label: groupedData[key].label,
    data: dates.map(date => groupedData[key].data[date] || 0),
    borderColor: `hsl(${index * 137.5}, 70%, 50%)`,
    backgroundColor: `hsla(${index * 137.5}, 70%, 50%, 0.1)`,
    tension: 0.4,
    fill: false
  }));

  // 格式化日期，只显示月和日
  const formattedDates = dates.map(date => {
    const dateObj = new Date(date);
    return `${dateObj.getMonth() + 1}/${dateObj.getDate()}`;
  });

  return {
    labels: formattedDates,
    datasets
  };
});

// 获取资产数据
const fetchAssetData = async () => {
  try {
    assetError.value = '';
    assetData.value = [];

    // 构建请求参数
    const params = {};
    // 总是发送资产类型ID，默认为理财类型
    if (selectedAssetType.value) {
      params.assetTypeId = selectedAssetType.value;
      console.debug('获取资产统计数据，类型ID:', selectedAssetType.value);
    }
    // 只有在选择了具体资产名称时才发送
    if (selectedAssetName.value && selectedAssetName.value !== '') {
      params.assetNameId = selectedAssetName.value;
      console.debug('获取资产统计数据，名称ID:', selectedAssetName.value);
    }

    // 请求统计数据
    console.debug('发送统计请求，参数:', params);
    const response = await axios.get('/api/asset/statistics', {params});
    console.debug('资产统计数据响应:', response.data);
    
    if (Array.isArray(response.data) && response.data.length > 0) {
      assetData.value = response.data;
      console.debug('成功获取资产统计数据:', {
        count: assetData.value.length,
        dates: [...new Set(assetData.value.map(item => item.date))].sort()
      });
    } else {
      console.warn('没有找到资产统计数据');
      assetError.value = selectedAssetType.value ? '暂无相关资产数据' : '请选择资产类型查看统计';
    }
  } catch (error) {
    console.error('获取资产数据失败:', error);
    if (error.response) {
      console.error('错误响应:', {
        status: error.response.status,
        data: error.response.data
      });
    }
    assetError.value = '获取资产数据失败，请稍后重试';
  }
};

// 获取健身数据
const fetchFitnessData = async () => {
  try {
    fitnessError.value = '';
    // 设置日期范围为最近31天
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - 30); // 31天（今天+过去30天）
    
    const response = await axios.get('/api/fitness-record/list', {
      params: {
        page: 1,
        pageSize: 999, // 获取足够多的记录以支持统计
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0]
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

// 获取资产名称
const fetchAssetNames = async () => {
  try {
    // 确保有选择资产类型
    if (!selectedAssetType.value) {
      console.debug('未选择资产类型，不获取资产名称');
      assetNames.value = [];
      return;
    }

    console.debug('开始获取资产名称，类型ID:', selectedAssetType.value);
    const response = await axios.get('/api/asset-names/all', {
      params: {
        assetTypeId: selectedAssetType.value
      }
    });
    
    console.debug('资产名称API响应:', response.data);
    if (response.data?.success && Array.isArray(response.data.data)) {
      assetNames.value = response.data.data;
      console.debug('成功获取资产名称列表:', {
        count: assetNames.value.length,
        names: assetNames.value.map(n => ({ id: n.id, name: n.name }))
      });
    } else {
      console.warn('资产名称数据格式不正确:', response.data);
      assetNames.value = [];
    }
  } catch (error) {
    console.error('获取资产名称失败:', error);
    if (error.response) {
      console.error('错误响应:', {
        status: error.response.status,
        data: error.response.data
      });
    }
    assetError.value = '获取资产名称失败，请稍后重试';
    assetNames.value = [];
  }
};

// 处理资产类型变化
async function handleAssetTypeChange() {
  console.debug('资产类型变更为:', selectedAssetType.value);
  // 清空已选择的资产名称
  selectedAssetName.value = '';
  // 获取新类型下的资产名称列表
  await fetchAssetNames();
  // 获取该类型下所有资产的统计数据
  await fetchAssetData();
}

// 处理资产名称变化
async function handleAssetNameChange() {
  console.debug('资产名称变更为:', selectedAssetName.value);
  // 获取选中资产名称的统计数据
  await fetchAssetData();
}

// 组件挂载时获取数据
onMounted(async () => {
  if (await verifyUserAuth()) {
    try {
      await fetchFitnessTypes(); // 首先获取健身类型元数据
      await fetchFitnessData();

      // 获取资产类型
      const typesRes = await axios.get('/api/common-meta/by-type', {params: {typeCode: 'ASSET_TYPE'}});

      if (typesRes.data?.success) {
        assetTypes.value = typesRes.data.data || [];
        // 默认选择理财类型
        const financeType = assetTypes.value.find(type => type.key1 === 'FINANCE');
        if (financeType) {
          console.debug('设置默认资产类型:', financeType);
          selectedAssetType.value = financeType.id;
          // 获取资产名称，但不选择具体名称
          await fetchAssetNames();
          // 获取该类型下所有资产的统计数据
          await fetchAssetData();
        }
      }
    } catch (error) {
      console.error('初始化数据失败:', error);
    }
  }
});
</script>







