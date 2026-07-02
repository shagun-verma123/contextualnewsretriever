package com.example.contextualnewsretriever.strategy;

import com.example.contextualnewsretriever.enums.NewsIntent;
import com.example.contextualnewsretriever.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class NewsStrategyFactory {

    private final Map<NewsIntent, NewsRetrievalStrategy> strategies;

    public NewsStrategyFactory(List<NewsRetrievalStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(NewsRetrievalStrategy::supportedIntent, Function.identity()));
    }

    public NewsRetrievalStrategy getStrategy(NewsIntent intent) {
        NewsRetrievalStrategy strategy = strategies.get(intent);
        if (strategy == null) {
            throw new InvalidRequestException("Unsupported intent: " + intent);
        }
        return strategy;
    }
}
