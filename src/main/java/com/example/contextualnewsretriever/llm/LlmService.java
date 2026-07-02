package com.example.contextualnewsretriever.llm;

public interface LlmService {

    /** Parses a free-text query into a strict {intent, entities, searchQuery} contract. */
    QueryUnderstandingResponse understandQuery(String query);

    /** Produces a short, human-readable summary/highlight for a single article. */
    String summarizeArticle(String title, String description);
}
