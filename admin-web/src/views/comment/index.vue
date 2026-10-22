<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">评价处理</h3>
      <span class="sub-text">平台回复与违规评价隐藏（隐藏需填写原因）</span>
    </div>

    <div class="table-toolbar">
      <el-radio-group v-model="quick" @change="search">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="bad">差评（1-2星）</el-radio-button>
        <el-radio-button value="mid">中评（3星）</el-radio-button>
        <el-radio-button value="good">好评（4-5星）</el-radio-button>
        <el-radio-button value="hidden">已隐藏</el-radio-button>
      </el-radio-group>
      <el-input v-model="keyword" placeholder="评价内容关键词" clearable style="width: 200px"
                @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="shopName" label="档口" width="130" show-overflow-tooltip />
      <el-table-column prop="userName" label="用户" width="110" show-overflow-tooltip />
      <el-table-column label="评分" width="150">
        <template #default="{ row }">
          <el-rate :model-value="row.score" disabled />
        </template>
      </el-table-column>
      <el-table-column label="评价内容" min-width="220">
        <template #default="{ row }">
          <div class="content">{{ row.content || '（无文字内容）' }}</div>
          <div v-if="row.images" class="images">
            <el-image v-for="(img, index) in row.images.split(',')" :key="index" :src="imgUrl(img)"
                      fit="cover" class="thumb" :preview-src-list="row.images.split(',').map(imgUrl)"
                      :initial-index="index" preview-teleported />
          </div>
        </template>
      </el-table-column>
      <el-table-column label="回复记录" min-width="180">
        <template #default="{ row }">
          <div v-if="row.replyContent" class="reply">
            <span class="reply-label">商家：</span>{{ row.replyContent }}
          </div>
          <div v-if="row.adminReply" class="reply">
            <span class="reply-label admin">平台：</span>{{ row.adminReply }}
          </div>
          <span v-if="!row.replyContent && !row.adminReply" class="sub-text">暂无回复</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag v-if="row.hidden === 1" type="danger">已隐藏</el-tag>
          <el-tag v-else type="success" effect="plain">正常</el-tag>
          <div v-if="row.hidden === 1 && row.hiddenReason" class="sub-text">{{ row.hiddenReason }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="评价时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReply(row)">回复</el-button>
          <el-button v-if="row.hidden === 1" link type="success" @click="unhide(row)">恢复</el-button>
          <el-button v-else link type="danger" @click="hide(row)">隐藏</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="replyVisible" title="平台回复" width="520px">
      <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="200" show-word-limit
                placeholder="请输入平台回复内容，用户端评价列表中可见" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReply">提交回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listComments, replyComment, hideComment } from '../../api/comment'
import { imgUrl } from '../../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const quick = ref('all')
const keyword = ref('')

const replyVisible = ref(false)
const replyContent = ref('')
const replyRow = ref(null)

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '')

const quickParams = () => ({
  all: {},
  bad: { maxScore: 2 },
  mid: { minScore: 3, maxScore: 3 },
  good: { minScore: 4 },
  hidden: { hidden: 1 }
}[quick.value])

const load = async () => {
  loading.value = true
  try {
    const data = await listComments({
      ...quickParams(),
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const openReply = (row) => {
  replyRow.value = row
  replyContent.value = row.adminReply || ''
  replyVisible.value = true
}

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  saving.value = true
  try {
    await replyComment(replyRow.value.id, replyContent.value.trim())
    ElMessage.success('回复成功')
    replyVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const hide = async (row) => {
  const result = await ElMessageBox.prompt('请输入隐藏原因（仅平台可见，用于留档）', '隐藏评价', {
    inputPlaceholder: '如：包含违规广告信息',
    inputValidator: (value) => (value && value.trim() ? true : '隐藏原因不能为空'),
    type: 'warning'
  }).catch(() => null)
  if (!result) {
    return
  }
  await hideComment(row.id, 1, result.value.trim())
  ElMessage.success('评价已隐藏')
  load()
}

const unhide = async (row) => {
  try {
    await ElMessageBox.confirm('确认恢复该评价的展示？', '恢复评价', { type: 'warning' })
  } catch (e) {
    return
  }
  await hideComment(row.id, 0)
  ElMessage.success('评价已恢复')
  load()
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: #909399;
  font-size: 12px;
}

.content {
  line-height: 1.5;
}

.images {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.thumb {
  width: 44px;
  height: 44px;
  border-radius: 4px;
}

.reply {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 4px;
}

.reply-label {
  color: #909399;
}

.reply-label.admin {
  color: #ff6b35;
}
</style>
