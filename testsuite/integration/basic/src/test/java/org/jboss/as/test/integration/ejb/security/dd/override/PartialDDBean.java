/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.dd.override;

import jakarta.annotation.Resource;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * User: jpai
 */
@Stateless
@SecurityDomain("ejb3-tests")
public class PartialDDBean {

    @Resource
    private SessionContext sessionContext;

    @DenyAll
    public void denyAllMethod() {
        throw new RuntimeException("Invocation on this method shouldn't have been allowed!");
    }

    @PermitAll
    public void permitAllMethod() {

    }

    @RolesAllowed("Role1")
    public void toBeInvokedOnlyByRole1() {
    }

    @RolesAllowed("Role1") // Role1 is set here but overriden as Role2 in ejb-jar.xml
    public void toBeInvokedByRole2() {

    }
}
