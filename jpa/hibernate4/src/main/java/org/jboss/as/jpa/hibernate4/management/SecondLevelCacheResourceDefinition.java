/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jpa.hibernate4.management;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jpa.spi.PersistenceUnitServiceRegistry;
import org.jboss.dmr.ModelType;

/**
 * {@link org.jboss.as.controller.ResourceDefinition} for a resource representing a Hibernate Second Level Cache region.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SecondLevelCacheResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition HIT_COUNT = new SimpleAttributeDefinitionBuilder("hit-count", ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition MISS_COUNT = new SimpleAttributeDefinitionBuilder("miss-count", ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition PUT_COUNT = new SimpleAttributeDefinitionBuilder("put-count", ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition ELEMENT_COUNT_IN_MEMORY = new SimpleAttributeDefinitionBuilder("element-count-in-memory", ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    // size in memory and element count on disk is not supported by org.hibernate.cache.infinispan.impl.BaseRegion
    //public static final SimpleAttributeDefinition ELEMENT_COUNT_ON_DISK = new SimpleAttributeDefinitionBuilder("element-count-on-disk", ModelType.LONG)
    //        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
    //        .build();

    //public static final SimpleAttributeDefinition SIZE_IN_MEMORY = new SimpleAttributeDefinitionBuilder("size-in-memory", ModelType.LONG)
    //        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
    //        .setMeasurementUnit(MeasurementUnit.BYTES)
    //        .build();

    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;

    SecondLevelCacheResourceDefinition(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        super(PathElement.pathElement(HibernateDescriptionConstants.ENTITYCACHE),
            HibernateDescriptions.getResourceDescriptionResolver(HibernateDescriptionConstants.SECOND_LEVEL_CACHE));
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerMetric(HIT_COUNT, SecondLevelCacheMetricsHandler.getHitCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(MISS_COUNT, SecondLevelCacheMetricsHandler.getMissCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(PUT_COUNT, SecondLevelCacheMetricsHandler.getPutCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(ELEMENT_COUNT_IN_MEMORY, SecondLevelCacheMetricsHandler.getElementCountInMemory(persistenceUnitRegistry));
        // resourceRegistration.registerMetric(ELEMENT_COUNT_ON_DISK, SecondLevelCacheMetricsHandler.getElementCountOnDisk(persistenceUnitRegistry));
        // resourceRegistration.registerMetric(SIZE_IN_MEMORY, SecondLevelCacheMetricsHandler.getSizeInMemory(persistenceUnitRegistry));
    }
}
