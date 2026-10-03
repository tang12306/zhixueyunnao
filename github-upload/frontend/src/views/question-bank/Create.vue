<template>
  <div class="question-create-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑题目' : '创建题目' }}</h2>
      <el-button @click="goBack">返回题库</el-button>
    </div>

    <el-card class="form-card">
      <el-form 
        ref="questionFormRef"
        :model="questionForm" 
        :rules="rules"
        label-width="100px"
        label-position="top"
        @submit.prevent
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="questionForm.subjectId" placeholder="选择科目" style="width: 100%" @change="handleSubjectChange">
                <el-option v-for="subject in subjectList" :key="subject.id" :label="subject.name" :value="subject.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="章节" prop="chapterId">
              <el-select 
                v-model="questionForm.chapterId" 
                placeholder="选择章节" 
                style="width: 100%"
                :disabled="!questionForm.subjectId || chapterList.length === 0"
              >
                <el-option v-for="chapter in chapterList" :key="chapter.id" :label="chapter.name" :value="chapter.id"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="题型" prop="type">
              <el-select 
                v-model="questionForm.type" 
                placeholder="选择题型"
                style="width: 100%"
                @change="handleTypeChange"
              >
                <el-option label="单选题" value="单选题"></el-option>
                <el-option label="多选题" value="多选题"></el-option>
                <el-option label="判断题" value="判断题"></el-option>
                <el-option label="填空题" value="填空题"></el-option>
                <el-option label="简答题" value="简答题"></el-option>
                <el-option label="作文题" value="作文题"></el-option>
                <el-option label="阅读理解" value="阅读理解"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="难度" prop="difficulty">
              <el-rate
                v-model="questionForm.difficulty"
                :max="5"
                show-text
                :texts="['非常简单', '简单', '中等', '困难', '非常困难']"
                style="width: 100%"
              ></el-rate>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="题目内容" prop="content">
          <el-input
            v-model="questionForm.content"
            type="textarea"
            :rows="5"
            placeholder="输入题目内容"
          ></el-input>
        </el-form-item>

        <!-- 选择题选项 -->
        <template v-if="['单选题', '多选题', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(questionForm.type)">
          <el-form-item label="选项" prop="options">
            <div v-for="(option, index) in questionForm.options" :key="index" class="option-item">
              <el-input
                v-model="option.text"
                placeholder="选项内容"
                class="option-input"
              ></el-input>
              <el-checkbox
                v-model="option.isCorrect"
                :label="true"
                border
                class="option-checkbox"
              >
                正确答案
              </el-checkbox>
              <el-button
                type="danger"
                circle
                icon="el-icon-delete"
                @click="removeOption(index)"
                size="small"
                class="option-delete"
              ></el-button>
            </div>
            <el-button type="primary" @click="addOption" size="small" plain>添加选项</el-button>
          </el-form-item>
        </template>

        <!-- 判断题答案 -->
        <template v-else-if="['判断题', 'TRUE_FALSE'].includes(questionForm.type)">
          <el-form-item label="答案" prop="answer">
            <el-radio-group v-model="questionForm.answer">
              <el-radio label="正确">正确</el-radio>
              <el-radio label="错误">错误</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>

        <!-- 其他题型答案 -->
        <template v-else>
          <el-form-item label="答案" prop="answer">
            <el-input
              v-model="questionForm.answer"
              type="textarea"
              :rows="3"
              placeholder="输入题目答案"
            ></el-input>
          </el-form-item>
        </template>

        <el-form-item label="解析" prop="analysis">
          <el-input
            v-model="questionForm.analysis"
            type="textarea"
            :rows="3"
            placeholder="输入解题思路和解析（可选）"
          ></el-input>
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <el-tag
            v-for="tag in questionForm.tags"
            :key="tag"
            closable
            @close="removeTag(tag)"
            style="margin-right: 10px; margin-bottom: 10px;"
          >
            {{ tag }}
          </el-tag>
          <el-input
            v-if="inputTagVisible"
            ref="tagInputRef"
            v-model="inputTagValue"
            class="tag-input"
            size="small"
            @keyup.enter="confirmTag"
            @blur="confirmTag"
          ></el-input>
          <el-button v-else size="small" @click="showTagInput">+ 添加标签</el-button>
          <div class="tag-hint">输入标签后按回车添加，多个标签用于分类和搜索</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">{{ isEdit ? '保存修改' : '创建题目' }}</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../../api';
import { processQuestionOptions } from '@/utils/errorHandler';

const router = useRouter();
const route = useRoute();
const questionForm = reactive({
  subjectId: null,
  chapterId: null,
  type: '',
  difficulty: 3,
  content: '',
  options: [],
  answer: '',
  analysis: '',
  tags: []
});

const subjectList = ref([]);
const chapterList = ref([]);

// 处理科目变更
const handleSubjectChange = (selectedValue) => {
  console.log('[Event Triggered] Subject selected in @change event, selectedValue:', selectedValue);
  console.log('[Before Manual Assignment] questionForm.subjectId was:', questionForm.subjectId);
  
  questionForm.subjectId = selectedValue; // 显式赋值
  questionForm.chapterId = null; // 清空章节选择
  
  // 加载所选科目的章节
  loadChapters(selectedValue);

  nextTick(() => {
    console.log('[After Manual Assignment] questionForm.subjectId in nextTick is now:', questionForm.subjectId);
  });
};

// 加载章节数据
const loadChapters = async (subjectId) => {
  if (!subjectId) {
    chapterList.value = [];
    return;
  }
  
  try {
    const response = await api.chapterAdminJ.getChaptersBySubjectId(subjectId);
    chapterList.value = response || [];
    console.log('Loaded chapters for subject:', JSON.parse(JSON.stringify(chapterList.value)));
    
    if (chapterList.value.length === 0) {
      ElMessage.warning('当前科目暂无章节，请先在系统管理中添加章节。');
    }
  } catch (error) {
    ElMessage.error('获取章节列表失败');
    console.error("Error fetching chapters:", error);
    chapterList.value = [];
  }
};

const rules = {
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  content: [{ required: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
};

const questionFormRef = ref(null);
const submitting = ref(false);
const inputTagVisible = ref(false);
const inputTagValue = ref('');
const tagInputRef = ref(null);

// 判断是编辑还是创建
const isEdit = computed(() => {
  return !!route.params.id;
});

// 添加选项
const addOption = () => {
  questionForm.options.push({
    text: '',
    isCorrect: false
  });
};

// 移除选项
const removeOption = (index) => {
  questionForm.options.splice(index, 1);
};

// 题型变更处理
const handleTypeChange = (value) => {
  // 根据题型初始化相关字段
  if (['单选题', '多选题', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(value)) {
    if (questionForm.options.length === 0) {
      // 初始化4个选项
      for (let i = 0; i < 4; i++) {
        addOption();
      }
    }
  } else if (['判断题', 'TRUE_FALSE'].includes(value)) {
    questionForm.options = [];
    questionForm.answer = '正确'; // 默认值
  } else {
    questionForm.options = [];
  }
};

// 显示标签输入框
const showTagInput = () => {
  inputTagVisible.value = true;
  nextTick(() => {
    tagInputRef.value.focus();
  });
};

// 确认添加标签
const confirmTag = () => {
  if (inputTagValue.value) {
    if (!questionForm.tags.includes(inputTagValue.value)) {
      questionForm.tags.push(inputTagValue.value);
    }
  }
  inputTagVisible.value = false;
  inputTagValue.value = '';
};

// 移除标签
const removeTag = (tag) => {
  const index = questionForm.tags.indexOf(tag);
  if (index !== -1) {
    questionForm.tags.splice(index, 1);
  }
};

// 加载科目列表
const loadSubjects = async () => {
  try {
    const response = await api.subjectAdminJ.getAll();
    subjectList.value = response || [];
    // 打印加载到的科目列表，确保ID和Name是正确的，特别是ID的类型
    console.log('Loaded subjects for form:', JSON.parse(JSON.stringify(subjectList.value))); 
    if (subjectList.value.length === 0) {
      ElMessage.warning('系统中暂无科目，请先在系统管理中添加科目。');
    }
  } catch (error) {
    ElMessage.error('获取科目列表失败，请检查网络或联系管理员。');
    console.error("Error fetching subjects for form:", error);
  }
};

// 加载题目详情 (如果是编辑模式)
const loadQuestionDetail = async (id) => {
  try {
    submitting.value = true; // 可以用一个通用的loading状态
    const response = await api.questionsJ.getQuestionById(id); 
    if (response) {
      // 将后端返回的 subject (可能是对象或字符串) 转换为 subjectId
      // 假设 response.subject 是一个对象 { id: ..., name: ...}
      // 或者如果后端直接返回 subjectId，则直接使用
      // 如果 response.subject 是科目名称，需要根据 subjectList 查找对应的 id
      
      Object.assign(questionForm, response); // 先批量赋值
      
      // 特殊处理 subjectId
      if (response.subject && typeof response.subject === 'object' && response.subject.id) {
        questionForm.subjectId = response.subject.id;
      } else if (response.subjectId) {
        questionForm.subjectId = response.subjectId;
      } else if (typeof response.subject === 'string' && subjectList.value.length > 0) {
        // 如果返回的是科目名称，并且科目列表已加载
        const foundSubject = subjectList.value.find(s => s.name === response.subject);
        if (foundSubject) {
          questionForm.subjectId = foundSubject.id;
        }
      }
      
      // 特殊处理 chapterId
      if (response.chapter && response.chapter.id) {
        questionForm.chapterId = response.chapter.id;
        // 确保章节列表已加载
        if (questionForm.subjectId) {
          await loadChapters(questionForm.subjectId);
        }
      }
      
      // 将后端枚举类型转换为中文类型
      if (questionForm.type) {
        questionForm.type = mapEnumTypeToChineseType(questionForm.type);
      }

      // 使用工具函数处理选项数据
      const processedData = processQuestionOptions(questionForm);
      if (processedData.options) {
        questionForm.options = processedData.options;
      } else {
        questionForm.options = [];
      }

      // 如果是选择题但没有选项，初始化选项
      if (['单选题', '多选题', 'SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(questionForm.type) &&
          questionForm.options.length === 0) {
        for (let i = 0; i < 4; i++) {
          addOption();
        }
      }

    } else {
      ElMessage.error('加载题目详情失败');
    }
  } catch (error) {
    ElMessage.error('加载题目详情出错: ' + error.message);
    console.error("Error loading question detail:", error);
  } finally {
    submitting.value = false;
  }
};

// 处理选项的答案组合，生成最终答案字符串
const prepareAnswerFromOptions = () => {
  if (['单选题', '多选题'].includes(questionForm.type)) {
    // 将所有正确选项的索引（使用A、B、C...表示）组合为答案
    const correctIndices = questionForm.options
      .map((option, index) => option.isCorrect ? String.fromCharCode(65 + index) : null)
      .filter(index => index !== null);
    
    return correctIndices.join(',');
  }
  return questionForm.answer;
};

// 提交表单
const submitForm = async () => {
  if (!questionFormRef.value) return;
  await questionFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      try {
        // 准备选项列表 (如果是选择题)
        const optionsList = ['单选题', '多选题'].includes(questionForm.type)
          ? questionForm.options.map(o => o.text)
          : [];

        // 处理答案 (选择题需要从选项中提取正确答案)
        const finalAnswer = prepareAnswerFromOptions();
        
        // 准备Java后端接受的数据格式
        const javaPayload = {
          id: isEdit.value ? parseInt(route.params.id) : null,
          subjectId: questionForm.subjectId,
          chapterId: questionForm.chapterId,
          type: mapChineseTypeToEnumType(questionForm.type),
          difficulty: questionForm.difficulty,
          content: questionForm.content,
          options: optionsList,
          answer: finalAnswer,
          analysis: questionForm.analysis || '',
          tags: questionForm.tags || []
        };
        
        // 发送到Java后端
        if (isEdit.value) {
          await api.questionsJ.updateQuestion(route.params.id, javaPayload);
          ElMessage.success('题目更新成功');
        } else {
          await api.questionsJ.createQuestion(javaPayload);
          ElMessage.success('题目创建成功');
        }
        
        // 返回题库列表
        router.push('/question-bank');
      } catch (error) {
        ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message));
        console.error("Error submitting form:", error);
      } finally {
        submitting.value = false;
      }
    } else {
      ElMessage.warning('请完善表单信息');
    }
  });
};

// 将中文题型映射为后端枚举类型
const mapChineseTypeToEnumType = (chineseType) => {
  const typeMap = {
    '单选题': 'SINGLE_CHOICE',
    '多选题': 'MULTIPLE_CHOICE',
    '判断题': 'TRUE_FALSE',
    '填空题': 'FILL_BLANK',
    '简答题': 'SHORT_ANSWER',
    '作文题': 'ESSAY',
    '阅读理解': 'READING'
  };
  return typeMap[chineseType] || chineseType;
};

// 将后端枚举类型映射为中文题型
const mapEnumTypeToChineseType = (enumType) => {
  const typeMap = {
    'SINGLE_CHOICE': '单选题',
    'MULTIPLE_CHOICE': '多选题',
    'TRUE_FALSE': '判断题',
    'FILL_BLANK': '填空题',
    'SHORT_ANSWER': '简答题',
    'ESSAY': '作文题',
    'READING': '阅读理解'
  };
  return typeMap[enumType] || enumType;
};

// 重置表单
const resetForm = () => {
  if (questionFormRef.value) {
    questionFormRef.value.resetFields();
    
    // 如果是编辑模式，重新加载原始数据
    if (isEdit.value) {
      loadQuestionDetail(route.params.id);
    } else {
      questionForm.options = [];
      questionForm.tags = [];
    }
  }
};

// 返回题库列表
const goBack = () => {
  router.push('/question-bank');
};

// 添加API客户端
onMounted(() => {
  if (!api.questionsJ) {
    // 添加Java题目API
    api.questionsJ = {
      queryQuestions: (params) => api.javaApi.get('/questions/api/query', { params }),
      getQuestionById: (id) => api.javaApi.get(`/questions/${id}`),
      createQuestion: (data) => api.javaApi.post('/questions', data),
      updateQuestion: (id, data) => api.javaApi.put(`/questions/${id}`, data),
      deleteQuestion: (id) => api.javaApi.delete(`/questions/${id}`)
    };
  }
  
  loadSubjects();
  if (isEdit.value) {
    loadQuestionDetail(route.params.id);
  }
});

// 监听subjectId变化，自动加载章节
watch(() => questionForm.subjectId, (newVal) => {
  if (newVal) {
    loadChapters(newVal);
  } else {
    chapterList.value = [];
  }
});
</script>

<style scoped>
.question-create-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.form-card {
  margin-bottom: 20px;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.option-input {
  flex: 1;
  margin-right: 10px;
}

.option-checkbox {
  margin-right: 10px;
}

.tag-input {
  width: 120px;
  vertical-align: bottom;
  margin-right: 10px;
}

.tag-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style> 