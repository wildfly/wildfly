/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import javax.naming.InitialContext;

import org.junit.Test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * Interceptor must also apply to super-methods. Migrated from EJB3 testsuite [JBQA-5451] from ejbthree471
 *
 * @author Carlo de Wolf, Bill Burke, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class SuperIntercepTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "super-intercept-test.jar");
        jar.addPackage(SuperIntercepTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testHasBeenIntercepted() throws Exception {
        B b = (B) ctx.lookup("java:module/" + BBean.class.getSimpleName());

        String result = b.getOtherMessage();
        Assert.assertEquals("InterceptedA: InterceptedB: The Other Message", result);
    }

    @Test
    public void testHasSuperBeenIntercepted() throws Exception {
        B b = (B) ctx.lookup("java:module/" + BBean.class.getSimpleName());

        String result = b.getMessage();
        Assert.assertEquals("InterceptedA: InterceptedB: The Message", result);
    }

    @Test
    public void testInterceptionWithNoSuperClassAroundInvoke() throws Exception {
        StatelessRemote slWithInterceptor = (StatelessRemote) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName());

        // supposing this chain fo interceptions
        String supposedResult = TestInterceptor.class.getSimpleName() + ":" + StatelessBean.class.getSimpleName();
        String result = slWithInterceptor.method();
        Assert.assertEquals(supposedResult + ".method()", result);

        result = slWithInterceptor.superMethod();
        Assert.assertEquals(supposedResult + ".superMethod()", result);
    }

    @Test
    public void testInterceptionWithSuperClassAroundInvoke() throws Exception {
        StatelessRemote slWithInterceptorAndBean = (StatelessRemote) ctx.lookup("java:module/"
                + StatelessWithBeanInterceptorBean.class.getSimpleName());

        // supposing this chain fo interceptions
        String supposedResult = TestInterceptor.class.getSimpleName() + ":"
                + AbstractBaseClassWithInterceptor.class.getSimpleName() + ":"
                + StatelessWithBeanInterceptorBean.class.getSimpleName();
        String result = slWithInterceptorAndBean.method();
        Assert.assertEquals(supposedResult + ".method()", result);

        result = slWithInterceptorAndBean.superMethod();
        Assert.assertEquals(supposedResult + ".superMethod()", result);
    }
}
