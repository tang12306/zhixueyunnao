import { h } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { errorMessage, aiRawReply } from '@/api';

const RAW_STYLE = 'white-space: pre-wrap; word-break: break-all; max-height: 300px; overflow: auto; '
  + 'background: #f5f7fa; padding: 8px; font-size: 12px; margin-top: 8px;';

/**
 * AI 接口出错时的提示。模型回复无法解析时，后端会带上原始回复，一并展示，方便判断是重试还是调整要求。
 */
export function showAiError(error, fallback = 'AI 生成失败，请稍后重试') {
  const message = errorMessage(error, fallback);
  const raw = aiRawReply(error);
  if (!raw) {
    ElMessage.error(message);
    return;
  }
  ElMessageBox.alert(
    h('div', [h('p', message), h('pre', { style: RAW_STYLE }, raw)]),
    'AI 回复无法解析',
    { type: 'error' }
  );
}
