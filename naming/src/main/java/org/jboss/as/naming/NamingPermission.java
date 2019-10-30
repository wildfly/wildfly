/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.naming;

import java.security.BasicPermission;

/**
 * <p>
 * This class is for WildFly Naming's permissions. A permission
 * contains a name (also referred to as a "target name") but
 * no actions list; you either have the named permission
 * or you don't.
 * </p>
 *
 * <p>
 * The naming convention follows the hierarchical property naming convention.
 * An asterisk may appear by itself, or if immediately preceded by a "."
 * may appear at the end of the name, to signify a wildcard match.
 * </p>
 *
 * <p>
 * The target name is the name of the permission. The following table lists all the possible permission target names,
 * and for each provides a description of what the permission allows.
 * </p>
 *
 * <p>
 * <table border=1 cellpadding=5 summary="permission target name,
 *  what the target allows">
 * <tr>
 * <th>Permission Target Name</th>
 * <th>What the Permission Allows</th>
 * </tr>
 *
 * <tr>
 *   <td>setActiveNamingStore</td>
 *   <td>Set the active {@link org.jboss.as.naming.NamingStore}</td>
 * </tr>
 *
 *  </table>
 * </p>
 * @author Eduardo Martins
 */
public class NamingPermission extends BasicPermission {
    /**
     * Creates a new permission with the specified name.
     * The name is the symbolic name of the permission, such as
     * "setActiveNamingStore".
     *
     * @param name the name of the permission.
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is empty.
     */
    public NamingPermission(String name) {
        super(name);
    }

    /**
     * Creates a new permission object with the specified name.
     * The name is the symbolic name of the permission, and the
     * actions String is currently unused and should be null.
     *
     * @param name the name of the permission.
     * @param actions should be null.
     *
     * @throws NullPointerException if <code>name</code> is <code>null</code>.
     * @throws IllegalArgumentException if <code>name</code> is empty.
     */
    public NamingPermission(String name, String actions) {
        super(name, actions);
    }
}
