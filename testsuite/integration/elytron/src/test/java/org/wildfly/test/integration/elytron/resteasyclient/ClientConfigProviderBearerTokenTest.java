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

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.util.HttpResponseCodes;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.credential.BearerTokenCredential;
import org.wildfly.test.integration.elytron.util.ClientConfigProviderBearerTokenAbortFilter;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ClientConfigProviderBearerTokenTest {

    private String dummyUrl = "dummyUrl";

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
                ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
                ResteasyClient client = builder.build();
                Response response = client.target(dummyUrl)
                        .register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
                Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
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
                ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
                ResteasyClient client = builder.build();
                Response response = client.target(dummyUrl)
                        .register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
                Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
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
                ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
                ResteasyClient client = builder.build();
                try {
                    client.target(dummyUrl).register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
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
                ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
                ResteasyClient client = builder.build();
                try {
                    client.target(dummyUrl).register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
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
            ResteasyClientBuilder builder = (ResteasyClientBuilder) ClientBuilder.newBuilder();
            ResteasyClient client = builder.build();
            Response response = client.target("http://127.0.0.1").register(ClientConfigProviderBearerTokenAbortFilter.class).request().get();
            Assert.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
            client.close();
        });
    }
}
