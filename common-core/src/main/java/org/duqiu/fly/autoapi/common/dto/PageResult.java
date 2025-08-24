package org.duqiu.fly.autoapi.common.dto;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PageResult<T> {
    
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.content = page.getContent();
        result.pageNumber = page.getNumber();
        result.pageSize = page.getSize();
        result.totalElements = page.getTotalElements();
        result.totalPages = page.getTotalPages();
        result.first = page.isFirst();
        result.last = page.isLast();
        return result;
    }
}