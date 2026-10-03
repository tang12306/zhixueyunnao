<template>
  <div class="student-layout">
    <el-container class="layout-container">
      <el-header class="header">
        <div class="header-left">
          <h2 class="logo">教务题库管理系统 - 学生端</h2>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-profile">
              <el-avatar :size="32" icon="el-icon-user"></el-avatar>
              <span class="username">{{ username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-container>
        <el-aside width="220px" class="sidebar">
          <el-menu
            :default-active="activeMenu"
            class="el-menu-vertical"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            router
          >
            <el-menu-item index="/student/exams">
              <el-icon><Document /></el-icon>
              <span>我的考试</span>
            </el-menu-item>
            <el-menu-item index="/student/scores">
              <el-icon><Reading /></el-icon>
              <span>成绩查询</span>
            </el-menu-item>
            <el-menu-item index="/student/profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        
        <el-main class="main-content">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, Reading, User } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 模拟用户名，实际应从API或store获取
const username = ref('学生用户')

// 当前激活的菜单项
const activeMenu = computed(() => {
  return route.path
})

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    // 清除登录状态
    localStorage.removeItem('token')
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/student/profile')
  }
}
</script>

<style scoped>
.student-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-container {
  height: 100%;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.username {
  margin-left: 8px;
  color: #606266;
}

.sidebar {
  background-color: #304156;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.main-content {
  background-color: #f0f2f5;
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
}

.el-menu-vertical {
  border-right: none;
}
</style> 