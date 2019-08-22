/*
 * Copyright 2017 Red Hat, Inc.
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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE;
import static org.jboss.as.connector.subsystems.common.pool.Constants.VALIDATE_ON_MATCH;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_CREDENTIAL_REFERENCE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.TRACKING;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_ELYTRON_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DEFAULT_PRINCIPAL;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUP;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_GROUPS;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_REQUIRED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.WM_SECURITY_MAPPING_USERS;
import static org.jboss.as.controller.security.CredentialReference.REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2017 Red Hat, inc.
 */
public class ResourceAdaptersTransformers implements ExtensionTransformerRegistration {
    private static final String LEGACY_MCP = "org.jboss.jca.core.connectionmanager.pool.mcp.SemaphoreArrayListManagedConnectionPool";

    private static final ModelVersion EAP_7_1 = ModelVersion.create(5, 0, 0);
    private static final ModelVersion EAP_7_0 = ModelVersion.create(4, 0, 0);
    private static final ModelVersion EAP_6_2 = ModelVersion.create(1, 3, 0);

    @Override
    public String getSubsystemName() {
        return ResourceAdaptersExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());
        ResourceTransformationDescriptionBuilder parentBuilder = chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), EAP_7_1);
        ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME))
                .getAttributeBuilder()
                .setValueConverter(AttributeConverter.Factory.createHardCoded(ModelNode.ZERO, true), INITIAL_POOL_SIZE)
                .end();
        builder.addChildResource(ConnectionDefinitionResourceDefinition.PATH)
                .getAttributeBuilder()
                .addRejectCheck(REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, RECOVERY_CREDENTIAL_REFERENCE)
                .end();

        parentBuilder = chainedBuilder.createBuilder(EAP_7_1, EAP_7_0);
        builder = parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, WM_ELYTRON_SECURITY_DOMAIN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, WM_ELYTRON_SECURITY_DOMAIN)
                .end();
        builder.addChildResource(ConnectionDefinitionResourceDefinition.PATH).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Constants.ELYTRON_ENABLED, Constants.RECOVERY_ELYTRON_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(ModelNode.FALSE), Constants.RECOVERY_CREDENTIAL_REFERENCE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, Constants.AUTHENTICATION_CONTEXT, Constants.AUTHENTICATION_CONTEXT_AND_APPLICATION, Constants.RECOVERY_AUTHENTICATION_CONTEXT)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.ELYTRON_ENABLED, Constants.RECOVERY_ELYTRON_ENABLED, Constants.RECOVERY_CREDENTIAL_REFERENCE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.AUTHENTICATION_CONTEXT, Constants.AUTHENTICATION_CONTEXT_AND_APPLICATION, Constants.RECOVERY_AUTHENTICATION_CONTEXT)
                .setValueConverter(new AttributeConverter.DefaultValueAttributeConverter(Constants.ENLISTMENT_TRACE), Constants.ENLISTMENT_TRACE)
                .end();

        parentBuilder = chainedBuilder.createBuilder(EAP_7_0, EAP_6_2);
        builder = parentBuilder.addChildResource(PathElement.pathElement(RESOURCEADAPTER_NAME))
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, WM_SECURITY_MAPPING_USER, WM_SECURITY_MAPPING_GROUP,
                        WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_USERS, WM_SECURITY_DEFAULT_GROUP,
                        WM_SECURITY_DEFAULT_GROUPS, WM_SECURITY_DEFAULT_PRINCIPAL)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, ModelNode.FALSE), WM_SECURITY, WM_SECURITY_MAPPING_REQUIRED, STATISTICS_ENABLED)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(false, true, new ModelNode("other")), WM_SECURITY_DOMAIN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MODULE, WM_SECURITY, WM_SECURITY_MAPPING_USER, WM_SECURITY_MAPPING_GROUP,
                        WM_SECURITY_MAPPING_GROUPS, WM_SECURITY_MAPPING_USERS, WM_SECURITY_DEFAULT_GROUP,
                        WM_SECURITY_DEFAULT_GROUPS, WM_SECURITY_DEFAULT_PRINCIPAL, WM_SECURITY_MAPPING_REQUIRED,
                        WM_SECURITY_DOMAIN, STATISTICS_ENABLED)
                .end();
        builder.addChildResource(ConnectionDefinitionResourceDefinition.PATH).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, Constants.ENLISTMENT, Constants.SHARABLE, org.jboss.as.connector.subsystems.common.pool.Constants.INITIAL_POOL_SIZE,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_CLASS, org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_CLASS,
                        org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_INCREMENTER_PROPERTIES, org.jboss.as.connector.subsystems.common.pool.Constants.CAPACITY_DECREMENTER_PROPERTIES)
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Constants.CONNECTABLE, org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR, Constants.ENLISTMENT_TRACE)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, TRACKING, VALIDATE_ON_MATCH)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(LEGACY_MCP)), Constants.MCP)
                .addRejectCheck(RejectAttributeChecker.DEFINED, org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FAIR)
                .addRejectCheck(new RejectAttributeChecker.SimpleRejectAttributeChecker(ModelNode.FALSE), Constants.ENLISTMENT_TRACE)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.MCP)
                .addRejectCheck(RejectAttributeChecker.DEFINED, Constants.CONNECTABLE, Constants.TRACKING, VALIDATE_ON_MATCH);



        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{
                EAP_6_2,
                EAP_7_0,
                EAP_7_1,
        });

    }
}
