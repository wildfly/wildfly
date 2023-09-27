/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

import java.util.ArrayList;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Migration test from EJB Testsuite (interceptors, 2061) to AS7 [JIRA JBQA-5483].
 * <p>
 * Interceptor injection test.
 * Bill Burke, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class InterceptorInjectionUnitTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "interceptor-inject-test.jar")
                .addPackage(InterceptorInjectionUnitTestCase.class.getPackage())
                .addAsManifestResource(InterceptorInjectionUnitTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml")
                .addAsManifestResource(InterceptorInjectionUnitTestCase.class.getPackage(), "persistence.xml", "persistence.xml");
        return jar;
    }

    static boolean deployed = false;
    static int test = 0;

    @Test
    public void testInterceptAndInjection() throws Exception {
        MySessionRemote test = (MySessionRemote) ctx.lookup("java:module/" + MySessionBean.class.getSimpleName());
        ArrayList list = test.doit();
        Assert.assertEquals("MyBaseInterceptor", list.get(0));
        Assert.assertEquals("MyInterceptor", list.get(1));
    }

    /**
     * Tests that the {@link SimpleStatelessBean} and its interceptor class {@link SimpleInterceptor}
     * have all the expected fields/methods injected
     *
     * @throws Exception
     */
    @Test
    public void testInjection() throws Exception {
        InjectionTester bean = (InjectionTester) ctx.lookup("java:module/" + SimpleStatelessBean.class.getSimpleName());
        bean.assertAllInjectionsDone();
    }

}
