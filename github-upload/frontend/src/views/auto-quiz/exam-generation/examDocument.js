import api from '@/api';
import { QUESTION_TYPES, sectionNumeral } from '@/utils/questionTypes';
import { saveBlob } from '@/utils/download';

/**
 * 按题型分组，题号连续编排，与导出的 Word 一致。
 */
export function groupQuestionsBySection(questions) {
  let number = 0;
  return QUESTION_TYPES
    .map(type => ({ label: type.label, questions: questions.filter(q => q.type === type.value) }))
    .filter(section => section.questions.length > 0)
    .map((section, index) => ({
      title: `${sectionNumeral(index)}、${section.label}`,
      questions: section.questions.map(q => ({ ...q, number: ++number }))
    }));
}

/** 由后端生成试卷 Word 并下载；失败时抛出接口错误 */
export async function downloadExamWord(exam) {
  const response = await api.aiJ.exportExamToWord({
    name: exam.name,
    description: exam.description,
    duration: exam.duration,
    totalScore: exam.totalScore,
    questions: exam.questions.map(q => ({
      type: q.type,
      content: q.content,
      options: q.options,
      answer: q.answer,
      analysis: q.analysis,
      score: q.score
    }))
  });

  saveBlob(response, `${exam.name || 'AI试卷'}.docx`);
}

/** 在浏览器里生成答题卡并下载，返回提示文字 */
export async function downloadAnswerSheet(exam, sections) {
  const { exportAnswerSheet } = await import('@/utils/wordExport');
  return exportAnswerSheet(exam, sections);
}
