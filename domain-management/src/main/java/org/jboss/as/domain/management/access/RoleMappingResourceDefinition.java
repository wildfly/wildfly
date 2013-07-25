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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLE_MAPPING;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.rbac.ConfigurableRoleMapper;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ResourceDefinition} representing an individual role mapping.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RoleMappingResourceDefinition extends SimpleResourceDefinition {

    private final ConfigurableRoleMapper roleMapper;

    private RoleMappingResourceDefinition(final ConfigurableRoleMapper roleMapper) {
        super(PathElement.pathElement(ROLE_MAPPING), DomainManagementResolver.getResolver("core.access-control.role-mapping"),
                RoleMappingAdd.create(roleMapper), RoleMappingRemove.create(roleMapper));
        this.roleMapper = roleMapper;
    }

    public static SimpleResourceDefinition create(final ConfigurableRoleMapper roleMapper) {
        return new RoleMappingResourceDefinition(roleMapper);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(PrincipalResourceDefinition.includeResourceDefinition(roleMapper));
        resourceRegistration.registerSubModel(PrincipalResourceDefinition.excludeResourceDefinition(roleMapper));
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(IsCallerInRoleOperation.DEFINITION, IsCallerInRoleOperation.create(roleMapper));
    }

    static String getRoleName(final ModelNode operation) {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        for (PathElement current : address) {
            if (ROLE_MAPPING.equals(current.getKey())) {
                return current.getValue().toUpperCase();
            }
        }
        throw new IllegalStateException();
    }

}
