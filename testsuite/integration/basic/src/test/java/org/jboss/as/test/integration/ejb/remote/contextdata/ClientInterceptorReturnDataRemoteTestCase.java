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
import java.net.URISyntaxException;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.naming.client.WildFlyInitialContextFactory;

/**
 * Tests that context data from the server is returned to client interceptors
 * for an true remote invocation.
 *
 * @author Stuart Douglas
 * @author Brad Maxwell
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ClientInterceptorReturnDataRemoteTestCase {

    @ArquillianResource
    URL url;

    @ContainerResource
    private ManagementClient managementClient;

    private static final String username = "user1";
    private static final String password = "password1";

    private static enum Protocol {
        REMOTE_HTTP("remote+http"),
        HTTP("http");

        private final String value;

        Protocol(String value){
            this.value = value;
        }
        @Override
        public String toString() {
            return value;
        }
    }

    @Deployment()
    public static Archive<?> deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ClientInterceptorReturnDataRemoteTestCase.class.getSimpleName() + ".jar");
        jar.addPackage(ClientInterceptorReturnDataRemoteTestCase.class.getPackage());
        return jar;
    }

    @Test
    public void testInvokeWithClientInterceptorData() throws Throwable {

        EJBClientContext context = EJBClientContext.getCurrent().withAddedInterceptors(new ClientInterceptor());

        context.runExceptionAction(new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {

                Context ic = getInitialContext(Protocol.REMOTE_HTTP);
                TestRemote bean = (TestRemote) ic.lookup("ejb:/" + ClientInterceptorReturnDataRemoteTestCase.class.getSimpleName() + "/" + TestSLSB.class.getSimpleName() + "!" + TestRemote.class.getName());

                UseCaseValidator useCaseValidator = new UseCaseValidator(UseCaseValidator.Interface.REMOTE);
                try {
                    useCaseValidator = bean.invoke(useCaseValidator);
                } catch(TestException te) {
                    Assert.fail(te.getMessage());
                }
                return useCaseValidator;
            }
        });
    }

    @Test
    public void testInvokeOverHttpWithClientInterceptorData() throws Throwable {

        EJBClientContext context = EJBClientContext.getCurrent().withAddedInterceptors(new ClientInterceptor());

        context.runExceptionAction(new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {

                Context ic = getInitialContext(Protocol.HTTP);
                TestRemote bean = (TestRemote) ic.lookup("ejb:/" + ClientInterceptorReturnDataRemoteTestCase.class.getSimpleName()
                        + "/" + TestSLSB.class.getSimpleName() + "!" + TestRemote.class.getName());

                UseCaseValidator useCaseValidator = new UseCaseValidator(UseCaseValidator.Interface.REMOTE);
                try {
                    useCaseValidator = bean.invoke(useCaseValidator);
                } catch (TestException te) {
                    Assert.fail(te.getMessage());
                }
                return useCaseValidator;
            }
        });
    }

    private Context getInitialContext(Protocol protocol)  throws Exception {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY,  WildFlyInitialContextFactory.class.getName());
        props.put(Context.PROVIDER_URL, getHttpUri(protocol).toString());
        if(username != null && password != null) {
           props.put(Context.SECURITY_PRINCIPAL, username);
           props.put(Context.SECURITY_CREDENTIALS, password);
         }
        return new InitialContext(props);
     }

    private URI getHttpUri(Protocol protocol) throws URISyntaxException {
        URI webUri = managementClient.getWebUri();
        if(protocol == Protocol.HTTP)
            return new URI(protocol.toString(), webUri.getUserInfo(), webUri.getHost(), webUri.getPort(), "/wildfly-services", "", "");
        else
            return new URI(protocol.toString(), null, webUri.getHost(), webUri.getPort(), null, null, null);
    }
}