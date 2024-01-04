/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.Stateless;

/**
 * EJB that will be called to demonstrate that roles have been configured properly even without specifying security domain explicitly.
 */
@Stateless
public class RolePropagationTestImpl implements RolePropagationTest {
    @Override
    @RolesAllowed("TEST")
    public String testRunAsWithoutSecDomainSpecified() {
        return "access allowed";
    }

}
