<template>
  <div class="ai-create-question-container">
    <el-page-header @back="goBack" content="智能AI出题 Pro" class="page-header-custom">
    </el-page-header>

    <el-row :gutter="20">
      <!-- 左侧：参数配置 -->
      <el-col :span="8">
        <el-card class="box-card config-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><Setting /></el-icon> 出题参数配置</span>
            </div>
          </template>
          <el-form :model="form" label-position="top" ref="formRef" class="config-form">
            <el-form-item label="选择学科" prop="subjectId" :rules="{ required: true, message: '请选择学科', trigger: 'change' }">
              <el-select v-model="form.subjectId" placeholder="请选择学科" @change="handleSubjectChange" style="width: 100%;">
                <el-option v-for="item in subjects" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="选择章节 (可多选)" prop="chapterIds">
              <el-select v-model="form.chapterIds" multiple placeholder="选择章节 (可选)" :disabled="!form.subjectId || chapters.length === 0" style="width: 100%;" filterable>
                <el-option v-for="item in chapters" :key="item.id" :label="item.name" :value="item.id"></el-option>
              </el-select>
            </el-form-item>

            <el-row :gutter="10">
              <el-col :span="12">
                <el-form-item label="题型" prop="type" :rules="{ required: true, message: '请选择题型', trigger: 'change' }">
                  <el-select v-model="form.type" placeholder="选择题型" style="width: 100%;">
                    <el-option label="单选题" value="单选题"></el-option>
                    <el-option label="多选题" value="多选题"></el-option>
                    <el-option label="判断题" value="判断题"></el-option>
                    <el-option label="填空题" value="填空题"></el-option>
                    <el-option label="简答题" value="简答题"></el-option>
                    <!-- 更多题型 -->
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="难度 (1-5)" prop="difficulty">
                  <el-rate v-model="form.difficulty" :max="5" :texts="['简单', '较易', '中等', '较难', '困难']" show-text style="margin-top:5px"/>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="题目数量" prop="count" :rules="{ required: true, type: 'number', min:1, message: '请输入有效的题目数量', trigger: 'blur' }">
              <el-input-number v-model="form.count" :min="1" :max="50" label="题目数量"></el-input-number>
            </el-form-item>

            <el-form-item label="自定义AI出题要求 (可选)" prop="customPrompt">
              <el-input type="textarea" :rows="4" v-model="form.customPrompt" placeholder="例如：题目请结合实际应用场景，或者考察某个具体知识点的理解深度。"></el-input>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" icon="MagicStick" @click="submitGenerationTask" :loading="generatingLoading" style="width: 100%; padding: 15px;">
                <el-icon style="vertical-align: middle"><MagicStick /></el-icon>
                <span style="vertical-align: middle"> 开始智能出题</span>
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：题目预览与编辑 -->
      <el-col :span="16">
        <el-card class="box-card results-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span><el-icon><List /></el-icon> 生成结果预览与编辑</span>
              <el-button 
                type="success" 
                icon="DocumentChecked" 
                @click="saveAllGeneratedQuestions" 
                :loading="savingLoading" 
                :disabled="!generatedQuestions || generatedQuestions.length === 0"
                v-if="generatedQuestions && generatedQuestions.length > 0"
              >
                全部保存至题库
              </el-button>
            </div>
          </template>

          <div v-if="generatingLoading" class="loading-placeholder">
            <el-icon class="is-loading" :size="50"><Loading /></el-icon>
            <p>AI正在努力出题中，请稍候...</p>
          </div>
          
          <el-empty 
            description="暂未生成题目，请在左侧配置参数后开始出题." 
            v-if="!generatingLoading && (!generatedQuestions || generatedQuestions.length === 0)"
            :image-size="150"
          />

          <div v-if="!generatingLoading && generatedQuestions && generatedQuestions.length > 0" class="questions-editor-list">
            <el-scrollbar max-height="calc(100vh - 280px)"> 
              <div v-for="(question, index) in generatedQuestions" :key="question.tempId || index" class="question-edit-item">
                <el-card shadow="hover" class="question-card-item">
                  <template #header>
                    <div class="card-header">
                      <span>题目 {{ index + 1 }} (AI建议分数: {{ question.originalScore || 'N/A' }})</span>
                      <el-button type="danger" icon="Delete" circle @click="removeQuestion(index)" />
                    </div>
                  </template>
                  
                  <el-form label-position="top">
                    <el-form-item label="题型">
                      <el-select v-model="question.type" placeholder="选择题型" style="width: 100%;">
                        <el-option label="单选题" value="单选题"></el-option>
                        <el-option label="多选题" value="多选题"></el-option>
                        <el-option label="判断题" value="判断题"></el-option>
                        <el-option label="填空题" value="填空题"></el-option>
                        <el-option label="简答题" value="简答题"></el-option>
                      </el-select>
                    </el-form-item>
                    <el-form-item label="题目内容 (Markdown支持)">
                      <el-input type="textarea" :rows="3" v-model="question.content" placeholder="题目内容" />
                    </el-form-item>
                    
                    <div v-if="isChoiceQuestion(question.type)">
                      <el-form-item label="选项 (每行一个选项)">
                        <el-input type="textarea" :rows="question.options ? question.options.length + 1 : 3" v-model="question.optionsString" @input="(val) => updateQuestionOptions(question, val)" placeholder="例如：\nA. 选项一\nB. 选项二" />
                      </el-form-item>
                      <el-form-item label="正确答案">
                        <el-input v-model="question.answer" placeholder="例如：A 或 A,B" />
                      </el-form-item>
                    </div>
                    <div v-else-if="question.type === '填空题'"> 
                        <el-form-item label="参考答案 (多个答案用 ; 分隔)">
                            <el-input type="textarea" :rows="2" v-model="question.answer" placeholder="例如：答案1;答案2" />
                        </el-form-item>
                    </div>
                     <div v-else> 
                        <el-form-item label="参考答案">
                            <el-input type="textarea" :rows="3" v-model="question.answer" placeholder="请输入参考答案" />
                        </el-form-item>
                    </div>

                    <el-form-item label="题目解析">
                      <el-input type="textarea" :rows="2" v-model="question.analysis" placeholder="题目解析" />
                    </el-form-item>
                    <el-form-item label="设置分数">
                       <el-input-number v-model="question.score" :min="0" :max="100" placeholder="分数" />
                    </el-form-item>
                  </el-form>
                </el-card>
              </div>
            </el-scrollbar>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Setting, List, MagicStick, Loading, DocumentChecked, Delete } from '@element-plus/icons-vue';
import api from '@/api/index.js'; 
import { v4 as uuidv4 } from 'uuid'; // 用于临时ID

const router = useRouter();

// --- Refs and Reactive Variables ---
const formRef = ref(null);
const form = reactive({
  subjectId: null,
  chapterIds: [], // 支持多选章节
  type: '单选题',
  difficulty: 3,
  count: 3, // 默认生成3条方便测试
  customPrompt: '',
});

const subjects = ref([]);
const chapters = ref([]);
const generatedQuestions = ref([]); // Store AI-generated questions for editing

const generatingLoading = ref(false);
const savingLoading = ref(false);

// --- Lifecycle Hooks ---
onMounted(() => {
  fetchSubjects();
});

// --- API Calls & Logic ---
const fetchSubjects = async () => {
  try {
    // const response = await api.subjectsJ.getAllSubjects(); // Old API call
    const response = await api.subjectAdminJ.getAll(); // Use the new admin API for dynamic subjects
    subjects.value = response || [];
  } catch (error) {
    ElMessage.error('获取学科列表失败');
    console.error("Error fetching subjects:", error);
  }
};

const handleSubjectChange = async (subjectId) => {
  form.chapterIds = []; // Reset chapter selection
  chapters.value = [];
  if (subjectId) {
    try {
      const response = await api.chaptersJ.getChaptersBySubject(subjectId);
      chapters.value = response || [];
    } catch (error) {
      ElMessage.error('获取章节列表失败');
      console.error("Error fetching chapters:", error);
    }
  }
};

const submitGenerationTask = async () => {
  if (!formRef.value) return;
  formRef.value.validate(async (valid) => {
    if (valid) {
      generatingLoading.value = true;
      generatedQuestions.value = []; // Clear previous results
      try {
        const requestPayload = {
          subjectId: form.subjectId,
          chapterIds: form.chapterIds && form.chapterIds.length > 0 ? form.chapterIds : null,
          type: form.type,
          difficulty: form.difficulty,
          count: form.count,
          customPrompt: form.customPrompt || null,
        };
        
        const response = await api.aiJ.generateBatchQuestions(requestPayload); // Use the new batch API

        if (response && response.success && Array.isArray(response.questions)) {
          generatedQuestions.value = response.questions.map(q => ({
            ...q,
            tempId: uuidv4(), // Assign a temporary unique ID for v-for key and editing tracking
            originalScore: q.score, // Store AI suggested score separately if needed for comparison
            score: q.score || 5, // Default score if AI doesn't provide, or use AI's score
            // 确保题目类型正确设置，如果AI没有返回类型，使用表单中选择的类型
            type: q.type || form.type,
            // For choice questions, transform options array to a newline-separated string for textarea editing
            optionsString: Array.isArray(q.options) ? q.options.join('\n') : '',
          }));
          ElMessage.success(`成功生成 ${response.questions.length} 道题目! 请预览和编辑。`);
        } else {
          ElMessage.error(response.message || 'AI生成题目失败，未返回有效数据。');
        }
      } catch (error) {
        ElMessage.error('请求AI服务失败: ' + (error.response?.data?.message || error.message));
        console.error("Error generating questions:", error);
      } finally {
        generatingLoading.value = false;
      }
    }
  });
};

const isChoiceQuestion = (type) => {
  return ['单选题', '多选题'].includes(type);
};

const updateQuestionOptions = (question, optionsString) => {
  question.options = optionsString.split('\n').map(opt => opt.trim()).filter(opt => opt); // Split by newline and remove empty options
};

const removeQuestion = (index) => {
  generatedQuestions.value.splice(index, 1);
  ElMessage.info('已移除一道题目');
};

const saveAllGeneratedQuestions = async () => {
  if (!generatedQuestions.value || generatedQuestions.value.length === 0) {
    ElMessage.warning('没有可保存的题目。');
    return;
  }
  savingLoading.value = true;
  try {
    const questionsToSave = generatedQuestions.value.map(q => ({
      subjectId: form.subjectId,
      chapterId: q.chapterId, // This needs to be handled if AI returns chapter specific info or if we need to pick one from form.chapterIds
      type: q.type || form.type, // 确保题目类型不为空，如果题目没有类型，使用表单中选择的类型
      difficulty: q.difficulty || form.difficulty, // Use question's own difficulty if edited, else form's
      content: q.content,
      options: q.options, // Already an array due to updateQuestionOptions or from AI
      answer: q.answer,
      analysis: q.analysis,
      score: q.score,
      // tags: q.tags, // If AI provides tags and QuestionDTO supports it
    }));

    // A potential issue: if form.chapterIds has multiple IDs, 
    // which chapterId should be associated with each question when saving?
    // Current DTO for save-questions seems to imply a single chapterId per question or none.
    // For now, I'm setting q.chapterId to null, assuming it will be handled or not required by the backend
    // OR that AI needs to return chapterId with each question if it was chapter-specific.
    // A simpler approach might be to associate all questions with the FIRST chapterId if multiple are selected in the form.
    if (form.chapterIds && form.chapterIds.length > 0) {
        questionsToSave.forEach(q => {
            if (!q.chapterId) { // If AI didn't specify a chapter for the question
                q.chapterId = form.chapterIds[0]; // Default to the first selected chapter
            }
        });
    }
    
    const response = await api.aiJ.saveGeneratedQuestions(questionsToSave);
    if (response && response.success) {
      ElMessage.success(response.message || `成功保存 ${response.count || questionsToSave.length} 道题目!`);
      generatedQuestions.value = []; // Clear after saving
    } else {
      ElMessage.error(response.message || '保存题目失败。');
    }
  } catch (error) {
    ElMessage.error('保存题目请求失败: ' + (error.response?.data?.message || error.message));
    console.error("Error saving questions:", error);
  } finally {
    savingLoading.value = false;
  }
};

const goBack = () => {
  router.back();
};

</script>

<style scoped>
.ai-create-question-container {
  padding: 0px 20px 20px 20px; /* Reduce top padding as page-header has its own */
}

.page-header-custom {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 16px 20px;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.config-card {
  height: calc(100vh - 120px); /* Adjust based on your header/footer height */
  overflow-y: auto;
}

.results-card {
  height: calc(100vh - 120px);
}

.questions-editor-list {
  /* max-height: calc(100vh - 280px); /* Deduct header, card-header, buttons etc. */
  /* overflow-y: auto; */ /* Moved to el-scrollbar */
}

.question-edit-item {
  margin-bottom: 15px;
}

.question-card-item .el-card__header {
  padding: 10px 15px; /* More compact header for question cards */
}

.question-card-item .el-form-item {
  margin-bottom: 10px; /* Compact form items within question cards */
}

.loading-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #909399;
}

.config-form .el-form-item {
  margin-bottom: 18px; /* Slightly more space for config form */
}
</style> 