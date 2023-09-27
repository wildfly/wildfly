/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.CREDENTIAL_REFERENCE;
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
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.TEST_CONNECTION;

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
import org.jboss.as.controller.security.CredentialReferenceWriteAttributeHandler;

/**
 * @author Stefano Maestri
 */
public class DataSourceDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DATASOURCE = PathElement.pathElement(DATA_SOURCE);

    private final boolean registerRuntimeOnly;
    private final boolean deployed;

    private DataSourceDefinition(final boolean registerRuntimeOnly, final boolean deployed) {
        super(new Parameters(PATH_DATASOURCE, DataSourcesExtension.getResourceDescriptionResolver(DATA_SOURCE))
                .setAddHandler(deployed ? null : DataSourceAdd.INSTANCE)
                .setRemoveHandler(deployed ? null : DataSourceRemove.INSTANCE)
                .setAccessConstraints(
                        new ApplicationTypeAccessConstraintDefinition(
                                new ApplicationTypeConfig(DataSourcesExtension.SUBSYSTEM_NAME, DATA_SOURCE)))
                .setCapabilities(getCapabilities(deployed))
        );
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.deployed = deployed;
    }

    public static DataSourceDefinition createInstance(final boolean registerRuntimeOnly, final boolean deployed) {
        return new DataSourceDefinition(registerRuntimeOnly, deployed);
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
            CredentialReferenceWriteAttributeHandler credentialReferenceWriteAttributeHandler = new CredentialReferenceWriteAttributeHandler(CREDENTIAL_REFERENCE, RECOVERY_CREDENTIAL_REFERENCE);
            for (final SimpleAttributeDefinition attribute : DATASOURCE_ATTRIBUTE) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
                } else  if (attribute.getName().equals(ENLISTMENT_TRACE.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, new EnlistmentTraceAttributeWriteHandler());
                } else if (attribute.getName().equals(CREDENTIAL_REFERENCE.getName()) || attribute.getName().equals(RECOVERY_CREDENTIAL_REFERENCE.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, credentialReferenceWriteAttributeHandler);
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
        resourceRegistration.registerSubModel(new ConnectionPropertyDefinition(this.deployed));
    }
}
