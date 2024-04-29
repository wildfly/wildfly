/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.superclass;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that @Resource bindings on interceptors that are applied to multiple
 * components without their own naming context work properly, and do not try
 * and create two duplicate bindings in the same namespace.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class SuperClassInjectionTestCase {

    @Deployment
    public static Archive<?> deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "multiple-bindings-superclass.war");
        war.addClasses(Bean1.class, Bean2.class, SuperClassInjectionTestCase.class, SuperBean.class, SimpleStatelessBean.class);
        war.addAsWebInfResource(SuperClassInjectionTestCase.class.getPackage(), "web.xml", "web.xml");
        return war;
    }

    @Test
    public void testCorrectBinding() throws NamingException {
        InitialContext context = new InitialContext();
        Object result = context.lookup("java:module/env/" + SuperBean.class.getName() + "/simpleStatelessBean");
        Assert.assertTrue(result instanceof SimpleStatelessBean);
    }


    @Test
    public void testSubClass1Injected() throws NamingException {
        InitialContext context = new InitialContext();
        Bean1 result = (Bean1) context.lookup("java:module/bean1");
        Assert.assertTrue(result.getBean() instanceof SimpleStatelessBean);
    }

    @Test
    public void testSubClass2Injected() throws NamingException {
        InitialContext context = new InitialContext();
        Bean2 result = (Bean2) context.lookup("java:module/bean2");
        Assert.assertTrue(result.getBean() instanceof SimpleStatelessBean);
    }

    //AS7-6500
    @Test
    public void testOverridenInjectionIsNotInjected() throws NamingException {
        InitialContext context = new InitialContext();
        Bean2 result = (Bean2) context.lookup("java:module/bean2");
        Assert.assertEquals("string2", result.getSimpleString());
        Assert.assertEquals(1, result.getSetCount());

    }

    @Test
    public void testNoInjectionOnOverride() throws NamingException {
        InitialContext context = new InitialContext();
        Bean1 result = (Bean1) context.lookup("java:module/bean1");
        Assert.assertNull(result.getSimpleString());
    }
}
