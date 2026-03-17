package com.skillmatch.controller;

import com.skillmatch.dto.response.TechNewsItem;
import com.skillmatch.service.TechNewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TechNewsController {

    private final TechNewsService techNewsService;

    @GetMapping("/tech")
    public ResponseEntity<List<TechNewsItem>> tech(@RequestParam(defaultValue = "9") int limit) {
        List<TechNewsItem> items = techNewsService.getLatestTechNews(limit);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(0, TimeUnit.SECONDS).cachePrivate().mustRevalidate())
                .body(items);
    }
}

