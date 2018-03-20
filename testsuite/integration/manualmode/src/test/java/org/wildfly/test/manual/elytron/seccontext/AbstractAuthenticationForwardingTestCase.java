/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.manual.elytron.seccontext;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.jboss.as.test.integration.security.common.Utils.REDIRECT_STRATEGY;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.wildfly.test.manual.elytron.seccontext.AbstractSecurityContextPropagationTestBase.isEjbAccessException;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BASIC;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BEARER_TOKEN;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_FORM;

import java.net.URL;
import java.util.concurrent.Callable;
import javax.ejb.EJBAccessException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.as.test.integration.security.common.Utils;
import org.junit.Ignore;
import org.junit.Test;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.credential.BearerTokenCredential;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * Authentication forwarding (credential forwarding) for security context propagation test.
 *
 * @author Josef Cacek
 */
public abstract class AbstractAuthenticationForwardingTestCase extends AbstractSecurityContextPropagationTestBase {

    /**
     * Test forwarding authentication (credential forwarding) works for EJB calls.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authentication forwarding  to WhoAmIBean call
     * Then: credentials are reused for WhoAmIBean call and it correctly returns "admin" username
     * </pre>
     */
    @Test
    public void testForwardedAuthenticationPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[]{"admin", "admin"},
                doubleWhoAmI);
    }

    /**
     * Test the EJB call fails when using forwarding authentication (credential forwarding) and user has insufficient roles.
     *
     * <pre>
     * When: EJB client calls EntryBean as entry user and Elytron AuthenticationContext API is used to
     *       authentication forwarding to WhoAmIBean call
     * Then: calling WhoAmIBean fails
     * </pre>
     */
    @Test
    public void testForwardedIdentityInsufficientRolesFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAccessException());
    }

    /**
     * Test the authentication propagation (credentials forwarding) works for OAUTHBEARER SASL mechanism.
     *
     * <pre>
     * When: EJB client calls EntryBean with valid OAuth bearer token of "admin" user. The
     *       authentication forwarding is configured and WhoAmIBean is called
     * Then: the bearer token is forwarded and WhoAmIBean call returns "admin" username
     * </pre>
     */
    @Test
    public void testOauthbearerPropagationPasses() throws Exception {
        String[] doubleWhoAmI = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                        .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("admin"))))
                .runCallable(getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null));
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[]{"admin", "admin"},
                doubleWhoAmI);
    }

    /**
     * Test the authentication propagation (credentials forwarding) fails for OAUTHBEARER SASL mechanism when user has
     * insufficient roles for the call.
     *
     * <pre>
     * When: EJB client calls EntryBean with valid OAuth bearer token of "entry" user. The
     *       authentication forwarding is configured and WhoAmIBean is called
     * Then: the WhoAmIBean call fails as the "entry" user has not roles allowed for the call
     * </pre>
     */
    @Test
    public void testOauthbearerPropagationInsufficientRolesFails() throws Exception {
        String[] doubleWhoAmI = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                        .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("entry"))))
                .runCallable(getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null));
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAccessException());
    }

    /**
     * Test the EJB call using OAUTHBEARER SASL mechanism authentication fails when user has insufficient roles for the call.
     *
     * <pre>
     * When: EJB client calls EntryBean with valid OAuth bearer token of "whoami" user
     * Then: the EntryBean call fails as the "whoami" user has not roles allowed for the call
     * </pre>
     */
    @Test
    public void testClientOauthbearerInsufficientRolesFails() throws Exception {
        try {
            AuthenticationContext.empty()
                    .with(MatchRule.ALL,
                            AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                            .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("whoami"))))
                    .runCallable(getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null));
            fail("Call to the protected bean should fail");
        } catch (EJBAccessException e) {
            // OK - expected
        }
    }

    /**
     * Tests the HTTP calls to EntryServlet using BASIC mechanism authentication with forwarding authentication (credentials).
     *
     * <pre>
     * When: HTTP client calls EntryServlet (using BASIC authn) and Elytron API is used to forward authentication
     *       to WhoAmIBean
     * Then:
     *      - "entry" user is not allowed to call EntryServlet (SC_FORBIDDEN returned)
     *      - "servlet" user is allowed to call EntryServlet, but WhoAmIBean call fails (insufficient roles)
     *      - "admin" user is allowed to call EntryServlet and credentials are reused for WhoAmIBean call - returns "admin"
     *      - once more called as "servlet" user - it's allowed to call EntryServlet, but WhoAmIBean call fails (insufficient roles)
     * </pre>
     */
    @Test
    public void testServletBasicToEjbForwardedIdentity() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);

        // call with user who doesn't have sufficient roles on Servlet
        Utils.makeCallWithBasicAuthn(entryServletUrl, "entry", "entry", SC_FORBIDDEN);

        // call with user who doesn't have sufficient roles on EJB
        assertThat(Utils.makeCallWithBasicAuthn(entryServletUrl, "servlet", "servlet", SC_OK), isEjbAccessException());

        // call with user who has all necessary roles
        assertEquals("Unexpected username returned", "admin",
                Utils.makeCallWithBasicAuthn(entryServletUrl, "admin", "admin", SC_OK));

        // call (again) with the user who doesn't have sufficient roles on EJB
        assertThat(Utils.makeCallWithBasicAuthn(entryServletUrl, "servlet", "servlet", SC_OK), isEjbAccessException());
    }

    /**
     * Test credentials propagation from HTTP FORM authentication when the servlet which needs propagation is not the
     * authenticated one (i.e. it's requested after the user is already authenticated).
     *
     * <pre>
     * When: HTTP client calls WhoAmIServlet as "admin" (using FORM authn) and then EntryServlet (already authenticated);
     *       the EntryServlet uses Elytron API to forward authentication (credentials) and call WhoAmIBean
     * Then: both call succeeds and WhoAmIBean returns "admin"
     * </pre>
     */
    @Test
    public void testServletFormWhoAmIFirst() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_FORM, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);
        final URL whoAmIServletUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet", "admin",
                    doHttpRequestFormAuthn(httpClient, whoAmIServletUrl, true, "admin", "admin", SC_OK));
            assertEquals("Unexpected result from EntryServlet", "admin", doHttpRequest(httpClient, entryServletUrl, SC_OK));
        }
    }

    /**
     * Test credentials propagation from HTTP FORM authentication when the servlet which needs propagation is not the
     * authenticated one (i.e. it's requested after the user is already authenticated).
     *
     * <pre>
     * When: HTTP client calls WhoAmIServlet as "servlet" (using FORM authn) and then EntryServlet (already authenticated);
     *       the EntryServlet uses Elytron API to forward authentication (credentials) and call WhoAmIBean
     * Then: EntryServlet forwards credentials, but the "servlet" user has not roles allowed to call the WhoAmIBean and the call fails
     * </pre>
     */
    @Test
    public void testServletFormWhoAmIFirstInsufficientRoles() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_FORM, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);
        final URL whoAmIServletUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet", "servlet",
                    doHttpRequestFormAuthn(httpClient, whoAmIServletUrl, true, "servlet", "servlet", SC_OK));
            assertThat("Unexpected result from EntryServlet", doHttpRequest(httpClient, entryServletUrl, SC_OK),
                    isEjbAccessException());
        }
    }

    /**
     * Test credentials propagation from HTTP FORM authentication when the servlet which needs propagation is the authenticated
     * one.
     *
     * <pre>
     * When: HTTP client calls EntryServlet as "admin" (using FORM authn);
     *       the EntryServlet uses Elytron API to forward authentication (credentials) and call WhoAmIBean
     *       subsequently the WhoAmIServlet is called (already authenticated)
     * Then: both servlet call succeeds and WhoAmIBean returns "admin"
     * </pre>
     */
    @Test
    public void testServletFormEntryFirst() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_FORM, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);
        final URL whoAmIServletUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from EntryServlet", "admin",
                    doHttpRequestFormAuthn(httpClient, entryServletUrl, true, "admin", "admin", SC_OK));
            assertEquals("Unexpected result from WhoAmIServlet", "admin", doHttpRequest(httpClient, whoAmIServletUrl, SC_OK));
        }
    }

    /**
     * Test credentials propagation from HTTP FORM authentication when the servlet which needs propagation is the authenticated
     * one.
     *
     * <pre>
     * When: HTTP client calls EntryServlet as "servlet" (using FORM authn);
     *       the EntryServlet uses Elytron API to forward authentication (credentials) and call WhoAmIBean
     *       subsequently the WhoAmIServlet is called (already authenticated)
     * Then: WhoAmIBean call fails (as the "servlet" has not sufficient roles); the servlet calls pass
     * </pre>
     */
    @Test
    public void testServletFormEntryFirstInsufficientRoles() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_FORM, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);
        final URL whoAmIServletUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertThat("Unexpected result from EntryServlet",
                    doHttpRequestFormAuthn(httpClient, entryServletUrl, true, "servlet", "servlet", SC_OK),
                    isEjbAccessException());
            assertEquals("Unexpected result from WhoAmIServlet", "servlet", doHttpRequest(httpClient, whoAmIServletUrl, SC_OK));
        }
    }

    /**
     * Test credentials propagation from HTTP BEARER_TOKEN authentication.
     */
    @Test
    public void testServletBearerTokenPropagation() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_BEARER_TOKEN, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);
        final URL whoAmIServletUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_BEARER_TOKEN + WhoAmIServlet.SERVLET_PATH);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            final String jwtToken = createJwtToken("admin");
            assertEquals("Unexpected result from WhoAmIServlet", "admin",
                    doHttpRequestTokenAuthn(httpClient, whoAmIServletUrl, jwtToken, SC_OK));
            assertEquals("Unexpected result from EntryServlet", "admin",
                    doHttpRequestTokenAuthn(httpClient, entryServletUrl, jwtToken, SC_OK));
        }

        // do the call without sufficient role in EJB (server2)
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            final String jwtToken = createJwtToken("servlet");
            assertThat("Unexpected result from EntryServlet",
                    doHttpRequestTokenAuthn(httpClient, entryServletUrl, jwtToken, SC_OK), isEjbAccessException());
            assertEquals("Unexpected result from WhoAmIServlet", "servlet",
                    doHttpRequestTokenAuthn(httpClient, whoAmIServletUrl, jwtToken, SC_OK));
        }
    }


    /**
     * Test propagation of RuntimeException back to server1 during a call using the authentication forwarding.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authentication forwarding to WhoAmIBean call with "server" user used as caller server identity
     * Then: WhoAmIBean.throwIllegalStateException call should result in expected IllegalStateException.
     * </pre>
     */
    @Test
    public void testIllegalStateExceptionFromForwardedAuthn() throws Exception {
        String[] doubleWhoAmI = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                        .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("admin"))))
                .runCallable(getWhoAmIAndIllegalStateExceptionCallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null));
        assertNotNull("The entryBean.whoAmIAndIllegalStateException() should return not-null instance", doubleWhoAmI);
        assertEquals("admin", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isExpectedIllegalStateException());
    }

    /**
     * Test propagation of Server2Exception (unknown on server1) back to server1 during a call using the authentication
     * forwarding.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authentication forwarding to WhoAmIBean call with "server" user used as caller server identity
     * Then: WhoAmIBean.throwServer2Exception call should result in expected ClassNotFoundException.
     * </pre>
     */
    @Test
    public void testServer2ExceptionFromForwardedAuthn() throws Exception {
        String[] doubleWhoAmI = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                        .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("admin"))))
                .runCallable(getWhoAmIAndServer2ExceptionCallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null));
        assertNotNull("The entryBean.whoAmIAndServer2Exception() should return not-null instance", doubleWhoAmI);
        assertEquals("admin", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isClassNotFoundException_Server2Exception());
    }

    /**
     * Test identity forwarding for HttpURLConnection calls.
     */
    @Test
    @Ignore("WFLY-9442")
    public void testHttpPropagation() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null);
        String servletResponse = SeccontextUtil.switchIdentity("admin", "admin", callable, ReAuthnType.AC_AUTHENTICATION);
        assertEquals("Unexpected principal name returned from servlet call", "admin", servletResponse);
    }
}
