/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.management.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.concurrent.Callable;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.WebUtil;
import org.jboss.as.test.shared.RetryTaskExecutor;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.as.test.shared.ServerSnapshot;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class SocketsAndInterfacesTestCase extends ContainerResourceMgmtTestBase {

    private static final Logger logger = Logger.getLogger(SocketsAndInterfacesTestCase.class);

    @ArquillianResource
    URL url;

    private NetworkInterface testNic;
    private String testHost;
    private static final int TEST_PORT = 9695;

    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "SocketsAndInterfacesTestCase-dummy.jar");
        ja.addClass(SocketsAndInterfacesTestCase.class);
        return ja;
    }

    private AutoCloseable snapshot;

    @Before
    public void before() throws IOException {
        snapshot = ServerSnapshot.takeSnapshot(getManagementClient());
        testHost = System.getProperty("node0");
        testNic = getNic(testHost);
        assumeFalse("No usable nic '" + testHost + "' is available", testNic == null);

    }

    @After
    public void after() throws Exception {
        snapshot.close();
        snapshot = null;
    }

    @Test
    public void testAddUpdateRemove() throws Exception {

        // add interface
        ModelNode op = createOpNode("interface=test123-interface", ADD);
        op.get("nic").set(testNic.getName());
        op.get("inet-address").set(testHost);
        executeOperation(op);

        // add socket binding using created interface
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test123-binding", ADD);
        op.get("interface").set("test123-interface");
        op.get("port").set(TEST_PORT);
        executeOperation(op);


        // add a web connector so we can test the interface
        op = createOpNode("subsystem=undertow/server=default-server/http-listener=test", ADD);
        op.get("socket-binding").set("test123-binding");
        ModelNode result = executeOperation(op);

        final URL url = new URL("http", testHost, TEST_PORT, "/");
        Assert.assertTrue("Could not connect to created connector: " + url + "<>" + InetAddress.getByName(url.getHost()) + "..." + testNic + ".>" + result, WebUtil.testHttpURL(url.toString()));

        // change socket binding port
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test123-binding", WRITE_ATTRIBUTE_OPERATION);
        op.get(NAME).set("port");
        op.get(VALUE).set(TEST_PORT + 1);
        result = executeOperation(op, false);
        Assert.assertEquals(result.asString(), "reload-required", result.get(RESPONSE_HEADERS).get(PROCESS_STATE).asString());

        logger.trace("Restarting server.");

        ServerReload.executeReloadAndWaitForCompletion(getManagementClient());

        // wait until the connector is available on the new port
        final String testUrl = new URL("http", testHost, TEST_PORT + 1, "/").toString();
        RetryTaskExecutor<Boolean> rte = new RetryTaskExecutor<Boolean>();
        rte.retryTask(new Callable<>() {
            public Boolean call() throws Exception {
                boolean available = WebUtil.testHttpURL(testUrl);
                if (!available) throw new Exception("Connector not available.");
                return available;
            }
        });

        logger.trace("Server is up.");

        // check the connector is not listening on the old port
        Assert.assertFalse("Could not connect to created connector.", WebUtil.testHttpURL(new URL(
                "http", testHost, TEST_PORT, "/").toString()));

        // try to remove the interface while the socket binding is still  bound to it - should fail
        op = createOpNode("interface=test123-interface", REMOVE);
        result = executeOperation(op, false);
        Assert.assertNotEquals("Removed interface with socket binding bound to it.", SUCCESS, result.get(OUTCOME).asString());

        // try to remove socket binding while the connector is still using it - should fail
        op = createOpNode("socket-binding-group=standard-sockets/socket-binding=test123-binding", REMOVE);
        result = executeOperation(op, false);
        Assert.assertNotEquals("Removed socked binding with connector still using it.", SUCCESS, result.get(OUTCOME).asString());
    }

    private NetworkInterface getNic(String node) throws SocketException, UnknownHostException {
        if (node == null) {
            return null;
        }
        InetAddress node1Address = InetAddress.getByName(node);
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface nic = interfaces.nextElement();
            if (!nic.isUp()) {
                continue;
            }
            for (InterfaceAddress addr : nic.getInterfaceAddresses()) {
                if (addr.getAddress().equals(node1Address)) {
                    return nic;
                }
            }
        }
        return null; // no interface found
    }
}
