<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">美食城管理</h3>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增美食城</el-button>
    </div>

    <el-table :data="malls" v-loading="loading" stripe>
      <el-table-column label="封面" width="100">
        <template #default="{ row }">
          <el-image v-if="row.coverImg" :src="imgUrl(row.coverImg)" fit="cover" class="cover" />
          <span v-else class="no-cover">暂无</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="businessHours" label="营业时间" width="140" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" active-text="营业"
                     inline-prompt @change="(value) => changeStatus(row, value)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑美食城' : '新增美食城'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如 青岛大学美食城" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" maxlength="200" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.businessHours" maxlength="64" placeholder="如 08:00-21:00" />
        </el-form-item>
        <el-form-item label="封面图">
          <div class="cover-upload">
            <el-image v-if="form.coverImg" :src="imgUrl(form.coverImg)" fit="cover" class="cover-preview" />
            <el-upload action="/api/file/upload" name="file" :headers="uploadHeaders()"
                       :show-file-list="false" :on-success="handleCoverSuccess"
                       :on-error="handleUploadError" accept="image/*">
              <el-button :icon="Upload">上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverImg" placeholder="也可直接填写图片地址" class="cover-input" />
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">营业</el-radio>
            <el-radio :value="0">关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { listMalls, createMall, updateMall, deleteMall } from '../../api/mall'
import { imgUrl, uploadHeaders } from '../../utils/format'

const loading = ref(false)
const saving = ref(false)
const malls = ref([])
const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  address: '',
  businessHours: '',
  coverImg: '',
  status: 1
})
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    malls.value = await listMalls()
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: null,
    name: '',
    address: '',
    businessHours: '',
    coverImg: '',
    status: 1
  })
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  resetForm()
  Object.assign(form, {
    id: row.id,
    name: row.name,
    address: row.address,
    businessHours: row.businessHours,
    coverImg: row.coverImg,
    status: row.status
  })
  dialogVisible.value = true
}

const handleCoverSuccess = (response) => {
  if (response && response.code === 0) {
    form.coverImg = response.data
  } else {
    ElMessage.error(response?.msg || '上传失败')
  }
}

const handleUploadError = () => ElMessage.error('上传失败')

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateMall(form.id, form)
    } else {
      await createMall(form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const changeStatus = async (row, value) => {
  try {
    await updateMall(row.id, { ...row, status: value })
    ElMessage.success(value === 1 ? '已上线' : '已下线')
  } catch (e) {
    load()
  }
}

const remove = async (row) => {
  await ElMessageBox.confirm(`确认删除美食城「${row.name}」？`, '提示', { type: 'warning' })
  try {
    await deleteMall(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) {
    // 失败提示由拦截器处理
  }
}

onMounted(load)
</script>

<style scoped>
.cover {
  width: 64px;
  height: 44px;
  border-radius: 6px;
}

.no-cover {
  color: #c0c4cc;
  font-size: 12px;
}

.cover-upload {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.cover-preview {
  width: 88px;
  height: 60px;
  border-radius: 6px;
}

.cover-input {
  width: 100%;
}
</style>
