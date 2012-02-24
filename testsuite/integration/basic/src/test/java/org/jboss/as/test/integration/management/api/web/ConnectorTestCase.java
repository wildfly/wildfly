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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.http.util.HttpClientUtils;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.Connector;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ConnectorTestCase extends AbstractMgmtTestBase {

    private final File keyStoreFile = new File(System.getProperty("java.io.tmpdir"), "test.keystore");
    private final File keyPEMFile = new File(System.getProperty("java.io.tmpdir"), "newkey.pem");
    private final File certPEMFile = new File(System.getProperty("java.io.tmpdir"), "newcert.pem");
    private boolean isNative = false;

    @ArquillianResource
    private URL url;

    /**
     * We use a different socket binding name for each test, as if the socket is still up the service
     * will not be removed. Rather than adding a sleep we use this approach
     */
    private static int socketBindingCount = 0;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }

    @Before
    public void before() throws IOException {
        try {
            addConnector(Connector.HTTPNATIVE);
            isNative = true;
            removeConnector(Connector.HTTPNATIVE);
        } catch (Exception ex) {
            // Assume native not enable on installed.

            try {
                ModelNode op = getRemoveSocketBindingOp(Connector.HTTPNATIVE);
                executeOperation(op);
            } catch (Exception e) {
                // hmmm
            }
        }
    }

    @Test
    public void testDefaultConnectorList() throws Exception {

        // only http connector present as a default

        HashSet<String> connNames = getConnectorList();

        assertTrue("HTTP connector missing.", connNames.contains("http"));
        assertTrue(connNames.size() == 1);
    }

    @Test
    public void testHttpConnector() throws Exception {

        addConnector(Connector.HTTP);

        // check that the connector is live
        String cURL = "http://" + url.getHost() + ":8181";
        String response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
        assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);
        removeConnector(Connector.HTTP);
    }

    @Test
    public void testHttpOtherConnector() throws Exception {
        if (isNative) {
            /* test the standard JIO connector too */
            addConnector(Connector.HTTPJIO);

            // check that the connector is live
            String cURL = "http://" + url.getHost() + ":8181";
            String response = HttpRequest.get(cURL, 10, TimeUnit.SECONDS);
            assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);
            removeConnector(Connector.HTTPJIO);
        }
    }

    @Test
    public void testHttpsConnector() throws Exception {

        FileUtils.copyURLToFile(ConnectorTestCase.class.getResource("test.keystore"), keyStoreFile);

        if (isNative)
            addConnector(Connector.HTTPSJIO);
        else
            addConnector(Connector.HTTPS);

        // check that the connector is live
        String cURL = "https://" + url.getHost() + ":8181";
        HttpClient httpClient = HttpClientUtils.wrapHttpsClient(new DefaultHttpClient());
        HttpGet get = new HttpGet(cURL);

        HttpResponse hr = httpClient.execute(get);
        String response = EntityUtils.toString(hr.getEntity());
        assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);

        if (isNative)
            removeConnector(Connector.HTTPSJIO);
        else
            removeConnector(Connector.HTTPS);

        if (keyStoreFile.exists())
            keyStoreFile.delete();
    }

    @Test
    public void testHttpsNativeConnector() throws Exception {
        if (isNative) {
            FileUtils.copyURLToFile(ConnectorTestCase.class.getResource("newkey.pem"), keyPEMFile);
            FileUtils.copyURLToFile(ConnectorTestCase.class.getResource("newcert.pem"), certPEMFile);
            addConnector(Connector.HTTPSNATIVE);
            // check that the connector is live
            String cURL = "https://" + url.getHost() + ":8181";
            HttpClient httpClient = HttpClientUtils.wrapHttpsClient(new DefaultHttpClient());
            HttpGet get = new HttpGet(cURL);

            HttpResponse hr = httpClient.execute(get);
            String response = EntityUtils.toString(hr.getEntity());
            assertTrue("Invalid response: " + response, response.indexOf("JBoss") >= 0);
            removeConnector(Connector.HTTPSNATIVE);

            if (keyPEMFile.exists())
                keyPEMFile.delete();
            if (certPEMFile.exists())
                certPEMFile.delete();
        }
    }

    @Test
    public void testAjpConnector() throws Exception {
        addConnector(Connector.AJP);
        removeConnector(Connector.AJP);
        if (isNative) {
            /* Test the JIO connector too */
            addConnector(Connector.AJPJIO);
            removeConnector(Connector.AJPJIO);
        }
    }

    @Test
    public void testAddAndRemoveRollbacks() throws Exception {

        // execute and rollback add socket
        ModelNode addSocketOp = getAddSocketBindingOp(Connector.HTTP);
        ModelNode ret = executeAndRollbackOperation(addSocketOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // add socket again
        executeOperation(addSocketOp);

        // execute and rollback add connector
        ModelNode addConnectorOp = getAddConnectorOp(Connector.HTTP);
        ret = executeAndRollbackOperation(addConnectorOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // add connector again
        executeOperation(addConnectorOp);

        // check it is listed
        assertTrue(getConnectorList().contains("test-" + Connector.HTTP.getName() + "-connector"));

        // execute and rollback remove connector
        ModelNode removeConnOp = getRemoveConnectorOp(Connector.HTTP);
        ret = executeAndRollbackOperation(removeConnOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // execute remove connector again
        executeOperation(removeConnOp);

        Thread.sleep(5000);
        // check that the connector is not live
        String cURL = Connector.HTTP.getScheme() + "://" + url.getHost() + ":8181";

        assertFalse("Connector not removed.", WebUtil.testHttpURL(cURL));

        // execute and rollback remove socket binding
        ModelNode removeSocketOp = getRemoveSocketBindingOp(Connector.HTTP);
        ret = executeAndRollbackOperation(removeSocketOp);
        assertTrue("failed".equals(ret.get("outcome").asString()));

        // execute remove socket again
        executeOperation(removeSocketOp);
    }

    private void addConnector(Connector conn) throws Exception {

        // add socket binding
        ModelNode op = getAddSocketBindingOp(conn);
        executeOperation(op);

        // add connector
        op = getAddConnectorOp(conn);
        executeOperation(op);

        // check it is listed
        assertTrue(getConnectorList().contains("test-" + conn.getName() + "-connector"));
    }

    private ModelNode getAddSocketBindingOp(Connector conn) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName() + (++socketBindingCount), "add");
        op.get("port").set(8181);
        return op;
    }

    private ModelNode getAddConnectorOp(Connector conn) {
        ModelNode op = createOpNode("subsystem=web/connector=test-" + conn.getName() + "-connector", "add");
        op.get("socket-binding").set("test-" + conn.getName() + socketBindingCount);
        op.get("scheme").set(conn.getScheme());
        op.get("protocol").set(conn.getProtocol());
        op.get("secure").set(conn.isSecure());
        op.get("enabled").set(true);
        if (conn.isSecure()) {
            ModelNode ssl = new ModelNode();
            if (conn.equals(Connector.HTTPSNATIVE)) {
                ssl.get("certificate-key-file").set(keyPEMFile.getAbsolutePath());
                ssl.get("certificate-file").set(certPEMFile.getAbsolutePath());
            } else {
                ssl.get("certificate-key-file").set(keyStoreFile.getAbsolutePath());
            }
            ssl.get("password").set("test123");
            op.get("ssl").set(ssl);
        }
        return op;
    }

    private void removeConnector(Connector conn) throws Exception {

        // remove connector
        ModelNode op = getRemoveConnectorOp(conn);
        executeOperation(op);

        Thread.sleep(5000);
        // check that the connector is not live
        String cURL = conn.getScheme() + "://" + url.getHost() + ":8181";

        assertFalse("Connector not removed.", WebUtil.testHttpURL(cURL));

        // remove socket binding
        op = getRemoveSocketBindingOp(conn);
        executeOperation(op);

    }

    private ModelNode getRemoveSocketBindingOp(Connector conn) {
        ModelNode op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test-" + conn.getName() + socketBindingCount, "remove");
        return op;
    }

    private ModelNode getRemoveConnectorOp(Connector conn) {
        ModelNode op = createOpNode("subsystem=web/connector=test-" + conn.getName() + "-connector", "remove");
        return op;
    }

    private HashSet<String> getConnectorList() throws Exception {

        ModelNode op = createOpNode("subsystem=web", "read-children-names");
        op.get("child-type").set("connector");
        ModelNode result = executeOperation(op);
        List<ModelNode> connectors = result.asList();
        HashSet<String> connNames = new HashSet<String>();
        for (ModelNode n : connectors) {
            connNames.add(n.asString());
        }

        return connNames;
    }

}
