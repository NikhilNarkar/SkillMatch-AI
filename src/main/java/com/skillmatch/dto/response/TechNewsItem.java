package com.skillmatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TechNewsItem {
    private String title;
    private String url;
    private String source;
    private String publishedAt; // ISO string
}

