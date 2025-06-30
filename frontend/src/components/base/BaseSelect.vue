<template>
  <div class="relative w-full" ref="containerRef">
    <!-- 统一管理的Tooltip -->
    <Teleport to="body">
      <div
          v-if="tooltip.visible && tooltip.content"
          class="fixed z-[10000] px-2 py-1 bg-gray-800 text-white text-xs rounded shadow-lg pointer-events-none"
          :style="{
          left: tooltip.position.x + 'px',
          top: tooltip.position.y + 'px'
        }"
      >
        {{ tooltip.content }}
      </div>
    </Teleport>

    <Listbox
        :model-value="modelValue"
        :multiple="multiple"
        @update:model-value="handleUpdateModelValue"
        as="div"
        v-slot="{ open, close }"
    >
      <div class="relative w-full">
        <div
            ref="titleWrapperRef"
            class="relative w-full select-no-title"
            @mouseenter="showInputTooltip"
            @mouseleave="hideTooltip"
            @mousemove="updateTooltipPosition"
        >
          <ListboxButton
              ref="referenceRef"
              :class="[
              'input-base flex items-center w-full min-w-0 text-left relative select-no-title',
              props.clearable ? 'pr-16' : 'pr-10',
              showError ? 'input-error' : 'input-normal'
            ]"
              @blur="onBlur"
              @click="handleButtonClick(open, close)"
          >
            <div class="flex-1 min-w-0 text-left overflow-hidden select-no-title">
              <span
                  class="block w-full text-left truncate select-no-title"
                  :class="{
                  'text-gray-400': !selectedLabels.length && !showError,
                  'text-red-500': !selectedLabels.length && showError,
                  'text-black': selectedLabels.length
                }"
              >
                {{ selectedText }}
              </span>
            </div>

            <div class="flex items-center gap-3 flex-shrink-0 absolute right-4 top-1/2 transform -translate-y-1/2">
              <button
                  v-if="props.clearable && hasValue"
                  @click.stop="clearSelection"
                  type="button"
                  class="text-gray-400 hover:text-gray-600 transition-colors flex-shrink-0 p-0.5 rounded hover:bg-gray-100"
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
        </div>

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
                  <LucideSearch class="absolute left-3 w-4 h-4 text-gray-400" />
                  <input
                      ref="searchInput"
                      v-model="searchQuery"
                      type="text"
                      placeholder="搜索选项..."
                      class="w-full h-10 pl-10 pr-10 text-sm border border-gray-300 rounded-xl focus:outline-none focus:ring-2 focus:ring-gray-500 focus:border-transparent bg-white"
                      @click.stop
                      @keydown.stop
                      @keydown.esc="() => closeDropdown(close)"
                  />
                  <button
                      v-if="searchQuery"
                      @click="searchQuery = ''"
                      type="button"
                      class="absolute right-3 text-gray-400 hover:text-gray-600 transition-colors"
                  >
                    <span class="text-sm">✕</span>
                  </button>
                </div>
              </div>

              <!-- 选项列表 -->
              <div
                  class="overflow-auto py-1"
                  :class="computedMaxHeight"
                  :style="{ maxHeight: dynamicMaxHeight }"
              >
                <!-- 空状态 -->
                <div v-if="!filteredOptions.length" class="px-4 py-12 text-center text-gray-500">
                  <div class="flex flex-col items-center gap-2">
                    <div class="w-8 h-8 rounded-full bg-gray-100 flex items-center justify-center">
                      <LucideSearch class="w-4 h-4 text-gray-400" />
                    </div>
                    <div class="text-sm">{{ searchQuery ? '未找到匹配项' : '暂无数据' }}</div>
                    <div v-if="searchQuery" class="text-xs text-gray-400">尝试其他关键词</div>
                  </div>
                </div>

                <!-- 选项组 -->
                <template v-else>
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
                          class="flex items-center justify-between px-3 py-2.5 mx-1 rounded-lg cursor-pointer select-none transition-all duration-150 text-left group select-no-title"
                          :class="{
                          'bg-gray-50 text-gray-900': active && selected,
                          'bg-gray-100 text-gray-800': !active && selected,
                          'bg-gray-50': active && !selected,
                          'text-gray-800': !selected,
                          'hover:bg-gray-50': !active && !selected
                        }"
                          @mouseenter="(e) => showOptionTooltip(e, item.label)"
                          @mouseleave="hideTooltip"
                          @mousemove="updateTooltipPosition"
                      >
                        <div class="flex items-center min-w-0 flex-1">
                          <span v-if="props.multiple" class="mr-3 flex items-center justify-center w-5 h-5 flex-shrink-0">
                            <CheckCircle class="w-5 h-5 text-gray-600" />
                          </span>

                          <div class="min-w-0 flex-1">
                            <span
                                class="block truncate text-sm"
                                :class="{ 'font-medium': selected, 'font-normal': !selected }"
                                v-html="highlightMatch(item.label, searchQuery)"
                            ></span>
                            <span v-if="item.description" class="block truncate text-xs text-gray-500 mt-0.5">
                              {{ item.description }}
                            </span>
                          </div>
                        </div>

                        <div v-if="!props.multiple && selected" class="ml-2 flex-shrink-0">
                          <CheckCircle class="w-4 h-4 text-gray-600" />
                        </div>
                      </div>
                    </ListboxOption>

                    <div class="h-px bg-gray-200 mx-3 my-2"></div>
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
                        class="flex items-center justify-between px-3 py-2.5 mx-1 rounded-lg cursor-pointer select-none transition-all duration-150 text-left group select-no-title"
                        :class="{
                        'bg-gray-50 text-gray-900': active && selected,
                        'bg-gray-100 text-gray-800': !active && selected,
                        'bg-gray-50': active && !selected,
                        'text-gray-800': !selected,
                        'hover:bg-gray-50': !active && !selected
                      }"
                        @mouseenter="(e) => showOptionTooltip(e, item.label)"
                        @mouseleave="hideTooltip"
                        @mousemove="updateTooltipPosition"
                    >
                      <div class="flex items-center min-w-0 flex-1">
                        <span v-if="props.multiple" class="mr-3 flex items-center justify-center w-5 h-5 flex-shrink-0">
                          <component
                              :is="selected ? CheckCircle : Circle"
                              class="w-5 h-5 transition-colors"
                              :class="selected ? 'text-gray-600' : 'text-gray-400 group-hover:text-gray-500'"
                          />
                        </span>

                        <div class="min-w-0 flex-1">
                          <span
                              class="block truncate text-sm"
                              :class="{ 'font-medium': selected, 'font-normal': !selected }"
                              v-html="highlightMatch(item.label, searchQuery)"
                          ></span>
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
                </template>
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
            </ListboxOptions>
          </Transition>
        </Teleport>
      </div>
    </Listbox>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch, onMounted, onUnmounted, reactive } from 'vue'
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
const titleWrapperRef = ref<HTMLElement>()
const searchInput = ref<HTMLInputElement | null>(null)
const showError = ref(false)
const searchQuery = ref('')
const isDropdownOpen = ref(false)
const closeFunction = ref<(() => void) | null>(null)

// 统一管理tooltip
const tooltip = reactive({
  visible: false,
  content: '',
  position: { x: 0, y: 0 },
  showTimer: null as number | null,
  hideTimer: null as number | null
})

// 高亮匹配文本
const highlightMatch = (text: string, query?: string): string => {
  if (!query?.trim()) return text
  const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  return text.replace(regex, '<mark class="bg-yellow-200 text-black">$1</mark>')
}

// 计算属性
const dynamicMaxHeight = computed(() => {
  const searchHeight = props.searchable ? 60 : 0
  const footerHeight = 40
  const maxHeight = Math.min(
      window.innerHeight * 0.6,
      parseInt(props.maxDisplayHeight)
  )
  return `${maxHeight - searchHeight - footerHeight}px`
})

const computedMaxHeight = computed(() => 'max-h-[300px] sm:max-h-[400px] lg:max-h-[500px]')

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

// 事件处理
const handleUpdateModelValue = (val: SelectValue) => {
  emit('update:modelValue', val)
  showError.value = false
}

const handleButtonClick = (open: boolean, close: () => void) => {
  isDropdownOpen.value = open
  closeFunction.value = close

  if (open) {
    emit('dropdown-open')
    hideTooltip()
    if (props.searchable) {
      nextTick(() => {
        setTimeout(() => {
          searchInput.value?.focus()
        }, 100)
      })
    }
    nextTick(() => {
      if (update) update()
    })
  } else {
    emit('dropdown-close')
    closeFunction.value = null
    hideTooltip()
  }
}

const closeDropdown = (close?: () => void) => {
  const closeFunc = close || closeFunction.value
  if (closeFunc) closeFunc()

  isDropdownOpen.value = false
  emit('dropdown-close')
  closeFunction.value = null
  hideTooltip()
}

// 数据处理
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

  const modelValueArray = Array.isArray(props.modelValue) ? props.modelValue : []
  return safeOptions.value.filter(option => modelValueArray.includes(option.value))
})

const displayOptions = computed(() => {
  if (!props.multiple || searchQuery.value) {
    return filteredOptions.value
  }

  const selectedValues = Array.isArray(props.modelValue) ? props.modelValue : []
  return filteredOptions.value.filter(option => !selectedValues.includes(option.value))
})

const isArrayValue = (val: unknown): val is (string | number)[] => {
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

const MAX_DISPLAY_ITEMS = 3

const selectedText = computed(() => {
  if (!selectedLabels.value.length) {
    return props.placeholder || `请选择${props.title || ''}`
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

// Tooltip 方法
const showTooltip = (event: MouseEvent, content: string) => {
  // 清除任何现有的定时器
  if (tooltip.showTimer) {
    clearTimeout(tooltip.showTimer)
    tooltip.showTimer = null
  }

  if (tooltip.hideTimer) {
    clearTimeout(tooltip.hideTimer)
    tooltip.hideTimer = null
  }

  // 防抖显示
  tooltip.showTimer = window.setTimeout(() => {
    tooltip.content = content
    tooltip.visible = true
    updateTooltipPosition(event)
  }, 150)
}

const hideTooltip = () => {
  // 清除显示定时器
  if (tooltip.showTimer) {
    clearTimeout(tooltip.showTimer)
    tooltip.showTimer = null
  }

  // 延迟隐藏
  if (tooltip.hideTimer) {
    clearTimeout(tooltip.hideTimer)
  }

  tooltip.hideTimer = window.setTimeout(() => {
    tooltip.visible = false
    tooltip.content = ''
    tooltip.hideTimer = null
  }, 100)
}

const updateTooltipPosition = (event: MouseEvent) => {
  if (!tooltip.visible && !tooltip.showTimer) return

  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  const tooltipWidth = 400
  const tooltipHeight = 50

  let x = event.clientX + 12
  let y = event.clientY + 20

  // 边界检查
  if (x + tooltipWidth > viewportWidth) {
    x = Math.max(0, viewportWidth - tooltipWidth - 10)
  }

  if (y + tooltipHeight > viewportHeight) {
    y = event.clientY - tooltipHeight - 10
  }

  tooltip.position.x = x
  tooltip.position.y = y
}

const showInputTooltip = (event: MouseEvent) => {
  const tooltipText = showError.value
      ? (props.requiredMessage || '此项为必填')
      : selectedText.value

  showTooltip(event, tooltipText)
}

const showOptionTooltip = (event: MouseEvent, content: string) => {
  showTooltip(event, content)
}

// 其他方法
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

const resetSearch = () => {
  searchQuery.value = ''
}

// 监听器
watch(isDropdownOpen, (newVal) => {
  if (!newVal) {
    resetSearch()
    hideTooltip()
  }
})

const handleScroll = () => {
  if (isDropdownOpen.value && update) {
    update()
  }
  hideTooltip()
}

const handleResize = () => {
  if (isDropdownOpen.value && update) {
    update()
  }
}

// 清除原生title
const clearAllTitles = () => {
  try {
    if (containerRef.value) {
      const allElements = containerRef.value.querySelectorAll('*')
      allElements.forEach(el => {
        if (el instanceof HTMLElement) {
          el.removeAttribute('title')
          // 阻止新的title被设置
          const originalSetAttribute = el.setAttribute
          el.setAttribute = function(name: string, value: string) {
            if (name === 'title') return
            return originalSetAttribute.call(this, name, value)
          }
        }
      })

      if (containerRef.value instanceof HTMLElement) {
        containerRef.value.removeAttribute('title')
      }
    }
  } catch (error) {
    // 静默处理错误
  }
}

// 清理定时器
const clearTooltipTimers = () => {
  if (tooltip.showTimer) {
    clearTimeout(tooltip.showTimer)
    tooltip.showTimer = null
  }

  if (tooltip.hideTimer) {
    clearTimeout(tooltip.hideTimer)
    tooltip.hideTimer = null
  }
}

// 生命周期
onMounted(() => {
  document.addEventListener('scroll', handleScroll, true)
  window.addEventListener('resize', handleResize)

  clearAllTitles()
  const titleClearInterval = setInterval(clearAllTitles, 100)

  // 使用MutationObserver监控title属性变化
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'attributes' && mutation.attributeName === 'title') {
        const target = mutation.target as HTMLElement
        if (target && target.hasAttribute('title')) {
          target.removeAttribute('title')
        }
      }
    })
  })

  if (containerRef.value) {
    observer.observe(containerRef.value, {
      attributes: true,
      attributeFilter: ['title'],
      subtree: true
    })
  }

  onUnmounted(() => {
    clearInterval(titleClearInterval)
    observer.disconnect()
  })
})

onUnmounted(() => {
  document.removeEventListener('scroll', handleScroll, true)
  window.removeEventListener('resize', handleResize)
  clearTooltipTimers()
})

defineExpose({
  validate: () => {
    onBlur()
    return !showError.value
  },
  resetSearch,
  closeDropdown: () => closeDropdown(),
  update,
  containerRef
})
</script>