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

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DISABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ENABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_PROPERTIES_ATTRIBUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DUMP_QUEUED_THREADS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_ALL_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_GRACEFULLY_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_IDLE_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_INVALID_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.TEST_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;

import java.util.List;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Stefano Maestri
 */
public class DataSourceDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DATASOURCE = PathElement.pathElement(DATA_SOURCE);
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
            resourceRegistration.registerOperationHandler(DATASOURCE_ENABLE, DataSourceEnable.LOCAL_INSTANCE);

            resourceRegistration.registerOperationHandler(DATASOURCE_DISABLE, DataSourceDisable.INSTANCE);
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
            for (final SimpleAttributeDefinition attribute : DATASOURCE_ATTRIBUTE) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
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

    static void registerTransformers120(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return ConnectorLogger.ROOT_LOGGER.rejectAttributesMustBeTrue(attributes.keySet());
                    }

                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                      TransformationContext context) {
                        //This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                        return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                    }
                }, STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
                    @Override
                    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return attributeValue.equals(new ModelNode(false));
                    }
                }, TRACKING)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TRACKING,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_MODULE,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_MODULE,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_MODULE_SLOT)
                        .end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();
    }

    static void registerTransformers200(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return ConnectorLogger.ROOT_LOGGER.rejectAttributesMustBeTrue(attributes.keySet());
                    }

                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                      TransformationContext context) {
                        //This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                        return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                    }
                }, STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
                    @Override
                    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return attributeValue.equals(new ModelNode(false));
                    }
                }, TRACKING)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TRACKING,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_MODULE,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_MODULE,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTION_SORTER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.REAUTH_PLUGIN_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.VALID_CONNECTION_CHECKER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.STALE_CONNECTION_CHECKER_MODULE_SLOT,
                        org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_MODULE,
                        org.jboss.as.connector.subsystems.datasources.Constants.RECOVER_PLUGIN_MODULE_SLOT)
                        .end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();
    }

}
