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

import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test for UndertowService created from xml file.
 *
 * @author Flavia Rainone
 */
public class UndertowServiceTestCase extends AbstractUndertowSubsystemTestCase {

    private static final String NODE_NAME = "node-name";
    private static final String DEFAULT_SERVER = "default-server";
    private static final String DEFAULT_SERVLET_CONTAINER = "default";
    private static final String DEFAULT_VIRTUAL_HOST = "default-host";

    public UndertowServiceTestCase() {
        super(UndertowSubsystemSchema.VERSION_12_0);
    }

    @Override
    public void setUp() {
        super.setUp();
        System.setProperty("jboss.node.name", NODE_NAME);
    }

    private UndertowService load(String xmlFile) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new RuntimeInitialization(this.values)).setSubsystemXml(readResource(xmlFile));
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        final UndertowService undertowService = (UndertowService) this.values.get(UndertowRootDefinition.UNDERTOW_CAPABILITY.getCapabilityServiceName()).get();
        assertNotNull(undertowService);
        return undertowService;
    }

    @Test
    public void testUndefinedAttributes() throws Exception {
        final UndertowService undertowService = load("undertow-service-undefined-attributes.xml");
        assertEquals(NODE_NAME, undertowService.getInstanceId());
        assertFalse(undertowService.isObfuscateSessionRoute());
        assertFalse(undertowService.isStatisticsEnabled());
        assertEquals(DEFAULT_SERVER, undertowService.getDefaultServer());
        assertEquals(DEFAULT_SERVLET_CONTAINER, undertowService.getDefaultContainer());
        assertEquals(DEFAULT_VIRTUAL_HOST, undertowService.getDefaultVirtualHost());
    }

    @Test
    public void testDefinedDefaultAttributes1() throws Exception {
        final UndertowService undertowService = load("undertow-service-defined-default-attributes1.xml");
        assertEquals(NODE_NAME, undertowService.getInstanceId());
        assertFalse(undertowService.isObfuscateSessionRoute());
        assertFalse(undertowService.isStatisticsEnabled());
        assertEquals(DEFAULT_SERVER, undertowService.getDefaultServer());
        assertEquals(DEFAULT_SERVLET_CONTAINER, undertowService.getDefaultContainer());
        assertEquals(DEFAULT_VIRTUAL_HOST, undertowService.getDefaultVirtualHost());
    }

    @Test
    public void testDefinedDefaultAttributes2() throws Exception {
        final UndertowService undertowService = load("undertow-service-defined-default-attributes2.xml");
        assertEquals(NODE_NAME, undertowService.getInstanceId());
        assertFalse(undertowService.isObfuscateSessionRoute());
        assertFalse(undertowService.isStatisticsEnabled());
        assertEquals(DEFAULT_SERVER, undertowService.getDefaultServer());
        assertEquals(DEFAULT_SERVLET_CONTAINER, undertowService.getDefaultContainer());
        assertEquals(DEFAULT_VIRTUAL_HOST, undertowService.getDefaultVirtualHost());
    }

    @Test
    public void testDefinedAttributes() throws Exception {
        final UndertowService undertowService = load("undertow-service-defined-attributes.xml");
        assertEquals(NODE_NAME + "-undertow", undertowService.getInstanceId());
        assertFalse(undertowService.isObfuscateSessionRoute());
        assertTrue(undertowService.isStatisticsEnabled());
        assertEquals("undertow-server", undertowService.getDefaultServer());
        assertEquals("servlet-container", undertowService.getDefaultContainer());
        assertEquals("virtual-host", undertowService.getDefaultVirtualHost());
    }

    @Test
    public void testObfuscateInstanceId1() throws Exception {
        final UndertowService undertowService = load("undertow-service-obfuscate-instance-id-attribute1.xml");
        //assertEquals(new String(Base64.getUrlEncoder().encode(NODE_NAME.getBytes())), undertowService.getInstanceId());
        assertEquals(NODE_NAME, undertowService.getInstanceId());
        assertTrue(undertowService.isObfuscateSessionRoute());
        assertFalse(undertowService.isStatisticsEnabled());
        assertEquals("undertow-server", undertowService.getDefaultServer());
        assertEquals(DEFAULT_SERVLET_CONTAINER, undertowService.getDefaultContainer());
        assertEquals("virtual-host3", undertowService.getDefaultVirtualHost());
    }

    @Test
    public void testObfuscateInstanceId2() throws Exception {
        final UndertowService undertowService = load("undertow-service-obfuscate-instance-id-attribute2.xml");
        assertEquals("my-undertow-instance", undertowService.getInstanceId());
        assertTrue(undertowService.isObfuscateSessionRoute());
        assertFalse(undertowService.isStatisticsEnabled());
        assertEquals("undertow-server", undertowService.getDefaultServer());
        assertEquals(DEFAULT_SERVLET_CONTAINER, undertowService.getDefaultContainer());
        assertEquals("virtual-host3", undertowService.getDefaultVirtualHost());
    }
}
