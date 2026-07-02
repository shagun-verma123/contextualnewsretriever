package com.example.contextualnewsretriever.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class TextMatchUtil {

    private TextMatchUtil() {
    }

    /**
     * Token-overlap ratio between the query and the target text, in [0, 1].
     * Simple and explainable: fraction of query tokens found in the target.
     */
    public static double tokenOverlapScore(String query, String text) {
        if (query == null || query.isBlank() || text == null || text.isBlank()) {
            return 0.0;
        }
        Set<String> queryTokens = tokenize(query);
        Set<String> textTokens = tokenize(text);
        if (queryTokens.isEmpty()) {
            return 0.0;
        }
        long matches = queryTokens.stream().filter(textTokens::contains).count();
        return (double) matches / queryTokens.size();
    }

    private static Set<String> tokenize(String value) {
        return Arrays.stream(value.toLowerCase().split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}
