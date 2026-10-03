<template>
  <div class="subject-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>科目管理</span>
          <el-button type="primary" @click="handleAddSubject">新增科目</el-button>
        </div>
      </template>

      <el-table :data="subjects" style="width: 100%" v-loading="loading">
        <el-table-column prop="id" label="ID" width="100"></el-table-column>
        <el-table-column prop="name" label="科目名称"></el-table-column>
        <el-table-column prop="description" label="描述"></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="scope">
            <el-button size="small" @click="handleEditSubject(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDeleteSubject(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑科目的对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="30%"
      @close="resetForm"
    >
      <el-form :model="currentSubject" :rules="rules" ref="subjectFormRef" label-width="80px">
        <el-form-item label="科目名称" prop="name">
          <el-input v-model="currentSubject.name"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="currentSubject.description"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitSubjectForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '@/api'; // Assuming your api/index.js exports default

const subjects = ref([]);
const loading = ref(false);
const dialogVisible = ref(false);
const dialogTitle = ref('');
const currentSubject = reactive({
  id: null,
  name: '',
  description: ''
});
const subjectFormRef = ref(null); // Reference to the form

const rules = {
  name: [
    { required: true, message: '请输入科目名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
};

const fetchSubjects = async () => {
  loading.value = true;
  try {
    // const response = await api.subjectsJ.getAllSubjects(); // If using the old one
    const response = await api.subjectAdminJ.getAll(); // Using the new admin API

    // 确保response是数组，如果不是则使用空数组
    if (Array.isArray(response)) {
      subjects.value = response;
    } else if (response && Array.isArray(response.data)) {
      subjects.value = response.data;
    } else {
      console.warn('API返回的科目数据格式不正确:', response);
      subjects.value = [];
      ElMessage.warning('科目数据格式异常，请检查后端API');
    }
  } catch (error) {
    console.error('获取科目列表失败:', error);
    subjects.value = []; // 确保在错误时设置为空数组
    ElMessage.error('获取科目列表失败: ' + (error.response?.data?.message || error.message));
  } finally {
    loading.value = false;
  }
};

onMounted(fetchSubjects);

const resetForm = () => {
  currentSubject.id = null;
  currentSubject.name = '';
  currentSubject.description = '';
  if (subjectFormRef.value) {
    subjectFormRef.value.resetFields();
  }
};

const handleAddSubject = () => {
  resetForm();
  dialogTitle.value = '新增科目';
  dialogVisible.value = true;
};

const handleEditSubject = (subject) => {
  resetForm(); // Clear previous data first
  // Object.assign(currentSubject, subject); // Shallow copy, might be ok for simple object
  currentSubject.id = subject.id;
  currentSubject.name = subject.name;
  currentSubject.description = subject.description;
  dialogTitle.value = '编辑科目';
  dialogVisible.value = true;
};

const submitSubjectForm = async () => {
  if (!subjectFormRef.value) return;
  subjectFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        if (currentSubject.id) {
          // Update
          await api.subjectAdminJ.update(currentSubject.id, { name: currentSubject.name, description: currentSubject.description });
          ElMessage.success('科目更新成功');
        } else {
          // Create
          await api.subjectAdminJ.create({ name: currentSubject.name, description: currentSubject.description });
          ElMessage.success('科目新增成功');
        }
        dialogVisible.value = false;
        fetchSubjects(); // Refresh the list
      } catch (error) {
        ElMessage.error('操作失败: ' + (error.response?.data || error.message));
        // Log the full error for debugging if needed
        console.error("Subject form submission error:", error.response || error);
      } finally {
        loading.value = false;
      }
    } else {
      ElMessage.error('请检查表单输入');
      return false;
    }
  });
};

const handleDeleteSubject = (id) => {
  ElMessageBox.confirm(
    '确定要删除该科目吗？相关章节和题目可能也会受到影响。',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }
  ).then(async () => {
    loading.value = true;
    try {
      await api.subjectAdminJ.delete(id);
      ElMessage.success('科目删除成功');
      fetchSubjects(); // Refresh the list
    } catch (error) {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message));
    } finally {
      loading.value = false;
    }
  }).catch(() => {
    // User cancelled
    ElMessage.info('已取消删除');
  });
};

</script>

<style scoped>
.subject-management-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 