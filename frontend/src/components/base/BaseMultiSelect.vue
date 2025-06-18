<script setup lang="ts">
import { computed } from 'vue'
import {
  Listbox,
  ListboxButton,
  ListboxOptions,
  ListboxOption
} from '@headlessui/vue'
import { LucideChevronDown } from 'lucide-vue-next'

interface Option {
  label: string
  value: string | number
}

const props = defineProps<{
  modelValue: (string | number)[]
  options: Option[]
  placeholder?: string
}>()

const emit = defineEmits(['update:modelValue'])

const modelValue = computed({
  get() {
    return props.modelValue || []
  },
  set(val) {
    emit('update:modelValue', val)
  }
})

const selectedLabels = computed(() =>
    props.options
        .filter(opt => modelValue.value.includes(opt.value))
        .map(opt => opt.label)
)

const selectedText = computed(() =>
    selectedLabels.value.length ? selectedLabels.value.join('、') : (props.placeholder || '请选择')
)
</script>

<template>
  <div class="relative w-[300px]">
    <Listbox v-model="modelValue" multiple>
      <ListboxButton
          class="input-base"
      >
        <span
            class="truncate whitespace-nowrap block max-w-full"
            :title="selectedText"
        >
          {{ selectedText }}
        </span>
        <LucideChevronDown class="w-4 h-4 text-gray-400" />
      </ListboxButton>

      <ListboxOptions
          class="absolute z-50 mt-1 max-h-60 w-full overflow-auto rounded-xl bg-white border border-gray-300 py-1 text-sm shadow-lg ring-1 ring-black ring-opacity-5 focus:outline-none"
      >
        <ListboxOption
            v-for="item in props.options"
            :key="item.value"
            :value="item.value"
            class="cursor-pointer select-none px-3 py-2 hover:bg-gray-100"
            v-slot="{ active, selected }"
        >
          <div
              :class="{
              'font-semibold': selected,
              'text-black': selected,
              'text-gray-900': !selected,
              'bg-gray-100': active && !selected,
            }"
          >
            {{ item.label }}
          </div>
        </ListboxOption>
      </ListboxOptions>
    </Listbox>
  </div>
</template>
