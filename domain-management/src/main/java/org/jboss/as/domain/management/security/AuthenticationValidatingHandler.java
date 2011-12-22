/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.DomainManagementMessages;
import org.jboss.dmr.ModelNode;

/**
 * {@link OperationContext.Stage#MODEL} handler that validates a security realm resource has at least on authentication
 * resource but only has one non-trustore authentication resource. This is meant to run after normal {@code MODEL}
 * stage handlers.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class AuthenticationValidatingHandler implements OperationStepHandler {

    static final AuthenticationValidatingHandler INSTANCE = new AuthenticationValidatingHandler();

    /**
     * Creates an operations that targets this handler.
     * @param operationToValidate the operation that this handler will validate
     * @return  the validation operation
     */
    static ModelNode createOperation(final ModelNode operationToValidate) {
        PathAddress pa = PathAddress.pathAddress(operationToValidate.require(ModelDescriptionConstants.OP_ADDR));
        PathAddress realmPA = null;
        for (int i = pa.size() - 1; i > 0; i++) {
            PathElement pe = pa.getElement(i);
            if (ModelDescriptionConstants.SECURITY_REALM.equals(pe.getKey())) {
                realmPA = pa.subAddress(0, i + 1);
                break;
            }
        }
        assert realmPA != null : "operationToValidate did not have an address that included a " + ModelDescriptionConstants.SECURITY_REALM;
        return Util.getEmptyOperation("validate-authentication", realmPA.toModelNode());
    }

    private AuthenticationValidatingHandler() {
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String realmName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> children = resource.getChildrenNames(ModelDescriptionConstants.AUTHENTICATION);
        int max = children.contains(ModelDescriptionConstants.TRUSTSTORE) ? 2 : 1;
        if (children.size() > max) {
            Set<String> invalid = new HashSet<String>(children);
            invalid.remove(ModelDescriptionConstants.TRUSTSTORE);
            throw DomainManagementMessages.MESSAGES.multipleAuthenticationMechanismsDefined(realmName, invalid);
        } else if (children.size() == 0) {
            throw DomainManagementMessages.MESSAGES.noAuthenticationDefined(realmName);
        }
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
}
