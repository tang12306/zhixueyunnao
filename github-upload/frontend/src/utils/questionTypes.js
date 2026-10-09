// 与后端 QuestionType 枚举保持一致：接口里传枚举名，界面上显示中文名
export const QUESTION_TYPES = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'FILL_IN_THE_BLANK', label: '填空题' },
  { value: 'SHORT_ANSWER', label: '简答题' }
];

const LABELS = Object.fromEntries(QUESTION_TYPES.map(t => [t.value, t.label]));

export function questionTypeLabel(type) {
  return LABELS[type] || type || '';
}

export function isChoiceType(type) {
  return type === 'SINGLE_CHOICE' || type === 'MULTIPLE_CHOICE';
}

/** 选项按顺序加字母：0 → A。后端返回的选项不带前缀 */
export function optionLetter(index) {
  return String.fromCharCode(65 + index);
}

const CHINESE_NUMERALS = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];

/** 大题序号：0 → 一 */
export function sectionNumeral(index) {
  return CHINESE_NUMERALS[index] || String(index + 1);
}
