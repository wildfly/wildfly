/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.injection.support.managedbean;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructInterceptor;
import org.jboss.as.test.integration.ee.injection.support.InjectionSupportTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Matus Abaffy
 */
@RunWith(Arquillian.class)
public class ManagedBeanConstructTestCase {

    @ArquillianResource
    private InitialContext context;

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "managedbean.war");
        war.addPackage(ManagedBeanConstructTestCase.class.getPackage());
        war.addClasses(InjectionSupportTestCase.constructTestsHelperClasses);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testManagedBeanAroundConstructInterception() throws Exception {
        AroundConstructInterceptor.reset();
        InterceptedManagedBean bean = (InterceptedManagedBean) context.lookup("java:module/"
                + InterceptedManagedBean.class.getSimpleName());
        Assert.assertTrue("AroundConstruct interceptor method not invoked.", AroundConstructInterceptor.aroundConstructCalled);
    }

    @Test
    public void testManagedBeanConstructorInjection() throws Exception {
        ManagedBeanWithInject bean = (ManagedBeanWithInject) context.lookup("java:module/"
                + ManagedBeanWithInject.class.getSimpleName());
        Assert.assertEquals("Constructor injection failed.", "Joe", bean.getName());
    }

    @Test
    public void testManagedBeanConstructorInjectionAndInterception() throws Exception {
        AroundConstructInterceptor.reset();
        ComplicatedManagedBean bean = (ComplicatedManagedBean) context.lookup("java:module/"
                + ComplicatedManagedBean.class.getSimpleName());
        Assert.assertTrue(AroundConstructInterceptor.aroundConstructCalled);
        Assert.assertEquals("AroundConstructInterceptor#Joe#ComplicatedManagedBean", bean.getName());
    }
}
