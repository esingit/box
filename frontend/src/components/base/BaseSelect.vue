<template>
  <div class="relative w-full" ref="containerRef">
    <Listbox
        :model-value="modelValue"
        :multiple="multiple"
        @update:model-value="handleUpdateModelValue"
        as="div"
        v-slot="{ open, close }"
    >
      <div class="relative w-full">
        <ListboxButton
            ref="referenceRef"
            :title="showError ? (requiredMessage || '此项为必填') : selectedText"
            :class="[
              'input-base flex items-center w-full min-w-0 text-left relative',
              props.clearable ? 'pr-16' : 'pr-10', // 根据是否有清除按钮动态调整
              showError ? 'input-error' : 'input-normal'
            ]"
            @blur="onBlur"
            @click="handleButtonClick(open, close)"
        >
          <!-- 文字内容区域 -->
          <div class="flex-1 min-w-0 text-left overflow-hidden">
            <span
                class="block w-full text-left truncate"
                :class="{
                  'text-gray-400': !selectedLabels.length && !showError,
                  'text-[var(--color-error)]': !selectedLabels.length && showError,
                  'text-black': selectedLabels.length
                }"
            >
              {{ selectedText }}
            </span>
          </div>

          <!-- 右侧图标区域 -->
          <div class="flex items-center gap-3 flex-shrink-0 absolute right-4 top-1/2 transform -translate-y-1/2">
            <button
                v-if="props.clearable && hasValue"
                @click.stop="clearSelection"
                type="button"
                class="text-gray-400 hover:text-gray-600 transition-colors flex-shrink-0 p-0.5 rounded hover:bg-gray-100"
                title="清除"
                tabindex="-1"
            >
              <span class="block w-3.5 h-3.5 text-center leading-none text-sm">✕</span>
            </button>
            <LucideChevronDown
                class="w-4 h-4 text-gray-400 transition-transform duration-200 flex-shrink-0"
                :class="{ 'rotate-180': open }"
            />
          </div>
        </ListboxButton>

        <Teleport to="body">
          <Transition
              enter-active-class="dropdown-enter-active"
              enter-from-class="dropdown-enter-from"
              enter-to-class="dropdown-enter-to"
              leave-active-class="dropdown-leave-active"
              leave-from-class="dropdown-leave-from"
              leave-to-class="dropdown-leave-to"
          >
            <ListboxOptions
                v-if="open"
                ref="floatingRef"
                :class="[
                  'absolute z-[9999] overflow-hidden rounded-2xl bg-white border border-gray-300 text-sm shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none',
                  props.direction === 'up' ? 'origin-bottom' : 'origin-top'
                ]"
                :style="floatingStyles"
                @blur="() => onOptionsBlur(close)"
            >
              <!-- 搜索框 -->
              <div v-if="props.searchable" class="p-3 border-b border-gray-200 bg-gray-50">
                <div class="relative flex items-center">
                  <!-- 搜索图标 -->
                  <div class="absolute left-0 top-0 bottom-0 flex items-center justify-center w-10 pointer-events-none z-10">
                    <LucideSearch class="w-4 h-4 text-gray-400" />
                  </div>

                  <!-- 输入框 -->
                  <input
                      ref="searchInput"
                      v-model="searchQuery"
                      type="text"
                      placeholder="搜索选项..."
                      class="w-full h-10 pl-10 pr-10 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-gray-500 focus:border-transparent bg-white"
                      style="padding-left: 2.5rem !important;"
                      @click.stop
                      @keydown.stop
                      @keydown.esc="() => closeDropdown(close)"
                  />

                  <!-- 清除搜索按钮 -->
                  <button
                      v-if="searchQuery"
                      @click="searchQuery = ''"
                      type="button"
                      class="absolute right-0 top-0 bottom-0 flex items-center justify-center w-10 text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    <span class="text-sm">✕</span>
                  </button>
                </div>
              </div>

              <!-- 选项列表 -->
              <div
                  class="overflow-auto py-1"
                  :class="computedMaxHeight"
                  :style="{
                    maxHeight: dynamicMaxHeight
                  }"
              >
                <!-- 空状态 -->
                <div v-if="!filteredOptions.length" class="px-4 py-12 text-center text-gray-500">
                  <div class="flex flex-col items-center gap-2">
                    <div class="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center">
                      <LucideSearch class="w-4 h-4 text-gray-400" />
                    </div>
                    <div class="text-sm">
                      {{ searchQuery ? '未找到匹配项' : '暂无数据' }}
                    </div>
                    <div v-if="searchQuery" class="text-xs text-gray-400">
                      尝试其他关键词
                    </div>
                  </div>
                </div>

                <!-- 选项组 -->
                <div v-else class="px-1">
                  <!-- 已选择的选项（多选时显示在顶部） -->
                  <template v-if="props.multiple && selectedOptions.length > 0 && !searchQuery">
                    <div class="px-3 py-1 text-xs font-medium text-gray-500 uppercase tracking-wide">
                      已选择 ({{ selectedOptions.length }})
                    </div>
                    <ListboxOption
                        v-for="item in selectedOptions"
                        :key="`selected-${item.value}`"
                        :value="item.value"
                        v-slot="{ active, selected }"
                        @click="() => closeDropdown(close)"
                    >
                      <div
                          class="flex items-center justify-between px-3 py-2.5 mx-1 rounded-lg cursor-pointer select-none transition-all duration-150 text-left"
                          :class="{
                            'bg-gray-50 text-gray-900': active && selected,
                            'bg-gray-100 text-gray-800': !active && selected,
                            'bg-gray-50': active && !selected,
                            'text-gray-800': !selected
                          }"
                      >
                        <div class="flex items-center min-w-0 flex-1">
                          <span class="mr-3 flex items-center justify-center w-5 h-5 flex-shrink-0">
                            <CheckCircle class="w-5 h-5 text-gray-600" />
                          </span>
                          <span
                              class="truncate text-sm font-medium"
                              v-html="highlightMatch(item.label, searchQuery)"
                          ></span>
                        </div>
                      </div>
                    </ListboxOption>

                    <!-- 分割线 -->
                    <div class="h-px bg-gray-200 mx-3 my-2"></div>

                    <!-- 未选择的选项标题 -->
                    <div class="px-3 py-1 text-xs font-medium text-gray-500 uppercase tracking-wide">
                      其他选项
                    </div>
                  </template>

                  <!-- 所有选项或未选择的选项 -->
                  <ListboxOption
                      v-for="item in displayOptions"
                      :key="item.value"
                      :value="item.value"
                      v-slot="{ active, selected }"
                      @click="() => closeDropdown(close)"
                  >
                    <div
                        class="flex items-center justify-between px-3 py-2.5 mx-1 rounded-lg cursor-pointer select-none transition-all duration-150 text-left group"
                        :class="{
                          'bg-gray-50 text-gray-900': active && selected,
                          'bg-gray-100 text-gray-800': !active && selected,
                          'bg-gray-50': active && !selected,
                          'text-gray-800': !selected,
                          'hover:bg-gray-50': !active && !selected
                        }"
                    >
                      <div class="flex items-center min-w-0 flex-1">
                        <template v-if="props.multiple">
                          <span class="mr-3 flex items-center justify-center w-5 h-5 flex-shrink-0">
                            <component
                                :is="selected ? CheckCircle : Circle"
                                class="w-5 h-5 transition-colors"
                                :class="selected ? 'text-gray-600' : 'text-gray-400 group-hover:text-gray-500'"
                            />
                          </span>
                        </template>

                        <div class="min-w-0 flex-1">
                          <span
                              class="block truncate text-sm"
                              :class="{ 'font-medium': selected, 'font-normal': !selected }"
                              v-html="highlightMatch(item.label, searchQuery)"
                          ></span>
                          <!-- 可以在这里添加副标题或描述 -->
                          <span v-if="item.description" class="block truncate text-xs text-gray-500 mt-0.5">
                            {{ item.description }}
                          </span>
                        </div>
                      </div>

                      <!-- 选中标识（单选） -->
                      <div v-if="!props.multiple && selected" class="ml-2 flex-shrink-0">
                        <CheckCircle class="w-4 h-4 text-gray-600" />
                      </div>
                    </div>
                  </ListboxOption>
                </div>

                <!-- 底部统计信息 -->
                <div v-if="filteredOptions.length > 0" class="px-4 py-2 border-t border-gray-100 bg-gray-50 text-xs text-gray-500">
                  <div class="flex justify-between items-center">
                    <span>
                      {{ searchQuery ? `找到 ${filteredOptions.length} 个匹配项` : `共 ${filteredOptions.length} 个选项` }}
                    </span>
                    <span v-if="props.multiple && hasValue">
                      已选择 {{ Array.isArray(props.modelValue) ? props.modelValue.length : 0 }} 项
                    </span>
                  </div>
                </div>
              </div>
            </ListboxOptions>
          </Transition>
        </Teleport>
      </div>
    </Listbox>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch, onMounted, onUnmounted } from 'vue'
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue'
import { CheckCircle, Circle, LucideChevronDown, LucideSearch } from 'lucide-vue-next'
import { useFloating, autoUpdate, offset, flip, shift, size } from '@floating-ui/vue'

// 类型定义
type SingleValue = string | number | null
type MultipleValue = (string | number)[]
type SelectValue = SingleValue | MultipleValue

interface Option {
  label: string
  value: string | number
  description?: string
}

interface BaseSelectProps {
  modelValue?: SelectValue
  options?: Option[]
  placeholder?: string
  title: string
  multiple?: boolean
  direction?: 'up' | 'down'
  clearable?: boolean
  required?: boolean
  requiredMessage?: string
  searchable?: boolean
  maxDisplayHeight?: string
}

const props = withDefaults(defineProps<BaseSelectProps>(), {
  options: () => [],
  placeholder: '',
  multiple: false,
  direction: 'down',
  clearable: false,
  required: false,
  requiredMessage: '',
  searchable: true,
  maxDisplayHeight: '400px'
})

const emit = defineEmits<{
  'update:modelValue': [value: SelectValue]
  'blur': []
  'dropdown-open': []
  'dropdown-close': []
}>()

// Refs
const containerRef = ref<HTMLElement>()
const referenceRef = ref<HTMLElement>()
const floatingRef = ref<HTMLElement>()
const searchInput = ref<HTMLInputElement | null>(null)
const showError = ref(false)
const searchQuery = ref('')
const isDropdownOpen = ref(false)
const closeFunction = ref<(() => void) | null>(null)

// 动态计算最大高度
const dynamicMaxHeight = computed(() => {
  const searchHeight = props.searchable ? 60 : 0
  const footerHeight = 40
  const maxHeight = Math.min(
      window.innerHeight * 0.6,
      parseInt(props.maxDisplayHeight)
  )

  return `${maxHeight - searchHeight - footerHeight}px`
})

const computedMaxHeight = computed(() => {
  return 'max-h-[300px] sm:max-h-[400px] lg:max-h-[500px]'
})

// Floating UI
const { floatingStyles, update } = useFloating(referenceRef, floatingRef, {
  placement: props.direction === 'up' ? 'top-start' : 'bottom-start',
  middleware: [
    offset(4),
    flip({
      fallbackPlacements: ['top-start', 'bottom-start', 'top-end', 'bottom-end']
    }),
    shift({ padding: 8 }),
    size({
      apply({ rects, elements, availableHeight }) {
        const minWidth = Math.max(rects.reference.width, 200)
        const maxWidth = Math.min(rects.reference.width * 1.5, 400)

        Object.assign(elements.floating.style, {
          width: `${rects.reference.width}px`,
          minWidth: `${minWidth}px`,
          maxWidth: `${maxWidth}px`,
          maxHeight: `${Math.min(availableHeight - 20, parseInt(props.maxDisplayHeight))}px`
        })
      }
    })
  ],
  whileElementsMounted: autoUpdate,
  strategy: 'fixed'
})

const handleUpdateModelValue = (val: SelectValue) => {
  emit('update:modelValue', val)
  showError.value = false
}

const handleButtonClick = (open: boolean, close: () => void) => {
  isDropdownOpen.value = open
  closeFunction.value = close

  if (open) {
    emit('dropdown-open')
    if (props.searchable) {
      nextTick(() => {
        setTimeout(() => {
          searchInput.value?.focus()
        }, 100)
      })
    }
    nextTick(() => {
      if (update) {
        update()
      }
    })
  } else {
    emit('dropdown-close')
    closeFunction.value = null
  }
}

const closeDropdown = (close?: () => void) => {
  const closeFunc = close || closeFunction.value
  if (closeFunc) {
    closeFunc()
  }
  isDropdownOpen.value = false
  emit('dropdown-close')
  closeFunction.value = null
}

const safeOptions = computed(() => props.options ?? [])

const filteredOptions = computed(() => {
  if (!props.searchable || !searchQuery.value.trim()) {
    return safeOptions.value
  }

  const query = searchQuery.value.toLowerCase().trim()
  return safeOptions.value.filter(option =>
      option.label.toLowerCase().includes(query) ||
      (option.description && option.description.toLowerCase().includes(query))
  )
})

const selectedOptions = computed(() => {
  if (!props.multiple || !props.modelValue) return []

  // 确保是数组且有includes方法
  const modelValueArray = Array.isArray(props.modelValue) ? props.modelValue : []

  return safeOptions.value.filter(option =>
      modelValueArray.includes(option.value)
  )
})

const displayOptions = computed(() => {
  if (!props.multiple || searchQuery.value) {
    return filteredOptions.value
  }

  const selectedValues = Array.isArray(props.modelValue) ? props.modelValue : []
  return filteredOptions.value.filter(option =>
      !selectedValues.includes(option.value)
  )
})

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

const selectedText = computed(() => {
  if (!selectedLabels.value.length) {
    return `请选择${props.title || ''}`
  }

  // 最多显示5项
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
  setTimeout(() => {
    emit('blur')
    if (props.required) {
      showError.value = props.multiple
          ? !Array.isArray(props.modelValue) || props.modelValue.length === 0
          : !props.modelValue || props.modelValue === '';
    }
  }, 150)
}

function onOptionsBlur(close: () => void) {
  setTimeout(() => {
    if (!document.activeElement?.closest('.relative')) {
      closeDropdown(close)
    }
  }, 100)
}

// 最多显示5项
const MAX_DISPLAY_ITEMS = 3

const resetSearch = () => {
  searchQuery.value = ''
}

watch(isDropdownOpen, (newVal) => {
  if (!newVal) {
    resetSearch()
  }
})

const handleScroll = () => {
  if (isDropdownOpen.value && update) {
    update()
  }
}

const handleResize = () => {
  if (isDropdownOpen.value && update) {
    update()
  }
}

onMounted(() => {
  document.addEventListener('scroll', handleScroll, true)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', handleResize)
})

defineExpose({
  validate: () => {
    onBlur()
    return !showError.value
  },
  resetSearch,
  closeDropdown: () => closeDropdown(),
  update
})
</script>