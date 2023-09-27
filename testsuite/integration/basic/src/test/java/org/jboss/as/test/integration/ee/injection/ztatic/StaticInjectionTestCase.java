/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.ztatic;

import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests static field and method injection for EE apps.
 *
 * @author Eduardo Martins
 *
 */
@RunWith(Arquillian.class)
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
        Assert.assertTrue("Static field should not be injected", !fieldTestEJB.isStaticResourceInjected());
        Assert.assertTrue("Static method should not be injected", !methodTestEJB.isStaticResourceInjected());
    }
}
