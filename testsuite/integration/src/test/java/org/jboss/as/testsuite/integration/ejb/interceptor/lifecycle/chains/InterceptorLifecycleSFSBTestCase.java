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
package org.jboss.as.testsuite.integration.ejb.interceptor.lifecycle.chains;

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
        InterceptedNoProceedSFSB bean = (InterceptedNoProceedSFSB)ctx.lookup("java:module/" + InterceptedNoProceedSFSB.class.getSimpleName());
        bean.doStuff();
        Assert.assertTrue(LifecycleInterceptorNoProceed.postConstruct);
        Assert.assertFalse(bean.isPostConstructCalled());
    }

    @Test
    public void testInterceptorPostConstructWithProceed() throws NamingException {
        InitialContext ctx = new InitialContext();
        InterceptedWithProceedSFSB bean = (InterceptedWithProceedSFSB)ctx.lookup("java:module/" + InterceptedWithProceedSFSB.class.getSimpleName());
        bean.doStuff();
        Assert.assertTrue(LifecycleInterceptorNoProceed.postConstruct);
        Assert.assertTrue(bean.isPostConstructCalled());
    }


}
