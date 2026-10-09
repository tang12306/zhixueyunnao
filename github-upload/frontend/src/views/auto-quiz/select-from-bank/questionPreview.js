import { ElMessageBox } from 'element-plus';
import { questionTypeLabel, optionLetter } from '@/utils/questionTypes';

const escapeHtml = (text) => String(text ?? '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&#039;');

/** 弹窗预览一道已选题目。题目里的文字都先转义再拼进 HTML */
export function previewQuestion(question) {
  const options = Array.isArray(question.options) ? question.options : [];
  const optionList = options.length > 0
    ? `<p><strong>选项:</strong></p>
       <ul style="margin: 10px 0; padding-left: 20px;">
         ${options.map((option, index) => `<li>${optionLetter(index)}. ${escapeHtml(option)}</li>`).join('')}
       </ul>`
    : '';
  const analysis = question.analysis
    ? `<p><strong>解析:</strong></p>
       <div style="margin: 10px 0; padding: 10px; background: #f0f9ff; border-radius: 4px;">
         ${escapeHtml(question.analysis)}
       </div>`
    : '';

  const html = `
    <div style="text-align: left; line-height: 1.6;">
      <p><strong>题型:</strong> ${escapeHtml(questionTypeLabel(question.type))}</p>
      <p><strong>难度:</strong> ${Number(question.difficulty) || 0} 星</p>
      <p><strong>分数:</strong> ${Number(question.score) || 0} 分</p>
      <p><strong>题目内容:</strong></p>
      <div style="margin: 10px 0; padding: 15px; background: #f5f7fa; border-radius: 6px; border-left: 4px solid #409eff;">
        ${escapeHtml(question.content)}
      </div>
      ${optionList}
      <p><strong>答案:</strong> ${escapeHtml(question.answer || '暂无')}</p>
      ${analysis}
    </div>
  `;

  ElMessageBox.alert(html, '题目预览', {
    dangerouslyUseHTMLString: true,
    customClass: 'question-preview-dialog',
    confirmButtonText: '关闭',
    closeOnClickModal: true,
    closeOnPressEscape: true
  }).catch(() => {
    // 点遮罩或按 Esc 关闭时 alert 会 reject，不算错误
  });
}
