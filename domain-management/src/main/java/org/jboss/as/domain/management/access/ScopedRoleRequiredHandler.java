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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST_SCOPED_ROLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP_SCOPED_ROLE;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * An {@link OperationStepHandler} to be executed at the end of stage MODEL to confirm that a scoped role of the name specified
 * does exist.
 *
 * This is used in domain mode where a non-standard role mapping is added, we need to be sure the scoped role does actually
 * exist.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ScopedRoleRequiredHandler implements OperationStepHandler {

    private final String roleName;

    private ScopedRoleRequiredHandler(final String roleName) {
        this.roleName = roleName;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<String> hostScopedRoles = resource.getChildrenNames(HOST_SCOPED_ROLE);
        Set<String> serverGroupScopedRoles = resource.getChildrenNames(SERVER_GROUP_SCOPED_ROLE);

        if (hostScopedRoles.contains(roleName) == false && serverGroupScopedRoles.contains(roleName) == false) {
            throw MESSAGES.invalidRoleNameDomain(roleName);
        }

        context.stepCompleted();
    }

    static void addOperation(OperationContext context, String roleName) {
        ModelNode operation = Util.createEmptyOperation("scoped-role-check", PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuthorizationResourceDefinition.PATH_ELEMENT));

        context.addStep(operation, new ScopedRoleRequiredHandler(roleName), Stage.MODEL);
    }

}
