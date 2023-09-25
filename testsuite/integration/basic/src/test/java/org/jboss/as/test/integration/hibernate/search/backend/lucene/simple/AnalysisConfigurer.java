/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.search.backend.lucene.simple;

import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class AnalysisConfigurer
        implements LuceneAnalysisConfigurer {
    public static final String AUTOCOMPLETE = "autocomplete";
    public static final String AUTOCOMPLETE_QUERY = "autocomplete-query";

    @Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer(AUTOCOMPLETE).custom()
                .tokenizer("whitespace")
                .tokenFilter("lowercase")
                .tokenFilter("asciiFolding")
                .tokenFilter("edgeNGram")
                        .param("minGramSize", "1")
                        .param("maxGramSize", "10");
        context.analyzer(AUTOCOMPLETE_QUERY).custom()
                .tokenizer("whitespace")
                .tokenFilter("lowercase")
                .tokenFilter("asciiFolding");
    }
}
