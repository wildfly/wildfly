/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;

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
 */
public class SecurityRoleReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final SecurityRoleReadAttributeHandler INSTANCE = new SecurityRoleReadAttributeHandler();

    private SecurityRoleReadAttributeHandler() {
    }

    @Override
    protected boolean resourceMustExist(OperationContext context, ModelNode operation) {
        return false;
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        PathAddress pathAddress = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        String addressName = pathAddress.getElement(pathAddress.size() - 2).getValue();
        String roleName = pathAddress.getLastElement().getValue();

        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
        AddressControl control = AddressControl.class.cast(server.getResource(ResourceNames.ADDRESS + addressName));

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        try {
            ModelNode roles = ManagementUtil.convertRoles(control.getRoles());
            ModelNode matchedRole = findRole(roleName, roles);
            if (matchedRole == null || !matchedRole.hasDefined(attributeName)) {
                throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(attributeName);
            }
            boolean value = matchedRole.get(attributeName).asBoolean();
            context.getResult().set(value);
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        }
    }

    private ModelNode findRole(String roleName, ModelNode roles) {
        for (ModelNode role : roles.asList()) {
            if (role.get(NAME).asString().equals(roleName)) {
                return role;
            }
        }
        return null;
    }
}
