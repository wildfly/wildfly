/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER;
import static org.wildfly.extension.elytron.oidc._private.ElytronOidcLogger.ROOT_LOGGER;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ResourceDefinition} for a Keycloak realm definition.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class RealmDefinition extends SimpleResourceDefinition {

    static final ResourceRegistration PATH = ResourceRegistration.of(PathElement.pathElement(ElytronOidcDescriptionConstants.REALM), Stability.DEFAULT);
    RealmDefinition() {
        super(new Parameters(PATH,
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcDescriptionConstants.REALM))
                .setAddHandler(RealmAddHandler.INSTANCE)
                .setRemoveHandler(RealmRemoveHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
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

            boolean disableTrustManager = DISABLE_TRUST_MANAGER.resolveModelAttribute(context, model).asBoolean();
            if (disableTrustManager) {
                ROOT_LOGGER.disableTrustManagerSetToTrue();
            }

            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addRealm(context.getCurrentAddressValue(), context.resolveExpressions(model));
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
            oidcConfigService.updateRealm(context.getCurrentAddressValue(), attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateRealm(context.getCurrentAddressValue(), attributeName, valueToRestore);
        }
    }

    static class RealmRemoveHandler extends AbstractRemoveStepHandler {
        public static ProviderDefinition.ProviderRemoveHandler INSTANCE = new ProviderDefinition.ProviderRemoveHandler();

        RealmRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeRealm(context.getCurrentAddressValue());
        }
    }
}
