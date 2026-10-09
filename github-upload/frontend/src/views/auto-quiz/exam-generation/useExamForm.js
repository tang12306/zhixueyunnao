import { reactive, ref, computed, provide, inject } from 'vue';
import { ElMessage } from 'element-plus';
import api, { errorMessage } from '@/api';
import { QUESTION_TYPES } from '@/utils/questionTypes';

// 与后端 ExamGenerationRequest.MAX_TOTAL_QUESTIONS 一致
export const MAX_TOTAL_QUESTIONS = 60;

// 默认题量合计 100 分
const DEFAULT_PLAN = {
  SINGLE_CHOICE: { count: 10, scorePerQuestion: 2 },
  MULTIPLE_CHOICE: { count: 5, scorePerQuestion: 3 },
  TRUE_FALSE: { count: 5, scorePerQuestion: 2 },
  FILL_IN_THE_BLANK: { count: 5, scorePerQuestion: 3 },
  SHORT_ANSWER: { count: 4, scorePerQuestion: 10 }
};

const EXAM_FORM_KEY = Symbol('examForm');

/**
 * 一键出卷的表单状态。由页面创建并 provide，各步骤组件用 useExamForm() 取得同一份数据。
 */
export function provideExamForm() {
  const examData = reactive({
    name: '',
    description: '',
    duration: 90,
    totalScore: 100,
    subjectId: null,
    allChapters: false,
    chapterIds: [],
    chapterDistribution: '',
    examRequirements: '',
    // 按题型顺序，键是后端枚举名
    questionPlan: Object.fromEntries(QUESTION_TYPES.map(t => [t.value, { ...DEFAULT_PLAN[t.value] }]))
  });
  const subjectList = ref([]);
  const chapterList = ref([]);

  const totalQuestions = computed(() =>
    Object.values(examData.questionPlan).reduce((sum, plan) => sum + plan.count, 0));

  const calculatedTotalScore = computed(() =>
    Object.values(examData.questionPlan).reduce((sum, plan) => sum + plan.count * plan.scorePerQuestion, 0));

  const planError = computed(() => {
    if (totalQuestions.value === 0) {
      return '请至少设置一道题';
    }
    if (totalQuestions.value > MAX_TOTAL_QUESTIONS) {
      return `一次最多生成 ${MAX_TOTAL_QUESTIONS} 道题，请减少题量`;
    }
    return '';
  });

  const loadSubjects = async () => {
    try {
      const response = await api.subjectAdminJ.getAll();
      subjectList.value = Array.isArray(response) ? response : [];
    } catch (error) {
      console.error('加载科目失败:', error);
      ElMessage.error(errorMessage(error, '加载科目失败，请刷新页面重试'));
    }
  };

  const fetchChapters = async () => {
    chapterList.value = [];
    if (!examData.subjectId) {
      return;
    }
    try {
      const response = await api.chaptersJ.getChaptersBySubject(examData.subjectId);
      chapterList.value = Array.isArray(response) ? response : [];
    } catch (error) {
      console.error('加载章节失败:', error);
      ElMessage.error(errorMessage(error, '加载章节失败，请重试'));
    }
  };

  // 换了科目，章节选择清空
  const loadChapters = () => {
    examData.chapterIds = [];
    examData.allChapters = false;
    return fetchChapters();
  };

  const toggleAllChapters = () => {
    examData.chapterIds = examData.allChapters ? chapterList.value.map(chapter => chapter.id) : [];
  };

  /** 提交给 /api/ai/generate-exam 的请求体 */
  const toRequest = () => ({
    name: examData.name,
    description: examData.description,
    duration: examData.duration,
    targetScore: examData.totalScore,
    subjectId: examData.subjectId,
    chapterIds: [...examData.chapterIds],
    questionPlan: JSON.parse(JSON.stringify(examData.questionPlan)),
    examRequirements: examData.examRequirements,
    chapterDistribution: examData.chapterDistribution
  });

  /** 恢复离开页面前提交的表单，用于接着等待生成结果 */
  const restore = (request) => {
    Object.assign(examData, {
      name: request.name || '',
      description: request.description || '',
      duration: request.duration || 90,
      totalScore: request.targetScore || 100,
      subjectId: request.subjectId || null,
      chapterIds: request.chapterIds || [],
      chapterDistribution: request.chapterDistribution || '',
      examRequirements: request.examRequirements || ''
    });
    for (const [type, plan] of Object.entries(request.questionPlan || {})) {
      if (examData.questionPlan[type]) {
        Object.assign(examData.questionPlan[type], plan);
      }
    }
    return fetchChapters();
  };

  const form = {
    examData,
    subjectList,
    chapterList,
    totalQuestions,
    calculatedTotalScore,
    planError,
    loadSubjects,
    loadChapters,
    toggleAllChapters,
    toRequest,
    restore
  };
  provide(EXAM_FORM_KEY, form);
  return form;
}

export function useExamForm() {
  return inject(EXAM_FORM_KEY);
}
