import axios from 'axios';

// 创建Node.js后端的axios实例
const nodeApi = axios.create({
  baseURL: process.env.VUE_APP_NODE_API_URL || 'http://localhost:5000/api', // 明确为Node后端URL
  timeout: 10000
});

// Node.js API 请求拦截器
nodeApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token'); // 假设Node.js后端也用这个token
    if (token) {
      config.headers['x-auth-token'] = token;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// Node.js API 响应拦截器 (保持原有逻辑，但只针对nodeApi)
nodeApi.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      // window.location.href = '/login'; // 登录页通常由Java后端管理，此处可能需要调整
      console.error('Node.js API returned 401');
    }
    return Promise.reject(error);
  }
);

// --- 创建Java后端的axios实例 ---
const javaApi = axios.create({
  baseURL: process.env.VUE_APP_JAVA_API_URL || 'http://localhost:8080', // Java后端API基础URL
  timeout: 180000, // 增加到3分钟，匹配后端设置
  withCredentials: true // 如果Java后端使用session/cookie进行认证，这很重要
});

// Java API 响应拦截器 (可以根据需要自定义，例如处理Java后端的特定错误)
javaApi.interceptors.response.use(
  response => response.data, // 直接返回 data 部分
  error => {
    // 可以在这里处理Java后端特有的错误，例如Spring Security的认证失败等
    console.error('Java API Error:', error.response || error.message);
    // 如果是401未授权错误，可能需要重定向到登录页面
    if (error.response && error.response.status === 401) {
      console.log('检测到未授权访问，重定向到登录页面');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);


// --- Node.js 后端 API 定义 (使用 nodeApi) ---
export const authAPI = {
  login: (credentials) => nodeApi.post('/auth/login', credentials),
  register: (userData) => nodeApi.post('/auth/register', userData),
  getCurrentUser: () => nodeApi.get('/auth/me'),
  updateProfile: (profileData) => nodeApi.put('/auth/profile', profileData),
  changePassword: (passwordData) => nodeApi.post('/auth/change-password', passwordData)
};

export const questionsAPI = {
  getQuestions: (params) => nodeApi.get('/questions', { params }),
  getQuestionById: (id) => nodeApi.get(`/questions/${id}`),
  createQuestion: (questionData) => nodeApi.post('/questions', questionData),
  updateQuestion: (id, questionData) => nodeApi.put(`/questions/${id}`, questionData),
  deleteQuestion: (id) => nodeApi.delete(`/questions/${id}`)
};

export const papersAPI = {
  getPapers: (params) => nodeApi.get('/papers', { params }),
  getPaperById: (id) => nodeApi.get(`/papers/${id}`),
  createPaper: (paperData) => nodeApi.post('/papers', paperData),
  updatePaper: (id, paperData) => nodeApi.put(`/papers/${id}`, paperData),
  deletePaper: (id) => nodeApi.delete(`/papers/${id}`),
  generatePaper: (criteria) => nodeApi.post('/papers/generate', criteria)
};

// Node.js AI辅助相关API (保留，以防仍有使用)
export const nodeAiAPI = {
  generateQuestion: (params) => nodeApi.post('/ai/generate-question', params),
  saveQuestion: (questionData) => nodeApi.post('/ai/save-question', questionData),
  improveQuestion: (params) => nodeApi.post('/ai/improve-question', params)
};

// --- Java 后端 API 定义 (使用 javaApi) ---
// 新增：Java后端用户API
export const javaUserAPI = {
  getCurrentUser: () => {
    console.log('调用getCurrentUser API');
    return javaApi.get('/api/user/current');
  },
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
  generateStructuredQuestions: (params) => javaApi.post('/api/ai/generate-question', null, { params }),
  saveGeneratedQuestions: (questionsData) => javaApi.post('/api/ai/save-questions', questionsData),
  generateBatchQuestions: (payload) => javaApi.post('/api/ai/generate-batch-questions', payload),
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

// 更新统一导出以包含 settingsAPI
const apis = {
  // Node.js APIs
  auth: authAPI,
  questions: questionsAPI,
  papers: papersAPI,
  ai: nodeAiAPI, // 旧的AI API 指向 Node.js
  // settings: settingsAPI, // Comment out or remove Node.js settings

  // Java APIs
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