<template>
  <div class="user-profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="info">
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="100px" label-position="top">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="profileForm.name"></el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="profileForm.email"></el-input>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="角色" prop="role">
                  <el-input v-model="profileForm.role" disabled></el-input>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item>
              <el-button type="primary" @click="submitProfileForm" :loading="loadingProfile">保存更改</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="修改密码" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px" label-position="top">
            <el-form-item label="当前密码" prop="oldPassword">
              <el-input type="password" v-model="passwordForm.oldPassword" show-password></el-input>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input type="password" v-model="passwordForm.newPassword" show-password></el-input>
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmNewPassword">
              <el-input type="password" v-model="passwordForm.confirmNewPassword" show-password></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitPasswordForm" :loading="loadingPassword">确认修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api'; // 确保路径正确

const activeTab = ref('info');
const profileFormRef = ref(null);
const passwordFormRef = ref(null);
const loadingProfile = ref(false);
const loadingPassword = ref(false);

const profileForm = reactive({
  name: '',
  email: '',
  role: ''
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmNewPassword: ''
});

// 校验规则
const profileRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: false, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: ['blur', 'change'] }
  ]
};

const validatePassConfirm = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'));
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致!'));
  } else {
    callback();
  }
};

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' }
  ],
  confirmNewPassword: [
    { required: true, validator: validatePassConfirm, trigger: 'blur' }
  ]
};

// 获取当前用户信息
const fetchUserProfile = async () => {
  try {
    const response = await api.userJ.getCurrentUser();
    if (response && response.success && response.user) {
      profileForm.name = response.user.name;
      profileForm.email = response.user.email || '';
      profileForm.role = response.user.role;
    } else {
      console.error('获取用户信息失败:', response);
      ElMessage.error(response.message || '获取用户信息失败');
    }
  } catch (error) {
    console.error("Error fetching user profile:", error);

    // 401 时全局拦截器会跳回登录页
    if (!(error.response && error.response.status === 401)) {
      ElMessage.error('加载用户信息时出错，请稍后重试');
    }
  }
};

// 提交个人信息表单
const submitProfileForm = async () => {
  if (!profileFormRef.value) return;
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      loadingProfile.value = true;
      try {
        const payload = {
          name: profileForm.name,
          email: profileForm.email
        };
        // 使用Java后端API更新用户信息
        const response = await api.userJ.updateProfile(payload);
        if (response && response.success) {
          ElMessage.success('个人信息更新成功!');
          // 可选：更新表单数据，如果后端返回了更新后的user对象
          if(response.user) {
            profileForm.name = response.user.name;
            profileForm.email = response.user.email || '';
          }
        } else {
          ElMessage.error(response.message || '更新个人信息失败');
        }
      } catch (error) {
        console.error("Error updating profile:", error);
        let errorMessage = '更新个人信息时发生错误';
        if (error.response && error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message;
        }
        ElMessage.error(errorMessage);
      } finally {
        loadingProfile.value = false;
      }
    }
  });
};

// 提交修改密码表单
const submitPasswordForm = async () => {
  if (!passwordFormRef.value) return;
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      loadingPassword.value = true;
      try {
        const payload = {
          oldPassword: passwordForm.oldPassword,
          newPassword: passwordForm.newPassword
        };
        // 使用Java后端API修改密码
        const response = await api.userJ.changePassword(payload);
        if (response && response.success) {
          ElMessage.success('密码修改成功!');
          passwordFormRef.value.resetFields(); // 清空密码表单
        } else {
          ElMessage.error(response.message || '修改密码失败');
        }
      } catch (error) {
        console.error("Error changing password:", error);
         let errorMessage = '修改密码时发生错误';
        if (error.response && error.response.data && error.response.data.message) {
            errorMessage = error.response.data.message;
        }
        ElMessage.error(errorMessage);
      } finally {
        loadingPassword.value = false;
      }
    }
  });
};

onMounted(() => {
  fetchUserProfile();
});

</script>

<style scoped>
.user-profile-container {
  padding: 20px;
  max-width: 800px; 
  margin: 0 auto;
}
.profile-card {
  /* 可以根据需要添加样式 */
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
