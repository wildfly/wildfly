/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.elytron.http;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import static org.junit.Assert.assertEquals;
import static org.wildfly.security.mechanism.gssapi.GSSCredentialSecurityFactory.KERBEROS_V5;
import static org.wildfly.security.mechanism.gssapi.GSSCredentialSecurityFactory.SPNEGO;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.commons.io.FileUtils;
import org.apache.kerby.kerberos.kerb.KrbException;
import org.apache.kerby.kerberos.kerb.client.KrbConfig;
import org.apache.kerby.kerberos.kerb.common.EncryptionUtil;
import org.apache.kerby.kerberos.kerb.keytab.Keytab;
import org.apache.kerby.kerberos.kerb.keytab.KeytabEntry;
import org.apache.kerby.kerberos.kerb.type.KerberosTime;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.apache.kerby.kerberos.kerb.server.impl.DefaultInternalKdcServerImpl;
import org.apache.kerby.kerberos.kerb.type.base.EncryptionKey;
import org.apache.kerby.kerberos.kerb.type.base.PrincipalName;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSystemPropertiesServerSetupTask;
import org.jboss.as.test.integration.security.common.Krb5LoginConfiguration;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.servlets.SimpleServlet;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.MechanismConfiguration;
import org.wildfly.test.security.common.elytron.PropertiesRealm;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;

/**
 * Test of SPNEGO HTTP mechanism.
 *
 * @author Jan Kalina
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
        SpnegoMechTestCase.KDCServerSetupTask.class,
        SpnegoMechTestCase.ServerSetup.class,
        SpnegoMechTestCase.KerberosSystemPropertiesSetupTask.class
})
public class SpnegoMechTestCase extends AbstractMechTestBase {

    private static final String NAME = SpnegoMechTestCase.class.getSimpleName();
    private static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String CHALLENGE_PREFIX = "Negotiate ";
    private static final File KRB5_CONF = new File(SpnegoMechTestCase.class.getResource(NAME + "-krb5.conf").getFile());
    private static final boolean DEBUG = false;
    private static final String SERVER_PRINCIPAL = "HTTP/localhost@WILDFLY.ORG";
    private static final String PRINCIPAL = "user1@WILDFLY.ORG";
    private static final String PRINCIPAL_PASSWORD = "password1";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .addClasses(SimpleServlet.class)
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(APP_DOMAIN), "jboss-web.xml")
                .addAsWebInfResource(SpnegoMechTestCase.class.getPackage(), NAME + "-web.xml", "web.xml");
    }

    /**
     * Setup system properties for client.
     */
    @BeforeClass
    public static void initProperties() {
        System.setProperty("java.security.krb5.conf", KRB5_CONF.getAbsolutePath());
        System.setProperty("sun.security.krb5.debug", Boolean.toString(DEBUG));
    }

    @Test
    public void testSuccess() throws Exception {

        final Krb5LoginConfiguration krb5Configuration = new Krb5LoginConfiguration(Utils.getLoginConfiguration());
        Configuration.setConfiguration(krb5Configuration);

        LoginContext lc = Utils.loginWithKerberos(krb5Configuration, PRINCIPAL, PRINCIPAL_PASSWORD);
        Subject.doAs(lc.getSubject(), (PrivilegedExceptionAction<Void>) () -> {
            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                GSSManager manager = GSSManager.getInstance();
                GSSName acceptorName = manager.createName("HTTP@localhost", GSSName.NT_HOSTBASED_SERVICE);
                GSSCredential credential = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, new Oid[] { KERBEROS_V5, SPNEGO }, GSSCredential.INITIATE_ONLY);
                GSSContext context = manager.createContext(acceptorName, KERBEROS_V5, credential, GSSContext.INDEFINITE_LIFETIME);

                URI uri = new URI(url.toExternalForm() + "role1");
                byte[] message = new byte[0];

                for (int i = 0; i < 5; i++) { // prevent infinite loop - max 5 continuations
                    message = context.initSecContext(message, 0, message.length);

                    HttpGet request = new HttpGet(uri);
                    request.setHeader(HEADER_AUTHORIZATION, CHALLENGE_PREFIX + Base64.getEncoder().encodeToString(message));
                    try ( CloseableHttpResponse response = httpClient.execute(request)) {
                        int statusCode = response.getStatusLine().getStatusCode();

                        if (statusCode != SC_UNAUTHORIZED) {
                            assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode);
                            assertEquals("Unexpected content of HTTP response.", SimpleServlet.RESPONSE_BODY, EntityUtils.toString(response.getEntity()));

                            // test cached identity
                            HttpGet request2 = new HttpGet(uri);
                            try ( CloseableHttpResponse response2 = httpClient.execute(request2)) {
                                int statusCode2 = response.getStatusLine().getStatusCode();
                                assertEquals("Unexpected status code in HTTP response.", SC_OK, statusCode2);
                                assertEquals("Unexpected content of HTTP response.", SimpleServlet.RESPONSE_BODY, EntityUtils.toString(response2.getEntity()));
                            }

                            return null;
                        }

                        String responseHeader = response.getFirstHeader(HEADER_WWW_AUTHENTICATE).getValue();
                        if (!responseHeader.startsWith(CHALLENGE_PREFIX)) Assert.fail("Invalid authenticate header");
                        message = Base64.getDecoder().decode(responseHeader.substring(CHALLENGE_PREFIX.length()));
                    }
                }
                Assert.fail("Infinite unauthorized loop");
            }
            return null;
        });
    }

    /**
     * A setup task which configures and starts Kerberos KDC server.
     */
    static class KDCServerSetupTask implements ServerSetupTask {

        private SimpleKdcServer simpleKdcServer;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {

            simpleKdcServer = new SimpleKdcServer();
            simpleKdcServer.setKdcHost("localhost");
            simpleKdcServer.setKdcRealm("WILDFLY.ORG");
            simpleKdcServer.setAllowUdp(true);
            simpleKdcServer.setKdcUdpPort(6088);
            simpleKdcServer.setInnerKdcImpl(new DefaultInternalKdcServerImpl(simpleKdcServer.getKdcSetting()));

            simpleKdcServer.init();

            simpleKdcServer.createPrincipal(SERVER_PRINCIPAL,"httppwd");
            simpleKdcServer.createPrincipal(PRINCIPAL, PRINCIPAL_PASSWORD);

            System.out.println("Starting kerberos");
            simpleKdcServer.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            System.out.println("Stopping kerby");
            simpleKdcServer.stop();
        }
    }

    /**
     * A setup task which configures application server to use Kerberos.
     * Includes keytab file generating. Excludes system properties setting.
     */
    static class ServerSetup extends AbstractMechTestBase.ServerSetup {

        private static final String SERVER_KEY_TAB = NAME + ".keytab";
        private static final File WORK_DIR = new File("target" + File.separatorChar + NAME);

        private final File SERVER_KEY_TAB_FILE = new File(WORK_DIR, SERVER_KEY_TAB);

        public ServerSetup() throws IOException, KrbException {
            FileUtils.deleteDirectory(WORK_DIR);
            WORK_DIR.mkdirs();
            generateKeyTab(SERVER_KEY_TAB_FILE, SERVER_PRINCIPAL, "httppwd");
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            List<ConfigurableElement> elements = new ArrayList<>();

            // create kerberos-security-factory
            elements.add(new ConfigurableElement() {

                @Override
                public String getName() {
                    return "Create kerberos-security-factory";
                }

                @Override
                public void create(CLIWrapper cli) throws Exception {
                    cli.sendLine("/subsystem=elytron/kerberos-security-factory=" + NAME + ":add(" +
                            "principal=\"" + SERVER_PRINCIPAL + "\", " +
                            "path=\"" + asAbsolutePath(SERVER_KEY_TAB_FILE) + "\", " +
                            "mechanism-names=[KRB5, SPNEGO], required=true, " +
                            "debug=" + Boolean.toString(DEBUG) + ")");
                    if (DEBUG) cli.sendLine("/subsystem=logging/logger=org.wildfly.security:add(level=TRACE)");
                }

                @Override
                public void remove(CLIWrapper cli) throws Exception {
                    cli.sendLine("/subsystem=elytron/kerberos-security-factory=" + NAME + ":remove");
                    if (DEBUG) cli.sendLine("/subsystem=logging/logger=org.wildfly.security:remove");
                }

            });

            // create properties-realm
            elements.add(PropertiesRealm.builder().withName(NAME)
                    .withUser(PRINCIPAL, "", "Role1")
                    .build());

            // create security-domain
            elements.add(SimpleSecurityDomain.builder().withName(NAME).withDefaultRealm(NAME)
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder().withRoleDecoder("groups-to-roles").withRealm(NAME).build())
                    .withPermissionMapper("default-permission-mapper").build());

            Collections.addAll(elements, super.getConfigurableElements());
            return elements.toArray(new ConfigurableElement[elements.size()]);
        }

        @Override
        protected String getSecurityDomain() {
            return NAME;
        }

        @Override
        protected MechanismConfiguration getMechanismConfiguration() {
            return MechanismConfiguration.builder()
                    .withMechanismName("SPNEGO")
                    .withCredentialSecurityFactory(NAME)
                    .build();
        }

        private void generateKeyTab(File keyTabFile, String... credentials) throws IOException, KrbException {
            List<KeytabEntry> entries = new ArrayList<>();
            KerberosTime ktm = new KerberosTime();
            KrbConfig krbConfig = new KrbConfig();

            for (int i = 0; i < credentials.length;) {
                String principal = credentials[i++];
                String password = credentials[i++];

                for (EncryptionKey key : EncryptionUtil.generateKeys(principal, password, krbConfig.getEncryptionTypes())) {
                    entries.add(new KeytabEntry(new PrincipalName(principal), ktm, key.getKvno(), key));
                }
            }

            Keytab keyTab = new Keytab();
            keyTab.addKeytabEntries(entries);
            keyTab.store(keyTabFile);
        }
    }


    /**
     * A setup task creating properties on application server.
     */
    static class KerberosSystemPropertiesSetupTask extends AbstractSystemPropertiesServerSetupTask {

        @Override
        protected SystemProperty[] getSystemProperties() {
            final Map<String, String> map = new HashMap<>();
            map.put("java.security.krb5.conf", KRB5_CONF.getAbsolutePath());
            map.put("sun.security.krb5.debug", Boolean.toString(DEBUG));
            return mapToSystemProperties(map);
        }

    }
}
