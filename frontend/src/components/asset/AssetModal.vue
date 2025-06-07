<template>
  <div v-if="show" class="modal-overlay">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">{{ title }}</h3>
        <button class="close-button" @click="$emit('cancel')">
          <LucideX class="close-icon" />
        </button>
      </div>
      <div class="input-group">
        <label class="input-label">
          资产名称
          <span class="required">*</span>
        </label>
        <div class="asset-name-controls">
          <select id="assetName" v-model="form.assetNameId" class="asset-name-select select" required>
            <option value="">请选择资产名称</option>
            <option v-for="name in assetNames" :key="name.id" :value="name.id">
              {{ name.name }}
            </option>
          </select>
          <button class="btn btn-gray maintain-btn" @click.prevent="handleMaintainClick">
            <LucideSettings size="16" class="btn-icon" />
            维护
          </button>
        </div>
      </div>
      <div class="input-group">
        <label class="input-label">
          资产分类
          <span class="required">*</span>
        </label>
        <select id="assetType" v-model="form.assetTypeId" class="type-select select" required>
          <option value="">请选择资产分类</option>
          <option v-for="type in types" :key="type.id" :value="type.id">
            {{ type.value1 }}
          </option>
        </select>
      </div>
      <div class="input-group">
        <label class="input-label">
          资产位置
          <span class="required">*</span>
        </label>
        <select id="location" v-model="form.assetLocationId" class="location-select select" required>
          <option value="">请选择资产位置</option>
          <option v-for="location in locations" :key="location.id" :value="location.id">
            {{ location.value1 }}
          </option>
        </select>
      </div>
      <div class="input-group">
        <label class="input-label">
          金额
          <span class="required">*</span>
        </label>
        <input
          id="amount"
          v-model="form.amount"
          class="amount-input input"
          type="number"
          step="0.01"
          min="0"
          required
          placeholder="请输入金额"
        />
      </div>
      <div class="input-group">
        <label class="input-label">
          货币单位
          <span class="required">*</span>
        </label>
        <select id="unit" v-model="form.unitId" class="unit-select select" required>
          <option value="">请选择货币单位</option>
          <option v-for="unit in units" :key="unit.id" :value="unit.id">
            {{ unit.value1 }}
          </option>
        </select>
      </div>
      <div class="input-group">
        <label class="input-label">
          日期
          <span class="required">*</span>
        </label>
        <input id="date" v-model="form.acquireTime" type="date" class="date-input input" required />
      </div>
      <div class="input-group">
        <label class="input-label">备注</label>
        <input id="remark" v-model="form.remark" type="text" class="remark-input input" :placeholder="remarkPlaceholder" />
      </div>
      <div class="modal-actions">
        <button class="btn btn-test" @click="$emit('cancel')">取消</button>
        <button
          class="btn btn-primary"
          :disabled="loading || !isFormValid"
          @click="$emit('submit')"
        >
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
  <AssetNamesModal
    v-if="showNamesModal"
    :show="showNamesModal"
    @close="handleNamesModalClose"
    @refresh="handleNamesModalRefresh"
  />
</template>

<script setup>
import { LucideX, LucideSettings } from 'lucide-vue-next'
import { computed, ref, watch } from 'vue'
import AssetNamesModal from './AssetNamesModal.vue'

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  form: {
    type: Object,
    required: true
  },
  assetNames: {
    type: Array,
    required: true
  },
  types: {
    type: Array,
    required: true
  },
  units: {
    type: Array,
    required: true
  },
  locations: {
    type: Array,
    required: true
  },
  loading: {
    type: Boolean,
    default: false
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  remarkPlaceholder: {
    type: String,
    default: '备注'
  }
})

const isFormValid = computed(() => {
  return props.form.assetNameId &&
         props.form.assetTypeId &&
         props.form.assetLocationId &&
         props.form.amount > 0 &&
         props.form.unitId &&
         props.form.acquireTime
})

// 确保acquireTime是ISO格式的日期时间字符串
const formatDate = (date) => {
  if (!date) return null
  // 如果已经是日期时间格式，直接返回
  if (date.includes('T')) return date
  // 否则添加时间部分
  return `${date}T00:00:00`
}

const emit = defineEmits(['submit', 'cancel', 'refresh-names'])

const handleSubmit = () => {
  if (isFormValid.value) {
    // 确保日期格式正确
    const formData = { ...props.form }
    formData.acquireTime = formatDate(formData.acquireTime)
    emit('submit', formData)
  }
}

const showNamesModal = ref(false)

function handleNamesModalRefresh() {
  emit('refresh-names')
}

function handleMaintainClick() {
  showNamesModal.value = true
}

function handleNamesModalClose() {
  showNamesModal.value = false
}

// 监听父组件的 show 属性变化
watch(() => props.show, (newVal) => {
  if (!newVal) {
    // 当父模态框关闭时，确保子模态框也关闭
    showNamesModal.value = false
  }
})
</script>

