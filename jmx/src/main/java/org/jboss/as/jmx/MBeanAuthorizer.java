/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.jmx;

import javax.management.MBeanException;
import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.rbac.RoleMapper;
import org.jboss.as.controller.access.rbac.StandardRole;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MBeanAuthorizer {

    private final boolean nonManagementMbeansSensitive;
    private final RoleMapper roleMapper;

    public MBeanAuthorizer(boolean nonManagementMbeansSensitive, RoleMapper roleMapper) {
        this.nonManagementMbeansSensitive = nonManagementMbeansSensitive;
        this.roleMapper = roleMapper;
    }

    void authorize(boolean readOnly) throws MBeanException {

        PrivilegedAction<Caller> getCallerAction = new PrivilegedAction<Caller>() {
            @Override
            public Caller run() {
                Subject subject = Subject.getSubject(AccessController.getContext());
                return Caller.createCaller(subject);
            }
        };
        Caller caller = System.getSecurityManager() == null ?
                getCallerAction.run() : AccessController.doPrivileged(getCallerAction);

        Set<String> roles = roleMapper.mapRoles(caller, null, null, (TargetAttribute) null);
        if (nonManagementMbeansSensitive) {
            authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR);
        } else {
            if (readOnly) {
                authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR, StandardRole.OPERATOR, StandardRole.MAINTAINER, StandardRole.AUDITOR, StandardRole.MONITOR);
            } else {
                authorize(roles, StandardRole.SUPERUSER, StandardRole.ADMINISTRATOR, StandardRole.OPERATOR, StandardRole.MAINTAINER);
            }
        }
    }

    private void authorize(Set<String> callerRoles, StandardRole...roles) throws MBeanException {
        for (StandardRole role : roles) {
            if (callerRoles.contains(role.toString())) {
                return;
            }
        }
        throw JmxMessages.MESSAGES.unauthorized();
    }
}
