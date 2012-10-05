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

import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.ManagementUtil.rollbackOperationWithNoHandler;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.hornetq.api.core.management.AddressControl;
import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 */
public class SecurityRoleReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final SecurityRoleReadAttributeHandler INSTANCE = new SecurityRoleReadAttributeHandler();

    private SecurityRoleReadAttributeHandler() {
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        PathAddress pathAddress = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        String addressName = pathAddress.getElement(pathAddress.size() - 2).getValue();
        String roleName = pathAddress.getLastElement().getValue();

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        AddressControl control = AddressControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.CORE_ADDRESS + addressName));

        if (control == null) {
            rollbackOperationWithNoHandler(context, operation);
            return;
        }

        try {
            String rolesAsJSON = control.getRolesAsJSON();
            ModelNode res = ModelNode.fromJSONString(rolesAsJSON);
            ModelNode roles = ManagementUtil.convertSecurityRole(res);
            ModelNode matchedRole = findRole(roleName, roles);
            if (matchedRole == null || !matchedRole.hasDefined(attributeName)) {
                throw MESSAGES.unsupportedAttribute(attributeName);
            }
            boolean value = matchedRole.get(attributeName).asBoolean();
            context.getResult().set(value);
        } catch (Exception e) {
            context.getFailureDescription().set(e.getLocalizedMessage());
        } finally {
            context.stepCompleted();
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
