package com.example.contextualnewsretriever.strategy;

import com.example.contextualnewsretriever.dto.ArticleResponse;
import com.example.contextualnewsretriever.dto.RetrievalCriteria;
import com.example.contextualnewsretriever.enums.NewsIntent;

import java.util.List;

public interface NewsRetrievalStrategy {

    NewsIntent supportedIntent();

    List<ArticleResponse> retrieve(RetrievalCriteria criteria);
}
