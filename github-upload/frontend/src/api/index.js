import axios from 'axios';

// 所有请求都发往 Spring Boot 后端。
// 开发时 vue devServer 把 /api、/questions 代理到后端（见 vue.config.js），上线时由 Nginx 同源转发，
// 所以 baseURL 默认为空。前后端确需分开部署时再设置 VUE_APP_API_BASE。
const javaApi = axios.create({
  baseURL: process.env.VUE_APP_API_BASE || '',
  timeout: 180000, // AI 生成较慢，3 分钟，与后端保持一致
  withCredentials: true, // 登录状态保存在会话 Cookie 里
  // 让后端在未登录时返回 401 JSON，而不是重定向到旧版登录页。
  // 写请求的 CSRF 令牌由 axios 自动从 XSRF-TOKEN Cookie 读出，放进 X-XSRF-TOKEN 请求头。
  headers: { 'X-Requested-With': 'XMLHttpRequest' }
});

// 会话失效时的处理（跳转登录页），由 main.js 注册，避免这里反过来依赖 router
let unauthorizedHandler = null;
export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

javaApi.interceptors.response.use(
  response => response.data, // 直接返回 data 部分
  async error => {
    const status = error.response && error.response.status;
    const url = (error.config && error.config.url) || '';
    // 登录、获取当前用户这类接口的 401 由调用方自己处理
    if (status === 401 && !url.startsWith('/api/auth/') && unauthorizedHandler) {
      unauthorizedHandler();
    }
    // 下载类接口（responseType: 'blob'）出错时，错误信息也是 Blob，先转回 JSON
    const data = error.response && error.response.data;
    if (typeof Blob !== 'undefined' && data instanceof Blob && (data.type || '').includes('json')) {
      try {
        error.response.data = JSON.parse(await data.text());
      } catch (e) {
        // 解析失败就保留原样
      }
    }
    return Promise.reject(error);
  }
);

/**
 * 取出后端返回的错误提示。后端出错时返回 {success: false, message, data}，message 可以直接展示。
 */
export function errorMessage(error, fallback = '请求失败，请稍后重试') {
  const data = error && error.response && error.response.data;
  if (data && typeof data.message === 'string' && data.message) {
    return data.message;
  }
  if (error && error.code === 'ECONNABORTED') {
    return '请求超时，请稍后重试';
  }
  return fallback;
}

/** AI 回复无法解析时，后端会在 data.raw 里带上模型的原始输出 */
export function aiRawReply(error) {
  const data = error && error.response && error.response.data;
  return (data && data.data && typeof data.data.raw === 'string') ? data.data.raw : '';
}

// 登录 / 退出 / 当前用户
export const authAPI = {
  login: (credentials) => javaApi.post('/api/auth/login', credentials),
  logout: () => javaApi.post('/api/auth/logout'),
  me: () => javaApi.get('/api/auth/me')
};

// 新增：Java后端用户API
export const javaUserAPI = {
  getCurrentUser: () => javaApi.get('/api/user/current'),
  updateProfile: (profileData) => javaApi.put('/api/user/profile', profileData),
  changePassword: (passwordData) => javaApi.post('/api/user/change-password', passwordData)
};

export const javaSubjectsAPI = {
  getAllSubjects: () => javaApi.get('/api/subjects')
};

export const javaChaptersAPI = {
  getChaptersBySubject: (subjectId) => javaApi.get(`/api/subjects/${subjectId}/chapters`)
};

export const javaAiAPI = {
  saveGeneratedQuestions: (questionsData) => javaApi.post('/api/ai/save-questions', questionsData),
  // 出题、出卷是异步任务：提交后返回 {taskId}，再用 getTask 轮询结果（见 composables/useAiTask.js）
  generateBatchQuestions: (payload) => javaApi.post('/api/ai/generate-batch-questions', payload),
  generateExam: (payload) => javaApi.post('/api/ai/generate-exam', payload),
  getTask: (taskId) => javaApi.get(`/api/ai/tasks/${encodeURIComponent(taskId)}`),
  exportExamToWord: (examData) => javaApi.post('/api/ai/export/word', examData, {
    responseType: 'blob',
    headers: {
      'Accept': 'application/octet-stream'
    }
  })
};

// Java后端题目管理API
export const javaQuestionsAPI = {
  queryQuestions: (params) => javaApi.get('/questions/api/query', { params }),
  getQuestionById: (id) => javaApi.get(`/questions/${id}`),
  createQuestion: (questionData) => javaApi.post('/questions', questionData),
  updateQuestion: (id, questionData) => javaApi.put(`/questions/${id}`, questionData),
  deleteQuestion: (id) => javaApi.delete(`/questions/${id}`)
};

// Java后端试卷管理API
export const javaPapersAPI = {
  exportToWord: (paperData) => javaApi.post('/api/papers/export/word', paperData, {
    responseType: 'blob',
    headers: {
      'Accept': 'application/octet-stream'
    }
  })
};

// 新增：Java后端科目管理 CRUD API
export const javaSubjectAdminAPI = {
  getAll: () => javaApi.get('/api/subjects'), // Changed from /subjects/api/all
  getById: (id) => javaApi.get(`/api/subjects/${id}`),
  create: (subjectData) => javaApi.post('/api/subjects', subjectData),
  update: (id, subjectData) => javaApi.put(`/api/subjects/${id}`, subjectData),
  delete: (id) => javaApi.delete(`/api/subjects/${id}`),
  getSubjectQuestionCounts: () => javaApi.get('/api/subjects/statistics/question-counts') // 新增统计接口
};

// 新增：Java后端学生管理 CRUD API
export const javaStudentAPI = {
  getAllStudents: (params) => javaApi.get('/api/students', { params }),
  count: () => javaApi.get('/api/students/count'),
  getStudentById: (id) => javaApi.get(`/api/students/${id}`),
  createStudent: (studentData) => javaApi.post('/api/students', studentData),
  updateStudent: (id, studentData) => javaApi.put(`/api/students/${id}`, studentData),
  deleteStudent: (id) => javaApi.delete(`/api/students/${id}`),
  getAvailableClasses: () => javaApi.get('/api/students/available-classes')
};

// 新增：Java后端组织管理 API (学院、专业、班级)
export const javaCollegeAdminAPI = {
  getAll: () => javaApi.get('/api/colleges'),
  getById: (id) => javaApi.get(`/api/colleges/${id}`),
  create: (data) => javaApi.post('/api/colleges', data),
  update: (id, data) => javaApi.put(`/api/colleges/${id}`, data),
  delete: (id) => javaApi.delete(`/api/colleges/${id}`)
};

export const javaMajorAdminAPI = {
  getAll: (params) => javaApi.get('/api/majors', { params }), // params can include collegeId
  getById: (id) => javaApi.get(`/api/majors/${id}`),
  create: (data) => javaApi.post('/api/majors', data),
  update: (id, data) => javaApi.put(`/api/majors/${id}`, data),
  delete: (id) => javaApi.delete(`/api/majors/${id}`)
};

export const javaClassAdminAPI = {
  getAll: (params) => javaApi.get('/api/classes', { params }), // params can include majorId
  getById: (id) => javaApi.get(`/api/classes/${id}`),
  create: (data) => javaApi.post('/api/classes', data),
  update: (id, data) => javaApi.put(`/api/classes/${id}`, data),
  delete: (id) => javaApi.delete(`/api/classes/${id}`),
  // 新增：检查班级是否可以删除
  canDelete: (id) => javaApi.get(`/api/classes/${id}/can-delete`)
};

// 新增：Java后端章节管理 CRUD API
export const javaChapterAdminAPI = {
  getChaptersBySubjectId: (subjectId) => javaApi.get(`/api/subjects/${subjectId}/chapters`),
  createChapter: (chapterData) => javaApi.post("/api/chapters", chapterData),
  updateChapter: (id, chapterData) => javaApi.put(`/api/chapters/${id}`, chapterData),
  deleteChapter: (id) => javaApi.delete(`/api/chapters/${id}`)
  // getChapterById might be needed if edit form fetches full details separately
  // getChapterById: (id) => javaApi.get(`/api/chapters/${id}`),
};

// 新增：Java后端系统设置 API
export const javaSettingsAPI = {
  getSettings: () => javaApi.get('/api/settings'),
  // getSettingByKey: (key) => javaApi.get(`/api/settings/${key}`), // Currently not used by UI but available
  updateSetting: (key, value) => javaApi.put(`/api/settings/${key}`, { value }) // Payload is { value: ... }
};

const apis = {
  auth: authAPI,
  userJ: javaUserAPI, // 新增Java用户API
  subjectsJ: javaSubjectsAPI,
  chaptersJ: javaChaptersAPI,
  aiJ: javaAiAPI,
  questionsJ: javaQuestionsAPI, // Java 题库API (用于新组件)
  papersJ: javaPapersAPI, // Java 试卷API
  subjectAdminJ: javaSubjectAdminAPI, // Exporting the new admin API for subjects
  chapterAdminJ: javaChapterAdminAPI, // Exporting the new admin API for chapters
  studentJ: javaStudentAPI,
  collegeAdminJ: javaCollegeAdminAPI, // 新增
  majorAdminJ: javaMajorAdminAPI,     // 新增
  classAdminJ: javaClassAdminAPI,      // 新增
  settingsJ: javaSettingsAPI      // 新增 Java 系统设置API
};

export default apis; 