<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">菜品管理</h3>
      <div class="header-actions">
        <el-button :icon="Collection" @click="openCategoryDialog">分类管理</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDish">新增菜品</el-button>
      </div>
    </div>

    <div class="table-toolbar">
      <el-select v-model="categoryFilter" placeholder="全部分类" clearable style="width: 180px">
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <span class="sub-text">共 {{ filteredDishes.length }} 个菜品</span>
    </div>

    <div class="table-wrap">
      <el-table :data="pagedDishes" v-loading="loading" stripe row-key="id" empty-text="还没有菜品">
        <el-table-column label="菜品" min-width="240">
          <template #default="{ row }">
            <div class="dish-cell">
              <el-image v-if="row.image" :src="imgUrl(row.image)" fit="cover" class="dish-img"
                        :preview-src-list="[imgUrl(row.image)]" preview-teleported />
              <div class="dish-placeholder" v-else>无图</div>
              <div class="dish-info">
                <div class="dish-name">{{ row.name }}</div>
                <div class="sub-text">{{ row.description || '暂无描述' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="售价 / 原价" width="140">
          <template #default="{ row }">
            <span class="price">¥{{ money(row.price) }}</span>
            <span v-if="row.originalPrice" class="origin">¥{{ money(row.originalPrice) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="soldCount" label="销量" width="90" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="上架状态" width="130">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" active-color="#ff5722"
                       active-text="上架" inactive-text="下架"
                       @change="(value) => toggleDishStatus(row, value)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDish(row)">编辑</el-button>
            <el-button link type="primary" @click="openSpecDialog(row)">规格</el-button>
            <el-button link type="danger" @click="removeDish(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="filteredDishes.length"
                   layout="total, prev, pager, next" />

    <!-- 菜品表单 -->
    <el-dialog v-model="dishVisible" :title="dishForm.id ? '编辑菜品' : '新增菜品'" width="620px" :close-on-click-modal="false">
      <el-form ref="dishFormRef" :model="dishForm" :rules="dishRules" label-width="96px">
        <el-form-item label="菜品图片">
          <ImageUpload v-model="dishForm.image" />
        </el-form-item>
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="dishForm.name" maxlength="64" placeholder="如：宫保鸡丁盖饭" />
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-select v-model="dishForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="售价（元）" prop="price">
          <el-input-number v-model="dishForm.price" :min="0.01" :precision="2" :step="1" style="width: 180px" />
        </el-form-item>
        <el-form-item label="原价（元）">
          <el-input-number v-model="dishForm.originalPrice" :min="0" :precision="2" :step="1" style="width: 180px" />
          <span class="form-tip">选填，用于展示划线价</span>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="dishForm.description" type="textarea" :rows="2" maxlength="200" show-word-limit
                    placeholder="口味、食材等" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="dishForm.sort" :min="0" :step="1" style="width: 180px" />
          <span class="form-tip">越小越靠前</span>
        </el-form-item>
        <el-form-item label="上架">
          <el-switch v-model="dishForm.status" :active-value="1" :inactive-value="0" active-color="#ff5722" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dishVisible = false">取消</el-button>
        <el-button type="primary" :loading="dishSaving" @click="saveDish">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类管理 -->
    <el-dialog v-model="categoryVisible" title="分类管理" width="520px">
      <div class="category-add">
        <el-input v-model="categoryForm.name" maxlength="32" placeholder="分类名称，如：热销推荐" @keyup.enter="saveCategory" />
        <el-input-number v-model="categoryForm.sort" :min="0" :step="1" style="width: 110px" />
        <el-button type="primary" @click="saveCategory">{{ categoryForm.id ? '保存' : '新增' }}</el-button>
        <el-button v-if="categoryForm.id" @click="resetCategoryForm">取消</el-button>
      </div>
      <div class="category-list" v-loading="categorySaving">
        <div v-for="category in categories" :key="category.id" class="category-row">
          <span class="category-name">{{ category.name }}</span>
          <span class="sub-text">排序 {{ category.sort ?? 0 }}</span>
          <div class="category-ops">
            <el-button link type="primary" @click="editCategory(category)">编辑</el-button>
            <el-button link type="danger" @click="removeCategory(category)">删除</el-button>
          </div>
        </div>
        <el-empty v-if="!categories.length" description="还没有分类，请先新增" :image-size="70" />
      </div>
      <template #footer>
        <el-button type="primary" @click="categoryVisible = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- 规格管理 -->
    <el-dialog v-model="specVisible" :title="`菜品规格 - ${specDish?.name || ''}`" width="720px">
      <div class="spec-add">
        <el-input v-model="specForm.name" maxlength="32" placeholder="规格名，如 大份" style="width: 180px" />
        <el-input-number v-model="specForm.price" :min="0" :precision="2" :step="1" style="width: 130px"
                         placeholder="加价" />
        <el-input-number v-model="specForm.sort" :min="0" :step="1" style="width: 110px" placeholder="排序" />
        <el-switch v-model="specForm.status" :active-value="1" :inactive-value="0" active-color="#ff5722"
                   active-text="启用" />
        <el-button type="primary" @click="saveSpec">{{ specForm.id ? '保存' : '添加' }}</el-button>
        <el-button v-if="specForm.id" @click="resetSpecForm">取消</el-button>
      </div>
      <div class="spec-tip">规格加价会叠加到菜品售价，停用后用户端不再展示该规格。</div>
      <div class="table-wrap">
        <el-table :data="specs" v-loading="specLoading" stripe empty-text="暂无规格，用户可直接下单">
          <el-table-column prop="name" label="规格名" min-width="140" />
          <el-table-column label="加价" width="110">
            <template #default="{ row }">
              {{ Number(row.price) > 0 ? `+¥${money(row.price)}` : '不加价' }}
            </template>
          </el-table-column>
          <el-table-column prop="sort" label="排序" width="90" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-switch :model-value="row.status === 1" active-color="#ff5722"
                         @change="(value) => toggleSpec(row, value)" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link type="primary" @click="editSpec(row)">编辑</el-button>
              <el-button link type="danger" @click="removeSpec(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="specVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Collection, Plus } from '@element-plus/icons-vue'
import ImageUpload from '../../components/ImageUpload.vue'
import {
  createDish,
  createDishCategory,
  createDishSpec,
  deleteDish,
  deleteDishCategory,
  deleteDishSpec,
  listDishes,
  listDishSpecs,
  updateDish,
  updateDishCategory,
  updateDishSpec,
  updateDishStatus
} from '../../api/merchant'
import { imgUrl, money } from '../../utils/format'

const loading = ref(false)
const categories = ref([])
const dishes = ref([])
const categoryFilter = ref(null)
const page = ref(1)
const pageSize = 10

const dishVisible = ref(false)
const dishSaving = ref(false)
const dishFormRef = ref()
const dishForm = reactive({
  id: null,
  categoryId: null,
  name: '',
  image: '',
  price: 1,
  originalPrice: null,
  description: '',
  status: 1,
  sort: 0
})
const dishRules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择菜品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }]
}

const categoryVisible = ref(false)
const categorySaving = ref(false)
const categoryForm = reactive({ id: null, name: '', sort: 0 })

const specVisible = ref(false)
const specLoading = ref(false)
const specDish = ref(null)
const specs = ref([])
const specForm = reactive({ id: null, name: '', price: 0, sort: 0, status: 1 })

const filteredDishes = computed(() => {
  if (!categoryFilter.value) {
    return dishes.value
  }
  return dishes.value.filter((dish) => dish.categoryId === categoryFilter.value)
})

const pagedDishes = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredDishes.value.slice(start, start + pageSize)
})

const load = async () => {
  loading.value = true
  try {
    const data = await listDishes()
    categories.value = data?.categories || []
    dishes.value = data?.dishes || []
    if ((page.value - 1) * pageSize >= filteredDishes.value.length) {
      page.value = 1
    }
  } catch (e) {
    categories.value = []
    dishes.value = []
  } finally {
    loading.value = false
  }
}

/* ---------------- 菜品 ---------------- */

const openCreateDish = () => {
  if (!categories.value.length) {
    ElMessage.warning('请先添加菜品分类')
    openCategoryDialog()
    return
  }
  Object.assign(dishForm, {
    id: null,
    categoryId: categories.value[0].id,
    name: '',
    image: '',
    price: 1,
    originalPrice: null,
    description: '',
    status: 1,
    sort: 0
  })
  dishVisible.value = true
}

const openEditDish = (row) => {
  Object.assign(dishForm, {
    id: row.id,
    categoryId: row.categoryId,
    name: row.name,
    image: row.image || '',
    price: Number(row.price) || 0,
    originalPrice: row.originalPrice === null || row.originalPrice === undefined ? null : Number(row.originalPrice),
    description: row.description || '',
    status: row.status,
    sort: row.sort || 0
  })
  dishVisible.value = true
}

const saveDish = async () => {
  const valid = await dishFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  dishSaving.value = true
  try {
    const payload = {
      categoryId: dishForm.categoryId,
      name: dishForm.name.trim(),
      image: dishForm.image,
      price: dishForm.price,
      originalPrice: dishForm.originalPrice || null,
      description: dishForm.description,
      status: dishForm.status,
      sort: Number(dishForm.sort) || 0
    }
    if (dishForm.id) {
      await updateDish(dishForm.id, payload)
    } else {
      await createDish(payload)
    }
    ElMessage.success('已保存')
    dishVisible.value = false
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    dishSaving.value = false
  }
}

const toggleDishStatus = async (row, value) => {
  const next = value ? 1 : 0
  try {
    await updateDishStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '已上架' : '已下架')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

const removeDish = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除菜品「${row.name}」吗？`, '删除菜品', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteDish(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

/* ---------------- 分类 ---------------- */

const openCategoryDialog = () => {
  resetCategoryForm()
  categoryVisible.value = true
}

const resetCategoryForm = () => {
  categoryForm.id = null
  categoryForm.name = ''
  categoryForm.sort = categories.value.length
}

const editCategory = (category) => {
  categoryForm.id = category.id
  categoryForm.name = category.name
  categoryForm.sort = category.sort || 0
}

const saveCategory = async () => {
  const name = categoryForm.name.trim()
  if (!name) {
    ElMessage.warning('分类名称不能为空')
    return
  }
  categorySaving.value = true
  try {
    const payload = { name, sort: Number(categoryForm.sort) || 0 }
    if (categoryForm.id) {
      await updateDishCategory(categoryForm.id, payload)
    } else {
      await createDishCategory(payload)
    }
    ElMessage.success('已保存')
    resetCategoryForm()
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    categorySaving.value = false
  }
}

const removeCategory = async (category) => {
  try {
    await ElMessageBox.confirm(`确定删除分类「${category.name}」？分类下有菜品时无法删除。`, '删除分类', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deleteDishCategory(category.id)
    ElMessage.success('已删除')
    if (categoryFilter.value === category.id) {
      categoryFilter.value = null
    }
    if (categoryForm.id === category.id) {
      resetCategoryForm()
    }
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

/* ---------------- 规格 ---------------- */

const openSpecDialog = async (row) => {
  specDish.value = row
  specVisible.value = true
  resetSpecForm()
  await loadSpecs()
}

const loadSpecs = async () => {
  if (!specDish.value) {
    return
  }
  specLoading.value = true
  try {
    specs.value = (await listDishSpecs(specDish.value.id)) || []
  } catch (e) {
    specs.value = []
  } finally {
    specLoading.value = false
  }
}

const resetSpecForm = () => {
  specForm.id = null
  specForm.name = ''
  specForm.price = 0
  specForm.sort = specs.value.length
  specForm.status = 1
}

const editSpec = (row) => {
  specForm.id = row.id
  specForm.name = row.name
  specForm.price = Number(row.price) || 0
  specForm.sort = row.sort || 0
  specForm.status = row.status
}

const saveSpec = async () => {
  const name = specForm.name.trim()
  if (!name) {
    ElMessage.warning('规格名不能为空')
    return
  }
  try {
    const payload = {
      name,
      price: Number(specForm.price) || 0,
      sort: Number(specForm.sort) || 0,
      status: specForm.status
    }
    if (specForm.id) {
      await updateDishSpec(specForm.id, specDish.value.id, payload)
    } else {
      await createDishSpec(specDish.value.id, payload)
    }
    ElMessage.success('已保存')
    resetSpecForm()
    await loadSpecs()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

const toggleSpec = async (row, value) => {
  const next = value ? 1 : 0
  try {
    await updateDishSpec(row.id, specDish.value.id, {
      name: row.name,
      price: Number(row.price) || 0,
      sort: row.sort || 0,
      status: next
    })
    row.status = next
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

const removeSpec = async (row) => {
  try {
    await ElMessageBox.confirm('删除后用户端将不再展示该规格，确定删除吗？', '删除规格', { type: 'warning' })
  } catch (e) {
    return
  }
  try {
    await deleteDishSpec(row.id)
    ElMessage.success('已删除')
    await loadSpecs()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

onMounted(load)
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
}

.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-img,
.dish-placeholder {
  width: 52px;
  height: 52px;
  border-radius: 6px;
  flex-shrink: 0;
}

.dish-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 12px;
}

.dish-info {
  min-width: 0;
}

.dish-name {
  font-weight: 500;
}

.price {
  color: #ff5722;
  font-weight: 600;
  margin-right: 6px;
}

.origin {
  color: #c0c4cc;
  text-decoration: line-through;
  font-size: 12px;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.category-add {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.category-list {
  max-height: 320px;
  overflow-y: auto;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid #f0f2f5;
}

.category-name {
  flex: 1;
  font-weight: 500;
}

.category-ops {
  display: flex;
  gap: 4px;
}

.spec-add {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.spec-tip {
  color: #909399;
  font-size: 12px;
  margin-bottom: 10px;
}

@media (max-width: 768px) {
  .header-actions {
    width: 100%;
  }
}
</style>
