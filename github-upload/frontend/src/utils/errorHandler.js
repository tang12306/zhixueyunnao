/**
 * 全局错误处理工具
 * 主要用于处理开发环境中的常见错误，如ResizeObserver错误
 */

/**
 * ResizeObserver错误处理器
 * 这个错误通常在Element Plus的表格、对话框等组件中出现
 * 不影响功能，但会在开发环境中显示错误提示
 */
export const handleResizeObserverError = () => {
  // 保存原始的console.error方法
  const originalConsoleError = console.error;
  
  // 重写console.error方法
  console.error = (...args) => {
    // 检查是否是ResizeObserver错误
    if (args[0] && typeof args[0] === 'string' && 
        args[0].includes('ResizeObserver loop completed with undelivered notifications')) {
      // 忽略这个错误，不输出到控制台
      return;
    }
    
    // 其他错误正常输出
    originalConsoleError.apply(console, args);
  };
  
  // 添加全局错误监听器
  window.addEventListener('error', (event) => {
    if (event.message && event.message.includes('ResizeObserver loop completed with undelivered notifications')) {
      event.preventDefault();
      return false;
    }
  });
  
  // 添加未处理的Promise错误监听器
  window.addEventListener('unhandledrejection', (event) => {
    if (event.reason && event.reason.message && 
        event.reason.message.includes('ResizeObserver loop completed with undelivered notifications')) {
      event.preventDefault();
      return false;
    }
  });
};

/**
 * Chunk加载错误处理器
 */
export const handleChunkLoadError = () => {
  // 监听全局错误事件
  window.addEventListener('error', (event) => {
    const target = event.target || event.srcElement;
    const isChunkLoadFailed = event.message && event.message.includes('Loading chunk');
    const isScriptError = target && target.tagName === 'SCRIPT' && target.src;

    if (isChunkLoadFailed || isScriptError) {
      console.warn('Chunk加载失败，尝试重新加载页面...');
      // 延迟重新加载，避免无限循环
      setTimeout(() => {
        window.location.reload();
      }, 1000);
      event.preventDefault();
      return false;
    }
  });

  // 监听未处理的Promise错误
  window.addEventListener('unhandledrejection', (event) => {
    if (event.reason && event.reason.name === 'ChunkLoadError') {
      console.warn('Chunk加载Promise错误，尝试重新加载页面...');
      setTimeout(() => {
        window.location.reload();
      }, 1000);
      event.preventDefault();
      return false;
    }
  });
};

/**
 * 网络错误处理器
 */
export const handleNetworkError = (error) => {
  if (error.code === 'NETWORK_ERROR') {
    console.warn('网络连接错误，请检查网络连接');
    return true;
  }
  return false;
};

/**
 * API错误处理器
 */
export const handleApiError = (error) => {
  if (error.response) {
    // 服务器返回了错误状态码
    const status = error.response.status;
    switch (status) {
      case 401:
        console.warn('未授权访问，请重新登录');
        // 可以在这里添加跳转到登录页面的逻辑
        break;
      case 403:
        console.warn('权限不足');
        break;
      case 404:
        console.warn('请求的资源不存在');
        break;
      case 500:
        console.warn('服务器内部错误');
        break;
      default:
        console.warn(`请求失败，状态码：${status}`);
    }
    return true;
  }
  return false;
};

/**
 * 初始化全局错误处理
 */
export const initErrorHandler = () => {
  handleResizeObserverError();
  handleChunkLoadError();

  // 可以在这里添加其他全局错误处理逻辑
  console.log('全局错误处理器已初始化');
};

/**
 * Vue组件错误处理器
 */
export const createVueErrorHandler = () => {
  return (error, instance, info) => {
    // 检查是否是ResizeObserver相关错误
    if (error.message && error.message.includes('ResizeObserver')) {
      // 忽略ResizeObserver错误
      return;
    }
    
    // 其他Vue错误正常处理
    console.error('Vue组件错误:', error);
    console.error('错误信息:', info);
    console.error('组件实例:', instance);
  };
};

/**
 * 处理题目选项数据格式转换
 * 将Java后端返回的字符串数组转换为前端需要的对象数组
 */
export const processQuestionOptions = (question) => {
  if (!question) return question;

  const processedQuestion = { ...question };

  if (processedQuestion.options && Array.isArray(processedQuestion.options)) {
    // 如果options是字符串数组，转换为对象数组
    if (processedQuestion.options.length > 0 && typeof processedQuestion.options[0] === 'string') {
      processedQuestion.options = processedQuestion.options.map((optionText, index) => {
        // 根据答案字段判断哪个选项是正确的
        const optionLetter = String.fromCharCode(65 + index); // A, B, C, D...
        const isCorrect = processedQuestion.answer && processedQuestion.answer.includes(optionLetter);
        return {
          text: optionText,
          isCorrect: isCorrect
        };
      });
    }
  }

  return processedQuestion;
};

export default {
  handleResizeObserverError,
  handleChunkLoadError,
  handleNetworkError,
  handleApiError,
  initErrorHandler,
  createVueErrorHandler,
  processQuestionOptions
};
