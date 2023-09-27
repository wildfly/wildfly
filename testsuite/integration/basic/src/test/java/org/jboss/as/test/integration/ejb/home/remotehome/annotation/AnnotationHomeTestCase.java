/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.home.remotehome.annotation;

import static org.junit.Assert.fail;

import java.rmi.NoSuchObjectException;

import jakarta.ejb.EJBException;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleHome;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleInterface;
import org.jboss.as.test.integration.ejb.home.remotehome.SimpleStatefulHome;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the
 */
@RunWith(Arquillian.class)
public class AnnotationHomeTestCase {

    private static final String ARCHIVE_NAME = "SimpleLocalHomeTest.war";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(AnnotationHomeTestCase.class.getPackage());
        war.addPackage(SimpleInterface.class.getPackage());
        return war;
    }

    @Test
    public void testStatelessLocalHome() throws Exception {
        final SimpleHome home = (SimpleHome) iniCtx.lookup("java:module/SimpleStatelessBean!" + SimpleHome.class.getName());
        final SimpleInterface ejbInstance = home.createSimple();
        Assert.assertEquals("Hello World", ejbInstance.sayHello());
    }

    @Test
    public void testGetEjbHome() throws Exception {
        final SimpleHome home = (SimpleHome) iniCtx.lookup("java:module/SimpleStatelessBean!" + SimpleHome.class.getName());
        Assert.assertTrue( home.createSimple().getEJBHome() instanceof SimpleHome);
    }

    @Test
    public void testStatefulLocalHome() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulHome home = (SimpleStatefulHome) iniCtx.lookup("java:module/SimpleStatefulBean!" + SimpleStatefulHome.class.getName());
        SimpleInterface ejbInstance = home.createSimple(message);
        Assert.assertEquals(message, ejbInstance.sayHello());
        ejbInstance = home.createComplex("hello", "world");
        Assert.assertEquals("hello world", ejbInstance.sayHello());
        ejbInstance.remove();
        try {
            ejbInstance.sayHello();
            fail("Expected bean to be removed");
        } catch (NoSuchObjectException expected) {

        }
    }

    @Test
    public void testGetEjbLocalObject() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulHome home = (SimpleStatefulHome) iniCtx.lookup("java:module/SimpleStatefulBean!" + SimpleStatefulHome.class.getName());
        SimpleInterface ejbInstance = home.createSimple(message);
        try {
            Assert.assertEquals(message, ejbInstance.otherMethod());
        } catch (EJBException e) {
            Assert.assertEquals(IllegalStateException.class, e.getCause().getClass());
        }
    }
}
