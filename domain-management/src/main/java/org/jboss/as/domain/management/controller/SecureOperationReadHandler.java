/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.controller;

import java.util.EnumSet;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Handler for reading the 'operation' and 'address' fields of an active operation that
 * ensures that responses are elided if the caller does not have rights to address the
 * operation's target.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class SecureOperationReadHandler implements OperationStepHandler {

    private static final String HIDDEN = "<hidden>";
    private static final Set<Action.ActionEffect> ADDRESS_EFFECT = EnumSet.of(Action.ActionEffect.ADDRESS);

    static final OperationStepHandler INSTANCE = new SecureOperationReadHandler();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        AuthorizedAddress authorizedAddress = authorizeAddress(context, operation);

        String attribute = operation.require(ModelDescriptionConstants.NAME).asString();
        if (ActiveOperationResourceDefinition.OPERATION_NAME.getName().equals(attribute)) {
            if (authorizedAddress.elided) {
                context.getResult().set(HIDDEN);
            } else {
                context.getResult().set(model.get(attribute));
            }
        } else if (ActiveOperationResourceDefinition.ADDRESS.getName().equals(attribute)) {
            if (authorizedAddress.elided) {
                context.getResult().set(authorizedAddress.address);
            } else {
                context.getResult().set(model.get(attribute));
            }
        } else {
            // Programming error
            throw new IllegalStateException();
        }

        context.stepCompleted();
    }

    private AuthorizedAddress authorizeAddress(OperationContext context, ModelNode operation) {
        ModelNode address = operation.get(ModelDescriptionConstants.OP_ADDR);
        ModelNode testOp = new ModelNode();
        testOp.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        testOp.get(ModelDescriptionConstants.OP_ADDR).set(address);

        AuthorizationResult authResult = context.authorize(testOp, ADDRESS_EFFECT);
        if (authResult.getDecision() == AuthorizationResult.Decision.PERMIT) {
            return new AuthorizedAddress(address, false);
        }

        // Failed. Now we need to see how far we can go
        ModelNode partialAddress = new ModelNode().setEmptyList();
        ModelNode elidedAddress = new ModelNode().setEmptyList();
        for (Property prop : address.asPropertyList()) {
            partialAddress.add(prop);
            testOp.get(ModelDescriptionConstants.OP_ADDR).set(address);
            authResult = context.authorize(testOp, ADDRESS_EFFECT);
            if (authResult.getDecision() == AuthorizationResult.Decision.DENY) {
                elidedAddress.add(prop.getName(), HIDDEN);
                return new AuthorizedAddress(elidedAddress, false);
            } else {
                elidedAddress.add(prop);
            }
        }

        // Should not be reachable, but in case of a bug, be conservative and hide data
        ModelNode strange = new ModelNode();
        strange.add(HIDDEN, HIDDEN);
        return new AuthorizedAddress(strange, true);
    }

    private static class AuthorizedAddress {
        private final ModelNode address;
        private final boolean elided;

        private AuthorizedAddress(ModelNode address, boolean elided) {
            this.address = address;
            this.elided = elided;
        }
    }
}
