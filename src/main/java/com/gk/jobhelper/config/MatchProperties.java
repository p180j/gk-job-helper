package com.gk.jobhelper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 匹配引擎配置（match.*），可在 application.yml 中覆盖
 */
@Component
@ConfigurationProperties(prefix = "match")
public class MatchProperties {

    /** 批量匹配每批加载的岗位数量 */
    private int batchSize = 200;

    /** 批量匹配失败明细最大返回条数（避免响应过大） */
    private int maxFailedItems = 20;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxFailedItems() {
        return maxFailedItems;
    }

    public void setMaxFailedItems(int maxFailedItems) {
        this.maxFailedItems = maxFailedItems;
    }
}
