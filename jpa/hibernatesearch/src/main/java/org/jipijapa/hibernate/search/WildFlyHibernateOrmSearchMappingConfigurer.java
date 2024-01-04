/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jipijapa.hibernate.search;

import org.hibernate.search.mapper.orm.mapping.HibernateOrmMappingConfigurationContext;
import org.hibernate.search.mapper.orm.mapping.HibernateOrmSearchMappingConfigurer;
import org.jboss.jandex.Index;

import java.util.Collection;

final class WildFlyHibernateOrmSearchMappingConfigurer implements HibernateOrmSearchMappingConfigurer {
    private Collection<Index> indexes;

    public WildFlyHibernateOrmSearchMappingConfigurer(Collection<Index> indexes) {
        this.indexes = indexes;
    }

    @Override
    public void configure(HibernateOrmMappingConfigurationContext context) {
        // Hibernate Search can't deal with WildFly's JAR URLs using vfs://,
        // so it fails to load Jandex indexes from the JARs.
        // Regardless, WildFly already has Jandex indexes in memory,
        // so we'll configure Hibernate Search to just use those.
        context.annotationMapping().discoverJandexIndexesFromAddedTypes(false)
                 .buildMissingDiscoveredJandexIndexes(false);
        if (indexes != null) {
            for (Index index : indexes) {
                // This class is garbage-collected after bootstrap,
                // so the reference to indexes does not matter.
                context.annotationMapping().addJandexIndex(index);
            }
        }
    }

    public void clearIndexReferences() {
        indexes = null;
    }
}
