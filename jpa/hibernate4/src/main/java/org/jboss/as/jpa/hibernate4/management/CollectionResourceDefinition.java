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
 * {@link org.jboss.as.controller.ResourceDefinition} for a resource representing a Hibernate collection.
 *
 * @author Scott Marlow
 */
public class CollectionResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition LOAD_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_COLLECTION_LOAD_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition FETCH_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_COLLECTION_FETCH_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition RECREATE_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_COLLECTION_RECREATED_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition REMOVE_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_COLLECTION_REMOVE_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition UPDATE_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_COLLECTION_UPDATE_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;

    CollectionResourceDefinition(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        super(PathElement.pathElement(HibernateDescriptionConstants.COLLECTION),
            HibernateDescriptions.getResourceDescriptionResolver(HibernateDescriptionConstants.COLLECTION_STATISTICS));
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerMetric(LOAD_COUNT, CollectionMetricsHandler.getLoadCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(FETCH_COUNT, CollectionMetricsHandler.getFetchCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(RECREATE_COUNT, CollectionMetricsHandler.getRecreateCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(REMOVE_COUNT, CollectionMetricsHandler.getRemoveCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(UPDATE_COUNT, CollectionMetricsHandler.getUpdateCount(persistenceUnitRegistry));
    }
}
