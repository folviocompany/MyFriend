package com.isp.assistant.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.isp.assistant.config.AppProperties;
import com.isp.assistant.knowledge.entity.KnowledgeArticle;
import com.isp.assistant.knowledge.repository.KnowledgeArticleRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeService {

    private static final int MAX_SEARCH_TERMS = 15;
    private static final Pattern TERM_PATTERN = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of(
            "aos", "com", "das", "dos", "ela", "ele", "eles", "elas", "esta", "está",
            "estão", "nas", "nos", "para", "pela", "pelo", "por", "que", "uma", "umas",
            "uns", "você", "or");

    private final KnowledgeArticleRepository repository;
    private final AppProperties properties;

    public KnowledgeService(KnowledgeArticleRepository repository, AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public String findRelevantContext(String message) {
        String searchQuery = buildSearchQuery(message);
        if (searchQuery.isEmpty()) {
            return "";
        }

        List<KnowledgeArticle> articles = repository.search(
                searchQuery,
                PageRequest.of(0, properties.knowledge().maxArticles()));
        return formatContext(articles, properties.knowledge().maxChars());
    }

    String buildSearchQuery(String message) {
        if (message == null) {
            return "";
        }

        Matcher matcher = TERM_PATTERN.matcher(message.toLowerCase(Locale.ROOT));
        Set<String> terms = new LinkedHashSet<>();
        while (matcher.find() && terms.size() < MAX_SEARCH_TERMS) {
            String term = matcher.group();
            if (term.length() >= 3 && !STOP_WORDS.contains(term)) {
                terms.add(term);
            }
        }
        return String.join(" or ", terms);
    }

    private String formatContext(List<KnowledgeArticle> articles, int maxChars) {
        StringBuilder context = new StringBuilder(Math.min(maxChars, 1024));
        for (KnowledgeArticle article : articles) {
            String block = "Título: %s\nCategoria: %s\nConteúdo: %s".formatted(
                    article.getTitle(), article.getCategory(), article.getContent());
            appendWithinLimit(context, block, maxChars);
            if (context.length() == maxChars) {
                break;
            }
        }
        return context.toString();
    }

    private void appendWithinLimit(StringBuilder context, String block, int maxChars) {
        String separator = context.isEmpty() ? "" : "\n\n";
        int remaining = maxChars - context.length();
        if (remaining <= separator.length()) {
            return;
        }

        context.append(separator);
        int blockLimit = Math.min(block.length(), maxChars - context.length());
        context.append(block, 0, blockLimit);
    }
}
