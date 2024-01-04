/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
        AuthenticationConfiguration config = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
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
            UseCaseValidator useCaseValidator = new UseCaseValidator(UseCaseValidator.Interface.REMOTE);
            try {
                useCaseValidator = bean.invoke(useCaseValidator);
            } catch(TestException te) {
                Assert.fail(te.getMessage());
            }
            return useCaseValidator;
        });
    }
}
