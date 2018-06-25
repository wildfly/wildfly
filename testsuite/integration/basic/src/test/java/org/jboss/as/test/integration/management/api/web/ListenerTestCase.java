/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.api.web;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.integration.security.common.SSLTruststoreUtil.HTTPS_PORT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.security.auth.x500.X500Principal;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.http.util.TestHttpClientUtils;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.Listener;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.as.test.integration.security.common.AbstractSecurityRealmsServerSetupTask;
import org.jboss.as.test.integration.security.common.config.realm.Authentication;
import org.jboss.as.test.integration.security.common.config.realm.RealmKeystore;
import org.jboss.as.test.integration.security.common.config.realm.SecurityRealm;
import org.jboss.as.test.integration.security.common.config.realm.ServerIdentity;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.x500.cert.SelfSignedX509CertificateAndSigningKey;

import io.undertow.util.FileUtils;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@ServerSetup(ListenerTestCase.SecurityRealmsSetup.class)
@RunAsClient
public class ListenerTestCase extends ContainerResourceMgmtTestBase {

    /**
     * We use a different socket binding name for each test, as if the socket is still up the service
     * will not be removed. Rather than adding a sleep we use this approach
     */
    private static int socketBindingCount = 0;
    @ArquillianResource
    private URL url;

    private static final char[] GENERATED_KEYSTORE_PASSWORD = "changeit".toCharArray();

    private static final String WORKING_DIRECTORY_LOCATION = "./target/test-classes/security";

    private static final String CLIENT_ALIAS = "client";
    private static final String CLIENT_2_ALIAS = "client2";
    private static final String TEST_ALIAS = "test";

    private static final File KEY_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "server.keystore");
    private static final File TRUST_STORE_FILE = new File(WORKING_DIRECTORY_LOCATION, "jsse.keystore");

    private static final String TEST_CLIENT_DN = "CN=Test Client, OU=JBoss, O=Red Hat, L=Raleigh, ST=North Carolina, C=US";
    private static final String TEST_CLIENT_2_DN = "CN=Test Client 2, OU=JBoss, O=Red Hat, L=Raleigh, ST=North Carolina, C=US";
    private static final String AS_7_DN = "CN=AS7, OU=JBoss, O=Red Hat, L=Raleigh, ST=North Carolina, C=US";

    private static final String SHA_1_RSA = "SHA1withRSA";

    private static KeyStore loadKeyStore() throws Exception{
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null, null);
        return ks;
    }

    private static SelfSignedX509CertificateAndSigningKey createSelfSigned(String DN) {
        return SelfSignedX509CertificateAndSigningKey.builder()
                .setDn(new X500Principal(DN))
                .setKeyAlgorithmName("RSA")
                .setSignatureAlgorithmName(SHA_1_RSA)
                .build();
    }

    private static void addKeyEntry(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, KeyStore keyStore) throws Exception {
        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setKeyEntry(TEST_ALIAS, selfSignedX509CertificateAndSigningKey.getSigningKey(), GENERATED_KEYSTORE_PASSWORD, new X509Certificate[]{certificate});
    }

    private static void addCertEntry(SelfSignedX509CertificateAndSigningKey selfSignedX509CertificateAndSigningKey, String alias, KeyStore keyStore) throws Exception {
        X509Certificate certificate = selfSignedX509CertificateAndSigningKey.getSelfSignedCertificate();
        keyStore.setCertificateEntry(alias, certificate);
    }

    private static void createTemporaryKeyStoreFile(KeyStore keyStore, File outputFile) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(outputFile)){
            keyStore.store(fos, GENERATED_KEYSTORE_PASSWORD);
        }
    }

    private static void setUpKeyStores() throws Exception {
        File workingDir = new File(WORKING_DIRECTORY_LOCATION);
        if (workingDir.exists() == false) {
            workingDir.mkdirs();
        }

        KeyStore keyStore = loadKeyStore();
        KeyStore trustStore = loadKeyStore();

        SelfSignedX509CertificateAndSigningKey testClientSelfSignedX509CertificateAndSigningKey = createSelfSigned(TEST_CLIENT_DN);
        SelfSignedX509CertificateAndSigningKey testClient2SelfSignedX509CertificateAndSigningKey = createSelfSigned(TEST_CLIENT_2_DN);
        SelfSignedX509CertificateAndSigningKey aS7SelfSignedX509CertificateAndSigningKey = createSelfSigned(AS_7_DN);

        addCertEntry(testClient2SelfSignedX509CertificateAndSigningKey, CLIENT_2_ALIAS, keyStore);
        addCertEntry(testClientSelfSignedX509CertificateAndSigningKey, CLIENT_ALIAS, keyStore);
        addKeyEntry(aS7SelfSignedX509CertificateAndSigningKey, keyStore);

        addCertEntry(testClient2SelfSignedX509CertificateAndSigningKey, CLIENT_2_ALIAS, trustStore);
        addCertEntry(testClientSelfSignedX509CertificateAndSigningKey, CLIENT_ALIAS, trustStore);

        createTemporaryKeyStoreFile(keyStore, KEY_STORE_FILE);
        createTemporaryKeyStoreFile(trustStore, TRUST_STORE_FILE);
    }

    private static void deleteKeyStoreFiles() {
        File[] testFiles = {
                KEY_STORE_FILE,
                TRUST_STORE_FILE
        };
        for (File file : testFiles) {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @AfterClass
    public static void afterTests() {
        deleteKeyStoreFiles();
    }

    @Deployment
    public static Archive<?> getDeployment() {
        WebArchive ja = ShrinkWrap.create(WebArchive.class, "proxy.war");
        ja.addClasses(ListenerTestCase.class, RemoteIpServlet.class);
        return ja;
    }

    @Test
    public void testDefaultConnectorList() throws Exception {

        // only http connector present as a default

        Map<String, Set<String>> listeners = getListenerList();
        Set<String> listenerNames = listeners.get("http");
        assertEquals(1, listenerNames.size());
        assertTrue("HTTP connector missing.", listenerNames.contains("default"));
    }

    @Test
    public void testHttpConnector() throws Exception {

        addListener(Listener.HTTP);

        // check that the connector is live
        String cURL = "http://" + url.getHost() + ":8181";
        String response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);
        removeListener(Listener.HTTP, 5000);
    }

    @Test
    public void testHttpsConnector() throws Exception {

        addListener(Listener.HTTPS);

        // check that the connector is live
        try (CloseableHttpClient httpClient = TestHttpClientUtils.getHttpsClient(null)){
            String cURL = "https://" + url.getHost() + ":8181";
            HttpGet get = new HttpGet(cURL);

            HttpResponse hr = httpClient.execute(get);
            String response = EntityUtils.toString(hr.getEntity());
            assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);
        } finally {
            removeListener(Listener.HTTPS);
        }
    }

    @Test
    public void testAjpConnector() throws Exception {
        addListener(Listener.AJP);
        removeListener(Listener.AJP);
    }

    @Test
    public void testAddAndRemoveRollbacks() throws Exception {

        // execute and rollback add socket
        ModelNode addSocketOp = getAddSocketBindingOp(Listener.HTTP);
        ModelNode ret = executeAndRollbackOperation(addSocketOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // add socket again
        executeOperation(addSocketOp);

        // execute and rollback add connector
        ModelNode addConnectorOp = getAddListenerOp(Listener.HTTP, false);
        ret = executeAndRollbackOperation(addConnectorOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // add connector again
        executeOperation(addConnectorOp);

        // check it is listed
        assertTrue(getListenerList().get("http").contains("test-" + Listener.HTTP.getName() + "-listener"));

        // execute and rollback remove connector
        ModelNode removeConnOp = getRemoveConnectorOp(Listener.HTTP);
        ret = executeAndRollbackOperation(removeConnOp);
        assertEquals("failed", ret.get("outcome").asString());

        // execute remove connector again
        executeOperation(removeConnOp);

        Thread.sleep(1000);
        // check that the connector is not live
        String cURL = Listener.HTTP.getScheme() + "://" + url.getHost() + ":8181";

        assertFalse("Connector not removed.", WebUtil.testHttpURL(cURL));

        // execute and rollback remove socket binding
        ModelNode removeSocketOp = getRemoveSocketBindingOp(Listener.HTTP);
        ret = executeAndRollbackOperation(removeSocketOp);
        assertEquals("failed", ret.get("outcome").asString());

        // execute remove socket again
        executeOperation(removeSocketOp);
    }

    @Test
    public void testProxyProtocolOverHTTP() throws Exception {
        addListener(Listener.HTTP, true);
        try (Socket s = new Socket(url.getHost(), 8181)) {
            s.getOutputStream().write("PROXY TCP4 1.2.3.4 5.6.7.8 444 555\r\nGET /proxy/addr HTTP/1.0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            String result = FileUtils.readFile(s.getInputStream());
            Assert.assertTrue(result, result.contains("result:1.2.3.4:444 5.6.7.8:555"));
        } finally {
            removeListener(Listener.HTTP);
        }
    }


    @Test
    public void testProxyProtocolOverHTTPS() throws Exception {
        addListener(Listener.HTTPS, true);
        try (Socket s = new Socket(url.getHost(), 8181)) {
            s.getOutputStream().write("PROXY TCP4 1.2.3.4 5.6.7.8 444 555\r\n".getBytes(StandardCharsets.US_ASCII));
            Socket ssl = TestHttpClientUtils.getSslContext().getSocketFactory().createSocket(s, url.getHost(), HTTPS_PORT, true);
            ssl.getOutputStream().write("GET /proxy/addr HTTP/1.0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            String result = FileUtils.readFile(ssl.getInputStream());
            Assert.assertTrue(result, result.contains("result:1.2.3.4:444 5.6.7.8:555"));
        } finally {
            removeListener(Listener.HTTPS);
        }
    }

    private void addListener(Listener conn) throws Exception {
        addListener(conn, false);
    }

    private void addListener(Listener conn, boolean proxyProtocol) throws Exception {

        // add socket binding
        ModelNode op = getAddSocketBindingOp(conn);
        executeOperation(op);

        // add connector
        op = getAddListenerOp(conn, proxyProtocol);
        executeOperation(op);

        // check it is listed
        assertTrue(getListenerList().get(conn.getScheme()).contains("test-" + conn.getName() + "-listener"));
    }

    private ModelNode getAddSocketBindingOp(Listener conn) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName() + (++socketBindingCount), "add");
        op.get("port").set(8181);
        return op;
    }

    private ModelNode getAddListenerOp(Listener conn, boolean proxyProtocol) {
        final ModelNode composite = Util.getEmptyOperation(COMPOSITE, new ModelNode());
        final ModelNode steps = composite.get(STEPS);
        ModelNode op = createOpNode("subsystem=undertow/server=default-server/" + conn.getScheme() + "-listener=test-" + conn.getName() + "-listener", "add");
        op.get("socket-binding").set("test-" + conn.getName() + socketBindingCount);
        if(proxyProtocol) {
            op.get("proxy-protocol").set(true);
        }
        if (conn.isSecure()) {
            op.get("security-realm").set("ssl-realm");
        }
        steps.add(op);
        return composite;
    }

    private void removeListener(Listener conn) throws Exception {
        removeListener(conn, 300);
    }

    private void removeListener(Listener conn, int delay) throws Exception {

        // remove connector
        ModelNode op = getRemoveConnectorOp(conn);
        executeOperation(op);

        Thread.sleep(delay);
        // check that the connector is not live
        String cURL = conn.getScheme() + "://" + url.getHost() + ":8181";

        assertFalse("Listener not removed.", WebUtil.testHttpURL(cURL));

        // remove socket binding
        op = getRemoveSocketBindingOp(conn);
        executeOperation(op);

    }

    private ModelNode getRemoveSocketBindingOp(Listener conn) {
        return createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName() + socketBindingCount, "remove");
    }

    private ModelNode getRemoveConnectorOp(Listener conn) {
        ModelNode op = createOpNode("subsystem=undertow/server=default-server/" + conn.getScheme() + "-listener=test-" + conn.getName() + "-listener", "remove");
        op.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        return op;
    }

    private Map<String, Set<String>> getListenerList() throws Exception {
        HashMap<String, Set<String>> result = new HashMap<>();
        result.put("http", getListenersByType("http-listener"));
        result.put("https", getListenersByType("https-listener"));
        result.put("ajp", getListenersByType("ajp-listener"));

        return result;
    }

    private Set<String> getListenersByType(String type) throws Exception {
        ModelNode op = createOpNode("subsystem=undertow/server=default-server", "read-children-names");
        op.get("child-type").set(type);
        ModelNode result = executeOperation(op);
        List<ModelNode> connectors = result.asList();
        HashSet<String> connNames = new HashSet<>();
        for (ModelNode n : connectors) {
            connNames.add(n.asString());

        }
        return connNames;
    }

    static class SecurityRealmsSetup extends AbstractSecurityRealmsServerSetupTask {
        @Override
        protected SecurityRealm[] getSecurityRealms() throws Exception {
            setUpKeyStores();
            URL keystoreResource = Thread.currentThread().getContextClassLoader().getResource("security/server.keystore");
            URL truststoreResource = Thread.currentThread().getContextClassLoader().getResource("security/jsse.keystore");

            RealmKeystore keystore = new RealmKeystore.Builder()
                    .keystorePassword("changeit")
                    .keystorePath(keystoreResource.getPath())
                    .build();

            RealmKeystore truststore = new RealmKeystore.Builder()
                    .keystorePassword("changeit")
                    .keystorePath(truststoreResource.getPath())
                    .build();

            return new SecurityRealm[]{new SecurityRealm.Builder()
                    .name("ssl-realm")
                    .serverIdentity(
                            new ServerIdentity.Builder()
                                    .ssl(keystore)
                                    .build())
                    .authentication(
                            new Authentication.Builder()
                                    .truststore(truststore)
                                    .build()
                    )
                    .build()};
        }
    }
}
