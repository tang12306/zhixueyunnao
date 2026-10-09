<template>
  <div class="auto-quiz-index-container">
    <el-card>
      <template #header>
        <span>自动出题与组卷</span>
      </template>
      <p>请选择您的出题方式：</p>
      <div class="options-container">
        <router-link to="/auto-quiz/ai-create">
          <el-button type="primary" icon="el-icon-magic-stick" class="option-button">
            AI 智能出题
          </el-button>
        </router-link>
        
        <router-link to="/auto-quiz/ai-exam-generation">
          <el-button type="warning" icon="el-icon-document" class="option-button">
            AI 一键出卷
          </el-button>
        </router-link>
        
        <router-link to="/auto-quiz/select-from-bank"> 
          <el-button type="success" icon="el-icon-s-grid" class="option-button">
            从题库选题组卷
          </el-button>
        </router-link>
      </div>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'AutoQuizIndex',
};
</script>

<style scoped>
.auto-quiz-index-container {
  padding: 20px;
}
.options-container {
  margin-top: 20px;
  display: flex;
  gap: 20px; /* Adds space between buttons */
}
.option-button {
  font-size: 16px;
  padding: 15px 30px;
}
</style> 