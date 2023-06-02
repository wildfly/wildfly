/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.elytron.http;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.jboss.as.test.shared.CliUtils.asAbsolutePath;
import static org.junit.Assert.assertEquals;
import static org.wildfly.security.auth.util.GSSCredentialSecurityFactory.KERBEROS_V5;
import static org.wildfly.security.auth.util.GSSCredentialSecurityFactory.SPNEGO;

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
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.keytab.Keytab;
import org.apache.directory.server.kerberos.shared.keytab.KeytabEntry;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import org.jboss.as.test.integration.security.common.KDCServerAnnotationProcessor;
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

        LoginContext lc = Utils.loginWithKerberos(krb5Configuration, "user1@WILDFLY.ORG", "password1");
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
    @CreateDS(
            name = "JBossDS-SpnegoMechTestCase",
            factory = org.jboss.as.test.integration.ldap.InMemoryDirectoryServiceFactory.class,
            partitions = {
                    @CreatePartition(
                            name = "wildfly",
                            suffix = "dc=wildfly,dc=org",
                            contextEntry = @ContextEntry(
                                    entryLdif = "dn: dc=wildfly,dc=org\n" +
                                                "dc: wildfly\n" +
                                                "objectClass: top\n" +
                                                "objectClass: domain\n\n"))
            },
            additionalInterceptors = { KeyDerivationInterceptor.class })
    @CreateKdcServer(
            primaryRealm = "WILDFLY.ORG",
            kdcPrincipal = "krbtgt/WILDFLY.ORG@WILDFLY.ORG",
            searchBaseDn = "dc=wildfly,dc=org",
            transports = {
                    @CreateTransport(protocol = "UDP", port = 6088)
            })
    static class KDCServerSetupTask implements ServerSetupTask {

        private DirectoryService directoryService;
        private KdcServer kdcServer;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            directoryService = DSAnnotationProcessor.getDirectoryService();
            final SchemaManager schemaManager = directoryService.getSchemaManager();
            for (LdifEntry ldifEntry : new LdifReader(SpnegoMechTestCase.class.getResourceAsStream(NAME + ".ldif"))) {
                directoryService.getAdminSession().add(new DefaultEntry(schemaManager, ldifEntry.getEntry()));
            }
            kdcServer = KDCServerAnnotationProcessor.getKdcServer(directoryService, 1024, "localhost");
            System.out.println("Starting kerberos");
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            System.out.println("Stopping kerberos");
            kdcServer.stop();
            directoryService.shutdown();
            FileUtils.deleteDirectory(directoryService.getInstanceLayout().getInstanceDirectory());
        }
    }

    /**
     * A setup task which configures application server to use Kerberos.
     * Includes keytab file generating. Excludes system properties setting.
     */
    static class ServerSetup extends AbstractMechTestBase.ServerSetup {

        private static final String SERVER_PRINCIPAL = "HTTP/localhost@WILDFLY.ORG";
        private static final String SERVER_KEY_TAB = NAME + ".keytab";
        private static final File WORK_DIR = new File("target" + File.separatorChar + NAME);

        private final File SERVER_KEY_TAB_FILE = new File(WORK_DIR, SERVER_KEY_TAB);

        public ServerSetup() throws IOException {
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
                    .withUser("user1@WILDFLY.ORG", "", "Role1")
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

        private void generateKeyTab(File keyTabFile, String... credentials) throws IOException {
            List<KeytabEntry> entries = new ArrayList<>();
            KerberosTime ktm = new KerberosTime();

            for (int i = 0; i < credentials.length;) {
                String principal = credentials[i++];
                String password = credentials[i++];

                for (Map.Entry<EncryptionType, EncryptionKey> keyEntry : KerberosKeyFactory.getKerberosKeys(principal, password).entrySet()) {
                    EncryptionKey key = keyEntry.getValue();
                    entries.add(new KeytabEntry(principal, KerberosPrincipal.KRB_NT_PRINCIPAL, ktm, (byte) key.getKeyVersion(), key));
                }
            }

            Keytab keyTab = Keytab.getInstance();
            keyTab.setEntries(entries);
            keyTab.write(keyTabFile);
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
