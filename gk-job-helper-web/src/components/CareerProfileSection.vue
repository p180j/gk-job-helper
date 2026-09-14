<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadUserFile } from 'element-plus/es/components/upload/src/upload'
import { currentResumeContentUrl, fetchCareerProfile, fetchCurrentResume, parseResume, saveCareerProfile } from '@/api/careerProfile'
import { deleteCareerFact, fetchCareerFactQuestions, fetchCareerFacts, saveCareerFact } from '@/api/careerFacts'
import { showError } from '@/api/http'
import { loadAiProviderConfig } from '@/utils/aiConfig'
import type { CareerFact, CareerFactQuestion, CareerProfile, CareerProfileDraft, ResumeFile } from '@/types/model'

const selectedFile = ref<File | null>(null)
const fileList = ref<UploadUserFile[]>([])
const parsing = ref(false)
const saving = ref(false)
const savedProfile = ref<CareerProfile | null>(null)
const draft = ref<CareerProfileDraft | null>(null)
const currentResume = ref<ResumeFile | null>(null)
const careerFacts = ref<CareerFact[]>([])
const factEditor = ref<CareerFact | null>(null)
const factSaving = ref(false)
const factQuestions = ref<CareerFactQuestion[]>([])

onMounted(async () => {
  try { const [profile, resume, facts, questions] = await Promise.all([fetchCareerProfile(), fetchCurrentResume(), fetchCareerFacts(), fetchCareerFactQuestions()]); savedProfile.value = profile; currentResume.value = resume; careerFacts.value = facts; factQuestions.value = questions }
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
function editFact(fact: CareerFact): void { factEditor.value = JSON.parse(JSON.stringify(fact)) as CareerFact }
function answerQuestion(question: CareerFactQuestion): void { factEditor.value = { factType: question.factType, factKey: question.factKey, valueType: question.valueType, numberValue: null, unit: question.unit || 'YEAR', displayName: question.displayName } }
function changeFactType(): void { if (!factEditor.value) return; const type = factEditor.value.factType; if (type === 'SKILL_EXPERIENCE' || type === 'INDUSTRY_EXPERIENCE') { factEditor.value.valueType = 'NUMBER'; factEditor.value.unit = 'YEAR'; factEditor.value.booleanValue = null; factEditor.value.textValue = null } else if (type === 'SPECIAL_EXPERIENCE') { factEditor.value.valueType = 'TEXT'; factEditor.value.unit = null; factEditor.value.booleanValue = null; factEditor.value.numberValue = null } else { factEditor.value.valueType = 'BOOLEAN'; factEditor.value.unit = null; factEditor.value.numberValue = null; factEditor.value.textValue = null } }
function factNameLabel(): string { if (!factEditor.value) return '名称'; return factEditor.value.factType === 'SKILL_EXPERIENCE' ? '技能名称' : factEditor.value.factType === 'INDUSTRY_EXPERIENCE' ? '行业名称' : factEditor.value.factType === 'SPECIAL_EXPERIENCE' ? '经历名称' : '证书或职称名称' }
function factNamePlaceholder(): string { if (!factEditor.value) return ''; return factEditor.value.factType === 'SKILL_EXPERIENCE' ? '如：Java 开发' : factEditor.value.factType === 'INDUSTRY_EXPERIENCE' ? '如：支付行业' : factEditor.value.factType === 'SPECIAL_EXPERIENCE' ? '如：基层服务项目' : '如：中级职称、CPA' }
function factKeyFromName(value: string): string { return value.trim().toUpperCase().replace(/\s+/g, '_').replace(/[^\w\u4e00-\u9fa5-]/g, '') }
async function saveFact(): Promise<void> { if (!factEditor.value || !factEditor.value.displayName.trim()) { ElMessage.warning('请填写要补充的信息名称。'); return } if (factEditor.value.valueType === 'TEXT' && !factEditor.value.textValue?.trim()) { ElMessage.warning('请填写补充说明。'); return } if (factEditor.value.valueType === 'BOOLEAN' && factEditor.value.booleanValue == null) { ElMessage.warning('请选择“有”或“无”。'); return } factEditor.value.factKey = factEditor.value.factKey.trim() || factKeyFromName(factEditor.value.displayName); factSaving.value = true; try { await saveCareerFact(factEditor.value); const [facts, questions] = await Promise.all([fetchCareerFacts(), fetchCareerFactQuestions()]); careerFacts.value = facts; factQuestions.value = questions; factEditor.value = null; ElMessage.success('职业信息已保存，招聘岗位正在自动重新匹配。') } catch (error) { showError(error, '保存职业信息失败。') } finally { factSaving.value = false } }
async function removeFact(fact: CareerFact): Promise<void> { if (!fact.id) return; try { await deleteCareerFact(fact.id); careerFacts.value = await fetchCareerFacts(); ElMessage.success('职业信息已删除，相关岗位已自动重新匹配。') } catch (error) { showError(error, '删除职业信息失败。') } }
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

    <div v-if="factQuestions.length || careerFacts.length || factEditor" class="fact-head"><h3>岗位补充信息（按需）</h3></div>
    <p v-if="factQuestions.length || careerFacts.length || factEditor" class="fact-tip">系统会优先使用简历和已保存职业画像。只有岗位要求无法从简历中可靠判断时，才需要补充对应信息。</p>
    <div v-if="factQuestions.length" class="fact-questions"><strong>还需补充 {{ factQuestions.length }} 项职业信息</strong><span>可进一步判断 {{ factQuestions.reduce((total, item) => total + item.affectedPositionCount, 0) }} 个岗位</span><div v-for="question in factQuestions" :key="`${question.factType}-${question.factKey}`" class="fact-question"><span>{{ question.question }}（影响 {{ question.affectedPositionCount }} 个岗位）</span><el-button link type="primary" @click="answerQuestion(question)">现在补充</el-button></div></div>
    <div v-if="factQuestions.length && !careerFacts.length && !factEditor" class="fact-empty"><el-empty description="请根据上方具体岗位问题补充信息" :image-size="56"/></div>
    <div v-for="fact in careerFacts" :key="fact.id" class="fact-row"><div><strong>{{ fact.displayName }}</strong><span>{{ fact.valueType==='NUMBER' ? `${fact.numberValue ?? '-'} ${fact.unit || ''}` : fact.valueType==='BOOLEAN' ? (fact.booleanValue ? '有' : '无') : (fact.textValue || '-') }}</span></div><div><el-button link type="primary" @click="editFact(fact)">修改</el-button><el-button link type="danger" @click="removeFact(fact)">删除</el-button></div></div>
    <div v-if="factEditor" class="fact-editor"><el-form label-position="top"><el-row :gutter="12"><el-col :span="8"><el-form-item label="信息类型"><el-select v-model="factEditor.factType"><el-option label="技能经验" value="SKILL_EXPERIENCE"/><el-option label="行业经验" value="INDUSTRY_EXPERIENCE"/><el-option label="证书" value="CERTIFICATE"/><el-option label="职业资格" value="PROFESSIONAL_QUALIFICATION"/><el-option label="职称" value="PROFESSIONAL_TITLE"/><el-option label="专项经历" value="SPECIAL_EXPERIENCE"/></el-select></el-form-item></el-col><el-col :span="8"><el-form-item label="名称"><el-input v-model="factEditor.displayName" placeholder="如：Java 开发经验"/></el-form-item></el-col><el-col :span="8"><el-form-item label="事实键"><el-input v-model="factEditor.factKey" placeholder="如：JAVA"/></el-form-item></el-col><el-col :span="8"><el-form-item label="值类型"><el-select v-model="factEditor.valueType"><el-option label="年限/数值" value="NUMBER"/><el-option label="有/无" value="BOOLEAN"/><el-option label="文本" value="TEXT"/></el-select></el-form-item></el-col><el-col v-if="factEditor.valueType==='NUMBER'" :span="8"><el-form-item label="数值"><el-input-number v-model="factEditor.numberValue" :min="0"/></el-form-item></el-col><el-col v-if="factEditor.valueType==='NUMBER'" :span="8"><el-form-item label="单位"><el-input v-model="factEditor.unit" placeholder="如：YEAR"/></el-form-item></el-col><el-col v-if="factEditor.valueType==='BOOLEAN'" :span="8"><el-form-item label="是否具备"><el-switch v-model="factEditor.booleanValue" active-text="有" inactive-text="无"/></el-form-item></el-col><el-col v-if="factEditor.valueType==='TEXT'" :span="16"><el-form-item label="内容"><el-input v-model="factEditor.textValue"/></el-form-item></el-col></el-row><div class="fact-actions"><el-button @click="factEditor=null">取消</el-button><el-button type="primary" :loading="factSaving" @click="saveFact">保存并重新匹配</el-button></div></el-form></div>

    <div v-if="factEditor" class="fact-editor simple-editor">
      <el-alert title="补充简历里已经真实具备的信息，用来判断岗位要求；不确定时可以先不填。" type="info" :closable="false" show-icon />
      <el-form label-position="top" class="simple-fact-form">
        <el-form-item label="要补充什么？">
          <el-select v-model="factEditor.factType" @change="changeFactType">
            <el-option label="某项技能的工作经验" value="SKILL_EXPERIENCE" />
            <el-option label="某个行业的工作经验" value="INDUSTRY_EXPERIENCE" />
            <el-option label="证书" value="CERTIFICATE" />
            <el-option label="职业资格" value="PROFESSIONAL_QUALIFICATION" />
            <el-option label="职称" value="PROFESSIONAL_TITLE" />
            <el-option label="其他专项经历" value="SPECIAL_EXPERIENCE" />
          </el-select>
        </el-form-item>
        <el-form-item :label="factNameLabel()">
          <el-input v-model="factEditor.displayName" :placeholder="factNamePlaceholder()" />
        </el-form-item>
        <template v-if="factEditor.valueType === 'NUMBER'">
          <el-form-item label="实际拥有几年？">
            <el-input-number v-model="factEditor.numberValue" :min="0" :precision="1" controls-position="right" />
            <span class="fact-unit">年</span>
          </el-form-item>
          <p class="fact-example">例如：有 3 年 Java 开发经验，就填写“Java 开发”和“3 年”。</p>
        </template>
        <template v-else-if="factEditor.valueType === 'BOOLEAN'">
          <el-form-item label="你是否具备？">
            <el-radio-group v-model="factEditor.booleanValue"><el-radio :label="true">有</el-radio><el-radio :label="false">无</el-radio></el-radio-group>
          </el-form-item>
        </template>
        <template v-else>
          <el-form-item label="补充说明"><el-input v-model="factEditor.textValue" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="只填写简历中能够确认的事实" /></el-form-item>
        </template>
        <div class="fact-actions"><el-button @click="factEditor=null">取消</el-button><el-button type="primary" :loading="factSaving" @click="saveFact">保存并重新匹配</el-button></div>
      </el-form>
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
.career-card { margin-top:20px; }.section-head,.resume-upload,.saved-summary,.resume-file-card,.fact-head,.fact-row { display:flex; justify-content:space-between; align-items:center; gap:18px; }.resume-upload { margin-top:18px; align-items:flex-end; }.resume-file-card { margin-top:22px; padding:14px 16px; border:1px solid #b3d8ff; border-radius:8px; background:#ecf5ff; }.resume-file-card p,.resume-missing,.fact-tip { margin:6px 0 0; color:#606266; }.resume-missing { margin-top:18px; }.saved-summary { margin-top:18px; color:#606266; justify-content:flex-start; }.career-card h3 { margin:24px 0 10px; font-size:16px; }.fact-head{margin-top:22px}.fact-tip{font-size:13px}.fact-empty{text-align:center;padding-bottom:16px}.fact-questions{margin-top:10px;padding:12px 14px;border:1px solid #e6a23c;border-radius:8px;background:#fdf6ec}.fact-questions>span{margin-left:10px;color:#606266;font-size:13px}.fact-question{display:flex;justify-content:space-between;align-items:center;margin-top:7px;font-size:14px}.fact-row{margin-top:8px;padding:10px 12px;border:1px solid #ebeef5;border-radius:6px}.fact-row>div:first-child{display:flex;gap:12px;align-items:center}.fact-row span{color:#606266}.fact-editor{margin-top:10px;padding:14px 16px;border:1px solid #b3d8ff;border-radius:8px;background:#ecf5ff}.fact-editor .el-select,.fact-editor .el-input,.fact-editor .el-input-number{width:100%}.fact-actions{text-align:right}.overview-card { margin-top:10px; padding:16px; border:1px solid #dcdfe6; border-radius:8px; background:#fafafa; }.overview-card .el-select { width:100%; }.saved-overview { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:16px; }.saved-overview > div { display:flex; align-items:center; flex-wrap:wrap; gap:8px; }.label { color:#909399; min-width:88px; }.compact-row { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)) auto; gap:10px; margin:10px 0; align-items:center; }.experience-card { margin:10px 0; padding:16px; border:1px solid #dcdfe6; border-radius:8px; background:#fff; }.card-title,.work-summary { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:12px; }.work-summary { justify-content:flex-start; flex-wrap:wrap; }.work-summary span { color:#606266; }.work-basic { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; margin-bottom:10px; }.saved-block p { margin:0; white-space:pre-wrap; color:#606266; line-height:1.7; }.skill-certificate-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; }.skill-certificate-grid h3 { margin-top:24px; }.skill-certificate-grid .el-select { width:100%; }.draft-actions { margin-top:20px; text-align:right; } @media (max-width:900px) { .section-head,.resume-upload,.resume-file-card,.fact-head,.fact-row { align-items:flex-start; flex-direction:column; }.saved-overview,.skill-certificate-grid,.compact-row,.work-basic { grid-template-columns:1fr; } }
.fact-editor:not(.simple-editor){display:none}.simple-editor{margin-top:10px}.simple-editor .el-select,.simple-editor .el-input,.simple-editor .el-input-number{width:100%}.simple-fact-form{margin-top:14px}.fact-unit{margin-left:8px;color:#606266}.fact-example{margin:-6px 0 12px;color:#909399;font-size:13px}
</style>
