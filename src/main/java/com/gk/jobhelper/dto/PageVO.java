package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用分页响应
 */
public class PageVO<T> {

    private long total;
    private int page;
    private int size;
    private List<T> items = new ArrayList<>();

    public PageVO() {
    }

    public PageVO(long total, int page, int size, List<T> items) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }
}
