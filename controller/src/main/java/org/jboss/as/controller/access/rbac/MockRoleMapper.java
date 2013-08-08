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

package org.jboss.as.controller.access.rbac;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link RoleMapper} for use in tests. Reads the set of roles from a request headers in the operation,
 * allowing the test to completely control the mapping. Roles are stored as a ModelNode of type
 * ModelType.LIST, elements of ModelType.STRING, under operation.get("operation-headers", "roles").
 * If no such header is found, the user is SUPERUSER. IF the list is empty, the user has no permissions.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 *
 * @deprecated Only for early prototyping use
 */
@Deprecated
public class MockRoleMapper implements RoleMapper {

    public static final MockRoleMapper INSTANCE = new MockRoleMapper();

    private final Set<String> SUPERUSER = Collections.singleton(StandardRole.SUPERUSER.toString());

    private MockRoleMapper() {
        // singleton
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetAttribute attribute) {
        return mapRoles(action.getOperation());
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetResource resource) {
        return mapRoles(action.getOperation());
    }

    private Set<String> mapRoles(ModelNode operation) {
        Set<String> result = SUPERUSER;
        ModelNode headers = operation.get(ModelDescriptionConstants.OPERATION_HEADERS);
        if (headers.isDefined() && headers.hasDefined("roles")) {
            ModelNode rolesNode = headers.get("roles");
            Set<String> roleSet = new HashSet<String>();
            if (rolesNode.getType() == ModelType.STRING) {
                roleSet.add(rolesNode.asString().toUpperCase());
            } else {
                for (ModelNode role : headers.get("roles").asList()) {
                    roleSet.add(role.asString().toUpperCase());
                }
            }
            result = Collections.unmodifiableSet(roleSet);
        }

        return result;
    }
}
