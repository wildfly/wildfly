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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Function;

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
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@ServerSetup(OutboundBindAddressTestCase.ServerSetupTask.class)
public class OutboundBindAddressTestCase {

    private static final Logger log = Logger.getLogger(OutboundBindAddressTestCase.class);

    private static final String ARCHIVE_NAME_CLIENT = "outbound-bind-address-client";
    private static final String ARCHIVE_NAME_SERVER = "outbound-bind-address-server";

    private static final String SOCKET_NAME = "self-ejb-socket";
    private static final String CONNECTION_NAME = "self-remote-ejb-connection";
    private static final String PROFILE_NAME = "test-profile";
    private static final String RECEIVER_NAME = "test-receiver";

    private static final String BIND_ADDRESS;
    private static final String MATCH_PATTERN;
    private static final int BIND_PORT = 0;
    private static final boolean CAN_TEST_SPECIFIC_BIND_ADDRESS;
    private static final boolean IS_IPV6_MODE;
    private static final String DEFAULT_IPV4_LOOPBACK = "127.0.0.1";
    private static final String DEFAULT_IPV6_LOOPBACK = "::1";

    static {
        // Detect IPv4 vs IPv6 mode from system properties
        boolean preferIPv4Stack = Boolean.getBoolean("java.net.preferIPv4Stack");
        boolean preferIPv6Addresses = Boolean.getBoolean("java.net.preferIPv6Addresses");
        IS_IPV6_MODE = !preferIPv4Stack && preferIPv6Addresses;

        // Detect bindable address based on mode
        AddressDetectionResult result;
        if (IS_IPV6_MODE) {
            result = detectBindableAddress(
                "IPv6",
                DEFAULT_IPV6_LOOPBACK,
                new String[]{"::2", "::3"},
                addr -> {
                    if (addr instanceof Inet6Address ipv6Addr && !ipv6Addr.isLinkLocalAddress()) {
                        try {
                            // Get address without scope identifier
                            return InetAddress.getByAddress(addr.getAddress()).getHostAddress();
                        } catch (Exception e) {
                            return addr.getHostAddress();
                        }
                    }
                    return null;
                },
                "::/0"
            );
        } else {
            result = detectBindableAddress(
                "IPv4",
                DEFAULT_IPV4_LOOPBACK,
                new String[]{"127.0.0.2", "127.0.0.3"},
                addr -> addr instanceof Inet4Address ? addr.getHostAddress() : null,
                "0.0.0.0/0"
            );
        }

        BIND_ADDRESS = result.address;
        MATCH_PATTERN = result.matchPattern;
        CAN_TEST_SPECIFIC_BIND_ADDRESS = result.canTest;
    }

    /**
     * Helper class to hold address detection results.
     */
    private static class AddressDetectionResult {
        final String address;
        final String matchPattern;
        final boolean canTest;

        AddressDetectionResult(String address, String matchPattern, boolean canTest) {
            this.address = address;
            this.matchPattern = matchPattern;
            this.canTest = canTest;
        }
    }

    /**
     * Detects a bindable loopback address for testing.
     *
     * @param addressFamily Human-readable address family name (IPv4 or IPv6)
     * @param defaultLoopback The default loopback address
     * @param fallbackCandidates Additional addresses to try (bindable but not always enumerated)
     * @param processAddress Lambda to process addresses (returns address string or null if not applicable)
     * @param matchPattern The CIDR pattern for outbound-bind-address configuration
     * @return AddressDetectionResult containing the selected address and test capability
     */
    private static AddressDetectionResult detectBindableAddress(
            String addressFamily,
            String defaultLoopback,
            String[] fallbackCandidates,
            Function<InetAddress, String> processAddress,
            String matchPattern) {

        // Enumerate available loopback addresses
        List<String> loopbackAddresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback()) {
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        String processedAddress = processAddress.apply(addr);
                        if (processedAddress != null) {
                            loopbackAddresses.add(processedAddress);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to enumerate network interfaces: " + e.getMessage());
        }

        // Add fallback candidates (bindable even if not enumerated)
        for (String candidate : fallbackCandidates) {
            if (!loopbackAddresses.contains(candidate)) {
                loopbackAddresses.add(candidate);
            }
        }

        log.info("Detecting " + addressFamily + " loopback addresses");
        log.info("Candidate addresses: " + loopbackAddresses);

        // Normalize default loopback for comparison (handles IPv6 compressed vs expanded forms)
        String normalizedDefault = normalizeAddress(defaultLoopback);

        // Find first bindable address that's not the default loopback
        String selectedAddress = defaultLoopback;
        boolean canTestSpecific = false;

        for (String addr : loopbackAddresses) {
            if (!normalizeAddress(addr).equals(normalizedDefault)) {
                // Try to bind to verify the address is actually usable
                if (canBind(addr)) {
                    selectedAddress = addr;
                    canTestSpecific = true;
                    log.info("Selected alternative address: " + selectedAddress);
                    break;
                }
            }
        }

        if (!canTestSpecific) {
            log.info("No alternative " + addressFamily + " loopback address available - test will be skipped");
        }

        return new AddressDetectionResult(selectedAddress, matchPattern, canTestSpecific);
    }

    /**
     * Normalizes an IP address string for comparison.
     * This ensures IPv6 addresses in different formats (compressed vs expanded) are treated as equal.
     */
    private static String normalizeAddress(String address) {
        try {
            return InetAddress.getByName(address).getHostAddress();
        } catch (Exception e) {
            return address;
        }
    }

    private static boolean canBind(String address) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(address, 0));
            return true;
        } catch (IOException e) {
            log.info("Cannot bind to " + address + ": " + e.getMessage());
            return false;
        }
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
                // Skip setup if test won't run (no alternative loopback address available)
                if (!CAN_TEST_SPECIFIC_BIND_ADDRESS) {
                    log.info("Setup: Skipping configuration - test will be skipped (no alternative loopback address)");
                    return;
                }

                log.info("Setup: Configuring multinode-client for outbound bind address test");

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
                addBind.get("match").set(MATCH_PATTERN);
                steps.add(addBind);

                Utils.applyUpdates(Collections.singletonList(compositeOp), managementClient.getControllerClient());
                ServerReload.reloadIfRequired(managementClient);

                log.info("Setup: Configuration complete for multinode-client");
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            if ("multinode-client".equals(containerId)) {
                // Skip teardown if setup was skipped (no alternative loopback address available)
                if (!CAN_TEST_SPECIFIC_BIND_ADDRESS) {
                    log.info("TearDown: Skipping cleanup - setup was skipped");
                    return;
                }

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
        // Return empty jar if test won't run to avoid dependency failures
        if (!CAN_TEST_SPECIFIC_BIND_ADDRESS) {
            return AssumeTestGroupUtil.emptyJar(ARCHIVE_NAME_SERVER);
        }
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_SERVER + ".jar");
        jar.addClasses(SourceAddressBean.class, SourceAddressRemote.class);
        return jar;
    }

    @Deployment(name = "client")
    @TargetsContainer("multinode-client")
    public static Archive<?> deployment1() {
        // Return empty jar if test won't run to avoid dependency failures
        if (!CAN_TEST_SPECIFIC_BIND_ADDRESS) {
            return AssumeTestGroupUtil.emptyJar(ARCHIVE_NAME_CLIENT);
        }
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME_CLIENT + ".jar");
        jar.addClasses(CallerBean.class, CallerRemote.class, SourceAddressRemote.class);
        jar.addAsManifestResource("META-INF/jboss-ejb-client-outboundbind.xml", "jboss-ejb-client.xml");
        return jar;
    }

    @Test
    @OperateOnDeployment("client")
    public void testOutboundBindAddressIsHonored(@ArquillianResource InitialContext ctx) throws Exception {
        log.info("Starting test");
        log.info("Configured bind address: " + BIND_ADDRESS + ":" + BIND_PORT);

        // Skip test if no alternative loopback address is available
        // This ensures the test explicitly signals when it cannot properly validate the bind address feature
        String addressFamily = IS_IPV6_MODE ? "IPv6" : "IPv4";
        String defaultLoopback = IS_IPV6_MODE ? DEFAULT_IPV6_LOOPBACK : DEFAULT_IPV4_LOOPBACK;
        String bindableRange = IS_IPV6_MODE ? "::1-::ffff" : "127.0.0.0/8";
        Assume.assumeTrue(
            "No alternative " + addressFamily + " loopback address available on this system. " +
            "Only " + defaultLoopback + " is present. Cannot fully validate outbound-bind-address behavior. " +
            "To enable this test, ensure the " + bindableRange + " range is bindable (typically default on Linux).",
            CAN_TEST_SPECIFIC_BIND_ADDRESS);

        CallerRemote caller = (CallerRemote) ctx.lookup("java:module/" + CallerBean.class.getSimpleName() + "!"
                + CallerRemote.class.getName());
        Assert.assertNotNull(caller);

        String sourceAddress = caller.callAndGetSourceAddress();
        log.info("Source address seen by server: " + sourceAddress);

        Assert.assertNotNull("Should successfully retrieve source address", sourceAddress);

        log.info("Validating specific bind address is honored");
        Assert.assertTrue("Expected configured bind address " + BIND_ADDRESS + " in source address, got: " + sourceAddress,
                sourceAddress.contains(BIND_ADDRESS));
        log.info("SUCCESS: Specific bind address " + BIND_ADDRESS + " is honored");
    }
}
