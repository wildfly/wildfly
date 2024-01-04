/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.defaultinterceptor;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
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
        archive.addAsManifestResource(DefaultInterceptorsTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return archive;
    }

    @Test
    public void testDefaultInterceptorApplied() throws NamingException {
        InitialContext ctx = new InitialContext();
        DefaultInterceptedSLSB bean = (DefaultInterceptedSLSB) ctx.lookup("java:module/"
                + DefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    /**
     * AS7-1436 Test interceptor is applied twice, if it is both class level and a default interceptor
     *
     * @throws NamingException
     */
    @Test
    public void testDefaultInterceptorAppliedTwice() throws NamingException {
        InitialContext ctx = new InitialContext();
        RepeatedDefaultInterceptedSLSB bean = (RepeatedDefaultInterceptedSLSB) ctx.lookup("java:module/"
                + RepeatedDefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + DefaultInterceptor.MESSAGE + DefaultInterceptor.MESSAGE + "Hello",
                message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    @Test
    public void testClassLevelExcludeDefaultInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        NoDefaultInterceptorsSLSB bean = (NoDefaultInterceptorsSLSB) ctx.lookup("java:module/"
                + NoDefaultInterceptorsSLSB.class.getSimpleName());
        final String message = bean.message();
        Assert.assertEquals(ClassInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(!bean.isPostConstructCalled());
    }

    @Test
    public void testClassLevelExcludeDefaultMethodLevelExcludeClassInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        NoDefaultInterceptorsSLSB bean = (NoDefaultInterceptorsSLSB) ctx.lookup("java:module/"
                + NoDefaultInterceptorsSLSB.class.getSimpleName());
        final String message = bean.noClassLevel();
        Assert.assertEquals(MethodInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(!bean.isPostConstructCalled());
    }

    @Test
    public void testMethodLevelExcludeDefaultInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        RepeatedDefaultInterceptedSLSB bean = (RepeatedDefaultInterceptedSLSB) ctx.lookup("java:module/"
                + RepeatedDefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.noClassLevel();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + "Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    @Test
    public void testMethodLevelExcludeDefaultAndClassInterceptors() throws NamingException {
        InitialContext ctx = new InitialContext();
        RepeatedDefaultInterceptedSLSB bean = (RepeatedDefaultInterceptedSLSB) ctx.lookup("java:module/"
                + RepeatedDefaultInterceptedSLSB.class.getSimpleName());
        final String message = bean.noClassLevelOrDefault();
        Assert.assertEquals("Hello", message);
        Assert.assertTrue(bean.isPostConstructCalled());
    }

    @Test
    public void testMethodLevelExcludeDefaultAndClassInterceptorsDescriptorDef() throws NamingException {
        InitialContext ctx = new InitialContext();
        DefaultAndClassInterceptedSLSB bean = (DefaultAndClassInterceptedSLSB) ctx.lookup("java:module/"
                + DefaultAndClassInterceptedSLSB.class.getSimpleName());
        final String message1 = bean.defaultAndClassIntercepted();
        Assert.assertEquals(DefaultInterceptor.MESSAGE + ClassInterceptor.MESSAGE + "Hello", message1);
        final String message2 = bean.noClassAndDefaultInDescriptor();
        Assert.assertEquals(MethodInterceptor.MESSAGE + "Hi", message2);
        Assert.assertTrue(bean.isPostConstructCalled());
    }
}
