/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.access;

import java.util.List;
import java.util.Map;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.constraint.ServerGroupEffectConstraint;
import org.jboss.as.controller.access.management.WritableAuthorizerConfiguration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Handles the {@code add} operation for a {@link ServerGroupScopedRoleResourceDefinition server group scoped role}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
class ServerGroupScopedRoleRemove implements OperationStepHandler {

    private final Map<String, ServerGroupEffectConstraint> constraintMap;
    private final WritableAuthorizerConfiguration authorizerConfiguration;

    ServerGroupScopedRoleRemove(Map<String, ServerGroupEffectConstraint> constraintMap,
                                WritableAuthorizerConfiguration authorizerConfiguration) {
        this.constraintMap = constraintMap;
        this.authorizerConfiguration = authorizerConfiguration;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

        context.removeResource(PathAddress.EMPTY_ADDRESS);

        final String roleName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        RoleMappingNotRequiredHandler.addOperation(context, roleName);

        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                final String baseRole = ServerGroupScopedRoleResourceDefinition.BASE_ROLE.resolveModelAttribute(context, model).asString();
                final List<ModelNode> serverGroupNodes = ServerGroupScopedRoleResourceDefinition.SERVER_GROUPS.resolveModelAttribute(context, model).asList();

                authorizerConfiguration.removeScopedRole(roleName);
                constraintMap.remove(roleName);
                context.completeStep(new OperationContext.RollbackHandler() {
                    @Override
                    public void handleRollback(OperationContext context, ModelNode operation) {
                        ServerGroupScopedRoleAdd.addScopedRole(roleName, baseRole, serverGroupNodes, authorizerConfiguration, constraintMap);
                    }
                });
            }
        }, OperationContext.Stage.RUNTIME);

        context.stepCompleted();
    }
}
