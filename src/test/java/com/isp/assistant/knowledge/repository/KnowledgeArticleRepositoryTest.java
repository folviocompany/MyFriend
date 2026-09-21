package com.isp.assistant.knowledge.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.isp.assistant.ai.TechnicalAssistant;
import com.isp.assistant.knowledge.entity.KnowledgeArticle;
import com.isp.assistant.knowledge.entity.KnowledgeCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "spring.ai.model.chat=none")
@Testcontainers(disabledWithoutDocker = true)
@Transactional
class KnowledgeArticleRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private KnowledgeArticleRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TechnicalAssistant technicalAssistant;

    @Test
    void flywayLoadsSeedAndCreatesGinIndex() {
        assertThat(repository.count()).isEqualTo(8);

        Integer ginIndexCount = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM pg_indexes
                WHERE tablename = 'knowledge_article'
                  AND indexname = 'idx_knowledge_article_search_vector'
                  AND indexdef ILIKE '%using gin%'
                """, Integer.class);

        assertThat(ginIndexCount).isEqualTo(1);
    }

    @Test
    void losOnuQuestionReturnsSeededArticle() {
        String query = "cliente or los or onu or quais or verificações or devo or realizar";

        List<KnowledgeArticle> results = repository.search(query, PageRequest.of(0, 3));

        assertThat(results)
                .extracting(KnowledgeArticle::getTitle)
                .contains("LED LOS aceso ou piscando na ONU");
    }

    @Test
    void mapsCategoryAndHonorsResultLimit() {
        List<KnowledgeArticle> results = repository.search("latência or perda or diagnóstico", PageRequest.of(0, 1));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getCategory())
                .isIn(KnowledgeCategory.LATENCIA_PERDA_PACOTES, KnowledgeCategory.DIAGNOSTICO_CONECTIVIDADE);
    }
}
