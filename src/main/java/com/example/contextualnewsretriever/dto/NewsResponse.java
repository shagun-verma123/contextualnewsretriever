package com.example.contextualnewsretriever.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsResponse {
    private ResponseMetadata metadata;
    private List<ArticleResponse> articles;

    public static NewsResponse of(String intent, List<ArticleResponse> articles) {
        return NewsResponse.builder()
                .metadata(ResponseMetadata.builder()
                        .intent(intent)
                        .count(articles.size())
                        .message("OK")
                        .build())
                .articles(articles)
                .build();
    }
}
