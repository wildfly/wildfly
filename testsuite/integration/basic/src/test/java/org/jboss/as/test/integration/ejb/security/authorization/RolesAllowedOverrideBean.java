/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.security.authorization;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@Stateless
@RolesAllowed("Role1")
@SecurityDomain("ejb3-tests")
public class RolesAllowedOverrideBean extends RolesAllowedOverrideBeanBase {

    public String defaultEcho(final String message) {
        return message;
    }

    @PermitAll
    public String permitAllEcho(final String message) {
        return message;
    }

    @DenyAll
    public String denyAllEcho(final String message) {
        return message;
    }

    @RolesAllowed("Role2")
    public String role2Echo(final String message) {
        return message;
    }

    @RolesAllowed("HR")
    public String aMethod(final String message) {
        return message;
    }

}
