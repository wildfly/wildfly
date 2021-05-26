/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;

/**
 * Tests that context data from the server is returned to client interceptors
 * for an invocation using EJB over HTTP.
 *
 * @author Joerg Baesner
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorReturnDataEjbOverHttpTestCase {

    @ArquillianResource
    URL url;

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment()
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ClientInterceptorReturnDataEjbOverHttpTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(ClientInterceptorReturnDataEjbOverHttpTestCase.class.getPackage());
        return jar;
    }

    private static AuthenticationContext old;

    @BeforeClass
    public static void setup() {
        AuthenticationConfiguration config = AuthenticationConfiguration.EMPTY.useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty().with(MatchRule.ALL, config);
        old = AuthenticationContext.captureCurrent();
        AuthenticationContext.getContextManager().setGlobalDefault(context);
    }

    @AfterClass
    public static void after() {
        AuthenticationContext.getContextManager().setGlobalDefault(old);
    }

    public Context getContext() throws Exception {
        final Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, getHttpUri());
        return new InitialContext(props);
    }

    private URI getHttpUri() throws URISyntaxException {
        URI webUri = managementClient.getWebUri();
        return new URI("http", webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "/wildfly-services", "", "");
    }

    @Test
    @RunAsClient
    public void testInvokeWithClientInterceptorData() throws Throwable {

        final EJBClientContext ejbClientContext = EJBClientContext.getCurrent().withAddedInterceptors(new ClientInterceptor());

        ejbClientContext.runCallable(() -> {
            TestRemote bean = (TestRemote) getContext().lookup("ejb:/" + ClientInterceptorReturnDataEjbOverHttpTestCase.class.getSimpleName() + "/" + TestSLSB.class.getSimpleName() + "!" + TestRemote.class.getName());
            Assert.assertEquals("DATA:client interceptor data(client data):bean context data", bean.invoke());
            return null;
        });
    }
}
