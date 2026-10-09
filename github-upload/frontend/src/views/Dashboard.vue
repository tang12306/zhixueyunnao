<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="24">
        <div
          class="dashboard-header"
          v-motion
          :initial="{ opacity: 0, y: -30 }"
          :enter="{ opacity: 1, y: 0, transition: { duration: 600 } }"
          data-aos="fade-down"
        >
          <div class="header-content">
            <!-- 校徽图片 - 请将校徽文件放在 src/assets/ 目录下 -->
            <img src="@/assets/logo.png" alt="江苏师范大学校徽" class="header-logo" v-if="logoExists" @error="logoExists = false"/>
            <Icon icon="mdi:view-dashboard" class="dashboard-icon" v-if="!logoExists"/>
            <div class="header-text">
              <h2 class="dashboard-title">智慧教学云脑系统</h2>
              <p class="dashboard-subtitle">欢迎使用江苏师范大学智慧教学云脑系统，基于AI技术的智能化教学管理平台。</p>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <StatCards class="mt-20" />

    <el-row :gutter="20" class="mt-20">
      <el-col
        :xs="24" :lg="16"
        v-motion
        :initial="{ opacity: 0, x: -50 }"
        :enter="{ opacity: 1, x: 0, transition: { duration: 600, delay: 400 } }"
        data-aos="fade-right"
        data-aos-delay="400"
      >
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <Icon icon="mdi:clock-outline" class="header-icon" />
                <span>最近添加的题目</span>
              </div>
              <el-button link @click="router.push('/question-bank')" class="btn-animate">
                查看全部
                <Icon icon="mdi:arrow-right" class="ml-1" />
              </el-button>
            </div>
          </template>
          <RecentQuestionsTable />
        </el-card>
      </el-col>

      <el-col
        :xs="24" :lg="8"
        v-motion
        :initial="{ opacity: 0, x: 50 }"
        :enter="{ opacity: 1, x: 0, transition: { duration: 600, delay: 600 } }"
        data-aos="fade-left"
        data-aos-delay="600"
      >
        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <Icon icon="mdi:lightning-bolt" class="header-icon" />
                <span>快速操作</span>
              </div>
            </div>
          </template>
          <div class="quick-actions">
            <el-button
              v-for="action in QUICK_ACTIONS"
              :key="action.path"
              :type="action.type"
              @click="router.push(action.path)"
              class="action-btn btn-animate"
              v-motion
              :initial="{ scale: 1 }"
              :hover="{ scale: 1.05 }"
              :tap="{ scale: 0.95 }"
            >
              <Icon :icon="action.icon" class="mr-1" />
              {{ action.label }}
            </el-button>
          </div>
        </el-card>

        <el-card
          class="mt-20 content-card"
          v-motion
          :initial="{ opacity: 0, y: 30 }"
          :enter="{ opacity: 1, y: 0, transition: { duration: 600, delay: 800 } }"
          data-aos="fade-up"
          data-aos-delay="800"
        >
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <Icon icon="mdi:chart-pie" class="header-icon" />
                <span>科目分布</span>
              </div>
            </div>
          </template>
          <SubjectChart />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import StatCards from './dashboard/StatCards.vue';
import RecentQuestionsTable from './dashboard/RecentQuestionsTable.vue';
import SubjectChart from './dashboard/SubjectChart.vue';

const QUICK_ACTIONS = [
  { label: '创建题目', icon: 'mdi:plus', type: 'primary', path: '/question-bank/create' },
  { label: '智能组卷', icon: 'mdi:auto-fix', type: 'success', path: '/auto-quiz/index' },
  { label: 'AI助手', icon: 'mdi:robot', type: 'info', path: '/auto-quiz/ai-create' }
];

const router = useRouter();

// 校徽图片加载失败时改用图标
const logoExists = ref(true);
</script>

<style scoped>
.dashboard {
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 60px);
}

.mt-20 {
  margin-top: 20px;
}

/* 头部样式 */
.dashboard-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 30px;
  color: white;
  margin-bottom: 20px;
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.3);
}

.header-content {
  display: flex;
  align-items: center;
}

/* 校徽图片样式 */
.header-logo {
  height: 80px;
  width: auto;
  margin-right: 20px;
  opacity: 0.95;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
}

.dashboard-icon {
  font-size: 3rem;
  margin-right: 20px;
  opacity: 0.9;
}

.dashboard-title {
  margin: 0 0 10px 0;
  font-size: 2.2rem;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.dashboard-subtitle {
  margin: 0;
  font-size: 1.1rem;
  opacity: 0.9;
  line-height: 1.6;
}

/* 内容卡片样式 */
.content-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  background: white;
}

.content-card:hover {
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #333;
}

.header-icon {
  margin-right: 8px;
  font-size: 1.2rem;
  color: #667eea;
}

/* 快速操作按钮 */
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

/* el-button 之间默认有左边距，竖排时去掉 */
.quick-actions .action-btn + .action-btn {
  margin-left: 0;
}

.action-btn {
  width: 100%;
  height: 50px;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border: none;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s;
}

.action-btn:hover::before {
  left: 100%;
}

/* 工具类 */
.mr-1 {
  margin-right: 4px;
}

.ml-1 {
  margin-left: 4px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard {
    padding: 10px;
  }

  .dashboard-header {
    padding: 20px;
  }

  .dashboard-title {
    font-size: 1.8rem;
  }

  .dashboard-subtitle {
    font-size: 1rem;
  }

  .header-content {
    flex-direction: column;
    text-align: center;
  }

  .dashboard-icon {
    margin-right: 0;
    margin-bottom: 15px;
  }
}
</style>
