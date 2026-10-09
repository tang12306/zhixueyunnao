<template>
  <div class="options-editor">
    <div v-for="(option, index) in modelValue" :key="index" class="option-item">
      <span class="option-letter">{{ optionLetter(index) }}.</span>
      <el-input
        :model-value="option.text"
        placeholder="选项内容"
        class="option-input"
        @update:model-value="text => updateOption(index, { text })"
      />
      <el-checkbox
        :model-value="option.isCorrect"
        border
        class="option-checkbox"
        @update:model-value="checked => setCorrect(index, checked)"
      >
        正确答案
      </el-checkbox>
      <el-button
        type="danger"
        :icon="Delete"
        circle
        size="small"
        :disabled="modelValue.length <= 2"
        @click="removeOption(index)"
      />
    </div>
    <el-button type="primary" size="small" plain :disabled="modelValue.length >= MAX_OPTIONS" @click="addOption">
      添加选项
    </el-button>
  </div>
</template>

<script setup>
import { Delete } from '@element-plus/icons-vue';
import { optionLetter } from '@/utils/questionTypes';
import { MAX_OPTIONS, blankOption } from './questionForm';

const props = defineProps({
  // [{ text, isCorrect }]
  modelValue: { type: Array, required: true },
  // 单选题：勾选一个正确答案时取消其它的
  single: { type: Boolean, default: false }
});
const emit = defineEmits(['update:modelValue']);

const updateOption = (index, changes) => {
  emit('update:modelValue', props.modelValue.map((option, i) => (i === index ? { ...option, ...changes } : option)));
};

const setCorrect = (index, checked) => {
  emit('update:modelValue', props.modelValue.map((option, i) => {
    if (i === index) {
      return { ...option, isCorrect: checked };
    }
    return props.single && checked ? { ...option, isCorrect: false } : option;
  }));
};

const addOption = () => {
  emit('update:modelValue', [...props.modelValue, blankOption()]);
};

const removeOption = (index) => {
  emit('update:modelValue', props.modelValue.filter((_, i) => i !== index));
};
</script>

<style scoped>
.options-editor {
  width: 100%;
}

.option-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.option-letter {
  width: 24px;
  font-weight: bold;
}

.option-input {
  flex: 1;
  margin-right: 10px;
}

.option-checkbox {
  margin-right: 10px;
}
</style>
