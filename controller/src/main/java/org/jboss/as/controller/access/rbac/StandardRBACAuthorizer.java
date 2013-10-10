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

import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.controller.access.AuthorizerConfiguration;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.permission.ManagementPermissionAuthorizer;

/**
 * Standard {@link org.jboss.as.controller.access.Authorizer} implementation that uses a provided
 * {@link RoleMapper} to construct a {@link DefaultPermissionFactory}, with that permission factory
 * used for the permissions used by the {@link ManagementPermissionAuthorizer superclass implementation}.
 * <p>Also supports the allowed roles being specified via a {@code roles} operation-header in the top level operation
 * whose value is the name of a role or a DMR list of strings each of which is the name of a role.</p>
 * <p>This operation-header based approach is only secure to the extent the clients using it are secure. To use this
 * approach the client must authenticate, and the underlying.
 * So, by adding the {@code roles} operation-header to the request the client can only reduce its privileges,
 * not increase them.
 * </p>
 *
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public final class StandardRBACAuthorizer extends ManagementPermissionAuthorizer {

    private static final Set<String> STANDARD_ROLES;
    static {

        Set<String> stdRoles = new LinkedHashSet<String>();
        for (StandardRole stdRole : StandardRole.values()) {
            stdRoles.add(stdRole.getFormalName());
        }
        STANDARD_ROLES = stdRoles;
    }

    public static final AuthorizerDescription AUTHORIZER_DESCRIPTION = new AuthorizerDescription() {
        @Override
        public boolean isRoleBased() {
            return true;
        }

        @Override
        public Set<String> getStandardRoles() {
            return STANDARD_ROLES;
        }
    };

    public static StandardRBACAuthorizer create(AuthorizerConfiguration configuration, final RoleMapper roleMapper) {
        final RunAsRoleMapper runAsRoleMapper = new RunAsRoleMapper(roleMapper);
        final DefaultPermissionFactory permissionFactory = new DefaultPermissionFactory(
                runAsRoleMapper, configuration);
        return new StandardRBACAuthorizer(configuration, permissionFactory, runAsRoleMapper);
    }

    private final AuthorizerConfiguration configuration;
    private final DefaultPermissionFactory permissionFactory;
    private final RoleMapper roleMapper;

    private StandardRBACAuthorizer(final AuthorizerConfiguration configuration,
                                   final DefaultPermissionFactory permissionFactory, final RoleMapper roleMapper) {
        super(permissionFactory, permissionFactory);
        this.configuration = configuration;
        this.permissionFactory = permissionFactory;
        configuration.registerScopedRoleListener(permissionFactory);
        this.roleMapper = roleMapper;
    }

    @Override
    public Set<String> getCallerRoles(Caller caller, Environment callEnvironment, Set<String> runAsroles) {
        return roleMapper.mapRoles(caller, callEnvironment, runAsroles);
    }

    @Override
    public AuthorizerDescription getDescription() {
        return AUTHORIZER_DESCRIPTION;
    }

    public void shutdown() {
        configuration.unregisterScopedRoleListener(permissionFactory);
    }
}
