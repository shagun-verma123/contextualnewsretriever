package com.example.contextualnewsretriever.service;

import com.example.contextualnewsretriever.dto.ImportResultResponse;
import com.example.contextualnewsretriever.entity.NewsArticle;
import com.example.contextualnewsretriever.repository.NewsArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsImportService {

    private static final String DATA_FILE = "news_data.json";

    private final NewsArticleRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Transactional
    public ImportResultResponse importFromClasspath() {
        List<NewsArticle> parsed = readArticles();
        int inserted = 0;
        int skipped = 0;
        List<NewsArticle> toSave = new ArrayList<>();

        for (NewsArticle article : parsed) {
            if (repository.existsById(article.getId())) {
                skipped++;
            } else {
                toSave.add(article);
                inserted++;
            }
        }
        repository.saveAll(toSave);

        log.info("News import complete: {} inserted, {} skipped (already present)", inserted, skipped);
        toSave.forEach(article -> log.info("Inserted article id={} title=\"{}\"", article.getId(), article.getTitle()));

        return ImportResultResponse.builder()
                .totalRecordsRead(parsed.size())
                .inserted(inserted)
                .skippedDuplicates(skipped)
                .build();
    }

    private List<NewsArticle> readArticles() {
        try (InputStream is = new ClassPathResource(DATA_FILE).getInputStream()) {
            ArrayNode root = (ArrayNode) objectMapper.readTree(is);
            List<NewsArticle> articles = new ArrayList<>();
            for (JsonNode node : root) {
                articles.add(toEntity(node));
            }
            return articles;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + DATA_FILE + " from classpath", e);
        }
    }

    private NewsArticle toEntity(JsonNode node) {
        List<String> categories = new ArrayList<>();
        if (node.has("category") && node.get("category").isArray()) {
            node.get("category").forEach(c -> categories.add(c.asText()));
        }
        return NewsArticle.builder()
                .id(node.get("id").asText())
                .title(textOrNull(node, "title"))
                .description(textOrNull(node, "description"))
                .url(textOrNull(node, "url"))
                .publicationDate(LocalDateTime.parse(node.get("publication_date").asText()))
                .sourceName(textOrNull(node, "source_name"))
                .relevanceScore(node.has("relevance_score") ? node.get("relevance_score").asDouble() : null)
                .latitude(node.has("latitude") ? node.get("latitude").asDouble() : null)
                .longitude(node.has("longitude") ? node.get("longitude").asDouble() : null)
                .categories(categories)
                .build();
    }

    private String textOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}
