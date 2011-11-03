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
 * {@link org.jboss.as.controller.ResourceDefinition} for a resource representing a Hibernate Entity.
 *
 * @author Scott Marlow
 */
public class QueryResourceDefinition extends SimpleResourceDefinition {

    public static final SimpleAttributeDefinition QUERY_NAME = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.ATTRIBUTE_QUERY_NAME, ModelType.STRING)
        .build();

    public static final SimpleAttributeDefinition EXECUTE_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_EXECUTION_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition HIT_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_CACHE_HIT_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition PUT_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_CACHE_PUT_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition MISS_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_CACHE_MISS_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition EXEC_ROW_COUNT = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_EXECUTION_ROW_COUNT, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition AVG_TIME = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_EXECUTION_AVG_TIME, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition MAX_TIME = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_EXECUTION_MAX_TIME, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    public static final SimpleAttributeDefinition MIN_TIME = new SimpleAttributeDefinitionBuilder(HibernateManagementAdaptor.OPERATION_QUERY_EXECUTION_MIN_TIME, ModelType.LONG)
        .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
        .build();

    private final PersistenceUnitServiceRegistry persistenceUnitRegistry;

    QueryResourceDefinition(final PersistenceUnitServiceRegistry persistenceUnitRegistry) {
        super(PathElement.pathElement(HibernateDescriptionConstants.QUERYCACHE),
            HibernateDescriptions.getResourceDescriptionResolver(HibernateDescriptionConstants.QUERY_STATISTICS));
        this.persistenceUnitRegistry = persistenceUnitRegistry;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerMetric(QUERY_NAME, QueryMetricsHandler.getOriginalName(persistenceUnitRegistry));
        resourceRegistration.registerMetric(EXECUTE_COUNT, QueryMetricsHandler.getExecutionCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(HIT_COUNT, QueryMetricsHandler.getCacheHitCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(PUT_COUNT, QueryMetricsHandler.getCachePutCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(MISS_COUNT, QueryMetricsHandler.getCacheMissCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(EXEC_ROW_COUNT, QueryMetricsHandler.getExecutionRowCount(persistenceUnitRegistry));
        resourceRegistration.registerMetric(AVG_TIME, QueryMetricsHandler.getExecutionAvgTime(persistenceUnitRegistry));
        resourceRegistration.registerMetric(MAX_TIME, QueryMetricsHandler.getExecutionMaxTime(persistenceUnitRegistry));
        resourceRegistration.registerMetric(MIN_TIME, QueryMetricsHandler.getExecutionMinTime(persistenceUnitRegistry));
    }
}
