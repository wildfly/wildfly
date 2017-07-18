/*
 * Copyright 2017 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DISABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_ENABLE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENLISTMENT_TRACE;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_COMPLIANT;
import static org.jboss.as.connector.subsystems.datasources.Constants.MCP;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import static org.jboss.as.connector.subsystems.datasources.Constants.PROFILE;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.datasources.DataSourceDefinition.PATH_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesExtension.SUBSYSTEM_NAME;
import static org.jboss.as.connector.subsystems.datasources.JdbcDriverDefinition.PATH_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.XaDataSourceDefinition.PATH_XA_DATASOURCE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STATISTICS_ENABLED;

import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class DataSourcesTransformers implements ExtensionTransformerRegistration {

    // The ManagedConnectionPool implementation used by default by versions < 4.0.0 (WildFly 10)
    private static final String LEGACY_MCP = "org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool";

    private static final ModelVersion EAP_6_2 = ModelVersion.create(1, 2, 0);
    private static final ModelVersion EAP_6_3 = ModelVersion.create(1, 3, 0);
    private static final ModelVersion EAP_7_0 = ModelVersion.create(4, 0, 0);
    @Override
    public String getSubsystemName() {
        return SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        get400TransformationDescription(chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), EAP_7_0));
        get130TransformationDescription(chainedBuilder.createBuilder(EAP_7_0, EAP_6_3));
        get120TransformationDescription(chainedBuilder.createBuilder(EAP_6_3, EAP_6_2));

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                EAP_6_2,
                EAP_6_3,
                EAP_7_0,
        });

    }

    static TransformationDescription get130TransformationDescription(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
                    @Override
                    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return attributeValue.equals(new ModelNode(false));
                    }
                }, TRACKING)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TRACKING)
                .end();

        builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
                    @Override
                    protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                        return attributeValue.equals(new ModelNode(false));
                    }
                }, TRACKING)
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ENABLED)
                .addRejectCheck(RejectAttributeChecker.DEFINED, TRACKING)
                .end();
        return parentBuilder.build();
    }

    static TransformationDescription get120TransformationDescription(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(true)), org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ENLISTMENT_TRACE.getDefaultValue()), ENLISTMENT_TRACE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(LEGACY_MCP)), MCP)
                .addRejectCheck(RejectAttributeChecker.DEFINED, org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(new ModelNode(false)), ENLISTMENT_TRACE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, MCP)
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
                .end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();
        builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), CONNECTABLE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, false, new ModelNode(true)), STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ENLISTMENT_TRACE.getDefaultValue()), ENLISTMENT_TRACE)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(LEGACY_MCP)), MCP)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ENLISTMENT_TRACE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, MCP)
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
                .end()
                //We're rejecting operations when statistics-enabled=false, so let it through in the enable/disable ops which do not use that attribute
                .addOperationTransformationOverride(DATASOURCE_ENABLE.getName())
                .end()
                .addOperationTransformationOverride(DATASOURCE_DISABLE.getName())
                .end();
        return parentBuilder.build();
    }

    private static TransformationDescription get400TransformationDescription(ResourceTransformationDescriptionBuilder parentBuilder) {
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ELYTRON_ENABLED.getDefaultValue()), ELYTRON_ENABLED)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AUTHENTICATION_CONTEXT, CREDENTIAL_REFERENCE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, ELYTRON_ENABLED, AUTHENTICATION_CONTEXT, CREDENTIAL_REFERENCE)
                .addRejectCheck(createConnURLRejectChecker(), CONNECTION_URL)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(ENLISTMENT_TRACE), ENLISTMENT_TRACE)
                .end();
        builder = parentBuilder.addChildResource(PATH_XA_DATASOURCE);
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ELYTRON_ENABLED.getDefaultValue()), ELYTRON_ENABLED, RECOVERY_ELYTRON_ENABLED)
                .setDiscard(DiscardAttributeChecker.UNDEFINED,
                        RECOVERY_CREDENTIAL_REFERENCE,
                        RECOVERY_AUTHENTICATION_CONTEXT,
                        CREDENTIAL_REFERENCE,
                        AUTHENTICATION_CONTEXT
                        )
                .addRejectCheck(RejectAttributeChecker.DEFINED,
                        RECOVERY_CREDENTIAL_REFERENCE,
                        RECOVERY_ELYTRON_ENABLED,
                        ELYTRON_ENABLED,
                        RECOVERY_AUTHENTICATION_CONTEXT,
                        CREDENTIAL_REFERENCE,
                        AUTHENTICATION_CONTEXT)

                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(ENLISTMENT_TRACE), ENLISTMENT_TRACE)
                .end();
        parentBuilder.addChildResource(PATH_DRIVER).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, MODULE_SLOT, JDBC_COMPLIANT, PROFILE,
                        DRIVER_MODULE_NAME, DRIVER_XA_DATASOURCE_CLASS_NAME, DRIVER_CLASS_NAME)
                .end();
        return parentBuilder.build();
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
