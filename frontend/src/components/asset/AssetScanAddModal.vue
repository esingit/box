<template>
  <TransitionRoot appear :show="visible" as="template">
    <Dialog as="div" @close="$emit('close')" class="relative z-50">
      <TransitionChild
          as="template"
          enter="duration-300 ease-out"
          enter-from="opacity-0"
          enter-to="opacity-100"
          leave="duration-200 ease-in"
          leave-from="opacity-100"
          leave-to="opacity-0"
      >
        <div class="fixed inset-0 bg-black/25 backdrop-blur-sm" />
      </TransitionChild>

      <div class="fixed inset-0 overflow-y-auto">
        <div class="flex min-h-full items-center justify-center p-4 text-center">
          <TransitionChild
              as="template"
              enter="duration-300 ease-out"
              enter-from="opacity-0 scale-95"
              enter-to="opacity-100 scale-100"
              leave="duration-200 ease-in"
              leave-from="opacity-100 scale-100"
              leave-to="opacity-0 scale-95"
          >
            <DialogPanel class="w-full max-w-4xl transform overflow-hidden rounded-2xl bg-white dark:bg-gray-800 p-6 text-left align-middle shadow-xl transition-all">
              <DialogTitle as="h3" class="text-lg font-medium leading-6 text-gray-800 dark:text-gray-100 mb-4">
                扫图批量添加单据
              </DialogTitle>

              <!-- 上传区域 -->
              <div class="mb-6">
                <div class="flex items-center gap-4 mb-4">
                  <label for="image-upload" class="cursor-pointer inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors">
                    <Camera class="w-5 h-5" />
                    <span>上传图片/拍照</span>
                  </label>
                  <input
                      id="image-upload"
                      type="file"
                      accept="image/*"
                      capture="environment"
                      @change="handleImageUpload"
                      class="hidden"
                  />

                  <button
                      v-if="imageFile"
                      @click="recognizeImage"
                      :disabled="isRecognizing"
                      class="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:bg-gray-400 transition-colors"
                  >
                    <Scan v-if="!isRecognizing" class="w-5 h-5" />
                    <Loader2 v-else class="w-5 h-5 animate-spin" />
                    <span>{{ isRecognizing ? '识别中...' : '开始识别' }}</span>
                  </button>
                </div>

                <!-- 图片预览 -->
                <div v-if="imagePreview" class="mb-4">
                  <img :src="imagePreview" alt="预览" class="max-h-48 rounded-lg border border-gray-200 dark:border-gray-700" />
                </div>
              </div>

              <!-- 统一属性设置 -->
              <div v-if="recognizedData.length > 0" class="mb-6 p-4 bg-gray-50 dark:bg-gray-900 rounded-lg">
                <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">统一设置属性</h4>
                <div class="grid grid-cols-2 gap-4">
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">资金类型</label>
                    <select
                        v-model="commonAttributes.assetTypeId"
                        class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="">请选择</option>
                      <option v-for="type in assetTypes" :key="type.id" :value="type.id">
                        {{ type.value }}
                      </option>
                    </select>
                  </div>
                  <div>
                    <label class="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">资金位置</label>
                    <select
                        v-model="commonAttributes.assetLocationId"
                        class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-800 dark:text-gray-100 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="">请选择</option>
                      <option v-for="location in assetLocations" :key="location.id" :value="location.id">
                        {{ location.value }}
                      </option>
                    </select>
                  </div>
                </div>
              </div>

              <!-- 识别结果表格 -->
              <div v-if="recognizedData.length > 0" class="mb-6">
                <h4 class="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">识别结果 ({{ recognizedData.length }} 条)</h4>
                <div class="overflow-x-auto">
                  <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                    <thead class="bg-gray-50 dark:bg-gray-900">
                    <tr>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">资产名称</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">金额</th>
                      <th class="px-3 py-2 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">操作</th>
                    </tr>
                    </thead>
                    <tbody class="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                    <tr v-for="(item, index) in recognizedData" :key="index">
                      <td class="px-3 py-2">
                        <input
                            v-model="item.assetName"
                            type="text"
                            class="w-full px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent"
                        />
                      </td>
                      <td class="px-3 py-2">
                        <input
                            v-model.number="item.amount"
                            type="number"
                            step="0.01"
                            class="w-full px-2 py-1 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-900 text-gray-800 dark:text-gray-100 text-sm focus:ring-1 focus:ring-blue-500 focus:border-transparent"
                        />
                      </td>
                      <td class="px-3 py-2">
                        <button
                            @click="removeItem(index)"
                            class="text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300"
                        >
                          <Trash2 class="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- 底部按钮 -->
              <div class="flex justify-end gap-3">
                <button
                    @click="$emit('close')"
                    class="px-4 py-2 text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg transition-colors"
                >
                  取消
                </button>
                <button
                    @click="handleSubmit"
                    :disabled="!canSubmit"
                    class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:bg-gray-400 transition-colors"
                >
                  批量添加 ({{ recognizedData.length }} 条)
                </button>
              </div>
            </DialogPanel>
          </TransitionChild>
        </div>
      </div>
    </Dialog>
  </TransitionRoot>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Dialog, DialogPanel, DialogTitle, TransitionChild, TransitionRoot } from '@headlessui/vue'
import { Camera, Scan, Loader2, Trash2 } from 'lucide-vue-next'
import { useAssetStore } from '@/store/assetStore'
import { useMetaStore } from '@/store/metaStore'
import emitter from '@/utils/eventBus'

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  submit: [data: any]
}>()

const assetStore = useAssetStore()
const metaStore = useMetaStore()

// 状态
const imageFile = ref<File | null>(null)
const imagePreview = ref<string>('')
const isRecognizing = ref(false)
const recognizedData = ref<Array<{
  assetName: string
  amount: number
}>>([])

// 统一属性
const commonAttributes = ref({
  assetTypeId: '',
  assetLocationId: ''
})

// 元数据
const assetTypes = computed(() => metaStore.typeMap?.ASSET_TYPE?.map(i => ({
  label: String(i.value1),
  value: i.id
})) || [])
const assetLocations = computed(() => metaStore.typeMap?.ASSET_LOCATION?.map(i => ({
  label: String(i.value1),
  value: i.id
})) || [])

// 计算属性
const canSubmit = computed(() => {
  return recognizedData.value.length > 0 &&
      commonAttributes.value.assetTypeId &&
      commonAttributes.value.assetLocationId &&
      recognizedData.value.every(item => item.assetName && item.amount > 0)
})

// 处理图片上传
function handleImageUpload(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return

  imageFile.value = file

  // 创建预览
  const reader = new FileReader()
  reader.onload = (e) => {
    imagePreview.value = e.target?.result as string
  }
  reader.readAsDataURL(file)
}

// 识别图片
async function recognizeImage() {
  if (!imageFile.value) return

  isRecognizing.value = true
  try {
    const formData = new FormData()
    formData.append('image', imageFile.value)

    const result = await assetStore.recognizeAssetImage(formData)
    recognizedData.value = result || []

    if (recognizedData.value.length === 0) {
      emitter.emit('notify', {
        message: '未识别到有效数据',
        type: 'warning'
      })
    } else {
      emitter.emit('notify', {
        message: `成功识别 ${recognizedData.value.length} 条数据`,
        type: 'success'
      })
    }
  } catch (error) {
    console.error('识别失败:', error)
  } finally {
    isRecognizing.value = false
  }
}

// 删除识别项
function removeItem(index: number) {
  recognizedData.value.splice(index, 1)
}

// 提交数据
async function handleSubmit() {
  if (!canSubmit.value) return

  const records = recognizedData.value.map(item => ({
    assetName: item.assetName,
    amount: item.amount,
    assetTypeId: commonAttributes.value.assetTypeId,
    assetLocationId: commonAttributes.value.assetLocationId,
    acquireTime: new Date().toISOString().split('T')[0], // 默认今天
    remark: '批量导入'
  }))

  emit('submit', records)
}
</script>