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
 * A {@link ResourceDefinition} for a Keycloak realm definition.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class RealmDefinition extends SimpleResourceDefinition {

    RealmDefinition() {
        super(new Parameters(PathElement.pathElement(ElytronOidcDescriptionConstants.REALM),
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcDescriptionConstants.REALM))
                .setAddHandler(RealmAddHandler.INSTANCE)
                .setRemoveHandler(RealmRemoveHandler.INSTANCE)
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
            resourceRegistration.registerReadWriteAttribute(attribute, null, RealmWriteAttributeHandler.INSTANCE);
        }
    }

    static class RealmAddHandler extends AbstractAddStepHandler {
        public static RealmAddHandler INSTANCE = new RealmAddHandler();

        private RealmAddHandler() {
            super(ProviderAttributeDefinitions.ATTRIBUTES);
        }

        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addRealm(operation, context.resolveExpressions(model));
        }
    }

    static class RealmWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final RealmWriteAttributeHandler INSTANCE = new RealmWriteAttributeHandler();

        public RealmWriteAttributeHandler(AttributeDefinition... definitions) {
            super(ProviderAttributeDefinitions.ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateRealm(operation, attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateRealm(operation, attributeName, valueToRestore);
        }
    }

    static class RealmRemoveHandler extends AbstractRemoveStepHandler {
        public static ProviderDefinition.ProviderRemoveHandler INSTANCE = new ProviderDefinition.ProviderRemoveHandler();

        RealmRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeRealm(operation);
        }
    }
}
