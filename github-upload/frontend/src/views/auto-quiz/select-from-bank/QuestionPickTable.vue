<template>
  <el-card>
    <template #header>
      <span>可选题目列表 (共 {{ pagination.total }} 条)</span>
    </template>
    <el-table
      ref="tableRef"
      :data="questions"
      row-key="id"
      style="width: 100%"
      v-loading="loading"
      @selection-change="rows => emit('selection-change', rows)"
    >
      <!-- reserve-selection：翻页、换筛选条件后之前勾选的题目仍然保留 -->
      <el-table-column type="selection" width="55" :reserve-selection="true" />
      <el-table-column prop="content" label="题目内容" show-overflow-tooltip />
      <el-table-column prop="type" label="题型" width="100">
        <template #default="scope">{{ questionTypeLabel(scope.row.type) }}</template>
      </el-table-column>
      <el-table-column prop="difficulty" label="难度" width="140">
        <template #default="scope">
          <el-rate :model-value="scope.row.difficulty || 0" disabled :max="5" />
        </template>
      </el-table-column>
      <el-table-column prop="subject" label="学科" width="100" />
    </el-table>
    <el-pagination
      class="pagination"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      :total="pagination.total"
      @size-change="search"
      @current-change="fetchQuestions"
    />
  </el-card>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api, { errorMessage } from '@/api';
import { questionTypeLabel } from '@/utils/questionTypes';

const props = defineProps({
  // 筛选条件；父组件每次查询都换一个新对象，这里随之回到第一页重新查
  filters: { type: Object, required: true }
});
const emit = defineEmits(['selection-change']);

const tableRef = ref(null);
const questions = ref([]);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 10, total: 0 });
// 连续查询时只用最后一次的结果
let latestRequest = 0;

const buildParams = () => {
  const { subjectId, chapterId, type, difficulty, keyword } = props.filters;
  const params = {
    subjectId,
    chapterId,
    type,
    difficulty: difficulty || null,
    keyword: keyword ? keyword.trim() : null,
    page: pagination.page - 1, // Spring 分页从 0 开始
    size: pagination.size,
    sort: 'id,desc' // 最新的题目在前
  };
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== null && value !== undefined && value !== '')
  );
};

const fetchQuestions = async () => {
  const requestId = ++latestRequest;
  loading.value = true;
  try {
    const page = await api.questionsJ.queryQuestions(buildParams());
    if (requestId !== latestRequest) {
      return;
    }
    questions.value = Array.isArray(page?.content) ? page.content : [];
    pagination.total = page?.totalElements ?? 0;
  } catch (error) {
    if (requestId !== latestRequest) {
      return;
    }
    console.error('查询题目失败:', error);
    ElMessage.error(errorMessage(error, '查询题目失败'));
    questions.value = [];
    pagination.total = 0;
  } finally {
    if (requestId === latestRequest) {
      loading.value = false;
    }
  }
};

const search = () => {
  pagination.page = 1;
  fetchQuestions();
};

watch(() => props.filters, search);
onMounted(fetchQuestions);

defineExpose({
  // 行不在当前页也能取消（Element Plus 按对象引用找已选行）
  deselect: (row) => tableRef.value?.toggleRowSelection(row, false),
  clearSelection: () => tableRef.value?.clearSelection()
});
</script>

<style scoped>
.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
