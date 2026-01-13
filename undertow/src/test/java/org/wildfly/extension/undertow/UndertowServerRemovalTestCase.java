/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

/**
 *  Tests server and host removal in Undertow subsystem.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@RunWith(Parameterized.class)
public class UndertowServerRemovalTestCase extends AbstractUndertowSubsystemTestCase {

    private static final String NODE_NAME = "node-name";
    private static final String SERVER_ABC = "abc";
    private static final String HOST_ABC = "abc-host";

    @Parameters
    public static Iterable<UndertowSubsystemSchema> parameters() {
        return UndertowSubsystemSchema.CURRENT.values();
    }

    public UndertowServerRemovalTestCase(UndertowSubsystemSchema schema) {
        super(schema);
    }

    @Override
    protected UndertowSubsystemSchema getSubsystemSchema() {
        return super.getSubsystemSchema();
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return super.getSubsystemXml();
    }

    @Override
    public void setUp() {
        super.setUp();
        System.setProperty("jboss.node.name", NODE_NAME);
    }

    private KernelServices load(String subsystemXml) throws Exception {
        KernelServicesBuilder builder = createKernelServicesBuilder(new RuntimeInitialization(this.values, super.schema)).setSubsystemXml(subsystemXml);
        KernelServices mainServices = builder.build();
        if (!mainServices.isSuccessfulBoot()) {
            Throwable t = mainServices.getBootError();
            Assert.fail("Boot unsuccessful: " + (t != null ? t.toString() : "no boot error provided"));
        }
        return mainServices;
    }

    private PathAddress serverAddress(String server) {
        return PathAddress.pathAddress("subsystem", "undertow").append("server", server);
    }

    private PathAddress hostAddress(String server, String host) {
        return serverAddress(server).append("host", host);
    }

    @Test
    public void removeDefaultServerShouldFail() throws Exception {
        KernelServices mainServices = null;
        try {
            final String defaultServer = "some-server";
            final String defaultHost = "default-virtual-host";
            mainServices = load(getSubsystemXml());
            final ServiceName undertowServerName = ServerDefinition.SERVER_CAPABILITY.getCapabilityServiceName(defaultServer);
            Server server = (Server) this.values.get(undertowServerName).get();
            assertNotNull(server);
            Assert.assertEquals(defaultServer, server.getName());
            final int times = 2;
            for (int i = 0; i < times; i ++) {
                ModelNode removeOp = Util.createOperation(ModelDescriptionConstants.REMOVE, serverAddress(defaultServer));
                ModelNode response = mainServices.executeOperation(removeOp);
                assertEquals(ModelDescriptionConstants.FAILED, response.get(ModelDescriptionConstants.OUTCOME).asString());
                assertNotNull(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION));
                assertTrue(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString().contains("WFLYCTL0367"));
            }
            for (int i = 0; i < times; i ++) {
                ModelNode removeOp = Util.createOperation(ModelDescriptionConstants.REMOVE, hostAddress(defaultServer, defaultHost));
                ModelNode response = mainServices.executeOperation(removeOp);
                assertEquals(ModelDescriptionConstants.FAILED, response.get(ModelDescriptionConstants.OUTCOME).asString());
                assertNotNull(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION));
                assertTrue(response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString().contains("WFLYCTL0367"));
            }
        } finally {
            if (mainServices != null) {
                mainServices.shutdown();
            }
        }
    }

    @Test
    public void removeNonDefaultHost() throws Exception {
        KernelServices mainServices = null;
        try {
            mainServices = load(getSubsystemXml("undertow-service-extra-server.xml"));
            ModelNode removeOp = Util.createOperation(ModelDescriptionConstants.REMOVE, hostAddress(SERVER_ABC, HOST_ABC));
            ModelNode response = mainServices.executeOperation(removeOp);
            assertEquals(ModelDescriptionConstants.SUCCESS, response.get(ModelDescriptionConstants.OUTCOME).asString());
            assertEquals(ModelDescriptionConstants.RELOAD_REQUIRED, response.get(ModelDescriptionConstants.RESPONSE_HEADERS).get(ModelDescriptionConstants.PROCESS_STATE).asString());
        } finally {
            if (mainServices != null) {
                mainServices.shutdown();
            }
        }
    }

    @Test
    public void removeNonDefaultHostAllowResourceServiceRestart() throws Exception {
        KernelServices mainServices = null;
        try {
            mainServices = load(getSubsystemXml("undertow-service-extra-server.xml"));
            ModelNode removeOp = Util.createOperation(ModelDescriptionConstants.REMOVE, hostAddress(SERVER_ABC, HOST_ABC));
            removeOp.get(ModelDescriptionConstants.OPERATION_HEADERS)
                    .get(ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART)
                    .set(true);
            ModelNode response = mainServices.executeOperation(removeOp);
            assertEquals(ModelDescriptionConstants.SUCCESS, response.get(ModelDescriptionConstants.OUTCOME).asString());
            assertFalse(response.hasDefined(ModelDescriptionConstants.RESPONSE_HEADERS));
        } finally {
            if (mainServices != null) {
                mainServices.shutdown();
            }
        }
    }


    @Test
    public void removeNonDefaultServer() throws Exception {
        KernelServices mainServices = null;
        try {
            mainServices = load(getSubsystemXml("undertow-service-extra-server.xml"));
            ModelNode removeOp = Util.createOperation(ModelDescriptionConstants.REMOVE, serverAddress(SERVER_ABC));
            ModelNode response = mainServices.executeOperation(removeOp);
            assertEquals(ModelDescriptionConstants.SUCCESS, response.get(ModelDescriptionConstants.OUTCOME).asString());
            assertEquals(ModelDescriptionConstants.RELOAD_REQUIRED, response.get(ModelDescriptionConstants.RESPONSE_HEADERS).get(ModelDescriptionConstants.PROCESS_STATE).asString());
        } finally {
            if (mainServices != null) {
                mainServices.shutdown();
            }
        }
    }

    @Test
    public void removeNonDefaultServerAllowResourceServiceRestart() throws Exception {
        KernelServices mainServices = null;
        try {
            mainServices = load(getSubsystemXml("undertow-service-extra-server.xml"));
            ModelNode removeOp = Util.createOperation(ModelDescriptionConstants.REMOVE, serverAddress(SERVER_ABC));
            removeOp.get(ModelDescriptionConstants.OPERATION_HEADERS)
                    .get(ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART)
                    .set(true);
            ModelNode response = mainServices.executeOperation(removeOp);
            assertEquals(ModelDescriptionConstants.SUCCESS, response.get(ModelDescriptionConstants.OUTCOME).asString());
            assertEquals(ModelDescriptionConstants.RELOAD_REQUIRED, response.get(ModelDescriptionConstants.RESPONSE_HEADERS).get(ModelDescriptionConstants.PROCESS_STATE).asString());
        } finally {
            if (mainServices != null) {
                mainServices.shutdown();
            }
        }
    }

}