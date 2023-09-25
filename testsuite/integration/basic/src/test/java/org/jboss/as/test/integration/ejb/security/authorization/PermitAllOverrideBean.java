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
@PermitAll
@SecurityDomain("ejb3-tests")
public class PermitAllOverrideBean {

    public String defaultEcho(final String message) {
        return message;
    }

    @DenyAll
    public String denyAllEcho(final String message) {
        return message;
    }

    @RolesAllowed("Role1")
    public String role1Echo(final String message) {
        return message;
    }

}
