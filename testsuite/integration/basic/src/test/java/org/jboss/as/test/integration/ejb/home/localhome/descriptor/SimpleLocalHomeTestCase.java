/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.home.localhome.descriptor;

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
 * Tests that lifecycle methods defined on classes in a different module to the component class
 * are called.
 */
@RunWith(Arquillian.class)
public class SimpleLocalHomeTestCase {

    private static final String ARCHIVE_NAME = "SimpleLocalHomeTest.war";

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(SimpleLocalHomeTestCase.class.getPackage());
        war.addPackage(SimpleLocalInterface.class.getPackage());
        war.addAsWebInfResource(SimpleLocalHomeTestCase.class.getPackage(),"ejb-jar.xml" , "ejb-jar.xml");
        return war;
    }

    @Test
    public void testStatelessLocalHome() throws Exception {
        final SimpleLocalHome home = (SimpleLocalHome) iniCtx.lookup("java:module/SimpleLocalHomeBean!" + SimpleLocalHome.class.getName());
        final SimpleLocalInterface ejbInstance = home.createSimple();
        Assert.assertEquals("Hello World", ejbInstance.sayHello());
    }

    @Test
    public void testGetEjbLocalHome() throws Exception {
        final SimpleLocalHome home = (SimpleLocalHome) iniCtx.lookup("java:module/SimpleLocalHomeBean!" + SimpleLocalHome.class.getName());
        final SimpleLocalInterface ejbInstance = home.createSimple();
        Assert.assertEquals("Hello World", ejbInstance.otherMethod());
    }

    @Test
    public void testStatefulLocalHome() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulLocalHome home = (SimpleStatefulLocalHome) iniCtx.lookup("java:module/SimpleStatefulLocalHomeBean!" + SimpleStatefulLocalHome.class.getName());
        SimpleLocalInterface ejbInstance = home.createSimple(message);
        Assert.assertEquals(message, ejbInstance.sayHello());
        ejbInstance = home.createComplex("hello", "world");
        Assert.assertEquals("hello world", ejbInstance.sayHello());
    }

    @Test
    public void testgetEjbLocalObject() throws Exception {
        final String message = "Bean Message";
        final SimpleStatefulLocalHome home = (SimpleStatefulLocalHome) iniCtx.lookup("java:module/SimpleStatefulLocalHomeBean!" + SimpleStatefulLocalHome.class.getName());
        SimpleLocalInterface ejbInstance = home.createSimple(message);
        Assert.assertEquals(message, ejbInstance.otherMethod());
        ejbInstance = home.createComplex("hello", "world");
        Assert.assertEquals("hello world", ejbInstance.otherMethod());
    }
}
