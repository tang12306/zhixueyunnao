<template>
  <div>
    <h3>考试基本信息</h3>
    <el-form ref="formRef" :model="examData" :rules="rules" label-position="top">
      <el-form-item label="考试名称" prop="name">
        <el-input v-model="examData.name" placeholder="请输入考试名称" maxlength="50" show-word-limit />
      </el-form-item>

      <el-form-item label="考试描述（可选）" prop="description">
        <el-input
          v-model="examData.description"
          type="textarea"
          placeholder="会显示在试卷标题下方"
          :rows="3"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="考试时长(分钟)" prop="duration">
            <el-input-number v-model="examData.duration" :min="30" :max="180" :step="15" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="考试总分" prop="totalScore">
            <el-input-number v-model="examData.totalScore" :min="10" :max="300" :step="10" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useExamForm } from './useExamForm';

const { examData } = useExamForm();
const formRef = ref(null);

const rules = {
  name: [{ required: true, message: '请输入考试名称', trigger: 'blur' }],
  duration: [{ required: true, message: '请设定考试时长', trigger: 'blur' }],
  totalScore: [{ required: true, message: '请设定考试总分', trigger: 'blur' }]
};

/** 页面点“下一步”前调用，校验不通过时返回 false */
const validate = () => formRef.value.validate().catch(() => false);

defineExpose({ validate });
</script>
