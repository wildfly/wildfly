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
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_ALL_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.FLUSH_IDLE_CONNECTION;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.TEST_CONNECTION;

import java.util.List;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorMessages;
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
 *
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

            resourceRegistration.registerOperationHandler(DATASOURCE_DISABLE, DataSourceDisable.LOCAL_INSTANCE);
        }
        if (registerRuntimeOnly) {
            resourceRegistration.registerOperationHandler(FLUSH_IDLE_CONNECTION, PoolOperations.FlushIdleConnectionInPool.DS_INSTANCE);
            resourceRegistration.registerOperationHandler(FLUSH_ALL_CONNECTION, PoolOperations.FlushAllConnectionInPool.DS_INSTANCE);
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
            DisableRequiredWriteAttributeHandler disableRequiredWriteHandler = new DisableRequiredWriteAttributeHandler(DATASOURCE_ATTRIBUTE);
            for (final SimpleAttributeDefinition attribute : DATASOURCE_ATTRIBUTE) {
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
            DisableRequiredWriteAttributeHandler disableRequiredPropertiesWriteHandler = new DisableRequiredWriteAttributeHandler(DATASOURCE_PROPERTIES_ATTRIBUTES);
            for (final PropertiesAttributeDefinition attribute : DATASOURCE_PROPERTIES_ATTRIBUTES) {
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
            resourceRegistration.registerSubModel(ConnectionPropertyDefinition.DEPLOYED_INSTANCE);
        } else {
            resourceRegistration.registerSubModel(ConnectionPropertyDefinition.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    static void registerTransformers110(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE)
                .getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return ConnectorMessages.MESSAGES.rejectAttributesMustBeTrue(attributes.keySet());
                    }

                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                      TransformationContext context) {
                        //This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                        return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                    }
                }, STATISTICS_ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        CONNECTABLE
                )
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, DATASOURCE_PROPERTIES_ATTRIBUTES)
                .end();
    }

    static void registerTransformers111(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
                builder.getAttributeBuilder()

                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                        .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                            @Override
                            public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                                return ConnectorMessages.MESSAGES.rejectAttributesMustBeTrue(attributes.keySet());
                            }

                            @Override
                            protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                              TransformationContext context) {
                                //This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                                return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                            }
                        }, STATISTICS_ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, CONNECTABLE)
                //Reject expressions for enabled, since if they are used we don't know their value for the operation transformer override
                //Although 'enabled' appears in the legacy model and the 'add' handler, the add does not actually set its value in the model
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.ENABLED)
                .end();
    }

    static void registerTransformers112(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
                builder.getAttributeBuilder()

                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                        .addRejectCheck(RejectAttributeChecker.DEFINED, CONNECTABLE)
                        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                        .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                            @Override
                            public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                                return ConnectorMessages.MESSAGES.rejectAttributesMustBeTrue(attributes.keySet());
                            }

                            @Override
                            protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                              TransformationContext context) {
                                //This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                                return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                            }
                        }, STATISTICS_ENABLED)
                                //Reject expressions for enabled, since if they are used we don't know their value for the operation transformer override
                //Although 'enabled' appears in the legacy model and the 'add' handler, the add does not actually set its value in the model
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.ENABLED)
                .end();
    }


    static void registerTransformers120(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .addRejectCheck(new RejectAttributeChecker.DefaultRejectAttributeChecker() {

                    @Override
                    public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
                        return ConnectorMessages.MESSAGES.rejectAttributesMustBeTrue(attributes.keySet());
                    }

                    @Override
                    protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue,
                                                      TransformationContext context) {
                        //This will not get called if it was discarded, so reject if it is undefined (default==false) or if defined and != 'true'
                        return !attributeValue.isDefined() || !attributeValue.asString().equals("true");
                    }
                }, STATISTICS_ENABLED).end();

    }


}
