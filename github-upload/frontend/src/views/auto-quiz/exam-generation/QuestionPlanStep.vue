<template>
  <div>
    <h3>考题规划</h3>
    <el-form :model="examData" label-position="top">
      <el-form-item label="科目" prop="subjectId" required>
        <el-select v-model="examData.subjectId" placeholder="请选择科目" @change="loadChapters">
          <el-option
            v-for="subject in subjectList"
            :key="subject.id"
            :label="subject.name"
            :value="subject.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="题目分布">
        <div class="question-distribution">
          <div v-for="type in QUESTION_TYPES" :key="type.value" class="question-type-item">
            <span>{{ type.label }}</span>
            <div class="question-type-controls">
              <el-input-number
                v-model="examData.questionPlan[type.value].count"
                :min="0"
                :max="50"
                size="small"
              />
              <span>题</span>
              <el-input-number
                v-model="examData.questionPlan[type.value].scorePerQuestion"
                :min="1"
                :max="50"
                size="small"
              />
              <span>分/题</span>
            </div>
          </div>
        </div>
        <div class="summary">
          <p>题目总数: {{ totalQuestions }} 题（一次最多 {{ MAX_TOTAL_QUESTIONS }} 题）</p>
          <p>总分: {{ calculatedTotalScore }} 分</p>
          <p v-if="planError" class="error">{{ planError }}</p>
          <p v-if="calculatedTotalScore !== examData.totalScore" class="warning">
            注意: 当前分配分数 ({{ calculatedTotalScore }}) 与设定总分 ({{ examData.totalScore }}) 不一致
          </p>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { QUESTION_TYPES } from '@/utils/questionTypes';
import { useExamForm, MAX_TOTAL_QUESTIONS } from './useExamForm';

const { examData, subjectList, totalQuestions, calculatedTotalScore, planError, loadChapters } = useExamForm();
</script>

<style scoped>
.question-distribution {
  margin: 15px 0;
}

.question-type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.question-type-controls {
  display: flex;
  align-items: center;
  gap: 5px;
}

.summary {
  margin-top: 15px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.warning {
  color: #E6A23C;
}

.error {
  color: #F56C6C;
}

.el-select {
  width: 100%;
  min-width: 200px;
}
</style>
