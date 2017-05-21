/*
 * Copyright 2017 Red Hat, Inc.
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

package org.wildfly.test.integration.elytron.sasl.mgmt;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.jboss.as.test.integration.security.common.SecurityTestConstants.KEYSTORE_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractKrb5ConfServerSetupTask;
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
import org.jboss.as.test.integration.security.common.KerberosSystemPropertiesSetupTask;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.ManagedCreateLdapServer;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.negotiation.KerberosTestUtils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.SecurityFactory;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.MatchRule;
import org.wildfly.security.auth.permission.LoginPermission;
import org.wildfly.security.sasl.SaslMechanismSelector;
import org.wildfly.security.ssl.SSLContextBuilder;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.ConstantPermissionMapper;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.DirContext;
import org.wildfly.test.security.common.elytron.IdentityMapping;
import org.wildfly.test.security.common.elytron.KerberosSecurityFactory;
import org.wildfly.test.security.common.elytron.LdapRealm;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.MechanismRealmConfiguration;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.PermissionRef;
import org.wildfly.test.security.common.elytron.SimpleConfigurableSaslServerFactory;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleSaslAuthenticationFactory;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain.SecurityDomainRealm;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.other.AccessIdentityConfigurator;
import org.wildfly.test.security.common.other.SimpleMgmtNativeInterface;
import org.wildfly.test.security.common.other.SimpleSocketBinding;

/**
 * Tests Elytron Kerberos remoting (GSSAPI and GS2-KRB5* SASL mechanism) through management interface.
 *
 * @author Josef Cacek
 */
@RunWith(Arquillian.class)
@ServerSetup({ KerberosMgmtSaslTestCase.Krb5ConfServerSetupTask.class, //
        KerberosSystemPropertiesSetupTask.class, //
        KerberosMgmtSaslTestCase.DirectoryServerSetupTask.class, //
        KerberosMgmtSaslTestCase.KeyMaterialSetup.class, //
        KerberosMgmtSaslTestCase.ServerSetup.class })
@RunAsClient
public class KerberosMgmtSaslTestCase {

    private static Logger LOGGER = Logger.getLogger(KerberosMgmtSaslTestCase.class);

    protected static final int PORT_NATIVE = 10567;
    protected static final int CONNECTION_TIMEOUT_IN_MS = TimeoutUtil.adjust(6 * 1000);

    private static final String NAME = KerberosMgmtSaslTestCase.class.getSimpleName();
    private static final int LDAP_PORT = 10389;
    private static final String LDAP_URL = "ldap://"
            + NetworkUtils.formatPossibleIpv6Address(Utils.getSecondaryTestAddress(null, true)) + ":" + LDAP_PORT;

    private static final File WORK_DIR_GSSAPI;
    static {
        try {
            WORK_DIR_GSSAPI = Utils.createTemporaryFolder("gssapi-");
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary folder", e);
        }
    }

    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR_GSSAPI, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR_GSSAPI, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR_GSSAPI, SecurityTestConstants.CLIENT_TRUSTSTORE);

    private static Krb5LoginConfiguration KRB5_CONFIGURATION;

    private static SecurityFactory<SSLContext> sslFactory;

    @ArquillianResource
    private ManagementClient client;

    @BeforeClass
    public static void beforeClass() {
        KerberosTestUtils.assumeKerberosAuthenticationSupported();
    }

    @Deployment(testable = false)
    public static WebArchive dummyDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war").addAsWebResource(new StringAsset("Test"), "index.html");
    }

    /**
     * Test GSSAPI SASL mechanism configured on the server-side for management interface without using SSL.
     */
    @Test
    public void testGssapiWithoutSsl() throws Exception {
        configureSaslMechanismOnServer("GSSAPI", false);
        assertKerberosSaslMechPasses("GSSAPI", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GS2-KRB5", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GS2-KRB5-PLUS", "hnelson", "secret", false);
    }

    /**
     * Tests that Kerberos login correctly fails when a wrong username or a wrong password is used.
     */
    @Test
    public void testKerberosWrongCredentials() throws Exception {
        assertKerberosLoginFails("hnelson", "wrongpassword");
        assertKerberosLoginFails("wronguser", "secret");
    }

    /**
     * Tests that ModelControllerClient is able to force using SSL (i.e. when SSL is not used the call should fail).
     */
    @Test
    @Ignore("WFCORE-3002")
    public void testClientForcingSsl() throws Exception {
        configureSaslMechanismOnServer("GSSAPI", false);
        assertKerberosSaslMechFails("GSSAPI", "hnelson", "secret", true);
    }

    /**
     * Test GSSAPI SASL mechanism configured on the server-side for management interface with using SSL.
     */
    @Test
    public void testGssapiOverSsl() throws Exception {
        configureSaslMechanismOnServer("GSSAPI", true);
        assertKerberosSaslMechPasses("GSSAPI", "hnelson", "secret", true);
        assertKerberosSaslMechFails("GSSAPI", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GS2-KRB5", "hnelson", "secret", true);
        assertKerberosSaslMechFails("GS2-KRB5-PLUS", "hnelson", "secret", true);
    }

    /**
     * Test GS2-KRB5 SASL mechanism configured on the server-side for management interface without using SSL.
     */
    @Test
    public void testGs2Krb5WithoutSsl() throws Exception {
        configureSaslMechanismOnServer("GS2-KRB5", false);
        assertKerberosSaslMechPasses("GS2-KRB5", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GSSAPI", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GS2-KRB5-PLUS", "hnelson", "secret", false);
    }

    /**
     * Test GS2-KRB5 SASL mechanism configured on the server-side for management interface with using SSL.
     */
    @Test
    public void testGs2Krb5OverSsl() throws Exception {
        configureSaslMechanismOnServer("GS2-KRB5", true);
        assertKerberosSaslMechPasses("GS2-KRB5", "hnelson", "secret", true);
        assertKerberosSaslMechFails("GS2-KRB5", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GSSAPI", "hnelson", "secret", true);
        assertKerberosSaslMechFails("GS2-KRB5-PLUS", "hnelson", "secret", true);
    }

    /**
     * Test GS2-KRB5-PLUS SASL mechanism configured on the server-side for management interface with using SSL.
     */
    @Test
    public void testGs2Krb5PlusWithoutSsl() throws Exception {
        configureSaslMechanismOnServer("GS2-KRB5-PLUS", false);
        assertKerberosSaslMechFails("GS2-KRB5-PLUS", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GS2-KRB5", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GSSAPI", "hnelson", "secret", false);
    }

    /**
     * Test GS2-KRB5-PLUS SASL mechanism configured on the server-side for management interface with using SSL.
     */
    @Test
    @Ignore("ELY-1232")
    public void testGs2Krb5PlusOverSsl() throws Exception {
        configureSaslMechanismOnServer("GS2-KRB5-PLUS", true);
        assertKerberosSaslMechPasses("GS2-KRB5-PLUS", "hnelson", "secret", true);
        assertKerberosSaslMechFails("GS2-KRB5-PLUS", "hnelson", "secret", false);
        assertKerberosSaslMechFails("GS2-KRB5", "hnelson", "secret", true);
        assertKerberosSaslMechFails("GSSAPI", "hnelson", "secret", true);
    }

    /**
     * Configures test sasl-server-factory to use given mechanism. It also enables/disables SSL based on provided flag.
     */
    private void configureSaslMechanismOnServer(String mechanism, boolean withSsl) throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(String.format(
                    "/subsystem=elytron/configurable-sasl-server-factory=%s:write-attribute(name=filters, value=[{pattern-filter=%s}])",
                    NAME, mechanism));
            String sslContextCli = withSsl
                    ? String.format(
                            "/core-service=management/management-interface=native-interface:write-attribute(name=ssl-context, value=%s)",
                            NAME)
                    : "/core-service=management/management-interface=native-interface:write-attribute(name=ssl-context)";
            cli.sendLine(sslContextCli);
        }
        ServerReload.reloadIfRequired(client.getControllerClient());
    }

    /**
     * Asserts that given user can authenticate with given Kerberos SASL mechanism.
     */
    private void assertKerberosSaslMechPasses(String mech, String user, String password, boolean withSsl)
            throws MalformedURLException, LoginException, Exception {
        // 1. Authenticate to Kerberos.
        final LoginContext lc = Utils.loginWithKerberos(KRB5_CONFIGURATION, user, password);
        try {
            AuthenticationConfiguration authCfg = AuthenticationConfiguration.empty()
                    .setSaslMechanismSelector(SaslMechanismSelector.fromString(mech))
                    .useGSSCredential(getGSSCredential(lc.getSubject()));

            AuthenticationContext authnCtx = AuthenticationContext.empty().with(MatchRule.ALL, authCfg);
            if (withSsl) {
                authnCtx = authnCtx.withSsl(MatchRule.ALL, sslFactory);
            }
            authnCtx.run(() -> assertWhoAmI(user + "@JBOSS.ORG", withSsl));
        } finally {
            lc.logout();
        }
    }

    private void assertKerberosSaslMechFails(String mech, String user, String password, boolean withSsl)
            throws MalformedURLException, LoginException, Exception {
        // 1. Authenticate to Kerberos.
        final LoginContext lc = Utils.loginWithKerberos(KRB5_CONFIGURATION, user, password);
        try {
            AuthenticationConfiguration authCfg = AuthenticationConfiguration.empty()
                    .setSaslMechanismSelector(SaslMechanismSelector.fromString(mech))
                    .useGSSCredential(getGSSCredential(lc.getSubject()));

            AuthenticationContext authnCtx = AuthenticationContext.empty().with(MatchRule.ALL, authCfg);
            if (withSsl) {
                authnCtx = authnCtx.withSsl(MatchRule.ALL, sslFactory);
            }
            authnCtx.run(() -> assertAuthenticationFails(null, null, withSsl));
        } finally {
            lc.logout();
        }
    }

    private void assertKerberosLoginFails(String user, String password) {
        LoginContext lc = null;
        try {
            lc = Utils.loginWithKerberos(KRB5_CONFIGURATION, user, password);
            fail("Kerberos authentication failure was expected.");
        } catch (LoginException e) {
            LOGGER.debug("Kerberos authentication failed as expected.");
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                    LOGGER.warn("Unsuccessful logout", e);
                }
            }
        }
    }

    /**
     * Retrieves {@link GSSCredential} from given Subject
     */
    private GSSCredential getGSSCredential(Subject subject) {
        return Subject.doAs(subject, new PrivilegedAction<GSSCredential>() {
            @Override
            public GSSCredential run() {
                try {
                    GSSManager gssManager = GSSManager.getInstance();
                    return gssManager.createCredential(GSSCredential.INITIATE_ONLY);
                } catch (Exception e) {
                    LOGGER.warn("Unable to retrieve GSSCredential from given Subject.", e);
                }
                return null;
            }
        });
    }

    /**
     * Get the trust manager for {@link #CLIENT_TRUSTSTORE_FILE}.
     *
     * @return the trust manager
     */
    private static X509TrustManager getTrustManager() throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(loadKeyStore(CLIENT_TRUSTSTORE_FILE));

        for (TrustManager current : trustManagerFactory.getTrustManagers()) {
            if (current instanceof X509TrustManager) {
                return (X509TrustManager) current;
            }
        }

        throw new IllegalStateException("Unable to obtain X509TrustManager.");
    }

    private static KeyStore loadKeyStore(final File ksFile) throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        try (FileInputStream fis = new FileInputStream(ksFile)) {
            ks.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        return ks;
    }

    private static void assertAuthenticationFails(String message, Class<? extends Exception> secondCauseClass,
            boolean withTls) {
        if (message == null) {
            message = "The failure of :whoami operation execution was expected, but the call passed";
        }
        final long startTime = System.currentTimeMillis();
        try {
            executeWhoAmI(withTls);
            fail(message);
        } catch (IOException | GeneralSecurityException e) {
            assertTrue("Connection reached its timeout (hang).",
                    startTime + CONNECTION_TIMEOUT_IN_MS > System.currentTimeMillis());
            Throwable cause = e.getCause();
            assertThat("ConnectionException was expected as a cause when authentication fails", cause,
                    is(instanceOf(ConnectException.class)));
            assertThat("Unexpected type of inherited exception for authentication failure", cause.getCause(),
                    anyOf(is(instanceOf(SSLException.class)), is(instanceOf(SaslException.class))));
        }
    }

    private static ModelNode executeWhoAmI(boolean withTls) throws IOException, GeneralSecurityException {
        ModelControllerClientConfiguration.Builder clientConfigBuilder = new ModelControllerClientConfiguration.Builder()
                .setHostName(Utils.getDefaultHost(false)).setPort(PORT_NATIVE)
                // .setProtocol(withTls ? "remote+tls" : "remote")
                .setProtocol("remote").setConnectionTimeout(CONNECTION_TIMEOUT_IN_MS);
        if (withTls) {
            clientConfigBuilder.setSslContext(sslFactory.create());
        }
        ModelControllerClient client = ModelControllerClient.Factory.create(clientConfigBuilder.build());

        ModelNode operation = new ModelNode();
        operation.get("operation").set("whoami");
        operation.get("verbose").set("true");

        return client.execute(operation);
    }

    private static void assertWhoAmI(String expected, boolean withTls) {
        try {
            ModelNode result = executeWhoAmI(withTls);
            assertTrue("The whoami operation should finish with success", Operations.isSuccessfulOutcome(result));
            assertEquals("The whoami operation returned unexpected value", expected,
                    Operations.readResult(result).get("identity").get("username").asString());
        } catch (Exception e) {
            LOGGER.warn("Operation execution failed", e);
            fail("The whoami operation failed - " + e.getMessage());
        }
    }

    /**
     * Setup task which configures Elytron security domains and remoting connectors for this test.
     */
    public static class ServerSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            elements.add(ConstantPermissionMapper.builder().withName(NAME)
                    .withPermissions(PermissionRef.fromPermission(new LoginPermission())).build());

            final CredentialReference credentialReference = CredentialReference.builder().withClearText(KEYSTORE_PASSWORD)
                    .build();

            // KeyStores
            final SimpleKeyStore.Builder ksCommon = SimpleKeyStore.builder().withType("JKS")
                    .withCredentialReference(credentialReference);
            elements.add(ksCommon.withName("server-keystore")
                    .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build()).build());
            elements.add(ksCommon.withName("server-truststore")
                    .withPath(Path.builder().withPath(SERVER_TRUSTSTORE_FILE.getAbsolutePath()).build()).build());

            // Key and Trust Managers
            elements.add(SimpleKeyManager.builder().withName("server-keymanager").withCredentialReference(credentialReference)
                    .withKeyStore("server-keystore").build());
            elements.add(
                    SimpleTrustManager.builder().withName("server-trustmanager").withKeyStore("server-truststore").build());

            // dir-context
            elements.add(DirContext.builder().withName(NAME).withUrl(LDAP_URL).withPrincipal("uid=admin,ou=system")
                    .withCredentialReference(CredentialReference.builder().withClearText("secret").build()).build());
            // ldap-realm
            elements.add(LdapRealm.builder()
                    .withName(NAME).withDirContext(NAME).withIdentityMapping(IdentityMapping.builder()
                            .withRdnIdentifier("krb5PrincipalName").withSearchBaseDn("ou=Users,dc=wildfly,dc=org").build())
                    .build());
            // security-domain
            elements.add(SimpleSecurityDomain.builder().withName(NAME).withDefaultRealm(NAME).withPermissionMapper(NAME)
                    .withRealms(SecurityDomainRealm.builder().withRealm(NAME).build()).build());
            elements.add(AccessIdentityConfigurator.builder().build());

            // kerberos-security-factory
            elements.add(
                    KerberosSecurityFactory
                            .builder().withName(NAME).withPrincipal(Krb5ConfServerSetupTask.REMOTE_PRINCIPAL).withPath(Path
                                    .builder().withPath(Krb5ConfServerSetupTask.REMOTE_KEYTAB_FILE.getAbsolutePath()).build())
                            .build());

            // SASL Authentication
            elements.add(SimpleConfigurableSaslServerFactory.builder().withName(NAME).withSaslServerFactory("elytron").build());
            MechanismConfiguration.Builder mechConfigBuilder = MechanismConfiguration.builder()
                    .addMechanismRealmConfiguration(MechanismRealmConfiguration.builder().withRealmName(NAME).build())
                    .withCredentialSecurityFactory(NAME);
            elements.add(SimpleSaslAuthenticationFactory.builder().withName(NAME).withSaslServerFactory(NAME)
                    .withSecurityDomain(NAME).addMechanismConfiguration(mechConfigBuilder.withMechanismName("GSSAPI").build())
                    .addMechanismConfiguration(mechConfigBuilder.withMechanismName("GS2-KRB5").build())
                    .addMechanismConfiguration(mechConfigBuilder.withMechanismName("GS2-KRB5-PLUS").build()).build());

            // SSLContext
            elements.add(SimpleServerSslContext.builder().withName(NAME).withKeyManagers("server-keymanager")
                    .withTrustManagers("server-trustmanager").withSecurityDomain(NAME).withAuthenticationOptional(true)
                    .withNeedClientAuth(false).build());

            // Socket binding and native management interface
            elements.add(SimpleSocketBinding.builder().withName(NAME).withPort(PORT_NATIVE).build());
            elements.add(SimpleMgmtNativeInterface.builder().withSocketBinding(NAME).withSaslAuthenticationFactory(NAME)
                    .withSslContext(NAME).build());

            return elements.toArray(new ConfigurableElement[elements.size()]);
        }
    }

    public static class KeyMaterialSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.deleteQuietly(WORK_DIR_GSSAPI);
            WORK_DIR_GSSAPI.mkdir();
            Utils.createKeyMaterial(WORK_DIR_GSSAPI);

            sslFactory = new SSLContextBuilder().setClientMode(true).setTrustManager(getTrustManager()).build();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            FileUtils.deleteQuietly(WORK_DIR_GSSAPI);
        }

    }

    /**
     * Task which generates krb5.conf and keytab file(s).
     */
    public static class Krb5ConfServerSetupTask extends AbstractKrb5ConfServerSetupTask {
        public static final File HNELSON_KEYTAB_FILE = new File(WORK_DIR, "hnelson.keytab");
        public static final File JDUKE_KEYTAB_FILE = new File(WORK_DIR, "jduke.keytab");
        public static final String REMOTE_PRINCIPAL = "remote/" + Utils.getSecondaryTestAddress(null, true) + "@JBOSS.ORG";
        public static final File REMOTE_KEYTAB_FILE = new File(WORK_DIR, "remote.keytab");

        @Override
        protected List<UserForKeyTab> kerberosUsers() {
            List<UserForKeyTab> users = new ArrayList<UserForKeyTab>();
            users.add(new UserForKeyTab("hnelson@JBOSS.ORG", "secret", HNELSON_KEYTAB_FILE));
            users.add(new UserForKeyTab("jduke@JBOSS.ORG", "theduke", JDUKE_KEYTAB_FILE));
            users.add(new UserForKeyTab(REMOTE_PRINCIPAL, "zelvicka", REMOTE_KEYTAB_FILE));
            return users;
        }

    }

    // @formatter:off
    @CreateDS(name = "WildFlyDS", factory = InMemoryDirectoryServiceFactory.class, partitions = @CreatePartition(name = "wildfly", suffix = "dc=wildfly,dc=org"), additionalInterceptors = {
            KeyDerivationInterceptor.class }, allowAnonAccess = true)
    @CreateKdcServer(primaryRealm = "JBOSS.ORG", kdcPrincipal = "krbtgt/JBOSS.ORG@JBOSS.ORG", searchBaseDn = "dc=wildfly,dc=org", transports = {
            @CreateTransport(protocol = "UDP", port = 6088) })
    @CreateLdapServer(transports = {
            @CreateTransport(protocol = "LDAP", port = LDAP_PORT) }, saslHost = "localhost", saslPrincipal = "ldap/localhost@JBOSS.ORG", saslMechanisms = {
                    @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
                    @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
                    @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
                    @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
                    @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
                    @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class) })
    // @formatter:on
    static class DirectoryServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private KdcServer kdcServer;
        private LdapServer ldapServer;
        private boolean removeBouncyCastle = false;

        /**
         * Creates directory services, starts LDAP server and KDCServer
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#setup(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                    removeBouncyCastle = true;
                }
            } catch (SecurityException ex) {
                LOGGER.warn("Cannot register BouncyCastleProvider", ex);
            }
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final String hostname = Utils.getCannonicalHost(managementClient);
            final Map<String, String> map = new HashMap<String, String>();
            map.put("hostname", NetworkUtils.formatPossibleIpv6Address(hostname));
            final String secondaryTestAddress = NetworkUtils.canonize(Utils.getSecondaryTestAddress(managementClient, true));
            map.put("ldaphost", secondaryTestAddress);
            final String ldifContent = StrSubstitutor.replace(
                    IOUtils.toString(KerberosMgmtSaslTestCase.class.getResourceAsStream("remoting-krb5-test.ldif"), "UTF-8"),
                    map);
            LOGGER.trace(ldifContent);
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            try {
                for (LdifEntry ldifEntry : new LdifReader(IOUtils.toInputStream(ldifContent, StandardCharsets.UTF_8))) {
                    directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
                }
            } catch (Exception e) {
                LOGGER.warn("Importing LDIF to a directoryService failed.", e);
                throw e;
            }
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, 1024, hostname);
            final ManagedCreateLdapServer createLdapServer = new ManagedCreateLdapServer(
                    (CreateLdapServer) AnnotationUtils.getInstance(CreateLdapServer.class));
            createLdapServer.setSaslHost(secondaryTestAddress);
            createLdapServer.setSaslPrincipal("ldap/" + secondaryTestAddress + "@JBOSS.ORG");
            Utils.fixApacheDSTransportAddress(createLdapServer, secondaryTestAddress);
            ldapServer = ServerAnnotationProcessor.instantiateLdapServer(createLdapServer, directoryService);
            ldapServer.getSaslHost();
            ldapServer.setSearchBaseDn("dc=wildfly,dc=org");
            ldapServer.start();

            KRB5_CONFIGURATION = new Krb5LoginConfiguration(Utils.getLoginConfiguration());
            // Use our custom configuration to avoid reliance on external config
            Configuration.setConfiguration(KRB5_CONFIGURATION);

        }

        /**
         * Stops LDAP server and KDCServer and shuts down the directory service.
         *
         * @param managementClient
         * @param containerId
         * @throws Exception
         * @see org.jboss.as.arquillian.api.ServerSetupTask#tearDown(org.jboss.as.arquillian.container.ManagementClient,
         *      java.lang.String)
         */
        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            KRB5_CONFIGURATION.resetConfiguration();
            ldapServer.stop();
            kdcServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
            if (removeBouncyCastle) {
                try {
                    Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
                } catch (SecurityException ex) {
                    LOGGER.warn("Cannot deregister BouncyCastleProvider", ex);
                }
            }

        }
    }

}
