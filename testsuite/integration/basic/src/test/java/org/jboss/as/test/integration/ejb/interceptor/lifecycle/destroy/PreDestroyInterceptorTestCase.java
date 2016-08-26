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
package org.jboss.as.test.integration.ejb.interceptor.lifecycle.destroy;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that lifecycle interceptors are handed correctly,
 * as per the interceptors specification.
 *
 * @author Stuart Douglas, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
public class PreDestroyInterceptorTestCase {

    @Deployment
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class,"testpredestroy.jar");
        jar.addPackage(PreDestroyInterceptorTestCase.class.getPackage());
        jar.addAsManifestResource(PreDestroyInterceptorTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        return jar;
    }

    @Test
    public void testPreDestroyInterceptor() throws NamingException {

        InitialContext ctx = new InitialContext();
        PreDestroySFSB bean = (PreDestroySFSB)ctx.lookup("java:module/" + PreDestroySFSB.class.getSimpleName());
        Assert.assertTrue(PreDestroySFSB.postConstructCalled);
        Assert.assertTrue(PreDestroyInterceptor.postConstruct);
        Assert.assertFalse("InvocationContext.getTarget() was null for post-construct interceptor", PreDestroyInterceptor.postConstructInvocationTargetNull);
        Assert.assertTrue(PreDestroyInterceptorDescriptor.postConstruct);
        Assert.assertFalse("InvocationContext.getTarget() was null for post-construct interceptor", PreDestroyInterceptorDescriptor.postConstructInvocationTargetNull);

        bean.remove();
        Assert.assertTrue(PreDestroySFSB.preDestroyCalled);
        Assert.assertTrue(PreDestroyInterceptor.preDestroy);
        Assert.assertFalse("InvocationContext.getTarget() was null for pre-destroy interceptor", PreDestroyInterceptor.preDestroyInvocationTargetNull);
        Assert.assertTrue(PreDestroyInterceptorDescriptor.preDestroy);
        Assert.assertFalse("InvocationContext.getTarget() was null for pre-destroy interceptor", PreDestroyInterceptorDescriptor.preDestroyInvocationTargetNull);
    }
}
