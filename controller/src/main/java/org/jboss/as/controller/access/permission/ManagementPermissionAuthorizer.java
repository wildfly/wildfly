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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.AuthorizationResult.Decision;
import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.JmxAction;
import org.jboss.as.controller.access.JmxAction.Impact;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;
import org.jboss.as.controller.access.rbac.StandardRole;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * {@link Authorizer} based on {@link ManagementPermission}s configured by a {@link PermissionFactory}.
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public class ManagementPermissionAuthorizer implements Authorizer {

    /** A fake action for JMX, just to have an operation with superuser permissions */
    private static final Action FAKE_JMX_ACTION = new Action(Util.createOperation("test", PathAddress.EMPTY_ADDRESS), null);

    private final PermissionFactory permissionFactory;
    private final JmxPermissionFactory jmxPermissionFactory;

    public ManagementPermissionAuthorizer(PermissionFactory permissionFactory, JmxPermissionFactory jmxPermissionFactory) {
        this.permissionFactory = permissionFactory;
        this.jmxPermissionFactory = jmxPermissionFactory;
    }

    @Override
    public AuthorizerDescription getDescription() {
        // We go ahead and create this each time because we expect this to be overridden anyway
        return new AuthorizerDescription() {
            @Override
            public boolean isRoleBased() {
                return true;
            }

            @Override
            public Set<String> getStandardRoles() {
                return Collections.emptySet();
            }
        };
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
                return new AuthorizationResult(AuthorizationResult.Decision.DENY,
                            new ModelNode(ControllerMessages.MESSAGES.permissionDenied()));
            }
        }
        return AuthorizationResult.PERMITTED;
    }

    @Override
    public AuthorizationResult authorizeJmxOperation(Caller caller, Environment callEnvironment, JmxAction action) {
        Set<String> roles = jmxPermissionFactory.getUserRoles(caller, null, FAKE_JMX_ACTION, (TargetResource) null);
        if (action.getImpact() == Impact.EXTRA_SENSITIVE) {
            return authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR);
        } else if (jmxPermissionFactory.isNonFacadeMBeansSensitive()) {
            if (action.getImpact() == Impact.READ_ONLY) {
                return authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR, StandardRole.AUDITOR);
            } else {
                return authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR);
            }
        } else {
            if (action.getImpact() == Impact.READ_ONLY) {
                //Everybody can read mbeans when not sensitive
                return AuthorizationResult.PERMITTED;
                //authorize(exception, roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR, StandardRole.OPERATOR, StandardRole.MAINTAINER, StandardRole.AUDITOR, StandardRole.MONITOR, StandardRole.DEPLOYER);
            } else {
                return authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR, StandardRole.OPERATOR, StandardRole.MAINTAINER);
            }
        }
    }

    @Override
    public Set<String> getCallerRoles(Caller caller, Environment callEnvironment, Set<String> runAsRoles) {
        // Not supported in this base class; see StandardRBACAuthorizer
        return null;
    }

    private AuthorizationResult authorize(Set<String> callerRoles, StandardRole...roles) {
        for (StandardRole role : roles) {
            if (callerRoles.contains(role.getOfficialForm())) {
                return AuthorizationResult.PERMITTED;
            }
        }
        return new AuthorizationResult(Decision.DENY);
    }}
