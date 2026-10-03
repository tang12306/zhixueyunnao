<template>
  <div class="select-from-bank-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <span>从题库选题组卷</span>
        </div>
      </template>

      <!-- 1. 筛选条件区域 -->
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="学科">
          <el-select v-model="filters.subjectId" placeholder="选择学科" clearable @change="handleSubjectChange" style="width: 200px;">
            <el-option v-for="item in subjects" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="章节">
          <el-select v-model="filters.chapterId" placeholder="选择章节" clearable :disabled="!filters.subjectId || chapters.length === 0" style="width: 200px;">
            <el-option v-for="item in chapters" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="filters.type" placeholder="选择题型" clearable style="width: 150px;">
            <el-option label="单选题" value="SINGLE_CHOICE"></el-option>
            <el-option label="多选题" value="MULTIPLE_CHOICE"></el-option>
            <el-option label="判断题" value="TRUE_FALSE"></el-option>
            <el-option label="填空题" value="FILL_IN_THE_BLANK"></el-option>
            <el-option label="简答题" value="SHORT_ANSWER"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-rate v-model="filters.difficulty" :max="5" allow-half clearable style="margin-top: 8px;"></el-rate>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="搜索题目内容、标签" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询题目</el-button>
          <el-button @click="resetFilters">重置筛选</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 左侧: 题目列表与选择 -->
      <el-col :span="16">
        <el-card class="box-card">
          <template #header>
            <span>可选题目列表 (共 {{ questionPagination.total }} 条)</span>
          </template>
          <el-table
            ref="questionTableRef"
            :data="questions"
            style="width: 100%"
            v-loading="loadingQuestions"
            @selection-change="handleQuestionSelectionChange"
            row-key="id"
          >
            <el-table-column type="selection" width="55" :reserve-selection="true"></el-table-column>
            <el-table-column prop="content" label="题目内容" show-overflow-tooltip>
                <template #default="scope">
                    <div v-html="scope.row.content.substring(0, 100) + (scope.row.content.length > 100 ? '...' : '')"></div>
                </template>
            </el-table-column>
            <el-table-column prop="type" label="题型" width="120">
                <template #default="scope">
                    {{ formatQuestionType(scope.row.type) }}
                </template>
            </el-table-column>
            <el-table-column prop="difficulty" label="难度" width="100">
                <template #default="scope">
                    <el-rate v-model="scope.row.difficulty" disabled :max="5"></el-rate>
                </template>
            </el-table-column>
             <el-table-column prop="subject" label="学科" width="100"></el-table-column>
          </el-table>
          <el-pagination
            style="margin-top: 20px; text-align: right;"
            v-model:current-page="questionPagination.page"
            v-model:page-size="questionPagination.size"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="questionPagination.total"
            @size-change="handleQuestionPageSizeChange"
            @current-change="handleQuestionPageCurrentChange"
          ></el-pagination>
        </el-card>
      </el-col>

      <!-- 右侧: 已选题目与试卷配置 -->
      <el-col :span="8">
        <el-card class="box-card">
          <template #header>
            <span>试卷配置 (已选 {{ selectedQuestions.length }} 题)</span>
          </template>
          <el-form :model="paperForm" label-position="top" ref="paperFormRef">
            <el-form-item label="试卷标题" prop="title">
              <el-input v-model="paperForm.title" placeholder="请输入试卷标题"></el-input>
            </el-form-item>
            <el-form-item label="试卷描述" prop="description">
              <el-input type="textarea" v-model="paperForm.description" placeholder="请输入试卷描述 (可选)"></el-input>
            </el-form-item>
            <el-form-item label="考试时长 (分钟)" prop="duration">
                <el-input-number v-model="paperForm.durationMinutes" :min="30" :step="10" placeholder="如: 120"></el-input-number>
            </el-form-item>

            <!-- 已选题目详细列表 -->
            <div v-if="selectedQuestionsWithScores.length > 0" class="selected-questions-detail">
              <h4>已选题目详情:</h4>
              <div class="question-list">
                <div v-for="(item, index) in selectedQuestionsWithScores" :key="item.id" class="question-item">
                  <div class="question-header">
                    <span class="question-number">{{ index + 1 }}.</span>
                    <span class="question-type">{{ formatQuestionType(item.type) }}</span>
                    <el-input-number
                      v-model="item.score"
                      :min="1"
                      :max="100"
                      size="small"
                      class="score-input"
                      @change="updateQuestionScore(item.id, item.score)"
                    ></el-input-number>
                    <span class="score-label">分</span>
                  </div>
                  <div class="question-content">
                    <el-text class="question-text" truncated>{{ item.content }}</el-text>
                  </div>
                  <div class="question-actions">
                    <el-button size="small" type="text" @click="previewQuestion(item)">预览</el-button>
                    <el-button size="small" type="text" @click="removeQuestion(item.id)" style="color: #f56c6c;">移除</el-button>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="selectedQuestionsWithScores.length === 0" class="no-questions">
              <el-empty description="暂未选择题目" :image-size="80"></el-empty>
            </div>

            <div class="score-summary">
              <p><strong>总题数: {{ selectedQuestionsWithScores.length }} 题</strong></p>
              <p><strong>总分: {{ calculatedTotalScore }} 分</strong></p>
            </div>


            <el-button type="primary" @click="exportToWord" :loading="exportingWord" :disabled="selectedQuestionsWithScores.length === 0">
              <el-icon><Download /></el-icon>
              导出Word试卷
            </el-button>
            <el-button @click="clearSelection">清空已选</el-button>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Download } from '@element-plus/icons-vue';
import api from '@/api/index.js'; // 假设 api/index.js 中已包含 javaApi 的定义

// --- Data Refs ---
const subjects = ref([]);
const chapters = ref([]);
const questions = ref([]);
const selectedQuestions = ref([]);
const selectedQuestionsWithScores = ref([]); // 存储选中题目及其分数信息
const exportingWord = ref(false); // Word导出状态

const filters = reactive({
  subjectId: null,
  chapterId: null,
  type: null,
  difficulty: null,
  keyword: '',
});

const questionPagination = reactive({
  page: 1,
  size: 10,
  total: 0,
});

const paperForm = reactive({
  title: '',
  description: '',
  durationMinutes: 120,
  defaultScore: 5, // 默认每题分数
});

const loadingSubjects = ref(false);
const loadingChapters = ref(false);
const loadingQuestions = ref(false);

// DOM Refs
const questionTableRef = ref(null);
const paperFormRef = ref(null);


// --- Computed Properties ---
const calculatedTotalScore = computed(() => {
  if (selectedQuestionsWithScores.value.length > 0) {
    return selectedQuestionsWithScores.value.reduce((total, item) => total + (item.score || 0), 0);
  }
  return 0;
});


// --- Lifecycle Hooks ---
onMounted(() => {
  fetchSubjects();
  // 页面加载时自动获取题目列表
  fetchQuestions();
});

// --- Watchers ---
watch(() => filters.subjectId, (newSubjectId) => {
  filters.chapterId = null; // Reset chapter when subject changes
  if (newSubjectId) {
    fetchChapters(newSubjectId);
  } else {
    chapters.value = [];
  }
  // 学科变化时重新查询题目
  questionPagination.page = 1;
  fetchQuestions();
});

// --- Methods ---
const fetchSubjects = async () => {
  loadingSubjects.value = true;
  try {
    const response = await api.subjectsJ.getAllSubjects();
    console.log('获取到的科目列表:', response);
    
    if (Array.isArray(response)) {
      subjects.value = response;
    } else if (response && response.content && Array.isArray(response.content)) {
      subjects.value = response.content;
    } else {
      console.warn('科目数据结构不符合预期:', response);
      subjects.value = [];
    }
  } catch (error) {
    ElMessage.error('获取科目列表失败');
    console.error("Error fetching subjects:", error);
    subjects.value = [];
  } finally {
    loadingSubjects.value = false;
  }
};

const fetchChapters = async (subjectId) => {
  if (!subjectId) return;
  loadingChapters.value = true;
  try {
    console.log(`获取科目ID ${subjectId} 的章节`);
    const response = await api.chaptersJ.getChaptersBySubject(subjectId);
    console.log('获取到的章节列表:', response);
    
    if (Array.isArray(response)) {
      chapters.value = response;
    } else if (response && response.content && Array.isArray(response.content)) {
      chapters.value = response.content;
    } else {
      console.warn('章节数据结构不符合预期:', response);
      chapters.value = [];
    }
  } catch (error) {
    ElMessage.error('获取章节列表失败');
    console.error(`Error fetching chapters for subjectId ${subjectId}:`, error);
    chapters.value = [];
  } finally {
    loadingChapters.value = false;
  }
};

const fetchQuestions = async () => {
  loadingQuestions.value = true;
  try {
    const params = {
      subjectId: filters.subjectId,
      chapterId: filters.chapterId,
      type: filters.type,
      difficulty: filters.difficulty ? Math.round(filters.difficulty) : null, // ElRate v-model อาจเป็นทศนิยม
      keyword: filters.keyword,
      page: questionPagination.page - 1, // Spring Pageable is 0-indexed
      size: questionPagination.size,
      sort: 'id,desc' // 按ID降序排列，显示最新题目
    };
    // 清理无效参数
    Object.keys(params).forEach(key => {
      if (params[key] === null || params[key] === '') {
        delete params[key];
      }
    });

    console.log('查询题目参数:', params);
    // questionsJ 指向Java后端题目查询接口
    const response = await api.questionsJ.queryQuestions(params);
    console.log('题目查询响应:', response);

    if (response && response.content) {
      questions.value = response.content;
      questionPagination.total = response.totalElements;

      if (response.content.length === 0 && questionPagination.total === 0) {
        ElMessage.info('暂无符合条件的题目，请调整筛选条件');
      }
    } else {
      questions.value = [];
      questionPagination.total = 0;
      ElMessage.warning('获取题目数据格式异常');
    }
  } catch (error) {
    ElMessage.error('查询题目失败: ' + (error.response?.data?.message || error.message));
    console.error("Error fetching questions:", error);
    questions.value = [];
    questionPagination.total = 0;
  } finally {
    loadingQuestions.value = false;
  }
};

const handleSearch = () => {
  questionPagination.page = 1; // Reset to first page for new search
  fetchQuestions();
};

const handleSubjectChange = (subjectId) => {
  // 学科变化时清空章节选择
  filters.chapterId = null;
  // 重置分页并查询题目
  questionPagination.page = 1;
  fetchQuestions();
};

const resetFilters = () => {
  Object.keys(filters).forEach(key => {
    filters[key] = (key === 'difficulty') ? null : (key === 'keyword') ? '' : null;
  });
  questionPagination.page = 1;
  questions.value = [];
  questionPagination.total = 0;
  selectedQuestions.value = [];
  selectedQuestionsWithScores.value = [];
  if (questionTableRef.value) {
    questionTableRef.value.clearSelection();
  }
};

const handleQuestionPageSizeChange = (newSize) => {
  questionPagination.size = newSize;
  fetchQuestions();
};

const handleQuestionPageCurrentChange = (newPage) => {
  questionPagination.page = newPage;
  fetchQuestions();
};

const handleQuestionSelectionChange = (selection) => {
  try {
    selectedQuestions.value = selection || [];

    // 更新带分数的选中题目列表
    selectedQuestionsWithScores.value = (selection || []).map(question => ({
      ...question,
      score: question.score || 5 // 使用题目原有分数，如果没有则默认5分
    }));
  } catch (error) {
    console.error('处理题目选择变化时出错:', error);
    selectedQuestions.value = [];
    selectedQuestionsWithScores.value = [];
  }
};

const clearSelection = () => {
    selectedQuestions.value = [];
    selectedQuestionsWithScores.value = [];
    if(questionTableRef.value) {
        questionTableRef.value.clearSelection();
    }
    ElMessage.success('已清空所选题目');
};

const formatQuestionType = (typeEnum) => {
  const map = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_IN_THE_BLANK: '填空题',
    SHORT_ANSWER: '简答题'
  };
  return map[typeEnum] || typeEnum;
};


// Word导出功能
const exportToWord = async () => {
  if (selectedQuestionsWithScores.value.length === 0) {
    ElMessage.warning('请至少选择一道题目');
    return;
  }
  if (!paperForm.title.trim()) {
    ElMessage.warning('请输入试卷标题');
    paperFormRef.value?.validateField('title');
    return;
  }

  exportingWord.value = true;
  try {
    const paperData = {
      title: paperForm.title,
      description: paperForm.description,
      duration: paperForm.durationMinutes,
      // 使用每个题目的实际分数
      questions: selectedQuestionsWithScores.value.map(item => ({
        questionId: item.id,
        score: item.score
      })),
      totalScore: calculatedTotalScore.value,
      subject: filters.subjectId ? subjects.value.find(s => s.id === filters.subjectId)?.name : '综合'
    };

    console.log('开始导出试卷Word文档:', paperData);

    const response = await api.papersJ.exportToWord(paperData);

    // 创建下载链接
    const blob = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;

    // 设置文件名
    const fileName = `${paperForm.title || '试卷'}.docx`;
    link.download = fileName;

    // 触发下载
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    ElMessage.success('试卷导出成功！');
    resetFormAndSelection();
  } catch (error) {
    console.error('导出试卷失败:', error);
    ElMessage.error('导出试卷失败: ' + (error.response?.data?.message || error.message));
  } finally {
    exportingWord.value = false;
  }
};

const resetFormAndSelection = () => {
    resetFilters(); // 清空筛选和题目列表
    paperForm.title = '';
    paperForm.description = '';
    paperForm.durationMinutes = 120;
    paperForm.defaultScore = 5;
     if (paperFormRef.value) {
        paperFormRef.value.resetFields();
    }
};

// 更新题目分数
const updateQuestionScore = (questionId, newScore) => {
  try {
    const item = selectedQuestionsWithScores.value.find(q => q.id === questionId);
    if (item && typeof newScore === 'number' && newScore > 0) {
      item.score = newScore;
    }
  } catch (error) {
    console.error('更新题目分数时出错:', error);
  }
};

// 预览题目
const previewQuestion = (question) => {
  // 安全地处理HTML内容，避免XSS和格式错误
  const escapeHtml = (text) => {
    if (!text) return '';
    return text.toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  };

  const formatOptions = (options) => {
    if (!options || !Array.isArray(options) || options.length === 0) {
      return '';
    }
    return options.map((opt, idx) =>
      `<li>${String.fromCharCode(65 + idx)}. ${escapeHtml(opt)}</li>`
    ).join('');
  };

  const htmlContent = `
    <div style="text-align: left; line-height: 1.6;">
      <p><strong>题型:</strong> ${escapeHtml(formatQuestionType(question.type))}</p>
      <p><strong>难度:</strong> ${question.difficulty || 0} 星</p>
      <p><strong>分数:</strong> ${question.score || 0} 分</p>
      <p><strong>题目内容:</strong></p>
      <div style="margin: 10px 0; padding: 15px; background: #f5f7fa; border-radius: 6px; border-left: 4px solid #409eff;">
        ${escapeHtml(question.content || '')}
      </div>
      ${question.options && question.options.length > 0 ?
        `<p><strong>选项:</strong></p>
         <ul style="margin: 10px 0; padding-left: 20px;">
           ${formatOptions(question.options)}
         </ul>` : ''}
      <p><strong>答案:</strong> ${escapeHtml(question.answer || '暂无')}</p>
      ${question.analysis ?
        `<p><strong>解析:</strong></p>
         <div style="margin: 10px 0; padding: 10px; background: #f0f9ff; border-radius: 4px;">
           ${escapeHtml(question.analysis)}
         </div>` : ''}
    </div>
  `;

  ElMessageBox.alert(htmlContent, '题目预览', {
    dangerouslyUseHTMLString: true,
    customClass: 'question-preview-dialog',
    confirmButtonText: '关闭',
    showCancelButton: false,
    closeOnClickModal: true,
    closeOnPressEscape: true
  }).catch(() => {
    // 捕获用户取消操作，避免控制台错误
    console.log('用户关闭了预览对话框');
  });
};

// 移除题目
const removeQuestion = (questionId) => {
  try {
    // 从选中列表中移除
    selectedQuestionsWithScores.value = selectedQuestionsWithScores.value.filter(q => q.id !== questionId);
    selectedQuestions.value = selectedQuestions.value.filter(q => q.id !== questionId);

    // 更新表格选择状态
    if (questionTableRef.value && questions.value) {
      // 使用 nextTick 确保DOM更新完成后再操作表格
      nextTick(() => {
        try {
          const tableData = questions.value;
          const currentSelection = tableData.filter(q =>
            selectedQuestions.value.some(sq => sq.id === q.id)
          );

          questionTableRef.value.clearSelection();
          currentSelection.forEach(row => {
            questionTableRef.value.toggleRowSelection(row, true);
          });
        } catch (error) {
          console.warn('更新表格选择状态时出错:', error);
        }
      });
    }

    ElMessage.success('已移除题目');
  } catch (error) {
    console.error('移除题目时出错:', error);
    ElMessage.error('移除题目失败');
  }
};



</script>

<style scoped>
.select-from-bank-container {
  padding: 20px;
}
.filter-form .el-form-item {
  margin-bottom: 10px; /* Reduce bottom margin for a more compact filter area */
  margin-right: 15px; /* Add right margin for better spacing */
}
.filter-form {
  margin-bottom: 20px;
}
.box-card {
  margin-bottom: 20px;
}
.selected-questions-summary ul {
    list-style-type: none;
    padding-left: 0;
    max-height: 150px;
    overflow-y: auto;
    font-size: 0.9em;
}
.selected-questions-summary li {
    margin-bottom: 5px;
    border-bottom: 1px solid #eee;
    padding-bottom: 3px;
}

/* 确保选择框有足够的宽度显示选中内容 */
.el-select {
  min-width: 120px;
}

/* 改善表格选择列的样式 */
.el-table .el-table__header-wrapper th:first-child,
.el-table .el-table__body-wrapper td:first-child {
  text-align: center;
}

/* 已选题目详情样式 */
.selected-questions-detail {
  margin: 15px 0;
}

.question-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.question-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.question-item:last-child {
  border-bottom: none;
}

.question-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.question-number {
  font-weight: bold;
  margin-right: 8px;
  min-width: 30px;
}

.question-type {
  background: #e1f3d8;
  color: #67c23a;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-right: 10px;
}

.score-input {
  margin-left: auto;
  margin-right: 5px;
  width: 80px;
}

.score-label {
  font-size: 14px;
  color: #606266;
}

.question-content {
  margin: 8px 0;
}

.question-text {
  color: #606266;
  font-size: 14px;
  line-height: 1.4;
}

.question-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.no-questions {
  text-align: center;
  padding: 20px;
}

.score-summary {
  margin-top: 15px;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 4px;
  border-left: 4px solid #409eff;
}

.score-summary p {
  margin: 5px 0;
  color: #303133;
}

/* 题目预览对话框样式 */
:deep(.question-preview-dialog) {
  width: 700px;
  max-width: 90vw;
}

:deep(.question-preview-dialog .el-message-box__content) {
  max-height: 600px;
  overflow-y: auto;
  text-align: left;
}

:deep(.question-preview-dialog .el-message-box__message) {
  margin: 0;
  padding: 0;
}

:deep(.question-preview-dialog .el-message-box__header) {
  padding-bottom: 15px;
}

:deep(.question-preview-dialog .el-message-box__btns) {
  padding-top: 15px;
  text-align: center;
}
</style>