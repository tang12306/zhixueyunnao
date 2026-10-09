<template>
  <div class="chapter-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>章节管理</span>
        </div>
      </template>

      <el-form :inline="true" @submit.prevent>
        <el-form-item label="选择科目" style="width: 300px;">
          <el-select v-model="selectedSubjectId" placeholder="请选择科目" @change="handleSubjectChange" filterable style="width: 100%;">
            <el-option v-for="subject in subjects" :key="subject.id" :label="subject.name" :value="subject.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedSubjectId">
          <el-button type="primary" @click="openChapterDialog(null)" icon="Plus">新增章节</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="chapters" v-loading="loadingChapters" style="width: 100%" class="mt-20" border>
        <el-table-column prop="name" label="章节名称" sortable></el-table-column>
        <el-table-column prop="description" label="章节描述" :show-overflow-tooltip="true"></el-table-column>
        <el-table-column prop="orderNum" label="排序号" width="100" sortable></el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button size="small" @click="openChapterDialog(scope.row)" icon="Edit">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteChapter(scope.row.id)" icon="Delete">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑章节对话框 -->
    <el-dialog 
      :title="isEditMode ? '编辑章节' : '新增章节'" 
      v-model="chapterDialogVisible" 
      width="500px"
      @closed="resetChapterForm"
      draggable
    >
      <el-form :model="chapterForm" ref="chapterFormRef" label-width="80px" @submit.prevent>
        <el-form-item 
            label="章节名称" 
            prop="name" 
            :rules="[{ required: true, message: '章节名称不能为空', trigger: 'blur' }]"
        >
          <el-input v-model="chapterForm.name" placeholder="请输入章节名称"></el-input>
        </el-form-item>
        <el-form-item label="章节描述" prop="description">
          <el-input type="textarea" :rows="3" v-model="chapterForm.description" placeholder="请输入章节描述"></el-input>
        </el-form-item>
        <el-form-item label="排序号" prop="orderNum">
          <el-input-number v-model="chapterForm.orderNum" :min="1" placeholder="可选，自动生成"></el-input-number>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="chapterDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChapterForm" :loading="submittingChapter">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../../api'; // Assuming default export from api/index.js

const subjects = ref([]);
const selectedSubjectId = ref(null);
const chapters = ref([]);
const loadingChapters = ref(false);

const chapterDialogVisible = ref(false);
const isEditMode = ref(false);
const chapterFormRef = ref(null);
const chapterForm = reactive({
  id: null,
  name: '',
  description: '',
  orderNum: null,
  subjectId: null // Will be set from selectedSubjectId when creating
});
const submittingChapter = ref(false);

// --- Lifecycle Hooks ---
onMounted(() => {
  fetchSubjects();
});

// --- API Calls & Logic ---
const fetchSubjects = async () => {
  try {
    const response = await api.subjectAdminJ.getAll();
    subjects.value = response || [];
  } catch (error) {
    ElMessage.error('获取科目列表失败');
    console.error("Error fetching subjects:", error);
  }
};

const handleSubjectChange = (subjectId) => {
  if (subjectId) {
    fetchChapters(subjectId);
  } else {
    chapters.value = [];
  }
};

const fetchChapters = async (subjectId) => {
  if (!subjectId) return;
  loadingChapters.value = true;
  try {
    const response = await api.chapterAdminJ.getChaptersBySubjectId(subjectId);
    chapters.value = response || [];
  } catch (error) {
    chapters.value = [];
    ElMessage.error('获取章节列表失败');
    console.error(`Error fetching chapters for subject ${subjectId}:`, error);
  } finally {
    loadingChapters.value = false;
  }
};

const openChapterDialog = (chapterData) => {
  isEditMode.value = !!chapterData;
  if (chapterData) {
    Object.assign(chapterForm, chapterData);
    chapterForm.subjectId = chapterData.subject?.id || selectedSubjectId.value; // Ensure subjectId is correct if editing
  } else {
    // Reset for new chapter, ensure subjectId is current selected one
    chapterForm.id = null;
    chapterForm.name = '';
    chapterForm.description = '';
    chapterForm.orderNum = null;
    chapterForm.subjectId = selectedSubjectId.value;
  }
  chapterDialogVisible.value = true;
};

const resetChapterForm = () => {
  chapterFormRef.value?.resetFields();
  chapterForm.id = null;
  chapterForm.name = '';
  chapterForm.description = '';
  chapterForm.orderNum = null;
  chapterForm.subjectId = null;
};

const submitChapterForm = async () => {
  if (!chapterFormRef.value) return;
  chapterFormRef.value.validate(async (valid) => {
    if (valid) {
      submittingChapter.value = true;
      try {
        const payload = { ...chapterForm };
        if (!isEditMode.value) {
          payload.subjectId = selectedSubjectId.value; // Ensure subjectId is set for new chapter
          await api.chapterAdminJ.createChapter(payload);
          ElMessage.success('章节创建成功');
        } else {
          await api.chapterAdminJ.updateChapter(payload.id, payload);
          ElMessage.success('章节更新成功');
        }
        chapterDialogVisible.value = false;
        fetchChapters(selectedSubjectId.value); // Refresh list
      } catch (error) {
        const errorMsg = error.response?.data?.message || error.response?.data || error.message || '操作失败';
        ElMessage.error(errorMsg);
        console.error("Error submitting chapter form:", error.response || error);
      } finally {
        submittingChapter.value = false;
      }
    }
  });
};

const handleDeleteChapter = (chapterId) => {
  ElMessageBox.confirm(
    '确定要删除此章节吗？如果章节下有关联的题目，根据后端设置可能无法删除。',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    try {
      await api.chapterAdminJ.deleteChapter(chapterId);
      ElMessage.success('章节删除成功');
      fetchChapters(selectedSubjectId.value); // Refresh list
    } catch (error) {
      const errorMsg = error.response?.data?.message || error.response?.data || error.message || '删除失败';
      ElMessage.error(errorMsg);
      console.error("Error deleting chapter:", error.response || error);
    }
  }).catch(() => {});
};

</script>

<style scoped>
.chapter-management-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.mt-20 {
  margin-top: 20px;
}
</style> 