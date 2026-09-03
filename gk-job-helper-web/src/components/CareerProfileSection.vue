<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus/es/components/upload/src/upload'
import { currentResumeContentUrl, fetchCareerProfile, fetchCurrentResume, parseResume, saveCareerProfile } from '@/api/careerProfile'
import { showError } from '@/api/http'
import { loadAiProviderConfig } from '@/utils/aiConfig'
import type { CareerProfile, CareerProfileDraft, ResumeFile } from '@/types/model'

const selectedFile = ref<File | null>(null)
const fileList = ref<UploadUserFile[]>([])
const parsing = ref(false)
const saving = ref(false)
const savedProfile = ref<CareerProfile | null>(null)
const draft = ref<CareerProfileDraft | null>(null)
const currentResume = ref<ResumeFile | null>(null)

onMounted(async () => {
  try { const [profile, resume] = await Promise.all([fetchCareerProfile(), fetchCurrentResume()]); savedProfile.value = profile; currentResume.value = resume }
  catch (error) { showError(error, '读取职业画像或当前简历失败。') }
})

function chooseFile(uploadFile: UploadFile): void {
  const file = uploadFile.raw
  if (!file) return
  const validName = /\.(pdf|docx)$/i.test(file.name)
  const validType = file.type === 'application/pdf' || file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  if (!validName || !validType) { clearResumeSelection(); ElMessage.error('仅支持 PDF 或 DOCX 格式的简历文件。'); return }
  if (file.size > 10 * 1024 * 1024) { clearResumeSelection(); ElMessage.error('简历文件不能超过 10MB。'); return }
  selectedFile.value = file
}

function clearResumeSelection(): void { selectedFile.value = null; fileList.value = [] }

async function createDraft(): Promise<void> {
  const config = loadAiProviderConfig()
  if (!config) { ElMessage.warning('请先在首页配置并保存 AI 模型。'); return }
  if (!selectedFile.value) { ElMessage.warning('请先选择简历文件。'); return }
  parsing.value = true
  try {
    draft.value = await parseResume(selectedFile.value, config)
    currentResume.value = await fetchCurrentResume()
    clearResumeSelection()
    ElMessage.success('AI 已生成职业画像草稿，请核对并修改后确认保存。')
  } catch (error) {
    showError(error, '简历解析失败。')
    try { currentResume.value = await fetchCurrentResume() } catch { /* 原始文件已由服务端保存，刷新页面后仍可获取 */ }
  } finally { parsing.value = false }
}

function currentDraft(): CareerProfileDraft { if (!draft.value) throw new Error('职业画像草稿不存在'); return draft.value }
function addEducation(): void { currentDraft().educationExperiences.push({ school: null, degree: null, major: null, startDate: null, endDate: null, description: null }) }
function addWork(): void { currentDraft().workExperiences.push({ company: null, position: null, startDate: null, endDate: null, description: null }) }
function addProject(): void { currentDraft().projectExperiences.push({ name: null, role: null, startDate: null, endDate: null, description: null }) }

async function save(): Promise<void> {
  if (!draft.value) return
  saving.value = true
  try { savedProfile.value = await saveCareerProfile(draft.value); draft.value = null; ElMessage.success('职业画像已确认保存。') }
  catch (error) { showError(error, '保存职业画像失败。') } finally { saving.value = false }
}

function editSaved(): void {
  if (!savedProfile.value) return
  const source = savedProfile.value
  draft.value = JSON.parse(JSON.stringify({
    currentPosition: source.currentPosition, totalWorkYears: source.totalWorkYears,
    careerDirections: source.careerDirections, industries: source.industries,
    educationExperiences: source.educationExperiences, workExperiences: source.workExperiences,
    projectExperiences: source.projectExperiences, skills: source.skills, certificates: source.certificates
  })) as CareerProfileDraft
}

function dates(startDate: string | null, endDate: string | null): string {
  return [startDate, endDate].filter(Boolean).join(' 至 ') || '时间未填写'
}
function formatSize(size: number): string { return size < 1024 * 1024 ? `${Math.max(1, Math.round(size / 1024))}KB` : `${(size / 1024 / 1024).toFixed(1)}MB` }
function formatDate(value: string | null): string { return value ? value.replace('T', ' ') : '-' }
function openResume(): void { window.open(currentResumeContentUrl, '_blank', 'noopener') }
</script>

<template>
  <section class="page-card career-card">
    <div class="section-head">
      <div><h2 class="page-title">招聘职业画像</h2><p class="page-subtitle">上传 PDF 或 DOCX 简历，由 AI 提取草稿；仅确认后的内容会保存并用于后续招聘功能。</p></div>
      <el-button v-if="savedProfile && !draft" @click="editSaved">修改职业画像</el-button>
    </div>

    <div v-if="!draft && currentResume" class="resume-file-card">
      <div><strong>📄 {{ currentResume.originalFilename }}</strong><p>{{ currentResume.fileType === 'application/pdf' ? 'PDF' : 'DOCX' }} · {{ formatSize(currentResume.fileSize) }} · 上传时间 {{ formatDate(currentResume.uploadedAt) }}</p></div>
      <div><el-button @click="openResume">查看简历</el-button></div>
    </div>
    <p v-else-if="!draft && savedProfile" class="resume-missing">暂无已保存的原始简历；已保存职业画像仍可正常使用。</p>

    <div v-if="!draft" class="resume-upload">
      <el-upload v-model:file-list="fileList" :auto-upload="false" :show-file-list="true" :limit="1" accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" :on-change="chooseFile" :on-remove="clearResumeSelection">
        <el-button>{{ currentResume ? '重新上传' : '选择简历' }}</el-button><template #tip><div class="el-upload__tip">支持 PDF、DOCX，最大 10MB；新文件会先保存，再生成职业画像草稿；确认前不会覆盖已保存职业画像。</div></template>
      </el-upload>
      <el-button type="primary" :loading="parsing" :disabled="!selectedFile" @click="createDraft">AI 解析简历</el-button>
    </div>

    <template v-if="savedProfile && !draft">
      <div class="saved-summary"><el-tag type="success">已保存</el-tag><span>教育经历 {{ savedProfile.educationExperiences.length }} 条 · 工作经历 {{ savedProfile.workExperiences.length }} 条 · 项目经历 {{ savedProfile.projectExperiences.length }} 条 · 技能 {{ savedProfile.skills.length }} 项</span></div>
      <div class="overview-card saved-overview"><div><span class="label">当前/最近岗位</span><strong>{{ savedProfile.currentPosition || '未填写' }}</strong></div><div><span class="label">工作年限</span><strong>{{ savedProfile.totalWorkYears || '未填写' }}</strong></div><div><span class="label">职业方向</span><el-tag v-for="item in savedProfile.careerDirections" :key="item">{{ item }}</el-tag><span v-if="!savedProfile.careerDirections.length">未填写</span></div><div><span class="label">行业经验</span><el-tag v-for="item in savedProfile.industries" :key="item" type="info">{{ item }}</el-tag><span v-if="!savedProfile.industries.length">未填写</span></div></div>
      <div v-if="savedProfile.workExperiences.length" class="saved-block"><h3>工作经历</h3><article v-for="(item, index) in savedProfile.workExperiences" :key="`saved-work-${index}`" class="experience-card"><div class="work-summary"><strong>{{ item.company || '公司未填写' }}</strong><span>{{ item.position || '职位未填写' }}</span><span>{{ dates(item.startDate, item.endDate) }}</span></div><p v-if="item.description">{{ item.description }}</p></article></div>
      <div v-if="savedProfile.projectExperiences.length" class="saved-block"><h3>项目经历</h3><article v-for="(item, index) in savedProfile.projectExperiences" :key="`saved-project-${index}`" class="experience-card"><div class="work-summary"><strong>{{ item.name || '项目名称未填写' }}</strong><span>{{ item.role || '角色未填写' }}</span><span>{{ dates(item.startDate, item.endDate) }}</span></div><p v-if="item.description">{{ item.description }}</p></article></div>
    </template>

    <template v-if="draft">
      <el-alert title="这是 AI 生成的草稿，请只保留简历中真实存在的信息，确认后才会保存。" type="warning" :closable="false" show-icon />
      <h3>职业概况</h3>
      <div class="overview-card"><el-form label-position="top"><el-row :gutter="16"><el-col :span="12"><el-form-item label="当前/最近岗位"><el-input v-model="draft.currentPosition" placeholder="仅由 AI 从简历提取，可人工修订" /></el-form-item></el-col><el-col :span="12"><el-form-item label="工作年限"><el-input v-model="draft.totalWorkYears" placeholder="仅保留简历明确写出的年限" /></el-form-item></el-col><el-col :span="12"><el-form-item label="职业方向"><el-select v-model="draft.careerDirections" multiple filterable allow-create default-first-option placeholder="AI 未识别时可保持为空"><el-option v-for="item in draft.careerDirections" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="行业经验"><el-select v-model="draft.industries" multiple filterable allow-create default-first-option placeholder="AI 未识别时可保持为空"><el-option v-for="item in draft.industries" :key="item" :label="item" :value="item" /></el-select></el-form-item></el-col></el-row></el-form></div>

      <h3>教育经历 <el-button link type="primary" @click="addEducation">新增</el-button></h3>
      <div v-for="(item, index) in draft.educationExperiences" :key="`education-${index}`" class="compact-row"><el-input v-model="item.school" placeholder="学校" /><el-input v-model="item.degree" placeholder="学历/学位" /><el-input v-model="item.major" placeholder="专业" /><el-input v-model="item.startDate" placeholder="开始时间：yyyy-MM 或 yyyy" /><el-input v-model="item.endDate" placeholder="结束时间：yyyy-MM 或 yyyy" /><el-input v-model="item.description" placeholder="简历原文描述（选填）" /><el-button type="danger" link @click="draft.educationExperiences.splice(index, 1)">删除</el-button></div>

      <h3>工作经历 <el-button link type="primary" @click="addWork">新增</el-button></h3>
      <article v-for="(item, index) in draft.workExperiences" :key="`work-${index}`" class="experience-card"><div class="card-title"><strong>工作经历 {{ index + 1 }}</strong><el-button type="danger" link @click="draft.workExperiences.splice(index, 1)">删除</el-button></div><div class="work-basic"><el-input v-model="item.company" placeholder="公司/单位" /><el-input v-model="item.position" placeholder="职位" /><el-input v-model="item.startDate" placeholder="开始时间：yyyy-MM 或 yyyy" /><el-input v-model="item.endDate" placeholder="结束时间：yyyy-MM 或 yyyy" /></div><el-input v-model="item.description" type="textarea" :autosize="{ minRows: 3, maxRows: 8 }" placeholder="简历原文中的职责、项目或成果描述" /></article>

      <h3>项目经历 <el-button link type="primary" @click="addProject">新增</el-button></h3>
      <article v-for="(item, index) in draft.projectExperiences" :key="`project-${index}`" class="experience-card"><div class="card-title"><strong>项目经历 {{ index + 1 }}</strong><el-button type="danger" link @click="draft.projectExperiences.splice(index, 1)">删除</el-button></div><div class="work-basic"><el-input v-model="item.name" placeholder="项目名称" /><el-input v-model="item.role" placeholder="项目角色" /><el-input v-model="item.startDate" placeholder="开始时间：yyyy-MM 或 yyyy" /><el-input v-model="item.endDate" placeholder="结束时间：yyyy-MM 或 yyyy" /></div><el-input v-model="item.description" type="textarea" :autosize="{ minRows: 3, maxRows: 8 }" placeholder="简历原文中的项目描述" /></article>

      <div class="skill-certificate-grid"><section><h3>核心技能</h3><el-select v-model="draft.skills" multiple filterable allow-create default-first-option placeholder="输入后按回车添加"><el-option v-for="item in draft.skills" :key="item" :label="item" :value="item" /></el-select></section><section><h3>职业资格/证书</h3><el-select v-model="draft.certificates" multiple filterable allow-create default-first-option placeholder="输入后按回车添加"><el-option v-for="item in draft.certificates" :key="item" :label="item" :value="item" /></el-select></section></div>
      <div class="draft-actions"><el-button @click="draft = null">放弃草稿</el-button><el-button type="primary" :loading="saving" @click="save">确认保存职业画像</el-button></div>
    </template>
  </section>
</template>

<style scoped>
.career-card { margin-top:20px; }.section-head,.resume-upload,.saved-summary,.resume-file-card { display:flex; justify-content:space-between; align-items:center; gap:18px; }.resume-upload { margin-top:18px; align-items:flex-end; }.resume-file-card { margin-top:22px; padding:14px 16px; border:1px solid #b3d8ff; border-radius:8px; background:#ecf5ff; }.resume-file-card p,.resume-missing { margin:6px 0 0; color:#606266; }.resume-missing { margin-top:18px; }.saved-summary { margin-top:18px; color:#606266; justify-content:flex-start; }.career-card h3 { margin:24px 0 10px; font-size:16px; }.overview-card { margin-top:10px; padding:16px; border:1px solid #dcdfe6; border-radius:8px; background:#fafafa; }.overview-card .el-select { width:100%; }.saved-overview { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }.saved-overview > div { display:flex; align-items:center; flex-wrap:wrap; gap:8px; }.label { color:#909399; min-width:88px; }.compact-row { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)) auto; gap:10px; margin:10px 0; align-items:center; }.experience-card { margin:10px 0; padding:16px; border:1px solid #dcdfe6; border-radius:8px; background:#fff; }.card-title,.work-summary { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:12px; }.work-summary { justify-content:flex-start; flex-wrap:wrap; }.work-summary span { color:#606266; }.work-basic { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; margin-bottom:10px; }.saved-block p { margin:0; white-space:pre-wrap; color:#606266; line-height:1.7; }.skill-certificate-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; }.skill-certificate-grid h3 { margin-top:24px; }.skill-certificate-grid .el-select { width:100%; }.draft-actions { margin-top:20px; text-align:right; } @media (max-width:900px) { .section-head,.resume-upload,.resume-file-card { align-items:flex-start; flex-direction:column; }.saved-overview,.skill-certificate-grid,.compact-row,.work-basic { grid-template-columns:1fr; } }
</style>
