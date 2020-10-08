/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.msc.service.ServiceName;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Test for Server service created from xml file.
 *
 * @author Flavia Rainone
 */
public class ServerServiceTestCase extends AbstractUndertowSubsystemTestCase {

    private static final String NODE_NAME = "node-name";
    private static final String DEFAULT_SERVER = "default-server";
    private static final String DEFAULT_VIRTUAL_HOST = "default-host";
    private static final String UNDERTOW_SERVER = "undertow-server";

    public ServerServiceTestCase() {
        super(UndertowSubsystemSchema.CURRENT);
    }

    @Override
    public void setUp() {
        super.setUp();
        System.setProperty("jboss.node.name", NODE_NAME);
    }

    private Server load(String xmlFile, String serverName) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new RuntimeInitialization(this.values)).setSubsystemXml(readResource(xmlFile));
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        final ServiceName undertowServerName = ServerDefinition.SERVER_CAPABILITY.getCapabilityServiceName(serverName);
        Server server = (Server) this.values.get(undertowServerName).get();
        assertNotNull(server);
        return server;
    }

    @Test
    public void testUndefinedAttributes() throws Exception {
        final Server server = load("undertow-service-undefined-attributes.xml", DEFAULT_SERVER);
        assertEquals(DEFAULT_SERVER, server.getName());
        Set<Host> hosts = server.getHosts();
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        Host host = hosts.iterator().next();
        assertNotNull(host);
        assertEquals(DEFAULT_VIRTUAL_HOST, host.getName());
        assertSame(server, host.getServer());
        assertEquals(DEFAULT_VIRTUAL_HOST, server.getDefaultHost());
        assertEquals(NODE_NAME, server.getRoute());
    }

    @Test
    public void testDefinedDefaultAttributes1() throws Exception {
        final Server server = load("undertow-service-defined-default-attributes1.xml", DEFAULT_SERVER);
        assertEquals(DEFAULT_SERVER, server.getName());
        Set<Host> hosts = server.getHosts();
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        Host host = hosts.iterator().next();
        assertNotNull(host);
        assertEquals(DEFAULT_VIRTUAL_HOST, host.getName());
        assertSame(server, host.getServer());
        assertEquals(DEFAULT_VIRTUAL_HOST, server.getDefaultHost());
        assertEquals(NODE_NAME, server.getRoute());
    }

    @Test
    public void testDefinedDefaultAttributes2() throws Exception {
        final Server server = load("undertow-service-defined-default-attributes2.xml", DEFAULT_SERVER);
        assertEquals(DEFAULT_SERVER, server.getName());
        Set<Host> hosts = server.getHosts();
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        Host host = hosts.iterator().next();
        assertNotNull(host);
        assertEquals(DEFAULT_VIRTUAL_HOST, host.getName());
        assertSame(server, host.getServer());
        assertEquals(DEFAULT_VIRTUAL_HOST, server.getDefaultHost());
        assertEquals(NODE_NAME, server.getRoute());
    }

    @Test
    public void testDefinedAttributes() throws Exception {
        final Server server = load("undertow-service-defined-attributes.xml", UNDERTOW_SERVER);
        assertEquals(UNDERTOW_SERVER, server.getName());
        Set<Host> hosts = server.getHosts();
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        Host host = hosts.iterator().next();
        assertNotNull(host);
        assertEquals("virtual-host", host.getName());
        assertSame(server, host.getServer());
        assertEquals("virtual-host", server.getDefaultHost());
        assertEquals(NODE_NAME + "-undertow", server.getRoute());
    }

    @Test
    public void testObfuscateInstanceId1() throws Exception {
        final Server server = load("undertow-service-obfuscate-instance-id-attribute1.xml", UNDERTOW_SERVER);
        assertEquals(UNDERTOW_SERVER, server.getName());
        final Set<Host> hosts = server.getHosts();
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        final Host host = hosts.iterator().next();
        assertNotNull(host);
        assertEquals("virtual-host3", host.getName());
        assertSame(server, host.getServer());
        assertEquals("virtual-host3", server.getDefaultHost());

        final MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(UNDERTOW_SERVER.getBytes(StandardCharsets.UTF_8));
        final byte[] md5Bytes = md.digest(NODE_NAME.getBytes(StandardCharsets.UTF_8));
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        assertEquals(new String(encoder.encode(md5Bytes), StandardCharsets.UTF_8), server.getRoute());
    }

    @Test
    public void testObfuscateInstanceId2() throws Exception {
        final Server server = load("undertow-service-obfuscate-instance-id-attribute2.xml", UNDERTOW_SERVER);
        assertEquals(UNDERTOW_SERVER, server.getName());
        final Set<Host> hosts = server.getHosts();
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        final Host host = hosts.iterator().next();
        assertNotNull(host);
        assertEquals("virtual-host3", host.getName());
        assertSame(server, host.getServer());
        assertEquals("virtual-host3", server.getDefaultHost());

        final MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(UNDERTOW_SERVER.getBytes(StandardCharsets.UTF_8));
        final byte[] md5Bytes = md.digest("my-undertow-instance".getBytes(StandardCharsets.UTF_8));
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        assertEquals(new String(encoder.encode(md5Bytes), StandardCharsets.UTF_8), server.getRoute());
    }
}
