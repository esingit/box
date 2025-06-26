<!-- BaseSelect.vue 修复位移问题 -->
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
              'input-base flex justify-between items-center w-full pr-8 min-w-0 text-left',
              showError ? 'input-error' : 'input-normal'
            ]"
            @blur="onBlur"
            @click="handleButtonClick(open, close)"
        >
          <div class="flex-1 min-w-0 mr-2 text-left">
            <span
                class="truncate whitespace-nowrap block w-full text-left"
                :class="{
                  'text-gray-400': !selectedLabels.length && !showError,
                  'text-red-500': showError,
                  'text-black': selectedLabels.length && !showError
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

        <!-- 使用 Teleport 渲染下拉框到 body -->
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
                      @keydown.esc="() => closeDropdown(close)"
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
                    @click="() => closeDropdown(close)"
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
        </Teleport>
      </div>
    </Listbox>
  </div>
</template>

<script setup lang="ts">
// ... script 部分保持不变，不需要修改 ...
import { computed, nextTick, ref, watch, onMounted, onUnmounted } from 'vue'
import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from '@headlessui/vue'
import { CheckCircle, Circle, LucideChevronDown, LucideSearch } from 'lucide-vue-next'
import { useFloating, autoUpdate, offset, flip, shift, size } from '@floating-ui/vue'

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

// Refs
const containerRef = ref<HTMLElement>()
const referenceRef = ref<HTMLElement>()
const floatingRef = ref<HTMLElement>()
const searchInput = ref<HTMLInputElement | null>(null)
const showError = ref(false)
const searchQuery = ref('')
const isDropdownOpen = ref(false)
const closeFunction = ref<(() => void) | null>(null)

// 使用 floating-ui 进行定位
const { floatingStyles, update } = useFloating(referenceRef, floatingRef, {
  placement: props.direction === 'up' ? 'top-start' : 'bottom-start',
  middleware: [
    offset(4),
    flip({
      fallbackPlacements: ['top-start', 'bottom-start', 'top-end', 'bottom-end']
    }),
    shift({ padding: 8 }),
    size({
      apply({ rects, elements }) {
        Object.assign(elements.floating.style, {
          width: `${rects.reference.width}px`,
          minWidth: `${rects.reference.width}px`,
          maxWidth: `${Math.max(rects.reference.width, 200)}px`
        })
      }
    })
  ],
  whileElementsMounted: autoUpdate,
  strategy: 'fixed'
})

// 处理模型值更新
const handleUpdateModelValue = (val: string | number | (string | number)[] | null) => {
  emit('update:modelValue', val)
  showError.value = false
}

// 处理按钮点击
const handleButtonClick = (open: boolean, close: () => void) => {
  isDropdownOpen.value = open
  closeFunction.value = close

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
    // 更新浮动位置
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

// 关闭下拉框
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

function onOptionsBlur(close: () => void) {
  // 当选项区域失去焦点时的处理
  setTimeout(() => {
    if (!document.activeElement?.closest('.relative')) {
      closeDropdown(close)
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

// 处理滚动时更新位置
const handleScroll = () => {
  if (isDropdownOpen.value && update) {
    update()
  }
}

// 处理窗口大小变化
const handleResize = () => {
  if (isDropdownOpen.value && update) {
    update()
  }
}

// 生命周期钩子
onMounted(() => {
  document.addEventListener('scroll', handleScroll, true)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', handleResize)
})

// 暴露方法
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