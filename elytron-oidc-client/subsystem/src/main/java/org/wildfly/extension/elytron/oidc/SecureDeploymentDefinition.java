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

import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link ResourceDefinition} for securing deployments via OpenID Connect.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class SecureDeploymentDefinition extends SimpleResourceDefinition {

    protected static final SimpleAttributeDefinition REALM =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REALM, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition PROVIDER =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PROVIDER, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    protected static final SimpleAttributeDefinition RESOURCE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.RESOURCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(ElytronOidcDescriptionConstants.CLIENT_ID)
                    .build();

    protected static final SimpleAttributeDefinition CLIENT_ID =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.CLIENT_ID, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .setAlternatives(ElytronOidcDescriptionConstants.RESOURCE)
                    .build();

    protected static final SimpleAttributeDefinition USE_RESOURCE_ROLE_MAPPINGS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.USE_RESOURCE_ROLE_MAPPINGS, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition BEARER_ONLY =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.BEARER_ONLY, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition ENABLE_BASIC_AUTH =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ENABLE_BASIC_AUTH, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition PUBLIC_CLIENT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PUBLIC_CLIENT, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN, ModelType.BOOLEAN, true)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition TOKEN_MINIMUM_TIME_TO_LIVE =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.TOKEN_MINIMUM_TIME_TO_LIVE, ModelType.INT, true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition MIN_TIME_BETWEEN_JWKS_REQUESTS =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.MIN_TIME_BETWEEN_JWKS_REQUESTS, ModelType.INT, true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition PUBLIC_KEY_CACHE_TTL =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.PUBLIC_KEY_CACHE_TTL, ModelType.INT, true)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(-1, true))
                    .build();

    protected static final SimpleAttributeDefinition ADAPTER_STATE_COOKIE_PATH =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.ADAPTER_STATE_COOKIE_PATH, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, true))
                    .build();

    static final List<SimpleAttributeDefinition> ALL_ATTRIBUTES = new ArrayList();
    static {
        ALL_ATTRIBUTES.add(REALM);
        ALL_ATTRIBUTES.add(PROVIDER);
        ALL_ATTRIBUTES.add(RESOURCE);
        ALL_ATTRIBUTES.add(CLIENT_ID);
        ALL_ATTRIBUTES.add(USE_RESOURCE_ROLE_MAPPINGS);
        ALL_ATTRIBUTES.add(BEARER_ONLY);
        ALL_ATTRIBUTES.add(ENABLE_BASIC_AUTH);
        ALL_ATTRIBUTES.add(PUBLIC_CLIENT);
        ALL_ATTRIBUTES.add(TURN_OFF_CHANGE_SESSION_ID_ON_LOGIN);
        ALL_ATTRIBUTES.add(TOKEN_MINIMUM_TIME_TO_LIVE);
        ALL_ATTRIBUTES.add(MIN_TIME_BETWEEN_JWKS_REQUESTS);
        ALL_ATTRIBUTES.add(PUBLIC_KEY_CACHE_TTL);
        ALL_ATTRIBUTES.add(ADAPTER_STATE_COOKIE_PATH);
        ALL_ATTRIBUTES.add(CredentialDefinition.CREDENTIAL);
        ALL_ATTRIBUTES.add(RedirectRewriteRuleDefinition.REDIRECT_REWRITE_RULE);
        for (SimpleAttributeDefinition attribute : ProviderAttributeDefinitions.ATTRIBUTES) {
            ALL_ATTRIBUTES.add(attribute);
        }
    }

    SecureDeploymentDefinition() {
        super(new Parameters(PathElement.pathElement(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT),
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcDescriptionConstants.SECURE_DEPLOYMENT))
                .setAddHandler(SecureDeploymentAddHandler.INSTANCE)
                .setRemoveHandler(SecureDeploymentRemoveHandler.INSTANCE)
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
        for (AttributeDefinition attribute : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, SecureDeploymentWriteAttributeHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new CredentialDefinition());
        resourceRegistration.registerSubModel(new RedirectRewriteRuleDefinition());
    }

    static class SecureDeploymentAddHandler extends AbstractAddStepHandler {
        public static SecureDeploymentAddHandler INSTANCE = new SecureDeploymentAddHandler();

        private SecureDeploymentAddHandler() {
            super(ALL_ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            String clientId = CLIENT_ID.resolveModelAttribute(context, model).asStringOrNull();
            String resource = RESOURCE.resolveModelAttribute(context, model).asStringOrNull();
            if (clientId == null && resource == null) {
                throw ROOT_LOGGER.resourceOrClientIdMustBeConfigured();
            }
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addSecureDeployment(operation, context.resolveExpressions(model));
        }
    }

    static class SecureDeploymentWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final SecureDeploymentWriteAttributeHandler INSTANCE = new SecureDeploymentWriteAttributeHandler();

        SecureDeploymentWriteAttributeHandler() {
            super(ALL_ATTRIBUTES.toArray(new SimpleAttributeDefinition[ALL_ATTRIBUTES.size()]));
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateSecureDeployment(operation, attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateSecureDeployment(operation, attributeName, valueToRestore);
        }
    }

    static class SecureDeploymentRemoveHandler extends AbstractRemoveStepHandler {
        public static SecureDeploymentRemoveHandler INSTANCE = new SecureDeploymentRemoveHandler();

        SecureDeploymentRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeSecureDeployment(operation);
        }
    }
}