<template>
  <div class="user-profile-container">
    <!-- 认证状态提示 -->
    <el-alert
      v-if="!isAuthenticated"
      title="未登录状态"
      type="warning"
      description="您当前未登录，无法查看和编辑个人信息。请先登录系统。"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    >
      <template #default>
        <div style="display: flex; align-items: center; justify-content: space-between;">
          <span>您当前未登录，无法查看和编辑个人信息。</span>
          <el-button type="primary" size="small" @click="goToLogin">前往登录</el-button>
        </div>
      </template>
    </el-alert>

    <!-- 后端认证提示 -->
    <el-alert
      v-if="isAuthenticated && needsBackendAuth"
      title="需要后端系统认证"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom: 20px;"
    >
      <template #default>
        <div>
          <p style="margin: 0 0 10px 0;">您已在前端系统登录，但个人信息功能需要在后端系统中进行认证。</p>
          <div style="display: flex; gap: 10px;">
            <el-button type="primary" size="small" @click="goToBackendLogin">前往后端登录</el-button>
            <el-button size="small" @click="refreshUserInfo">重新获取信息</el-button>
          </div>
        </div>
      </template>
    </el-alert>

    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
          <el-tag v-if="isAuthenticated" type="success" size="small">已登录</el-tag>
          <el-tag v-else type="warning" size="small">未登录</el-tag>
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
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api'; // 确保路径正确

const router = useRouter();
const activeTab = ref('info');
const profileFormRef = ref(null);
const passwordFormRef = ref(null);
const loadingProfile = ref(false);
const loadingPassword = ref(false);

// 认证状态
const isAuthenticated = computed(() => !!localStorage.getItem('token'));
const needsBackendAuth = ref(false);

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
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmNewPassword: [
    { required: true, validator: validatePassConfirm, trigger: 'blur' }
  ]
};

// 获取当前用户信息
const fetchUserProfile = async () => {
  try {
    // 检查是否有认证token
    const token = localStorage.getItem('token');
    if (!token) {
      ElMessage.warning('请先登录以查看个人信息');
      return;
    }

    console.log('获取用户信息...');
    const response = await api.userJ.getCurrentUser();
    console.log('用户信息响应:', response);

    // 检查是否返回了HTML登录页面（说明需要Spring Boot认证）
    if (typeof response === 'string' && response.includes('<!DOCTYPE html>')) {
      console.log('检测到Spring Boot登录页面，需要后端认证');
      needsBackendAuth.value = true;
      ElMessage.warning('需要在后端系统中登录才能查看个人信息');
      return;
    }

    // 如果成功获取到用户信息，重置后端认证状态
    needsBackendAuth.value = false;

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

    // 如果是认证错误，显示友好提示
    if (error.response && error.response.status === 401) {
      ElMessage.warning('登录状态已过期，请重新登录');
      // 可以选择跳转到登录页面
      // router.push('/login');
    } else {
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

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login');
};

// 跳转到后端登录页面
const goToBackendLogin = () => {
  // 在新窗口打开后端登录页面
  window.open('http://localhost:8080/login', '_blank');
  ElMessage.info('请在新窗口中完成后端系统登录，然后返回此页面刷新信息');
};

// 重新获取用户信息
const refreshUserInfo = () => {
  needsBackendAuth.value = false;
  fetchUserProfile();
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
