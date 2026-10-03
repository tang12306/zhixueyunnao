<template>
  <div class="student-list-container">
    <div class="page-header">
      <h2>学生列表</h2>
      <div v-if="filterByClass" class="class-filter-info">
        <el-alert
          :title="classFilterTitle"
          type="info"
          show-icon
          :closable="false"
          style="margin-bottom: 16px;"
        >
          <template #default>
            <div style="display: flex; align-items: center; justify-content: space-between;">
              <span>正在显示班级 "{{ filterClassName }}" 的学生 (共 {{ studentList.length }} 人)</span>
              <el-button size="small" @click="clearClassFilter">显示所有学生</el-button>
            </div>
          </template>
        </el-alert>
      </div>
      <el-button type="primary" @click="goToCreateStudent">
        <el-icon><Plus /></el-icon> 添加学生
      </el-button>
    </div>

    <!-- TODO: 添加搜索和筛选区域 -->

    <el-table :data="studentList" v-loading="loading" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="username" label="学号"></el-table-column>
      <el-table-column prop="name" label="姓名"></el-table-column>
      <el-table-column prop="gender" label="性别" width="100">
        <template #default="{ row }">
          {{ formatGender(row.gender) }}
        </template>
      </el-table-column>
      <el-table-column label="班级">
        <template #default="{ row }">
          {{ row.clazz ? row.clazz.name : '未分配' }}
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱"></el-table-column>
      <el-table-column prop="phone" label="电话"></el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="editStudent(row.id)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button size="small" type="danger" @click="confirmDeleteStudent(row.id)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- TODO: 添加分页组件 -->

  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Edit, Delete } from '@element-plus/icons-vue';
import api from '../../api'; // 修正API路径

const router = useRouter();
const route = useRoute();
const studentList = ref([]);
const loading = ref(false);

// 班级过滤相关
const filterByClass = computed(() => !!route.query.classId);
const filterClassName = computed(() => route.query.className || '未知班级');
const filterClassId = computed(() => route.query.classId ? parseInt(route.query.classId) : null);
const classFilterTitle = computed(() => `正在显示班级 "${filterClassName.value}" 的学生`);

// 性别格式化函数
const formatGender = (gender) => {
  if (!gender) return '未知';
  const lowerGender = String(gender).toLowerCase();
  if (lowerGender === 'male' || gender === '男') return '男';
  if (lowerGender === 'female' || gender === '女') return '女';
  if (lowerGender === 'other' || gender === '其他') return '其他';
  return gender; 
};

const fetchStudents = async () => {
  loading.value = true;
  try {
    const response = await api.studentJ.getAllStudents();
    let students = [];

    // 确保response是数组，如果不是则使用空数组
    if (Array.isArray(response)) {
      students = response;
    } else if (response && Array.isArray(response.data)) {
      students = response.data;
    } else {
      console.warn('API返回的数据格式不正确:', response);
      students = [];
    }

    // 如果需要按班级过滤
    if (filterByClass.value && filterClassId.value) {
      students = students.filter(student =>
        student.clazz && student.clazz.id === filterClassId.value
      );
      console.log(`过滤班级 ${filterClassName.value} (ID: ${filterClassId.value}) 的学生:`, students);
    }

    studentList.value = students;
  } catch (error) {
    ElMessage.error('获取学生列表失败!');
    console.error("Error fetching students:", error);
    studentList.value = []; // 确保在错误时设置为空数组
  } finally {
    loading.value = false;
  }
};

// 清除班级过滤
const clearClassFilter = () => {
  router.push('/student-management');
};

const goToCreateStudent = () => {
  router.push('/student-management/create');
};

const editStudent = (id) => {
  router.push(`/student-management/edit/${id}`);
};

const confirmDeleteStudent = async (id) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该学生吗? 此操作不可撤销。',
      '警告',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    loading.value = true;
    await api.studentJ.deleteStudent(id);
    ElMessage.success('学生删除成功!');
    await fetchStudents(); // 重新加载列表
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除学生失败!');
      console.error("Error deleting student:", error);
    }
  } finally {
    loading.value = false;
  }
};

// 监听路由变化，重新加载数据
watch(() => route.query, () => {
  fetchStudents();
}, { immediate: false });

onMounted(() => {
  fetchStudents();
});

</script>

<style scoped>
.student-list-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style> 