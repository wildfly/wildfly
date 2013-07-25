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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.access.rbac.ConfigurableRoleMapper;
import org.jboss.dmr.ModelNode;

/**
 * A {@link OperationStepHandler} for adding a role mapping.
 *
 * Initially this is just creating the resource in the model but will be updated later for additional functionality.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RoleMappingRemove implements OperationStepHandler {

    private final ConfigurableRoleMapper roleMapper;

    private RoleMappingRemove(final ConfigurableRoleMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public static OperationStepHandler create(final ConfigurableRoleMapper roleMapper) {
        return new RoleMappingRemove(roleMapper);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.removeResource(PathAddress.EMPTY_ADDRESS);

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String roleName = address.getLastElement().getValue().toUpperCase();

        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                roleMapper.removeRole(roleName);
                context.stepCompleted(); // TODO - Add roll back support.
            }
        }, Stage.RUNTIME);

        context.stepCompleted();
    }

}
