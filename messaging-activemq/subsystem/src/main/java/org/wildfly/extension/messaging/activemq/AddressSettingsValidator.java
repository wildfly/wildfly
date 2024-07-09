/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

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
            // Jakarta Messaging Queue
            childType = CommonAttributes.JMS_QUEUE;
            addressPrefix = JMS_QUEUE_ADDRESS_PREFIX;
        } else if (address.startsWith(JMS_TOPIC_ADDRESS_PREFIX)) {
            // Jakarta Messaging Topic
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
