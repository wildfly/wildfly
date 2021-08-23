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
 * A {@link ResourceDefinition} for an OpenID Connect rewrite rule definition.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class RedirectRewriteRuleDefinition extends SimpleResourceDefinition {

    protected static final SimpleAttributeDefinition REPLACEMENT =
            new SimpleAttributeDefinitionBuilder(ElytronOidcDescriptionConstants.REPLACEMENT, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
                    .build();

    protected static final ObjectTypeAttributeDefinition REDIRECT_REWRITE_RULE = new ObjectTypeAttributeDefinition.Builder(ElytronOidcDescriptionConstants.REDIRECT_REWRITE_RULE,
            REPLACEMENT).build();

    RedirectRewriteRuleDefinition() {
        super(new Parameters(PathElement.pathElement(ElytronOidcDescriptionConstants.REDIRECT_REWRITE_RULE),
                ElytronOidcExtension.getResourceDescriptionResolver(SECURE_DEPLOYMENT, ElytronOidcDescriptionConstants.REDIRECT_REWRITE_RULE))
                .setAddHandler(RedirectRewriteRuleAddHandler.INSTANCE)
                .setRemoveHandler(RedirectRewriteRuleRemoveHandler.INSTANCE)
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
        resourceRegistration.registerReadWriteAttribute(REPLACEMENT, null, RedirectRewriteRuleWriteHandler.INSTANCE);
    }

    static class RedirectRewriteRuleAddHandler extends AbstractAddStepHandler {

        public static RedirectRewriteRuleAddHandler INSTANCE = new RedirectRewriteRuleAddHandler();

        private RedirectRewriteRuleAddHandler() {
            super(REPLACEMENT);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addRedirectRewriteRule(operation, context.resolveExpressions(model));
        }
    }

    static class RedirectRewriteRuleWriteHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final RedirectRewriteRuleWriteHandler INSTANCE = new RedirectRewriteRuleWriteHandler();

        private RedirectRewriteRuleWriteHandler() {
            super(REPLACEMENT);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateRedirectRewriteRule(operation, attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateRedirectRewriteRule(operation, attributeName, valueToRestore);
        }
    }

    static class RedirectRewriteRuleRemoveHandler extends AbstractRemoveStepHandler {
        public static RedirectRewriteRuleRemoveHandler INSTANCE = new RedirectRewriteRuleRemoveHandler();

        RedirectRewriteRuleRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeRedirectRewriteRule(operation);
        }
    }

}
