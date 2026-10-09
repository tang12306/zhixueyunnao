import { isChoiceType, optionLetter } from '@/utils/questionTypes';

// 后端按 A～H 解析选择题答案
export const MAX_OPTIONS = 8;
const DEFAULT_OPTION_COUNT = 4;
export const TRUE_FALSE_ANSWERS = ['正确', '错误'];

export const blankOption = () => ({ text: '', isCorrect: false });
const blankOptions = () => Array.from({ length: DEFAULT_OPTION_COUNT }, blankOption);

export function emptyQuestionForm() {
  return {
    subjectId: null,
    chapterId: null,
    type: '',
    difficulty: 3,
    content: '',
    options: [],
    answer: '',
    analysis: '',
    tags: []
  };
}

/** 选择题答案 "A,C" → 对应选项标为正确；答案不是纯字母时都不勾选 */
function optionsFromQuestion(options, answer) {
  if (!Array.isArray(options) || options.length === 0) {
    return blankOptions();
  }
  const letters = String(answer || '').replace(/[\s,，、]/g, '').toUpperCase();
  const correct = /^[A-H]+$/.test(letters) ? new Set(letters) : new Set();
  return options.map((text, index) => ({ text, isCorrect: correct.has(optionLetter(index)) }));
}

/** 后端返回的题目 → 表单。题目里学科只存了名称，用科目列表对应回 ID */
export function formFromQuestion(question, subjects) {
  const subjectId = question.chapter?.subject?.id
    ?? subjects.find(subject => subject.name === question.subject)?.id
    ?? null;
  const choice = isChoiceType(question.type);
  return {
    subjectId,
    chapterId: question.chapter?.id ?? null,
    type: question.type || '',
    difficulty: question.difficulty || 3,
    content: question.content || '',
    options: choice ? optionsFromQuestion(question.options, question.answer) : [],
    answer: choice ? '' : (question.answer || ''),
    analysis: question.analysis || '',
    tags: Array.isArray(question.tags) ? [...question.tags] : []
  };
}

/** 表单 → 保存接口的请求。选择题答案由勾选的选项生成，如 "A,C" */
export function toQuestionPayload(form) {
  const choice = isChoiceType(form.type);
  return {
    subjectId: form.subjectId,
    chapterId: form.chapterId || null,
    type: form.type,
    difficulty: form.difficulty,
    content: form.content.trim(),
    options: choice ? form.options.map(option => option.text.trim()) : [],
    answer: choice
      ? form.options
        .map((option, index) => (option.isCorrect ? optionLetter(index) : null))
        .filter(Boolean)
        .join(',')
      : form.answer.trim(),
    analysis: form.analysis ? form.analysis.trim() : '',
    tags: [...form.tags]
  };
}

/** 检查选择题选项，有问题时返回提示，没问题返回空字符串 */
export function optionsError(type, options) {
  if (!isChoiceType(type)) {
    return '';
  }
  if (options.length < 2) {
    return '至少需要两个选项';
  }
  if (options.some(option => !option.text.trim())) {
    return '选项内容不能为空';
  }
  const correctCount = options.filter(option => option.isCorrect).length;
  if (correctCount === 0) {
    return '请勾选正确答案';
  }
  if (type === 'SINGLE_CHOICE' && correctCount > 1) {
    return '单选题只能有一个正确答案';
  }
  return '';
}

/** 换题型时要改的字段：选择题补上空选项，判断题给默认答案，其它题型去掉选项 */
export function adaptToType(form, type) {
  if (isChoiceType(type)) {
    if (form.options.length === 0) {
      return { options: blankOptions() };
    }
    if (type === 'SINGLE_CHOICE') {
      // 多选改单选：只保留第一个正确选项
      const first = form.options.findIndex(option => option.isCorrect);
      return { options: form.options.map((option, index) => ({ ...option, isCorrect: index === first })) };
    }
    return {};
  }
  const wasTrueFalse = TRUE_FALSE_ANSWERS.includes(form.answer);
  if (type === 'TRUE_FALSE') {
    return { options: [], answer: wasTrueFalse ? form.answer : TRUE_FALSE_ANSWERS[0] };
  }
  return { options: [], answer: wasTrueFalse ? '' : form.answer };
}
