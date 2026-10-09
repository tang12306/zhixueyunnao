<template>
  <div>
    <h3>课程章节规划</h3>
    <el-form label-position="top">
      <el-form-item label="选择考试范围的章节（不选则覆盖整个科目）">
        <div class="chapter-selection">
          <el-checkbox
            v-model="examData.allChapters"
            :disabled="chapterList.length === 0"
            @change="toggleAllChapters"
          >全选</el-checkbox>
          <div class="chapter-list">
            <el-checkbox-group v-model="examData.chapterIds">
              <el-checkbox v-for="chapter in chapterList" :key="chapter.id" :label="chapter.id">
                {{ chapter.name }}
              </el-checkbox>
            </el-checkbox-group>
            <span v-if="chapterList.length === 0" class="tip">该科目还没有章节</span>
          </div>
        </div>
        <div class="summary">
          <p>已选章节: {{ examData.chapterIds.length }} 章</p>
        </div>
      </el-form-item>

      <el-form-item label="章节分布说明">
        <el-input
          v-model="examData.chapterDistribution"
          type="textarea"
          placeholder="请描述各章节题目的分布要求，例如：第一章占30%，第二章占20%..."
          :rows="3"
          maxlength="1000"
        />
      </el-form-item>

      <el-form-item label="考试要求">
        <el-input
          v-model="examData.examRequirements"
          type="textarea"
          placeholder="请输入对考试的特殊要求，例如：题目难度、覆盖知识点等"
          :rows="3"
          maxlength="1000"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { useExamForm } from './useExamForm';

const { examData, chapterList, toggleAllChapters } = useExamForm();
</script>

<style scoped>
.chapter-selection {
  margin: 15px 0;
}

.chapter-list {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.summary {
  margin-top: 15px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.tip {
  color: #909399;
  font-size: 14px;
}
</style>
