/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.Principal;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBAccessException;
import javax.security.auth.login.LoginContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.ejb.security.authentication.EntryBean;
import org.jboss.as.test.integration.ejb.security.base.WhoAmIBean;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainSetup;
import org.jboss.as.test.shared.integration.ejb.security.Util;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * Test case to hold the authentication scenarios, these range from calling a servlet which calls a bean to calling a bean which
 * calls another bean to calling a bean which re-authenticated before calling another bean.
 *
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({EjbSecurityDomainSetup.class})
@Category(CommonCriteria.class)
public class AuthenticationTestCase {
    private static final Logger log = Logger.getLogger(AuthenticationTestCase.class.getName());

    @ArquillianResource
    private ManagementClient managementClient;

    /*
     * Authentication Scenarios
     *
     * Client -> Bean
     * Client -> Bean -> Bean
     * Client -> Bean (Re-auth) -> Bean
     * Client -> Servlet -> Bean
     * Client -> Servlet (Re-auth) -> Bean
     * Client -> Servlet -> Bean -> Bean
     * Client -> Servlet -> Bean (Re Auth) -> Bean
     */

    @Deployment
    public static Archive<?> deployment() {
        // using JavaArchive doesn't work, because of a bug in Arquillian, it only deploys wars properly
        final WebArchive war = ShrinkWrap.create(WebArchive.class, "ejb3security.war")
                .addPackage(WhoAmIBean.class.getPackage()).addPackage(EntryBean.class.getPackage())
                .addPackage(HttpRequest.class.getPackage()).addClass(WhoAmI.class).addClass(Util.class).addClass(Entry.class)
                .addClasses(WhoAmIServlet.class, AuthenticationTestCase.class)
                .addClasses(AbstractSecurityDomainSetup.class, EjbSecurityDomainSetup.class)
                .addAsResource(AnnotationAuthorizationTestCase.class.getPackage(), "users.properties", "users.properties")
                .addAsResource(AnnotationAuthorizationTestCase.class.getPackage(), "roles.properties", "roles.properties")
                .addAsWebInfResource(AnnotationAuthorizationTestCase.class.getPackage(), "web.xml", "web.xml")
                .addAsWebInfResource(AnnotationAuthorizationTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml")
                .addAsWebInfResource(AuthenticationTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml")
                .addAsManifestResource(new StringAsset("Manifest-Version: 1.0\nDependencies: org.jboss.as.controller-client,org.jboss.dmr\n"), "MANIFEST.MF");
        war.addPackage(CommonCriteria.class.getPackage());
        log.info(war.toString(true));
        return war;
    }

    @EJB(mappedName = "java:global/ejb3security/WhoAmIBean!org.jboss.as.test.integration.ejb.security.WhoAmI")
    private WhoAmI whoAmIBean;

    @EJB(mappedName = "java:global/ejb3security/EntryBean!org.jboss.as.test.integration.ejb.security.Entry")
    private Entry entryBean;

    @Test
    public void testAuthentication() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            String response = entryBean.whoAmI();
            assertEquals("user1", response);
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testAuthentication_BadPwd() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "wrong_password");
        lc.login();
        try {
            entryBean.whoAmI();
            fail("Expected EJBAccessException due to bad password not thrown. (EJB 3.1 FR 17.6.9)");
        } catch (EJBAccessException ignored) {
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testAuthentication_TwoBeans() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            String[] response = entryBean.doubleWhoAmI();
            assertEquals("user1", response[0]);
            assertEquals("user1", response[1]);
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testAuthentication_TwoBeans_ReAuth() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            String[] response = entryBean.doubleWhoAmI("user2", "password2");
            assertEquals("user1", response[0]);
            assertEquals("user2", response[1]);
        } finally {
            lc.logout();
        }
    }

    // TODO - Similar test with first bean @RunAs - does it make sense to also manually switch?
    @Test
    public void testAuthentication_TwoBeans_ReAuth_BadPwd() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            entryBean.doubleWhoAmI("user2", "wrong_password");
            fail("Expected EJBAccessException due to bad password not thrown. (EJB 3.1 FR 17.6.9)");
        } catch (EJBAccessException ignored) {
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testAuthenticatedCall() throws Exception {
        // TODO: this is not spec
        final SecurityClient client = SecurityClientFactory.getSecurityClient();
        client.setSimple("user1", "password1");
        client.login();
        try {
            try {
                final Principal principal = whoAmIBean.getCallerPrincipal();
                assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                        principal);
                assertEquals("user1", principal.getName());
            } catch (RuntimeException e) {
                e.printStackTrace();
                fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                        + e.getMessage() + ")");
            }
        } finally {
            client.logout();
        }
    }

    @Test
    public void testUnauthenticated() throws Exception {
        try {
            final Principal principal = whoAmIBean.getCallerPrincipal();
            assertNotNull("EJB 3.1 FR 17.6.5 The container must never return a null from the getCallerPrincipal method.",
                    principal);
            // TODO: where is 'anonymous' configured?
            assertEquals("anonymous", principal.getName());
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("EJB 3.1 FR 17.6.5 The EJB container must provide the caller’s security context information during the execution of a business method ("
                    + e.getMessage() + ")");
        }
    }

    @Test
    public void testAuthentication_ViaServlet() throws Exception {
        final String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=whoAmI", "user1", "password1",
                10, SECONDS);
        assertEquals("user1", result);
    }

    @Test
    public void testAuthentication_ReAuth_ViaServlet() throws Exception {
        final String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=whoAmI&username=user2&password=password2", "user1",
                "password1", 10, SECONDS);
        assertEquals("user2", result);
    }

    @Test
    public void testAuthentication_TwoBeans_ViaServlet() throws Exception {
        final String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleWhoAmI", "user1",
                "password1", 10, SECONDS);
        assertEquals("user1,user1", result);
    }

    @Test
    public void testAuthentication_TwoBeans_ReAuth_ViaServlet() throws Exception {
        final String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleWhoAmI&username=user2&password=password2", "user1",
                "password1", 10, SECONDS);
        assertEquals("user1,user2", result);
    }

    @Test
    public void testAuthentication_TwoBeans_ReAuth__BadPwd_ViaServlet() throws Exception {
        try {
            HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleWhoAmI&username=user2&password=bad_password",
                    "user1", "password1", 10, SECONDS);
            fail("Expected IOException");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("javax.ejb.EJBAccessException"));
        }
    }

    /*
     * isCallerInRole Scenarios
     */

    @Test
    public void testICIRSingle() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            assertTrue(entryBean.doIHaveRole("Users"));
            assertTrue(entryBean.doIHaveRole("Role1"));
            assertFalse(entryBean.doIHaveRole("Role2"));
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testICIR_TwoBeans() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            boolean[] response;
            response = entryBean.doubleDoIHaveRole("Users");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role1");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role2");
            assertFalse(response[0]);
            assertFalse(response[1]);
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testICIR_TwoBeans_ReAuth() throws Exception {
        LoginContext lc = Util.getCLMLoginContext("user1", "password1");
        lc.login();
        try {
            boolean[] response;
            response = entryBean.doubleDoIHaveRole("Users", "user2", "password2");
            assertTrue(response[0]);
            assertTrue(response[1]);

            response = entryBean.doubleDoIHaveRole("Role1", "user2", "password2");
            assertTrue(response[0]);
            assertFalse(response[1]);

            response = entryBean.doubleDoIHaveRole("Role2", "user2", "password2");
            assertFalse(response[0]);
            assertTrue(response[1]);
        } finally {
            lc.logout();
        }
    }

    @Test
    public void testICIR_ViaServlet() throws Exception {
        String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doIHaveRole&role=Users", "user1",
                "password1", 10, SECONDS);
        assertEquals("true", result);
        result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doIHaveRole&role=Role1", "user1",
                "password1", 10, SECONDS);
        assertEquals("true", result);
        result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doIHaveRole&role=Role2", "user1",
                "password1", 10, SECONDS);
        assertEquals("false", result);
    }

    @Test
    public void testICIR_ReAuth_ViaServlet() throws Exception {
        String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doIHaveRole&role=Users&username=user2&password=password2",
                "user1", "password1", 10, SECONDS);
        assertEquals("true", result);
        result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doIHaveRole&role=Role1&username=user2&password=password2",
                "user1", "password1", 10, SECONDS);
        assertEquals("false", result);
        result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doIHaveRole&role=Role2&username=user2&password=password2",
                "user1", "password1", 10, SECONDS);
        assertEquals("true", result);
    }

    @Test
    public void testICIR_TwoBeans_ViaServlet() throws Exception {
        String result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleDoIHaveRole&role=Users",
                "user1", "password1", 10, SECONDS);
        assertEquals("true,true", result);
        result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleDoIHaveRole&role=Role1", "user1",
                "password1", 10, SECONDS);
        assertEquals("true,true", result);
        result = HttpRequest.get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleDoIHaveRole&role=Role2", "user1",
                "password1", 10, SECONDS);
        assertEquals("false,false", result);
    }

    @Test
    public void testICIR_TwoBeans_ReAuth_ViaServlet() throws Exception {
        String result = HttpRequest
                .get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleDoIHaveRole&role=Users&username=user2&password=password2",
                        "user1", "password1", 10, SECONDS);
        assertEquals("true,true", result);
        result = HttpRequest
                .get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleDoIHaveRole&role=Role1&username=user2&password=password2",
                        "user1", "password1", 10, SECONDS);
        assertEquals("true,false", result);
        result = HttpRequest
                .get(managementClient.getWebUri() + "/ejb3security/whoAmI?method=doubleDoIHaveRole&role=Role2&username=user2&password=password2",
                        "user1", "password1", 10, SECONDS);
        assertEquals("false,true", result);
    }

    /*
     * isCallerInRole Scenarios with @RunAs Defined
     *
     * EJB 3.1 FR 17.2.5.2 isCallerInRole tests the principal that represents the caller of the enterprise bean, not the
     * principal that corresponds to the run-as security identity for the bean.
     */

    // 17.2.5 - Programatic Access to Caller's Security Context
    // Include tests for methods not implemented to pick up if later they are implemented.
    // 17.2.5.1 - Use of getCallerPrincipal
    // 17.6.5 - Security Methods on EJBContext
    // 17.2.5.2 - Use of isCallerInRole
    // 17.2.5.3 - Declaration of Security Roles Referenced from the Bean's Code
    // 17.3.1 - Security Roles
    // 17.3.2.1 - Specification of Method Permissions with Metadata Annotation
    // 17.3.2.2 - Specification of Method Permissions in the Deployment Descriptor
    // 17.3.2.3 - Unspecified Method Permission
    // 17.3.3 - Linking Security Role References to Security Roles
    // 17.3.4 - Specification on Security Identities in the Deployment Descriptor
    // (Include permutations for overrides esp where deployment descriptor removes access)
    // 17.3.4.1 - Run-as
    // 17.5 EJB Client Responsibilities
    // A transactional client can not change principal association within transaction.
    // A session bean client must not change the principal association for the duration of the communication.
    // If transactional requests within a single transaction arrive from multiple clients all must be associated
    // with the same security context.

    // 17.6.3 - Security Mechanisms
    // 17.6.4 - Passing Principals on EJB Calls
    // 17.6.6 - Secure Access to Resource Managers
    // 17.6.7 - Principal Mapping
    // 17.6.9 - Runtime Security Enforcement
    // 17.6.10 - Audit Trail
}
