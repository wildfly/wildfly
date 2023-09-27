/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.initializeinorder;

import java.util.ArrayList;
import java.util.List;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that <initialize-in-order> works as expected.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class InitializeInOrderTestCase {

    private static final List<String> initOrder = new ArrayList<String>();

    @Deployment
    public static Archive<?> deployment() {
        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "init.ear");
        ear.addAsResource(InitializeInOrderTestCase.class.getPackage(), "application.xml", "application.xml");

        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ejb.jar");
        jar.addClasses(MyEjb.class, InitializeInOrderTestCase.class);
        ear.addAsModule(jar);

        final WebArchive war = ShrinkWrap.create(WebArchive.class, "web.war");
        war.addClass(MyServlet.class);
        ear.addAsModule(war);

        return ear;
    }

    @Test
    public void testPostConstruct() throws NamingException {
        Assert.assertEquals(2, initOrder.size());
        Assert.assertEquals("MyServlet", initOrder.get(0));
        Assert.assertEquals("MyEjb", initOrder.get(1));
    }

    public static void recordInit(final String clazz) {
        initOrder.add(clazz);
    }

}
