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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ResourceDefinition} for an OpenID Connect provider definition.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ProviderDefinition extends SimpleResourceDefinition {

    ProviderDefinition() {
        super(new Parameters(PathElement.pathElement(ElytronOidcDescriptionConstants.PROVIDER),
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcDescriptionConstants.PROVIDER))
                .setAddHandler(ProviderAddHandler.INSTANCE)
                .setRemoveHandler(ProviderRemoveHandler.INSTANCE)
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
        for (AttributeDefinition attribute : ProviderAttributeDefinitions.ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, ProviderWriteAttributeHandler.INSTANCE);
        }
    }

    static class ProviderAddHandler extends AbstractAddStepHandler {
        public static ProviderAddHandler INSTANCE = new ProviderAddHandler();

        private ProviderAddHandler() {
            super(ProviderAttributeDefinitions.ATTRIBUTES);
        }

        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addProvider(operation, context.resolveExpressions(model));
        }
    }

    static class ProviderWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final ProviderWriteAttributeHandler INSTANCE = new ProviderWriteAttributeHandler();

        private ProviderWriteAttributeHandler() {
            super(ProviderAttributeDefinitions.ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateProvider(operation, attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateProvider(operation, attributeName, valueToRestore);
        }
    }

    static class ProviderRemoveHandler extends AbstractRemoveStepHandler {
        public static ProviderRemoveHandler INSTANCE = new ProviderRemoveHandler();

        ProviderRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeProvider(operation);
        }
    }
}
