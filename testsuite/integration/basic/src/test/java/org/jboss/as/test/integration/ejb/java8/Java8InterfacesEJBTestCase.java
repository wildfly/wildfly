/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.java8;

import jakarta.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.classfilewriter.InvalidBytecodeException;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that deployment does not fail with {@link InvalidBytecodeException} if EJB's local interface declares a static method (Java 8 feature).
 * See WFLY-4316 for details.
 *
 * @author Jozef Hartinger
 * @author Stuart Douglas
 */
@ExtendWith(ArquillianExtension.class)
public class Java8InterfacesEJBTestCase {

    @Inject
    private Airplane airplane;

    @Deployment
    public static Archive<?> getDeployment() {
        return ShrinkWrap.create(WebArchive.class).addPackage(Java8InterfacesEJBTestCase.class.getPackage());
    }

    @Test
    public void testDeploymentWorks() {
        Assertions.assertTrue(Airplane.barrelRoll());
        Assertions.assertTrue(airplane.takeOff());
    }


    // See https://issues.jboss.org/browse/WFCORE-3512
    @Test
    public void testDefaultMethodWorks() {
        Assertions.assertEquals("Cargo", airplane.getPlaneType());
    }
}
