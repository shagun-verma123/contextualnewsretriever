package com.example.contextualnewsretriever.dto;

import com.example.contextualnewsretriever.enums.NewsIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared input contract for every NewsRetrievalStrategy - built either directly from
 * request params (direct REST endpoints) or derived from LLM output (smart /query endpoint).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetrievalCriteria {
    private NewsIntent intent;
    private String category;
    private String source;
    private Double minScore;
    private String searchQuery;
    private Double latitude;
    private Double longitude;
    private Double radiusKm;
    private int limit;
}
