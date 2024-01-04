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
@DenyAll
@SecurityDomain("ejb3-tests")
public class DenyAllOverrideBean {

    public String defaultEcho(final String message) {
        return message;
    }

    @PermitAll
    public String permitAllEcho(final String message) {
        return message;
    }

    @PermitAll
    public String[] permitAllEchoWithArrayParams(final String[] messages) {
        return messages;
    }

    @RolesAllowed("Role1")
    public String role1Echo(final String message) {
        return message;
    }

}
