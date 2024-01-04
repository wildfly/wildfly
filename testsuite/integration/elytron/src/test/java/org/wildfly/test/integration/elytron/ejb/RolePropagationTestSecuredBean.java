/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.elytron.ejb;

import org.junit.Assert;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.ejb.Singleton;

/**
 * EJB that uses RunAs and PostConstruct annotation to successfully call another secured bean without specifying security domain explicitly.
 */
@Singleton
@Startup
@RunAs("TEST")
public class RolePropagationTestSecuredBean {

    @Inject
    private RolePropagationTest rolePropagationTest;

    @PostConstruct
    protected void callSecuredBeanWithTESTRole() {
        Assert.assertEquals(rolePropagationTest.testRunAsWithoutSecDomainSpecified(), "access allowed");
    }

}
