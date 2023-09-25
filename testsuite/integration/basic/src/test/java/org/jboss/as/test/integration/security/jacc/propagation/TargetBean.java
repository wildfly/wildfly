/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.jacc.propagation;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;

/**
 * An implementation of {@link Manage} interface which has protected {@link #admin()} and {@link #manage()} methods.
 *
 * @author Josef Cacek
 */
@Stateless(name = Manage.BEAN_NAME_TARGET)
@DeclareRoles({Manage.ROLE_ADMIN, Manage.ROLE_MANAGER, Manage.ROLE_USER})
public class TargetBean implements Manage {

    // Public methods --------------------------------------------------------

    /**
     * Method with only {@link Manage#ROLE_ADMIN} access.
     *
     * @return
     */
    @RolesAllowed({ROLE_ADMIN})
    public String admin() {
        return RESULT;
    }

    /**
     * Method with only {@link Manage#ROLE_MANAGER} access.
     *
     * @return
     */
    @RolesAllowed({ROLE_MANAGER})
    public String manage() {
        return RESULT;
    }

    /**
     * Unprotected method.
     *
     * @return
     */
    @PermitAll
    public String work() {
        return RESULT;
    }

}
