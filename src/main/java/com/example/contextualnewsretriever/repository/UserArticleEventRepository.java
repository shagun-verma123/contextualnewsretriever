package com.example.contextualnewsretriever.repository;

import com.example.contextualnewsretriever.entity.UserArticleEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserArticleEventRepository extends JpaRepository<UserArticleEvent, Long> {

    @Query("select e from UserArticleEvent e where e.createdAt >= :since")
    List<UserArticleEvent> findRecentEvents(@Param("since") LocalDateTime since);
}
