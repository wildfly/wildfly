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
package org.jboss.as.test.integration.ejb.container.interceptor.security.api;

import javax.annotation.security.DeclareRoles;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.Stateless;

/**
 * An implementation of {@link Manage} interface which has protected {@link #role1()} and {@link #role2()} methods.
 *
 * @author Josef Cacek
 */
@Stateless(name = Manage.BEAN_NAME_TARGET)
@DeclareRoles({Manage.ROLE_ROLE1, Manage.ROLE_ROLE2, Manage.ROLE_GUEST, Manage.ROLE_USERS})
@Remote(Manage.class)
public class TargetBean implements Manage {

    // Public methods --------------------------------------------------------

    /**
     * Method with only {@link Manage#ROLE_ROLE1} access.
     *
     * @return
     */
    @RolesAllowed({ROLE_ROLE1})
    public String role1() {
        return RESULT;
    }

    /**
     * Method with only {@link Manage#ROLE_ROLE2} access.
     *
     * @return
     */
    @RolesAllowed({ROLE_ROLE2})
    public String role2() {
        return RESULT;
    }

    /**
     * Unprotected method.
     *
     * @return
     */
    @PermitAll
    public String allRoles() {
        return RESULT;
    }

}
