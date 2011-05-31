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

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.NewStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Emanuel Muckenhuber
 */
class JMSSubsystemDescribeHandler implements NewStepHandler {

    static final JMSSubsystemDescribeHandler INSTANCE = new JMSSubsystemDescribeHandler();

    /** {@inheritDoc} */
    public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
        final PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        final ModelNode result = context.getResult();
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);

        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());
        result.add(subsystemAdd);

        if(subModel.hasDefined(CommonAttributes.CONNECTION_FACTORY)) {
            for(final Property property : subModel.get(CommonAttributes.CONNECTION_FACTORY).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CONNECTION_FACTORY, property.getName());
                result.add(ConnectionFactoryAdd.getAddOperation(address, property.getValue()));
            }
        }
        if(subModel.hasDefined(CommonAttributes.QUEUE)) {
            for(final Property property : subModel.get(CommonAttributes.QUEUE).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.QUEUE, property.getName());
                result.add(JMSQueueAdd.getOperation(address, property.getValue()));
            }
        }
        if(subModel.hasDefined(CommonAttributes.TOPIC)) {
            for(final Property property : subModel.get(CommonAttributes.TOPIC).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.TOPIC, property.getName());
                result.add(JMSTopicAdd.getOperation(address, property.getValue()));
            }
        }

        context.completeStep();
    }
}
