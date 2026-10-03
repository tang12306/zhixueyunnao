<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>学院管理</span>
          <el-button type="primary" @click="handleCreate" style="float: right;">新增学院</el-button>
        </div>
      </template>

      <!-- 搜索和操作 -->
      <el-form :inline="true" :model="listQuery" class="demo-form-inline" @submit.native.prevent>
        <el-form-item label="学院名称">
          <el-input v-model="listQuery.name" placeholder="按学院名称搜索" @keyup.enter="handleFilter"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">搜索</el-button>
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
          <template #default="{row}">
            <span>{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学院名称" prop="name" sortable="custom" min-width="150px">
          <template #default="{row}">
            <span>{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="描述" prop="description" min-width="200px">
          <template #default="{row}">
            <span>{{ row.description }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
          <template #default="{row,$index}">
            <el-button type="primary" size="mini" @click="handleUpdate(row)">编辑</el-button>
            <el-button type="danger" size="mini" @click="handleDelete(row,$index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <!-- <pagination v-show="total>0" :total="total" v-model:page="listQuery.page" v-model:limit="listQuery.limit" @pagination="getList" /> -->
      <el-alert v-if="list.length === 0 && !listLoading" title="暂无数据" type="info" show-icon style="margin-top: 20px;"/>


      <!-- 编辑/创建对话框 -->
      <el-dialog :title="dialogStatus==='create'?'新增学院':'编辑学院'" v-model="dialogFormVisible">
        <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
          <el-form-item label="学院名称" prop="name">
            <el-input v-model="temp.name" />
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
import api from '@/api'; // 确保路径正确
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';

const list = ref([]);
const listLoading = ref(true);
const listQuery = reactive({
  // page: 1,
  // limit: 20,
  name: '', // 用于搜索
});
// const total = ref(0); // 如果使用分页

const dialogFormVisible = ref(false);
const dialogStatus = ref('');
const temp = reactive({
  id: undefined,
  name: '',
  description: '',
  // school: { id: 1 } // 假设默认学校ID为1，后端会自动关联
});

const rules = {
  name: [{ required: true, message: '学院名称不能为空', trigger: 'blur' }],
};
const dataForm = ref(null); // For form validation

const filteredList = computed(() => {
  const { name } = listQuery;
  if (name) {
    return list.value.filter(item => item.name && item.name.toLowerCase().includes(name.toLowerCase()));
  }
  return list.value;
});


async function getList() {
  listLoading.value = true;
  try {
    const response = await api.collegeAdminJ.getAll();
    // 后端直接返回的是List<College>，不是分页结构
    if (Array.isArray(response)) {
      list.value = response;
    } else if (response && Array.isArray(response.data)) { // 兼容可能包装的响应
        list.value = response.data
    } else if (response && Array.isArray(response.colleges)) { // 兼容可能包装的响应
        list.value = response.colleges
    }
     else {
      list.value = [];
      console.error('Received colleges data is not an array:', response);
      ElMessage.error('获取学院列表格式不正确');
    }
    // total.value = list.value.length; // 如果是前端分页
  } catch (error) {
    console.error("Error fetching colleges:", error);
    ElMessage.error(error.response?.data?.message || error.message || '获取学院列表失败');
    list.value = [];
  } finally {
    listLoading.value = false;
  }
}

function handleFilter() {
  // 前端搜索，不需要重新从API获取
  // getList(); 
}

function resetTemp() {
  temp.id = undefined;
  temp.name = '';
  temp.description = '';
}

function handleCreate() {
  resetTemp();
  dialogStatus.value = 'create';
  dialogFormVisible.value = true;
  dataForm.value?.clearValidate();
}

function createData() {
  dataForm.value?.validate(async (valid) => {
    if (valid) {
      try {
        const createPayload = { name: temp.name, description: temp.description };
        // School will be set by backend
        const response = await api.collegeAdminJ.create(createPayload);
        if (response && (response.id || (response.success !== false && response.message === undefined))) { // 简单成功判断
          list.value.unshift(response); // 假设返回的是创建的对象
          dialogFormVisible.value = false;
          ElNotification({
            title: '成功',
            message: '学院创建成功',
            type: 'success',
            duration: 2000
          });
          getList(); // 重新获取列表以保证ID等信息正确
        } else {
           ElMessage.error(response.message || '创建学院失败');
        }
      } catch (error) {
        console.error("Error creating college:", error);
        ElMessage.error(error.response?.data?.message || error.message || '创建学院时发生错误');
      }
    }
  });
}

function handleUpdate(row) {
  Object.assign(temp, row); // Copy row data to temp
  dialogStatus.value = 'update';
  dialogFormVisible.value = true;
  dataForm.value?.clearValidate();
}

function updateData() {
  dataForm.value?.validate(async (valid) => {
    if (valid) {
      try {
        const updatePayload = { id: temp.id, name: temp.name, description: temp.description };
        const response = await api.collegeAdminJ.update(temp.id, updatePayload);
         if (response && (response.id || (response.success !== false && response.message === undefined))) {
          const index = list.value.findIndex(v => v.id === temp.id);
          if (index !== -1) {
            list.value.splice(index, 1, { ...list.value[index], ...temp });
          }
          dialogFormVisible.value = false;
          ElNotification({
            title: '成功',
            message: '学院更新成功',
            type: 'success',
            duration: 2000
          });
          getList(); // 重新获取列表以保证数据一致性
        } else {
           ElMessage.error(response.message || '更新学院失败');
        }
      } catch (error) {
        console.error("Error updating college:", error);
        ElMessage.error(error.response?.data?.message || error.message || '更新学院时发生错误');
      }
    }
  });
}

function handleDelete(row, index) {
  ElMessageBox.confirm(`确认删除学院 "${row.name}" 吗？此操作不可恢复。`, '警告', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const response = await api.collegeAdminJ.delete(row.id);
      // delete API 通常返回 204 No Content, response 可能为空或特定成功对象
      if (response === undefined || response === null || response.success !== false) { // 简单判断
        ElNotification({
          title: '成功',
          message: '学院删除成功',
          type: 'success',
          duration: 2000
        });
        list.value.splice(index, 1);
        // total.value--;
      } else {
         ElMessage.error(response.message || '删除学院失败');
      }
    } catch (error) {
      console.error("Error deleting college:", error);
      ElMessage.error(error.response?.data?.message || error.message || '删除学院时发生错误');
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
}

onMounted(() => {
  getList();
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