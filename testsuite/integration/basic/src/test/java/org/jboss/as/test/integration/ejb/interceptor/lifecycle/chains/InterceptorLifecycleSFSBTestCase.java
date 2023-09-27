/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.chains;

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
public class InterceptorLifecycleSFSBTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class,"testlocal.war");
        war.addPackage(InterceptorLifecycleSFSBTestCase.class.getPackage());
        return war;
    }


    @Test
    public void testInterceptorPostConstructWithoutProceed() throws NamingException {
        InitialContext ctx = new InitialContext();
        InterceptedNoProceedSLSB bean = (InterceptedNoProceedSLSB)ctx.lookup("java:module/" + InterceptedNoProceedSLSB.class.getSimpleName());
        bean.doStuff();
        Assert.assertTrue(LifecycleInterceptorNoProceed.postConstruct);
        Assert.assertFalse(InterceptedNoProceedSLSB.isPostConstructCalled());
    }

    @Test
    public void testInterceptorPostConstructWithProceed() throws NamingException {
        InitialContext ctx = new InitialContext();
        InterceptedWithProceedSLSB bean = (InterceptedWithProceedSLSB)ctx.lookup("java:module/" + InterceptedWithProceedSLSB.class.getSimpleName());
        bean.doStuff();
        Assert.assertTrue(LifecycleInterceptorWithProceed.postConstruct);
        Assert.assertTrue(InterceptedWithProceedSLSB.isPostConstructCalled());
    }


}
