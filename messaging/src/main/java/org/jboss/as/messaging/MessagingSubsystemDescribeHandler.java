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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.messaging.jms.ConnectionFactoryAdd;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAdd;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
class MessagingSubsystemDescribeHandler implements OperationStepHandler {

    static final MessagingSubsystemDescribeHandler INSTANCE = new MessagingSubsystemDescribeHandler();

    /** {@inheritDoc} */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode subsystemAdd = new ModelNode();
        final ModelNode subModel = context.readModel(PathAddress.EMPTY_ADDRESS);
        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());
        //
        for(final String attribute : MessagingSubsystemProviders.MESSAGING_ROOT_ATTRIBUTES) {
            if(subModel.hasDefined(attribute)) {
                subsystemAdd.get(attribute).set(subModel.get(attribute));
            }
        }
        final ModelNode result = context.getResult();
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
        if(subModel.hasDefined(CommonAttributes.JMS_TOPIC)) {
            for(final Property property : subModel.get(CommonAttributes.JMS_TOPIC).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.JMS_TOPIC, property.getName());
                result.add(JMSTopicAdd.getOperation(address, property.getValue()));
            }
        }
        if(subModel.hasDefined(CommonAttributes.POOLED_CONNECTION_FACTORY)) {
            for(final Property property : subModel.get(CommonAttributes.POOLED_CONNECTION_FACTORY).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.POOLED_CONNECTION_FACTORY, property.getName());
                result.add(PooledConnectionFactoryAdd.getAddOperation(address, property.getValue()));
            }
        }

        context.completeStep();
    }

}
