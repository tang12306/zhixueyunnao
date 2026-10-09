<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>班级管理</span>
          <el-button type="primary" @click="handleCreate" style="float: right;" :disabled="!selectedMajorId">新增班级</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="listQuery" class="demo-form-inline" @submit.prevent>
        <el-form-item label="选择学院">
          <el-select v-model="selectedCollegeId" placeholder="请选择学院" clearable @change="handleCollegeChange">
            <el-option v-for="c in colleges" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="选择专业">
          <el-select v-model="selectedMajorId" placeholder="请选择专业" clearable @change="handleMajorChange" :disabled="!selectedCollegeId">
            <el-option v-for="m in majors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级名称">
          <el-input v-model="listQuery.name" placeholder="按班级名称搜索" @keyup.enter="handleFilter" :disabled="!selectedMajorId"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter" :disabled="!selectedMajorId">搜索</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table v-loading="listLoading" :data="filteredList" border fit highlight-current-row style="width: 100%;">
        <el-table-column label="ID" prop="id" align="center" width="80">
            <template #default="{row}"><span>{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column label="班级名称" prop="name" min-width="150px">
            <template #default="{row}"><span>{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column label="年级" prop="grade" width="100px">
             <template #default="{row}"><span>{{ row.grade }}</span></template>
        </el-table-column>
        <el-table-column label="所属专业" prop="major.name" min-width="120px">
            <template #default="{row}"><span>{{ row.major?.name }}</span></template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
          <template #default="{row,$index}">
            <el-button type="primary" size="small" @click="handleUpdate(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row,$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-alert v-if="list.length === 0 && !listLoading && selectedMajorId" title="该专业下暂无班级数据" type="info" show-icon style="margin-top: 20px;"/>
      <el-alert v-if="!selectedMajorId && !listLoading && selectedCollegeId" title="请先选择专业以查看班级" type="info" show-icon style="margin-top: 20px;"/>
      <el-alert v-if="!selectedCollegeId && !listLoading" title="请先选择学院" type="info" show-icon style="margin-top: 20px;"/>


      <!-- 编辑/创建对话框 -->
      <el-dialog :title="dialogStatus==='create'?'新增班级':'编辑班级'" v-model="dialogFormVisible">
        <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
          <el-form-item label="所属专业" prop="majorId">
             <el-select v-model="temp.majorId" placeholder="专业" disabled style="width:100%">
                <el-option v-for="m in majorsForDialog" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="班级名称" prop="name">
            <el-input v-model="temp.name" />
          </el-form-item>
          <el-form-item label="年级" prop="grade">
            <el-input-number v-model="temp.grade" :min="2000" :max="2100" />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="dialog-footer">
            <el-button @click="dialogFormVisible = false">取消</el-button>
            <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">确认</el-button>
          </div>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api';
import { ElMessage, ElMessageBox, ElNotification, ElAlert } from 'element-plus';

// 初始化路由
const router = useRouter();

const list = ref([]);
const listLoading = ref(false);
const listQuery = reactive({ name: '' });

const colleges = ref([]);
const selectedCollegeId = ref(null);
const majors = ref([]); // Majors for selected college
const selectedMajorId = ref(null);
const majorsForDialog = ref([]); // Majors for the dialog dropdown (could be just the selected one)

const dialogFormVisible = ref(false);
const dialogStatus = ref('');
const temp = reactive({
  id: undefined,
  name: '',
  grade: new Date().getFullYear(), // Default to current year
  majorId: null,
});

const rules = {
  name: [{ required: true, message: '班级名称不能为空', trigger: 'blur' }],
  majorId: [{ required: true, message: '必须选择所属专业', trigger: 'change' }],
  grade: [{ required: true, message: '年级不能为空', trigger: 'blur' }],
};
const dataForm = ref(null);

const filteredList = computed(() => {
  const { name } = listQuery;
  if (name) {
    return list.value.filter(item => item.name && item.name.toLowerCase().includes(name.toLowerCase()));
  }
  return list.value;
});

async function fetchColleges() {
  try {
    const response = await api.collegeAdminJ.getAll();
    colleges.value = Array.isArray(response) ? response : [];
  } catch (e) { ElMessage.error('加载学院列表失败'); colleges.value = []; }
}

async function fetchMajorsByCollege(collegeId) {
  if (!collegeId) {
    majors.value = [];
    selectedMajorId.value = null;
    list.value = []; // Clear classes if college is cleared
    return;
  }
  try {
    const response = await api.majorAdminJ.getAll({ collegeId });
    majors.value = Array.isArray(response) ? response : [];
    if (majors.value.length > 0) {
      // Optionally auto-select first major or leave for user to select
      // selectedMajorId.value = majors.value[0].id;
      // fetchClassesByMajor(); 
    } else {
      selectedMajorId.value = null; // No majors for this college
      list.value = []; // Clear class list
    }
    // If currently selected major is not in the new list, reset it (should not happen if logic is correct)
    if (selectedMajorId.value && !majors.value.find(m => m.id === selectedMajorId.value)) {
        selectedMajorId.value = null;
        list.value = []; 
    }
  } catch (e) { 
    ElMessage.error(e.response?.data?.message || '加载专业列表失败'); 
    majors.value = []; 
    selectedMajorId.value = null; 
    list.value = [];
  }
}

async function fetchClassesByMajor() {
  if (!selectedMajorId.value) {
    list.value = [];
    return;
  }
  listLoading.value = true;
  try {
    const response = await api.classAdminJ.getAll({ majorId: selectedMajorId.value });
    list.value = Array.isArray(response) ? response : [];
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取班级列表失败');
    list.value = [];
  } finally { listLoading.value = false; }
}

function handleCollegeChange(collegeId) {
  selectedMajorId.value = null; 
  majors.value = [];
  list.value = []; 
  if (collegeId) {
    fetchMajorsByCollege(collegeId);
  }
}

function handleMajorChange(majorId) {
  listQuery.name = ''; 
  if (majorId) {
    fetchClassesByMajor();
  } else {
    list.value = []; // Clear classes if major is deselected
  }
}

function handleFilter() { /* Frontend search for class name within current list */ }

function resetTemp() {
  temp.id = undefined;
  temp.name = '';
  temp.grade = new Date().getFullYear();
  temp.majorId = selectedMajorId.value;
}

function handleCreate() {
  if (!selectedMajorId.value) {
    ElMessage.warning('请先选择学院和专业以新增班级');
    return;
  }
  resetTemp();
  // For dialog, ensure the selected major is available in its dropdown
  const selectedMajorObject = majors.value.find(m => m.id === selectedMajorId.value);
  majorsForDialog.value = selectedMajorObject ? [selectedMajorObject] : [];
  temp.majorId = selectedMajorId.value; // Ensure temp has the ID

  dialogStatus.value = 'create';
  dialogFormVisible.value = true;
  dataForm.value?.clearValidate();
}

function createData() {
  dataForm.value?.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { name: temp.name, grade: temp.grade, major: { id: temp.majorId } };
        const response = await api.classAdminJ.create(payload);
        if (response && response.id) {
          dialogFormVisible.value = false;
          ElNotification({ title: '成功', message: '班级创建成功', type: 'success', duration: 2000 });
          fetchClassesByMajor(); // Refresh list
        } else { ElMessage.error(response?.message || '创建班级失败'); }
      } catch (e) { ElMessage.error(e.response?.data?.message || '创建班级时发生错误'); }
    }
  });
}

function handleUpdate(row) {
  temp.id = row.id;
  temp.name = row.name;
  temp.grade = row.grade;
  temp.majorId = row.major?.id;
  
  // Ensure the major of the row being edited is in majorsForDialog
  const majorOfRow = majors.value.find(m => m.id === row.major?.id);
  majorsForDialog.value = majorOfRow ? [majorOfRow] : (row.major ? [{id: row.major.id, name: row.major.name}] : []);
  if (!temp.majorId && selectedMajorId.value) { // If major was somehow null, assign current
    temp.majorId = selectedMajorId.value;
    const selectedMajorObject = majors.value.find(m => m.id === selectedMajorId.value);
    if(selectedMajorObject) majorsForDialog.value = [selectedMajorObject];
  }

  dialogStatus.value = 'update';
  dialogFormVisible.value = true;
  dataForm.value?.clearValidate();
}

function updateData() {
  dataForm.value?.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { id: temp.id, name: temp.name, grade: temp.grade, major: { id: temp.majorId } };
        const response = await api.classAdminJ.update(temp.id, payload);
        if (response && response.id) {
          dialogFormVisible.value = false;
          ElNotification({ title: '成功', message: '班级更新成功', type: 'success', duration: 2000 });
          fetchClassesByMajor(); // Refresh list
        } else { ElMessage.error(response?.message || '更新班级失败'); }
      } catch (e) { ElMessage.error(e.response?.data?.message || '更新班级时发生错误'); }
    }
  });
}

async function handleDelete(row) {
  try {
    console.log('检查班级删除状态，班级ID:', row.id);
    // 首先检查班级是否可以删除
    const checkResult = await api.classAdminJ.canDelete(row.id);
    console.log('检查结果:', checkResult);

    // 由于Java API响应拦截器直接返回response.data，所以checkResult就是我们需要的数据
    const { canDelete, message } = checkResult;

    if (!canDelete) {
      // 如果不能删除，显示详细信息和操作建议
      ElMessageBox.confirm(
        `${message}\n\n您可以选择以下操作：\n1. 转移学生到其他班级\n2. 删除班级下的所有学生\n3. 取消删除操作\n\n是否继续查看学生管理页面？`,
        '无法删除班级',
        {
          confirmButtonText: '查看学生管理',
          cancelButtonText: '取消',
          type: 'warning',
          dangerouslyUseHTMLString: false
        }
      ).then(() => {
        // 跳转到学生管理页面
        ElMessage.info('正在跳转到学生管理页面...');
        // 跳转到学生管理页面，并传递班级ID作为查询参数
        router.push({
          path: '/student-management',
          query: { classId: row.id, className: row.name }
        });
      }).catch(() => {
        ElMessage.info('已取消删除');
      });
      return;
    }

    // 如果可以删除，显示确认对话框
    ElMessageBox.confirm(
      `确认删除班级 "${row.name}" 吗？\n该班级下没有学生，可以安全删除。\n此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      try {
        await api.classAdminJ.delete(row.id);
        ElNotification({
          title: '成功',
          message: '班级删除成功',
          type: 'success',
          duration: 2000
        });
        fetchClassesByMajor(); // Refresh list
      } catch (e) {
        ElMessage.error(e.response?.data?.message || '删除班级失败，请重试。');
      }
    }).catch(() => {
      ElMessage.info('已取消删除');
    });

  } catch (error) {
    console.error('检查班级删除状态失败:', error);

    // 如果检查失败，询问用户是否直接尝试删除
    ElMessageBox.confirm(
      `无法检查班级状态（${error.response?.data?.message || error.message || '网络错误'}）。\n\n是否直接尝试删除班级 "${row.name}"？\n\n注意：如果班级下有学生，删除将会失败。`,
      '检查状态失败',
      {
        confirmButtonText: '直接删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      // 直接尝试删除
      try {
        await api.classAdminJ.delete(row.id);
        ElNotification({
          title: '成功',
          message: '班级删除成功',
          type: 'success',
          duration: 2000
        });
        fetchClassesByMajor(); // Refresh list
      } catch (deleteError) {
        ElMessage.error(deleteError.response?.data?.message || '删除班级失败，请重试。');
      }
    }).catch(() => {
      ElMessage.info('已取消删除');
    });
  }
}

onMounted(() => {
  fetchColleges();
  // Class list will be fetched when a major is selected
});

</script>
<style scoped>
.app-container { padding: 20px; }
.demo-form-inline { margin-bottom: 20px; }
</style> 