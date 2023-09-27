/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.home.remotehome.descriptor;

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
 * Tests that lifecycle methods defined on classes in a different module to the component class
 * are called.
 */
@RunWith(Arquillian.class)
public class SimpleHomeTestCase {

    private static final String ARCHIVE_NAME = "SimpleHomeTest.war";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(SimpleHomeTestCase.class.getPackage());
        war.addPackage(SimpleInterface.class.getPackage());
        war.addAsWebInfResource(SimpleHomeTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return war;
    }

    @Test
    public void testStatelessLocalHome() throws Exception {
        final SimpleHome home = (SimpleHome) iniCtx.lookup("java:module/SimpleHomeBean!" + SimpleHome.class.getName());
        final SimpleInterface ejbInstance = home.createSimple();
        Assert.assertEquals("Hello World", ejbInstance.sayHello());
    }

    @Test
    public void testGetEjbLocalHome() throws Exception {
        final SimpleHome home = (SimpleHome) iniCtx.lookup("java:module/SimpleHomeBean!" + SimpleHome.class.getName());
        final SimpleInterface ejbInstance = home.createSimple();
        Assert.assertEquals("Hello World", ejbInstance.otherMethod());
    }

    @Test
    public void testStatefulLocalHome() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulHome home = (SimpleStatefulHome) iniCtx.lookup("java:module/SimpleStatefulHomeBean!" + SimpleStatefulHome.class.getName());
        SimpleInterface ejbInstance = home.createSimple(message);
        Assert.assertEquals(message, ejbInstance.sayHello());
        ejbInstance = home.createComplex("hello", "world");
        Assert.assertEquals("hello world", ejbInstance.sayHello());
    }

    @Test
    public void testgetEjbLocalObject() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulHome home = (SimpleStatefulHome) iniCtx.lookup("java:module/SimpleStatefulHomeBean!" + SimpleStatefulHome.class.getName());
        SimpleInterface ejbInstance = home.createSimple(message);
        Assert.assertEquals(message, ejbInstance.otherMethod());
        ejbInstance = home.createComplex("hello", "world");
        Assert.assertEquals("hello world", ejbInstance.otherMethod());
    }
}
