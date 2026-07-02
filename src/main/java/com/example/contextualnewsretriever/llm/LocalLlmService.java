package com.example.contextualnewsretriever.llm;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keyword/regex-based "LLM" that runs fully in-process, so the app works offline out of the
 * box with no API key. Active by default; only replaced by OpenAiLlmService when
 * llm.provider=openai is explicitly configured with an API key.
 */
@Service
@Primary
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class LocalLlmService implements LlmService {

    private static final List<String> KNOWN_CATEGORIES = List.of(
            "world", "national", "business", "technology", "sports", "entertainment", "health", "politics", "science");

    private static final Pattern SOURCE_PATTERN = Pattern.compile("from ([a-zA-Z0-9 .]+?)(?:\\s+(?:near|about|on)\\b|$)");

    @Override
    public QueryUnderstandingResponse understandQuery(String query) {
        String lower = query.toLowerCase();
        Map<String, String> entities = new HashMap<>();

        if (containsAny(lower, "near", "nearby", "close to", "around me")) {
            return QueryUnderstandingResponse.builder()
                    .intent("nearby")
                    .entities(entities)
                    .searchQuery(query)
                    .build();
        }

        Matcher sourceMatcher = SOURCE_PATTERN.matcher(lower);
        if (sourceMatcher.find()) {
            String source = sourceMatcher.group(1).trim();
            entities.put("source", source);
            return QueryUnderstandingResponse.builder()
                    .intent("source")
                    .entities(entities)
                    .searchQuery(query)
                    .build();
        }

        for (String category : KNOWN_CATEGORIES) {
            if (lower.contains(category)) {
                entities.put("category", category);
                return QueryUnderstandingResponse.builder()
                        .intent("category")
                        .entities(entities)
                        .searchQuery(query)
                        .build();
            }
        }

        if (containsAny(lower, "top rated", "highest rated", "most relevant", "best", "high relevance", "high score")) {
            return QueryUnderstandingResponse.builder()
                    .intent("score")
                    .entities(entities)
                    .searchQuery(query)
                    .build();
        }

        return QueryUnderstandingResponse.builder()
                .intent("search")
                .entities(entities)
                .searchQuery(query)
                .build();
    }

    @Override
    public String summarizeArticle(String title, String description) {
        if (description == null || description.isBlank()) {
            return title;
        }
        String trimmed = description.strip();
        int cutoff = Math.min(160, trimmed.length());
        String snippet = trimmed.substring(0, cutoff);
        return snippet.length() < trimmed.length() ? snippet + "..." : snippet;
    }

    private boolean containsAny(String text, String... phrases) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
}
