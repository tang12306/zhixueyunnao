<template>
  <div class="navbar">
    <!-- 面包屑导航 -->
    <el-breadcrumb separator="/">
      <el-breadcrumb-item v-for="item in breadcrumbItems" :key="item.path" :to="{ path: item.path }">
        {{ item.meta.title }}
      </el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 右侧菜单 -->
    <div class="right-menu">
      <el-dropdown @command="handleCommand">
        <span class="el-dropdown-link">
          <!-- 显示用户名，后续从store获取 -->
          张三<el-icon class="el-icon--right"><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown } from '@element-plus/icons-vue'; // 确保已安装 @element-plus/icons-vue

const route = useRoute();
const router = useRouter();

// 计算面包屑导航项
const breadcrumbItems = computed(() => {
  // route.matched 是一个包含当前路由所有嵌套路径片段的数组
  return route.matched.filter(item => item.meta && item.meta.title);
});

const handleCommand = (command) => {
  if (command === 'logout') {
    // 执行退出登录逻辑，清除token，跳转到登录页
    localStorage.removeItem('token');
    console.log('User logout');
    router.push('/login');
  } else if (command === 'profile') {
    // 跳转到个人中心页面
    router.push('/profile');
  }
};

// 如果Element Plus图标报错，请确保您已在 frontend/ 目录下执行 npm install @element-plus/icons-vue
</script>

<style scoped>
.navbar {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
}

.right-menu {
  display: flex;
  align-items: center;
}

.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}
</style> 