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

package org.jboss.as.messaging.jms;

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
class NewJMSSubsystemDescribeHandler implements ModelQueryOperationHandler {

    static final NewJMSSubsystemDescribeHandler INSTANCE = new NewJMSSubsystemDescribeHandler();

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode result = new ModelNode();
        final ModelNode subModel = context.getSubModel();

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());
        result.add(subsystemAdd);

        if(subModel.hasDefined(CommonAttributes.CONNECTION_FACTORY)) {
            for(final Property property : subModel.get(CommonAttributes.CONNECTION_FACTORY).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CONNECTION_FACTORY, property.getName());
                result.add(NewConnectionFactoryAdd.getAddOperation(address, subModel));
            }
        }
        if(subModel.hasDefined(CommonAttributes.QUEUE)) {
            for(final Property property : subModel.get(CommonAttributes.QUEUE).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.QUEUE, property.getName());
                result.add(NewJMSQueueAdd.getOperation(address, property.getValue()));
            }
        }
        if(subModel.hasDefined(CommonAttributes.TOPIC)) {
            for(final Property property : subModel.get(CommonAttributes.TOPIC).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.TOPIC, property.getName());
                result.add(NewJMSTopicAdd.getOperation(address, property.getValue()));
            }
        }

        resultHandler.handleResultFragment(Util.NO_LOCATION, result);
        resultHandler.handleResultComplete(new ModelNode());

        return Cancellable.NULL;
    }

}
