package com.example.contextualnewsretriever.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NewsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void categoryEndpointReturnsRankedArticles() throws Exception {
        mockMvc.perform(get("/api/v1/news/category").param("category", "world").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.intent").value("category"))
                .andExpect(jsonPath("$.articles").isArray());
    }

    @Test
    void categoryEndpointRejectsBlankCategory() throws Exception {
        mockMvc.perform(get("/api/v1/news/category").param("category", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchEndpointReturnsRankedArticles() throws Exception {
        mockMvc.perform(get("/api/v1/news/search").param("query", "technology").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.intent").value("search"));
    }

    @Test
    void nearbyEndpointRequiresLatAndLon() throws Exception {
        mockMvc.perform(get("/api/v1/news/nearby").param("lat", "28.61"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nearbyEndpointReturnsArticlesWithinRadius() throws Exception {
        mockMvc.perform(get("/api/v1/news/nearby")
                        .param("lat", "28.6139")
                        .param("lon", "77.2090")
                        .param("radius", "500")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.intent").value("nearby"));
    }

    @Test
    void smartQueryEndpointUsesMockLlmAndEnrichesResults() throws Exception {
        mockMvc.perform(post("/api/v1/news/query")
                        .contentType("application/json")
                        .content("{\"query\":\"Show me the latest technology news\",\"limit\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.intent").value("category"));
    }

    @Test
    void trendingEndpointReturnsScoredArticlesAfterSimulation() throws Exception {
        mockMvc.perform(post("/api/v1/news/events/simulate").param("count", "50"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/news/trending").param("lat", "28.6139").param("lon", "77.2090"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metadata.intent").value("trending"));
    }
}
