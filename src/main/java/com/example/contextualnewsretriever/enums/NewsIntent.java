package com.example.contextualnewsretriever.enums;

public enum NewsIntent {
    CATEGORY,
    SOURCE,
    SCORE,
    SEARCH,
    NEARBY;

    public static NewsIntent fromStringOrDefault(String value, NewsIntent fallback) {
        if (value == null) {
            return fallback;
        }
        for (NewsIntent intent : values()) {
            if (intent.name().equalsIgnoreCase(value.trim())) {
                return intent;
            }
        }
        return fallback;
    }
}
