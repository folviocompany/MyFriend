package com.isp.assistant.service;

import java.util.List;

import com.isp.assistant.config.AppProperties;
import com.isp.assistant.knowledge.entity.KnowledgeArticle;
import com.isp.assistant.knowledge.entity.KnowledgeCategory;
import com.isp.assistant.knowledge.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeArticleRepository repository;

    private KnowledgeService knowledgeService;

    @BeforeEach
    void setUp() {
        knowledgeService = new KnowledgeService(repository, properties(3, 120));
    }

    @Test
    void buildsSafeOrQueryAndLimitsRepositoryResults() {
        when(repository.search(anyString(), any(Pageable.class))).thenReturn(List.of());

        knowledgeService.findRelevantContext(
                "O cliente está com LOS na ONU. LOS or quais verificações devo realizar?");

        ArgumentCaptor<String> query = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).search(query.capture(), pageable.capture());
        assertThat(query.getValue())
                .isEqualTo("cliente or los or onu or quais or verificações or devo or realizar");
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
    }

    @Test
    void keepsOnlyFirstFifteenDistinctTerms() {
        String query = knowledgeService.buildSearchQuery(
                "alfa beta gama delta epsilon zeta eta theta iota kappa lambda omicron sigma tau upsilon phi chi psi");

        assertThat(query.split(" or ")).containsExactly(
                "alfa", "beta", "gama", "delta", "epsilon", "zeta", "eta", "theta",
                "iota", "kappa", "lambda", "omicron", "sigma", "tau", "upsilon");
    }

    @Test
    void returnsEmptyContextWithoutSearchingWhenThereAreNoUsefulTerms() {
        String context = knowledgeService.findRelevantContext("a e o or 12");

        assertThat(context).isEmpty();
        verify(repository, never()).search(anyString(), any(Pageable.class));
    }

    @Test
    void formatsArticlesAndTruncatesTotalContext() {
        KnowledgeArticle first = new KnowledgeArticle(
                "LOS na ONU", KnowledgeCategory.FIBRA_ONU, "A".repeat(100));
        KnowledgeArticle second = new KnowledgeArticle(
                "Outro artigo", KnowledgeCategory.DIAGNOSTICO_CONECTIVIDADE, "B".repeat(100));
        when(repository.search(anyString(), any(Pageable.class))).thenReturn(List.of(first, second));

        String context = knowledgeService.findRelevantContext("los onu diagnóstico");

        assertThat(context)
                .hasSize(120)
                .startsWith("Título: LOS na ONU\nCategoria: FIBRA_ONU\nConteúdo: ");
    }

    private AppProperties properties(int maxArticles, int maxChars) {
        return new AppProperties(
                new AppProperties.Ai("gemini", 1024, 30),
                new AppProperties.Chat(2000),
                "",
                20,
                new AppProperties.Knowledge(maxArticles, maxChars));
    }
}
