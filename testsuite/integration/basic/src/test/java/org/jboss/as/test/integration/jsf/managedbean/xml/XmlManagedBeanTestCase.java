/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.jsf.managedbean.xml;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class XmlManagedBeanTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "jsfmanagedbean.war");
        war.addPackage(XmlManagedBeanTestCase.class.getPackage());
        war.addAsWebInfResource(new StringAsset(""), "beans.xml");
        war.addAsWebInfResource(new StringAsset("<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<faces-config version=\"1.2\" \n" +
                "    xmlns=\"http://java.sun.com/xml/ns/javaee\" \n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
                "    xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-facesconfig_1_2.xsd\">\n" +
                "    <managed-bean eager=\"true\">\n" +
                "        <managed-bean-name>simpleBean</managed-bean-name>\n" +
                "        <managed-bean-class>"+SimpleJsfXmlManagedBean.class.getName()+"</managed-bean-class>\n" +
                "        <managed-bean-scope>application</managed-bean-scope>\n" +
                "    </managed-bean>\n" +
                "</faces-config>"), "faces-config.xml");
        return war;
    }

    @Test
    public void testPostConstructCalled() {
        Assert.assertTrue(SimpleJsfXmlManagedBean.isPostConstructCalled());
    }


    @Test
    public void testUserTransactionInjected() {
        Assert.assertTrue(SimpleJsfXmlManagedBean.isUserTransactionInjected());
    }


    @Test
    public void testInitializerMethodCalled() {
        Assert.assertTrue(SimpleJsfXmlManagedBean.isInitializerCalled());
    }

}
