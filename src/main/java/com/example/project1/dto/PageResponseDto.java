package com.example.project1.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

// Stable JSON shape for paged results, instead of serializing Spring's PageImpl directly
@Getter
public class PageResponseDto<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PageResponseDto(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}
