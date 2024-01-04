/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
