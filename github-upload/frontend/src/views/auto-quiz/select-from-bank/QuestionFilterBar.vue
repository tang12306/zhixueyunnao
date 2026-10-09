<template>
  <el-form :inline="true" :model="filters" class="filter-form" @submit.prevent="emitSearch">
    <el-form-item label="学科">
      <el-select
        v-model="filters.subjectId"
        placeholder="选择学科"
        clearable
        :loading="loadingSubjects"
        style="width: 200px;"
        @change="handleSubjectChange"
      >
        <el-option v-for="item in subjects" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="章节">
      <el-select
        v-model="filters.chapterId"
        placeholder="选择章节"
        clearable
        :disabled="!filters.subjectId || chapters.length === 0"
        style="width: 200px;"
      >
        <el-option v-for="item in chapters" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="题型">
      <el-select v-model="filters.type" placeholder="选择题型" clearable style="width: 150px;">
        <el-option v-for="type in QUESTION_TYPES" :key="type.value" :label="type.label" :value="type.value" />
      </el-select>
    </el-form-item>
    <el-form-item label="难度">
      <el-rate v-model="filters.difficulty" :max="5" clearable />
    </el-form-item>
    <el-form-item label="关键词">
      <el-input v-model="filters.keyword" placeholder="搜索题目内容、标签" clearable />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" native-type="submit">查询题目</el-button>
      <el-button @click="resetFilters">重置筛选</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api, { errorMessage } from '@/api';
import { QUESTION_TYPES } from '@/utils/questionTypes';

const emit = defineEmits(['search']);

// 难度 0 表示不限
const EMPTY_FILTERS = { subjectId: null, chapterId: null, type: null, difficulty: 0, keyword: '' };

const filters = reactive({ ...EMPTY_FILTERS });
const subjects = ref([]);
const chapters = ref([]);
const loadingSubjects = ref(false);

// 有的接口直接返回数组，有的返回分页对象
const listOf = (response) => {
  if (Array.isArray(response)) {
    return response;
  }
  return Array.isArray(response?.content) ? response.content : [];
};

const emitSearch = () => {
  const subject = subjects.value.find(s => s.id === filters.subjectId);
  emit('search', { filters: { ...filters }, subjectName: subject ? subject.name : '' });
};

const loadChapters = async (subjectId) => {
  chapters.value = [];
  if (!subjectId) {
    return;
  }
  try {
    const list = listOf(await api.chaptersJ.getChaptersBySubject(subjectId));
    // 等待期间可能又换了学科
    if (filters.subjectId === subjectId) {
      chapters.value = list;
    }
  } catch (error) {
    console.error('获取章节列表失败:', error);
    ElMessage.error(errorMessage(error, '获取章节列表失败'));
  }
};

// 换学科立即重新查询；其它条件点“查询题目”才生效
const handleSubjectChange = (subjectId) => {
  filters.chapterId = null;
  loadChapters(subjectId);
  emitSearch();
};

// 只重置筛选条件，已勾选的题目保留（右侧有“清空已选”）
const resetFilters = () => {
  Object.assign(filters, EMPTY_FILTERS);
  chapters.value = [];
  emitSearch();
};

onMounted(async () => {
  loadingSubjects.value = true;
  try {
    subjects.value = listOf(await api.subjectsJ.getAllSubjects());
  } catch (error) {
    console.error('获取学科列表失败:', error);
    ElMessage.error(errorMessage(error, '获取学科列表失败'));
  } finally {
    loadingSubjects.value = false;
  }
});
</script>

<style scoped>
.filter-form .el-form-item {
  margin-bottom: 10px;
  margin-right: 15px;
}
</style>
