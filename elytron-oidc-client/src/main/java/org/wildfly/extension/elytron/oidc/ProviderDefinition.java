/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.elytron.oidc;

import static org.wildfly.extension.elytron.oidc.ElytronOidcSubsystemSchema.VERSION_4_0_PREVIEW;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.DISABLE_TRUST_MANAGER;
import static org.wildfly.extension.elytron.oidc.ProviderAttributeDefinitions.PROVIDER_JWT_CLAIMS_TYP;
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
 * A {@link ResourceDefinition} for an OpenID Connect provider definition.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
class ProviderDefinition extends SimpleResourceDefinition {

    static final ResourceRegistration PATH = ResourceRegistration.of(PathElement.pathElement(ElytronOidcDescriptionConstants.PROVIDER), Stability.DEFAULT);
    ProviderDefinition() {
        super(new Parameters(PATH,
                ElytronOidcExtension.getResourceDescriptionResolver(ElytronOidcDescriptionConstants.PROVIDER))
                .setAddHandler(ProviderAddHandler.INSTANCE)
                .setRemoveHandler(ProviderRemoveHandler.INSTANCE)
                .setAddRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES)
                .setRemoveRestartLevel(OperationEntry.Flag.RESTART_RESOURCE_SERVICES));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attribute : ProviderAttributeDefinitions.ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attribute, null, ProviderWriteAttributeHandler.INSTANCE);
        }
        resourceRegistration.registerReadWriteAttribute(PROVIDER_JWT_CLAIMS_TYP, null, ProviderWriteAttributeHandler.INSTANCE);
    }

    static class ProviderAddHandler extends AbstractAddStepHandler {
        public static ProviderAddHandler INSTANCE = new ProviderAddHandler();

        private ProviderAddHandler() {
            super((VERSION_4_0_PREVIEW.getVersion().major() < (ElytronOidcClientSubsystemModel.CURRENT.getVersion().getMajor()))
                ? ProviderAttributeDefinitions.ATTRIBUTES_VERSION_4_0 : ProviderAttributeDefinitions.ATTRIBUTES);
        }

        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRuntime(context, operation, model);

            boolean disableTrustManager = DISABLE_TRUST_MANAGER.resolveModelAttribute(context, model).asBoolean();
            if (disableTrustManager) {
                ROOT_LOGGER.disableTrustManagerSetToTrue();
            }

            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.addProvider(context.getCurrentAddressValue(), context.resolveExpressions(model));
        }
    }

    static class ProviderWriteAttributeHandler extends AbstractWriteAttributeHandler<OidcConfigService> {
        public static final ProviderWriteAttributeHandler INSTANCE = new ProviderWriteAttributeHandler();

        private ProviderWriteAttributeHandler() {
            super((VERSION_4_0_PREVIEW.getVersion().major() < (ElytronOidcClientSubsystemModel.CURRENT.getVersion().getMajor()))
                    ? ProviderAttributeDefinitions.ATTRIBUTES_VERSION_4_0 : ProviderAttributeDefinitions.ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<OidcConfigService> handbackHolder) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.updateProvider(context.getCurrentAddressValue(), attributeName, resolvedValue);
            handbackHolder.setHandback(oidcConfigService);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, OidcConfigService oidcConfigService) throws OperationFailedException {
            oidcConfigService.updateProvider(context.getCurrentAddressValue(), attributeName, valueToRestore);
        }
    }

    static class ProviderRemoveHandler extends AbstractRemoveStepHandler {
        public static ProviderRemoveHandler INSTANCE = new ProviderRemoveHandler();

        ProviderRemoveHandler() {
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            OidcConfigService oidcConfigService = OidcConfigService.getInstance();
            oidcConfigService.removeProvider(context.getCurrentAddressValue());
        }
    }
}
