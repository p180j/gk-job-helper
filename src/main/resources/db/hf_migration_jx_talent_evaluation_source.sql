INSERT INTO recruitment_source(source_code,source_name,source_type,list_url,enabled,created_at,updated_at)
SELECT 'JIANGXI_TALENT_EVALUATION','江西省人才测评中心','WEB','https://www.jxrcrsksta.com/list/7',1,NOW(),NOW()
WHERE NOT EXISTS (SELECT 1 FROM recruitment_source WHERE source_code='JIANGXI_TALENT_EVALUATION');
