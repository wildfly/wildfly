/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BINDING_NAMES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NUMBER_OF_BYTES_PER_PAGE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NUMBER_OF_PAGES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.QUEUE_NAMES;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ROLES_ATTR_NAME;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.ignoreOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.rollbackOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.ManagementUtil.reportListOfStrings;
import static org.wildfly.extension.messaging.activemq.ManagementUtil.reportRoles;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Handles operations and attribute reads supported by a ActiveMQ {@link org.apache.activemq.api.core.management.AddressControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class AddressControlHandler extends AbstractRuntimeOnlyHandler {

    static final AddressControlHandler INSTANCE = new AddressControlHandler();

    private AddressControlHandler() {
    }

    @Override
    protected boolean resourceMustExist(OperationContext context, ModelNode operation) {
        return false;
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final String operationName = operation.require(OP).asString();
        if (READ_ATTRIBUTE_OPERATION.equals(operationName)) {
            handleReadAttribute(context, operation);
        }
    }

    private void handleReadAttribute(OperationContext context, ModelNode operation) {

        if (ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        final AddressControl addressControl = getAddressControl(context, operation);
        if (addressControl == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        final String name = operation.require(ModelDescriptionConstants.NAME).asString();

        try {
            if (ROLES_ATTR_NAME.equals(name)) {
                reportRoles(context, addressControl.getRoles());
            } else if (QUEUE_NAMES.equals(name)) {
                String[] queues = addressControl.getQueueNames();
                reportListOfStrings(context, queues);
            } else if (NUMBER_OF_BYTES_PER_PAGE.equals(name)) {
                long l = addressControl.getNumberOfBytesPerPage();
                context.getResult().set(l);
            } else if (NUMBER_OF_PAGES.equals(name)) {
                context.getResult().set(addressControl.getNumberOfPages());
            } else if (BINDING_NAMES.equals(name)) {
                String[] bindings = addressControl.getBindingNames();
                reportListOfStrings(context, bindings);
            } else {
                // Bug
                throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(name);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
    }

    private AddressControl getAddressControl(final OperationContext context, final ModelNode operation) {
        final String addressName = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
        return AddressControl.class.cast(server.getResource(ResourceNames.ADDRESS + addressName));
    }
}
