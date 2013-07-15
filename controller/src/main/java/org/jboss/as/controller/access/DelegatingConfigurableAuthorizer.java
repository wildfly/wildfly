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

package org.jboss.as.controller.access;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.as.controller.access.constraint.ScopingConstraint;
import org.jboss.as.controller.access.permission.CombinationPolicy;
import org.jboss.as.controller.access.permission.ManagementPermissionAuthorizer;
import org.jboss.as.controller.access.rbac.DefaultPermissionFactory;
import org.jboss.as.controller.access.rbac.MockRoleMapper;
import org.jboss.as.controller.access.rbac.RoleMapper;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 * A {@link ConfigurableAuthorizer} that delegates to another. Used for initial boot to allow
 * an instance of this class to be provided to the {@code ModelController} but then have the
 * functional implementation swapped out when boot proceeds to the point where the user-configured
 * authorizer is available.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class DelegatingConfigurableAuthorizer implements ConfigurableAuthorizer {

    private volatile ConfigurableAuthorizer delegate = new DefaultDelegate();

    void setDelegate(ConfigurableAuthorizer delegate) {
        assert delegate != null : "null delegate";
        this.delegate = delegate;
    }

    @Override
    public boolean isRoleBased() {
        return delegate.isRoleBased();
    }

    @Override
    public Set<String> getStandardRoles() {
        return delegate.getStandardRoles();
    }

    @Override
    public Set<String> getAllRoles() {
        return delegate.getAllRoles();
    }

    @Override
    public void addScopedRole(String roleName, String baseRole, ScopingConstraint scopingConstraint) {
        delegate.addScopedRole(roleName, baseRole, scopingConstraint);
    }

    @Override
    public void removeScopedRole(String roleName) {
        delegate.removeScopedRole(roleName);
    }

    @Override
    public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
        return delegate.authorize(caller, callEnvironment, action, target);
    }

    @Override
    public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
        return delegate.authorize(caller, callEnvironment, action, target);
    }

    private static class DefaultDelegate implements ConfigurableAuthorizer {

        private final RoleMapper roleMapper = MockRoleMapper.INSTANCE;
        private final DefaultPermissionFactory permissionFactory = new DefaultPermissionFactory(CombinationPolicy.PERMISSIVE, roleMapper);
        private final Authorizer authorizer = new ManagementPermissionAuthorizer(permissionFactory);
        private final Set<String> addedRoles = new HashSet<String>();

        @Override
        public boolean isRoleBased() {
            return true;
        }

        @Override
        public Set<String> getStandardRoles() {
            Set<String> result = new LinkedHashSet<String>();
            for (StandardRole stdRole : StandardRole.values()) {
                result.add(stdRole.toString());
            }
            return result;
        }

        @Override
        public Set<String> getAllRoles() {
            Set<String> result = getStandardRoles();
            synchronized (addedRoles) {
                result.addAll(addedRoles);
            }
            return result;
        }

        @Override
        public void addScopedRole(String roleName, String baseRole, ScopingConstraint scopingConstraint) {
            synchronized (addedRoles) {
                permissionFactory.addScopedRole(roleName, baseRole, scopingConstraint);
                addedRoles.add(roleName);
            }
        }

        @Override
        public void removeScopedRole(String roleName) {
            synchronized (addedRoles) {
                permissionFactory.removeScopedRole(roleName);
                addedRoles.remove(roleName);
            }
        }

        @Override
        public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
            return authorizer.authorize(caller, callEnvironment, action, target);
        }

        @Override
        public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
            return authorizer.authorize(caller, callEnvironment, action, target);
        }
    }
}
