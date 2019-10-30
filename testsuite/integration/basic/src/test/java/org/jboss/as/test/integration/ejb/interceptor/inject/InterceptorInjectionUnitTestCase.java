/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
