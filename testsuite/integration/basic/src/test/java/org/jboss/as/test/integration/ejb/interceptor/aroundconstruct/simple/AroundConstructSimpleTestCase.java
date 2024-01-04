/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.aroundconstruct.simple;

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
 * Tests that lifecycle interceptors are handed correctly,
 * as per the interceptors specification.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class AroundConstructSimpleTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "testlocal.war");
        war.addPackage(AroundConstructSimpleTestCase.class.getPackage());
        return war;
    }


    @Test
    public void testSimpleAroundConstruct() throws NamingException {
        InitialContext ctx = new InitialContext();
        AroundConstructSLSB bean = (AroundConstructSLSB) ctx.lookup("java:module/" + AroundConstructSLSB.class.getSimpleName());
        Assert.assertEquals("AroundConstructPostConstruct", bean.getMessage());
    }

    @Test
    public void testSimpleAroundConstructWhichReturnsObject() throws NamingException {
        InitialContext ctx = new InitialContext();
        AroundConstructInterceptorWithObjectReturnTypeSLSB bean = (AroundConstructInterceptorWithObjectReturnTypeSLSB) ctx.lookup("java:module/" + AroundConstructInterceptorWithObjectReturnTypeSLSB.class.getSimpleName());
        Assert.assertEquals("AroundConstructPostConstruct", bean.getMessage());
    }

}
