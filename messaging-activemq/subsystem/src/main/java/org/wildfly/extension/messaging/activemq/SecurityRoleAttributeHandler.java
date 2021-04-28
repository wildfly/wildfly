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

import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.getActiveMQServer;

import java.util.HashSet;
import java.util.Set;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class SecurityRoleAttributeHandler extends AbstractWriteAttributeHandler<Set<Role>> {

    static final SecurityRoleAttributeHandler INSTANCE = new SecurityRoleAttributeHandler();

    private SecurityRoleAttributeHandler() {
        super(SecurityRoleDefinition.ATTRIBUTES);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode newValue, ModelNode currentValue,
                                           HandbackHolder<Set<Role>> handbackHolder) throws OperationFailedException {

        final ActiveMQServer server = getActiveMQServer(context, operation);
        if(server != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final String match = address.getElement(address.size() - 2).getValue();
            final String roleName = address.getLastElement().getValue();
            final Set<Role> newRoles = new HashSet<Role>();
            final Set<Role> roles = server.getSecurityRepository().getMatch(match);
            handbackHolder.setHandback(roles);
            for(final Role role : roles) {
                if(! roleName.equals(role.getName())) {
                     newRoles.add(role);
                }
            }
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode subModel = resource.getModel();
            final Role updatedRole = SecurityRoleDefinition.transform(context, roleName, subModel);
            newRoles.add(updatedRole);
            server.getSecurityRepository().addMatch(match, newRoles);
        }
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Set<Role> handback) throws OperationFailedException {
        if (handback != null) {
            final ActiveMQServer server = getActiveMQServer(context, operation);
            if(server != null) {
                final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                final String match = address.getElement(address.size() - 2).getValue();
                server.getSecurityRepository().addMatch(match, handback);
            }
        }
    }
}
