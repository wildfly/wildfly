/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DISABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ENABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DUMP_QUEUED_THREADS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_ALL_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_GRACEFULLY_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_IDLE_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_INVALID_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.TEST_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_PROPERTIES_ATTRIBUTES;

import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.connector.subsystems.common.pool.PoolConfigurationRWHandler;
import org.jboss.as.connector.subsystems.common.pool.PoolOperations;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Stefano Maestri
 */
public class XaDataSourceDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_XA_DATASOURCE = PathElement.pathElement(XA_DATASOURCE);

    // The ManagedConnectionPool implementation used by default by versions < 4.0.0 (WildFly 10)
    private static final String LEGACY_MCP = "org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool";

    private final boolean registerRuntimeOnly;
    private final boolean deployed;

    private XaDataSourceDefinition(final boolean registerRuntimeOnly, final boolean deployed) {
        super(new Parameters(PATH_XA_DATASOURCE, DataSourcesExtension.getResourceDescriptionResolver(XA_DATASOURCE))
                .setAddHandler(deployed ? null : XaDataSourceAdd.INSTANCE)
                .setRemoveHandler(deployed ? null : DataSourceRemove.XA_INSTANCE)
                .setAccessConstraints(new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(DataSourcesExtension.SUBSYSTEM_NAME, XA_DATASOURCE)))
                .setCapabilities(getCapabilities(deployed))
        );
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.deployed = deployed;
    }

    public static XaDataSourceDefinition createInstance(final boolean registerRuntimeOnly, final boolean deployed) {
        return new XaDataSourceDefinition(registerRuntimeOnly, deployed);
    }

    @SuppressWarnings("rawtypes")
    private static RuntimeCapability[] getCapabilities(boolean deployed) {
        return deployed ? new RuntimeCapability[0] : new RuntimeCapability[] {Capabilities.DATA_SOURCE_CAPABILITY};
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
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            for (final SimpleAttributeDefinition attribute : XA_DATASOURCE_ATTRIBUTE) {
                SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(attribute).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
                resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLXaDataSourceRuntimeHandler.INSTANCE);
            }
            for (final PropertiesAttributeDefinition attribute : XA_DATASOURCE_PROPERTIES_ATTRIBUTES) {
                PropertiesAttributeDefinition runtimeAttribute = new PropertiesAttributeDefinition.Builder(attribute).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
                resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLXaDataSourceRuntimeHandler.INSTANCE);
            }

        } else {
            ReloadRequiredWriteAttributeHandler reloadRequiredWriteHandler = new ReloadRequiredWriteAttributeHandler(XA_DATASOURCE_ATTRIBUTE);
            for (final SimpleAttributeDefinition attribute : XA_DATASOURCE_ATTRIBUTE) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
                } else  if (attribute.getName().equals(ENLISTMENT_TRACE.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, new EnlistmentTraceAttributeWriteHandler());
                } else {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, reloadRequiredWriteHandler);
                }
            }
            ReloadRequiredWriteAttributeHandler reloadRequiredPropertiesWriteHandler = new ReloadRequiredWriteAttributeHandler(XA_DATASOURCE_PROPERTIES_ATTRIBUTES);
            for (final PropertiesAttributeDefinition attribute : XA_DATASOURCE_PROPERTIES_ATTRIBUTES) {
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
        resourceRegistration.registerSubModel(new XaDataSourcePropertyDefinition(this.deployed));
    }
}
