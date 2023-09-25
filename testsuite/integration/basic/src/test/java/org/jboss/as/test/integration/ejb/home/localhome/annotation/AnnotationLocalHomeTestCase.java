/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.home.localhome.annotation;

import jakarta.ejb.EJBException;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.home.localhome.SimpleLocalHome;
import org.jboss.as.test.integration.ejb.home.localhome.SimpleLocalInterface;
import org.jboss.as.test.integration.ejb.home.localhome.SimpleStatefulLocalHome;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the @LocalHome annotation works as expected
 */
@RunWith(Arquillian.class)
public class AnnotationLocalHomeTestCase {

    private static final String ARCHIVE_NAME = "SimpleLocalHomeTest.war";


    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(AnnotationLocalHomeTestCase.class.getPackage());
        war.addPackage(SimpleLocalInterface.class.getPackage());
        return war;
    }

    @Test
    public void testStatelessLocalHome() throws Exception {
        final SimpleLocalHome home = (SimpleLocalHome) iniCtx.lookup("java:module/SimpleStatelessLocalBean!" + SimpleLocalHome.class.getName());
        final SimpleLocalInterface ejbInstance = home.createSimple();
        Assert.assertEquals("Hello World", ejbInstance.sayHello());
    }

    @Test
    public void testGetEjbLocalHome() throws Exception {
        final SimpleLocalHome home = (SimpleLocalHome) iniCtx.lookup("java:module/SimpleStatelessLocalBean!" + SimpleLocalHome.class.getName());
        Assert.assertTrue(home.createSimple().getEJBLocalHome() instanceof SimpleLocalHome);
    }

    @Test
    public void testStatefulLocalHome() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulLocalHome home = (SimpleStatefulLocalHome) iniCtx.lookup("java:module/SimpleStatefulLocalBean!" + SimpleStatefulLocalHome.class.getName());
        SimpleLocalInterface ejbInstance = home.createSimple(message);
        Assert.assertEquals(message, ejbInstance.sayHello());
        ejbInstance = home.createComplex("hello", "world");
        Assert.assertEquals("hello world", ejbInstance.sayHello());
    }

    @Test
    public void testGetEjbLocalObject() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulLocalHome home = (SimpleStatefulLocalHome) iniCtx.lookup("java:module/SimpleStatefulLocalBean!" + SimpleStatefulLocalHome.class.getName());
        SimpleLocalInterface ejbInstance = home.createSimple(message);
        try {
            Assert.assertEquals(message, ejbInstance.otherMethod());
        } catch (EJBException e) {
            Assert.assertEquals(IllegalStateException.class, e.getCause().getClass());
        }
    }
}
