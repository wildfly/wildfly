/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.resteasyclient;

import static javax.servlet.http.HttpServletResponse.SC_OK;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.credential.BearerTokenCredential;
import org.wildfly.test.integration.elytron.util.ClientConfigProviderBearerTokenAbortFilter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;

@RunAsClient
@RunWith(Arquillian.class)
public class ClientConfigProviderBearerTokenTest {

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, ClientConfigProviderBearerTokenTest.class.getSimpleName() + ".war")
                .addClasses(SimpleServlet.class);
    }

    @ArquillianResource
    private URL dummyUrl;

    /**
     * Test that bearer token is loaded from Elytron client config and is used in Authorization header.
     * This is done with registered filter that checks Authorization header.
     */
    @Test
    public void testClientWithBearerToken() {
        AuthenticationContext previousAuthContext = AuthenticationContext.getContextManager().getGlobalDefault();
        try {
            BearerTokenCredential bearerTokenCredential = new BearerTokenCredential("myTestToken");
            AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useBearerTokenCredential(bearerTokenCredential);
            AuthenticationContext context = AuthenticationContext.empty();
            context = context.with(MatchRule.ALL, adminConfig);
            AuthenticationContext.getContextManager().setGlobalDefault(context);
            context.run(() -> {
                ClientBuilder builder = ClientBuilder.newBuilder();
                Client client = builder.build();
                Response response = client.target(dummyUrl.toString())
                        .register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
                Assert.assertEquals(SC_OK, response.getStatus());
                client.close();
            });
        } finally {
            AuthenticationContext.getContextManager().setGlobalDefault(previousAuthContext);
        }
    }

    /**
     * Test that RESTEasy client uses Bearer token auth and not HTTP BASIC if both username with password and bearer token are present in Elytron client config.
     * This is done with registered filter that checks Authorization header.
     */
    @Test
    public void testClientWithBearerTokenAndCredentials() {
        AuthenticationContext previousAuthContext = AuthenticationContext.getContextManager().getGlobalDefault();
        try {
            BearerTokenCredential bearerTokenCredential = new BearerTokenCredential("myTestToken");
            AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("username").usePassword("password").useBearerTokenCredential(bearerTokenCredential);
            AuthenticationContext context = AuthenticationContext.empty();
            context = context.with(MatchRule.ALL, adminConfig);
            AuthenticationContext.getContextManager().setGlobalDefault(context);
            context.run(() -> {
                ClientBuilder builder = ClientBuilder.newBuilder();
                Client client = builder.build();
                Response response = client.target(dummyUrl.toString())
                        .register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
                Assert.assertEquals(SC_OK, response.getStatus());
                client.close();
            });
        } finally {
            AuthenticationContext.getContextManager().setGlobalDefault(previousAuthContext);
        }
    }

    /**
     * Test that request does not contain Bearer token if none is retrieved from Elytron client config.
     * This is done with registered filter that checks Authorization header.
     */
    @Test
    public void testClientWithoutBearerToken() {
        AuthenticationContext previousAuthContext = AuthenticationContext.getContextManager().getGlobalDefault();
        try {
            AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty();
            AuthenticationContext context = AuthenticationContext.empty();
            context = context.with(MatchRule.ALL, adminConfig);
            AuthenticationContext.getContextManager().setGlobalDefault(context);
            context.run(() -> {
                ClientBuilder builder = ClientBuilder.newBuilder();
                Client client = builder.build();
                try {
                    client.target(dummyUrl.toString().toString()).register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
                    fail("Configuration not found ex should be thrown.");
                } catch (Exception e) {
                    assertTrue(e.getMessage().contains("The request authorization header is not correct expected:<Bearer myTestToken> but was:<null>"));
                } finally {
                    client.close();
                }
            });
        } finally {
            AuthenticationContext.getContextManager().setGlobalDefault(previousAuthContext);
        }
    }

    /**
     * Test that request does choose bearer token based on destination of the request.
     * This test will fail since bearer token was set on different URL.
     */
    @Test
    public void testClientChooseCorrectBearerToken() {
        AuthenticationContext previousAuthContext = AuthenticationContext.getContextManager().getGlobalDefault();
        try {
            BearerTokenCredential bearerTokenCredential = new BearerTokenCredential("myTestToken");
            AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useBearerTokenCredential(bearerTokenCredential);
            AuthenticationContext context = AuthenticationContext.empty();
            context = context.with(MatchRule.ALL.matchHost("www.redhat.com"), adminConfig);
            AuthenticationContext.getContextManager().setGlobalDefault(context);
            context.run(() -> {
                ClientBuilder builder = ClientBuilder.newBuilder();
                Client client = builder.build();
                try {
                    client.target(dummyUrl.toString()).register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
                    fail("Configuration not found ex should be thrown.");
                } catch (Exception e) {
                    assertTrue(e.getMessage().contains("The request authorization header is not correct expected:<Bearer myTestToken> but was:<null>"));
                } finally {
                    client.close();
                }
            });
        } finally {
            AuthenticationContext.getContextManager().setGlobalDefault(previousAuthContext);
        }
    }

    /**
     * Test that request does choose credentials based on destination of the request.
     * Test will succeed since Bearer token was set on requested URL.
     */
    @Test
    public void testClientChooseCorrectBearerToken2() {
        BearerTokenCredential bearerTokenCredential = new BearerTokenCredential("myTestToken");
        AuthenticationConfiguration authenticationConfiguration = AuthenticationConfiguration.empty().useBearerTokenCredential(bearerTokenCredential);
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL.matchHost("127.0.0.1"), authenticationConfiguration);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            Response response = client.target("http://127.0.0.1").register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
            Assert.assertEquals(SC_OK, response.getStatus());
            client.close();
        });
    }
}
