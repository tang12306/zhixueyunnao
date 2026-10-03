<template>
  <router-view />
</template>

<style>
body {
  margin: 0;
  padding: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  transition: all 0.3s ease;
}

#app {
  height: 100%;
  min-height: 100vh;
}

.container {
  padding: 20px;
  animation: fadeInUp 0.6s ease-out;
}

/* 全局动画类 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.5s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-fade-enter-active {
  transition: all 0.3s ease-out;
}

.slide-fade-leave-active {
  transition: all 0.8s cubic-bezier(1.0, 0.5, 0.8, 1.0);
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(20px);
  opacity: 0;
}

/* 卡片悬停效果 */
.card-hover {
  transition: all 0.3s ease;
  cursor: pointer;
}

.card-hover:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
}

/* 按钮动画效果 */
.btn-animate {
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.btn-animate::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.btn-animate:hover::before {
  left: 100%;
}

/* 加载动画 */
.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 页面切换动画 */
.page-enter-active,
.page-leave-active {
  transition: all 0.4s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(30px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-30px);
}

/* 自定义 NProgress 样式 */
#nprogress .bar {
  background: #409eff !important;
  height: 3px !important;
}

#nprogress .peg {
  box-shadow: 0 0 10px #409eff, 0 0 5px #409eff !important;
}

/* 全局选择框样式优化 */
.el-select {
  min-width: 120px !important;
}

/* 表单中的选择框样式 */
.el-form-item .el-select {
  min-width: 150px;
}

/* 内联表单中的选择框样式 */
.el-form--inline .el-form-item .el-select {
  min-width: 140px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .container {
    padding: 10px;
  }

  /* 移动端选择框样式 */
  .el-form-item .el-select {
    min-width: 120px;
  }
}
</style>