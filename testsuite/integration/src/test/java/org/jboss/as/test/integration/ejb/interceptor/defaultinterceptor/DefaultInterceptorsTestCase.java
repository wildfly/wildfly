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
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "testdefaultinterceptors.jar");
        archive.addPackage(DefaultInterceptorsTestCase.class.getPackage());
        archive.addAsManifestResource(new StringAsset("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ejb-jar xmlns=\"http://java.sun.com/xml/ns/javaee\"\n" +
                "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "         xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd\"\n" +
                "         version=\"3.0\">\n" +
                "    <interceptors>\n" +
                "      <interceptor>\n" +
                "         <interceptor-class>" + DefaultInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor>\n" +
                "      <interceptor>\n" +
                "         <interceptor-class>" + ClassInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor>\n" +
                "      <interceptor>\n" +
                "         <interceptor-class>" + MethodInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor>\n" +
                "    </interceptors>\n" +
                "   <assembly-descriptor>\n" +
                "      <interceptor-binding>\n" +
                "         <ejb-name>*</ejb-name>\n" +
                "         <interceptor-class>" + DefaultInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor-binding>\n" +
                "      <interceptor-binding>\n" +
                "         <ejb-name>NoDefaultInterceptorsSLSB</ejb-name>\n" +
                "         <interceptor-class>" + ClassInterceptor.class.getName() + "</interceptor-class>\n" +
                "      </interceptor-binding>\n" +
                "      <interceptor-binding>\n" +
                "         <ejb-name>NoDefaultInterceptorsSLSB</ejb-name>\n" +
                "         <interceptor-class>" + MethodInterceptor.class.getName() + "</interceptor-class>\n" +
                "         <method><method-name>noClassLevel</method-name></method>" +
                "      </interceptor-binding>\n" +
                "   </assembly-descriptor>\n" +
                "\n" +
                "</ejb-jar>"), "ejb-jar.xml");
        return archive;
    }


    @Test
    public void testDefaultInterceptorApplied() throws NamingException {
        InitialContext ctx = new InitialContext();
        DefaultInterceptedSLSB bean = (DefaultInterceptedSLSB) ctx.lookup("java:module/" + DefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    /**
     * AS7-1436
     * <p/>
     * Test interceptor is applied twice, if it is both class level and a default interceptor
     *
     * @throws NamingException
     */
    @Test
    public void testDefaultInterceptorAppliedTwice() throws NamingException {
        InitialContext ctx = new InitialContext();
        RepeatedDefaultInterceptedSLSB bean = (RepeatedDefaultInterceptedSLSB) ctx.lookup("java:module/" + RepeatedDefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + DefaultInterceptor.MESSAGE + DefaultInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }


    @Test
    public void testClassLevelExcludeDefaultInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        NoDefaultInterceptorsSLSB bean = (NoDefaultInterceptorsSLSB) ctx.lookup("java:module/" + NoDefaultInterceptorsSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals(ClassInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(!bean.isPostConstructCalled());
    }

    @Test
    public void testClassLevelExcludeDefaultMethodLevelExcludeClassInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        NoDefaultInterceptorsSLSB bean = (NoDefaultInterceptorsSLSB) ctx.lookup("java:module/" + NoDefaultInterceptorsSLSB.class.getSimpleName());
        final String message = bean.noClassLevel();
        Assert.assertEquals(MethodInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(!bean.isPostConstructCalled());
    }

    @Test
    public void testMethodLevelExcludeDefaultInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        RepeatedDefaultInterceptedSLSB bean = (RepeatedDefaultInterceptedSLSB) ctx.lookup("java:module/" + RepeatedDefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.noClassLevel();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    @Test
    public void testMethodLevelExcludeDefaultAndClassInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        RepeatedDefaultInterceptedSLSB bean = (RepeatedDefaultInterceptedSLSB) ctx.lookup("java:module/" + RepeatedDefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.noClassLevelOrDefault();
        Assert.assertEquals("Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

}
