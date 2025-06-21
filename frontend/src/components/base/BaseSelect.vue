<template>
  <div class="relative w-full">
    <Listbox
        v-model="modelValue"
        :multiple="multiple"
        ref="listBoxRef"
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

          <!-- 图标区域：清空按钮 + 下拉箭头 -->
          <div class="absolute right-4 top-1/2 -translate-y-1/2 flex items-center gap-[2px]">
            <button
                v-if="clearable && hasValue"
                @click.stop="clearSelection"
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
            'absolute z-50 w-full overflow-auto rounded-2xl bg-white border border-gray-300 p-2 text-sm shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none',
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
  modelValue?: string | number | (string | number)[]
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

const emit = defineEmits(['update:modelValue'])

const listBoxRef = ref<InstanceType<typeof Listbox> | null>(null)

const safeModelValue = computed(() => {
  if (props.modelValue === undefined || props.modelValue === null) {
    return props.multiple ? [] : ''
  }
  return props.modelValue
})

const modelValue = computed({
  get: () => safeModelValue.value,
  set: val => {
    emit('update:modelValue', val)
    showError.value = false
  }
})

const safeOptions = computed(() => props.options ?? [])

function isArrayValue(val: unknown): val is (string | number)[] {
  return Array.isArray(val)
}

const selectedLabels = computed(() => {
  const options = safeOptions.value
  const value = modelValue.value

  if (props.multiple && isArrayValue(value)) {
    return options.filter(opt => value.includes(opt.value)).map(opt => opt.label)
  } else {
    const selected = options.find(opt => opt.value === value)
    return selected ? [selected.label] : []
  }
})

const computedPlaceholder = computed(() => props.placeholder || `请输入${props.title}`)

const selectedText = computed(() =>
    selectedLabels.value.length
        ? selectedLabels.value.join('、')
        : computedPlaceholder.value
)

const direction = computed(() => props.direction ?? 'down')

const hasValue = computed(() =>
    props.multiple
        ? Array.isArray(modelValue.value) && modelValue.value.length > 0
        : !!modelValue.value
)

const showError = ref(false)

function clearSelection() {
  modelValue.value = props.multiple ? [] : ''
  showError.value = false
}

function onBlur() {
  if (props.required) {
    const empty =
        props.multiple
            ? !Array.isArray(modelValue.value) || modelValue.value.length === 0
            : !modelValue.value || modelValue.value === ''
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
</script>
