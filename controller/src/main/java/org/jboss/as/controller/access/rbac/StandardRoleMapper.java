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

import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;

/**
 * A {@link RoleMapper} that supports configuration from the WildFly management API.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class StandardRoleMapper implements RoleMapper {

    private static final String IN_VM_ROLE = StandardRole.SUPERUSER.getOfficialForm();
    private static final RunAsRolePermission RUN_AS_IN_VM_ROLE = new RunAsRolePermission(IN_VM_ROLE);
    private final AuthorizerConfiguration authorizerConfiguration;

    public StandardRoleMapper(final AuthorizerConfiguration authorizerConfiguration) {
        this.authorizerConfiguration = authorizerConfiguration;
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetAttribute attribute) {
        return mapRoles(caller);
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Action action, TargetResource resource) {
        return mapRoles(caller);
    }

    @Override
    public Set<String> mapRoles(Caller caller, Environment callEnvironment, Set<String> operationHeaderRoles) {
        return mapRoles(caller);
    }

    @Override
    public boolean canRunAs(Set<String> mappedRoles, String runAsRole) {
        if (runAsRole == null) {
            return false;
        }

        boolean hasRole = authorizerConfiguration.hasRole(runAsRole);
        boolean isSuperUser = mappedRoles.contains(StandardRole.SUPERUSER.toString());

        /*
         * We only allow users to specify roles to run as if they are SuperUser, if the user is not SuperUser we need to be
         * careful to not provide a way for the user to test which roles actually exist.
         */

        if (isSuperUser && hasRole == false) {
            throw ControllerMessages.MESSAGES.unknownRole(runAsRole);
        }

        return hasRole && isSuperUser;
    }

    private Set<String> mapRoles(final Caller caller) {
        Set<String> mappedRoles = new HashSet<String>();

        boolean traceEnabled = ACCESS_LOGGER.isTraceEnabled();

        if (caller.hasSubject()) {
            Map<String, AuthorizerConfiguration.RoleMapping> rolesToCheck;
            if (authorizerConfiguration.isMapUsingRealmRoles()) {
                rolesToCheck = new HashMap<String, AuthorizerConfiguration.RoleMapping>(authorizerConfiguration.getRoleMappings());
                Set<String> realmRoles = caller.getAssociatedRoles();
                for (String current : realmRoles) {
                    String roleName = current.toUpperCase();
                    if (rolesToCheck.containsKey(roleName)) {
                        AuthorizerConfiguration.RoleMapping roleMapping = rolesToCheck.remove(roleName);
                        AuthorizerConfiguration.MappingPrincipal exclusion = roleMapping.isExcluded(caller);
                        if (exclusion == null) {
                            if (traceEnabled) {
                                ACCESS_LOGGER
                                        .tracef("User '%s' assigned role '%s' due to realm assignment and no exclusion in role mapping definition.",
                                                caller.getName(), roleName);
                            }
                            mappedRoles.add(roleName);
                        } else {
                            if (traceEnabled) {
                                ACCESS_LOGGER
                                        .tracef("User '%s' NOT assigned role '%s' despite realm assignment due to exclusion match against %s.",
                                                caller.getName(), roleName, exclusion);
                            }
                        }
                    } else {
                        if (traceEnabled) {
                            ACCESS_LOGGER
                                    .tracef("User '%s' assigned role '%s' due to realm assignment and no role mapping to check for exclusion.",
                                            caller.getName(), roleName);
                        }
                        mappedRoles.add(roleName);
                    }
                }
            } else {
                // A clone is not needed here as the whole set of values is to be iterated with no need for removal.
                rolesToCheck = authorizerConfiguration.getRoleMappings();
            }

            for (AuthorizerConfiguration.RoleMapping current : rolesToCheck.values()) {
                boolean includeAll = current.includeAllAuthedUsers();
                AuthorizerConfiguration.MappingPrincipal inclusion = includeAll == false ? current.isIncluded(caller) : null;
                if (includeAll || inclusion != null) {
                    AuthorizerConfiguration.MappingPrincipal exclusion = current.isExcluded(caller);
                    if (exclusion == null) {
                        if (traceEnabled) {
                            if (includeAll) {
                                ACCESS_LOGGER.tracef("User '%s' assiged role '%s' due to include-all set on role.", caller.getName(),
                                        current.getName());
                            } else {
                                ACCESS_LOGGER.tracef("User '%s' assiged role '%s' due to match on inclusion %s", caller.getName(),
                                        current.getName(), inclusion);
                            }
                        }
                        mappedRoles.add(current.getName());
                    } else {
                        if (traceEnabled) {
                            ACCESS_LOGGER.tracef("User '%s' denied membership of role '%s' due to exclusion %s",
                                    caller.getName(), current.getName(), exclusion);
                        }
                    }
                } else {
                    if (traceEnabled) {
                        ACCESS_LOGGER.tracef(
                                "User '%s' not assigned role '%s' as no match on the include definition of the role mapping.",
                                caller.getName(), current.getName());
                    }
                }
            }
        } else {
            /*
             * If the IN-VM code does not have the required permission a SecurityException will be thrown.
             *
             * At the moment clients should not be making speculation requests so in a correctly configured installation this
             * check should pass with no error.
             */
            checkPermission(RUN_AS_IN_VM_ROLE);
            ACCESS_LOGGER.tracef("Assigning role '%s' for call with no assigned Subject (An IN-VM Call).", IN_VM_ROLE);

            mappedRoles.add(IN_VM_ROLE);
        }

        if (traceEnabled) {
            StringBuilder sb = new StringBuilder("User '").append(caller.getName()).append("' Assigned Roles { ");
            for (String current : mappedRoles) {
                sb.append("'").append(current).append("' ");
            }
            sb.append("}");
            ACCESS_LOGGER.trace(sb.toString());
        }

        // TODO - We could consider something along the lines of a WeakHashMap to hold this result keyed on the Caller.
        // The contents of the Caller are not expected to change during a call and we could clear the cache on a config change.
        return Collections.unmodifiableSet(mappedRoles);
    }

    private static void checkPermission(final Permission permission) {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(permission);
        }
    }

}
