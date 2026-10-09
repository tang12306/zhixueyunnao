<template>
  <div class="ai-exam-generation">
    <h1 class="page-title">AI一键出卷</h1>

    <el-card class="generation-card">
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="基本信息" />
        <el-step title="考题规划" />
        <el-step title="课程规划" />
        <el-step title="生成预览" />
      </el-steps>

      <div class="step-content">
        <BasicInfoStep v-if="activeStep === 0" ref="basicInfoStep" />
        <QuestionPlanStep v-else-if="activeStep === 1" />
        <ChapterPlanStep v-else-if="activeStep === 2" />

        <!-- 步骤4：生成预览 -->
        <div v-else-if="activeStep === 3">
          <h3>试卷生成预览</h3>

          <el-card v-if="generating" class="preview-placeholder" shadow="never">
            <div class="start-generation">
              <el-icon class="is-loading" :size="40"><Loading /></el-icon>
              <p>AI 正在生成试卷：{{ progressText }}</p>
              <p class="tip">题目较多时需要 1～2 分钟，可以先离开此页面，回来后会接着显示结果</p>
            </div>
          </el-card>

          <el-card v-else-if="!examPreview.questions.length" class="preview-placeholder" shadow="never">
            <div class="start-generation">
              <el-button type="primary" @click="generateExam">开始生成试卷</el-button>
              <p class="tip">共 {{ totalQuestions }} 道题，可能需要等待 1～2 分钟</p>
            </div>
          </el-card>

          <template v-else>
            <ExamPreview :exam="examPreview" :sections="questionSections" />

            <div class="action-buttons">
              <el-button @click="regenerateExam">重新生成</el-button>
              <el-button type="success" :loading="saving" :disabled="savedToBank" @click="saveQuestionsToBank">
                {{ savedToBank ? '已保存到题库' : '保存题目到题库' }}
              </el-button>
              <el-dropdown @command="handleExportCommand">
                <el-button type="primary">导出文档</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="word">导出试卷(Word)</el-dropdown-item>
                    <el-dropdown-item command="answerSheet">导出答题卡(Word)</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </div>
      </div>

      <div class="step-actions">
        <el-button v-if="activeStep > 0" :disabled="generating" @click="prevStep">上一步</el-button>
        <el-button v-if="activeStep < 3" type="primary" @click="nextStep">下一步</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';
import api, { errorMessage } from '@/api';
import { showAiError } from '@/utils/aiErrors';
import { useAiTask } from '@/composables/useAiTask';
import { provideExamForm } from './exam-generation/useExamForm';
import { groupQuestionsBySection, downloadExamWord, downloadAnswerSheet } from './exam-generation/examDocument';
import BasicInfoStep from './exam-generation/BasicInfoStep.vue';
import QuestionPlanStep from './exam-generation/QuestionPlanStep.vue';
import ChapterPlanStep from './exam-generation/ChapterPlanStep.vue';
import ExamPreview from './exam-generation/ExamPreview.vue';

const { examData, totalQuestions, planError, loadSubjects, toRequest, restore } = provideExamForm();
const { running: generating, progressText, run: runAiTask, resume: resumeAiTask } =
  useAiTask('ai-exam-generation-task');

const activeStep = ref(0);
const basicInfoStep = ref(null);

// 试卷预览数据，字段与后端 ExamDraft 一致
const examPreview = reactive({
  name: '',
  description: '',
  duration: 0,
  targetScore: 0,
  totalScore: 0,
  subjectId: null,
  subjectName: '',
  questions: [],
  warnings: []
});

const saving = ref(false);
const savedToBank = ref(false);
// 生成时只选了一个章节，保存题目时归到该章节；多选或不选只记学科
const generatedChapterId = ref(null);

const questionSections = computed(() => groupQuestionsBySection(examPreview.questions));

const nextStep = async () => {
  if (activeStep.value === 0) {
    if (!basicInfoStep.value || !(await basicInfoStep.value.validate())) return;
  } else if (activeStep.value === 1) {
    if (!examData.subjectId) {
      ElMessage.warning('请选择科目');
      return;
    }
    if (planError.value) {
      ElMessage.warning(planError.value);
      return;
    }
  }
  activeStep.value++;
};

const prevStep = () => {
  activeStep.value--;
};

const showExam = (exam, context) => {
  Object.assign(examPreview, exam);
  generatedChapterId.value = context.chapterId ?? null;
  savedToBank.value = false;
  ElMessage.success(`试卷生成成功，共 ${examPreview.questions.length} 道题`);
};

const generateExam = async () => {
  const request = toRequest();
  const context = { request, chapterId: request.chapterIds.length === 1 ? request.chapterIds[0] : null };
  try {
    // 参数错误时提交就返回 4xx；AI 生成失败时任务以失败结束，都由 catch 处理
    const exam = await runAiTask(() => api.aiJ.generateExam(request), context);
    showExam(exam, context);
  } catch (error) {
    console.error('试卷生成失败:', error);
    showAiError(error, '试卷生成失败，请稍后重试');
  }
};

const regenerateExam = async () => {
  try {
    await ElMessageBox.confirm('重新生成会替换当前预览的试卷，确定吗？', '重新生成', { type: 'warning' });
  } catch (e) {
    return; // 取消
  }
  await generateExam();
};

const saveQuestionsToBank = async () => {
  if (!examPreview.questions.length) {
    ElMessage.warning('没有可保存的题目');
    return;
  }

  saving.value = true;
  try {
    const questionsData = examPreview.questions.map(question => ({
      subjectId: examPreview.subjectId,
      chapterId: generatedChapterId.value,
      type: question.type,
      content: question.content,
      options: question.options || [],
      answer: question.answer,
      analysis: question.analysis || '',
      score: question.score
    }));

    const response = await api.aiJ.saveGeneratedQuestions(questionsData);
    savedToBank.value = true;
    ElMessage.success(response?.message || `成功保存 ${questionsData.length} 道题目到题库`);
  } catch (error) {
    console.error('保存题目失败:', error);
    ElMessage.error(errorMessage(error, '保存题目失败'));
  } finally {
    saving.value = false;
  }
};

const handleExportCommand = async (command) => {
  if (!examPreview.questions.length) {
    ElMessage.warning('请先生成试卷内容');
    return;
  }

  if (command === 'word') {
    try {
      await downloadExamWord(examPreview);
      ElMessage.success('试卷导出成功！');
    } catch (error) {
      console.error('导出试卷失败:', error);
      ElMessage.error(errorMessage(error, '导出试卷失败'));
    }
  } else if (command === 'answerSheet') {
    try {
      ElMessage.success(await downloadAnswerSheet(examPreview, questionSections.value));
    } catch (error) {
      console.error('导出答题卡失败:', error);
      ElMessage.error('导出答题卡失败');
    }
  }
};

onMounted(() => {
  loadSubjects();
  // 上次生成途中离开了页面：恢复表单，直接回到预览步骤等结果
  const pending = resumeAiTask(showExam, error => showAiError(error, '试卷生成失败，请稍后重试'));
  if (pending) {
    if (pending.request) {
      restore(pending.request);
    }
    activeStep.value = 3;
  }
});
</script>

<style scoped>
.ai-exam-generation {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
}

.generation-card {
  margin-bottom: 20px;
}

.step-content {
  margin: 30px 0;
  min-height: 300px;
}

.step-actions {
  display: flex;
  justify-content: flex-end;
}

.preview-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  text-align: center;
}

.start-generation {
  text-align: center;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.tip {
  margin-top: 10px;
  color: #909399;
  font-size: 14px;
}
</style>
