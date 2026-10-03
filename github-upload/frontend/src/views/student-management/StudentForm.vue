<template>
  <div class="student-form-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEditMode ? '编辑学生' : '添加学生' }}</span>
          <el-button @click="goBackToList">返回列表</el-button>
        </div>
      </template>

      <el-form ref="studentFormRef" :model="studentForm" :rules="formRules" label-width="100px" label-position="top">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="username">
              <el-input v-model="studentForm.username" placeholder="请输入学号"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="studentForm.name" placeholder="请输入姓名"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" :prop="isEditMode ? '' : 'password'"> <!-- 编辑模式密码可选 -->
              <el-input type="password" v-model="studentForm.password" placeholder="请输入密码 (编辑时留空则不修改)" show-password></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" :prop="studentForm.password || !isEditMode ? 'confirmPassword' : ''">
              <el-input type="password" v-model="studentForm.confirmPassword" placeholder="请再次输入密码" show-password></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="studentForm.gender">
                <el-radio label="MALE">男</el-radio>
                <el-radio label="FEMALE">女</el-radio>
                <el-radio label="OTHER">其他</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthday">
              <el-date-picker v-model="studentForm.birthday" type="date" placeholder="选择出生日期" style="width: 100%;" format="YYYY-MM-DD" value-format="YYYY-MM-DD"></el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="电话" prop="phone">
              <el-input v-model="studentForm.phone" placeholder="请输入电话号码"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="studentForm.email" placeholder="请输入邮箱地址"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="班级" prop="clazzId">
          <el-select v-model="studentForm.clazzId" placeholder="选择班级 (可选)" clearable style="width: 100%;">
            <el-option v-for="item in classList" :key="item.id" :label="item.name" :value="item.id"></el-option>
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">{{ isEditMode ? '保存修改' : '立即创建' }}</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../../api';

const router = useRouter();
const route = useRoute();
const studentFormRef = ref(null);
const submitting = ref(false);
const classList = ref([]);

const studentForm = reactive({
  username: '',
  name: '',
  password: '',
  confirmPassword: '',
  gender: '',
  birthday: null,
  phone: '',
  email: '',
  clazzId: null, // 用于存储班级ID
});

const isEditMode = computed(() => !!route.params.id);
const studentId = computed(() => route.params.id ? Number(route.params.id) : null);

// 表单校验规则
const validatePass = (rule, value, callback) => {
  if (studentForm.password && value === '') {
    callback(new Error('请再次输入密码'));
  } else if (value !== studentForm.password) {
    callback(new Error('两次输入的密码不一致!'));
  } else {
    callback();
  }
};

const formRules = reactive({
  username: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  password: [
    { required: !isEditMode.value, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    // 仅当创建模式或编辑模式下输入了密码时，此字段才必填
    { required: computed(() => !isEditMode.value || (isEditMode.value && studentForm.password)), validator: validatePass, trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }],
  // clazzId: [{ required: true, message: '请选择班级', trigger: 'change' }] // 班级设为可选
});

// 获取班级列表
const fetchClasses = async () => {
  try {
    const response = await api.studentJ.getAvailableClasses();
    classList.value = response || []; // 后端返回的是 List<ClassEntity>
  } catch (error) {
    ElMessage.error('获取班级列表失败!');
    console.error("Error fetching classes:", error);
  }
};

// 加载学生数据 (编辑模式)
const loadStudentData = async () => {
  if (!isEditMode.value || !studentId.value) return;
  submitting.value = true;
  try {
    const response = await api.studentJ.getStudentById(studentId.value);
    if (response) {
      studentForm.username = response.username;
      studentForm.name = response.name;
      // 密码和确认密码在编辑时不直接填充
      studentForm.gender = response.gender;
      studentForm.birthday = response.birthday; // 后端返回的应该是 YYYY-MM-DD 格式
      studentForm.phone = response.phone;
      studentForm.email = response.email;
      studentForm.clazzId = response.clazz ? response.clazz.id : null;
    }
  } catch (error) {
    ElMessage.error('加载学生信息失败!');
    router.push('/student-management');
  } finally {
    submitting.value = false;
  }
};

const submitForm = async () => {
  if (!studentFormRef.value) return;
  await studentFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true;
      const payload = {
        username: studentForm.username,
        name: studentForm.name,
        gender: studentForm.gender,
        birthday: studentForm.birthday,
        phone: studentForm.phone,
        email: studentForm.email,
        // clazzId: studentForm.clazzId, // 直接传递clazzId
        clazz: studentForm.clazzId ? { id: studentForm.clazzId } : null, // 按后端的 StudentApiController 要求构建对象
      };
      if (studentForm.password) {
        payload.password = studentForm.password;
      }

      try {
        if (isEditMode.value) {
          await api.studentJ.updateStudent(studentId.value, payload);
          ElMessage.success('学生信息更新成功!');
        } else {
          await api.studentJ.createStudent(payload);
          ElMessage.success('学生添加成功!');
        }
        router.push('/student-management');
      } catch (error) {
        let errorMessage = isEditMode.value ? '更新失败!' : '添加失败!';
        if (error.response && error.response.data && error.response.data.message) {
            errorMessage += ` (${error.response.data.message})`;
        } else if (error.message) {
            errorMessage += ` (${error.message})`;
        }
        ElMessage.error(errorMessage);
        console.error("Error submitting student form:", error);
      } finally {
        submitting.value = false;
      }
    }
  });
};

const resetForm = () => {
  if (studentFormRef.value) {
    studentFormRef.value.resetFields();
    // 如果是编辑模式，重置后应该恢复到原始加载的数据，或者清空密码
    if (isEditMode.value) {
        studentForm.password = '';
        studentForm.confirmPassword = '';
        loadStudentData(); // 重新加载原始数据
    } else {
        studentForm.clazzId = null;
    }
  }
};

const goBackToList = () => {
  router.push('/student-management');
};

onMounted(() => {
  fetchClasses();
  if (isEditMode.value) {
    loadStudentData();
  }
});

</script>

<style scoped>
.student-form-container {
  padding: 20px;
  max-width: 800px; /* 限制最大宽度，使其居中更美观 */
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 