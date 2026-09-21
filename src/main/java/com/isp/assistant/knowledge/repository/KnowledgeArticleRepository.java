package com.isp.assistant.knowledge.repository;

import java.util.List;

import com.isp.assistant.knowledge.entity.KnowledgeArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {

    @Query(value = """
            SELECT article.*
            FROM knowledge_article article
            WHERE article.search_vector @@ websearch_to_tsquery('portuguese', :query)
            ORDER BY ts_rank(
                article.search_vector,
                websearch_to_tsquery('portuguese', :query)
            ) DESC, article.id ASC
            """, nativeQuery = true)
    List<KnowledgeArticle> search(@Param("query") String query, Pageable pageable);
}
