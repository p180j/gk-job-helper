# 岗位资格解析与工作地点改造验收记录

记录日期：2026-10-26。统计范围为本机数据库中 2026 年职位导入批次 `importId=23` 的 4,409 条岗位、考生档案 `profileId=1`。本机没有 2027 年导入批次；下述真实数据统计不能外推到 2027 年。

## 1. 根因与影响链路

- 专业解析器在选出本科等学历段后，没有再次识别“专业不限”；匹配器还将空专业要求当成匹配。部分导入表把各学历专业要求仅保存在原始 JSON 的独立列，单一 `major_requirement` 无法完整表达。
- 备注匹配使用整段文本和全局识别标记，容易使录用后的最低服务年限进入资格疑点，也无法独立呈现一条备注中的多个条件。
- 旧地点解析从整条原始数据找地名，联系电话区号等非地点文本可能被误用；2026 年本地批次的 `province/city/district` 均为空。筛选、推荐和页面此前使用不同的地区来源。
- 受影响链路为原始岗位 JSON → 岗位特征 → 专业及备注匹配 → 匹配结果存储 → 地区筛选、推荐、收藏、对比、详情展示。

## 2. 方案与取舍

保留官方原始岗位行，将适用学历层级的专业要求、备注条件和地点证据存入 `job_position_feature`。匹配器读取原始字段及对应学历层级的原始 JSON 列；“专业不限”直接判定专业项 MATCH，空值或无法解析仍为 UNCERTAIN。备注条件分别标记报考资格、录用后约束和说明；不能证实的硬性条件保留核验状态。地点只有明确职位列、备注或简介证据才判 CONFIRMED；单位名称只能推断行政辖区，不能当作实际工作地点。筛选仅命中确认地点，用户可显式包含待核实岗位；推荐地点加分仅使用确认地点。

行政区划采用仓库内离线的 2025 年区划快照及 MIT 许可文本，来源为 [zoningjs 发布记录](https://github.com/netnr/zoning/releases)。同名地区缺少上级上下文时不作唯一归属推断。当前快照不覆盖所有历史别名、乡镇及特殊管理区，也没有经核验的机关办公地址库。

## 3. 修改文件

- 专业：`MajorRequirementParser`、`MajorMatcher`、`JobMajorRequirementResolver`、`MajorRestrictionAnalyzer`。
- 备注：`JobRemarkParser`、`RemarkMatcher`。
- 地点：`AdministrativeDivisionCatalog`、`JobWorkLocationResolver`、`JobRegionResolver`、`administrative-divisions-2025.json` 及许可文本。
- 派生数据与接口：`JobFeatureService`、`JobMatchService`、`JobCompareService`、相关 Controller、DTO、实体、Mapper 及 `schema.sql`、`hf_migration_position_qualification_location.sql`。
- 页面：`ResultsView.vue`、`JobDetailView.vue`、`FavoritesView.vue`、`CompareView.vue`、`model.ts`；推荐读取逻辑及其 Mapper。
- 测试：`MajorMatcherTest`、`RemarkMatcherTest`、`JobRemarkParserTest`、`JobWorkLocationResolverTest`、`JobRegionResolverTest`、`Iteration7ApiTest`、`Iteration8ApiTest`、`MatchApiTest`。

## 4. 数据库迁移

增量脚本 `src/main/resources/db/hf_migration_position_qualification_location.sql` 仅给 `job_position_feature` 新增 8 个派生字段及地点索引；不改原始职位、面试分、考试科目、专业目录或既有匹配表结构。此 DDL **只执行一次**，不是可重复执行脚本。新建数据库用同步更新后的 `schema.sql`。部署顺序：备份数据库 → 执行增量脚本 → 部署后端与前端 → 按批次预览并重建派生字段 → 预览并重算相应档案的匹配结果。

## 5. 专业验证

自动化测试覆盖软件工程代码 `080902`、计算机类 `0809`、相同专业、无关专业类、各学历段的“专业不限”、研究生不限但本科受限、本科不限但专科受限、空要求和混合分隔符。保留原有专业目录及锚点判断。历史专业特征采用原解析口径：本地 4,409 行的考试科目与历史专业限制、相似度键及锚点相关字段，与迁移前备份逐行比较，变化均为 0。

## 6. 备注覆盖

本地批次结构化条件实例数（**同一岗位可贡献多条，不是岗位数**）：定向身份 2,318、性别 1,018、证书 742、警察体能 454、工作经历 48、最低服务年限 29、岗位分配 21、地点 4、政治身份 1、其他说明 15、仍未识别 35。最低服务年限作为录用后约束展示，不单凭缺少个人偏好判资格不符。剩余未识别文本保留核验提示；样例包括“限本县乡镇事业单位工作 5 年以上人员报考”、特定民族及未明确名称的证书要求。

## 7. 地点覆盖

4,409 条中，明确工作地点 CONFIRMED 1 条（0.02%）、多个地点 MULTI_SITE 3 条（0.07%）、仅能推断机关行政辖区 INFERRED 4,045 条（91.74%）、UNKNOWN 360 条（8.17%）。来源为明确备注 4 条、机关名称行政辖区 4,045 条、无可用证据 360 条；无职位原始“工作地点”独立列记录。可确认的实际工作地点覆盖率很低，不能把 4,045 条推断行政辖区算作工作地点。电话区号不参与地点识别。

## 8. 资格匹配变化

重算前：MATCH 133、NOT_MATCH 4,191、UNCERTAIN 85。只读变更预览：综合结果变化 2 条，专业项变化 608 条，备注项变化 666 条；两条综合结果由 UNCERTAIN 转 MATCH。重算后：MATCH 135、NOT_MATCH 4,191、UNCERTAIN 83，失败 0。再次预览：综合、专业及备注项待变更数均为 0。

## 9. 本地历史派生数据重建

重建前备份存于本机忽略目录 `target/gk_job_helper_before_qualification_location.sql`。`GET /api/job-features/parsed-preview?importId=23` 初次显示 4,409 条待变更；`POST /api/job-features/parsed-rebuild?importId=23` 后再次预览为 0。之后先调用 `GET /api/match/change-preview?profileId=1&importId=23`，再用既有 `POST /api/match/execute` 对该批次重算。事务保护派生字段更新，重复执行可重试且不新增重复特征。

用迁移前备份恢复临时审计库并比较：`job_position`、`job_interview_score`、`major_catalog`、`major_catalog_item`、`major_alias` 的 MySQL 表校验和均相同；4,409 行岗位特征中的考试科目与历史专业比较字段变化数为 0。审计库已删除。项目实际数据库不存在 `job_exam_subject` 表；考试科目存于岗位特征中。

## 10. 编译及测试

后端 `mvn -q test`：287 项，失败 0、错误 0、跳过 1（仅统计本次运行生成的报告；旧的手工 LiveInspection 报告未包含）。新增收藏与对比一致性断言后，`mvn -q -Dtest=Iteration7ApiTest test`：19 项，失败 0。前端 `npm run build` 和后续 `npx vue-tsc -b` 均通过。`git diff --check` 无空白错误。真实数据批次解析与数据库重建已运行；没有浏览器端人工逐页验收。

## 11. 未解决问题及风险

- 本机无 2027 年数据，不能提供该年的真实解析统计或重建结果；导入后需逐批预览和验收。
- 实际工作地点明确证据仅 1 条；行政辖区不等于实际岗位地点。若业务需要大范围精确城市筛选，须补充官方地点来源或经人工核验的数据，不应自动把省直机关映射到省会。
- 离线区划快照无法可靠处理全部历史别名、乡镇与特殊管理区；此类地点保持待核实。
- 备注仍有 35 个未识别条件实例，部分可能是硬性资格；系统不自动认定符合。
- 前端详情、列表、收藏和对比已经读取统一派生地点；尚无人工核验、覆盖或版本审计工作流。

## 12. 其他开发环境与部署操作

共享代码没有写入本机绝对路径，也没有新增生产依赖。其他电脑或部署环境拉取代码后需：先备份各自数据库；若为旧库，执行一次 `hf_migration_position_qualification_location.sql`；启动新后端并发布前端；按各自实际 `importId` 调用解析预览及重建；按各自 `profileId` 预览并重算匹配。已有旧版本后端不能直接读取新页面需要的派生字段，部署时应同步更新。新库直接使用更新后的 `schema.sql`，不用再执行增量脚本。未同步的电脑仍运行旧逻辑及旧数据；同步前留意各自未提交代码和本地数据库备份。
