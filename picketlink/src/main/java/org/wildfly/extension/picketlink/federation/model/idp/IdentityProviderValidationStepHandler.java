/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
