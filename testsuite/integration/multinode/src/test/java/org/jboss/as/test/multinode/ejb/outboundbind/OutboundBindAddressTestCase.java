/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.multinode.ejb.outboundbind;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that outbound-bind-address configuration is honored when EJBs make outbound calls.
 * Uses multinode setup: server deployment provides SourceAddressBean, client deployment
 * provides CallerBean which calls SourceAddressBean via remoting.
 *
 * @author <a href="mailto:egonzalez@redhat.com">Eduardo Gonzalez</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(OutboundBindAddressTestCase.ServerSetupTask.class)
public class OutboundBindAddressTestCase {

    private static final String ARCHIVE_NAME_CLIENT = "outbound-bind-address-client";
    private static final String ARCHIVE_NAME_SERVER = "outbound-bind-address-server";

    private static final String SOCKET_NAME = "self-ejb-socket";
    private static final String CONNECTION_NAME = "self-remote-ejb-connection";
    private static final String PROFILE_NAME = "test-profile";
    private static final String RECEIVER_NAME = "test-receiver";

    private static final String BIND_ADDRESS;
    private static final int BIND_PORT;
    private static final boolean CAN_TEST_SPECIFIC_BIND_ADDRESS;

    static {
        String configuredAddress = System.getProperty("test.bind.address");
        String configuredPort = System.getProperty("test.bind.port");

        if (configuredAddress != null) {
            BIND_ADDRESS = configuredAddress;
            BIND_PORT = configuredPort != null ? Integer.parseInt(configuredPort) : 0;
            CAN_TEST_SPECIFIC_BIND_ADDRESS = !BIND_ADDRESS.equals("127.0.0.1");
            System.out.println("OutboundBindAddressTestCase: Using system property configuration");
            System.out.println("  test.bind.address = " + BIND_ADDRESS);
            System.out.println("  test.bind.port = " + BIND_PORT);
        } else {
            List<String> loopbackAddresses = new ArrayList<>();
            try {
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isLoopback()) {
                        Enumeration<InetAddress> addresses = ni.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (addr.getAddress().length == 4) {
                                loopbackAddresses.add(addr.getHostAddress());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to enumerate network interfaces: " + e.getMessage());
            }

            System.out.println("OutboundBindAddressTestCase: Auto-detecting network interfaces");
            System.out.println("  Available IPv4 loopback addresses: " + loopbackAddresses);

            String selectedAddress = "127.0.0.1";
            boolean canTestSpecific = false;

            for (String addr : loopbackAddresses) {
                if (!addr.equals("127.0.0.1")) {
                    selectedAddress = addr;
                    canTestSpecific = true;
                    break;
                }
            }

            BIND_ADDRESS = selectedAddress;
            BIND_PORT = 0;
            CAN_TEST_SPECIFIC_BIND_ADDRESS = canTestSpecific;

            if (canTestSpecific) {
                System.out.println("  Selected alternative address: " + BIND_ADDRESS);
            } else {
                System.out.println("  Using default address: " + BIND_ADDRESS);
            }
        }

        System.out.println("OutboundBindAddressTestCase: Configuration complete");
        System.out.println("  BIND_ADDRESS = " + BIND_ADDRESS);
        System.out.println("  BIND_PORT = " + BIND_PORT);
        System.out.println("  CAN_TEST_SPECIFIC_BIND_ADDRESS = " + CAN_TEST_SPECIFIC_BIND_ADDRESS);
    }

    static class ServerSetupTask implements org.jboss.as.arquillian.api.ServerSetupTask {

        private static final PathAddress ADDR_REMOTING_PROFILE = PathAddress.pathAddress()
                .append(SUBSYSTEM, "ejb3")
                .append("remoting-profile", PROFILE_NAME);
        private static final PathAddress ADDR_REMOTING_EJB_RECEIVER = ADDR_REMOTING_PROFILE
                .append("remoting-ejb-receiver", RECEIVER_NAME);

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            if ("multinode-client".equals(containerId)) {
                System.out.println("Setup: Configuring multinode-client for outbound bind address test");

                final ModelNode compositeOp = new ModelNode();
                compositeOp.get(OP).set(COMPOSITE);
                compositeOp.get(OP_ADDR).setEmptyList();
                ModelNode steps = compositeOp.get(STEPS);

                // Create socket binding pointing to server
                ModelNode addSocket = Util.createAddOperation(
                        PathAddress.pathAddress()
                                .append("socket-binding-group", "standard-sockets")
                                .append("remote-destination-outbound-socket-binding", SOCKET_NAME));
                addSocket.get("host").set("${node1:127.0.0.1}");
                addSocket.get("port").set("${node1.port:8180}");
                steps.add(addSocket);

                // Create remoting outbound connection
                ModelNode addConn = Util.createAddOperation(
                        PathAddress.pathAddress()
                                .append(SUBSYSTEM, "remoting")
                                .append("remote-outbound-connection", CONNECTION_NAME));
                addConn.get("outbound-socket-binding-ref").set(SOCKET_NAME);
                steps.add(addConn);

                // Create EJB3 remoting profile
                ModelNode addProfile = Util.createAddOperation(ADDR_REMOTING_PROFILE);
                steps.add(addProfile);

                // Add remoting-ejb-receiver to profile
                ModelNode addReceiver = Util.createAddOperation(ADDR_REMOTING_EJB_RECEIVER);
                addReceiver.get("outbound-connection-ref").set(CONNECTION_NAME);
                steps.add(addReceiver);

                // Create outbound-bind-address
                ModelNode addBind = Util.createAddOperation(
                        PathAddress.pathAddress()
                                .append(SUBSYSTEM, "io")
                                .append("worker", "default")
                                .append("outbound-bind-address", "default"));
                addBind.get("bind-address").set(BIND_ADDRESS);
                addBind.get("bind-port").set(BIND_PORT);
                addBind.get("match").set("0.0.0.0/0");
                steps.add(addBind);

                Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
                ServerReload.reloadIfRequired(managementClient);

                System.out.println("Setup: Configuration complete for multinode-client");
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            if ("multinode-client".equals(containerId)) {
                final ModelNode compositeOp = new ModelNode();
                compositeOp.get(OP).set(COMPOSITE);
                compositeOp.get(OP_ADDR).setEmptyList();
                ModelNode steps = compositeOp.get(STEPS);

                steps.add(Util.createRemoveOperation(
                        PathAddress.pathAddress()
                                .append(SUBSYSTEM, "io")
                                .append("worker", "default")
                                .append("outbound-bind-address", "default")));
                steps.add(Util.createRemoveOperation(ADDR_REMOTING_PROFILE));
                steps.add(Util.createRemoveOperation(
                        PathAddress.pathAddress()
                                .append(SUBSYSTEM, "remoting")
                                .append("remote-outbound-connection", CONNECTION_NAME)));
                steps.add(Util.createRemoveOperation(
                        PathAddress.pathAddress()
                                .append("socket-binding-group", "standard-sockets")
                                .append("remote-destination-outbound-socket-binding", SOCKET_NAME)));

                Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
                ServerReload.reloadIfRequired(managementClient);
            }
        }
    }

    @Deployment(name = "server")
    @TargetsContainer("multinode-server")
    public static Archive<?> deployment0() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SERVER + ".jar");
        jar.addClasses(SourceAddressBean.class, SourceAddressRemote.class);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_CLIENT + ".jar");
        jar.addClasses(CallerBean.class, CallerRemote.class, SourceAddressRemote.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-outboundbind.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testOutboundBindAddressIsHonored(@ArquillianResource InitialContext ctx) throws Exception {
        System.out.println("OutboundBindAddressTestCase: Starting test");
        System.out.println("  Configured bind address: " + BIND_ADDRESS + ":" + BIND_PORT);

        CallerRemote caller = (CallerRemote) ctx.lookup("java:module/" + CallerBean.class.getSimpleName() + "!"
                + CallerRemote.class.getName());
        Assert.assertNotNull(caller);

        String sourceAddress = caller.callAndGetSourceAddress();
        System.out.println("  Source address seen by server: " + sourceAddress);

        Assert.assertNotNull("Should successfully retrieve source address", sourceAddress);

        if (CAN_TEST_SPECIFIC_BIND_ADDRESS) {
            System.out.println("  Validating specific bind address is honored");
            Assert.assertTrue("Expected configured bind address " + BIND_ADDRESS + " in source address, got: " + sourceAddress,
                    sourceAddress.contains(BIND_ADDRESS));
            System.out.println("  SUCCESS: Specific bind address " + BIND_ADDRESS + " is honored");
        } else {
            System.out.println("  Running smoke test (loopback validation only)");
            Assert.assertTrue("Expected loopback address, got: " + sourceAddress,
                    sourceAddress.contains("127.0.0.1") || sourceAddress.contains("localhost"));
            System.out.println("  SUCCESS: Connection works with outbound-bind-address configured");
        }
    }
}
