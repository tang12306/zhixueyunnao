<template>
  <el-card>
    <template #header>
      <span>试卷配置 (已选 {{ questions.length }} 题)</span>
    </template>
    <el-form ref="formRef" :model="paperForm" :rules="rules" label-position="top">
      <el-form-item label="试卷标题" prop="title">
        <el-input v-model="paperForm.title" placeholder="请输入试卷标题" />
      </el-form-item>
      <el-form-item label="试卷描述" prop="description">
        <el-input v-model="paperForm.description" type="textarea" placeholder="请输入试卷描述 (可选)" />
      </el-form-item>
      <el-form-item label="考试时长 (分钟)" prop="durationMinutes">
        <el-input-number v-model="paperForm.durationMinutes" :min="30" :step="10" />
      </el-form-item>
    </el-form>

    <div v-if="questions.length > 0" class="selected-questions-detail">
      <h4>已选题目详情:</h4>
      <div class="question-list">
        <div v-for="(item, index) in questions" :key="item.id" class="question-item">
          <div class="question-header">
            <span class="question-number">{{ index + 1 }}.</span>
            <span class="question-type">{{ questionTypeLabel(item.type) }}</span>
            <el-input-number
              :model-value="item.score"
              :min="1"
              :max="100"
              value-on-clear="min"
              size="small"
              class="score-input"
              @update:model-value="score => emit('score', item.id, score)"
            />
            <span class="score-label">分</span>
          </div>
          <div class="question-content">
            <el-text class="question-text" truncated>{{ item.content }}</el-text>
          </div>
          <div class="question-actions">
            <el-button size="small" link type="primary" @click="previewQuestion(item)">预览</el-button>
            <el-button size="small" link type="danger" @click="emit('remove', item.id)">移除</el-button>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else description="暂未选择题目" :image-size="80" />

    <div class="score-summary">
      <p><strong>总题数: {{ questions.length }} 题</strong></p>
      <p><strong>总分: {{ totalScore }} 分</strong></p>
    </div>

    <el-button type="primary" :loading="exporting" :disabled="questions.length === 0" @click="exportPaper">
      <el-icon><Download /></el-icon>
      导出Word试卷
    </el-button>
    <el-button :disabled="questions.length === 0" @click="emit('clear')">清空已选</el-button>
  </el-card>
</template>

<script setup>
import { ref, reactive, computed } from 'vue';
import { ElMessage } from 'element-plus';
import { Download } from '@element-plus/icons-vue';
import api, { errorMessage } from '@/api';
import { questionTypeLabel } from '@/utils/questionTypes';
import { saveBlob } from '@/utils/download';
import { previewQuestion } from './questionPreview';

const props = defineProps({
  // 已选题目，每题带 score
  questions: { type: Array, required: true },
  // 当前筛选的学科名，写进试卷；没选学科时为空
  subjectName: { type: String, default: '' }
});
const emit = defineEmits(['score', 'remove', 'clear', 'exported']);

const formRef = ref(null);
const exporting = ref(false);
const paperForm = reactive({
  title: '',
  description: '',
  durationMinutes: 120
});
const rules = {
  title: [{ required: true, whitespace: true, message: '请输入试卷标题', trigger: 'blur' }]
};

const totalScore = computed(() => props.questions.reduce((sum, item) => sum + (item.score || 0), 0));

const exportPaper = async () => {
  if (props.questions.length === 0) {
    ElMessage.warning('请至少选择一道题目');
    return;
  }
  if (!(await formRef.value.validate().catch(() => false))) {
    return;
  }

  exporting.value = true;
  try {
    const title = paperForm.title.trim();
    const data = await api.papersJ.exportToWord({
      title,
      description: paperForm.description,
      duration: paperForm.durationMinutes,
      questions: props.questions.map(item => ({ questionId: item.id, score: item.score })),
      totalScore: totalScore.value,
      subject: props.subjectName || '综合'
    });
    saveBlob(data, `${title}.docx`);
    ElMessage.success('试卷导出成功！');
    formRef.value.resetFields();
    emit('exported');
  } catch (error) {
    console.error('导出试卷失败:', error);
    ElMessage.error(errorMessage(error, '导出试卷失败'));
  } finally {
    exporting.value = false;
  }
};
</script>

<style scoped>
.selected-questions-detail {
  margin: 15px 0;
}

.question-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.question-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.question-item:last-child {
  border-bottom: none;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.question-number {
  font-weight: bold;
  margin-right: 8px;
  min-width: 30px;
}

.question-type {
  background: #e1f3d8;
  color: #67c23a;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-right: 10px;
}

.score-input {
  margin-left: auto;
  margin-right: 5px;
  width: 80px;
}

.score-label {
  font-size: 14px;
  color: #606266;
}

.question-content {
  margin: 8px 0;
}

.question-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.question-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.score-summary {
  margin: 15px 0;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.score-summary p {
  margin: 5px 0;
  color: #303133;
}
</style>

<style>
/* 预览弹窗挂在 body 下，scoped 样式作用不到，所以单独写成全局样式 */
.question-preview-dialog {
  width: 700px;
  max-width: 90vw;
}

.question-preview-dialog .el-message-box__content {
  max-height: 600px;
  overflow-y: auto;
  text-align: left;
}

.question-preview-dialog .el-message-box__btns {
  padding-top: 15px;
  justify-content: center;
}
</style>
