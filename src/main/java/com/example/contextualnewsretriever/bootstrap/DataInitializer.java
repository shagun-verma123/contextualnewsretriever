package com.example.contextualnewsretriever.bootstrap;

import com.example.contextualnewsretriever.dto.ImportResultResponse;
import com.example.contextualnewsretriever.service.NewsImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Loads news_data.json into H2 automatically on startup, since H2 is in-memory and
 * resets on every restart. Article-level ids are logged by NewsImportService itself.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final NewsImportService newsImportService;

    @Override
    public void run(String... args) {
        log.info("Starting automatic news data import from classpath...");
        ImportResultResponse result = newsImportService.importFromClasspath();
        log.info("Startup import finished: totalRecordsRead={}, inserted={}, skippedDuplicates={}",
                result.getTotalRecordsRead(), result.getInserted(), result.getSkippedDuplicates());
    }
}
