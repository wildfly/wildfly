/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jca.security;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

@Stateless
@SecurityDomain("RaRealm")
@DeclareRoles({"eis-role"})
public class TestBean {

    @RolesAllowed("eis-role")
    public boolean test() {
        return true;
    }
}
