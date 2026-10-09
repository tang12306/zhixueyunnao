<template>
  <el-table :data="questions" style="width: 100%" v-loading="loading" class="animated-table">
    <el-table-column prop="subject" label="科目" width="100">
      <template #default="scope">
        <el-tag type="primary" size="small">{{ scope.row.subject }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="type" label="题型" width="120">
      <template #default="scope">
        <el-tag :type="TYPE_TAGS[scope.row.type] || 'info'" size="small">{{ questionTypeLabel(scope.row.type) }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="content" label="内容">
      <template #default="scope">
        <div class="question-content-truncate">{{ scope.row.content }}</div>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="120">
      <template #default="scope">
        <el-button link size="small" class="btn-animate" @click="router.push(`/question-bank/edit/${scope.row.id}`)">
          <Icon icon="mdi:eye" class="mr-1" />
          查看
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api, { errorMessage } from '@/api';
import { questionTypeLabel } from '@/utils/questionTypes';

// Element Plus 的 tag 没有 default 类型，未知题型用 info
const TYPE_TAGS = {
  SINGLE_CHOICE: 'primary',
  MULTIPLE_CHOICE: 'success',
  TRUE_FALSE: 'warning',
  FILL_IN_THE_BLANK: 'info',
  SHORT_ANSWER: 'danger'
};

const router = useRouter();
const questions = ref([]);
const loading = ref(true);

onMounted(async () => {
  try {
    const page = await api.questionsJ.queryQuestions({ page: 0, size: 5 });
    questions.value = Array.isArray(page?.content) ? page.content : [];
  } catch (error) {
    console.error('获取最近题目失败:', error);
    ElMessage.error(errorMessage(error, '获取最近题目失败'));
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.animated-table {
  border-radius: 8px;
  overflow: hidden;
}

.animated-table :deep(.el-table__row) {
  transition: all 0.3s ease;
}

.animated-table :deep(.el-table__row:hover) {
  background-color: #f8f9ff;
  transform: scale(1.01);
}

.question-content-truncate {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.mr-1 {
  margin-right: 4px;
}
</style>
