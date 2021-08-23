/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link ResourceDefinition} for an OpenID Connect credential definition.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class CredentialDefinition extends SimpleResourceDefinition {

    protected static final SimpleAttributeDefinition SECRET =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.SECRET, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE_FILE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_FILE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE_TYPE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_TYPE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEYSTORE_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEYSTORE_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEY_PASSWORD =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEY_PASSWORD, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition TOKEN_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TOKEN_TIMEOUT, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_KEY_ALIAS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_KEY_ALIAS, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition ALGORITHM =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ALGORITHM, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final SimpleAttributeDefinition[] ATTRIBUTES = {SECRET, CLIENT_KEYSTORE_FILE, CLIENT_KEYSTORE_TYPE,
            CLIENT_KEYSTORE_PASSWORD, CLIENT_KEY_PASSWORD, TOKEN_TIMEOUT, CLIENT_KEY_ALIAS, ALGORITHM};

    protected static final ObjectTypeAttributeDefinition CREDENTIAL = new ObjectTypeAttributeDefinition.Builder(ElytronOidcDescriptionConstants.CREDENTIAL,
            ATTRIBUTES).build();

    CredentialDefinition() {
        super(new Parameters(PathElement.pathElement(ElytronOidcDescriptionConstants.CREDENTIAL),
                ElytronOidcExtension.getResourceDescriptionResolver(SECURE_DEPLOYMENT, ElytronOidcDescriptionConstants.CREDENTIAL))
                .setAddHandler(CredentialAddHandler.INSTANCE)
                .setRemoveHandler(CredentialRemoveHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));

    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attribute : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, CredentialWriteAttributeHandler.INSTANCE);
        }
    }

    static class CredentialAddHandler extends AbstractAddStepHandler {

        public static CredentialAddHandler INSTANCE = new CredentialAddHandler();

        private CredentialAddHandler() {
            super(ATTRIBUTES);
        }

        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addCredential(operation, context.resolveExpressions(model));
        }
    }

    static class CredentialWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final CredentialWriteAttributeHandler INSTANCE = new CredentialWriteAttributeHandler();

        private CredentialWriteAttributeHandler() {
            super(ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateCredential(operation, attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateCredential(operation, attributeName, valueToRestore);
        }
    }

    static class CredentialRemoveHandler extends AbstractRemoveStepHandler {
        public static CredentialRemoveHandler INSTANCE = new CredentialRemoveHandler();

        CredentialRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeCredential(operation);
        }
    }
}
