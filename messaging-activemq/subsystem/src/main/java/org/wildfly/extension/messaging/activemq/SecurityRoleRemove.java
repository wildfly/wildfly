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
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * {@code OperationStepHandler} for removing a security role.
 *
 * @author Emanuel Muckenhuber
 */
class SecurityRoleRemove extends AbstractRemoveStepHandler {

    static final SecurityRoleRemove INSTANCE = new SecurityRoleRemove();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final ActiveMQServer server = getActiveMQServer(context, operation);
        final String match = address.getElement(address.size() - 2).getValue();
        final String roleName = address.getLastElement().getValue();
        removeRole(server, match, roleName);
    }

    static void removeRole(ActiveMQServer server, String match, String roleName) {
        if (server != null) {
            final Set<Role> roles = server.getSecurityRepository().getMatch(match);
            final Set<Role> newRoles = new HashSet<>();
            for (final Role role : roles) {
                if (!roleName.equals(role.getName())) {
                    newRoles.add(role);
                }
            }
            server.getSecurityRepository().addMatch(match, newRoles);
        }
    }
}
