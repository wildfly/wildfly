/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DISABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ENABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_PROPERTIES_ATTRIBUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DUMP_QUEUED_THREADS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_ALL_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_GRACEFULLY_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_IDLE_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_INVALID_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.TEST_CONNECTION;

import java.util.List;
import java.util.Map;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.subsystems.common.pool.PoolConfigurationRWHandler;
import org.jboss.as.connector.subsystems.common.pool.PoolOperations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.dmr.ModelNode;

/**
 * @author Stefano Maestri
 */
public class DataSourceDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DATASOURCE = PathElement.pathElement(DATA_SOURCE);

    // The ManagedConnectionPool implementation used by default by versions < 4.0.0 (WildFly 10)
    private static final String LEGACY_MCP = "org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool";

    private final boolean registerRuntimeOnly;
    private final boolean deployed;

    private final List<AccessConstraintDefinition> accessConstraints;

    private DataSourceDefinition(final boolean registerRuntimeOnly, final boolean deployed) {
        super(PATH_DATASOURCE,
                DataSourcesExtension.getResourceDescriptionResolver(DATA_SOURCE),
                deployed ? null : DataSourceAdd.INSTANCE,
                deployed ? null : DataSourceRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.deployed = deployed;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(DataSourcesExtension.SUBSYSTEM_NAME, DATA_SOURCE);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    public static DataSourceDefinition createInstance(final boolean registerRuntimeOnly, final boolean deployed) {
        return new DataSourceDefinition(registerRuntimeOnly, deployed);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (!deployed) {
            resourceRegistration.registerOperationHandler(DATASOURCE_ENABLE, DataSourceEnableDisable.ENABLE);

            resourceRegistration.registerOperationHandler(DATASOURCE_DISABLE, DataSourceEnableDisable.DISABLE);
        }
        if (registerRuntimeOnly) {
            resourceRegistration.registerOperationHandler(FLUSH_IDLE_CONNECTION, PoolOperations.FlushIdleConnectionInPool.DS_INSTANCE);
            resourceRegistration.registerOperationHandler(FLUSH_ALL_CONNECTION, PoolOperations.FlushAllConnectionInPool.DS_INSTANCE);
            resourceRegistration.registerOperationHandler(DUMP_QUEUED_THREADS, PoolOperations.DumpQueuedThreadInPool.DS_INSTANCE);
            resourceRegistration.registerOperationHandler(FLUSH_INVALID_CONNECTION, PoolOperations.FlushInvalidConnectionInPool.DS_INSTANCE);
            resourceRegistration.registerOperationHandler(FLUSH_GRACEFULLY_CONNECTION, PoolOperations.FlushGracefullyConnectionInPool.DS_INSTANCE);
            resourceRegistration.registerOperationHandler(TEST_CONNECTION, PoolOperations.TestConnectionInPool.DS_INSTANCE);
        }
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        if (!deployed)
            resourceRegistration.registerCapability(Capabilities.DATA_SOURCE_CAPABILITY);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            for (final SimpleAttributeDefinition attribute : DATASOURCE_ATTRIBUTE) {
                SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(attribute).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
                resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLDataSourceRuntimeHandler.INSTANCE);
            }
            for (final PropertiesAttributeDefinition attribute : DATASOURCE_PROPERTIES_ATTRIBUTES) {
                PropertiesAttributeDefinition runtimeAttribute = new PropertiesAttributeDefinition.Builder(attribute).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
                resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLDataSourceRuntimeHandler.INSTANCE);
            }

        } else {
            ReloadRequiredWriteAttributeHandler reloadRequiredWriteAttributeHandler = new ReloadRequiredWriteAttributeHandler(DATASOURCE_ATTRIBUTE);
            for (final SimpleAttributeDefinition attribute : DATASOURCE_ATTRIBUTE) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
                } else  if (attribute.getName().equals(ENLISTMENT_TRACE.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, new EnlistmentTraceAttributeWriteHandler());
                } else {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, reloadRequiredWriteAttributeHandler);
                }
            }
            ReloadRequiredWriteAttributeHandler reloadRequiredPropertiesWriteHandler = new ReloadRequiredWriteAttributeHandler(DATASOURCE_PROPERTIES_ATTRIBUTES);
            for (final PropertiesAttributeDefinition attribute : DATASOURCE_PROPERTIES_ATTRIBUTES) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
                } else {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, reloadRequiredPropertiesWriteHandler);
                }
            }
        }


    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            resourceRegistration.registerSubModel(ConnectionPropertyDefinition.DEPLOYED_INSTANCE);
        } else {
            resourceRegistration.registerSubModel(ConnectionPropertyDefinition.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    private static RejectAttributeChecker createConnURLRejectChecker() {
        return new RejectAttributeChecker.DefaultRejectAttributeChecker() {

            @Override
            public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                return RejectAttributeChecker.UNDEFINED.getRejectionLogMessage(attributes);
            }

            @Override
            public boolean rejectOperationParameter(PathAddress address, String attributeName,
                    ModelNode attributeValue, ModelNode operation, TransformationContext context) {
                return operation.get(ModelDescriptionConstants.OP).asString().equals(ModelDescriptionConstants.ADD)
                        && !attributeValue.isDefined();
            }

            @Override
            protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                    TransformationContext context) {
                return !attributeValue.isDefined();
            }
        };
    }
}
