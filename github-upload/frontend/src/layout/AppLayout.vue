<template>
  <el-container class="app-layout">
    <el-aside width="200px" class="sidebar-container">
      <Sidebar />
    </el-aside>
    <el-container>
      <el-header class="navbar-container">
        <Navbar />
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import Sidebar from './Sidebar.vue';
import Navbar from './Navbar.vue';
// 如果仍然报错找不到 vue, vue-router, 请确保在 frontend 目录下执行了 npm install 并重启IDE
// import { computed } from 'vue'; // 示例：如果需要从 'vue' 导入
// import { useRoute } from 'vue-router'; // 示例：如果需要从 'vue-router' 导入
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.sidebar-container {
  background-color: #304156;
  color: #bfcbd9;
  /* transition: width 0.28s; */ /* 可选的侧边栏展开/收起动画 */
}

.navbar-container {
  height: 50px;
  line-height: 50px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, .08);
  padding: 0; /* 通常 Navbar 内部会自己处理 padding */
}

.main-content {
  padding: 20px;
  background-color: #f0f2f5;
  /* 根据需要添加 overflow-y: auto; */
}
</style> 