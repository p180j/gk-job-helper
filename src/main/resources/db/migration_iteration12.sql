-- 移除 Iteration 4 遗留的 20 条本科样例目录；完整官方目录由 MajorCatalogCsvImporter 从 CSV 导入。
DELETE a
FROM major_alias a
         JOIN major_catalog c ON c.id = a.catalog_id
WHERE c.catalog_code = 'MOE_UNDERGRADUATE_2026';

DELETE emc
FROM exam_major_catalog emc
         JOIN major_catalog c ON c.id = emc.catalog_id
WHERE c.catalog_code = 'MOE_UNDERGRADUATE_2026';

DELETE i
FROM major_catalog_item i
         JOIN major_catalog c ON c.id = i.catalog_id
WHERE c.catalog_code = 'MOE_UNDERGRADUATE_2026';

DELETE FROM major_catalog WHERE catalog_code = 'MOE_UNDERGRADUATE_2026';
