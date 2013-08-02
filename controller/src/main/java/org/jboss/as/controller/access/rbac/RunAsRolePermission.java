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

import java.security.BasicPermission;

/**
 * A simple {@link Permission} to allow code being executed without an associated remote to be granted the permission to execute
 * using a specified role.
 *
 * Initially by default only one role is used for in-vm calls, however this could be extended to allow different in-vm calls to
 * be granted different roles.
 *
 * Where a {@link Subject} representing a remote user is already combined with the {@link AccessControlContext} this
 * {@link SecurityManager} check is not used.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public final class RunAsRolePermission extends BasicPermission {

    private static final long serialVersionUID = 640368502336382562L;

    public RunAsRolePermission(final String roleName) {
        super(RunAsRolePermission.class.getName() + "." + roleName);
    }

}
