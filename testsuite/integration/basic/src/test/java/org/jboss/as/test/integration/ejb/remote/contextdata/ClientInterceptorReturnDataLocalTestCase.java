/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.remote.contextdata;

import javax.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that context data from the server is returned to client interceptors
 * for an in-vm invocation.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class ClientInterceptorReturnDataLocalTestCase {

    @Deployment()
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ClientInterceptorReturnDataLocalTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(ClientInterceptorReturnDataLocalTestCase.class.getPackage());
        jar.addAsServiceProvider(EJBClientInterceptor.class, ClientInterceptor.class);
        return jar;
    }

    @EJB
    TestRemote testSingleton;

    @Test
    public void testInvokeWithClientInterceptorData() throws Exception {
        String result = testSingleton.invoke();
        Assert.assertEquals("DATA:client interceptor data(client data):bean context data", result);
    }
}
