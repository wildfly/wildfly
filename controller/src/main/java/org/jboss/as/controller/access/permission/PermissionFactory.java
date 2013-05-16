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

import java.security.PermissionCollection;

import org.jboss.as.controller.access.Action;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.Environment;
import org.jboss.as.controller.access.TargetAttribute;
import org.jboss.as.controller.access.TargetResource;

/**
 * Factory for a compatible set of permissions. Implementations must ensure that the permissions returned
 * by the {@code getUserPermissions} methods are compatible with the permissions returned by the
 * {@code getRequiredPermissions} methods. Compatible means the user permissions can correctly
 * evaluate whether they
 * {@link java.security.Permission#implies(java.security.Permission) imply the required permissions}
 *
 * @author Brian Stansberry (c) 2013 Red Hat Inc.
 */
public interface PermissionFactory {

    PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetAttribute target);

    PermissionCollection getUserPermissions(Caller caller, Environment callEnvironment, Action action, TargetResource target);

    PermissionCollection getRequiredPermissions(Action action, TargetAttribute target);

    PermissionCollection getRequiredPermissions(Action action, TargetResource target);
}
