<template>
  <div class="exam-preview">
    <el-alert
      v-for="(warning, index) in exam.warnings"
      :key="index"
      :title="warning"
      type="warning"
      show-icon
      :closable="false"
      class="preview-warning"
    />

    <div class="exam-header-preview">
      <h2>{{ exam.name }}</h2>
      <p v-if="exam.description">{{ exam.description }}</p>
      <div class="exam-info-preview">
        <span>科目: {{ exam.subjectName }}</span>
        <span>考试时长: {{ exam.duration }}分钟</span>
        <span>总分: {{ exam.totalScore }}分</span>
      </div>
      <p v-if="exam.totalScore !== exam.targetScore" class="warning">
        题目分值合计 {{ exam.totalScore }} 分，与设定总分 {{ exam.targetScore }} 分不一致
      </p>
    </div>

    <div class="preview-toolbar">
      <el-switch v-model="showAnswers" active-text="显示答案和解析" />
    </div>

    <el-divider content-position="center">预览</el-divider>

    <div v-for="section in sections" :key="section.title" class="question-section">
      <h3>{{ section.title }}（共{{ section.questions.length }}题）</h3>
      <div v-for="question in section.questions" :key="question.number" class="preview-question-item">
        <div class="preview-question-header">
          <span class="preview-question-number">{{ question.number }}</span>
          <span class="preview-question-score">({{ question.score }}分)</span>
        </div>
        <!-- 题目内容来自模型，按纯文本显示 -->
        <div class="preview-question-content">{{ question.content }}</div>

        <div v-if="isChoiceType(question.type)" class="preview-options">
          <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="preview-option">
            {{ optionLetter(optionIndex) }}. {{ option }}
          </div>
        </div>

        <div v-if="showAnswers" class="preview-answer">
          <div>答案：{{ question.answer || '（无）' }}</div>
          <div v-if="question.analysis">解析：{{ question.analysis }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { isChoiceType, optionLetter } from '@/utils/questionTypes';

defineProps({
  // 后端 ExamDraft
  exam: { type: Object, required: true },
  // 按题型分组、题号连续的题目，见 examDocument.groupQuestionsBySection
  sections: { type: Array, required: true }
});

const showAnswers = ref(false);
</script>

<style scoped>
.preview-warning {
  margin-bottom: 10px;
}

.exam-header-preview {
  text-align: center;
  margin-bottom: 20px;
}

.exam-info-preview {
  display: flex;
  justify-content: center;
  gap: 20px;
  color: #606266;
}

.warning {
  color: #E6A23C;
}

.preview-toolbar {
  display: flex;
  justify-content: flex-end;
}

.question-section {
  margin-bottom: 30px;
}

.preview-question-item {
  margin-bottom: 20px;
  padding: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.preview-question-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.preview-question-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 4px;
  background-color: #409eff;
  color: white;
  border-radius: 12px;
  margin-right: 10px;
}

.preview-question-score {
  color: #f56c6c;
}

.preview-question-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-options {
  margin-top: 10px;
  padding-left: 20px;
}

.preview-option {
  margin-bottom: 5px;
  white-space: pre-wrap;
}

.preview-answer {
  margin-top: 10px;
  padding: 8px 12px;
  background-color: #f0f9eb;
  border-radius: 4px;
  color: #529b2e;
  white-space: pre-wrap;
}
</style>
