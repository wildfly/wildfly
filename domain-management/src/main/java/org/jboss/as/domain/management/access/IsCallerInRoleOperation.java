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

import static org.jboss.as.domain.management.ModelDescriptionConstants.IS_CALLER_IN_ROLE;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.rbac.RoleMapper;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link ResourceDefinition} representing an individual role mapping.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class IsCallerInRoleOperation implements OperationStepHandler {

    public static final SimpleOperationDefinition DEFINITION  = new SimpleOperationDefinitionBuilder(IS_CALLER_IN_ROLE, DomainManagementResolver.getResolver("core", "management", "access-control"))
            .setReplyType(ModelType.BOOLEAN)
            .setReadOnly()
            .build();

    private final RoleMapper roleMapper;

    private IsCallerInRoleOperation(final RoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String roleName = RoleMappingResourceDefinition.getRoleName(operation);

        if (context.getCurrentStage() == Stage.MODEL) {
            context.addStep(this, Stage.RUNTIME);
        } else {
            ModelNode result = context.getResult();

            Set<String> roles = roleMapper.mapRoles(context.getCaller(), null, null, (TargetAttribute) null);

            result.set(roles.contains(roleName));
        }

        context.stepCompleted();
    }

    public static OperationStepHandler create(final RoleMapper roleMapper) {
        return new IsCallerInRoleOperation(roleMapper);
    }

}
