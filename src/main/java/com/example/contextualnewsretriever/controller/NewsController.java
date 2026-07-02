package com.example.contextualnewsretriever.controller;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.NewsResponse;
import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.strategy.NewsStrategyFactory;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class NewsController {

    private final NewsStrategyFactory strategyFactory;


    @GetMapping("/category")
    public NewsResponse byCategory(@RequestParam @NotBlank String category,
                                    @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        RetrievalCriteria criteria = RetrievalCriteria.builder().category(category).limit(limit).build();
        List<ArticleResponse> articles = strategyFactory.getStrategy(NewsIntent.CATEGORY).retrieve(criteria);
        return NewsResponse.of("category", articles);
    }

    @GetMapping("/source")
    public NewsResponse bySource(@RequestParam @NotBlank String source,
                                  @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        RetrievalCriteria criteria = RetrievalCriteria.builder().source(source).limit(limit).build();
        List<ArticleResponse> articles = strategyFactory.getStrategy(NewsIntent.SOURCE).retrieve(criteria);
        return NewsResponse.of("source", articles);
    }

    @GetMapping("/score")
    public NewsResponse byScore(@RequestParam(defaultValue = "0.0") @DecimalMin("0.0") @DecimalMax("1.0") double minScore,
                                 @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        RetrievalCriteria criteria = RetrievalCriteria.builder().minScore(minScore).limit(limit).build();
        List<ArticleResponse> articles = strategyFactory.getStrategy(NewsIntent.SCORE).retrieve(criteria);
        return NewsResponse.of("score", articles);
    }

    @GetMapping("/search")
    public NewsResponse search(@RequestParam @NotBlank String query,
                                @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        RetrievalCriteria criteria = RetrievalCriteria.builder().searchQuery(query).limit(limit).build();
        List<ArticleResponse> articles = strategyFactory.getStrategy(NewsIntent.SEARCH).retrieve(criteria);
        return NewsResponse.of("search", articles);
    }

    @GetMapping("/nearby")
    public NewsResponse nearby(@RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double lat,
                                @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double lon,
                                @RequestParam(required = false) @DecimalMin("0.0") Double radius,
                                @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        RetrievalCriteria criteria = RetrievalCriteria.builder()
                .latitude(lat)
                .longitude(lon)
                .radiusKm(radius)
                .limit(limit)
                .build();
        List<ArticleResponse> articles = strategyFactory.getStrategy(NewsIntent.NEARBY).retrieve(criteria);
        return NewsResponse.of("nearby", articles);
    }
}
