/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
