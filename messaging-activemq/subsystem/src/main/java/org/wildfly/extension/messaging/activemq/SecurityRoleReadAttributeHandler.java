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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

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
        ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
        AddressControl control = AddressControl.class.cast(server.getManagementService().getResource(ResourceNames.ADDRESS + addressName));

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        try {
            String rolesAsJSON = control.getRolesAsJSON();
            ModelNode res = ModelNode.fromJSONString(rolesAsJSON);
            ModelNode roles = ManagementUtil.convertSecurityRole(res);
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
