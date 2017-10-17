/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.manual.elytron.seccontext;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.jboss.as.test.integration.security.common.Utils.REDIRECT_STRATEGY;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.JAR_ENTRY_EJB;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER1;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER1_BACKUP;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER2;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BASIC;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_BEARER_TOKEN;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_ENTRY_SERVLET_FORM;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.WAR_WHOAMI;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.SocketPermission;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

import javax.ejb.EJBAccessException;
import javax.naming.Context;
import javax.naming.InitialContext;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandLineException;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.CLIOpResult;
import org.jboss.as.test.integration.management.util.CLITestUtil;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.management.util.ServerReload;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.permission.RunAsPrincipalPermission;
import org.wildfly.security.credential.BearerTokenCredential;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * Tests for testing (re)authentication and security identity propagation between servers. Test scenarios use following
 * configuration.
 * <h3>Given</h3>
 *
 * <pre>
 * EJBs used for testing:
 * - WhoAmIBean & WhoAmIBeanSFSB - protected (whoami and admin roles are allowed), just returns caller principal
 * - EntryBean & EntryBeanSFSB - protected (entry and admin roles are allowed), configures identity propagation and calls a remote WhoAmIBean
 *
 * Servlets used for testing:
 * - WhoAmIServlet - protected (servlet and admin roles are allowed) - just returns name of the incoming user name
 * - EntryServlet - protecte (servlet and admin roles are allowed) - configures identity propagation and calls a remote WhoAmIBean
 *
 * Deployments used for testing:
 * - entry-ejb.jar (EntryBean & EntryBeanSFSB)
 * - whoami.war (WhoAmIBean & WhoAmIBeanSFSB, WhoAmIServlet)
 * - entry-servlet-basic.war (EntryServlet, WhoAmIServlet) - authentication mechanism BASIC
 * - entry-servlet-form.war (EntryServlet, WhoAmIServlet) - authentication mechanism FORM
 * - entry-servlet-bearer.war (EntryServlet, WhoAmIServlet) - authentication mechanism BEARER_TOKEN
 *
 *
 * Servers started and configured for context propagation scenarios:
 * - seccontext-server1 (standalone-ha.xml)
 *   * entry-ejb.jar
 *   * entry-servlet-basic.war
 *   * entry-servlet-form.war
 *   * entry-servlet-bearer.war
 * - seccontext-server1-backup (standalone-ha.xml - creates cluster with seccontext-server1) -
 *   * entry-servlet-form.war
 * - seccontext-server2 (standalone-ha.xml)
 *   * whoami.war
 *
 * Users used for testing (username==password==role):
 * - entry
 * - whoami
 * - servlet
 * - admin
 * - server (has configured additional permission - RunAsPrincipalPermission)
 * - server-norunas
 * </pre>
 *
 * @see ReAuthnType reauthentication types
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractSecurityContextPropagationTestBase {

    private static final Logger LOGGER = Logger.getLogger(AbstractSecurityContextPropagationTestBase.class);

    private static final ServerHolder server1 = new ServerHolder(SERVER1, TestSuiteEnvironment.getServerAddress(), 0);
    private static final ServerHolder server1backup = new ServerHolder(SERVER1_BACKUP, TestSuiteEnvironment.getServerAddress(),
            2000);
    private static final ServerHolder server2 = new ServerHolder(SERVER2, TestSuiteEnvironment.getServerAddressNode1(), 100);

    private static final Package PACKAGE = AbstractSecurityContextPropagationTestBase.class.getPackage();

    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String JWT_HEADER_B64 = B64_ENCODER
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    @ArquillianResource
    private static volatile ContainerController containerController;

    @ArquillianResource
    private static volatile Deployer deployer;

    /**
     * Creates deployment with Entry bean - to be placed on the first server.
     */
    @Deployment(name = JAR_ENTRY_EJB, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryBeanDeployment() {
        return ShrinkWrap.create(JavaArchive.class, JAR_ENTRY_EJB + ".jar")
                .addClasses(EntryBean.class, EntryBeanSFSB.class, Entry.class, WhoAmI.class, ReAuthnType.class,
                        SeccontextUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getPrivateCredentials"), new ElytronPermission("getSecurityDomain"),
                        new SocketPermission(TestSuiteEnvironment.getServerAddressNode1() + ":8180", "connect,resolve")),
                        "permissions.xml")
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset("seccontext-entry"), "jboss-ejb3.xml");
    }

    /**
     * Creates deployment with Entry servlet and BASIC authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_BASIC, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryServletBasicAuthnDeployment() {
        return createEntryServletDeploymentBase(WAR_ENTRY_SERVLET_BASIC)
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml");
    }

    /**
     * Creates deployment with Entry servlet and FORM authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_FORM, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryServletFormAuthnDeployment() {
        return createEntryServletDeploymentBase(WAR_ENTRY_SERVLET_FORM)
                .addAsWebInfResource(PACKAGE, "web-form-authn.xml", "web.xml")
                .addAsWebResource(PACKAGE, "login.html", "login.html").addAsWebResource(PACKAGE, "error.html", "error.html");
    }

    /**
     * Creates deployment with Entry servlet and FORM authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_FORM + "backup", managed = false, testable = false)
    @TargetsContainer(SERVER1_BACKUP)
    public static Archive<?> createDeploymentForBackup() {
        return createEntryServletFormAuthnDeployment();
    }

    /**
     * Creates deployment with Entry servlet and BEARER authentication.
     */
    @Deployment(name = WAR_ENTRY_SERVLET_BEARER_TOKEN, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryServletBearerAuthnDeployment() {
        return createEntryServletDeploymentBase(WAR_ENTRY_SERVLET_BEARER_TOKEN).addAsWebInfResource(PACKAGE,
                "web-token-authn.xml", "web.xml");
    }

    /**
     * Creates deployment with WhoAmI bean and servlet - to be placed on the second server.
     */
    @Deployment(name = WAR_WHOAMI, managed = false, testable = false)
    @TargetsContainer(SERVER2)
    public static Archive<?> createEjbClientDeployment() {
        return ShrinkWrap.create(WebArchive.class, WAR_WHOAMI + ".war")
                .addClasses(WhoAmIBean.class, WhoAmIBeanSFSB.class, WhoAmI.class, WhoAmIServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("seccontext-web"), "jboss-web.xml")
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml")
                .addAsWebInfResource(Utils.getJBossEjb3XmlAsset("seccontext-whoami"), "jboss-ejb3.xml");
    }

    /**
     * Start servers (if not yet started) and if it's the first execution it sets configuration of test servers and deploys test
     * applications.
     */
    @Before
    public void before() throws CommandLineException, IOException, MgmtOperationException {
        server1.resetContainerConfiguration(JAR_ENTRY_EJB, WAR_ENTRY_SERVLET_BASIC, WAR_ENTRY_SERVLET_FORM,
                WAR_ENTRY_SERVLET_BEARER_TOKEN);
        server1backup.resetContainerConfiguration(WAR_ENTRY_SERVLET_FORM + "backup");
        server2.resetContainerConfiguration(WAR_WHOAMI);
    }

    /**
     * Shut down servers.
     */
    @AfterClass
    public static void afterClass() throws IOException {
        server1.shutDown();
        server1backup.shutDown();
        server2.shutDown();
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
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "entry", "whoami" },
                doubleWhoAmI);
    }

    /**
     * Test that {@link InitialContext} properties (principal+credentials) takes priority over the Elytron authentication
     * configuration.
     *
     * <pre>
     * When: EJB client calls WhoAmIBean using both Elytron AuthenticationContext API InitialContext properties to set
     *       username/password combination
     * Then: username/password combination from InitialContext is used
     * </pre>
     */
    @Test
    public void testInitialContextPropertiesOverride() throws Exception {
        // Let's call the WhoAmIBean with different username+password combinations in Elytron API and InitialContext properties
        Callable<String> callable = () -> {
            final Properties jndiProperties = new Properties();
            jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
            jndiProperties.put(Context.PROVIDER_URL, server2.getApplicationRemotingUrl());
            jndiProperties.put(Context.SECURITY_PRINCIPAL, "whoami");
            jndiProperties.put(Context.SECURITY_CREDENTIALS, "whoami");
            final Context context = new InitialContext(jndiProperties);

            final WhoAmI bean = (WhoAmI) context.lookup(
                    SeccontextUtil.getRemoteEjbName(WAR_WHOAMI, "WhoAmIBean", WhoAmI.class.getName(), isWhoAmIStateful()));
            return bean.getCallerPrincipal().getName();
        };
        // Elytron API uses "entry" user, the InitialContext uses "whoami"
        String whoAmI = SeccontextUtil.switchIdentity("entry", "entry", callable, ReAuthnType.AC_AUTHENTICATION);
        // The identity should be created from InitialContext properties
        assertEquals("The whoAmIBean.whoAmI() returned unexpected principal", "whoami", whoAmI);
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
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "admin", "admin" },
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
     * Test the authorization forwarding (credential less propagation) works for EJB calls when {@link RunAsPrincipalPermission}
     * is assigned to caller server identity.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "server" user used as caller server identity
     * Then: WhoAmIBean call is possible and returns "admin" username
     * </pre>
     */
    @Test
    public void testForwardedAuthorizationPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server", "server"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "admin", "admin" },
                doubleWhoAmI);
    }

    /**
     * Test the authorization forwarding works for EJB calls when {@link RunAsPrincipalPermission} is not assigned to the caller
     * identity, but the authentication identity == authorization identity (which has sufficient roles to call the EJB).
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with "admin" user used as caller server identity.
     * Then: WhoAmIBean call is possible and returns "admin" username
     * </pre>
     */
    @Test
    public void testSameAuthorizationIdentityPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "admin", "admin"), ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "admin", "admin" },
                doubleWhoAmI);
    }

    /**
     * Test the authorization forwarding fails for EJB calls when {@link RunAsPrincipalPermission} is not assigned to the caller
     * identity.
     *
     * <pre>
     * When: EJB client calls EntryBean as admin user and Elytron AuthenticationContext API is used to
     *       authorization forwarding to WhoAmIBean call with either "server-norunas" or "whoami" users
     *       used as caller server identity.
     * Then: WhoAmIBean call fails in both cases as the server identity don't have RunAsPrincipalPermission
     * </pre>
     */
    @Test
    public void testForwardedAuthorizationIdentityWithoutRunAsFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "server-norunas", "server-norunas"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());

        doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_AUTHORIZATION, "whoami", "whoami"),
                ReAuthnType.AC_AUTHENTICATION);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
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
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "entry", "whoami" },
                doubleWhoAmI);
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
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "admin", "admin" },
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

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
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

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet", "servlet",
                    doHttpRequestFormAuthn(httpClient, whoAmIServletUrl, true, "servlet", "servlet", SC_OK));
            assertThat("Unexpected result from EntryServlet", doHttpRequest(httpClient, entryServletUrl, SC_OK),
                    isEjbAccessException());
        }
    }

    /**
     * Verifies, the distributable web-app with FORM authentication supports SSO out of the box.
     *
     * <pre>
     * When: HTTP client calls WhoAmIServlet as "admin" (using FORM authn) on first cluster node and then
     *       it calls WhoAmIServlet (without authentication needed) on the second cluster node
     * Then: the call to WhoAmIServlet on second node (without authentication) passes and returns "admin"
     *       (i.e. SSO works with FORM authentication)
     * </pre>
     */
    @Test
    public void testServletSso() throws Exception {
        final URL whoamiUrl = new URL(
                server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);
        final URL whoamiBackupUrl = new URL(
                server1backup.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet", "admin",
                    doHttpRequestFormAuthn(httpClient, whoamiUrl, true, "admin", "admin", SC_OK));
            assertEquals("Unexpected result from WhoAmIServlet (backup-server)", "admin",
                    doHttpRequest(httpClient, whoamiBackupUrl, SC_OK));
        }
    }

    /**
     * Verifies, the credential forwarding works within clustered SSO (FORM authentication). This simulates failover on
     * distributed web application (e.g. when load balancer is used).
     *
     * <pre>
     * When: HTTP client calls WhoAmIServlet as "admin" (using FORM authn) on second cluster node and then
     *       it calls EntryServlet (without authentication needed) on the first cluster node;
     *       the EntryServlet uses Elytron API to forward authentication (credentials) to call remote WhoAmIBean
     * Then: the calls pass and WhoAmIBean returns "admin" username
     * </pre>
     */
    @Test
    @Ignore("JBEAP-13217")
    public void testServletSsoPropagation() throws Exception {
        final URL entryServletUrl = getEntryServletUrl(WAR_ENTRY_SERVLET_FORM, null, null,
                ReAuthnType.FORWARDED_AUTHENTICATION);
        final URL whoamiUrl = new URL(
                server1backup.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + WhoAmIServlet.SERVLET_PATH);

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            assertEquals("Unexpected result from WhoAmIServlet (backup-server)", "admin",
                    doHttpRequestFormAuthn(httpClient, whoamiUrl, true, "admin", "admin", SC_OK));
            assertEquals("Unexpected result from EntryServlet", "admin", doHttpRequest(httpClient, entryServletUrl, SC_OK));
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

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
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

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
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

        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            final String jwtToken = createJwtToken("admin");
            assertEquals("Unexpected result from WhoAmIServlet", "admin",
                    doHttpRequestTokenAuthn(httpClient, whoAmIServletUrl, jwtToken, SC_OK));
            assertEquals("Unexpected result from EntryServlet", "admin",
                    doHttpRequestTokenAuthn(httpClient, entryServletUrl, jwtToken, SC_OK));
        }

        // do the call without sufficient role in EJB (server2)
        try (final CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(REDIRECT_STRATEGY).build()) {
            final String jwtToken = createJwtToken("servlet");
            assertThat("Unexpected result from EntryServlet",
                    doHttpRequestTokenAuthn(httpClient, entryServletUrl, jwtToken, SC_OK), isEjbAccessException());
            assertEquals("Unexpected result from WhoAmIServlet", "servlet",
                    doHttpRequestTokenAuthn(httpClient, whoAmIServletUrl, jwtToken, SC_OK));
        }
    }

    /**
     * Test identity forwarding for HttpURLConnection calls.
     */
    @Test
    @Ignore("JBEAP-12340")
    public void testHttpPropagation() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.FORWARDED_AUTHENTICATION, null, null);
        String servletResponse = SeccontextUtil.switchIdentity("admin", "admin", callable, ReAuthnType.AC_AUTHENTICATION);
        assertEquals("Unexpected principal name returned from servlet call", "admin", servletResponse);
    }

    /**
     * Tests if re-authentication works for HttpURLConnection calls.
     */
    @Test
    @Ignore("JBEAP-12340")
    public void testHttpReauthn() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AC_AUTHENTICATION, "servlet", "servlet");
        String servletResponse = SeccontextUtil.switchIdentity("admin", "admin", callable, ReAuthnType.AC_AUTHENTICATION);
        assertEquals("Unexpected principal name returned from servlet call", "servlet", servletResponse);
    }

    /**
     * Tests propagation when user propagated to HttpURLConnection has insufficient roles.
     */
    @Test
    @Ignore("JBEAP-12340")
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

    /**
     * Returns true if the stateful Entry bean variant should be used by the tests. False otherwise.
     */
    protected abstract boolean isEntryStateful();

    /**
     * Returns true if the stateful WhoAmI bean variant should be used by the tests. False otherwise.
     */
    protected abstract boolean isWhoAmIStateful();

    /**
     * Do HTTP GET request with given client.
     *
     * @param httpClient
     * @param url
     * @param expectedStatus expected status coe
     * @return response body
     */
    private String doHttpRequest(final CloseableHttpClient httpClient, final URL url, int expectedStatus)
            throws URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {
        return doHttpRequestFormAuthn(httpClient, url, false, null, null, expectedStatus);
    }

    /**
     * Do HTTP request using given client with possible FORM authentication.
     *
     * @param httpClient client instance
     * @param url URL to make request to
     * @param loginFormExpected flag which says if login (FORM) is expected, if true username and password arguments are used to
     *        login.
     * @param username user to fill into the login form
     * @param password password to fill into the login form
     * @param expectedStatus expected status code
     * @return response body
     */
    private String doHttpRequestFormAuthn(final CloseableHttpClient httpClient, final URL url, boolean loginFormExpected,
            String username, String password, int expectedStatus)
            throws URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {
        HttpGet httpGet = new HttpGet(url.toURI());

        HttpResponse response = httpClient.execute(httpGet);

        HttpEntity entity = response.getEntity();
        assertNotNull(entity);
        String responseBody = EntityUtils.toString(entity);
        if (loginFormExpected) {
            assertThat("Login page was expected", responseBody, containsString("j_security_check"));
            assertEquals("HTTP OK response for login page was expected", SC_OK, response.getStatusLine().getStatusCode());

            // We should now login with the user name and password
            HttpPost httpPost = new HttpPost(
                    server1.getApplicationHttpUrl() + "/" + WAR_ENTRY_SERVLET_FORM + "/j_security_check");

            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("j_username", username));
            nvps.add(new BasicNameValuePair("j_password", password));

            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));

            response = httpClient.execute(httpPost);
            entity = response.getEntity();
            assertNotNull(entity);
            responseBody = EntityUtils.toString(entity);
        } else {
            assertThat("Login page was not expected", responseBody, not(containsString("j_security_check")));
        }
        assertEquals("Unexpected status code", expectedStatus, response.getStatusLine().getStatusCode());
        return responseBody;
    }

    /**
     * Do HTTP request using given client with BEARER_TOKEN authentication. The implementation makes 2 calls - the first without
     * Authorization header provided (just to check response code and WWW-Authenticate header value), the second with
     * Authorization header.
     *
     * @param httpClient client instance
     * @param url URL to make request to
     * @param token bearer token
     * @param expectedStatus expected status code
     * @return response body
     */
    private String doHttpRequestTokenAuthn(final CloseableHttpClient httpClient, final URL url, String token,
            int expectedStatusCode)
            throws URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {
        final HttpGet httpGet = new HttpGet(url.toURI());

        HttpResponse response = httpClient.execute(httpGet);
        assertEquals("Unexpected HTTP response status code.", SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
        Header[] authenticateHeaders = response.getHeaders("WWW-Authenticate");
        assertTrue("Expected WWW-Authenticate header was not present in the HTTP response",
                authenticateHeaders != null && authenticateHeaders.length > 0);
        boolean bearerAuthnHeaderFound = false;
        for (Header header : authenticateHeaders) {
            final String headerVal = header.getValue();
            if (headerVal != null && headerVal.startsWith("Bearer")) {
                bearerAuthnHeaderFound = true;
                break;
            }
        }
        assertTrue("WWW-Authenticate response header didn't request expected Bearer token authentication",
                bearerAuthnHeaderFound);
        HttpEntity entity = response.getEntity();
        if (entity != null)
            EntityUtils.consume(entity);

        httpGet.addHeader("Authorization", "Bearer " + token);
        response = httpClient.execute(httpGet);
        assertEquals("Unexpected status code returned after the authentication.", expectedStatusCode,
                response.getStatusLine().getStatusCode());
        return EntityUtils.toString(response.getEntity());
    }

    /**
     * Creates callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as "whoami" user.
     *
     * @param type reauthentication reauthentication type used within the doubleWhoAmI
     * @return Callable
     */
    private Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type) {
        return getDoubleWhoAmICallable(type, "whoami", "whoami");
    }

    /**
     * Creates a callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as given user.
     *
     * @param type reauthentication re-authentication type used within the doubleWhoAmI
     * @param username
     * @param password
     * @return
     */
    private Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type, final String username, final String password) {
        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(JAR_ENTRY_EJB, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String server2Url = server2.getApplicationRemotingUrl();
            return bean.doubleWhoAmI(username, password, type, server2Url, isWhoAmIStateful());
        };
    }

    private Callable<String> getEjbToServletCallable(final ReAuthnType type, final String username, final String password) {
        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(JAR_ENTRY_EJB, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String servletUrl = server2.getApplicationHttpUrl() + "/" + WAR_WHOAMI + WhoAmIServlet.SERVLET_PATH;
            return bean.readUrl(username, password, type, new URL(servletUrl));
        };
    }

    private static org.hamcrest.Matcher<java.lang.String> isEjbAuthenticationError() {
        // different behavior for stateless and stateful beans
        // is reported under https://issues.jboss.org/browse/JBEAP-12439
        return anyOf(startsWith("javax.ejb.NoSuchEJBException: EJBCLIENT000079"),
                startsWith("javax.naming.CommunicationException: EJBCLIENT000062"), containsString("JBREM000308"),
                containsString("javax.security.sasl.SaslException: Authentication failed"));
    }

    private static org.hamcrest.Matcher<java.lang.String> isEvidenceVerificationError() {
        return startsWith("java.lang.SecurityException: ELY01151");
    }

    private static org.hamcrest.Matcher<java.lang.String> isEjbAccessException() {
        return startsWith("javax.ejb.EJBAccessException");
    }

    private String createJwtToken(String userName) {
        String jwtPayload = String.format("{" //
                + "\"iss\": \"issuer.wildfly.org\"," //
                + "\"sub\": \"elytron@wildfly.org\"," //
                + "\"exp\": 2051222399," //
                + "\"aud\": \"%1$s\"," //
                + "\"groups\": [\"%1$s\"]" //
                + "}", userName);
        return JWT_HEADER_B64 + "." + B64_ENCODER.encodeToString(jwtPayload.getBytes(StandardCharsets.UTF_8)) + ".";
    }

    private URL getEntryServletUrl(String warName, String username, String password, ReAuthnType type) throws IOException {
        final StringBuilder sb = new StringBuilder(server1.getApplicationHttpUrl() + "/" + warName + EntryServlet.SERVLET_PATH);
        addQueryParam(sb, EntryServlet.PARAM_USERNAME, username);
        addQueryParam(sb, EntryServlet.PARAM_PASSWORD, password);
        addQueryParam(sb, EntryServlet.PARAM_STATEFULL, String.valueOf(isWhoAmIStateful()));
        addQueryParam(sb, EntryServlet.PARAM_CREATE_SESSION, String.valueOf(true));
        addQueryParam(sb, EntryServlet.PARAM_REAUTHN_TYPE, type.name());
        addQueryParam(sb, EntryServlet.PARAM_PROVIDER_URL, server2.getApplicationRemotingUrl());
        return new URL(sb.toString());
    }

    private static void addQueryParam(StringBuilder sb, String paramName, String paramValue) {
        final String encodedPair = Utils.encodeQueryParam(paramName, paramValue);
        if (encodedPair != null) {
            sb.append(sb.indexOf("?") < 0 ? "?" : "&").append(encodedPair);
        }
    }

    /**
     * Creates deployment base with Entry servlet. It doesn't contain web.xml and related resources if needed (e.g. login page).
     */
    private static WebArchive createEntryServletDeploymentBase(String name) {
        return ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(EntryServlet.class, WhoAmIServlet.class, WhoAmI.class, ReAuthnType.class, SeccontextUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getPrivateCredentials"), new ElytronPermission("getSecurityDomain"),
                        new SocketPermission(TestSuiteEnvironment.getServerAddressNode1() + ":8180", "connect,resolve")),
                        "permissions.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("seccontext-web"), "jboss-web.xml");
    }

    private static class ServerHolder {
        private final String name;
        private final String host;
        private final int portOffset;
        private volatile ModelControllerClient client;
        private volatile CommandContext commandCtx;
        private volatile ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();

        private volatile String snapshot;

        public ServerHolder(String name, String host, int portOffset) {
            this.name = name;
            this.host = host;
            this.portOffset = portOffset;
        }

        public void resetContainerConfiguration(String... deployments)
                throws CommandLineException, IOException, MgmtOperationException {
            if (!containerController.isStarted(name)) {
                containerController.start(name);
                client = ModelControllerClient.Factory.create(host, getManagementPort());
                commandCtx = CLITestUtil.getCommandContext(host, getManagementPort(), null, consoleOut, -1);
                commandCtx.connectController();
                readSnapshot();

                if (snapshot == null) {
                    // configure each server just once
                    createPropertyFile();
                    final File cliFIle = File.createTempFile("seccontext-", ".cli");
                    try (FileOutputStream fos = new FileOutputStream(cliFIle)) {
                        IOUtils.copy(
                                AbstractSecurityContextPropagationTestBase.class.getResourceAsStream("seccontext-setup.cli"),
                                fos);
                    }
                    runBatch(cliFIle);
                    switchJGroupsToTcpping();
                    cliFIle.delete();
                    reload();

                    for (String deployment : deployments) {
                        deployer.deploy(deployment);
                    }

                    takeSnapshot();
                }
            }
        }

        public void shutDown() throws IOException {
            if (containerController.isStarted(name)) {
                // deployer.undeploy(name);
                commandCtx.terminateSession();
                client.close();
                containerController.stop(name);
            }
        }

        public int getManagementPort() {
            return 9990 + portOffset;
        }

        public int getApplicationPort() {
            return 8080 + portOffset;
        }

        public String getApplicationHttpUrl() throws IOException {
            return "http://" + NetworkUtils.formatPossibleIpv6Address(host) + ":" + getApplicationPort();
        }

        public String getApplicationRemotingUrl() throws IOException {
            return "remote+" + getApplicationHttpUrl();
        }

        /**
         * Sends command line to CLI.
         *
         * @param line specifies the command line.
         * @param ignoreError if set to false, asserts that handling the line did not result in a
         *        {@link org.jboss.as.cli.CommandLineException}.
         *
         * @return true if the CLI is in a non-error state following handling the line
         */
        public boolean sendLine(String line, boolean ignoreError) {
            consoleOut.reset();
            if (ignoreError) {
                commandCtx.handleSafe(line);
                return commandCtx.getExitCode() == 0;
            } else {
                try {
                    commandCtx.handle(line);
                } catch (CommandLineException e) {
                    StringWriter stackTrace = new StringWriter();
                    e.printStackTrace(new PrintWriter(stackTrace));
                    Assert.fail(String.format("Failed to execute line '%s'%n%s", line, stackTrace.toString()));
                }
            }
            return true;
        }

        /**
         * Runs given CLI script file as a batch.
         *
         * @param batchFile CLI file to run in batch
         * @return true if CLI returns Success
         */
        public boolean runBatch(File batchFile) throws IOException {
            sendLine("run-batch --file=\"" + batchFile.getAbsolutePath() + "\" -v", false);
            if (consoleOut.size() <= 0) {
                return false;
            }
            return new CLIOpResult(ModelNode.fromStream(new ByteArrayInputStream(consoleOut.toByteArray())))
                    .isIsOutcomeSuccess();
        }

        /**
         * Switch JGroups subsystem (if present) from using UDP multicast to TCPPING discovery protocol.
         */
        private void switchJGroupsToTcpping() throws IOException {
            consoleOut.reset();
            try {
                commandCtx.handle("if outcome==success of /subsystem=jgroups:read-resource()");
                commandCtx.handle(String.format(
                        "/subsystem=jgroups/stack=tcp/protocol=TCPPING:add(add-index=0, properties={initial_hosts=\"%1$s[7600],%1$s[9600]\",port_range=0,timeout=3000})",
                        Utils.stripSquareBrackets(host)));
                commandCtx.handle("/subsystem=jgroups/stack=tcp/protocol=MPING:remove");
                commandCtx.handle("/subsystem=jgroups/channel=ee:write-attribute(name=stack,value=tcp)");
                commandCtx.handle("end-if");
            } catch (CommandLineException e) {
                LOGGER.error("Command line error occured during JGroups reconfiguration", e);
            } finally {
                LOGGER.debugf("Output of JGroups reconfiguration (switch to TCPPING): %s",
                        new String(consoleOut.toByteArray()));
            }
        }

        private void takeSnapshot() throws IOException, MgmtOperationException {
            DomainTestUtils.executeForResult(Util.createOperation("take-snapshot", null), client);
            readSnapshot();
        }

        private void readSnapshot() throws IOException, MgmtOperationException {
            ModelNode namesNode = DomainTestUtils.executeForResult(Util.createOperation("list-snapshots", null), client)
                    .get("names");
            if (namesNode == null || namesNode.getType() != ModelType.LIST) {
                throw new IllegalStateException("Unexpected return value from :list-snaphot operation: " + namesNode);
            }
            List<ModelNode> snapshots = namesNode.asList();
            if (!snapshots.isEmpty()) {
                snapshot = namesNode.get(snapshots.size() - 1).asString();
            }
        }

        private void reload() {
            ModelNode operation = Util.createOperation("reload", null);
            ServerReload.executeReloadAndWaitForCompletion(client, operation, (int) SECONDS.toMillis(90), host,
                    getManagementPort());
        }

        /**
         * Create single property file with users and/or roles in standalone server config directory. It will be used for
         * property-realm configuration (see {@code seccontext-setup.cli} script)
         */
        private void createPropertyFile() throws IOException {
            sendLine("/core-service=platform-mbean/type=runtime:read-attribute(name=system-properties)", false);
            assertTrue(consoleOut.size() > 0);
            ModelNode node = ModelNode.fromStream(new ByteArrayInputStream(consoleOut.toByteArray()));
            String configDirPath = node.get(ModelDescriptionConstants.RESULT).get("jboss.server.config.dir").asString();
            Files.write(Paths.get(configDirPath, "seccontext.properties"),
                    Utils.createUsersFromRoles("admin", "servlet", "entry", "whoami", "server", "server-norunas")
                            .getBytes(StandardCharsets.ISO_8859_1));
        }
    }
}
