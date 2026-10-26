/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.classinherit;

import javax.naming.InitialContext;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Interceptor must also apply to super-methods. Migrated from EJB3 testsuite [JBQA-5451] from ejbthree471
 *
 * @author Carlo de Wolf, Bill Burke, Ondrej Chaloupka
 */
@ExtendWith(ArquillianExtension.class)
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
        Assertions.assertEquals("InterceptedA: InterceptedB: The Other Message", result);
    }

    @Test
    public void testHasSuperBeenIntercepted() throws Exception {
        B b = (B) ctx.lookup("java:module/" + BBean.class.getSimpleName());

        String result = b.getMessage();
        Assertions.assertEquals("InterceptedA: InterceptedB: The Message", result);
    }

    @Test
    public void testInterceptionWithNoSuperClassAroundInvoke() throws Exception {
        StatelessRemote slWithInterceptor = (StatelessRemote) ctx.lookup("java:module/" + StatelessBean.class.getSimpleName());

        // supposing this chain fo interceptions
        String supposedResult = TestInterceptor.class.getSimpleName() + ":" + StatelessBean.class.getSimpleName();
        String result = slWithInterceptor.method();
        Assertions.assertEquals(supposedResult + ".method()", result);

        result = slWithInterceptor.superMethod();
        Assertions.assertEquals(supposedResult + ".superMethod()", result);
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
        Assertions.assertEquals(supposedResult + ".method()", result);

        result = slWithInterceptorAndBean.superMethod();
        Assertions.assertEquals(supposedResult + ".superMethod()", result);
    }
}
