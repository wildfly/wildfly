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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.JAR_ENTRY_EJB;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BASIC;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BEARER_TOKEN;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_FORM;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;

import java.io.IOException;
import java.util.concurrent.Callable;

import javax.ejb.EJBAccessException;

import org.jboss.as.cli.CommandLineException;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.security.common.Utils;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Identity switching for security context propagation test.
 *
 * @author Hynek Švábek <hsvabek@redhat.com>
 */
public abstract class AbstractIdentitySwitchingTestCase extends AbstractSecurityContextPropagationTestBase {

    /**
     * Setup seccontext-server1.
     */
    protected void setupServer1() throws CommandLineException, IOException, MgmtOperationException {
        server1.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(JAR_ENTRY_EJB, WAR_ENTRY_SERVLET_BASIC, WAR_ENTRY_SERVLET_FORM,
                        WAR_ENTRY_SERVLET_BEARER_TOKEN)
                .withCliCommands("/subsystem=elytron/simple-permission-mapper=seccontext-server-permissions"
                    + ":write-attribute(name=permission-mappings[1],value={principals=[entry],"
                    + "permissions=[{class-name=org.wildfly.security.auth.permission.LoginPermission},"
                    + "{class-name=org.wildfly.security.auth.permission.RunAsPrincipalPermission, target-name=authz},"
                    + "{class-name=org.wildfly.security.auth.permission.RunAsPrincipalPermission, target-name="
                    + "no-server2-identity}]})")
                .build());
    }

    /**
     * Setup seccontext-server2.
     */
    protected void setupServer2() throws CommandLineException, IOException, MgmtOperationException {
        server2.resetContainerConfiguration(new ServerConfigurationBuilder()
                .withDeployments(WAR_WHOAMI)
                .withCliCommands("/subsystem=elytron/simple-permission-mapper=seccontext-server-permissions"
                    + ":write-attribute(name=permission-mappings[1],value={principals=[entry],"
                    + "permissions=[{class-name=org.wildfly.security.auth.permission.LoginPermission},"
                    + "{class-name=org.wildfly.security.auth.permission.RunAsPrincipalPermission, target-name=authz},"
                    + "{class-name=org.wildfly.security.auth.permission.RunAsPrincipalPermission, target-name="
                    + "no-server2-identity}]})")
                .build());
    }

    /**
     * Test Elytron API used to reauthorization.
     *
     * <pre>
     * When: EJB client calls EntryBean with {@link ReAuthnType#AC_AUTHENTICATION} and provides valid credentials and valid authorization name and
     * calls WhoAmIBean with {@link ReAuthnType#AC_AUTHORIZATION} and provides valid credentials and valid authorization name
     * Then: call passes and returned usernames are the expected ones;
     * </pre>
     */
    @Test
    public void testAuthCtxAuthzPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
            getDoubleWhoAmICallable(ReAuthnType.AC_AUTHORIZATION, "server", "server", "admin"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "entry", "admin" },
            doubleWhoAmI);
    }

    /**
     * Test Elytron API used to reauthorization.
     *
     * <pre>
     * When: EJB client calls EntryBean with {@link ReAuthnType#AC_AUTHORIZATION} and provides valid credentials and valid authorization name for both servers
     * Then: call passes and returned usernames are the expected ones;
     * </pre>
     */
    @Test
    public void testAuthzCtxAuthzPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry", "authz",
            getDoubleWhoAmICallable(ReAuthnType.AC_AUTHORIZATION, "server", "server", "whoami"), ReAuthnType.AC_AUTHORIZATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "authz", "whoami" },
            doubleWhoAmI);
    }

    /**
     * Test Elytron API used to reauthorization.
     *
     * <pre>
     * When: EJB client calls EntryBean with {@link ReAuthnType#AC_AUTHENTICATION} and provides valid credentials and valid authorization name and
     * calls WhoAmIBean with {@link ReAuthnType#AC_AUTHENTICATION} and provides valid credentials
     * Then: call passes and returned usernames are the expected ones;
     * </pre>
     */
    @Test
    public void testAuthzCtxAuthPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry", "authz",
            getDoubleWhoAmICallable(ReAuthnType.AC_AUTHENTICATION, "whoami", "whoami"), ReAuthnType.AC_AUTHORIZATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "authz", "whoami" },
            doubleWhoAmI);
    }

    /**
     * Test EJB call fails when user has insufficient roles.
     *
     * <pre>
     * When: EJB client calls EntryBean as a user without allowed roles assigned
     * Then: call fails with EJBAccessExcption
     * </pre>
     */
    @Test
    public void testClientInsufficientRolesAuthz() throws Exception {
        try {
            SeccontextUtil.switchIdentity("server", "server", "whoami", getDoubleWhoAmICallable(ReAuthnType.AC_AUTHORIZATION),
                ReAuthnType.AC_AUTHORIZATION);
            fail("Calling Entry bean must fail when user without required roles is used");
        } catch (EJBAccessException e) {
            // OK - expected
        }
    }

    /**
     * Test EJB call fails when invalid username/password combination is used for reauthorization.
     *
     * <pre>
     * When: EJB client calls (with valid credentials and authrozation name) EntryBean and Elytron AuthenticationContext API is used to
     *       reauthorizate (with invalid username/password) and call the WhoAmIBean
     * Then: WhoAmIBean call fails
     * </pre>
     */
    @Test
    public void testAuthzCtxWrongUserFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry", "authz",
                getDoubleWhoAmICallable(ReAuthnType.AC_AUTHORIZATION, "doesntexist", "whoami"), ReAuthnType.AC_AUTHORIZATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("authz", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test EJB call fails when invalid username/password combination is used for reauthorization.
     *
     * <pre>
     * When: EJB client calls (with valid credentials and authrozation name) EntryBean and Elytron AuthenticationContext API is used to
     *       reauthorizate (with invalid username/password) and call the WhoAmIBean
     * Then: WhoAmIBean call fails
     * </pre>
     */
    @Test
    public void testAuthzCtxWrongPasswdFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry", "authz",
                getDoubleWhoAmICallable(ReAuthnType.AC_AUTHORIZATION, "whoami", "wrongpass"), ReAuthnType.AC_AUTHORIZATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("authz", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test EJB call fails when invalidauthorization name is used for reauthorization.
     *
     * <pre>
     * When: EJB client calls (with valid credentials and authrozation name) EntryBean and Elytron AuthenticationContext API is used to
     *       reauthorizate (with invalid authorization name) and call the WhoAmIBean
     * Then: WhoAmIBean call fails
     * </pre>
     */
    @Test
    public void testAuthzCtxWrongAuthzNameFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry", "authz",
                getDoubleWhoAmICallable(ReAuthnType.AC_AUTHORIZATION, "whoami", "whoami", "doesntexist"), ReAuthnType.AC_AUTHORIZATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("authz", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test Elytron API used to reauthentication.
     *
     * <pre>
     * When: EJB client calls EntryBean with {@link ReAuthnType#AC_AUTHENTICATION} and provides valid credentials for both servers
     * Then: call passes and returned usernames are the expected ones;
     * </pre>
     */
    @Test
    public void testAuthCtxPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.AC_AUTHENTICATION), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[]{"entry", "whoami"},
                doubleWhoAmI);
    }

    /**
     * Test EJB call fails when user has insufficient roles.
     *
     * <pre>
     * When: EJB client calls EntryBean as a user without allowed roles assigned
     * Then: call fails with EJBAccessExcption
     * </pre>
     */
    @Test
    public void testClientInsufficientRoles() throws Exception {
        try {
            SeccontextUtil.switchIdentity("whoami", "whoami", getDoubleWhoAmICallable(ReAuthnType.AC_AUTHENTICATION),
                    ReAuthnType.AC_AUTHENTICATION);
            fail("Calling Entry bean must fail when user without required roles is used");
        } catch (EJBAccessException e) {
            // OK - expected
        }
    }

    /**
     * Test EJB call fails when invalid username/password combination is used for reauthentication.
     *
     * <pre>
     * When: EJB client calls (with valid credentials) EntryBean and Elytron AuthenticationContext API is used to
     *       reauthenticate (with invalid username/password) and call the WhoAmIBean
     * Then: WhoAmIBean call fails
     * </pre>
     */
    @Test
    public void testAuthCtxWrongUserFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.AC_AUTHENTICATION, "doesntexist", "whoami"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test EJB call fails when invalid username/password combination is used for reauthentication.
     *
     * <pre>
     * When: EJB client calls (with valid credentials) EntryBean and Elytron AuthenticationContext API is used to
     *       reauthenticate (with invalid username/password) and call the WhoAmIBean
     * Then: WhoAmIBean call fails
     * </pre>
     */
    @Test
    public void testAuthCtxWrongPasswdFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.AC_AUTHENTICATION, "whoami", "wrongpass"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test the security domain reauthentication on one server is not propagated to second server without explicitly asking for
     * identity forwarding.
     *
     * <pre>
     * When: EJB client calls EntryBean as "entry" user and Elytron AuthenticationContext API is used to
     *       re-authenticate to the security domain as "whoami" user; WhoAmIBean is called
     * Then: WhoAmIBean call fails as the whoami identity is not propagated
     * </pre>
     */
    @Test
    public void testSecurityDomainAuthenticateWithoutForwarding() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SD_AUTHENTICATION), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    /**
     * Test the security domain reauthentication fails when wrong password is used
     *
     * <pre>
     * When: EJB client calls EntryBean as "entry" user and Elytron AuthenticationContext API is used to
     *       re-authenticate to the security domain as "whoami" user with wrong password provided
     * Then: reauthentication fails
     * </pre>
     */
    @Test
    public void testSecurityDomainAuthenticateWrongPassFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SD_AUTHENTICATION, "doesntexist", "whoami"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEvidenceVerificationError());
    }

    /**
     * Test the security domain reauthentication followed by authentication forwarding is possible.
     *
     * <pre>
     * When: EJB client calls EntryBean as "entry" user and Elytron AuthenticationContext API is used to
     *       re-authenticate to the security domain as "whoami" user and
     *       the authentication forwarding is configured afterwards; WhoAmIBean is called
     * Then: WhoAmIBean returns "whoami"
     * </pre>
     */
    @Test
    public void testSecurityDomainAuthenticateForwardedPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SD_AUTHENTICATION_FORWARDED), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[]{"entry", "whoami"},
                doubleWhoAmI);
    }

    /**
     * Test reauthentication through authentication context API when using HTTP BASIC authentication.
     *
     * <pre>
     * When: HTTP client calls EntryServlet (using BASIC authn) and Elytron API is used to reauthenticate
     *       and call the WhoAmIBean
     * Then:
     *      - call as "servlet" and reauthenticate as "whoami" passes and returns "whoami"
     *      - call as "servlet" and reauthenticate as "admin" passes and returns "admin"
     *      - call as "servlet" and reauthenticate as "whoami" passes and returns "whoami"
     *      - call as "admin" and reauthenticate as "xadmin" fails as "xadmin" is not valid user
     *      - call as "admin" and reauthenticate as "admin" with wrong password fails
     * </pre>
     */
    @Test
    public void testServletBasicToEjbAuthenticationContext() throws Exception {
        // call with users who have all necessary roles
        assertEquals("Unexpected username returned", "whoami",
                Utils.makeCallWithBasicAuthn(
                        getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "whoami", "whoami", ReAuthnType.AC_AUTHENTICATION),
                        "servlet", "servlet", SC_OK));

        // call with another user who have sufficient roles on EJB
        assertEquals("Unexpected username returned", "admin",
                Utils.makeCallWithBasicAuthn(
                        getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "admin", "admin", ReAuthnType.AC_AUTHENTICATION), "servlet",
                        "servlet", SC_OK));

        // call with another servlet user
        assertEquals("Unexpected username returned", "whoami",
                Utils.makeCallWithBasicAuthn(
                        getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "whoami", "whoami", ReAuthnType.AC_AUTHENTICATION), "admin",
                        "admin", SC_OK));

        // call with wrong EJB username
        assertThat(Utils.makeCallWithBasicAuthn(
                getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "xadmin", "admin", ReAuthnType.AC_AUTHENTICATION), "admin", "admin",
                SC_OK), isEjbAuthenticationError());

        // call with wrong EJB password
        assertThat(Utils.makeCallWithBasicAuthn(
                getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "admin", "adminx", ReAuthnType.AC_AUTHENTICATION), "admin", "admin",
                SC_OK), isEjbAuthenticationError());

        //authorization
        // call with users who have all necessary roles
        assertEquals("Unexpected username returned", "whoami",
                Utils.makeCallWithBasicAuthn(
                        getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "server", "server", "whoami", ReAuthnType.AC_AUTHORIZATION),
                        "servlet", "servlet", SC_OK));

        // call with another user who have sufficient roles on EJB
        assertEquals("Unexpected username returned", "authz",
                Utils.makeCallWithBasicAuthn(
                getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "entry", "entry", "authz", ReAuthnType.AC_AUTHORIZATION),
                "servlet",
                        "servlet", SC_OK));

        // call with another servlet user
        assertEquals("Unexpected username returned", "authz",
                Utils.makeCallWithBasicAuthn(
                getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "entry", "entry", "authz", ReAuthnType.AC_AUTHORIZATION), "admin",
                        "admin", SC_OK));

        // call with wrong EJB username
        assertThat(Utils.makeCallWithBasicAuthn(
                getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "xentry", "entry", "authz", ReAuthnType.AC_AUTHORIZATION), "admin", "admin",
                SC_OK), isEjbAuthenticationError());

        // call with wrong EJB password
        assertThat(Utils.makeCallWithBasicAuthn(
                getEntryServletUrl(WAR_ENTRY_SERVLET_BASIC, "entry", "entryx", "authz", ReAuthnType.AC_AUTHORIZATION), "admin", "admin",
                SC_OK), isEjbAuthenticationError());
    }

    /**
     * Tests if re-authentication works for HttpURLConnection calls.
     */
    @Test
    @Ignore("WFLY-9442")
    public void testHttpReauthn() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AC_AUTHENTICATION, "servlet", "servlet");
        String servletResponse = SeccontextUtil.switchIdentity("admin", "admin", callable, ReAuthnType.AC_AUTHENTICATION);
        assertEquals("Unexpected principal name returned from servlet call", "servlet", servletResponse);
    }

    /**
     * Tests propagation when user propagated to HttpURLConnection has insufficient roles.
     */
    @Test
    @Ignore("WFLY-9442")
    public void testHttpReauthnInsufficientRoles() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AC_AUTHENTICATION, "whoami", "whoami");
        String servletResponse = SeccontextUtil.switchIdentity("entry", "entry", callable, ReAuthnType.AC_AUTHENTICATION);
        assertThat(servletResponse, allOf(startsWith("java.io.IOException"), containsString("403")));
    }

    /**
     * Tests propagation when user propagated to HttpURLConnection has insufficient roles.
     */
    @Test
    public void testHttpReauthnWrongPass() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AC_AUTHENTICATION, "servlet", "whoami");
        String servletResponse = SeccontextUtil.switchIdentity("entry", "entry", callable, ReAuthnType.AC_AUTHENTICATION);
        assertThat(servletResponse, allOf(startsWith("java.io.IOException"), containsString("401")));
    }
}
