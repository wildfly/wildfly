/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import org.junit.Assert;
import org.junit.Before;
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


    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("undertow-12.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-undertow_12_0.xsd";
    }

    @Before
    public void setUp() {
        setProperty();
        System.setProperty("jboss.node.name", NODE_NAME);
    }

    private Server load(String xmlFile, String serverName) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(RUNTIME).setSubsystemXml(readResource(xmlFile));
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        final ServiceName undertowServerName = ServerDefinition.SERVER_CAPABILITY.getCapabilityServiceName(serverName);
        ServiceController<Server> serverService = (ServiceController<Server>) mainServices.getContainer().getService(undertowServerName);

        assertNotNull(serverService);
        serverService.setMode(ServiceController.Mode.ACTIVE);
        final Server server = serverService.getValue();
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
