package com.example.contextualnewsretriever.controller;

import com.example.contextualnewsretriever.dto.NewsResponse;
import com.example.contextualnewsretriever.dto.SmartQueryRequest;
import com.example.contextualnewsretriever.service.NewsQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class QueryController {

    private final NewsQueryService newsQueryService;

    @PostMapping("/query")
    public NewsResponse smartQuery(@Valid @RequestBody SmartQueryRequest request) {
        return newsQueryService.handleSmartQuery(request);
    }
}
