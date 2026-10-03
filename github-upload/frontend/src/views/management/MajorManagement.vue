<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>专业管理</span>
          <el-button type="primary" @click="handleCreate" style="float: right;" :disabled="!selectedCollegeId">新增专业</el-button>
        </div>
      </template>

      <!-- 筛选 -->
      <el-form :inline="true" :model="listQuery" class="demo-form-inline" @submit.native.prevent>
        <el-form-item label="选择学院">
          <el-select v-model="selectedCollegeId" placeholder="请选择学院" clearable @change="handleCollegeChange">
            <el-option
              v-for="college in colleges"
              :key="college.id"
              :label="college.name"
              :value="college.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业名称">
          <el-input v-model="listQuery.name" placeholder="按专业名称搜索" @keyup.enter="handleFilter" :disabled="!selectedCollegeId"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter" :disabled="!selectedCollegeId">搜索</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        v-loading="listLoading"
        :data="filteredList"
        border
        fit
        highlight-current-row
        style="width: 100%;"
      >
        <el-table-column label="ID" prop="id" sortable="custom" align="center" width="80">
           <template #default="{row}"><span>{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column label="专业名称" prop="name" sortable="custom" min-width="150px">
            <template #default="{row}"><span>{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column label="专业代码" prop="code" min-width="100px">
            <template #default="{row}"><span>{{ row.code }}</span></template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="200px">
            <template #default="{row}"><span>{{ row.description }}</span></template>
        </el-table-column>
        <el-table-column label="所属学院" prop="college.name" min-width="120px">
            <template #default="{row}"><span>{{ row.college?.name }}</span></template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
          <template #default="{row,$index}">
            <el-button type="primary" size="mini" @click="handleUpdate(row)">编辑</el-button>
            <el-button type="danger" size="mini" @click="handleDelete(row,$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-alert v-if="list.length === 0 && !listLoading && selectedCollegeId" title="该学院下暂无专业数据" type="info" show-icon style="margin-top: 20px;"/>
      <el-alert v-if="!selectedCollegeId && !listLoading" title="请先选择一个学院以查看其专业" type.warning show-icon style="margin-top: 20px;"/>

      <!-- 编辑/创建对话框 -->
      <el-dialog :title="dialogStatus==='create'?'新增专业':'编辑专业'" v-model="dialogFormVisible">
        <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
          <el-form-item label="所属学院" prop="collegeId">
             <el-select v-model="temp.collegeId" placeholder="请选择学院" disabled style="width:100%">
                <el-option
                  v-for="college in colleges"
                  :key="college.id"
                  :label="college.name"
                  :value="college.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业名称" prop="name">
            <el-input v-model="temp.name" />
          </el-form-item>
          <el-form-item label="专业代码" prop="code">
            <el-input v-model="temp.code" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="temp.description" type="textarea" :rows="3" />
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
import api from '@/api';
import { ElMessage, ElMessageBox, ElNotification, ElAlert } from 'element-plus';

const list = ref([]);
const listLoading = ref(false);
const listQuery = reactive({
  name: '',
});

const colleges = ref([]);
const selectedCollegeId = ref(null);

const dialogFormVisible = ref(false);
const dialogStatus = ref('');
const temp = reactive({
  id: undefined,
  name: '',
  code: '',
  description: '',
  collegeId: null, // 用于表单绑定和创建/更新时传递
});

const rules = {
  name: [{ required: true, message: '专业名称不能为空', trigger: 'blur' }],
  collegeId: [{ required: true, message: '必须选择所属学院', trigger: 'change' }], // 在创建时确保
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
    if (Array.isArray(response)) {
        colleges.value = response;
    } else {
        colleges.value = [];
        ElMessage.error('获取学院列表失败或格式不正确');
    }
  } catch (error) {
    console.error("Error fetching colleges for dropdown:", error);
    ElMessage.error('加载学院下拉列表失败');
  }
}

async function getMajorsByCollege() {
  if (!selectedCollegeId.value) {
    list.value = [];
    return;
  }
  listLoading.value = true;
  try {
    const response = await api.majorAdminJ.getAll({ collegeId: selectedCollegeId.value });
    if (Array.isArray(response)) {
      list.value = response;
    } else {
      list.value = [];
      console.error('Received majors data is not an array:', response);
      ElMessage.error('获取专业列表格式不正确');
    }
  } catch (error) {
    console.error("Error fetching majors:", error);
    ElMessage.error(error.response?.data?.message || error.message || '获取专业列表失败');
    list.value = [];
  } finally {
    listLoading.value = false;
  }
}

function handleCollegeChange() {
  listQuery.name = ''; // Reset major name search on college change
  getMajorsByCollege();
}

function handleFilter() {
  // 前端搜索
}

function resetTemp() {
  temp.id = undefined;
  temp.name = '';
  temp.code = '';
  temp.description = '';
  temp.collegeId = selectedCollegeId.value; // Set to currently selected college for new major
}

function handleCreate() {
  if (!selectedCollegeId.value) {
    ElMessage.warning('请先选择一个学院以新增专业');
    return;
  }
  resetTemp();
  dialogStatus.value = 'create';
  dialogFormVisible.value = true;
  dataForm.value?.clearValidate();
}

function createData() {
  dataForm.value?.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { 
          name: temp.name, 
          code: temp.code, 
          description: temp.description, 
          college: { id: temp.collegeId } // API需要college对象包含id
        };
        const response = await api.majorAdminJ.create(payload);
        if (response && (response.id || (response.success !== false && response.message === undefined))) {
          // list.value.unshift(response); // 后端返回的对象可能不包含完整的college信息，最好重新获取
          dialogFormVisible.value = false;
          ElNotification({ title: '成功', message: '专业创建成功', type: 'success', duration: 2000 });
          getMajorsByCollege(); // Refresh list for the current college
        } else {
          ElMessage.error(response.message || '创建专业失败');
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || error.message || '创建专业时发生错误');
      }
    }
  });
}

function handleUpdate(row) {
  temp.id = row.id;
  temp.name = row.name;
  temp.code = row.code;
  temp.description = row.description;
  temp.collegeId = row.college?.id || selectedCollegeId.value; // Fallback to selected if not present
  dialogStatus.value = 'update';
  dialogFormVisible.value = true;
  dataForm.value?.clearValidate();
}

function updateData() {
  dataForm.value?.validate(async (valid) => {
    if (valid) {
      try {
        const payload = { 
          id: temp.id, 
          name: temp.name, 
          code: temp.code, 
          description: temp.description, 
          college: { id: temp.collegeId } 
        };
        const response = await api.majorAdminJ.update(temp.id, payload);
        if (response && (response.id || (response.success !== false && response.message === undefined))) {
          dialogFormVisible.value = false;
          ElNotification({ title: '成功', message: '专业更新成功', type: 'success', duration: 2000 });
          getMajorsByCollege(); // Refresh list
        } else {
          ElMessage.error(response.message || '更新专业失败');
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || error.message || '更新专业时发生错误');
      }
    }
  });
}

function handleDelete(row, index) {
  ElMessageBox.confirm(`确认删除专业 "${row.name}" 吗？此操作不可恢复。`, '警告', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await api.majorAdminJ.delete(row.id);
      if (response === undefined || response === null || response.success !== false) {
        ElNotification({ title: '成功', message: '专业删除成功', type: 'success', duration: 2000 });
        // list.value.splice(index, 1); // 更安全的做法是重新获取列表
        getMajorsByCollege();
      } else {
         ElMessage.error(response.message || '删除专业失败');
      }
    } catch (error) {
      ElMessage.error(error.response?.data?.message || error.message || '删除专业时发生错误');
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
}

onMounted(() => {
  fetchColleges();
  // Initially, majors list is empty until a college is selected
});

</script>

<style scoped>
.app-container {
  padding: 20px;
}
.demo-form-inline {
  margin-bottom: 20px;
}
</style> 