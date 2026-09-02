<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { addFavorite, removeFavorite } from '@/api/favorites'
import { fetchPreferences, fetchRecommendations, savePreferences } from '@/api/recommendations'
import { fetchImports } from '@/api/import'
import { showError } from '@/api/http'
import type { JobPreference, RecommendationItem, RecentImport } from '@/types/model'
import { normalizeRegionPreference, regionOptions } from '@/constants/regions'
import PositionLibraryBar from '@/components/PositionLibraryBar.vue'

const route=useRoute(),router=useRouter();const profileId=ref<number>();const loading=ref(false),saving=ref(false);const items=ref<RecommendationItem[]>([]),total=ref(0),page=ref(1),priority=ref('');const imports=ref<RecentImport[]>([]),selectedImportId=ref<number>();const size=20
const currentImport=computed(()=>imports.value.find(item=>item.importId===selectedImportId.value))
const interviewImportRoute=computed(()=>({path:'/interview-scores/import',query:{importId:String(selectedImportId.value||''),examYear:String(currentImport.value?.examYear||'')}}))
const preference=reactive<JobPreference>({preferredRegions:[],acceptedOrgLevels:[],excludedOrgLevels:[],preferredSubjectGroups:[],acceptExtraSubjects:true,preferMoreRecruits:true})
const orgOptions=[{label:'省级',value:'PROVINCE'},{label:'市级',value:'CITY'},{label:'县区级',value:'COUNTY'},{label:'乡镇街道',value:'TOWNSHIP'}]
const regionCascaderProps={multiple:true,checkStrictly:true,emitPath:false}
onMounted(async()=>{try{const p=await fetchProfile();if(!p)return;profileId.value=p.id;Object.assign(preference,await fetchPreferences(p.id));preference.preferredRegions=preference.preferredRegions.map(normalizeRegionPreference);const records=await fetchImports(1,100);imports.value=records.items.filter(item=>item.status==='IMPORTED'&&item.matchStats.total>0);const requested=Number(route.query.importId);selectedImportId.value=imports.value.some(item=>item.importId===requested)?requested:imports.value[0]?.importId;if(selectedImportId.value){await syncRoute();await load()}}catch(e){if(isProfileNotFound(e))router.replace('/profile');else showError(e,'读取优选岗位失败。')}})
async function syncRoute(){if(!selectedImportId.value)return;await router.replace({path:'/recommend',query:{importId:String(selectedImportId.value)}})}
async function changeImport(){page.value=1;priority.value='';items.value=[];total.value=0;await syncRoute();await load()}
async function load(){if(!profileId.value||!selectedImportId.value)return;loading.value=true;try{const data=await fetchRecommendations({profileId:profileId.value,importId:selectedImportId.value,priorityLevel:priority.value||undefined,page:page.value,size});items.value=data.items;total.value=data.total}catch(e){showError(e,'读取优选岗位失败。')}finally{loading.value=false}}
async function save(){if(!profileId.value)return;saving.value=true;try{Object.assign(preference,await savePreferences(profileId.value,preference));ElMessage.success('偏好已保存');page.value=1;await load()}catch(e){showError(e,'保存偏好失败。')}finally{saving.value=false}}
async function favorite(item:RecommendationItem){if(!profileId.value)return;try{item.favorite?await removeFavorite(item.positionId,profileId.value):await addFavorite(item.positionId,profileId.value);item.favorite=!item.favorite;ElMessage.success(item.favorite?'已收藏岗位':'已取消收藏')}catch(e){showError(e,'更新收藏失败。')}}
function relative(v:string|null){return ({LOWER:'较低',LOWER_MIDDLE:'中低',UPPER_MIDDLE:'中高',HIGHER:'较高'} as Record<string,string>)[v||'']||'暂无可靠分析'}
function priorityText(v:string){return v==='PRIORITY'?'优先关注':v==='NORMAL'?'符合偏好':'其他可报'}
function priorityType(v:string){return v==='PRIORITY'?'success':v==='NORMAL'?'primary':'info'}
function changePage(value:number){page.value=value;load()}
function openDetail(positionId:number){router.push({path:`/jobs/${positionId}`,query:{from:'recommend',importId:String(selectedImportId.value)}})}
</script>

<template><section>
 <PositionLibraryBar :imports="imports" :import-id="selectedImportId" active="recommend" @select="selectedImportId = $event; changeImport()" />
 <div class="page-card preference"><div class="head"><div><h2 class="page-title">为我优选</h2><p class="page-subtitle">仅从当前岗位表“可以报”的岗位中优选，不改变资格匹配结论。</p></div><router-link v-if="selectedImportId" :to="interviewImportRoute"><el-button>导入进面名单</el-button></router-link></div>
  <div v-if="currentImport" class="context-summary"><strong>{{ currentImport.matchStats.match }}</strong><span>个 MATCH 岗位</span><i></i><strong>{{ currentImport.examYear||'—' }}</strong><span>进面年度</span></div>
  <el-form label-position="top"><div class="pref-grid">
   <el-form-item label="偏好地区"><el-cascader v-model="preference.preferredRegions" :options="regionOptions" :props="regionCascaderProps" clearable collapse-tags collapse-tags-tooltip filterable placeholder="先选省份，再选择市或区" /></el-form-item>
   <el-form-item label="接受的单位层级"><el-select v-model="preference.acceptedOrgLevels" multiple clearable><el-option v-for="o in orgOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
   <el-form-item label="排除的单位层级"><el-select v-model="preference.excludedOrgLevels" multiple clearable><el-option v-for="o in orgOptions" :key="o.value" :label="o.label" :value="o.value" /></el-select></el-form-item>
   <el-form-item label="偏好考试口径"><el-select v-model="preference.preferredSubjectGroups" multiple filterable allow-create default-first-option placeholder="输入完整科目口径后回车" /></el-form-item>
  </div><div class="switches"><div class="option-group"><el-checkbox v-model="preference.acceptExtraSubjects">接受其他考试科目</el-checkbox><el-checkbox v-model="preference.preferMoreRecruits">同等条件下偏好招录人数更多</el-checkbox></div><el-button type="primary" :loading="saving" @click="save">保存并重新优选</el-button></div></el-form>
 </div>
 <div class="toolbar"><el-radio-group v-model="priority" @change="page=1;load()"><el-radio-button label="">全部</el-radio-button><el-radio-button label="PRIORITY">优先关注</el-radio-button><el-radio-button label="NORMAL">符合偏好</el-radio-button><el-radio-button label="OTHER">其他可报</el-radio-button></el-radio-group><span>共 {{ total }} 个 MATCH 岗位</span></div>
 <div v-loading="loading" class="cards"><el-empty v-if="!items.length&&!loading" description="暂无可以报的岗位，或尚未完成匹配。" />
  <article v-for="item in items" :key="item.positionId" class="recommend-card"><div class="card-top"><div><div class="title"><el-tag :type="priorityType(item.priorityLevel)">{{ priorityText(item.priorityLevel) }}</el-tag><h2>{{ item.positionName||'未命名岗位' }}</h2></div><p>{{ item.departmentName||'-' }}<span v-if="item.organizationName"> · {{ item.organizationName }}</span></p></div><el-button circle :type="item.favorite?'warning':'default'" @click="favorite(item)"><el-icon><StarFilled v-if="item.favorite"/><Star v-else/></el-icon></el-button></div>
   <div class="facts"><span>代码 {{ item.positionCode||'-' }}</span><span>{{ item.region||'地区未填' }}</span><span>招录 {{ item.recruitCount??'-' }} 人</span><span>{{ item.educationRequirement||'学历无要求' }}</span></div>
   <div class="subjects"><strong>考试科目：</strong>{{ item.examSubjects.length?item.examSubjects.join(' + '):'暂未识别' }}</div>
   <div class="badges"><el-tag v-if="item.minInterviewScore!=null" type="warning" effect="plain">{{ currentImport?.examYear }}最低进面{{ item.minInterviewScore }}</el-tag><el-tag v-if="item.historicalAnalysis?.sampleAverageScore!=null" type="primary" effect="plain">同类平均分 {{ item.historicalAnalysis.sampleAverageScore }}</el-tag><el-tag v-if="item.historicalAnalysis?.percentile!=null" effect="plain">同类进面位置{{ relative(item.historicalAnalysis.relativeLevel) }}</el-tag><span v-if="item.historicalAnalysis?.sampleCount" class="sample">{{ item.historicalAnalysis.comparisonDescription }} · {{ item.historicalAnalysis.confidence }}</span></div>
   <div class="reasons"><span v-for="reason in item.recommendReasons" :key="reason">{{ reason }}</span></div><div class="actions"><el-button type="primary" plain @click="openDetail(item.positionId)">查看详情</el-button></div>
  </article><div v-if="total" class="pager"><el-pagination background layout="total,prev,pager,next" :total="total" :page-size="size" :current-page="page" @current-change="changePage"/></div>
 </div>
</section></template>
<style scoped>.preference{margin-bottom:18px}.head,.card-top,.toolbar,.switches,.title,.badges,.current-import,.context-summary,.option-group{display:flex;align-items:center}.head,.card-top,.toolbar,.switches{justify-content:space-between}.current-import{gap:14px;margin:22px 0 20px;padding:14px 16px;background:linear-gradient(90deg,#f3f8ff,#f8fbff);border:1px solid #d9e8fb;border-radius:8px}.context-label{flex:0 0 auto;color:#303133;font-weight:600}.current-import .el-select{flex:1;min-width:280px}.context-summary{flex:0 0 auto;gap:6px;color:#76839a;font-size:13px;white-space:nowrap}.context-summary strong{color:#337ecc;font-size:16px}.context-summary i{width:1px;height:18px;margin:0 8px;background:#d8e2ef}.pref-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));column-gap:20px}.pref-grid :deep(.el-form-item){margin-bottom:16px}.pref-grid :deep(.el-cascader){width:100%}.switches{gap:20px;padding-top:14px;border-top:1px solid #ebeef5}.option-group{gap:26px}.toolbar{margin:18px 0;color:#909399}.recommend-card{background:#fff;border:1px solid #ebeef5;border-radius:10px;padding:20px 22px;margin-bottom:14px}.title{gap:10px}.title h2{font-size:18px;margin:0}.card-top p{color:#606266;margin:10px 0}.facts{display:flex;gap:20px;flex-wrap:wrap;color:#606266;font-size:13px}.subjects{margin-top:14px;padding:10px 12px;background:#f5f7fa;border-radius:6px}.badges{gap:8px;margin-top:12px;flex-wrap:wrap}.sample{font-size:12px;color:#909399}.reasons{display:flex;gap:8px;flex-wrap:wrap;margin-top:12px}.reasons span{padding:4px 9px;color:#337ecc;background:#ecf5ff;border-radius:12px;font-size:12px}.actions{text-align:right}.pager{display:flex;justify-content:flex-end;margin:24px 0}@media(max-width:900px){.current-import{align-items:flex-start;flex-wrap:wrap}.current-import .el-select{order:3;flex-basis:100%}.context-summary{margin-left:auto}.pref-grid{grid-template-columns:1fr}.switches,.option-group{align-items:flex-start;flex-direction:column}.toolbar{align-items:flex-start;gap:12px;flex-direction:column}}</style>
