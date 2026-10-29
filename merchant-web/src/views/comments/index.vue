<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">顾客评价</h3>
      <span class="sub-text">差评（1-2 星）自动置顶标红</span>
    </div>

    <div class="summary" v-if="list.length">
      <div class="score">{{ avgScore }}</div>
      <div class="score-info">
        <div class="score-label">综合评分</div>
        <div class="sub-text">共 {{ list.length }} 条评价 · 差评 {{ badCount }} 条</div>
      </div>
    </div>

    <div class="table-wrap">
      <el-table :data="pagedRows" v-loading="loading" stripe row-key="id" :row-class-name="rowClassName"
                empty-text="暂无评价">
        <el-table-column prop="userName" label="顾客" width="130" show-overflow-tooltip />
        <el-table-column label="评分" width="170">
          <template #default="{ row }">
            <el-rate :model-value="row.score" disabled />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="260">
          <template #default="{ row }">
            <div class="content">{{ row.content || '（无文字内容）' }}</div>
            <div v-if="row.images" class="images">
              <el-image v-for="(img, index) in row.images.split(',')" :key="index" :src="imgUrl(img)" fit="cover"
                        class="thumb" :preview-src-list="row.images.split(',').map(imgUrl)"
                        :initial-index="index" preview-teleported />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="评价时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="商家回复" min-width="200">
          <template #default="{ row }">
            <template v-if="row.replyContent">
              <div class="reply">{{ row.replyContent }}</div>
              <div class="sub-text">{{ formatTime(row.replyTime) }}</div>
            </template>
            <span v-else class="sub-text">暂无回复</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">
              {{ row.replyContent ? '修改回复' : '回复' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="list.length"
                   layout="total, prev, pager, next" />

    <el-dialog v-model="replyVisible" title="回复评价" width="460px">
      <div v-if="replyTarget?.content" class="quote">{{ replyTarget.content }}</div>
      <el-input v-model="replyContent" type="textarea" :rows="4" maxlength="200" show-word-limit
                placeholder="用真诚的回复留住顾客～" />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitReply">发布回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listComments, replyComment } from '../../api/merchant'
import { formatTime, imgUrl } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const page = ref(1)
const pageSize = 10

const replyVisible = ref(false)
const saving = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return list.value.slice(start, start + pageSize)
})

const avgScore = computed(() => {
  if (!list.value.length) {
    return '0.0'
  }
  const total = list.value.reduce((sum, item) => sum + (item.score || 0), 0)
  return (total / list.value.length).toFixed(1)
})

const badCount = computed(() => list.value.filter((item) => item.score <= 2).length)

const rowClassName = ({ row }) => (row.score <= 2 ? 'bad-row' : '')

const load = async () => {
  loading.value = true
  try {
    const data = (await listComments()) || []
    // 差评置顶，其余按时间倒序
    list.value = [...data].sort((a, b) => {
      const badDiff = (b.score <= 2 ? 1 : 0) - (a.score <= 2 ? 1 : 0)
      if (badDiff !== 0) {
        return badDiff
      }
      return String(b.createTime || '').localeCompare(String(a.createTime || ''))
    })
  } catch (e) {
    list.value = []
  } finally {
    loading.value = false
  }
}

const openReply = (row) => {
  replyTarget.value = row
  replyContent.value = row.replyContent || ''
  replyVisible.value = true
}

const submitReply = async () => {
  const content = replyContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  saving.value = true
  try {
    await replyComment(replyTarget.value.id, content)
    ElMessage.success('回复成功')
    replyVisible.value = false
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.summary {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  margin-bottom: 16px;
  border-radius: 8px;
  background: linear-gradient(120deg, #fff3ef, #fffaf7);
}

.score {
  font-size: 36px;
  font-weight: 700;
  color: #ff5722;
  line-height: 1;
}

.score-label {
  font-weight: 600;
  margin-bottom: 4px;
}

.content {
  white-space: pre-wrap;
  word-break: break-all;
}

.images {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.thumb {
  width: 60px;
  height: 60px;
  border-radius: 6px;
}

.reply {
  color: #606266;
  word-break: break-all;
}

.quote {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
}

:deep(.bad-row) {
  background: #fff1f0;
}

:deep(.bad-row td) {
  color: #c45656;
}
</style>
