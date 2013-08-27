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

package org.jboss.as.controller.access.permission;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.JmxTarget;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.dmr.ModelNode;

/**
 * {@link Authorizer} based on {@link ManagementPermission}s configured by a {@link PermissionFactory}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ManagementPermissionAuthorizer implements Authorizer {

    private final PermissionFactory permissionFactory;

    public ManagementPermissionAuthorizer(PermissionFactory permissionFactory) {
        this.permissionFactory = permissionFactory;
    }

    @Override
    public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetAttribute target) {
        // TODO a direct "booting" flag might be better
        if (callEnvironment.getProcessState() == ControlledProcessState.State.STARTING) {
            return AuthorizationResult.PERMITTED;
        }
        PermissionCollection userPerms = permissionFactory.getUserPermissions(caller, callEnvironment, action, target);
        PermissionCollection requiredPerms = permissionFactory.getRequiredPermissions(action, target);
        return authorize(userPerms, requiredPerms);
    }

    @Override
    public AuthorizationResult authorize(Caller caller, Environment callEnvironment, Action action, TargetResource target) {
        // TODO a direct "booting" flag might be better
        if (callEnvironment.getProcessState() == ControlledProcessState.State.STARTING) {
            return AuthorizationResult.PERMITTED;
        }
        PermissionCollection userPerms = permissionFactory.getUserPermissions(caller, callEnvironment, action, target);
        PermissionCollection requiredPerms = permissionFactory.getRequiredPermissions(action, target);
        return authorize(userPerms, requiredPerms);
    }

    private AuthorizationResult authorize(PermissionCollection userPermissions, PermissionCollection requiredPermissions) {

        final Enumeration<Permission> enumeration = requiredPermissions.elements();
        while (enumeration.hasMoreElements()){
            Permission requiredPermission = enumeration.nextElement();
            if (!userPermissions.implies(requiredPermission)) {
                // TODO better explanation, at least i18n
                return new AuthorizationResult(AuthorizationResult.Decision.DENY, new ModelNode("Permission denied"));
            }
        }
        return AuthorizationResult.PERMITTED;
    }

    @Override
    public AuthorizationResult authorizeJmxOperation(Caller caller, Environment callEnvironment, JmxTarget target) {
        //We should never end up here?
        throw new IllegalStateException();
    }
}
