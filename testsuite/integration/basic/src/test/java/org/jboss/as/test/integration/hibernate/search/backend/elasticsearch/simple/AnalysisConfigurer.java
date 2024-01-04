/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.simple;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;

public class AnalysisConfigurer
        implements ElasticsearchAnalysisConfigurer {
    public static final String AUTOCOMPLETE = "autocomplete";
    public static final String AUTOCOMPLETE_QUERY = "autocomplete-query";

    @Override
    public void configure(ElasticsearchAnalysisConfigurationContext context) {
        context.analyzer(AUTOCOMPLETE).custom()
                .tokenizer("whitespace")
                .tokenFilters("lowercase", "asciifolding", "my-edge-ngram");
        context.tokenFilter("my-edge-ngram").type("edge_ngram")
                .param("min_gram", 1)
                .param("max_gram", 10);

        context.analyzer(AUTOCOMPLETE_QUERY).custom()
                .tokenizer("whitespace")
                .tokenFilters("lowercase", "asciifolding");
    }
}
