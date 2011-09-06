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

import org.hornetq.core.security.Role;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Emanuel Muckenhuber
 */
class SecurityRoleAttributeHandler extends ServerWriteAttributeOperationHandler {

    static final SecurityRoleAttributeHandler INSTANCE = new SecurityRoleAttributeHandler();

    public void registerAttributes(final ManagementResourceRegistration registry) {
        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_NONE);
        for (AttributeDefinition attr : SecurityRoleAdd.ROLE_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }

    @Override
    protected void validateValue(String name, ModelNode value) throws OperationFailedException {
        final AttributeDefinition def = getAttributeDefinition(name);
        def.getValidator().validateParameter(name, value);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final HornetQServer server = getServer(context);
                if(server != null) {
                    final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                    final String match = address.getElement(address.size() - 2).getValue();
                    final String roleName = address.getLastElement().getValue();
                    final Set<Role> newRoles = new HashSet<Role>();
                    final Set<Role> roles = server.getSecurityRepository().getMatch(match);
                    for(final Role role : roles) {
                        if(! roleName.equals(role.getName())) {
                             newRoles.add(role);
                        }
                    }
                    final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                    final ModelNode subModel = resource.getModel();
                    final Role updatedRole = SecurityRoleAdd.transform(roleName, subModel);
                    newRoles.add(updatedRole);
                    server.getSecurityRepository().addMatch(match, newRoles);
                }
                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
        return false;
    }

    static HornetQServer getServer(final OperationContext context) {
        final ServiceController<?> controller = context.getServiceRegistry(true).getService(MessagingServices.JBOSS_MESSAGING);
        if(controller != null) {
            return HornetQServer.class.cast(controller.getValue());
        }
        return null;
    }

    static final AttributeDefinition getAttributeDefinition(final String attributeName) {
        for(final AttributeDefinition def : SecurityRoleAdd.ROLE_ATTRIBUTES) {
            if(def.getName().equals(attributeName)) {
                return def;
            }
        }
        return null;
    }
}
