import { ref, computed, onBeforeUnmount } from 'vue';
import { javaAiAPI } from '@/api';

const POLL_INTERVAL = 2000;
// 轮询时网络抖动或服务端 5xx，连续失败这么多次才放弃
const MAX_POLL_FAILURES = 3;

/**
 * 把失败的任务转成和接口报错相同的结构，errorMessage / showAiError 可以直接处理。
 */
function taskError(task) {
  const error = task.error || {};
  const failure = new Error(error.message || 'AI 生成失败，请稍后重试');
  failure.response = {
    status: error.status,
    data: { success: false, message: error.message, data: error.raw ? { raw: error.raw } : null }
  };
  return failure;
}

function readStorage(key) {
  try {
    const saved = JSON.parse(sessionStorage.getItem(key) || 'null');
    return saved && saved.taskId ? saved : null;
  } catch (e) {
    return null;
  }
}

function writeStorage(key, value) {
  try {
    if (value) {
      sessionStorage.setItem(key, JSON.stringify(value));
    } else {
      sessionStorage.removeItem(key);
    }
  } catch (e) {
    // 浏览器禁用存储时只是离开页面后不能恢复，不影响生成
  }
}

/**
 * AI 生成任务：提交后轮询进度，直到成功或失败。
 *
 * 任务 id 记在 sessionStorage（storageKey）里，教师生成途中离开页面再回来，可以用 resume 接着等结果。
 *
 * @param {string} storageKey sessionStorage 的键，每个页面用不同的值
 */
export function useAiTask(storageKey) {
  const running = ref(false);
  const stage = ref('');
  const elapsedSeconds = ref(0);
  let timer = null;
  let stopped = false;

  const progressText = computed(() => {
    if (!running.value) {
      return '';
    }
    return `${stage.value || '处理中'}，已用时 ${elapsedSeconds.value} 秒`;
  });

  function follow(taskId) {
    running.value = true;
    stage.value = '排队中';
    elapsedSeconds.value = 0;
    let failures = 0;

    return new Promise((resolve, reject) => {
      const finish = (callback, value) => {
        running.value = false;
        writeStorage(storageKey, null);
        callback(value);
      };
      const poll = async () => {
        if (stopped) {
          return;
        }
        try {
          const task = await javaAiAPI.getTask(taskId);
          failures = 0;
          stage.value = task.stage;
          elapsedSeconds.value = task.elapsedSeconds;
          if (task.status === 'SUCCEEDED') {
            finish(resolve, task.result);
            return;
          }
          if (task.status === 'FAILED') {
            finish(reject, taskError(task));
            return;
          }
        } catch (error) {
          const status = error.response && error.response.status;
          // 4xx（任务不存在、未登录）重试也没用；网络错误和 5xx 再试几次
          if ((status && status < 500) || ++failures >= MAX_POLL_FAILURES) {
            finish(reject, error);
            return;
          }
        }
        if (!stopped) {
          timer = setTimeout(poll, POLL_INTERVAL);
        }
      };
      poll();
    });
  }

  /**
   * 提交并等待结果。
   *
   * @param {() => Promise<{taskId: string}>} submit 提交请求；参数错误等会直接抛出接口错误
   * @param {object} context 页面需要随结果一起恢复的信息（例如生成时选的学科）
   * @returns {Promise<any>} 任务结果；失败时抛出可交给 showAiError 的错误
   */
  async function run(submit, context = null) {
    running.value = true;
    stage.value = '提交中';
    let accepted;
    try {
      accepted = await submit();
    } catch (error) {
      running.value = false;
      throw error;
    }
    writeStorage(storageKey, { taskId: accepted.taskId, context });
    return follow(accepted.taskId);
  }

  /**
   * 页面打开时调用：上次离开时还有任务没等完，就接着等。
   * 任务已过期（服务重启或超过 30 分钟）时静默丢弃。
   *
   * @param {(result: any, context: object) => void} onDone
   * @param {(error: Error) => void} onError
   * @returns {object|null} 提交时保存的 context；没有要恢复的任务时返回 null
   */
  function resume(onDone, onError) {
    const saved = readStorage(storageKey);
    if (!saved) {
      return null;
    }
    follow(saved.taskId)
      .then(result => onDone(result, saved.context))
      .catch(error => {
        if (!(error.response && error.response.status === 404)) {
          onError(error);
        }
      });
    return saved.context || {};
  }

  // 离开页面只停止轮询，任务在后端继续执行，回来后可以 resume
  onBeforeUnmount(() => {
    stopped = true;
    clearTimeout(timer);
  });

  return { running, stage, elapsedSeconds, progressText, run, resume };
}
