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
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.jboss.as.controller.client.helpers.ClientConstants.SERVER_CONFIG;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER1;
import static org.wildfly.test.manual.elytron.seccontext.SeccontextUtil.SERVER2;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.SocketPermission;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;
import java.util.concurrent.Callable;

import javax.ejb.EJBAccessException;

import org.apache.commons.io.IOUtils;
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
import org.wildfly.security.credential.BearerTokenCredential;
import org.wildfly.security.permission.ElytronPermission;
import org.wildfly.security.sasl.SaslMechanismSelector;

/**
 * Tests for testing (re)authentication and security identity propagation between 2 servers. Test scenarios use following
 * configuration:
 *
 * <pre>
 * EJB client -> Entry bean on server1 -> WhoAmI bean on server 2
 * EJB client -> Entry bean on server1 -> WhoAmI servlet on server 2
 * </pre>
 *
 * The Entry bean uses Elytron API to configure security context for outbound calls (e.g. identity forwarding, reauthentication,
 * ...).
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@RunAsClient
public abstract class AbstractSecurityContextPropagationTestBase {

    private static ServerHolder server1 = new ServerHolder(SERVER1, TestSuiteEnvironment.getServerAddress(), 0);
    private static ServerHolder server2 = new ServerHolder(SERVER2, TestSuiteEnvironment.getServerAddressNode1(), 100);

    private static final Encoder B64_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final String JWT_HEADER_B64 = B64_ENCODER
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    @ArquillianResource
    private static ContainerController containerController;

    @ArquillianResource
    private static Deployer deployer;

    /**
     * Creates deployment with Entry bean - to be placed on the first server.
     */
    @Deployment(name = SERVER1, managed = false, testable = false)
    @TargetsContainer(SERVER1)
    public static Archive<?> createEntryBeanDeployment() {
        return ShrinkWrap.create(JavaArchive.class, SERVER1 + ".jar")
                .addClasses(EntryBean.class, EntryBeanSFSB.class, Entry.class, WhoAmI.class, ReAuthnType.class,
                        SeccontextUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(new ElytronPermission("authenticate"),
                        new ElytronPermission("getPrivateCredentials"),
                        new ElytronPermission("getSecurityDomain"),
                        new SocketPermission(TestSuiteEnvironment.getServerAddressNode1()+":8180", "connect,resolve")
                        ),
                        "permissions.xml")
                .addAsManifestResource(Utils.getJBossEjb3XmlAsset("seccontext-entry"), "jboss-ejb3.xml");
    }

    /**
     * Creates deployment with WhoAmI bean and servlet - to be placed on the second server.
     */
    @Deployment(name = SERVER2, managed = false, testable = false)
    @TargetsContainer(SERVER2)
    public static Archive<?> createEjbClientDeployment() {
        return ShrinkWrap.create(WebArchive.class, SERVER2 + ".war")
                .addClasses(WhoAmIBean.class, WhoAmIBeanSFSB.class, WhoAmI.class, WhoAmIServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset("seccontext-web"), "jboss-web.xml")
                .addAsWebInfResource(new StringAsset(SecurityTestConstants.WEB_XML_BASIC_AUTHN), "web.xml")
                .addAsWebInfResource(Utils.getJBossEjb3XmlAsset("seccontext-whoami"), "jboss-ejb3.xml");
    }

    /**
     * Set or reset configuration of test servers.
     */
    @Before
    public void before() throws CommandLineException, IOException, MgmtOperationException {
        server1.resetContainerConfiguration();
        server2.resetContainerConfiguration();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        server1.shutDown();
        server2.shutDown();
    }

    @Test
    public void testAuthCtxPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.AUTHENTICATION_CONTEXT), ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "entry", "whoami" },
                doubleWhoAmI);
    }

    @Test
    public void testClientInsufficientRoles() throws Exception {
        try {
            SeccontextUtil.switchIdentity("whoami", "whoami", getDoubleWhoAmICallable(ReAuthnType.AUTHENTICATION_CONTEXT),
                    ReAuthnType.AUTHENTICATION_CONTEXT);
            fail("Calling Entry bean must fail when user without required roles is used");
        } catch (EJBAccessException e) {
            // OK - expected
        }
    }

    @Test
    public void testAuthCtxWrongUserFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.AUTHENTICATION_CONTEXT, "doesntexist", "whoami"),
                ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    @Test
    public void testAuthCtxWrongPasswdFail() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.AUTHENTICATION_CONTEXT, "whoami", "wrongpass"),
                ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    @Test
    public void testForwardedIdentityPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("admin", "admin",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_IDENTITY, null, null), ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "admin", "admin" },
                doubleWhoAmI);
    }

    @Test
    public void testForwardedIdentityInsufficientRolesFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.FORWARDED_IDENTITY, null, null), ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAccessException());
    }

    @Test
    public void testSecurityDomainAuthenticateWithoutForwarding() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SECURITY_DOMAIN_AUTHENTICATE), ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAuthenticationError());
    }

    @Test
    public void testSecurityDomainAuthenticateWrongPassFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SECURITY_DOMAIN_AUTHENTICATE, "doesntexist", "whoami"),
                ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEvidenceVerificationError());
    }

    @Test
    public void testSecurityDomainAuthenticateForwardedPasses() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SECURITY_DOMAIN_AUTHENTICATE_FORWARDED),
                ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "entry", "whoami" },
                doubleWhoAmI);
    }

    @Test
    public void testSecurityDomainAuthenticateForwardedWrongPasswordFails() throws Exception {
        String[] doubleWhoAmI = SeccontextUtil.switchIdentity("entry", "entry",
                getDoubleWhoAmICallable(ReAuthnType.SECURITY_DOMAIN_AUTHENTICATE_FORWARDED, "doesntexist", "whoami"),
                ReAuthnType.AUTHENTICATION_CONTEXT);
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEvidenceVerificationError());
    }

    @Test
    public void testOauthbearerPropagationPasses() throws Exception {
        String[] doubleWhoAmI = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                                .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("admin"))))
                .runCallable(getDoubleWhoAmICallable(ReAuthnType.FORWARDED_IDENTITY, null, null));
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertArrayEquals("Unexpected principal names returned from doubleWhoAmI", new String[] { "admin", "admin" },
                doubleWhoAmI);
    }

    @Test
    public void testOauthbearerPropagationInsufficientRolesFails() throws Exception {
        String[] doubleWhoAmI = AuthenticationContext.empty()
                .with(MatchRule.ALL,
                        AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                                .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("entry"))))
                .runCallable(getDoubleWhoAmICallable(ReAuthnType.FORWARDED_IDENTITY, null, null));
        assertNotNull("The entryBean.doubleWhoAmI() should return not-null instance", doubleWhoAmI);
        assertEquals("The result of doubleWhoAmI() has wrong lenght", 2, doubleWhoAmI.length);
        assertEquals("entry", doubleWhoAmI[0]);
        assertThat(doubleWhoAmI[1], isEjbAccessException());
    }

    @Test
    public void testClientOauthbearerInsufficientRolesFails() throws Exception {
        try {
            AuthenticationContext.empty()
                    .with(MatchRule.ALL,
                            AuthenticationConfiguration.empty().setSaslMechanismSelector(SaslMechanismSelector.ALL)
                                    .useBearerTokenCredential(new BearerTokenCredential(createJwtToken("whoami"))))
                    .runCallable(getDoubleWhoAmICallable(ReAuthnType.FORWARDED_IDENTITY, null, null));
            fail("Call to the protected bean should fail");
        } catch (EJBAccessException e) {
            // OK - expected
        }
    }

    /**
     * Test identity forwarding for HttpURLConnection calls.
     */
    @Test
    @Ignore("JBEAP-12340 JBEAP-12341")
    public void testHttpPropagation() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.FORWARDED_IDENTITY, null, null);
        String servletResponse = SeccontextUtil.switchIdentity("admin", "admin", callable, ReAuthnType.AUTHENTICATION_CONTEXT);
        assertEquals("Unexpected principal name returned from servlet call", "admin", servletResponse);
    }

    /**
     * Tests if re-authentication works for HttpURLConnection calls.
     */
    @Test
    @Ignore("JBEAP-12340 JBEAP-12341")
    public void testHttpReauthn() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AUTHENTICATION_CONTEXT, "servlet", "servlet");
        String servletResponse = SeccontextUtil.switchIdentity("admin", "admin", callable, ReAuthnType.AUTHENTICATION_CONTEXT);
        assertEquals("Unexpected principal name returned from servlet call", "servlet", servletResponse);
    }

    /**
     * Tests propagation when user propagated to HttpURLConnection has insufficient roles.
     */
    @Test
    @Ignore("JBEAP-12340 JBEAP-12341")
    public void testHttpReauthnInsufficientRoles() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AUTHENTICATION_CONTEXT, "whoami", "whoami");
        String servletResponse = SeccontextUtil.switchIdentity("entry", "entry", callable, ReAuthnType.AUTHENTICATION_CONTEXT);
        assertThat(servletResponse, allOf(startsWith("java.io.IOException"), containsString("403")));
    }

    /**
     * Tests propagation when user propagated to HttpURLConnection has insufficient roles.
     */
    @Test
    public void testHttpReauthnWrongPass() throws Exception {
        Callable<String> callable = getEjbToServletCallable(ReAuthnType.AUTHENTICATION_CONTEXT, "servlet", "whoami");
        String servletResponse = SeccontextUtil.switchIdentity("entry", "entry", callable, ReAuthnType.AUTHENTICATION_CONTEXT);
        assertThat(servletResponse, allOf(startsWith("java.io.IOException"), containsString("401")));
    }

    /**
     * Tests security context propagation behavior without isolation. This should be replaced in the future by removing reloads between the tests.
     */
    @Test
    @Ignore("JBEAP-12410")
    public void scenario1() throws Exception {
        testOauthbearerPropagationPasses();
        testSecurityDomainAuthenticateForwardedPasses();
    }

    /**
     * Tests security context propagation behavior without isolation. This should be replaced in the future by removing reloads between the tests.
     */
    @Test
    @Ignore("JBEAP-12410")
    public void scenario2() throws Exception {
        testSecurityDomainAuthenticateWithoutForwarding();
        testOauthbearerPropagationPasses();
        testSecurityDomainAuthenticateForwardedPasses();
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
     * Creates callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as "whoami" user.
     *
     * @param type reauthentication reauthentication type used within the doubleWhoAmI
     * @return Callable
     */
    private Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type) {
        return getDoubleWhoAmICallable(type, "whoami", "whoami");
    }

    /**
     * Creates callable for executing {@link Entry#doubleWhoAmI(String, String, ReAuthnType, String)} as given user.
     *
     * @param type reauthentication re-authentication type used within the doubleWhoAmI
     * @param username
     * @param password
     * @return
     */
    private Callable<String[]> getDoubleWhoAmICallable(final ReAuthnType type, final String username, final String password) {
        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(SERVER1, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String server2Url = server2.getApplicationRemotingUrl();
            return bean.doubleWhoAmI(username, password, type, server2Url, isWhoAmIStateful());
        };
    }

    private Callable<String> getEjbToServletCallable(final ReAuthnType type, final String username, final String password) {
        return () -> {
            final Entry bean = SeccontextUtil.lookup(
                    SeccontextUtil.getRemoteEjbName(SERVER1, "EntryBean", Entry.class.getName(), isEntryStateful()),
                    server1.getApplicationRemotingUrl());
            final String servletUrl = server2.getApplicationHttpUrl() + "/" + SERVER2 + WhoAmIServlet.SERVLET_PATH;
            return bean.readUrl(username, password, type, new URL(servletUrl));
        };
    }

    private static org.hamcrest.Matcher<java.lang.String> isEjbAuthenticationError() {
        // different behavior for stateless and stateful beans
        // is reported under https://issues.jboss.org/browse/JBEAP-12439
        return anyOf(startsWith("javax.ejb.NoSuchEJBException: EJBCLIENT000079"),
                startsWith("javax.naming.CommunicationException: EJBCLIENT000062"));
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

    private static class ServerHolder {
        private String name;
        private String host;
        private int portOffset;
        private ModelControllerClient client;
        private CommandContext commandCtx;
        private ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();

        private String snapshot;

        public ServerHolder(String name, String host, int portOffset) {
            this.name = name;
            this.host = host;
            this.portOffset = portOffset;
        }

        public void resetContainerConfiguration() throws CommandLineException, IOException, MgmtOperationException {
            if (!containerController.isStarted(name)) {
                containerController.start(name);
                client = ModelControllerClient.Factory.create(host, getManagementPort());
                commandCtx = CLITestUtil.getCommandContext(host, getManagementPort(), null, consoleOut, -1);
                commandCtx.connectController();
                readSnapshot();
            }

            if (snapshot == null) {
                final File cliFIle = File.createTempFile("seccontext-", ".cli");
                try (FileOutputStream fos = new FileOutputStream(cliFIle)) {
                    IOUtils.copy(AbstractSecurityContextPropagationTestBase.class.getResourceAsStream("seccontext-setup.cli"),
                            fos);
                }
                runBatch(cliFIle);
                cliFIle.delete();
                reload();
                // deployment name is the same as the container name in this test case
                deployer.deploy(name);

                takeSnapshot();
            } else {
                reloadToSnapshot();
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

        private void reloadToSnapshot() {
            ModelNode operation = Util.createOperation("reload", null);
            operation.get(SERVER_CONFIG).set(snapshot);
            ServerReload.executeReloadAndWaitForCompletion(client, operation, (int) SECONDS.toMillis(90), host,
                    getManagementPort());
        }

        private void reload() {
            ModelNode operation = Util.createOperation("reload", null);
            ServerReload.executeReloadAndWaitForCompletion(client, operation, (int) SECONDS.toMillis(90), host,
                    getManagementPort());
        }
    }
}
