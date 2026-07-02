package com.example.contextualnewsretriever.controller;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.NewsResponse;
import com.example.contextualnewsretriever.service.TrendingService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class TrendingController {

    private final TrendingService trendingService;

    @GetMapping("/trending")
    public NewsResponse trending(@RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
                                  @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        List<ArticleResponse> articles = trendingService.getTrending(lat, lon, limit);
        return NewsResponse.of("trending", articles);
    }
}
