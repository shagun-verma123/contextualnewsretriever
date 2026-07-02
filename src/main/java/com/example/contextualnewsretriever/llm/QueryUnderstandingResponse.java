package com.example.contextualnewsretriever.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/** Strict JSON contract returned by the LlmService: {intent, entities, searchQuery}. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryUnderstandingResponse {
    private String intent;
    private Map<String, String> entities;
    private String searchQuery;
}
