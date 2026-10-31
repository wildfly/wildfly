/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.managedbean;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.ee.injection.support.AroundConstructInterceptor;
import org.jboss.as.test.integration.ee.injection.support.InjectionSupportTestCase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Matus Abaffy
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertTrue(AroundConstructInterceptor.aroundConstructCalled, "AroundConstruct interceptor method not invoked.");
    }

    @Test
    public void testManagedBeanConstructorInjection() throws Exception {
        ManagedBeanWithInject bean = (ManagedBeanWithInject) context.lookup("java:module/"
                + ManagedBeanWithInject.class.getSimpleName());
        Assertions.assertEquals("Joe", bean.getName(), "Constructor injection failed.");
    }

    @Test
    public void testManagedBeanConstructorInjectionAndInterception() throws Exception {
        AroundConstructInterceptor.reset();
        ComplicatedManagedBean bean = (ComplicatedManagedBean) context.lookup("java:module/"
                + ComplicatedManagedBean.class.getSimpleName());
        Assertions.assertTrue(AroundConstructInterceptor.aroundConstructCalled);
        Assertions.assertEquals("AroundConstructInterceptor#Joe#ComplicatedManagedBean", bean.getName());
    }
}
