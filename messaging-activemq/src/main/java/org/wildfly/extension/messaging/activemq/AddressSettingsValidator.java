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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.PathAddress.pathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.EXPIRY_ADDRESS;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Validate that address-settings' attributes are properly configured.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
class AddressSettingsValidator {

    private static final String JMS_QUEUE_ADDRESS_PREFIX = "jms.queue.";
    private static final String JMS_TOPIC_ADDRESS_PREFIX = "jms.topic.";
    /**
     * Validates that an address-setting:add operation does not define expiry-address or dead-letter address
     * without corresponding resources for them.
     */
    static OperationStepHandler ADD_VALIDATOR = new OperationStepHandler() {
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            String addressSetting = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

            PathAddress address = pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            Resource activeMQServer = context.readResourceFromRoot(MessagingServices.getActiveMQServerPathAddress(address), false);

            checkExpiryAddress(context, operation, activeMQServer, addressSetting);
            checkDeadLetterAddress(context, operation, activeMQServer, addressSetting);
        }
    };

    /**
     * Validate that an updated address-settings still has resources bound corresponding
     * to expiry-address and dead-letter-address (if they are defined).
     */
    static void validateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        String addressSetting = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        PathAddress address = pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));

        Resource activeMQServer = context.readResourceFromRoot(MessagingServices.getActiveMQServerPathAddress(address), true);

        checkExpiryAddress(context, resource.getModel(), activeMQServer, addressSetting);
        checkDeadLetterAddress(context, resource.getModel(), activeMQServer, addressSetting);
    }

    private static void checkExpiryAddress(OperationContext context, ModelNode model, Resource activeMQServer, String addressSetting) throws OperationFailedException {
        ModelNode expiryAddress = EXPIRY_ADDRESS.resolveModelAttribute(context, model);
        if (!findMatchingResource(expiryAddress, activeMQServer)) {
            MessagingLogger.ROOT_LOGGER.noMatchingExpiryAddress(expiryAddress.asString(), addressSetting);
        }
    }

    private static void checkDeadLetterAddress(OperationContext context, ModelNode model, Resource activeMQServer, String addressSetting) throws OperationFailedException {
        ModelNode deadLetterAddress = DEAD_LETTER_ADDRESS.resolveModelAttribute(context, model);
        if (!findMatchingResource(deadLetterAddress, activeMQServer)) {
            MessagingLogger.ROOT_LOGGER.noMatchingDeadLetterAddress(deadLetterAddress.asString(), addressSetting);
        }
    }

    private static boolean findMatchingResource(ModelNode addressNode, Resource activeMQServer) {
        if (!addressNode.isDefined()) {
            // do not search for a match if the address is not defined
            return true;
        }

        final String address = addressNode.asString();
        final String addressPrefix;
        final String childType;

        if (address.startsWith(JMS_QUEUE_ADDRESS_PREFIX)) {
            // JMS Queue
            childType = CommonAttributes.JMS_QUEUE;
            addressPrefix = JMS_QUEUE_ADDRESS_PREFIX;
        } else if (address.startsWith(JMS_TOPIC_ADDRESS_PREFIX)) {
            // JMS Topic
            childType = CommonAttributes.JMS_TOPIC;
            addressPrefix = JMS_TOPIC_ADDRESS_PREFIX;
        } else {
            // Core Queue
            childType = CommonAttributes.CORE_QUEUE;
            // no special address prefix for core queues
            addressPrefix = "";
        }

        for (String childName : activeMQServer.getChildrenNames(childType)) {
            if (address.equals(addressPrefix + childName)) {
                return true;
            }
        }
        return false;
    }
}
