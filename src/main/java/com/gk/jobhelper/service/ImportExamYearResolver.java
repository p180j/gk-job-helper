package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.ImportFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 岗位表考试年度的统一解析规则。数据库值优先，旧记录可从原始文件名兼容识别。 */
public final class ImportExamYearResolver {
    private static final Pattern YEAR = Pattern.compile("(?<!\\d)(20\\d{2})(?!\\d)");

    private ImportExamYearResolver() {}

    public static Integer resolve(ImportFile record) {
        if (record == null) return null;
        return record.getExamYear() != null ? record.getExamYear() : resolve(record.getOriginalName());
    }

    public static Integer resolve(String text) {
        if (text == null) return null;
        Matcher matcher = YEAR.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }
}
