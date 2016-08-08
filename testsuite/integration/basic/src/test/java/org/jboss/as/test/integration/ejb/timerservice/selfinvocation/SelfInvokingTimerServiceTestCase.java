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
package org.jboss.as.test.integration.ejb.timerservice.selfinvocation;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.InitialContext;

/**
 * Tests that business methods invoked by @Timeout method are treated as such.
 *
 * @author Guido Bonazza
 */
@RunWith(Arquillian.class)
public class SelfInvokingTimerServiceTestCase {

    @Deployment
    public static Archive<?> deploy() {
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "testTimerServiceSimple.war");
        war.addPackage(SelfInvokingTimerServiceTestCase.class.getPackage());
        war.addAsWebInfResource(
                new StringAsset("<beans><interceptors><class>" + CdiInterceptor.class.getName() + "</class></interceptors></beans>"), "beans.xml");
        return war;
    }

    /**
     * Tests that @Asynchronous annotation is honored when called from @Timeout method.
     * 
     * @throws Exception
     */
    @Test
    public void testAsyncMethod() throws Exception {
        InitialContext ctx = new InitialContext();
        SelfAsyncNoViewTimerServiceBean bean = (SelfAsyncNoViewTimerServiceBean) ctx.lookup("java:module/" + SelfAsyncNoViewTimerServiceBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(SelfAsyncNoViewTimerServiceBean.awaitAsyncCall());
    }

    /**
     * Tests that CdiInterceptor is invoked when intercepted method
     * is invoked from @Timeout method.
     * 
     * @throws Exception
     */
    @Test
    public void testCdiInterceptedMethod() throws Exception {
        InitialContext ctx = new InitialContext();
        SelfCdiInterceptedTimerServiceBean bean = (SelfCdiInterceptedTimerServiceBean) ctx.lookup("java:module/" + SelfCdiInterceptedTimerServiceBean.class.getSimpleName());
        bean.interceptedMethod();
        Assert.assertTrue(CdiInterceptor.invoked);
        CdiInterceptor.invoked = false;
        bean.createTimer();
        SelfCdiInterceptedTimerServiceBean.awaitInterceptedMethod();
        Assert.assertTrue(CdiInterceptor.invoked);
    }

    /**
     * Tests that EjbInterceptor is invoked when intercepted method
     * is invoked from @Timeout method.
     * 
     * @throws Exception
     */
    @Test
    public void testEjbInterceptedMethod() throws Exception {
        InitialContext ctx = new InitialContext();
        SelfEjbInterceptedTimerServiceBean bean = (SelfEjbInterceptedTimerServiceBean) ctx.lookup("java:module/" + SelfEjbInterceptedTimerServiceBean.class.getSimpleName());
        bean.interceptedMethod();
        Assert.assertTrue(EjbInterceptor.invoked);
        EjbInterceptor.invoked = false;
        bean.createTimer();
        SelfCdiInterceptedTimerServiceBean.awaitInterceptedMethod();
        Assert.assertTrue(EjbInterceptor.invoked);
    }

}
