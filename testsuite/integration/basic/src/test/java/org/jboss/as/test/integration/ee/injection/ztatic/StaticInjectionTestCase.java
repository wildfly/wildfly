/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.ztatic;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests static field and method injection for EE apps.
 *
 * @author Eduardo Martins
 *
 */
@ExtendWith(ArquillianExtension.class)
public class StaticInjectionTestCase {

    private static final String DEPLOYMENT_NAME = "static-injection-test-du";

    @EJB(mappedName = "java:global/" + DEPLOYMENT_NAME + "/FieldTestEJB")
    FieldTestEJB fieldTestEJB;

    @EJB(mappedName = "java:global/" + DEPLOYMENT_NAME + "/MethodTestEJB")
    MethodTestEJB methodTestEJB;

    @Deployment
    public static WebArchive createFieldTestDeployment() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
        war.addPackage(StaticInjectionTestCase.class.getPackage());
        war.addAsWebInfResource(StaticInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testStaticInjection() {
        Assertions.assertTrue(!fieldTestEJB.isStaticResourceInjected(), "Static field should not be injected");
        Assertions.assertTrue(!methodTestEJB.isStaticResourceInjected(), "Static method should not be injected");
    }
}
