<template>
  <el-row :gutter="20">
    <el-col
      :xs="24" :sm="12" :md="8" :lg="6"
      v-for="(stat, index) in STATS"
      :key="stat.title"
      v-motion
      :initial="{ opacity: 0, y: 50, scale: 0.9 }"
      :enter="{ opacity: 1, y: 0, scale: 1, transition: { duration: 500, delay: index * 100, ease: 'easeOut' } }"
      data-aos="fade-up"
      :data-aos-delay="index * 100"
    >
      <el-card class="stat-card card-hover" :body-style="{ padding: '20px' }" @click="router.push(stat.path)">
        <div class="stat-content">
          <div class="stat-icon">
            <Icon :icon="stat.icon" class="stat-icon-svg" />
          </div>
          <div class="stat-info">
            <div class="stat-title">{{ stat.title }}</div>
            <div class="stat-value" :ref="el => valueRefs[index] = el">{{ values[index] }}</div>
          </div>
        </div>
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import api from '@/api';
import { animateNumber } from '@/utils/animations';

// 每张卡片的取数方式和点击后去的页面
const STATS = [
  {
    title: '题库总数',
    icon: 'mdi:database',
    path: '/subject-management',
    load: () => api.subjectAdminJ.getAll().then(subjects => subjects.length)
  },
  {
    title: '题目总数',
    icon: 'mdi:file-document-multiple',
    path: '/question-bank',
    load: () => api.questionsJ.queryQuestions({ page: 0, size: 1 }).then(page => page.totalElements)
  },
  {
    title: '学院总数',
    icon: 'mdi:school',
    path: '/college-management',
    load: () => api.collegeAdminJ.getAll().then(colleges => colleges.length)
  },
  {
    title: '学生总数',
    icon: 'mdi:account-group',
    path: '/student-management',
    // 只取人数，不必拉取整个学生列表
    load: () => api.studentJ.count().then(result => result.count)
  }
];

const router = useRouter();
const values = ref(STATS.map(() => 0));
const valueRefs = ref([]);

const loadStat = async (stat, index) => {
  try {
    const value = await stat.load();
    if (typeof value !== 'number') {
      return;
    }
    const oldValue = values.value[index];
    values.value[index] = value;
    await nextTick();
    if (valueRefs.value[index]) {
      animateNumber(valueRefs.value[index], oldValue, value, 1000);
    }
  } catch (error) {
    console.error(`获取${stat.title}失败:`, error);
  }
};

onMounted(() => {
  STATS.forEach(loadStat);
});
</script>

<style scoped>
.stat-card {
  border-radius: 16px;
  border: none;
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  background: white;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #667eea, #764ba2);
}

.stat-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.15);
}

.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100px;
  padding: 20px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  margin-right: 15px;
}

.stat-icon-svg {
  font-size: 2rem;
  color: white;
}

.stat-info {
  text-align: right;
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

@media (max-width: 768px) {
  .stat-content {
    flex-direction: column;
    text-align: center;
    height: auto;
    padding: 15px;
  }

  .stat-icon {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .stat-info {
    text-align: center;
  }
}
</style>
