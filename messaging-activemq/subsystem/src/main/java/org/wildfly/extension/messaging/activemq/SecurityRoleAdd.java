/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.getActiveMQServer;

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
    protected boolean requiresRuntime(OperationContext context) {
        return context.isDefaultRequiresRuntime() && !context.isBooting();
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                    throws OperationFailedException {
        if(context.isNormalServer()) {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
            final ActiveMQServer server = getActiveMQServer(context, operation);
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
        final ActiveMQServer server = getActiveMQServer(context, operation);
        final String match = address.getElement(address.size() - 2).getValue();
        final String roleName = address.getLastElement().getValue();
        SecurityRoleRemove.removeRole(server, match, roleName);
    }
}
