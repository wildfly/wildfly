/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.managedbean;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructInterceptor;
import org.jboss.as.test.integration.ee.injection.support.InjectionSupportTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
        war.addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
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
