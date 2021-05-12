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

import java.util.Set;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@code OperationStepHandler} for adding a new security role.
 *
 * @author Emanuel Muckenhuber
 */
class SecurityRoleAdd extends AbstractAddStepHandler {

    static final SecurityRoleAdd INSTANCE = new SecurityRoleAdd(SecurityRoleDefinition.ATTRIBUTES);

    private SecurityRoleAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                    throws OperationFailedException {
        if(context.isNormalServer()) {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final ActiveMQServer server = getServer(context, operation);
            final String match = address.getElement(address.size() - 2).getValue();
            final String roleName = address.getLastElement().getValue();

            if(server != null) {
                final Role role = SecurityRoleDefinition.transform(context, roleName, model);
                final Set<Role> roles = server.getSecurityRepository().getMatch(match);
                roles.add(role);
                server.getSecurityRepository().addMatch(match, roles);
            }
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, Resource resource) {
        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final ActiveMQServer server = getServer(context, operation);
        final String match = address.getElement(address.size() - 2).getValue();
        final String roleName = address.getLastElement().getValue();
        SecurityRoleRemove.removeRole(server, match, roleName);
    }

    static ActiveMQServer getServer(final OperationContext context, ModelNode operation) {
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
        final ServiceController<?> controller = context.getServiceRegistry(true).getService(serviceName);
        if(controller != null) {
            return ActiveMQServer.class.cast(controller.getValue());
        }
        return null;
    }
}
