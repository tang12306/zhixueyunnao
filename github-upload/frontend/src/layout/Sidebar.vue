<template>
  <div class="sidebar-container">
    <div
      class="sidebar-logo-container"
      v-motion
      :initial="{ opacity: 0, x: -20 }"
      :enter="{ opacity: 1, x: 0, transition: { duration: 600 } }"
    >
      <router-link to="/" class="logo-link">
        <div class="logo-content">
          <Icon icon="mdi:school" class="logo-icon" />
          <h1 class="sidebar-title" v-show="!isCollapse">教务系统</h1>
        </div>
      </router-link>
    </div>

    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        class="el-menu-vertical-demo animated-menu"
        :collapse="isCollapse"
        background-color="transparent"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <!-- 主要菜单项 -->
        <el-menu-item
          index="/dashboard"
          class="menu-item-animated"
          v-motion
          :initial="{ opacity: 0, x: -30 }"
          :enter="{ opacity: 1, x: 0, transition: { duration: 400, delay: 100 } }"
        >
          <Icon icon="mdi:view-dashboard" class="menu-icon" />
          <template #title>
            <span class="menu-title">控制台</span>
          </template>
        </el-menu-item>

        <el-menu-item
          index="/question-bank"
          class="menu-item-animated"
          v-motion
          :initial="{ opacity: 0, x: -30 }"
          :enter="{ opacity: 1, x: 0, transition: { duration: 400, delay: 200 } }"
        >
          <Icon icon="mdi:book-multiple" class="menu-icon" />
          <template #title>
            <span class="menu-title">题库管理</span>
          </template>
        </el-menu-item>

        <el-menu-item
          index="/auto-quiz/index"
          class="menu-item-animated"
          v-motion
          :initial="{ opacity: 0, x: -30 }"
          :enter="{ opacity: 1, x: 0, transition: { duration: 400, delay: 300 } }"
        >
          <Icon icon="mdi:auto-fix" class="menu-icon" />
          <template #title>
            <span class="menu-title">智能组卷</span>
          </template>
        </el-menu-item>

        <!-- 管理菜单 -->
        <el-sub-menu
          index="management"
          class="submenu-animated"
          v-motion
          :initial="{ opacity: 0, x: -30 }"
          :enter="{ opacity: 1, x: 0, transition: { duration: 400, delay: 400 } }"
        >
          <template #title>
            <Icon icon="mdi:cog" class="menu-icon" />
            <span class="menu-title">系统管理</span>
          </template>
          <el-menu-item index="/management/subjects" class="submenu-item">
            <Icon icon="mdi:book-open-variant" class="submenu-icon" />
            <span>科目管理</span>
          </el-menu-item>
          <el-menu-item index="/management/chapters" class="submenu-item">
            <Icon icon="mdi:format-list-numbered" class="submenu-icon" />
            <span>章节管理</span>
          </el-menu-item>
          <el-menu-item index="/student-management" class="submenu-item">
            <Icon icon="mdi:account-group" class="submenu-icon" />
            <span>学生管理</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 组织管理 -->
        <el-sub-menu
          index="organization"
          class="submenu-animated"
          v-motion
          :initial="{ opacity: 0, x: -30 }"
          :enter="{ opacity: 1, x: 0, transition: { duration: 400, delay: 500 } }"
        >
          <template #title>
            <Icon icon="mdi:office-building" class="menu-icon" />
            <span class="menu-title">组织管理</span>
          </template>
          <el-menu-item index="/organization/colleges" class="submenu-item">
            <Icon icon="mdi:school" class="submenu-icon" />
            <span>学院管理</span>
          </el-menu-item>
          <el-menu-item index="/organization/majors" class="submenu-item">
            <Icon icon="mdi:book-education" class="submenu-icon" />
            <span>专业管理</span>
          </el-menu-item>
          <el-menu-item index="/organization/classes" class="submenu-item">
            <Icon icon="mdi:google-classroom" class="submenu-icon" />
            <span>班级管理</span>
          </el-menu-item>
        </el-sub-menu>

        <el-menu-item
          index="/settings"
          class="menu-item-animated"
          v-motion
          :initial="{ opacity: 0, x: -30 }"
          :enter="{ opacity: 1, x: 0, transition: { duration: 400, delay: 600 } }"
        >
          <Icon icon="mdi:settings" class="menu-icon" />
          <template #title>
            <span class="menu-title">系统设置</span>
          </template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

// 根据当前路由计算激活的菜单项
const activeMenu = computed(() => {
  const { meta, path } = route;
  if (meta.activeMenu) {
    return meta.activeMenu;
  }
  return path;
});

// 控制侧边栏是否折叠，可以后续通过 Pinia store 或 props 控制
const isCollapse = ref(false);

// 如果Element Plus图标报错，请确保您已在 frontend/ 目录下执行 npm install @element-plus/icons-vue
</script>

<style scoped>
.sidebar-container {
  height: 100%;
  background: linear-gradient(180deg, #2c3e50 0%, #34495e 100%);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
}

.sidebar-logo-container {
  position: relative;
  width: 100%;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.logo-link {
  text-decoration: none;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-content {
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.logo-icon {
  font-size: 2rem;
  color: white;
  margin-right: 10px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.sidebar-title {
  margin: 0;
  color: white;
  font-weight: 600;
  font-size: 1.2rem;
  font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.animated-menu {
  border-right: none;
  background: transparent;
}

.el-menu-vertical-demo:not(.el-menu--collapse) {
  width: 220px;
  min-height: calc(100vh - 60px);
}

.menu-item-animated,
.submenu-animated {
  margin: 4px 8px;
  border-radius: 8px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.menu-item-animated::before,
.submenu-animated::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(102, 126, 234, 0.1), transparent);
  transition: left 0.5s;
}

.menu-item-animated:hover::before,
.submenu-animated:hover::before {
  left: 100%;
}

.menu-item-animated:hover,
.submenu-animated:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateX(5px);
}

.menu-item-animated.is-active {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.menu-item-animated.is-active .menu-icon,
.menu-item-animated.is-active .menu-title {
  color: white;
}

.menu-icon {
  font-size: 1.2rem;
  margin-right: 8px;
  color: #bfcbd9;
  transition: all 0.3s ease;
}

.menu-title {
  font-weight: 500;
  color: #bfcbd9;
  transition: all 0.3s ease;
}

.submenu-item {
  margin: 2px 4px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.submenu-item:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateX(3px);
}

.submenu-icon {
  font-size: 1rem;
  margin-right: 6px;
  color: #a0a8b8;
}

.scrollbar-wrapper {
  height: calc(100% - 60px);
  overflow-x: hidden !important;
}

/* 折叠状态样式 */
.el-menu--collapse .logo-content {
  justify-content: center;
}

.el-menu--collapse .logo-icon {
  margin-right: 0;
}

.el-menu--collapse .menu-item-animated,
.el-menu--collapse .submenu-animated {
  margin: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .el-menu-vertical-demo:not(.el-menu--collapse) {
    width: 200px;
  }

  .sidebar-title {
    font-size: 1rem;
  }

  .logo-icon {
    font-size: 1.5rem;
  }
}

/* 菜单项动画 */
.menu-item-animated,
.submenu-animated {
  animation: slideInLeft 0.3s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 子菜单展开动画 */
.el-sub-menu .el-menu {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  margin: 4px;
  overflow: hidden;
}

.el-sub-menu .el-menu-item {
  background: transparent;
  border-radius: 4px;
  margin: 2px 4px;
}

/* 滚动条样式 */
.scrollbar-wrapper :deep(.el-scrollbar__bar) {
  opacity: 0.3;
}

.scrollbar-wrapper :deep(.el-scrollbar__thumb) {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
}

.scrollbar-wrapper:hover :deep(.el-scrollbar__bar) {
  opacity: 0.6;
}
</style>