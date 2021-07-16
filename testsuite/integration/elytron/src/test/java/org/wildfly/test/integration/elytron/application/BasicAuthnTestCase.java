/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.application;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.security.common.Utils;
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
import org.wildfly.test.integration.elytron.util.ClientConfigProviderNoBasicAuthorizationHeaderFilter;
import org.wildfly.test.integration.elytron.util.HttpAuthorization;

import javax.ws.rs.Priorities;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

/**
 * Smoke test for web application authentication using Elytron with default server configuration.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public class BasicAuthnTestCase {

    private static final String NAME = BasicAuthnTestCase.class.getSimpleName();

    /**
     * Creates WAR with a secured servlet and BASIC authentication configured in web.xml deployment descriptor.
     */
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addClasses(SimpleServlet.class)
                .addAsWebInfResource(BasicAuthnTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    /**
     * Tests access without authentication.
     */
    @Test
    public void testUnprotectedAccess(@ArquillianResource URL url) throws Exception {
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCall(url.toURI(), SC_OK));
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCall(new URI(url.toExternalForm() + "foo"), SC_OK));
    }

    /**
     * Tests '*' wildcard in authn-constraints.
     */
    @Test
    public void testAllRolesAllowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "all");

        assertUrlProtected(servletUrl);

        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_OK));
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_OK));
        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "guest", "guest", SC_OK));
    }

    /**
     * Test case sensitivity of role in authn-constraints.
     */
    @Test
    public void testCaseSensitiveRole(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "users");

        assertUrlProtected(servletUrl);

        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(servletUrl, "guest", "guest", SC_FORBIDDEN);
    }

    /**
     * Tests single role in authn-constraint.
     */
    @Test
    public void testUser1Allowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");

        assertUrlProtected(servletUrl);

        assertEquals("Response body is not correct.", SimpleServlet.RESPONSE_BODY,
                Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password1", SC_OK));

        Utils.makeCallWithBasicAuthn(servletUrl, "user2", "password2", SC_FORBIDDEN);
        Utils.makeCallWithBasicAuthn(servletUrl, "guest", "guest", SC_FORBIDDEN);
    }

    /**
     * Tests empty authn-constraint in web.xml.
     */
    @Test
    public void testNoRoleAllowed(@ArquillianResource URL url) throws Exception {
        final URL servletUrl = new URL(url.toExternalForm() + "prohibited");

        Utils.makeCall(servletUrl.toURI(), SC_FORBIDDEN);
    }

    private void assertUrlProtected(final URL servletUrl) throws Exception, URISyntaxException, IOException {
        Utils.makeCall(servletUrl.toURI(), SC_UNAUTHORIZED);
        // wrong password
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "password", SC_UNAUTHORIZED);
        Utils.makeCallWithBasicAuthn(servletUrl, "user1", "Password1", SC_UNAUTHORIZED);
        // unknown user
        Utils.makeCallWithBasicAuthn(servletUrl, "User1", "password1", SC_UNAUTHORIZED);
    }

    /**
     *  Test that RESTEasy client successfully uses Elytron client configuration to authenticate to the secured server with HTTP BASIC auth.
     */
    @Test
    public void testRESTEasyClientUsesElytronConfigAuthenticatedUser(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL, adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            Response response = client.target(servletUrl.toString()).request().get();
            Assert.assertEquals(SC_OK, response.getStatus());
            client.close();
        });
    }

    /**
     * Test that RESTEasy client ignores ClientConfigProvider credentials if credentials are specified directly by user for RESTEasy client.
     */
    @Test
    public void testClientConfigCredentialsAreIgnoredIfSpecified(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("incorrectUsername").usePassword("incorrectPassword");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL, adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            client.register(HttpAuthorization.basic("user1", "password1"));
            Response response = client.target(servletUrl.toString()).request().get();
            Assert.assertEquals(SC_OK, response.getStatus());
            client.close();
        });
    }

    /**
     * Test secured resource with correct credentials of user that is authorized to the resource.
     * Bearer token from ClientConfigProvider impl is ignored since credentials are specified for RESTEasy client.
     */
    @Test
    public void testClientConfigBearerTokenIsIgnoredIfBasicSpecified(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        BearerTokenCredential bearerTokenCredential = new BearerTokenCredential("myTestToken");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useBearerTokenCredential(bearerTokenCredential);
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL, adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            client.register(HttpAuthorization.basic("user1", "password1"));
            client.register(ClientConfigProviderBearerTokenAbortFilter.class);
            try {
                client.target(servletUrl.toString()).request().get();
                fail("Configuration not found ex should be thrown.");
            } catch (Exception e) {
                // check that bearer token was not added
                assertTrue(e.getMessage().contains("The request authorization header is not correct expected:<B[earer myTestToken]> but was:<B[asic"));
                client.close();
            }
        });
    }

    /**
     * Unauthorized user's credentials were set on Elytron client and so authentication will fail with 403.
     */
    @Test
    public void testClientConfigForbiddenUser(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user2").usePassword("password2");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL, adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            Response response = client.target(servletUrl.toString()).request().get();
            Assert.assertEquals(SC_FORBIDDEN, response.getStatus());
            client.close();
        });
    }

    /**
     * Test that access will be unauthenticated when accessing secured resource with RESTEasy client without credentials set on Elytron client config.
     */
    @Test
    public void testClientUnauthenticatedUser(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty();
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL, adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            Response response = client.target(servletUrl.toString()).request().get();
            Assert.assertEquals(SC_UNAUTHORIZED, response.getStatus());
            client.close();
        });
    }

    /**
     * Test that access credentials from ClientConfigProvider are used only if both username and password are present.
     */
    @Test
    public void testClientConfigProviderUsernameWithoutPasswordWillBeIgnored(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("thisNameWillBeIgnoredBecausePasswordIsMissing");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL, adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            client.register(new ClientConfigProviderNoBasicAuthorizationHeaderFilter(), Priorities.USER);
            try {
                client.target(servletUrl.toString()).request().get();
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("The request authorization header is not correct expected:<Bearer myTestToken> but was:<null>"));
                client.close();
            }
            Response response = builder.build().target(servletUrl.toString()).request().get();
            Assert.assertEquals(SC_UNAUTHORIZED, response.getStatus());
            client.close();
        });
    }

    /**
     * Test that Elytron config credentials are not used when specified for different destination of the request.
     */
    @Test
    public void testClientConfigProviderChooseCredentialsBasedOnDestination(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL.matchHost("www.some-example.com"), adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            Response response = client.target(servletUrl.toString()).request().get();
            // will be unauthorized because credentials were set for different hostname than we are calling
            Assert.assertEquals(SC_UNAUTHORIZED, response.getStatus());
            client.close();
        });
    }

    /**
     * Test that ClientConfigProvider credentials are used when specified for requested  URL.
     */
    @Test
    public void testClientConfigProviderChooseCredentialsBasedOnDestination2(@ArquillianResource URL url) throws MalformedURLException {
        final URL servletUrl = new URL(url.toExternalForm() + "role1");
        AuthenticationConfiguration adminConfig = AuthenticationConfiguration.empty().useName("user1").usePassword("password1");
        AuthenticationContext context = AuthenticationContext.empty();
        context = context.with(MatchRule.ALL.matchHost(servletUrl.getHost()), adminConfig);
        context.run(() -> {
            ClientBuilder builder = ClientBuilder.newBuilder();
            Client client = builder.build();
            Response response = client.target(servletUrl.toString()).request().get();
            // will be authorized because we are calling hostname that credentials are set for
            Assert.assertEquals(SC_OK, response.getStatus());
            Assert.assertEquals("response was not GOOD", "GOOD", response.readEntity(String.class));
            client.close();
        });
    }
}
