<template>
  <div class="image-upload">
    <el-image v-if="modelValue" :src="imgUrl(modelValue)" fit="cover" class="preview"
              :preview-src-list="[imgUrl(modelValue)]" preview-teleported />
    <div class="upload-actions">
      <el-upload action="/api/file/upload" name="file" :headers="uploadHeaders()" :show-file-list="false"
                 accept="image/*" :before-upload="handleBeforeUpload"
                 :on-success="handleSuccess" :on-error="handleError">
        <el-button :icon="Upload" :loading="uploading">
          {{ modelValue ? '重新上传' : '上传图片' }}
        </el-button>
      </el-upload>
      <el-button v-if="modelValue" link type="danger" @click="clear">移除</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import { imgUrl, uploadHeaders } from '../utils/format'

defineProps({
  modelValue: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['update:modelValue'])
const uploading = ref(false)

const handleBeforeUpload = () => {
  uploading.value = true
  return true
}

const handleSuccess = (response) => {
  uploading.value = false
  if (response && response.code === 0 && response.data) {
    emit('update:modelValue', response.data)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(response?.msg || response?.message || '上传失败')
  }
}

const handleError = () => {
  uploading.value = false
  ElMessage.error('上传失败，请稍后重试')
}

const clear = () => {
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.preview {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  background: #fafafa;
}

.upload-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
