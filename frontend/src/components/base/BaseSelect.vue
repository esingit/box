<!-- BaseSelect.vue -->
<template>
  <div class="relative w-full">
    <Listbox
        :model-value="modelValue"
        :multiple="multiple"
        @update:model-value="handleUpdateModelValue"
        as="div"
        v-slot="{ open }"
    >
      <div class="relative w-full">
        <ListboxButton
            :title="showError ? (requiredMessage || '此项为必填') : selectedText"
            :class="[
              'input-base flex justify-between items-center w-full pr-8 min-w-0 text-left', // 添加 text-left
              showError ? 'msg-error' : ''
            ]"
            @blur="onBlur"
            @click="handleButtonClick(open)"
        >
          <div class="flex-1 min-w-0 mr-2 text-left"> <!-- 添加 text-left -->
            <span
                class="truncate whitespace-nowrap block w-full text-left"
                :class="{
                  'text-gray-400': !selectedLabels.length && !showError,
                  'text-black': selectedLabels.length || showError
                }"
            >
              {{ showError ? (requiredMessage || '此项为必填') : selectedText }}
            </span>
          </div>

          <div class="flex items-center gap-[15px] flex-shrink-0">
            <button
                v-if="clearable && hasValue"
                @click.stop="clearSelection"
                type="button"
                class="text-gray-400 hover:text-gray-600 transition"
                title="清除"
                tabindex="-1"
            >
              ✕
            </button>
            <LucideChevronDown
                class="w-4 h-4 text-gray-400 transition-transform duration-200"
                :class="{ 'rotate-180': open }"
            />
          </div>
        </ListboxButton>

        <Transition
            enter-active-class="transition duration-100 ease-out"
            enter-from-class="transform scale-95 opacity-0"
            enter-to-class="transform scale-100 opacity-100"
            leave-active-class="transition duration-75 ease-in"
            leave-from-class="transform scale-100 opacity-100"
            leave-to-class="transform scale-95 opacity-0"
        >
          <ListboxOptions
              v-show="open"
              :class="[
                'absolute z-[9999] overflow-hidden rounded-2xl bg-white border border-gray-300 text-sm shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none',
                direction === 'up' ? 'mb-1 bottom-full' : 'mt-1 top-full',
                'w-full min-w-[200px]'
              ]"
              @blur="onOptionsBlur"
          >
            <!-- 搜索框 -->
            <div v-if="searchable" class="p-2 border-b border-gray-200">
              <div class="relative">
                <input
                    ref="searchInput"
                    v-model="searchQuery"
                    type="text"
                    placeholder="搜索..."
                    class="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent text-left"
                    @click.stop
                    @keydown.stop
                    @keydown.esc="closeDropdown"
                />
                <LucideSearch class="absolute right-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              </div>
            </div>

            <!-- 选项列表 -->
            <div class="max-h-60 overflow-auto p-2">
              <div v-if="!filteredOptions.length" class="px-3 py-8 text-center text-gray-500">
                {{ searchQuery ? '未找到匹配项' : '暂无数据' }}
              </div>

              <ListboxOption
                  v-for="item in filteredOptions"
                  :key="item.value"
                  :value="item.value"
                  v-slot="{ active, selected }"
              >
                <div
                    class="flex items-center px-3 py-2 rounded-xl cursor-pointer select-none transition-colors duration-150 text-left"
                    :class="{
                      'bg-gray-100': active,
                      'font-semibold text-black': selected,
                      'text-gray-800': !selected
                    }"
                >
                  <template v-if="multiple">
                    <span class="mr-2 flex items-center justify-center w-5 h-5">
                      <component
                          :is="selected ? CheckCircle : Circle"
                          class="w-5 h-5"
                          :class="selected ? 'text-black' : 'text-black'"
                      />
                    </span>
                  </template>
                  <span
                      class="text-left"
                      v-html="highlightMatch(item.label, searchQuery)"
                  ></span>
                </div>
              </ListboxOption>
            </div>
          </ListboxOptions>
        </Transition>
      </div>
    </Listbox>
  </div>
</template>

<script setup lang="ts">
// script 部分保持不变
import {computed, nextTick, ref, watch} from 'vue'
import {Listbox, ListboxButton, ListboxOption, ListboxOptions} from '@headlessui/vue'
import {CheckCircle, Circle, LucideChevronDown, LucideSearch} from 'lucide-vue-next'

interface Option {
  label: string
  value: string | number
}

interface BaseSelectProps {
  modelValue?: string | number | (string | number)[] | null
  options?: Option[]
  placeholder?: string
  title: string
  multiple?: boolean
  direction?: 'up' | 'down'
  clearable?: boolean
  required?: boolean
  requiredMessage?: string
  searchable?: boolean
}

const props = withDefaults(defineProps<BaseSelectProps>(), {
  options: () => [],
  placeholder: '',
  multiple: false,
  direction: 'down',
  clearable: false,
  required: false,
  requiredMessage: '',
  searchable: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | (string | number)[] | null]
  'blur': []
  'dropdown-open': []
  'dropdown-close': []
}>()

const searchInput = ref<HTMLInputElement | null>(null)
const showError = ref(false)
const searchQuery = ref('')
const isDropdownOpen = ref(false)

// 处理模型值更新
const handleUpdateModelValue = (val: string | number | (string | number)[] | null) => {
  emit('update:modelValue', val)
  showError.value = false
}

// 处理按钮点击
const handleButtonClick = (open: boolean) => {
  isDropdownOpen.value = open
  if (open) {
    emit('dropdown-open')
    // 延迟聚焦搜索框
    if (props.searchable) {
      nextTick(() => {
        setTimeout(() => {
          searchInput.value?.focus()
        }, 100)
      })
    }
  } else {
    emit('dropdown-close')
  }
}

// 关闭下拉框
const closeDropdown = () => {
  isDropdownOpen.value = false
  emit('dropdown-close')
}

const safeOptions = computed(() => props.options ?? [])

// 过滤选项
const filteredOptions = computed(() => {
  if (!props.searchable || !searchQuery.value.trim()) {
    return safeOptions.value
  }

  const query = searchQuery.value.toLowerCase().trim()
  return safeOptions.value.filter(option =>
      option.label.toLowerCase().includes(query)
  )
})

// 高亮匹配文本
const highlightMatch = (text: string, query: string) => {
  if (!query.trim()) return text

  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<mark class="bg-yellow-200 text-black">$1</mark>')
}

function isArrayValue(val: unknown): val is (string | number)[] {
  return Array.isArray(val)
}

const selectedLabels = computed(() => {
  const options = safeOptions.value
  const value = props.modelValue

  if (props.multiple && isArrayValue(value)) {
    return options.filter(opt => value.includes(opt.value)).map(opt => opt.label)
  } else {
    const selected = options.find(opt => opt.value === value)
    return selected ? [selected.label] : []
  }
})

const computedPlaceholder = computed(() => props.placeholder || `请选择${props.title}`)

// 优化多选显示
const MAX_DISPLAY_ITEMS = 2
const selectedText = computed(() => {
  if (!selectedLabels.value.length) {
    return computedPlaceholder.value
  }

  if (props.multiple && selectedLabels.value.length > MAX_DISPLAY_ITEMS) {
    const displayItems = selectedLabels.value.slice(0, MAX_DISPLAY_ITEMS)
    return `${displayItems.join('、')} 等${selectedLabels.value.length}项`
  }

  return selectedLabels.value.join('、')
})

const hasValue = computed(() =>
    props.multiple
        ? Array.isArray(props.modelValue) && props.modelValue.length > 0
        : !!props.modelValue
)

function clearSelection() {
  const newValue = props.multiple ? [] : null
  emit('update:modelValue', newValue)
  showError.value = false
}

function onBlur() {
  // 延迟执行，避免与选项点击冲突
  setTimeout(() => {
    emit('blur')
    if (props.required) {
      showError.value = props.multiple
          ? !Array.isArray(props.modelValue) || props.modelValue.length === 0
          : !props.modelValue || props.modelValue === '';
    }
  }, 150)
}

function onOptionsBlur() {
  // 当选项区域失去焦点时的处理
  setTimeout(() => {
    if (!document.activeElement?.closest('.relative')) {
      closeDropdown()
    }
  }, 100)
}

// 重置搜索查询
const resetSearch = () => {
  searchQuery.value = ''
}

// 监听 dropdown 状态变化，重置搜索
watch(isDropdownOpen, (newVal) => {
  if (!newVal) {
    resetSearch()
  }
})

// 暴露方法
defineExpose({
  validate: () => {
    onBlur()
    return !showError.value
  },
  resetSearch,
  closeDropdown
})
</script>