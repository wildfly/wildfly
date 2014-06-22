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
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_LISTENER_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DISABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ENABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_PROPERTIES_ATTRIBUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DUMP_QUEUED_THREADS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLE_ADD_TRANSFORMER;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLE_TRANSFORMER;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_ALL_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_GRACEFULLY_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_IDLE_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_INVALID_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.TEST_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_ATTRIBUTE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE_PROPERTIES_ATTRIBUTES;

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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
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
public class XaDataSourceDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_XA_DATASOURCE = PathElement.pathElement(XA_DATASOURCE);
    private final boolean registerRuntimeOnly;
    private final boolean deployed;

    private final List<AccessConstraintDefinition> accessConstraints;

    private XaDataSourceDefinition(final boolean registerRuntimeOnly, final boolean deployed) {
        super(PATH_XA_DATASOURCE,
                DataSourcesExtension.getResourceDescriptionResolver(XA_DATASOURCE),
                deployed ? null : XaDataSourceAdd.INSTANCE,
                deployed ? null : XaDataSourceRemove.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
        this.deployed = deployed;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(DataSourcesExtension.SUBSYSTEM_NAME, XA_DATASOURCE);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    public static XaDataSourceDefinition createInstance(final boolean registerRuntimeOnly, final boolean deployed) {
        return new XaDataSourceDefinition(registerRuntimeOnly, deployed);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        if (!deployed) {
            resourceRegistration.registerOperationHandler(DATASOURCE_ENABLE, DataSourceEnable.XA_INSTANCE);
            resourceRegistration.registerOperationHandler(DATASOURCE_DISABLE, DataSourceDisable.XA_INSTANCE);
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
            DisableRequiredWriteAttributeHandler disableRequiredWriteHandler = new DisableRequiredWriteAttributeHandler(XA_DATASOURCE_ATTRIBUTE);
            for (final SimpleAttributeDefinition attribute : XA_DATASOURCE_ATTRIBUTE) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
                } else {
                    if (attribute.equals(STATISTICS_ENABLED)) {
                        resourceRegistration.registerReadWriteAttribute(attribute, null, new ReloadRequiredWriteAttributeHandler());
                    } else {
                        resourceRegistration.registerReadWriteAttribute(attribute, null, disableRequiredWriteHandler);
                    }

                }
            }
            DisableRequiredWriteAttributeHandler disableRequiredPropertiesWriteHandler = new DisableRequiredWriteAttributeHandler(XA_DATASOURCE_PROPERTIES_ATTRIBUTES);
            for (final PropertiesAttributeDefinition attribute : XA_DATASOURCE_PROPERTIES_ATTRIBUTES) {
                if (PoolConfigurationRWHandler.ATTRIBUTES.contains(attribute.getName())) {
                    resourceRegistration.registerReadWriteAttribute(attribute, PoolConfigurationRWHandler.PoolConfigurationReadHandler.INSTANCE, PoolConfigurationRWHandler.LocalAndXaDataSourcePoolConfigurationWriteHandler.INSTANCE);
                } else {
                    resourceRegistration.registerReadWriteAttribute(attribute, null, disableRequiredPropertiesWriteHandler);
                }
            }
        }


    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            resourceRegistration.registerSubModel(XaDataSourcePropertyDefinition.DEPLOYED_INSTANCE);
        } else {
            resourceRegistration.registerSubModel(XaDataSourcePropertyDefinition.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    static void registerTransformers110(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE)
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES, CONNECTION_LISTENER_CLASS,
                        CONNECTION_LISTENER_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE
                )
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, TRACKING)
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
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES, CONNECTION_LISTENER_CLASS,
                        CONNECTION_LISTENER_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE,
                        Constants.URL_PROPERTY, CONNECTABLE, TRACKING
                )
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, DATASOURCE_PROPERTIES_ATTRIBUTES)
                 /*These are nillable in the old model, but appear as not nillable in CompareModelUtils due to problems in the resource description
                  (leave the line commented out so no one else gets confused)
                  .addRejectCheck(RejectAttributeChecker.UNDEFINED, Constants.EXCEPTION_SORTER_PROPERTIES, Constants.REAUTHPLUGIN_PROPERTIES, Constants.STALE_CONNECTION_CHECKER_PROPERTIES, Constants.VALID_CONNECTION_CHECKER_PROPERTIES)*/
                        //Reject expressions for enabled, since if they are used we don't know their value for the operation transformer override
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.ENABLED)
                .end()
                .addOperationTransformationOverride(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(ENABLE_TRANSFORMER)
                    .end()
                .addOperationTransformationOverride(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(ENABLE_TRANSFORMER)
                    .end()
                .addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(ENABLE_ADD_TRANSFORMER)
                    .end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();

        ConnectionPropertyDefinition.registerTransformers11x(builder);
    }

    static void registerTransformers111(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES, CONNECTION_LISTENER_CLASS,
                        CONNECTION_LISTENER_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS,
                        Constants.CONNECTION_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES,
                        org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE
                )
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, TRACKING)
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
                }, STATISTICS_ENABLED).addRejectCheck(RejectAttributeChecker.DEFINED,
                org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES, CONNECTION_LISTENER_CLASS,
                CONNECTION_LISTENER_PROPERTIES,
                org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS,
                org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS,
                Constants.CONNECTION_PROPERTIES,
                org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES,
                org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE,
                Constants.URL_PROPERTY, CONNECTABLE, TRACKING
        )
                //Reject expressions for enabled, since if they are used we don't know their value for the operation transformer override
                //Although 'enabled' appears in the legacy model and the 'add' handler, the add does not actually set its value in the model
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.ENABLED)
                .end()
                .addOperationTransformationOverride(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(ENABLE_TRANSFORMER)
                    .end()
                .addOperationTransformationOverride(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(ENABLE_TRANSFORMER)
                    .end()
                .addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                    .inheritResourceAttributeDefinitions()
                    .setCustomOperationTransformer(ENABLE_ADD_TRANSFORMER)
                    .end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();

        ConnectionPropertyDefinition.registerTransformers11x(builder);
    }

    static void registerTransformers200(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
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
                .setDiscard(DiscardAttributeChecker.UNDEFINED, TRACKING)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TRACKING).end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();
    }
}
