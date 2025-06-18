<template>
  <div class="relative w-full">
    <Listbox v-model="modelValue" :multiple="multiple">
      <ListboxButton class="input-base flex justify-between items-center w-full">
        <span
            class="truncate whitespace-nowrap block max-w-full"
            :title="selectedText"
        >
          {{ selectedText }}
        </span>
        <LucideChevronDown class="w-4 h-4 text-gray-400 ml-2" />
      </ListboxButton>

      <ListboxOptions
          class="absolute z-50 mt-1 w-full max-h-60 overflow-auto rounded-2xl bg-white border border-gray-300 p-2 text-sm shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none"
      >
        <ListboxOption
            v-for="item in props.options"
            :key="item.value"
            :value="item.value"
            v-slot="{ active, selected }"
        >
          <div
              class="flex items-center px-3 py-2 rounded-xl cursor-pointer select-none transition-colors duration-150"
              :class="{
              'bg-gray-100': active,
              'font-semibold text-black': selected,
              'text-gray-900': !selected,
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
    </Listbox>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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

const props = defineProps<{
  modelValue: string | number | (string | number)[]
  options: Option[]
  placeholder?: string
  multiple?: boolean
}>()

const emit = defineEmits(['update:modelValue'])

const multiple = props.multiple ?? false

const modelValue = computed({
  get: () => props.modelValue,
  set: val => emit('update:modelValue', val)
})

function isArrayValue(val: unknown): val is (string | number)[] {
  return Array.isArray(val)
}

const selectedLabels = computed(() => {
  if (multiple && isArrayValue(modelValue.value)) {
    return props.options
        .filter(opt => modelValue.value.includes(opt.value))
        .map(opt => opt.label)
  } else {
    const selected = props.options.find(opt => opt.value === modelValue.value)
    return selected ? [selected.label] : []
  }
})

const selectedText = computed(() =>
    selectedLabels.value.length
        ? selectedLabels.value.join('、')
        : props.placeholder || '请选择'
)
</script>
