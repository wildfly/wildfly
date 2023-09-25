/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.clientinterceptor.secured;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

@Stateless
@Remote(Secured.class)
public class SecuredBean implements Secured {

    @Resource
    private SessionContext sessionContext;

    @PermitAll
    public String permitAll(String message) {
        return message;
    }

    @DenyAll
    public void denyAll(String message) {
    }

    @RolesAllowed("Role1")
    public String roleEcho(final String message) {
        return message;
    }

    @RolesAllowed("Role2")
    public String role2Echo(final String message) {
        return message;
    }

}
