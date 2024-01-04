/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.backend.elasticsearch.simple;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@Indexed
public class Book {

    @Id
    @GeneratedValue
    Long id;

    @FullTextField
    @FullTextField(name = "title_autocomplete",
            analyzer = AnalysisConfigurer.AUTOCOMPLETE,
            searchAnalyzer = AnalysisConfigurer.AUTOCOMPLETE_QUERY)
    String title;

}
