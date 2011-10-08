/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.testsuite.integration.ejb.home.localhome.descriptor;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleLocalHome;
import org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleLocalInterface;
import org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleStatefulLocalHome;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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

    private static final String EJB_JAR_XML = "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd\"\n" +
            "         version=\"3.1\">\n" +
            "    <enterprise-beans>\n" +
            "        <session>\n" +
            "            <ejb-name>SimpleLocalHomeBean</ejb-name>\n" +
            "            <local-home>org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleLocalHome</local-home>\n" +
            "            <local>org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleLocalInterface</local>\n" +
            "            <ejb-class>org.jboss.as.testsuite.integration.ejb.home.localhome.descriptor.SimpleStatelessLocalBean</ejb-class>\n" +
            "            <session-type>Stateless</session-type>\n" +
            "        </session>\n" +
            "        <session>\n" +
            "            <ejb-name>SimpleStatefulLocalHomeBean</ejb-name>\n" +
            "            <local-home>org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleStatefulLocalHome</local-home>\n" +
            "            <local>org.jboss.as.testsuite.integration.ejb.home.localhome.SimpleLocalInterface</local>\n" +
            "            <ejb-class>org.jboss.as.testsuite.integration.ejb.home.localhome.descriptor.SimpleStatefulLocalBean</ejb-class>\n" +
            "            <session-type>Stateful</session-type>\n" +
            "        </session>\n" +
            "    </enterprise-beans>\n" +
            "</ejb-jar>";


    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> deploy() {

        WebArchive war = ShrinkWrap.create(WebArchive.class, ARCHIVE_NAME);
        war.addPackage(SimpleLocalHomeTestCase.class.getPackage());
        war.addPackage(SimpleLocalInterface.class.getPackage());
        war.addAsWebInfResource(new StringAsset(EJB_JAR_XML), "ejb-jar.xml");
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
