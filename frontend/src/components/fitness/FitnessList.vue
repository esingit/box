<template>
  <div class="table-wrapper">
    <table class="data-table">
      <thead>
        <tr>
          <th>类型</th>
          <th>数量</th>
          <th>单位</th>
          <th>日期</th>
          <th>备注</th>
          <th class="center">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="records.length === 0">
          <td colspan="6">
            <div class="empty-text">
              <span class="empty-icon">📊</span>
              <p>暂无健身记录</p>
            </div>
          </td>
        </tr>
        <tr v-for="(record, idx) in records" :key="record.id || idx">
          <td>{{ record.typeValue }}</td>
          <td class="count-cell">{{ record.count }}</td>
          <td>{{ record.unitValue }}</td>
          <td>{{ record.finishTime ? record.finishTime.slice(0, 10) : '-' }}</td>
          <td class="remark-cell">
            <span :title="record.remark">{{ record.remark || '-' }}</span>
          </td>
          <td>
            <div class="operations">
              <button @click="$emit('edit', idx)" class="action-btn edit-btn" title="编辑">
                <LucideEdit size="16" />
              </button>
              <button @click="$emit('delete', idx)" class="action-btn delete-btn" title="删除">
                <LucideTrash2 size="16" />
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <div class="pagination-container">
    <PaginationBar
      :current="current"
      :total="total"
      :pageSize="pageSize"
      @page-change="$emit('page-change', $event)"
      @page-size-change="$emit('page-size-change', $event)"
    />
  </div>
</template>

<script setup>
const props = defineProps({
  records: Array,
  current: {
    type: Number,
    default: 1
  },
  total: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  }
})
import { LucideEdit, LucideTrash2 } from 'lucide-vue-next'
import { computed } from 'vue'
import PaginationBar from '@/components/PaginationBar.vue'
const totalPages = computed(() => Math.ceil(props.total / props.pageSize))
const showPagination = computed(() => props.total > props.pageSize)
</script>

<style scoped>
.count-cell {
  font-weight: 500;
  color: var(--primary-color);
}

.remark-cell {
  max-width: 200px;
}

.remark-cell span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.center {
  text-align: center;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .remark-cell {
    max-width: 120px;
  }
}
</style>
