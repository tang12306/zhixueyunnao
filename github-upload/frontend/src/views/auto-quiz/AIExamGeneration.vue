<template>
  <div class="ai-exam-generation">
    <h1 class="page-title">AI一键出卷</h1>
    
    <el-card class="generation-card">
      <el-steps :active="activeStep" finish-status="success" simple>
        <el-step title="基本信息" icon="el-icon-edit" />
        <el-step title="考题规划" icon="el-icon-document" />
        <el-step title="课程规划" icon="el-icon-s-data" />
        <el-step title="生成预览" icon="el-icon-view" />
      </el-steps>

      <div class="step-content">
        <!-- 步骤1：基本信息 -->
        <div v-if="activeStep === 0">
          <h3>考试基本信息</h3>
          <el-form ref="examForm" :model="examData" :rules="examRules" label-position="top">
            <el-form-item label="考试名称" prop="name">
              <el-input v-model="examData.name" placeholder="请输入考试名称" maxlength="50" show-word-limit />
            </el-form-item>
            
            <el-form-item label="考试描述" prop="description">
              <el-input 
                v-model="examData.description" 
                type="textarea" 
                placeholder="请输入考试描述" 
                :rows="3"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="考试时长(分钟)" prop="duration">
                  <el-input-number v-model="examData.duration" :min="30" :max="180" :step="15" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="考试总分" prop="totalScore">
                  <el-input-number v-model="examData.totalScore" :min="60" :max="150" :step="10" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="考试日期" prop="examDate">
              <el-date-picker
                v-model="examData.examDate"
                type="datetime"
                placeholder="选择考试日期和时间"
                format="YYYY-MM-DD HH:mm"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>

            <el-form-item label="目标班级" prop="targetClasses">
              <el-select
                v-model="examData.targetClasses"
                multiple
                filterable
                placeholder="请选择目标班级"
              >
                <el-option
                  v-for="item in classList"
                  :key="item.id"
                  :label="item.name"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤2：考题规划 -->
        <div v-else-if="activeStep === 1">
          <h3>考题规划</h3>
          <el-form ref="questionPlanForm" :model="examData" label-position="top">
            <el-form-item label="科目" prop="subjectId">
              <el-select
                v-model="examData.subjectId"
                placeholder="请选择科目"
                @change="loadChapters"
              >
                <el-option
                  v-for="subject in subjectList"
                  :key="subject.id"
                  :label="subject.name"
                  :value="subject.id"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="题目分布">
              <div class="question-distribution">
                <div 
                  v-for="(type, index) in questionTypes" 
                  :key="index" 
                  class="question-type-item"
                >
                  <span>{{ type.label }}</span>
                  <div class="question-type-controls">
                    <el-input-number 
                      v-model="examData.questionPlan[type.value].count" 
                      :min="0" 
                      :max="20" 
                      size="small"
                      @change="updateTotalQuestions"
                    />
                    <span>题</span>
                    <el-input-number 
                      v-model="examData.questionPlan[type.value].scorePerQuestion" 
                      :min="1" 
                      :max="20" 
                      size="small"
                      @change="updateTotalScore"
                    />
                    <span>分/题</span>
                  </div>
                </div>
              </div>
              <div class="summary">
                <p>题目总数: {{ totalQuestions }} 题</p>
                <p>总分: {{ calculatedTotalScore }} 分</p>
                <p v-if="calculatedTotalScore !== examData.totalScore" class="warning">
                  注意: 当前分配分数 ({{ calculatedTotalScore }}) 与设定总分 ({{ examData.totalScore }}) 不一致
                </p>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 步骤3：课程规划 -->
        <div v-else-if="activeStep === 2">
          <h3>课程章节规划</h3>
          <el-form v-if="examData.subjectId" ref="chapterPlanForm" label-position="top">
            <el-form-item label="选择考试范围的章节">
              <div class="chapter-selection">
                <el-checkbox
                  v-model="examData.allChapters"
                  @change="toggleAllChapters"
                >全选</el-checkbox>
                <div class="chapter-list">
                  <el-checkbox-group v-model="examData.chapterIds">
                    <el-checkbox 
                      v-for="chapter in chapterList" 
                      :key="chapter.id" 
                      :label="chapter.id"
                    >
                      {{ chapter.name }}
                    </el-checkbox>
                  </el-checkbox-group>
                </div>
              </div>
              <div class="summary">
                <p>已选章节: {{ examData.chapterIds.length }} 章</p>
              </div>
            </el-form-item>

            <el-form-item label="章节分布说明">
              <el-input
                v-model="examData.chapterDistribution"
                type="textarea"
                placeholder="请描述各章节题目的分布要求，例如：第一章占30%，第二章占20%..."
                :rows="3"
              />
            </el-form-item>

            <el-form-item label="考试要求">
              <el-input
                v-model="examData.examRequirements"
                type="textarea"
                placeholder="请输入对考试的特殊要求，例如：题目难度、覆盖知识点等"
                :rows="3"
              />
            </el-form-item>
          </el-form>
          <el-alert v-else
            title="请先在考题规划步骤中选择科目"
            type="warning"
            show-icon
          />
        </div>

        <!-- 步骤4：生成预览 -->
        <div v-else-if="activeStep === 3">
          <h3>试卷生成预览</h3>
          
          <el-card v-if="!generating && !examPreview.questions.length" class="preview-placeholder">
            <div class="start-generation">
              <el-button type="primary" :loading="generating" @click="generateExam">
                开始生成试卷
              </el-button>
              <p class="tip">点击按钮开始生成试卷，可能需要等待1-2分钟</p>
            </div>
          </el-card>
          
          <div v-else>
            <div v-if="generating" class="generating-spinner">
              <el-spinner type="primary" />
              <p>正在生成试卷，请稍候...</p>
            </div>
            
            <div v-else class="exam-preview">
              <div class="exam-header-preview">
                <h2>{{ examPreview.name }}</h2>
                <p>{{ examPreview.description }}</p>
                <div class="exam-info-preview">
                  <span>考试时长: {{ examPreview.duration }}分钟</span>
                  <span>总分: {{ examPreview.totalScore }}分</span>
                </div>
              </div>
              
              <el-divider content-position="center">预览</el-divider>
              
              <div v-for="(section, sectionIndex) in questionSections" :key="sectionIndex" class="question-section">
                <h3>{{ section.title }}</h3>
                <div v-for="(question, questionIndex) in section.questions" :key="questionIndex" class="preview-question-item">
                  <div class="preview-question-header">
                    <span class="preview-question-number">{{ questionIndex + 1 }}</span>
                    <span class="preview-question-score">({{ question.score }}分)</span>
                  </div>
                  <div class="preview-question-content" v-html="question.content"></div>
                  
                  <div v-if="question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE'" class="preview-options">
                    <div v-for="(option, optionIndex) in question.options" :key="optionIndex" class="preview-option">
                      {{ option }}
                    </div>
                  </div>
                </div>
              </div>
              
              <div class="action-buttons">
                <el-button type="success" @click="saveQuestionsToBank">保存题目到题库</el-button>
                <el-dropdown @command="handleExportCommand">
                  <el-button type="primary">
                    导出文档 <i class="el-icon-arrow-down el-icon--right"></i>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item command="word">导出试卷(Word)</el-dropdown-item>
                      <el-dropdown-item command="answerSheet">导出答题卡(Word)</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="step-actions">
        <el-button v-if="activeStep > 0" @click="prevStep">上一步</el-button>
        <el-button
          v-if="activeStep < 3"
          type="primary"
          @click="nextStep"
        >
          下一步
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import axios from 'axios';
import api from '../../api';

// 当前步骤
const activeStep = ref(0);

// 班级列表
const classList = ref([]);
// 科目列表
const subjectList = ref([]);
// 章节列表
const chapterList = ref([]);

// 题型列表
const questionTypes = [
  { label: '单选题', value: 'SINGLE_CHOICE' },
  { label: '多选题', value: 'MULTIPLE_CHOICE' },
  { label: '填空题', value: 'FILL_IN_THE_BLANK' },
  { label: '简答题', value: 'SHORT_ANSWER' }
];

// 考试数据
const examData = reactive({
  name: '',
  description: '',
  duration: 90,
  totalScore: 100,
  examDate: '',
  subjectId: null,
  targetClasses: [],
  allChapters: false,
  chapterIds: [],
  chapterDistribution: '',
  examRequirements: '',
  questionPlan: {
    SINGLE_CHOICE: { count: 10, scorePerQuestion: 3 },
    MULTIPLE_CHOICE: { count: 5, scorePerQuestion: 4 },
    FILL_IN_THE_BLANK: { count: 10, scorePerQuestion: 3 },
    SHORT_ANSWER: { count: 3, scorePerQuestion: 10 }
  }
});

// 试卷预览数据
const examPreview = reactive({
  name: '',
  description: '',
  duration: 0,
  totalScore: 0,
  questions: []
});

// 验证规则
const examRules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  description: [{ required: true, message: '请输入考试描述', trigger: 'blur' }],
  duration: [{ required: true, message: '请设定考试时长', trigger: 'blur' }],
  totalScore: [{ required: true, message: '请设定考试总分', trigger: 'blur' }],
  examDate: [{ required: true, message: '请选择考试日期', trigger: 'blur' }],
  targetClasses: [{ required: true, message: '请选择目标班级', trigger: 'blur' }]
};

// 表单引用
const examForm = ref(null);
const questionPlanForm = ref(null);
const chapterPlanForm = ref(null);

// 生成状态
const generating = ref(false);

// 计算题目总数
const totalQuestions = computed(() => {
  return Object.values(examData.questionPlan).reduce((sum, type) => sum + type.count, 0);
});

// 计算实际总分
const calculatedTotalScore = computed(() => {
  return Object.values(examData.questionPlan).reduce((sum, type) => sum + type.count * type.scorePerQuestion, 0);
});

// 按题型分组的试题
const questionSections = computed(() => {
  const sections = [];
  
  // 如果还没有生成试题，返回空数组
  if (!examPreview.questions.length) return sections;
  
  // 单选题
  const singleChoiceQuestions = examPreview.questions.filter(q => q.type === 'SINGLE_CHOICE');
  if (singleChoiceQuestions.length > 0) {
    sections.push({
      title: '一、单选题',
      questions: singleChoiceQuestions
    });
  }
  
  // 多选题
  const multipleChoiceQuestions = examPreview.questions.filter(q => q.type === 'MULTIPLE_CHOICE');
  if (multipleChoiceQuestions.length > 0) {
    sections.push({
      title: '二、多选题',
      questions: multipleChoiceQuestions
    });
  }
  
  // 填空题
  const fillBlankQuestions = examPreview.questions.filter(q => q.type === 'FILL_IN_THE_BLANK');
  if (fillBlankQuestions.length > 0) {
    sections.push({
      title: '三、填空题',
      questions: fillBlankQuestions
    });
  }
  
  // 简答题
  const shortAnswerQuestions = examPreview.questions.filter(q => q.type === 'SHORT_ANSWER');
  if (shortAnswerQuestions.length > 0) {
    sections.push({
      title: '四、简答题',
      questions: shortAnswerQuestions
    });
  }
  
  return sections;
});

// 下一步
const nextStep = async () => {
  if (activeStep.value === 0) {
    // 验证基本信息
    if (!examForm.value) return;
    
    await examForm.value.validate((valid) => {
      if (valid) {
        activeStep.value++;
      }
    });
  } else {
    activeStep.value++;
  }
};

// 上一步
const prevStep = () => {
  activeStep.value--;
};

// 加载数据
const loadData = async () => {
  try {
    // 加载班级列表
    const classResponse = await api.classAdminJ.getAll();
    if (classResponse && Array.isArray(classResponse)) {
      classList.value = classResponse;
    } else {
      // 尝试新的API格式
      classList.value = classResponse?.data || [];
    }
    
    console.log("获取到的班级列表:", classList.value);
    
    // 加载科目列表
    const subjectsResponse = await api.subjectAdminJ.getAll();
    if (subjectsResponse && subjectsResponse.success) {
      subjectList.value = subjectsResponse.subjects || [];
    } else {
      // 尝试新的API格式
      subjectList.value = Array.isArray(subjectsResponse) ? subjectsResponse : [];
    }
  } catch (error) {
    console.error('加载数据失败:', error);
    ElMessage.error('加载数据失败，请刷新页面重试');
    
    // 如果API调用失败，添加一些模拟数据方便开发测试
    classList.value = [
      { id: 1, name: '计算机科学1班' },
      { id: 2, name: '计算机科学2班' },
      { id: 3, name: '软件工程1班' }
    ];
    
    subjectList.value = [
      { id: 1, name: '高等数学' },
      { id: 2, name: '线性代数' },
      { id: 3, name: '概率论与数理统计' }
    ];
  }
};

// 加载章节
const loadChapters = async () => {
  if (!examData.subjectId) {
    chapterList.value = [];
    examData.chapterIds = [];
    return;
  }
  
  try {
    const response = await api.chaptersJ.getChaptersBySubject(examData.subjectId);
    
    if (response && Array.isArray(response)) {
      chapterList.value = response;
      examData.chapterIds = [];
    } else {
      throw new Error('获取章节列表数据格式异常');
    }
  } catch (error) {
    console.error('加载章节失败:', error);
    ElMessage.error('加载章节失败，请重试');
    
    // 如果API调用失败，添加一些模拟数据方便开发测试
    chapterList.value = [
      { id: 101, name: '第一章：函数与极限', subjectId: 1 },
      { id: 102, name: '第二章：导数与微分', subjectId: 1 },
      { id: 103, name: '第三章：微分中值定理与导数应用', subjectId: 1 },
      { id: 104, name: '第四章：不定积分', subjectId: 1 },
      { id: 105, name: '第五章：定积分', subjectId: 1 }
    ];
  }
};

// 更新题目总数
const updateTotalQuestions = () => {
  // 更新题目总数时重新计算总分
  updateTotalScore();
};

// 更新总分
const updateTotalScore = () => {
  // 可以在这里添加逻辑，例如自动调整分值以匹配目标总分
};

// 全选/取消全选章节
const toggleAllChapters = () => {
  if (examData.allChapters) {
    examData.chapterIds = chapterList.value.map(chapter => chapter.id);
  } else {
    examData.chapterIds = [];
  }
};

// 生成试卷
const generateExam = async () => {
  generating.value = true;
  
  try {
    // 构造请求参数
    const requestData = {
      name: examData.name,
      description: examData.description,
      duration: examData.duration,
      targetScore: examData.totalScore,
      subjectId: examData.subjectId,
      chapterIds: examData.chapterIds,
      questionPlan: examData.questionPlan,
      examRequirements: examData.examRequirements,
      chapterDistribution: examData.chapterDistribution
    };
    
    console.log('发送AI出卷请求:', requestData);
    
    // 调用AI出卷API - 使用自定义API请求方法或创建一个新方法
    const response = await axios.post('/api/ai/generate-exam', requestData, {
      baseURL: process.env.VUE_APP_JAVA_API_URL || 'http://localhost:8080',
      withCredentials: true,
      timeout: 180000 // 增加超时时间到3分钟，匹配后端设置
    });
    
    console.log('AI出卷API返回数据:', response.data);
    
    if (response.data && response.data.success) {
      // 将生成的试卷数据填充到预览对象中
      Object.assign(examPreview, response.data.exam);
      ElMessage.success('试卷生成成功！');
    } else {
      throw new Error(response.data?.message || '试卷生成失败');
    }
  } catch (error) {
    console.error('试卷生成失败:', error);
    console.error('错误详情:', error.response?.data || error.message);
    ElMessage.error(`试卷生成失败: ${error.response?.data?.message || error.message}`);
    ElMessageBox.alert(
      `生成试卷过程中出现错误，请稍后重试。
      错误信息: ${error.response?.data?.message || error.message}
      (注意：系统不再使用模拟数据填充试卷)`, 
      '生成失败', 
      { type: 'error' }
    );
    // 清空预览数据，不再使用模拟数据
    examPreview.questions = [];
  } finally {
    generating.value = false;
  }
};

// 保存题目到题库
const saveQuestionsToBank = async () => {
  if (!examPreview.questions || examPreview.questions.length === 0) {
    ElMessage.warning('没有可保存的题目');
    return;
  }

  try {
    // 构造保存请求参数
    const questionsData = examPreview.questions.map(question => ({
      subjectId: examData.subjectId,
      chapterId: examData.chapterIds && examData.chapterIds.length > 0 ? examData.chapterIds[0] : null,
      type: question.type,
      difficulty: question.difficulty || 2,
      content: question.content,
      options: question.options || [],
      answer: question.answer,
      analysis: question.analysis || '',
      tags: question.tags || []
    }));

    console.log("保存题目到题库请求数据:", questionsData);

    // 调用保存API
    const response = await axios.post('http://localhost:8080/api/ai/save-questions', questionsData, {
      withCredentials: true
    });

    if (response.data && response.data.success) {
      ElMessage.success(`成功保存 ${questionsData.length} 道题目到题库！`);
    } else {
      ElMessage.error(response.data?.message || '保存题目失败');
    }
  } catch (error) {
    console.error('保存题目失败:', error);
    ElMessage.error(`保存题目失败: ${error.response?.data?.message || error.message}`);
  }
};



// 处理导出命令
const handleExportCommand = async (command) => {
  if (!examPreview.questions || examPreview.questions.length === 0) {
    ElMessage.warning('请先生成试卷内容');
    return;
  }

  try {
    if (command === 'word') {
      await exportExamToWord();
    } else if (command === 'answerSheet') {
      // 暂时保留原有的答题卡导出功能
      const { exportAnswerSheet } = await import('@/utils/wordExport');
      const result = await exportAnswerSheet(examPreview, questionSections.value);
      ElMessage.success(result);
    }
  } catch (error) {
    console.error('导出失败:', error);
    ElMessage.error('导出失败');
  }
};

// 导出试卷为Word文档（使用后端API）
const exportExamToWord = async () => {
  try {
    // 构造导出请求数据
    const exportData = {
      name: examData.name,
      description: examData.description,
      duration: examData.duration,
      totalScore: examData.totalScore,
      questions: examPreview.questions
    };

    console.log('开始导出AI试卷Word文档:', exportData);

    // 调用后端API导出Word
    const response = await api.aiJ.exportExamToWord(exportData);

    // 创建下载链接
    const blob = new Blob([response], {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;

    // 设置文件名
    const fileName = `${examData.name || 'AI试卷'}.docx`;
    link.download = fileName;

    // 触发下载
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    ElMessage.success('试卷导出成功！');
  } catch (error) {
    console.error('导出试卷失败:', error);
    ElMessage.error('导出试卷失败: ' + (error.response?.data?.message || error.message));
  }
};

// 组件挂载时加载数据
onMounted(() => {
  loadData();
});
</script>

<style scoped>
.ai-exam-generation {
  padding: 20px;
}

.page-title {
  margin-bottom: 20px;
}

.generation-card {
  margin-bottom: 20px;
}

.step-content {
  margin: 30px 0;
  min-height: 300px;
}

.step-actions {
  display: flex;
  justify-content: flex-end;
}

.question-distribution {
  margin: 15px 0;
}

.question-type-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.question-type-controls {
  display: flex;
  align-items: center;
  gap: 5px;
}

.chapter-selection {
  margin: 15px 0;
}

.chapter-list {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.summary {
  margin-top: 15px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.warning {
  color: #E6A23C;
}

.preview-placeholder {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  text-align: center;
}

.generating-spinner {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.exam-header-preview {
  text-align: center;
  margin-bottom: 20px;
}

.exam-info-preview {
  display: flex;
  justify-content: center;
  gap: 20px;
  color: #606266;
}

.question-section {
  margin-bottom: 30px;
}

.preview-question-item {
  margin-bottom: 20px;
  padding: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.preview-question-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.preview-question-number {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background-color: #409eff;
  color: white;
  border-radius: 50%;
  margin-right: 10px;
}

.preview-question-score {
  color: #f56c6c;
}

.preview-options {
  margin-top: 10px;
  padding-left: 20px;
}

.preview-option {
  margin-bottom: 5px;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.tip {
  margin-top: 10px;
  color: #909399;
  font-size: 14px;
}

.start-generation {
  text-align: center;
}

/* 选择框样式优化 */
.el-select {
  width: 100%;
  min-width: 200px;
}
</style> 