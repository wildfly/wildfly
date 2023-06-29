/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
