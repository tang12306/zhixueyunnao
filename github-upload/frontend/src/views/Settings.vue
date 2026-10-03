<template>
  <div class="app-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统设置</span>
        </div>
      </template>

      <div v-if="listLoading" class="loading-container">
        <el-skeleton :rows="10" animated />
      </div>

      <div v-if="!listLoading && errorMsg" class="error-container">
        <el-alert :title="'加载设置失败'" :description="errorMsg" type="error" show-icon />
      </div>

      <div v-if="!listLoading && !errorMsg && groupedSettings.length === 0" class="empty-container">
        <el-empty description="暂无可配置的系统设置" />
      </div>

      <div v-if="!listLoading && !errorMsg && groupedSettings.length > 0">
        <el-tabs v-model="activeCategory" type="border-card">
          <el-tab-pane
            v-for="group in groupedSettings"
            :key="group.category"
            :label="group.category || '其他设置'"
            :name="group.category || 'other'"
          >
            <el-form label-width="180px" label-position="left">
              <el-form-item
                v-for="setting in group.settings"
                :key="setting.key"
                :label="setting.name"
                class="setting-item"
              >
                <div class="setting-item-content">
                  <el-input
                    v-model="editableSettings[setting.key]"
                    :placeholder="setting.description"
                    :disabled="!setting.isEditable || isSaving[setting.key]"
                    clearable
                  />
                  <div class="actions">
                    <el-button
                      type="primary"
                      @click="saveSetting(setting.key)"
                      :loading="isSaving[setting.key]"
                      :disabled="!setting.isEditable || editableSettings[setting.key] === originalSettings[setting.key]"
                      size="small"
                    >
                      保存
                    </el-button>
                    <el-button
                      @click="revertSetting(setting.key)"
                      :disabled="!setting.isEditable || editableSettings[setting.key] === originalSettings[setting.key] || isSaving[setting.key]"
                      size="small"
                    >
                      撤销
                    </el-button>
                  </div>
                </div>
                <div class="setting-description" v-if="setting.description">
                  {{ setting.description }}
                </div>
              </el-form-item>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue';
import api from '@/api'; // Assuming your API setup is in @/api
import { ElMessage, ElNotification } from 'element-plus';

const listLoading = ref(true);
const settings = ref([]);
const originalSettings = reactive({}); // Store original values for revert
const editableSettings = reactive({}); // For two-way binding in form
const isSaving = reactive({}); // Loading state for individual save buttons
const errorMsg = ref('');
const activeCategory = ref('');

const fetchSettings = async () => {
  listLoading.value = true;
  errorMsg.value = '';
  try {
    // Use the new javaSettingsAPI
    const response = await api.settingsJ.getSettings(); 
    if (Array.isArray(response)) {
      settings.value = response;
      response.forEach(setting => {
        originalSettings[setting.key] = setting.value;
        editableSettings[setting.key] = setting.value;
        isSaving[setting.key] = false;
      });
      if (groupedSettings.value.length > 0) {
        activeCategory.value = groupedSettings.value[0].category || 'other';
      }
    } else {
      console.error("Unexpected response format for settings:", response);
      errorMsg.value = "获取设置项失败：返回格式不正确。";
      settings.value = [];
    }
  } catch (err) {
    console.error("Error fetching settings:", err);
    errorMsg.value = err.response?.data?.message || err.message || '加载设置时发生未知网络错误。';
    settings.value = [];
  } finally {
    listLoading.value = false;
  }
};

const saveSetting = async (key) => {
  if (!editableSettings[key] && editableSettings[key] !== '') { // Allow empty string
    ElMessage.warning('设置值不能为空');
    return;
  }
  isSaving[key] = true;
  try {
    // Use the new javaSettingsAPI.updateSetting(key, value)
    // The Java API expects { value: "..." } in the payload
    await api.settingsJ.updateSetting(key, editableSettings[key]);
    originalSettings[key] = editableSettings[key]; // Update original value upon successful save
    ElNotification({
      title: '成功',
      message: `设置项 '${settings.value.find(s => s.key === key)?.name || key}' 已保存。`,
      type: 'success',
      duration: 2000,
    });
  } catch (err) {
    console.error(`Error saving setting ${key}:`, err);
    ElMessage.error(err.response?.data?.message || `保存设置项 ${key} 失败`);
    // Optionally revert on error, or let user retry
    // editableSettings[key] = originalSettings[key]; 
  } finally {
    isSaving[key] = false;
  }
};

const revertSetting = (key) => {
  editableSettings[key] = originalSettings[key];
  ElMessage.info(`设置项 '${settings.value.find(s => s.key === key)?.name || key}' 的更改已撤销。`);
};

const groupedSettings = computed(() => {
  const groups = {};
  settings.value.forEach(setting => {
    const category = setting.category || '其他设置';
    if (!groups[category]) {
      groups[category] = [];
    }
    groups[category].push(setting);
  });
  return Object.keys(groups).map(category => ({
    category,
    settings: groups[category],
  }));
});

onMounted(() => {
  fetchSettings();
});

</script>

<style scoped>
.app-container {
  padding: 20px;
}
.loading-container, .error-container, .empty-container {
  margin-top: 20px;
}
.setting-item {
  margin-bottom: 22px; /* Increased margin for better separation */
}
.setting-item-content {
  display: flex;
  align-items: center;
}
.setting-item-content .el-input {
  flex-grow: 1;
  margin-right: 10px;
}
.actions .el-button {
  margin-left: 8px;
}
.setting-description {
  font-size: 0.85em;
  color: #888;
  margin-top: 5px;
  padding-left: 5px; /* Align with input text if possible */
}
.el-tabs--border-card > .el-tabs__content {
    padding: 20px;
}
</style>
