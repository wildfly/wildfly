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

package org.jboss.as.test.integration.web.security.tg;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.categories.CommonCriteria;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.as.test.integration.web.security.WebSecurityCommon;
import org.jboss.as.test.integration.web.security.WebTestsSecurityDomainSetup;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.other.SimpleSocketBinding;
import org.wildfly.test.undertow.common.elytron.SimpleHttpsListener;

/**
 * This test case check if transport-guarantee security constraint works properly.
 *
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({WebTestsSecurityDomainSetup.class, TransportGuaranteeTestCase.ListenerSetup.class})
@Category(CommonCriteria.class)
public class TransportGuaranteeTestCase {

    private static final Logger log = Logger.getLogger(TransportGuaranteeTestCase.class);
    private static final String WAR = ".war";
    private static final String TG_ANN = "tg-annotated";
    private static final String TG_DD = "tg-dd";
    private static final String TG_MIXED = "tg-mixed";
    private static String httpsTestURL = null;
    private static String httpTestURL = null;

    @Deployment(name = TG_ANN + WAR, order = 1, testable = false)
    public static WebArchive deployAnnWar() throws Exception {
        return getDeployment(TG_ANN);
    }

    @Deployment(name = TG_DD + WAR, order = 2, testable = false)
    public static WebArchive deployDdWar() {
        return getDeployment(TG_DD);
    }

    @Deployment(name = TG_MIXED + WAR, order = 3, testable = false)
    public static WebArchive deployMixedWar() {
        return getDeployment(TG_MIXED);
    }

    private static WebArchive getDeployment(String warName) {
        log.trace("starting to deploy " + warName + ".war");

        WebArchive war = ShrinkWrap.create(WebArchive.class, warName + WAR);

        if (TG_MIXED.equals(warName)) {
            war.addClass(TransportGuaranteeMixedServlet.class);
            war.setWebXML(TransportGuaranteeTestCase.class.getPackage(), "mixed-web.xml");
        } else if (TG_DD.equals(warName)) {
            war.addClass(TransportGuaranteeServlet.class);
            war.setWebXML(TransportGuaranteeTestCase.class.getPackage(), "dd-web.xml");
        } else if (TG_ANN.equals(warName)) {
            war.addClass(TransportGuaranteeAnnotatedServlet.class);
            war.setWebXML(TransportGuaranteeTestCase.class.getPackage(), "annotated-web.xml");
        }

        war.addAsWebInfResource(TransportGuaranteeTestCase.class.getPackage(), "jboss-web.xml", "jboss-web.xml");

        return war;
    }

    @Before
    public void before() throws IOException {
        // set test URL
        httpsTestURL = "https://" + TestSuiteEnvironment.getHttpAddress() + ":" + Integer.toString
                (TransportGuaranteeTestCase.ListenerSetup.HTTPS_PORT);
        httpTestURL = "http://" + TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort();
    }

    @AfterClass
    public static void after() throws IOException {
    }

    /**
     * Check response on given url
     *
     * @param url
     * @param responseSubstring - if null we are checking response code only
     * @return
     * @throws Exception
     */
    private boolean checkGetURL(String url, String responseSubstring, String user, String pass) throws Exception {
        log.trace("Checking URL=" + url);

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope(AuthScope.ANY),
                new UsernamePasswordCredentials(user, pass));

        CloseableHttpClient httpClient;
        if (url.startsWith("https")) {
            httpClient = TestHttpClientUtils.getHttpsClient(credentialsProvider);
        } else {
            httpClient = HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .build();
        }

        HttpGet get = new HttpGet(url);
        HttpResponse hr;
        try {
            try {
                hr = httpClient.execute(get);
            } catch (Exception e) {
                if (responseSubstring == null) {
                    return false;
                } else {
                    // in case substring is defined, rethrow exception so, we can easier analyze the cause
                    throw new Exception(e);
                }
            }

            int statusCode = hr.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                log.trace("statusCode not expected. statusCode=" + statusCode + ", URL=" + url);
                return false;
            }

            if (responseSubstring == null) {
                // this indicates that negative test had problems
                log.trace("statusCode==200 on URL=" + url);
                return true;
            }

            String response = EntityUtils.toString(hr.getEntity());
            if (response.indexOf(responseSubstring) != -1) {
                return true;
            } else {
                log.trace("Response doesn't contain expected substring (" + responseSubstring + ")");
                return false;
            }
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    @Test
    public void testTransportGuaranteedAnnotation() throws Exception {
        performRequestsAndCheck("/" + TG_ANN + TransportGuaranteeAnnotatedServlet.servletContext);
    }

    @Test
    public void testTransportGuaranteedDD() throws Exception {
        performRequestsAndCheck("/" + TG_DD + TransportGuaranteeServlet.servletContext);
    }

    @Test
    public void testTransportGuaranteedMixed() throws Exception {
        performRequestsAndCheck("/" + TG_MIXED + "/tg_mixed_override/srv");
    }

    private void performRequestsAndCheck(String testURLContext) throws Exception {
        boolean result = checkGetURL(
                httpsTestURL + testURLContext,
                "TransportGuaranteedGet",
                "anil",
                "anil");
        Assert.assertTrue("Not expected response", result);

        result = checkGetURL(
                httpTestURL + testURLContext,
                null,
                "anil",
                "anil");
        Assert.assertFalse("Non secure transport on URL has to be prevented, but was not", result);
    }

    static class ListenerSetup extends AbstractSecurityRealmsServerSetupTask implements ServerSetupTask {

        private static final Logger log = Logger.getLogger(ListenerSetup.class);

        private static final String NAME = TransportGuaranteeTestCase.class.getSimpleName();
        private static final File WORK_DIR = new File("target", "wildfly" + File.separator + "standalone" + File
                .separator + "configuration");
        private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, "application.keystore");
        private static final String PASSWORD = "password";

        public static final int HTTPS_PORT = 8343;

        private CLIWrapper cli;
        private SimpleKeyStore simpleKeystore;
        private SimpleKeyManager simpleKeyManager;
        private SimpleServerSslContext simpleServerSslContext;
        private SimpleSocketBinding simpleSocketBinding;
        private SimpleHttpsListener simpleHttpsListener;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            if (WebSecurityCommon.isElytron()) {
                cli = new CLIWrapper(true);
                setElytronBased(managementClient);
            } else {
                super.setup(managementClient, containerId);
                setLegacySecurityRealmBased(managementClient);
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (WebSecurityCommon.isElytron()) {
                cli = new CLIWrapper(true);
                simpleHttpsListener.remove(cli);
                simpleSocketBinding.remove(cli);
                simpleServerSslContext.remove(cli);
                simpleKeyManager.remove(cli);
                simpleKeystore.remove(cli);
            } else {
                final List<ModelNode> updates = new ArrayList<ModelNode>();

                ModelNode op = new ModelNode();
                op.get(OP).set(REMOVE);
                op.get(OP_ADDR).add(SUBSYSTEM, "undertow");
                op.get(OP_ADDR).add("server", "default-server");
                op.get(OP_ADDR).add("https-listener", NAME);
                // Don't rollback when the AS detects the war needs the module
                op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
                op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                updates.add(op);

                op = new ModelNode();
                op.get(OP).set(REMOVE);
                op.get(OP_ADDR).add("socket-binding-group", "standard-sockets");
                op.get(OP_ADDR).add("socket-binding", NAME);
                op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
                updates.add(op);
                try {
                    applyUpdates(managementClient.getControllerClient(), updates);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                super.tearDown(managementClient, containerId);
            }
        }

        protected void setElytronBased(ManagementClient managementClient) throws Exception {
            setHttpsListenerSslContextBased(managementClient, cli, NAME, NAME, HTTPS_PORT, NAME, false);
        }

        protected void setLegacySecurityRealmBased(final ManagementClient managementClient) throws Exception {
            setHttpsListenerSecurityRealmBased(NAME, NAME, HTTPS_PORT, NAME, "NOT_REQUESTED", managementClient);
        }

        private void setHttpsListenerSecurityRealmBased(String httpsListenerName, String sockBindName, int httpsPort,
                                                        String secRealmName, String verifyClient, ManagementClient
                                                                managementClient) {
            log.debug("start of the creation of the https-listener with legacy security-realm");

            final List<ModelNode> updates = new ArrayList<ModelNode>();

            // Add the HTTPS socket binding.
            ModelNode op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add("socket-binding-group", "standard-sockets");
            op.get(OP_ADDR).add("socket-binding", sockBindName);
            op.get("interface").set("public");
            op.get("port").set(httpsPort);
            op.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(op);

            // Add the HTTPS connector.
            final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
            final ModelNode steps = composite.get(STEPS);
            op = new ModelNode();
            op.get(OP).set(ADD);
            op.get(OP_ADDR).add(SUBSYSTEM, "undertow");
            op.get(OP_ADDR).add("server", "default-server");
            op.get(OP_ADDR).add("https-listener", httpsListenerName);
            op.get("socket-binding").set(sockBindName);
            op.get("enabled").set(true);
            op.get("security-realm").set(secRealmName);
            op.get("verify-client").set(verifyClient);
            steps.add(op);

            composite.get(OPERATION_HEADERS).get(ALLOW_RESOURCE_SERVICE_RESTART).set(true);
            updates.add(composite);

            applyUpdates(managementClient.getControllerClient(), updates);

            log.debug("end of the security-realm https-listener creation");
        }

        @Override
        protected SecurityRealm[] getSecurityRealms() throws Exception {
            RealmKeystore keystore = new RealmKeystore.Builder()
                    .keystorePassword(PASSWORD)
                    .keystorePath(SERVER_KEYSTORE_FILE.getAbsolutePath())
                    .build();
            return new SecurityRealm[]{new SecurityRealm.Builder().name(NAME).serverIdentity(new
                    ServerIdentity.Builder().ssl(keystore).build()).build()};
        }

        private void setHttpsListenerSslContextBased(ManagementClient managementClient, CLIWrapper cli, String
                httpsListenerName, String sockBindName, int httpsPort, String sslContext, boolean verifyClient) throws
                Exception {
            log.debug("start of the creation of the https-listener with ssl-context");

            simpleKeystore = SimpleKeyStore.builder().withName(NAME)
                    .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getAbsolutePath()).build())
                    .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                    .build();
            simpleKeystore.create(cli);
            simpleKeyManager = SimpleKeyManager.builder().withName(NAME)
                    .withKeyStore(NAME)
                    .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                    .build();
            simpleKeyManager.create(cli);
            simpleServerSslContext = SimpleServerSslContext.builder().withName(sslContext)
                    .withKeyManagers(NAME)
                    .withProtocols("TLSv1.2")
                    .withNeedClientAuth(verifyClient)
                    .withAuthenticationOptional(false)
                    .build();
            simpleServerSslContext.create(cli);

            simpleSocketBinding = SimpleSocketBinding.builder().withName(sockBindName).withPort(httpsPort)
                    .build();
            simpleSocketBinding.create(managementClient.getControllerClient(), cli);
            simpleHttpsListener = SimpleHttpsListener.builder().withName(httpsListenerName).withSocketBinding
                    (sockBindName).
                    withSslContext(NAME).build();
            simpleHttpsListener.create(cli);

            log.debug("end of the ssl-context https-listener creation");
        }

        protected static void applyUpdates(final ModelControllerClient client, final List<ModelNode> updates) {
            for (ModelNode update : updates) {
                try {
                    applyUpdate(client, update, false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        protected static void applyUpdate(final ModelControllerClient client, ModelNode update, boolean allowFailure)
                throws IOException {
            ModelNode result = client.execute(new OperationBuilder(update).build());
            if (result.hasDefined("outcome") && (allowFailure || "success".equals(result.get("outcome").asString()))) {
                if (result.hasDefined("result")) {
                    log.trace(result.get("result"));
                }
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }
}
