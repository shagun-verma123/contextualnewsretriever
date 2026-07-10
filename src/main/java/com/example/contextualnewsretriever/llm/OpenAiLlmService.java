package com.example.contextualnewsretriever.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Real LLM-backed implementation. Only wired in when llm.provider=openai - kept isolated
 * behind the LlmService interface so the rest of the app never depends on this directly.
 * Output is still parsed defensively: an unparseable or malformed response falls back to
 * a SEARCH intent rather than propagating an exception up to the caller.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public class OpenAiLlmService implements LlmService {

    private static final String SYSTEM_PROMPT = """
            You are a query understanding engine for a news retrieval API. Given a user's \
            natural language query, respond with STRICT JSON only, no prose, matching exactly \
            this shape: {"intent": "category|source|score|search|nearby", "entities": {"${intent}": "${valueOfTheIntent}"}, "searchQuery": "..."}. \
            Choose "nearby" when the user references their location or proximity. Choose "category" \
            when they name a topic (world, national, business, technology, sports, entertainment, \
            health, politics, science). Choose "source" when they name a publication. Choose "score" \
            when they ask for top/highest-rated/most relevant articles. Otherwise choose "search". \
            Always populate searchQuery with the core topic terms.""";

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public OpenAiLlmService(@Value("${llm.openai.api-key:}") String apiKey,
                             @Value("${llm.openai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    @Override
    public QueryUnderstandingResponse understandQuery(String query) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", new Object[] {
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", query)
            });
            body.put("temperature", 0);

            String response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            String content = root.at("/choices/0/message/content").asText();
            return objectMapper.readValue(content, QueryUnderstandingResponse.class);
        } catch (Exception e) {
            log.warn("OpenAI query understanding failed, falling back to SEARCH intent: {}", e.getMessage());
            return QueryUnderstandingResponse.builder()
                    .intent("search")
                    .entities(Map.of())
                    .searchQuery(query)
                    .build();
        }
    }

    @Override
    public String summarizeArticle(String title, String description) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", new Object[] {
                    Map.of("role", "system", "content", "Summarize this news article in one short sentence."),
                    Map.of("role", "user", "content", title + ". " + description)
            });
            body.put("temperature", 0.3);

            String response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.at("/choices/0/message/content").asText();
        } catch (Exception e) {
            log.warn("OpenAI summarization failed, falling back to title: {}", e.getMessage());
            return title;
        }
    }
}
