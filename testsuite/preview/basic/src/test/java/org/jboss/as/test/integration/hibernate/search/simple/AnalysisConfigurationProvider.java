package org.jboss.as.test.integration.hibernate.search.simple;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ngram.EdgeNGramFilterFactory;
import org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionRegistryBuilder;

public class AnalysisConfigurationProvider
        implements org.hibernate.search.analyzer.definition.LuceneAnalysisDefinitionProvider {
    public static final String AUTOCOMPLETE = "autocomplete";
    public static final String AUTOCOMPLETE_QUERY = "autocomplete-query";

    @Override
    public void register(LuceneAnalysisDefinitionRegistryBuilder builder) {
        builder.analyzer(AUTOCOMPLETE)
                .tokenizer(WhitespaceTokenizerFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class)
                .tokenFilter(ASCIIFoldingFilterFactory.class)
                .tokenFilter(EdgeNGramFilterFactory.class)
                        .param("minGramSize", "1")
                        .param("maxGramSize", "10");
        builder.analyzer(AUTOCOMPLETE_QUERY)
                .tokenizer(WhitespaceTokenizerFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class)
                .tokenFilter(ASCIIFoldingFilterFactory.class);
    }
}
