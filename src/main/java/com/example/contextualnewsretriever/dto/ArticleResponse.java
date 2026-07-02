package com.example.contextualnewsretriever.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleResponse {
    private String id;
    private String title;
    private String description;
    private String url;
    private LocalDateTime publicationDate;
    private String sourceName;
    private Double relevanceScore;
    private Double latitude;
    private Double longitude;
    private List<String> categories;

    /** Only populated for endpoints that compute a distance (nearby) or trending score. */
    private Double distanceKm;
    private Double trendingScore;

    /** Only populated by the smart /query endpoint when the LLM layer enriches results. */
    private String llmSummary;
}
