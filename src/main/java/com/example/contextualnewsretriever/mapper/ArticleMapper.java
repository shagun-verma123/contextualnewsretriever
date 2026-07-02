package com.example.contextualnewsretriever.mapper;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.entity.NewsArticle;

public final class ArticleMapper {

    private ArticleMapper() {
    }

    public static ArticleResponse toResponse(NewsArticle article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .description(article.getDescription())
                .url(article.getUrl())
                .publicationDate(article.getPublicationDate())
                .sourceName(article.getSourceName())
                .relevanceScore(article.getRelevanceScore())
                .latitude(article.getLatitude())
                .longitude(article.getLongitude())
                .categories(article.getCategories())
                .build();
    }
}
