<template>
  <div class="tag-editor">
    <el-tag v-for="tag in modelValue" :key="tag" closable class="tag" @close="removeTag(tag)">
      {{ tag }}
    </el-tag>
    <el-input
      v-if="inputVisible"
      ref="inputRef"
      v-model="inputValue"
      class="tag-input"
      size="small"
      @keyup.enter="confirmTag"
      @blur="confirmTag"
    />
    <el-button v-else size="small" @click="showInput">+ 添加标签</el-button>
    <div class="tag-hint">输入标签后按回车添加，多个标签用于分类和搜索</div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue';

const props = defineProps({
  modelValue: { type: Array, required: true }
});
const emit = defineEmits(['update:modelValue']);

const inputVisible = ref(false);
const inputValue = ref('');
const inputRef = ref(null);

const showInput = async () => {
  inputVisible.value = true;
  await nextTick();
  inputRef.value?.focus();
};

// 回车后输入框隐藏还会触发一次 blur，那时输入已清空，不会重复添加
const confirmTag = () => {
  const tag = inputValue.value.trim();
  if (tag && !props.modelValue.includes(tag)) {
    emit('update:modelValue', [...props.modelValue, tag]);
  }
  inputVisible.value = false;
  inputValue.value = '';
};

const removeTag = (tag) => {
  emit('update:modelValue', props.modelValue.filter(item => item !== tag));
};
</script>

<style scoped>
.tag {
  margin-right: 10px;
  margin-bottom: 10px;
}

.tag-input {
  width: 120px;
  vertical-align: bottom;
  margin-right: 10px;
}

.tag-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style>
