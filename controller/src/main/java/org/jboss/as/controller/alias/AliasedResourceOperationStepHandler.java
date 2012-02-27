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

package org.jboss.as.controller.alias;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import sun.tools.tree.ReturnStatement;

/**
 * {@link OperationStepHandler} for a resource that is just an alias for another resource.  Simply translates the
 * operation address to the primary address and adds a Stage.IMMEDIATE step for the translated operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class AliasedResourceOperationStepHandler implements OperationStepHandler {

    private final PathAddress mainAddress;

    public AliasedResourceOperationStepHandler(PathAddress mainAddress) {
        this.mainAddress = mainAddress;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        String opName = operation.require(ModelDescriptionConstants.OP).asString();
        PathAddress unaliasedAddress = getUnaliasedAddress(operation);
        ModelNode unaliasedOp = operation.clone();
        unaliasedOp.get(ModelDescriptionConstants.OP_ADDR).set(unaliasedAddress.toModelNode());

        ImmutableManagementResourceRegistration resourceRegistration = context.getRootResourceRegistration();
        OperationStepHandler handler = resourceRegistration.getOperationHandler(unaliasedAddress, opName);

        context.addStep(unaliasedOp, handler, OperationContext.Stage.IMMEDIATE);

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    private PathAddress getUnaliasedAddress(final ModelNode operation) throws OperationFailedException{
        PathAddress orig = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        PathAddress base;
        if (orig.size() == mainAddress.size()) {
            base = PathAddress.EMPTY_ADDRESS;
        } else {
            base = orig.subAddress(0, orig.size() - mainAddress.size());
        }
        PathAddress alias = base.append(mainAddress);
        if (alias.equals(orig)){
            throw new OperationFailedException("Alias cannot be same as original");
        }
        return alias;
    }
}
