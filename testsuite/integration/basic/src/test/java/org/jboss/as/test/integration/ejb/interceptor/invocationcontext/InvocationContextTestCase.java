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

package org.jboss.as.test.integration.ejb.interceptor.invocationcontext;

import javax.naming.InitialContext;
import javax.naming.NamingException;

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
 * Interceptor invocation context testing.
 *
 * @author Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class InvocationContextTestCase {

    @ArquillianResource
    InitialContext ctx;

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "invocation-context.jar");
        jar.addPackage(InvocationContextTestCase.class.getPackage());
        jar.addAsManifestResource(InvocationContextTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testInvocationContext() throws NamingException {
        final InvocationBean bean = (InvocationBean) ctx.lookup("java:module/" + InvocationBean.class.getSimpleName());

        final String result = bean.callMethod(1, "invoked");
        Assert.assertEquals("DefaultOK:ClassOK:MethodOK:BeanOK:invokedDefaultClassMethodBean", result);
    }

    @Test
    public void tesTimerInvocationContext() throws NamingException {
        TimeoutBean bean = (TimeoutBean) ctx.lookup("java:module/" + TimeoutBean.class.getSimpleName());
        bean.createTimer();
        Assert.assertTrue(TimeoutBean.awaitTimerCall());
        Assert.assertEquals("TimeoutDefaultOK:TimeoutClassOK:TimeoutMethodOK:TimeoutBeanOK:@Timeout", TimeoutBean.interceptorResults);
    }
}
