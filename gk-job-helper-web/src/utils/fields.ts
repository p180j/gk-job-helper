export const STANDARD_FIELDS = [
  ['departmentName', '招录单位'], ['organizationName', '用人单位'], ['positionName', '岗位名称'],
  ['positionCode', '岗位代码'], ['province', '省份'], ['city', '城市'], ['district', '区县'],
  ['recruitCount', '招录人数'], ['educationRequirement', '学历要求'], ['degreeRequirement', '学位要求'],
  ['majorRequirement', '专业要求'], ['majorCodes', '专业代码'], ['ageRequirement', '年龄要求'],
  ['politicalRequirement', '政治面貌'], ['workYearRequirement', '基层工作年限'],
  ['freshGraduateRequirement', '应届要求'], ['householdRequirement', '户籍'],
  ['serviceProjectRequirement', '服务基层项目'], ['certificateRequirement', '资格证书'],
  ['genderRequirement', '性别'], ['positionDescription', '职位描述'], ['remark', '备注']
] as const

export const fieldLabel = (value: string | null | undefined) => STANDARD_FIELDS.find(([key]) => key === value)?.[1] ?? '不导入该字段'
