<template>
  <div class="question-bank-container">
    <div class="page-header">
      <h2>题库管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="navigateTo('/question-bank/create')">
          <i class="el-icon-plus"></i> 创建题目
        </el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="科目">
          <el-select v-model="filters.subjectId" placeholder="选择科目" clearable @change="handleSearch">
            <el-option v-for="subject in subjectList" :key="subject.id" :label="subject.name" :value="subject.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="filters.type" placeholder="选择题型" clearable>
            <el-option v-for="t in QUESTION_TYPES" :key="t.value" :label="t.label" :value="t.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="filters.difficulty" placeholder="选择难度" clearable>
            <el-option label="1级" value="1"></el-option>
            <el-option label="2级" value="2"></el-option>
            <el-option label="3级" value="3"></el-option>
            <el-option label="4级" value="4"></el-option>
            <el-option label="5级" value="5"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="filters.search" placeholder="搜索题目内容或标签"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt-20">
      <el-table
        :data="questions"
        style="width: 100%"
        v-loading="loading"
        border
        :height="tableHeight"
        :flexible="true"
      >
        <el-table-column prop="subject" label="科目" width="80"></el-table-column>
        <el-table-column label="题型" width="100">
          <template #default="scope">{{ questionTypeLabel(scope.row.type) }}</template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="scope">
            <el-rate 
              v-model="scope.row.difficulty" 
              disabled 
              :max="5"
              text-color="#ff9900"
            ></el-rate>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题目内容">
          <template #default="scope">
            <div class="question-content">
              <div class="content-text">{{ scope.row.content }}</div>
              <div class="content-tags" v-if="scope.row.tags && scope.row.tags.length">
                <el-tag v-for="tag in scope.row.tags" :key="tag" size="small">{{ tag }}</el-tag>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link size="small" @click="viewQuestion(scope.row)">查看</el-button>
            <el-button link size="small" @click="editQuestion(scope.row)">编辑</el-button>
            <el-button link size="small" @click="confirmDelete(scope.row)" class="delete-btn">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="pagination.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.limit"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 题目详情对话框 -->
    <el-dialog 
      title="题目详情" 
      v-model="detailDialogVisible" 
      width="60%"
      destroy-on-close
    >
      <div class="question-detail" v-if="currentQuestion">
        <div class="detail-header">
          <div class="detail-subject">{{ currentQuestion.subject }}</div>
          <div class="detail-type">{{ questionTypeLabel(currentQuestion.type) }}</div>
          <div class="detail-difficulty">
            难度: 
            <el-rate 
              v-model="currentQuestion.difficulty" 
              disabled 
              :max="5"
              text-color="#ff9900"
            ></el-rate>
          </div>
        </div>
        
        <div class="detail-content">
          <div class="content-label">题目内容:</div>
          <div class="content-text">{{ currentQuestion.content }}</div>
        </div>
        
        <div class="detail-options" v-if="currentQuestion.options && currentQuestion.options.length">
          <div class="options-label">选项:</div>
          <ul class="options-list">
            <li v-for="(option, index) in currentQuestion.options" :key="index" class="option-item">
              <span class="option-letter">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-text">{{ option.text }}</span>
              <span class="option-correct" v-if="option.isCorrect">✓</span>
            </li>
          </ul>
        </div>
        
        <div class="detail-answer" v-if="currentQuestion.answer">
          <div class="answer-label">答案:</div>
          <div class="answer-text">{{ currentQuestion.answer }}</div>
        </div>
        
        <div class="detail-analysis" v-if="currentQuestion.analysis">
          <div class="analysis-label">解析:</div>
          <div class="analysis-text">{{ currentQuestion.analysis }}</div>
        </div>
        
        <div class="detail-tags" v-if="currentQuestion.tags && currentQuestion.tags.length">
          <div class="tags-label">标签:</div>
          <div class="tags-list">
            <el-tag v-for="tag in currentQuestion.tags" :key="tag" size="small" class="tag-item">{{ tag }}</el-tag>
          </div>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="editQuestion(currentQuestion)">编辑</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../../api';
import { processQuestionOptions } from '@/utils/errorHandler';
import { QUESTION_TYPES, questionTypeLabel } from '@/utils/questionTypes';

const router = useRouter();
const questions = ref([]);
const loading = ref(true);
const detailDialogVisible = ref(false);
const currentQuestion = ref(null);
const subjectList = ref([]);

// 表格高度计算，避免ResizeObserver错误
const tableHeight = ref(400);

// 过滤条件
const filters = reactive({
  subjectId: null,
  type: '',
  difficulty: '',
  search: ''
});

// 分页信息
const pagination = reactive({
  page: 1,
  limit: 20,
  total: 0
});

// 导航到新页面
const navigateTo = (path) => {
  router.push(path);
};

// 新增: 加载科目列表的函数
const loadSubjects = async () => {
  try {
    const response = await api.subjectAdminJ.getAll();
    subjectList.value = response || [];
  } catch (error) {
    ElMessage.error('获取科目列表失败');
    console.error("Error fetching subjects for filter:", error);
  }
};

// 加载题目数据
const loadQuestions = async () => {
  try {
    loading.value = true;
    const javaParams = {
      page: pagination.page - 1, // Java API page从0开始
      size: pagination.limit,
      type: filters.type,
      difficulty: filters.difficulty ? parseInt(filters.difficulty) : null,
      keyword: filters.search,
      subjectId: filters.subjectId
    };

    // 清理掉null或空字符串的参数，以避免发送不必要的查询条件
    Object.keys(javaParams).forEach(key => {
      if (javaParams[key] === null || javaParams[key] === '') {
        delete javaParams[key];
      }
    });
    
    // const response = await api.questions.getQuestions(params);
    // questions.value = response.questions;
    // pagination.total = response.pagination.total;

    const response = await api.questionsJ.queryQuestions(javaParams);
    if (response && response.content) {
      questions.value = response.content;
      pagination.total = response.totalElements;
    } else {
      questions.value = [];
      pagination.total = 0;
      ElMessage.error('获取题目数据格式不正确或无数据');
    }

  } catch (error) {
    ElMessage.error('获取题目数据失败');
    console.error('获取题目数据失败:', error);
    questions.value = []; // 出错时清空列表
    pagination.total = 0; // 出错时重置总数
  } finally {
    loading.value = false;
  }
};

// 处理搜索
const handleSearch = () => {
  pagination.page = 1;
  loadQuestions();
};

// 重置过滤器
const resetFilters = () => {
  filters.subjectId = null;
  filters.type = '';
  filters.difficulty = '';
  filters.search = '';
  pagination.page = 1;
  loadQuestions();
};

// 处理每页显示数量变化
const handleSizeChange = (val) => {
  pagination.limit = val;
  pagination.page = 1;
  loadQuestions();
};

// 处理页码变化
const handleCurrentChange = (val) => {
  pagination.page = val;
  loadQuestions();
};

// 查看题目详情
const viewQuestion = (question) => {
  // 使用工具函数处理选项数据
  const processedQuestion = processQuestionOptions(question);
  currentQuestion.value = processedQuestion;
  detailDialogVisible.value = true;
};

// 编辑题目
const editQuestion = (question) => {
  router.push(`/question-bank/edit/${question.id}`);
};

// 确认删除
const confirmDelete = (question) => {
  ElMessageBox.confirm(
    '确定要删除这道题目吗？此操作不可恢复。',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await api.questionsJ.deleteQuestion(question.id);
      ElMessage.success('题目删除成功');
      loadQuestions();
    } catch (error) {
      ElMessage.error('删除题目失败');
      console.error('删除题目失败:', error);
    }
  }).catch(() => {});
};

onMounted(() => {
  loadSubjects();
  loadQuestions();
});
</script>

<style scoped>
.question-bank-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.filter-form .el-form-item {
  margin-right: 20px;
  margin-bottom: 15px;
}

.filter-form .el-select {
  width: 150px;
}

.filter-form .el-input {
  width: 200px;
}

.mt-20 {
  margin-top: 20px;
}

.question-content {
  display: flex;
  flex-direction: column;
}

.content-text {
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.content-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.delete-btn {
  color: #F56C6C;
}

/* 题目详情样式 */
.question-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

.detail-subject {
  font-weight: bold;
  margin-right: 15px;
}

.detail-type {
  background-color: #409EFF;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 15px;
}

.detail-content,
.detail-options,
.detail-answer,
.detail-analysis,
.detail-tags {
  margin-bottom: 20px;
}

.content-label,
.options-label,
.answer-label,
.analysis-label,
.tags-label {
  font-weight: bold;
  margin-bottom: 8px;
}

.options-list {
  list-style: none;
  padding-left: 0;
}

.option-item {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.option-letter {
  font-weight: bold;
  margin-right: 10px;
  min-width: 20px;
}

.option-correct {
  color: #67C23A;
  margin-left: 8px;
}

.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style> 