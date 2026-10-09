<template>
  <div class="question-create-container">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑题目' : '创建题目' }}</h2>
      <el-button @click="goBack">返回题库</el-button>
    </div>

    <el-card class="form-card" v-loading="loading">
      <el-form
        ref="questionFormRef"
        :model="questionForm"
        :rules="rules"
        label-position="top"
        @submit.prevent
      >
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="科目" prop="subjectId">
              <el-select v-model="questionForm.subjectId" placeholder="选择科目" style="width: 100%" @change="handleSubjectChange">
                <el-option v-for="subject in subjectList" :key="subject.id" :label="subject.name" :value="subject.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="章节" prop="chapterId">
              <el-select
                v-model="questionForm.chapterId"
                placeholder="选择章节（可选）"
                clearable
                style="width: 100%"
                :disabled="!questionForm.subjectId || chapterList.length === 0"
              >
                <el-option v-for="chapter in chapterList" :key="chapter.id" :label="chapter.name" :value="chapter.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="题型" prop="type">
              <el-select v-model="questionForm.type" placeholder="选择题型" style="width: 100%" @change="handleTypeChange">
                <el-option v-for="type in QUESTION_TYPES" :key="type.value" :label="type.label" :value="type.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="难度" prop="difficulty">
          <el-rate v-model="questionForm.difficulty" :max="5" show-text :texts="DIFFICULTY_TEXTS" />
        </el-form-item>

        <el-form-item label="题目内容" prop="content">
          <el-input v-model="questionForm.content" type="textarea" :rows="5" placeholder="输入题目内容" />
        </el-form-item>

        <el-form-item v-if="isChoiceType(questionForm.type)" label="选项（勾选正确答案）" prop="options">
          <OptionsEditor v-model="questionForm.options" :single="questionForm.type === 'SINGLE_CHOICE'" />
        </el-form-item>

        <el-form-item v-else-if="questionForm.type === 'TRUE_FALSE'" label="答案" prop="answer">
          <el-radio-group v-model="questionForm.answer">
            <el-radio v-for="answer in TRUE_FALSE_ANSWERS" :key="answer" :value="answer">{{ answer }}</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-else label="答案" prop="answer">
          <el-input v-model="questionForm.answer" type="textarea" :rows="3" placeholder="输入题目答案" />
        </el-form-item>

        <el-form-item label="解析" prop="analysis">
          <el-input v-model="questionForm.analysis" type="textarea" :rows="3" placeholder="输入解题思路和解析（可选）" />
        </el-form-item>

        <el-form-item label="标签" prop="tags">
          <TagEditor v-model="questionForm.tags" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitForm">{{ isEdit ? '保存修改' : '创建题目' }}</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import api, { errorMessage } from '@/api';
import { QUESTION_TYPES, isChoiceType } from '@/utils/questionTypes';
import OptionsEditor from './question-form/OptionsEditor.vue';
import TagEditor from './question-form/TagEditor.vue';
import {
  TRUE_FALSE_ANSWERS,
  emptyQuestionForm,
  formFromQuestion,
  toQuestionPayload,
  optionsError,
  adaptToType
} from './question-form/questionForm';

const DIFFICULTY_TEXTS = ['非常简单', '简单', '中等', '困难', '非常困难'];

const router = useRouter();
const route = useRoute();

const questionFormRef = ref(null);
const questionForm = reactive(emptyQuestionForm());
const subjectList = ref([]);
const chapterList = ref([]);
const loading = ref(false);
const submitting = ref(false);
// 编辑时加载到的原题，“重置”恢复成它
let originalQuestion = null;

const isEdit = computed(() => !!route.params.id);

const rules = {
  subjectId: [{ required: true, message: '请选择科目', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }],
  content: [{ required: true, whitespace: true, message: '请输入题目内容', trigger: 'blur' }],
  answer: [{ required: true, whitespace: true, message: '请输入答案', trigger: 'blur' }],
  // 边填边校验会一直提示“选项内容不能为空”，所以只在提交时检查（trigger 为空数组）
  options: [{
    validator: (rule, value, callback) => {
      const message = optionsError(questionForm.type, value);
      callback(message ? new Error(message) : undefined);
    },
    trigger: []
  }]
};

const loadChapters = async (subjectId) => {
  chapterList.value = [];
  if (!subjectId) {
    return;
  }
  try {
    const chapters = await api.chapterAdminJ.getChaptersBySubjectId(subjectId);
    // 等待期间可能又换了科目
    if (questionForm.subjectId !== subjectId) {
      return;
    }
    chapterList.value = Array.isArray(chapters) ? chapters : [];
    if (chapterList.value.length === 0) {
      ElMessage.info('当前科目暂无章节，题目将只归到科目下');
    }
  } catch (error) {
    console.error('获取章节列表失败:', error);
    ElMessage.error(errorMessage(error, '获取章节列表失败'));
  }
};

// 用户换科目和编辑时载入原题都会改 subjectId，统一在这里加载章节
watch(() => questionForm.subjectId, loadChapters);

// 修改过选项后去掉上次提交时的选项错误提示
watch(() => questionForm.options, () => questionFormRef.value?.clearValidate('options'));

const handleSubjectChange = () => {
  questionForm.chapterId = null;
};

const handleTypeChange = (type) => {
  Object.assign(questionForm, adaptToType(questionForm, type));
};

const loadSubjects = async () => {
  try {
    const subjects = await api.subjectAdminJ.getAll();
    subjectList.value = Array.isArray(subjects) ? subjects : [];
    if (subjectList.value.length === 0) {
      ElMessage.warning('系统中暂无科目，请先在系统管理中添加科目。');
    }
  } catch (error) {
    console.error('获取科目列表失败:', error);
    ElMessage.error(errorMessage(error, '获取科目列表失败'));
  }
};

const loadQuestion = async (id) => {
  try {
    originalQuestion = await api.questionsJ.getQuestionById(id);
    Object.assign(questionForm, formFromQuestion(originalQuestion, subjectList.value));
  } catch (error) {
    console.error('加载题目详情失败:', error);
    ElMessage.error(errorMessage(error, '加载题目详情失败'));
  }
};

const submitForm = async () => {
  if (!(await questionFormRef.value.validate().catch(() => false))) {
    ElMessage.warning('请完善表单信息');
    return;
  }

  submitting.value = true;
  try {
    const payload = toQuestionPayload(questionForm);
    if (isEdit.value) {
      await api.questionsJ.updateQuestion(route.params.id, payload);
      ElMessage.success('题目更新成功');
    } else {
      await api.questionsJ.createQuestion(payload);
      ElMessage.success('题目创建成功');
    }
    router.push('/question-bank');
  } catch (error) {
    console.error('保存题目失败:', error);
    ElMessage.error(errorMessage(error, '保存题目失败'));
  } finally {
    submitting.value = false;
  }
};

const resetForm = () => {
  Object.assign(questionForm, originalQuestion
    ? formFromQuestion(originalQuestion, subjectList.value)
    : emptyQuestionForm());
  questionFormRef.value?.clearValidate();
};

const goBack = () => {
  router.push('/question-bank');
};

onMounted(async () => {
  loading.value = true;
  // 原题只存了科目名称，先有科目列表才能对应到科目 ID
  await loadSubjects();
  if (isEdit.value) {
    await loadQuestion(route.params.id);
  }
  loading.value = false;
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
</style>
