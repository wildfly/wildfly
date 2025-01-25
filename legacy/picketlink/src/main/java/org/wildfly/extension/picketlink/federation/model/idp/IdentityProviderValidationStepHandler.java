/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.picketlink.federation.model.idp;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.common.model.validator.ElementMaxOccurrenceValidationStepHandler;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.wildfly.extension.picketlink.logging.PicketLinkLogger.ROOT_LOGGER;

/**
 * @author Pedro Igor
 */
public class IdentityProviderValidationStepHandler extends ElementMaxOccurrenceValidationStepHandler {

    public IdentityProviderValidationStepHandler() {
        super(ModelElement.IDENTITY_PROVIDER, ModelElement.FEDERATION, 1);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        validateSecurityDomain(context);
        validateOccurrence(context, operation);
    }

    private void validateSecurityDomain(OperationContext context) throws OperationFailedException {
        ModelNode identityProviderNode = context.readResource(EMPTY_ADDRESS, false).getModel();
        boolean external = IdentityProviderResourceDefinition.EXTERNAL.resolveModelAttribute(context, identityProviderNode).asBoolean();
        ModelNode securityDomain = IdentityProviderResourceDefinition.SECURITY_DOMAIN.resolveModelAttribute(context, identityProviderNode);

        if (!external && !securityDomain.isDefined()) {
            throw ROOT_LOGGER.requiredAttribute(ModelElement.COMMON_SECURITY_DOMAIN.getName(), ModelElement.IDENTITY_PROVIDER
                .getName());
        }
    }
}
