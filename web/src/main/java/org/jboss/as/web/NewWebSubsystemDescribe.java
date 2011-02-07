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

package org.jboss.as.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Emanuel Muckenhuber
 */
class NewWebSubsystemDescribe implements ModelQueryOperationHandler {

    static final NewWebSubsystemDescribe INSTANCE = new NewWebSubsystemDescribe();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        final ModelNode result = new ModelNode();
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode subModel = context.getSubModel();

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());
        if(subModel.hasDefined(CommonAttributes.CONTAINER_CONFIG)) {
            subsystemAdd.get(CommonAttributes.CONTAINER_CONFIG).set(subModel.get(CommonAttributes.CONTAINER_CONFIG));
        }
        result.add(subsystemAdd);
        if(subModel.hasDefined(CommonAttributes.CONNECTOR)) {
            for(final Property connector : subModel.get(CommonAttributes.CONNECTOR).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CONNECTOR, connector.getName());
                result.add(NewWebConnectorAdd.getRecreateOperation(address, connector.getValue()));
            }
        }
        if(subModel.hasDefined(CommonAttributes.VIRTUAL_SERVER)) {
            for(final Property host : subModel.get(CommonAttributes.VIRTUAL_SERVER).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.VIRTUAL_SERVER, host.getName());
                result.add(NewWebVirtualHostAdd.getAddOperation(address, host.getValue()));
            }
        }

        resultHandler.handleResultFragment(Util.NO_LOCATION, result);
        resultHandler.handleResultComplete(new ModelNode());

        return Cancellable.NULL;
    }

}
