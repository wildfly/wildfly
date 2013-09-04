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
import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.rbac.RunAsRoleMapper;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} representing an individual role mapping.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class IsCallerInRoleOperation implements OperationStepHandler {

    public static final SimpleOperationDefinition DEFINITION  = new SimpleOperationDefinitionBuilder(IS_CALLER_IN_ROLE, DomainManagementResolver.getResolver("core", "management", "access-control"))
            .setReplyType(ModelType.BOOLEAN)
            .setReadOnly()
            .build();

    private final Authorizer authorizer;

    private IsCallerInRoleOperation(final Authorizer authorizer) {
        this.authorizer = authorizer;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        String roleName = RoleMappingResourceDefinition.getRoleName(operation);

        if (context.getCurrentStage() == Stage.MODEL) {
            context.addStep(this, Stage.RUNTIME);
        } else {
            ModelNode result = context.getResult();
            Set<String> operationHeaderRoles = RunAsRoleMapper.getOperationHeaderRoles(operation);
            result.set(isCallerInRole(roleName, context.getCaller(), context.getCallEnvironment(), operationHeaderRoles));
        }

        context.stepCompleted();
    }

    private boolean isCallerInRole(String roleName, Caller caller, Environment callEnvironment, Set<String> operationHeaderRoles) {
        Set<String> mappedRoles = authorizer.getCallerRoles(caller, callEnvironment, operationHeaderRoles);
        if (mappedRoles == null) {
            return false;
        } else if (mappedRoles.contains(roleName)) {
            return true;
        } else {
            for (String role : mappedRoles) {
                if (role.equalsIgnoreCase(roleName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static OperationStepHandler create(final Authorizer authorizer) {
        return new IsCallerInRoleOperation(authorizer);
    }

}
