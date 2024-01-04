/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.hibernate.search.v5migrationhelper.simple;

import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@Indexed
// We know the migration helper is deprecated; we want to test it anyway.
@SuppressWarnings("deprecation")
public class Book {

    @Id
    @GeneratedValue
    Long id;

    @Field
    @Field(name = "title_autocomplete",
            analyzer = @Analyzer(definition = AnalysisConfigurer.AUTOCOMPLETE))
    String title;

}
