/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.security.common.elytron;

import java.util.List;

/**
 * This interface represent Elytron Security Domain with predefined list of users and their roles. It provides ability to tests
 * to come up with own user population for the tested scenario.
 * <p>
 * <b>Implementation notes:</b> If the Elytron security realm can be preconfigured with user list (e.g. domain implementation is
 * creating a property file with users), then the domain created around such a realm should implement this interface.
 * </p>
 *
 * @author Josef Cacek
 */
public interface UsersRolesSecurityDomain extends SecurityDomain {

    /**
     * Returns predefined (not {@code null}) list of users and their attributes to be created in the domain (realm in fact).
     */
    List<UserWithRoles> getUsersWithRoles();
}
