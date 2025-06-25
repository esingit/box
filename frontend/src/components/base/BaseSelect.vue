<!-- BaseSelect.vue -->
<template>
  <div class="relative w-full">
    <Listbox
        :model-value="modelValue"
        :multiple="multiple"
        ref="listBoxRef"
        @update:model-value="handleUpdateModelValue"
        @blur="onBlur"
    >
      <div class="relative w-full">
        <ListboxButton
            :title="showError ? (requiredMessage || '此项为必填') : selectedText"
            :class="[
            'input-base flex justify-between items-center w-full pr-8',
            showError ? 'msg-error' : ''
          ]"
        >
          <span
              class="truncate whitespace-nowrap block max-w-full"
              :class="{
              'text-gray-400': !selectedLabels.length && !showError,
              'text-black': selectedLabels.length || showError
            }"
          >
            {{ showError ? (requiredMessage || '此项为必填') : selectedText }}
          </span>

          <div class="absolute right-3 top-1/2 -translate-y-1/2 flex items-center gap-[15px]">
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
            <LucideChevronDown class="w-4 h-4 text-gray-400" />
          </div>
        </ListboxButton>

        <ListboxOptions
            :class="[
            'absolute z-[9999] w-full overflow-auto rounded-2xl bg-white border border-gray-300 p-2 text-sm shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none',
            direction === 'up' ? 'mb-1 bottom-full' : 'mt-1 top-full',
            'min-h-[80px] max-h-60'
          ]"
        >
          <ListboxOption
              v-for="item in safeOptions"
              :key="item.value"
              :value="item.value"
              v-slot="{ active, selected }"
          >
            <div
                class="flex items-center px-3 py-2 rounded-xl cursor-pointer select-none transition-colors duration-150"
                :class="{
                'bg-gray-100': active,
                'font-semibold text-black': selected,
                'text-gray-800 ': !selected
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
              {{ item.label }}
            </div>
          </ListboxOption>
        </ListboxOptions>
      </div>
    </Listbox>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import {
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption
} from '@headlessui/vue'
import { LucideChevronDown, Circle, CheckCircle } from 'lucide-vue-next'

interface Option {
  label: string
  value: string | number
}

const props = withDefaults(defineProps<{
  modelValue?: string | number | (string | number)[] | null
  options?: Option[]
  placeholder?: string
  title: string
  multiple?: boolean
  direction?: 'up' | 'down'
  clearable?: boolean
  required?: boolean
  requiredMessage?: string
}>(), {
  options: () => [],
  placeholder: '',
  multiple: false,
  direction: 'down',
  clearable: false,
  required: false,
  requiredMessage: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number | (string | number)[] | null]
  'blur': []
  'dropdown-open': []
  'dropdown-close': []
}>()

const listBoxRef = ref<InstanceType<typeof Listbox> | null>(null)
const showError = ref(false)

// 处理模型值更新
const handleUpdateModelValue = (val: string | number | (string | number)[] | null) => {
  emit('update:modelValue', val)
  showError.value = false
}

const safeOptions = computed(() => props.options ?? [])

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

const selectedText = computed(() =>
    selectedLabels.value.length
        ? selectedLabels.value.join('、')
        : computedPlaceholder.value
)

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
  emit('blur')
  if (props.required) {
    const empty =
        props.multiple
            ? !Array.isArray(props.modelValue) || props.modelValue.length === 0
            : !props.modelValue || props.modelValue === ''
    if (empty) {
      showError.value = true
      nextTick(() => {
        const btn = document.querySelector('.input-base')
        if (btn instanceof HTMLElement) {
          btn.focus()
        }
      })
    } else {
      showError.value = false
    }
  }
}

// 暴露验证方法
defineExpose({
  validate: () => {
    onBlur()
    return !showError.value
  }
})
</script>