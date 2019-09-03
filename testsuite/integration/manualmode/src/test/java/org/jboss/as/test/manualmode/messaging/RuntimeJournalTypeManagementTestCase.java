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
package org.jboss.as.test.manualmode.messaging;

import static org.jboss.as.controller.client.helpers.ClientConstants.VALUE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import org.jboss.arquillian.container.test.api.ContainerController;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test reading the journal runtime type.
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
@RunAsClient()
@RunWith(Arquillian.class)
public class RuntimeJournalTypeManagementTestCase {

    private static final String DEFAULT_FULL_JBOSSAS = "default-full-jbossas";

    private static final ModelNode SERVER_ADDRESS = PathAddress.parseCLIStyleAddress("/subsystem=messaging-activemq/server=default").toModelNode();
    private static final ModelNode AIO_DISABLED_ADDRESS = Operations.createAddress("system-property", "org.apache.activemq.artemis.core.io.aio.AIOSequentialFileFactory.DISABLED");

    @ArquillianResource
    protected static ContainerController container;

    @Test
    public void testReadFileJournaltype() throws IOException {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        ManagementClient managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertNotNull(readRuntimeJournalType(managementClient));
        ModelNode op = Operations.createAddOperation(AIO_DISABLED_ADDRESS);
        op.get(VALUE).set(true);
        execute(managementClient, op, true);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertEquals("NIO", readRuntimeJournalType(managementClient));
        op = Operations.createRemoveOperation(AIO_DISABLED_ADDRESS);
        execute(managementClient, op, true);
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    @Test
    public void testReadDatabaseJournaltype() throws IOException {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        ManagementClient managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertNotNull(readRuntimeJournalType(managementClient));
        ModelNode op = Operations.createWriteAttributeOperation(SERVER_ADDRESS, "journal-datasource", "ExampleDS");
        execute(managementClient, op, true);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertEquals("DATABASE", readRuntimeJournalType(managementClient));
        op = Operations.createUndefineAttributeOperation(SERVER_ADDRESS, "journal-datasource");
        execute(managementClient, op, true);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    @Test
    public void testReadNoneJournaltype() throws IOException {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        ManagementClient managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertNotNull(readRuntimeJournalType(managementClient));
        ModelNode op = Operations.createWriteAttributeOperation(SERVER_ADDRESS, "persistence-enabled", false);
        execute(managementClient, op, true);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertEquals("NONE", readRuntimeJournalType(managementClient));
        op = Operations.createWriteAttributeOperation(SERVER_ADDRESS, "persistence-enabled", true);
        execute(managementClient, op, true);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    @Test
    public void testReadNioJournaltype() throws IOException {
        if (!container.isStarted(DEFAULT_FULL_JBOSSAS)) {
            container.start(DEFAULT_FULL_JBOSSAS);
        }
        ManagementClient managementClient = createManagementClient();
        assertEquals("ASYNCIO", readJournalType(managementClient));
        assertNotNull(readRuntimeJournalType(managementClient));
        ModelNode op = Operations.createWriteAttributeOperation(SERVER_ADDRESS, "journal-type", "NIO");
        execute(managementClient, op, true);
        assertEquals("NIO", readJournalType(managementClient));
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
        container.start(DEFAULT_FULL_JBOSSAS);
        managementClient = createManagementClient();
        assertEquals("NIO", readJournalType(managementClient));
        assertEquals("NIO", readRuntimeJournalType(managementClient));
        op = Operations.createWriteAttributeOperation(SERVER_ADDRESS, "journal-type", "ASYNCIO");
        execute(managementClient, op, true);
        managementClient.close();
        container.stop(DEFAULT_FULL_JBOSSAS);
    }

    private ModelNode execute(final org.jboss.as.arquillian.container.ManagementClient managementClient, final ModelNode op, final boolean expectSuccess) throws IOException {
        ModelNode response = managementClient.getControllerClient().execute(op);
        if (expectSuccess) {
            assertTrue(response.toJSONString(true), Operations.isSuccessfulOutcome(response));
            return Operations.readResult(response);
        }
        assertEquals("failed", response.get("outcome").asString());
        return response.get("failure-description");
    }

    private String readJournalType(ManagementClient managementClient) throws IOException {
        return execute(managementClient, Operations.createReadAttributeOperation(SERVER_ADDRESS, "journal-type"), true).asString();
    }

    private String readRuntimeJournalType(ManagementClient managementClient) throws IOException {
        return execute(managementClient, Operations.createReadAttributeOperation(SERVER_ADDRESS, "runtime-journal-type"), true).asString();
    }

    private static ManagementClient createManagementClient() throws UnknownHostException {
        return new ManagementClient(
                TestSuiteEnvironment.getModelControllerClient(),
                TestSuiteEnvironment.formatPossibleIpv6Address(TestSuiteEnvironment.getServerAddress()),
                TestSuiteEnvironment.getServerPort(),
                "remote+http");
    }

}
