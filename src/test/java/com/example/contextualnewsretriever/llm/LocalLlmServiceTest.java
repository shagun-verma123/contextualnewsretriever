package com.example.contextualnewsretriever.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalLlmServiceTest {

    private final LocalLlmService llmService = new LocalLlmService();

    @Test
    void detectsNearbyIntent() {
        QueryUnderstandingResponse response = llmService.understandQuery("Show me news near me");
        assertThat(response.getIntent()).isEqualTo("nearby");
    }

    @Test
    void detectsSourceIntent() {
        QueryUnderstandingResponse response = llmService.understandQuery("Show me news from Reuters");
        assertThat(response.getIntent()).isEqualTo("source");
        assertThat(response.getEntities()).containsEntry("source", "reuters");
    }

    @Test
    void detectsCategoryIntent() {
        QueryUnderstandingResponse response = llmService.understandQuery("Give me the latest technology news");
        assertThat(response.getIntent()).isEqualTo("category");
        assertThat(response.getEntities()).containsEntry("category", "technology");
    }

    @Test
    void detectsScoreIntent() {
        QueryUnderstandingResponse response = llmService.understandQuery("Show me the top rated articles");
        assertThat(response.getIntent()).isEqualTo("score");
    }

    @Test
    void fallsBackToSearchIntent() {
        QueryUnderstandingResponse response = llmService.understandQuery("Tell me about the election results");
        assertThat(response.getIntent()).isEqualTo("search");
        assertThat(response.getSearchQuery()).isEqualTo("Tell me about the election results");
    }

    @Test
    void summarizeArticleTruncatesLongDescriptions() {
        String longDescription = "x".repeat(300);
        String summary = llmService.summarizeArticle("Title", longDescription);
        assertThat(summary).endsWith("...");
        assertThat(summary.length()).isLessThanOrEqualTo(163);
    }
}
