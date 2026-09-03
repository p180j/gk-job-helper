/** 后端统一响应结构 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T | null
}

export interface AiProviderConfig {
  provider: string
  model: string
  apiKey: string
  baseUrl: string
}

export interface AiTestResult {
  success: boolean
  message: string
}
export type RecruitmentNoticeStatus='NEW'|'INTERESTED'|'FOLLOWING'|'IGNORED'
export interface RecruitmentAttachment {id:number;fileName:string;fileUrl:string;fileType:string;attachmentType:string;parseStatus?:'UNPARSED'|'PARSING'|'PARSED'|'FAILED';parseError?:string|null;positionCount?:number;sourceText:string|null}
export interface RecruitmentNotice {id:number;title:string;noticeUrl:string;publishDate:string|null;noticeType:string;userStatus:RecruitmentNoticeStatus;viewedAt:string|null;discoveredAt:string;sourceCode:string;sourceName:string;detailStatus?:'DISCOVERED'|'FETCHING'|'FETCHED'|'FAILED';detailFetchedAt?:string|null;detailError?:string|null;bodyHtml?:string|null;bodyText?:string|null;attachments?:RecruitmentAttachment[]}
export interface RecruitmentDiscoveryResult {fetchedCount:number;newCount:number;duplicateCount:number;failedCount:number}
export interface RecruitmentRequirement {id:number;requirementType:string;requirementLevel:string;rawText:string|null;normalizedValue:string|null;sourceText:string|null}
export interface RecruitmentPosition {id:number;noticeId:number;sourceAttachmentId:number;organizationName:string|null;departmentName:string|null;positionName:string;positionCode:string|null;recruitCount:number|null;workLocation:string|null;educationRequirement:string|null;degreeRequirement:string|null;majorRequirement:string|null;ageRequirement:string|null;workYearsRequirement:string|null;responsibility:string|null;otherRequirement:string|null;preferredRequirement:string|null;rawRequirement:string|null;sourceSheet:string;sourceRow:number;requirements?:RecruitmentRequirement[]}
export interface RecruitmentPositionExtractionResponse {noticeId:number;attachmentCount:number;positionCount:number;failedAttachmentCount:number}

/** 用户个人档案 */
export interface UserProfile {
  id: number
  name: string | null
  gender: string | null
  birthDate: string | null
  politicalStatus: string | null
  education: string | null
  degree: string | null
  major: string | null
  majorCode: string | null
  graduationDate: string | null
  workYears: number | null
  freshGraduateStatus: string | null
  household: string | null
  studentOrigin: string | null
  serviceProjectType: string | null
  veteran: string | null
  certificates: string | null
  englishLevel: 'NONE' | 'CET4' | 'CET6' | null
  targetRegion: string | null
  notes: string | null
}

export type ProfileForm = Omit<UserProfile, 'id'> & { id?: number }

export interface CareerEducation {
  school: string | null
  degree: string | null
  major: string | null
  startDate: string | null
  endDate: string | null
  description: string | null
}

export interface CareerWorkExperience {
  company: string | null
  position: string | null
  startDate: string | null
  endDate: string | null
  description: string | null
}

export interface CareerProjectExperience {
  name: string | null
  role: string | null
  startDate: string | null
  endDate: string | null
  description: string | null
}

export interface CareerProfileDraft {
  fileName?: string
  currentPosition: string | null
  totalWorkYears: string | null
  careerDirections: string[]
  industries: string[]
  educationExperiences: CareerEducation[]
  workExperiences: CareerWorkExperience[]
  projectExperiences: CareerProjectExperience[]
  skills: string[]
  certificates: string[]
}

export interface CareerProfile extends CareerProfileDraft {
  id: number
  profileId: number
  updatedAt: string | null
}

export interface ResumeFile {
  resumeId: number
  originalFilename: string
  fileType: string
  fileSize: number
  uploadedAt: string | null
}

/** Excel 上传预览 */
export interface ExcelPreview {
  fileId: number
  fileName: string
  fileType: string
  sheetName: string
  headers: string[]
  totalRows: number
  previewRows: Array<Record<string, string>>
  sheets: ExcelSheetPreview[]
}

export interface ExcelSheetPreview {
  sheetName: string
  headers: string[]
  totalRows: number
  previewRows: Array<Record<string, string>>
  suggestedForImport: boolean
}

/** 字段映射置信度 */
export type FieldConfidence = 'EXACT' | 'ALIAS' | 'UNKNOWN'

export interface HeaderSuggestion {
  sourceField: string
  suggestedField: string | null
  confidence: FieldConfidence
}

export interface FieldMappingPreview {
  importId: number
  sheetName: string
  headers: HeaderSuggestion[]
  sheets: ExcelSheetPreview[]
}

export interface MappingItem {
  sourceField: string
  targetField: string | null
}

export interface ImportFailedItem {
  row: number
  reason: string
}

export interface ImportResult {
  importId: number
  totalRows: number
  successRows: number
  failedRows: number
  failedItems: ImportFailedItem[]
}

export interface ImportProgress {
  id: number
  totalRows: number
  status: 'PREVIEWED' | 'IMPORTING' | 'IMPORTED' | 'IMPORT_FAILED'
  processedRows: number
  successRows: number
  failedRows: number
  errorMessage: string | null
}

/** 匹配结果三态 */
export type MatchResultValue = 'MATCH' | 'UNCERTAIN' | 'NOT_MATCH'

export interface MatchEvidence {
  catalogCode: string | null
  catalogName: string | null
  majorCode: string | null
  majorName: string | null
  parentCode: string | null
  parentName: string | null
}

export interface MatchItem {
  conditionType: string
  result: MatchResultValue
  userValue: string | null
  requirementValue: string | null
  reason: string | null
  evidence: MatchEvidence | null
}

export interface MatchResult {
  jobId: number
  profileId: number
  result: MatchResultValue
  referenceDate: string | null
  items: MatchItem[]
}

export interface MatchSummary {
  total: number
  match: number
  uncertain: number
  notMatch: number
  failedCount: number
  failedItems: Array<{ jobId: number; reason: string }>
}

/** 标准岗位模型 */
export interface JobPosition {
  id: number
  examId: number | null
  importFileId: number | null
  departmentName: string | null
  organizationName: string | null
  positionName: string | null
  positionCode: string | null
  province: string | null
  city: string | null
  district: string | null
  recruitCount: number | null
  educationRequirement: string | null
  degreeRequirement: string | null
  majorRequirement: string | null
  majorCodes: string | null
  ageRequirement: string | null
  politicalRequirement: string | null
  workYearRequirement: string | null
  freshGraduateRequirement: string | null
  householdRequirement: string | null
  serviceProjectRequirement: string | null
  certificateRequirement: string | null
  genderRequirement: string | null
  positionDescription: string | null
  remark: string | null
  sourceSheet: string | null
  sourceRow: number | null
  rawData: string | null
}

/** 匹配结果列表行 */
export interface MatchPositionResult {
  jobId: number
  positionName: string | null
  positionCode: string | null
  departmentName: string | null
  organizationName: string | null
  province: string | null
  city: string | null
  district: string | null
  region: string | null
  recruitCount: number | null
  educationRequirement: string | null
  majorRequirement: string | null
  matchResult: MatchResultValue | null
  referenceDate: string | null
  favorite: boolean
  examSubjectCount: number | null
  examSubjectsJson: string | null
  examSubjectGroup: string | null
  minInterviewScore: number | null
}

export interface JobFeature {
  positionId: number
  examSubjectCount: number | null
  examSubjects: string[]
  examSubjectGroup: string | null
  examSubjectStatus: string
  rawExamSubjectText: string | null
  majorRestrictionType: string
  majorDomains: string[]
  majorScopeCount: number | null
  majorAnalysisStatus: string
  organizationLevel: string | null
}

export interface HistoricalAnalysis {
  examYear: number | null
  minInterviewScore: number | null
  sampleMinScore: number | null
  sampleMaxScore: number | null
  sampleMedianScore: number | null
  sampleAverageScore: number | null
  comparisonLevel: string | null
  sampleCount: number
  percentile: number | null
  relativeLevel: string | null
  confidence: 'LOW' | 'MEDIUM' | 'HIGH'
  comparisonDescription: string
  available: boolean
  reliable: boolean
}

export interface JobPreference {
  preferredRegions: string[]
  acceptedOrgLevels: string[]
  excludedOrgLevels: string[]
  preferredSubjectGroups: string[]
  acceptExtraSubjects: boolean
  preferMoreRecruits: boolean
}

export interface RecommendationItem {
  positionId: number
  positionCode: string | null
  positionName: string | null
  departmentName: string | null
  organizationName: string | null
  region: string | null
  recruitCount: number | null
  educationRequirement: string | null
  majorRequirement: string | null
  examSubjectCount: number | null
  examSubjects: string[]
  examSubjectGroup: string | null
  majorRestrictionType: string | null
  minInterviewScore: number | null
  historicalAnalysis: HistoricalAnalysis
  priorityLevel: 'PRIORITY' | 'NORMAL' | 'OTHER'
  recommendReasons: string[]
  favorite: boolean
}

export interface InterviewScoreImportResult {
  importId: number
  examYear: number
  rawRowCount: number
  aggregatedPositionCount: number
  insertedCount: number
  updatedCount: number
  linkedPositionCount: number
  unlinkedPositionCount: number
  invalidRowCount: number
  invalidRows: string[]
}

export interface MatchResultFilters {
  profileId: number
  importId: number
  status?: MatchResultValue
  region?: string
  organizationKeyword?: string
  positionKeyword?: string
  recruitCountMin?: number
  recruitCountMax?: number
  educationKeyword?: string
  majorKeyword?: string
  page: number
  size: number
}

export interface JobCompareItem {
  jobId: number
  region: string | null
  departmentName: string | null
  organizationName: string | null
  positionName: string | null
  positionCode: string | null
  recruitCount: number | null
  educationRequirement: string | null
  majorRequirement: string | null
  ageRequirement: string | null
  politicalRequirement: string | null
  workYearRequirement: string | null
  freshGraduateRequirement: string | null
  otherRestrictions: string | null
  overallStatus: MatchResultValue
  matchItems: MatchItem[]
}

/** 通用分页 */
export interface PageVO<T> {
  total: number
  page: number
  size: number
  items: T[]
}

/** 首页"最近分析"卡片 */
export interface RecentImport {
  importId: number
  fileName: string
  sheetName: string | null
  totalRows: number
  examYear: number | null
  status: 'PREVIEWED' | 'IMPORTING' | 'IMPORTED' | 'IMPORT_FAILED'
  createdAt: string | null
  jobCount: number
  matchStats: {
    total: number
    match: number
    uncertain: number
    notMatch: number
  }
}

/** 专业目录 */
export interface MajorCatalog {
  id: number
  catalogCode: string
  catalogName: string
  catalogType: string
  educationLevel: string
  version: string | null
  sourceName: string | null
  sourceYear: string | null
}

export interface MajorSearchItem {
  catalogId: number
  catalogCode: string
  catalogName: string
  itemId: number
  majorCode: string | null
  majorName: string
  parentCode: string | null
  parentName: string | null
}
