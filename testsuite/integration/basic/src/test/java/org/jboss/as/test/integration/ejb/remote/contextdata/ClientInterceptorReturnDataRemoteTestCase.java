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

import java.net.URI;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that context data from the server is returned to client interceptors
 * for an true remote invocation.
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorReturnDataRemoteTestCase {

    @ArquillianResource
    URL url;

    @Deployment()
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ClientInterceptorReturnDataRemoteTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(ClientInterceptorReturnDataRemoteTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testInvokeWithClientInterceptorData() throws Throwable {

        EJBClientContext context = new EJBClientContext.Builder().addInterceptor(new ClientInterceptor())
                .addTransportProvider(new RemoteTransportProvider())
                .addClientConnection(new EJBClientConnection.Builder()
                        .setForDiscovery(true)
                        .setDestination(new URI("remote+http", null, url.getHost(), url.getPort(), null, null, null))
                        .build()).build();

        context.runExceptionAction(new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
                Properties props = new Properties();
                props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
                InitialContext ic = new InitialContext(props);
                TestRemote bean = (TestRemote) ic.lookup("ejb:/" + ClientInterceptorReturnDataRemoteTestCase.class.getSimpleName() + "/" + TestSLSB.class.getSimpleName() + "!" + TestRemote.class.getName());
                Assert.assertEquals("DATA:client interceptor data(client data):bean context data", bean.invoke());
                return null;
            }
        });
    }
}
