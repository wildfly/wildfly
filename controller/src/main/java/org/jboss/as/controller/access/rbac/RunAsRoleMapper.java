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

import static org.jboss.as.controller.ControllerLogger.ACCESS_LOGGER;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
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
 * A {@link RoleMapper} that allows clients to specify the roles they desire to run as. By default this {@link RoleMapper} Reads
 * the set of roles from a request headers in the operation, allowing the test to completely control the mapping. Roles are
 * stored as a ModelNode of type ModelType.LIST, elements of ModelType.STRING, under operation.get("operation-headers",
 * "roles"). If no such header is found, the user is SUPERUSER. IF the list is empty, the user has no permissions.
 *
 * This {@link RoleMapper} can be extended to allow the ability to run as different roles to be checked.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RunAsRoleMapper implements RoleMapper {

    private final RoleMapper realRoleMapper;
    private volatile Set<String> knownRoles;

    public RunAsRoleMapper(RoleMapper realRoleMapper) {
        this.realRoleMapper = realRoleMapper;

        Set<String> knownRoles = new HashSet<String>();
        for (StandardRole current : StandardRole.values()) {
            knownRoles.add(current.toString());
        }
        this.knownRoles = knownRoles;
    }

    public void addKnownRole(String roleName) {
        Set<String> newKnownRoles = new HashSet<String>(knownRoles);
        newKnownRoles.add(roleName);
        this.knownRoles = newKnownRoles;
    }

    public void removeKnownRole(String roleName) {
        Set<String> newKnownRoles = new HashSet<String>(knownRoles);
        newKnownRoles.remove(roleName);
        this.knownRoles = newKnownRoles;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetAttribute attribute) {
        Set<String> mappedRoles = realRoleMapper.mapRoles(caller, callEnvironment, action, attribute);
        if (action != null) {
            mappedRoles = mapRoles(caller, mappedRoles, action.getOperation());
        }
        return mappedRoles;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetResource resource) {
        return mapRoles(caller, realRoleMapper.mapRoles(caller, callEnvironment, action, resource), action.getOperation());
    }

    private Set<String> mapRoles(Caller caller, Set<String> currentRoles, ModelNode operation) {
        Set<String> result = currentRoles;
        ModelNode headers = operation.get(ModelDescriptionConstants.OPERATION_HEADERS);
        if (headers.isDefined() && headers.hasDefined("roles")) {
            ModelNode rolesNode = headers.get("roles");
            Set<String> roleSet = new HashSet<String>();
            if (rolesNode.getType() == ModelType.STRING) {
                String requestedRole = getRoleFromText(rolesNode.asString());
                if (canRunAs(currentRoles, requestedRole)) {
                    roleSet.add(requestedRole);
                }
            } else {
                for (ModelNode role : headers.get("roles").asList()) {
                    String requestedRole = getRoleFromText(role.asString());
                    if (canRunAs(currentRoles, requestedRole)) {
                        roleSet.add(requestedRole);
                    }
                }
            }
            if (roleSet.isEmpty() == false) {
                result = Collections.unmodifiableSet(roleSet);
                if (ACCESS_LOGGER.isTraceEnabled()) {
                    StringBuilder sb = new StringBuilder("User '").append(caller.getName()).append(
                            "' Mapped to requested roles { ");
                    for (String current : result) {
                        sb.append("'").append(current).append("' ");
                    }
                    sb.append("}");
                    ACCESS_LOGGER.trace(sb.toString());
                }
            }
        }

        return result;
    }

    private boolean canRunAs(Set<String> currentRoles, String requestedRole) {
        return requestedRole != null && currentRoles.contains(StandardRole.SUPERUSER.toString())
                && knownRoles.contains(requestedRole);
    }

    private static String getRoleFromText(String text) {
        try {
            StandardRole standardRole = StandardRole.valueOf(text.toUpperCase(Locale.ENGLISH));
            return standardRole.toString();
        } catch (Exception e) {
            return text;
        }
    }
}
