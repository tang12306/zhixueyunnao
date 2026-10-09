<template>
  <div class="select-from-bank-container">
    <el-card class="filter-card">
      <template #header>
        <div class="card-header">
          <span>从题库选题组卷</span>
        </div>
      </template>
      <QuestionFilterBar @search="handleSearch" />
    </el-card>

    <el-row :gutter="20">
      <!-- 左侧：题目列表与选择 -->
      <el-col :span="16">
        <QuestionPickTable ref="pickTable" :filters="filters" @selection-change="handleSelectionChange" />
      </el-col>

      <!-- 右侧：已选题目与试卷配置 -->
      <el-col :span="8">
        <PaperPanel
          :questions="selectedQuestions"
          :subject-name="subjectName"
          @score="updateScore"
          @remove="removeQuestion"
          @clear="clearSelection"
          @exported="resetSelection"
        />
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { ElMessage } from 'element-plus';
import QuestionFilterBar from './select-from-bank/QuestionFilterBar.vue';
import QuestionPickTable from './select-from-bank/QuestionPickTable.vue';
import PaperPanel from './select-from-bank/PaperPanel.vue';

const DEFAULT_SCORE = 5;

const pickTable = ref(null);
const filters = ref({});
const subjectName = ref('');
// 表格里勾选的行（跨页保留）。分值按题目 id 单独记，勾选别的题时不会把已改的分值冲掉
const selectedRows = ref([]);
const scores = ref(new Map());

const selectedQuestions = computed(() =>
  selectedRows.value.map(row => ({ ...row, score: scores.value.get(row.id) }))
);

const handleSearch = (search) => {
  filters.value = search.filters;
  subjectName.value = search.subjectName;
};

const handleSelectionChange = (rows) => {
  const previous = scores.value;
  scores.value = new Map(rows.map(row => [row.id, previous.get(row.id) ?? (row.score || DEFAULT_SCORE)]));
  selectedRows.value = rows;
};

const updateScore = (id, score) => {
  if (typeof score === 'number' && score > 0) {
    scores.value.set(id, score);
  }
};

// 取消表格里的勾选，表格随后发出 selection-change 更新已选列表
const removeQuestion = (id) => {
  const row = selectedRows.value.find(item => item.id === id);
  if (row && pickTable.value) {
    pickTable.value.deselect(row);
  }
};

const resetSelection = () => {
  pickTable.value?.clearSelection();
  selectedRows.value = [];
  scores.value = new Map();
};

const clearSelection = () => {
  resetSelection();
  ElMessage.success('已清空所选题目');
};
</script>

<style scoped>
.select-from-bank-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}
</style>
