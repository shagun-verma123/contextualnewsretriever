package com.example.contextualnewsretriever.repository;

import com.example.contextualnewsretriever.entity.NewsArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, String> {

    @Query("select a from NewsArticle a join a.categories c where lower(c) = lower(:category) order by a.publicationDate desc")
    List<NewsArticle> findByCategory(@Param("category") String category, Pageable pageable);

    List<NewsArticle> findBySourceNameIgnoreCaseOrderByPublicationDateDesc(String sourceName, Pageable pageable);

    List<NewsArticle> findByRelevanceScoreGreaterThanEqualOrderByRelevanceScoreDesc(Double minScore, Pageable pageable);

    @Query("select a from NewsArticle a where a.latitude is not null and a.longitude is not null")
    List<NewsArticle> findAllWithCoordinates();

}
