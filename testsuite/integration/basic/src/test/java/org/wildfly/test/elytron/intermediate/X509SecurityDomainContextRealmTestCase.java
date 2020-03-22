/*
 * Copyright 2020 Red Hat, Inc.
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
package org.wildfly.test.elytron.intermediate;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.AbstractSecurityDomainsServerSetupTask;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SSLTruststoreUtil;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.integration.security.common.config.JSSE;
import org.jboss.as.test.integration.security.common.config.SecureStore;
import org.jboss.as.test.integration.security.common.config.SecurityDomain;
import org.jboss.as.test.integration.security.common.config.SecurityModule;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.ModelNodeUtil;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleSecurityDomain;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;
import org.wildfly.test.security.common.elytron.UndertowSslContext;
import org.wildfly.test.undertow.common.UndertowApplicationSecurityDomain;

/**
 * Test to check intermediate elytron/picketbox configuration with a X509
 * certificate login.
 *
 * @author rmartinc
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({
    X509SecurityDomainContextRealmTestCase.ElytronSslContextInUndertowSetupTask.class,
    X509SecurityDomainContextRealmTestCase.SecurityDomainsSetup.class,
    X509SecurityDomainContextRealmTestCase.SecurityElytronRealmSetup.class,
    X509SecurityDomainContextRealmTestCase.ElytronSetup.class})
public class X509SecurityDomainContextRealmTestCase {

    private static final String NAME = X509SecurityDomainContextRealmTestCase.class.getSimpleName();
    private static final String DEPLOYMENT = "X509SecurityDomainContextRealmTestCaseDep";
    private static final String SECURITY_DOMAIN_NAME = "X509SecurityDomainContextRealmSecDom";
    private static final String ELYTRON_REALM_NAME = "X509SecurityDomainContextRealmRealm";
    private static final String ELYTRON_DOMAIN_NAME = "X509SecurityDomainContextRealmDom";

    private static final File WORK_DIR = new File("target" + File.separatorChar +  NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final File CLIENT_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_KEYSTORE);
    private static final File CLIENT_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.CLIENT_TRUSTSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;

    @Deployment(name = DEPLOYMENT)
    public static WebArchive deployment() {
        final Package currentPackage = X509SecurityDomainContextRealmTestCase.class.getPackage();
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT + ".war")
                .addClasses(PrincipalCounterServlet.class, LoginCounterLoginModule.class)
                .addAsWebInfResource(currentPackage, "x509-security-domain-context-realm-web.xml", "web.xml")
                .addAsWebInfResource(Utils.getJBossWebXmlAsset(ELYTRON_DOMAIN_NAME), "jboss-web.xml");
    }

    private String makeCallWithCerts(URL webAppURL, File keyStoreFile, String keyStorePassword,
            File trustStoreFile, String trustStorePassword, int expectedStatusCode) throws Exception {
        HttpClient client = null;
        try {
            client = SSLTruststoreUtil.getHttpClientWithSSL(keyStoreFile, keyStorePassword, trustStoreFile, trustStorePassword);
            return Utils.makeCallWithHttpClient(webAppURL, client, expectedStatusCode);
        } finally {
            if (client != null) {
                ((CloseableHttpClient) client).close();
            }
        }
    }

    @Test
    public void testCertificateLogin(@ArquillianResource URL webAppURL) throws Exception {
        webAppURL = new URIBuilder()
                .setScheme("https")
                .setHost(webAppURL.getHost())
                .setPort(SSLTruststoreUtil.HTTPS_PORT)
                .setPath(webAppURL.getPath() + PrincipalCounterServlet.SERVLET_PATH)
                .build().toURL();
        // test KO without client certs
        makeCallWithCerts(webAppURL, null, null, CLIENT_TRUSTSTORE_FILE, PASSWORD, HttpServletResponse.SC_FORBIDDEN);
        // test OK with valid client certs
        String response = makeCallWithCerts(webAppURL, CLIENT_KEYSTORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD, HttpServletResponse.SC_OK);
        Assert.assertThat("Username is correct", response, CoreMatchers.startsWith("CN=client:"));
        int counter = Integer.parseInt(response.substring("CN=client:".length()));
        // same call OK but checking cache is used
        response = makeCallWithCerts(webAppURL, CLIENT_KEYSTORE_FILE, PASSWORD, CLIENT_TRUSTSTORE_FILE, PASSWORD, HttpServletResponse.SC_OK);
        Assert.assertThat("Username is correct", response, CoreMatchers.startsWith("CN=client:"));
        int newCounter = Integer.parseInt(response.substring("CN=client:".length()));
        Assert.assertThat("Cache is working and same login count", newCounter, CoreMatchers.is(counter));
    }

    /**
     * Configures the server to use Elytron server-ssl-context and key/trust stores.
     */
    static class ElytronSslContextInUndertowSetupTask extends AbstractElytronSetupTask {

        @Override
        protected void setup(final ModelControllerClient modelControllerClient) throws Exception {
            keyMaterialSetup(WORK_DIR);
            super.setup(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[] {
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                        .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getPath()).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                        .build(),
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .withPath(Path.builder().withPath(SERVER_TRUSTSTORE_FILE.getPath()).build())
                        .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                        .build(),
                SimpleKeyManager.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                        .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                        .build(),
                SimpleTrustManager.builder().withName(NAME)
                        .withKeyStore(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                        .build(),
                SimpleServerSslContext.builder().withName(NAME).withKeyManagers(NAME).withTrustManagers(NAME).build(),
                UndertowSslContext.builder().withName(NAME).build()
            };
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
            FileUtils.deleteDirectory(WORK_DIR);
        }

        protected static void keyMaterialSetup(File workDir) throws Exception {
            FileUtils.deleteDirectory(workDir);
            workDir.mkdirs();
            Assert.assertTrue(workDir.exists());
            Assert.assertTrue(workDir.isDirectory());
            CoreUtils.createKeyMaterial(workDir);
        }
    }

    /**
     * Creates a security-domain with default cache to be used as legacy realm.
     * A certificate module is used to test the X509 integration in elytron.
     * The LoginCounterLoginModule is added to count the number of logins.
     *
     * <security-domain name="SECURITY_DOMAIN_NAME" cache-type="default">
     *      <authentication>
     *          <login-module code="Certificate" flag="required">
     *              <module-option name="password-stacking" value="useFirstPass"/>
     *              <module-option name="securityDomain" value="SECURITY_DOMAIN_NAME"/>
     *          </login-module>
     *         <login-module code="org.wildfly.test.elytron.LoginCounterLoginModule" flag="optional">
     *             <module-option name="password-stacking" value="useFirstPass"/>
     *         </login-module>
     *      </authentication>
     *      <jsse keystore-password="PASSWORD" keystore-url="CLIENT_KEYSTORE_FILE"/>
     *  </security-domain>
     */
    static class SecurityDomainsSetup extends AbstractSecurityDomainsServerSetupTask {

        @Override
        protected SecurityDomain[] getSecurityDomains() throws Exception {
            Map<String, String> lmOptions = new HashMap<>();
            lmOptions.put("password-stacking", "useFirstPass");
            lmOptions.put("securityDomain", SECURITY_DOMAIN_NAME);
            final SecurityModule.Builder certificateBuilder = new SecurityModule.Builder()
                    .name("Certificate")
                    .options(lmOptions)
                    .flag("required");

            lmOptions = new HashMap<>();
            lmOptions.put("password-stacking", "useFirstPass");
            final SecurityModule.Builder counterBuilder = new SecurityModule.Builder()
                    .name(LoginCounterLoginModule.class.getName())
                    .options(lmOptions)
                    .flag("optional");

            final SecurityDomain sd = new SecurityDomain.Builder()
                    .name(SECURITY_DOMAIN_NAME)
                    .cacheType("default")
                    .loginModules(certificateBuilder.build(), counterBuilder.build())
                    .jsse(new JSSE.Builder()
                            .keyStore(new SecureStore.Builder()
                                    .url(CLIENT_KEYSTORE_FILE.toURI().toURL())
                                    .password(PASSWORD)
                                    .build())
                            .build())
                    .build();

            return new SecurityDomain[]{sd};
        }
    }

    /**
     * Creates the elytron realm mapper to use in the mixed configuration:
     *
     *  <security-realms>
     *      <elytron-realm name="ELYTRON_REALM_NAME" legacy-jaas-config="SECURITY_DOMAIN_NAME"/>
     *  </security-realms>
     */
    static class SecurityElytronRealmSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient mc, String string) throws Exception {
            // /subsystem=security/elytron-realm=ELYTRON_REALM_NAME:add(legacy-jaas-config=SECURITY_DOMAIN_NAME)
            ModelNode op = Util.createAddOperation(
                PathAddress.pathAddress().append("subsystem", "security").append("elytron-realm", ELYTRON_REALM_NAME));
            ModelNodeUtil.setIfNotNull(op, "legacy-jaas-config", SECURITY_DOMAIN_NAME);
            CoreUtils.applyUpdate(op, mc.getControllerClient());
            ServerReload.reloadIfRequired(mc);
        }

        @Override
        public void tearDown(ManagementClient mc, String string) throws Exception {
            // /subsystem=security/elytron-realm=ELYTRON_REALM_NAME:remove
            CoreUtils.applyUpdate(Util.createRemoveOperation(
                    PathAddress.pathAddress().append("subsystem", "security").append("elytron-realm", ELYTRON_REALM_NAME)),
                    mc.getControllerClient());
            ServerReload.reloadIfRequired(mc);
        }
    }

    /**
     * Creates the elytron setup to use the legacy security domain.
     */
    static class ElytronSetup extends AbstractElytronSetupTask {

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[]{
                SimpleSecurityDomain.builder()
                    .withName(ELYTRON_DOMAIN_NAME)
                    .withDefaultRealm(ELYTRON_REALM_NAME)
                    .withPermissionMapper("default-permission-mapper")
                    .withRealms(SimpleSecurityDomain.SecurityDomainRealm.builder()
                        .withRealm(ELYTRON_REALM_NAME)
                        .build())
                    .build(),
                UndertowApplicationSecurityDomain.builder()
                    .withName(ELYTRON_DOMAIN_NAME)
                    .withSecurityDomain(ELYTRON_DOMAIN_NAME)
                    .build()
            };
        }
    }
}
