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
package org.jboss.as.testsuite.integration.ejb.interceptor.defaultinterceptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;

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
 * Tests that default interceptors are correctly applied
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class DefaultInterceptorsTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testdefaultinterceptors.war");
        war.addPackage(DefaultInterceptorsTestCase.class.getPackage());
        war.addAsWebResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                "         version=\"3.0\">\n" +
                "    <interceptors>\n" +
                "      <interceptor>\n" +
                "         <interceptor-class>" + DefaultInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor>\n" +
                "    </interceptors>\n" +
                "   <assembly-descriptor>\n" +
                "      <interceptor-binding>\n" +
                "         <ejb-name>*</ejb-name>\n" +
                "         <interceptor-class>" + DefaultInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor-binding>\n" +
                "   </assembly-descriptor>\n" +
                "\n" +
                "</ejb-jar>"), "ejb-jar.xml");
        return war;
    }


    @Test
    public void testDefaultInterceptorApplied() throws NamingException {
        InitialContext ctx = new InitialContext();
        InterceptedSLSB bean = (InterceptedSLSB) ctx.lookup("java:module/" + InterceptedSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals("DefaultInterceptor Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    @Test
    public void testClassLevelExceludeDefaultInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        NoDefaultInterceptorsSLSB bean = (NoDefaultInterceptorsSLSB) ctx.lookup("java:module/" + NoDefaultInterceptorsSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals("Hello", message);
        Assert.assertTrue(!bean.isPostConstructCalled());
    }

}
