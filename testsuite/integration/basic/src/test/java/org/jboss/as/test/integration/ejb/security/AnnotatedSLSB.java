/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * User: jpai
 */
@Stateless
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@Local ({Restriction.class, FullAccess.class})
@LocalBean
@SecurityDomain("other")
public class AnnotatedSLSB extends Base implements Restriction, FullAccess {


    @Override
    @DenyAll
    public void restrictedMethod() {
        throw new RuntimeException("This method was supposed to be restricted to all!");
    }

    @Override
    public void overriddenMethod() {

    }

    @RolesAllowed({}) // this should act like a @DenyAll
    public void methodWithEmptyRolesAllowedAnnotation() {
        throw new RuntimeException("This method was supposed to be restricted to all!");
    }

}
