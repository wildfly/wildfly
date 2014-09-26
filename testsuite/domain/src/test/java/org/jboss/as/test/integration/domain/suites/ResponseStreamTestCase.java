/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.integration.domain.extension.ExtensionSetup;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.extension.streams.LogStreamExtension;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of propagating response streams around a domain.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ResponseStreamTestCase {

    private static DomainTestSupport testSupport;
    private static DomainClient masterClient;
    private static DomainClient slaveClient;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ResponseStreamTestCase.class.getSimpleName());
        masterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
        slaveClient = testSupport.getDomainSlaveLifecycleUtil().getDomainClient();
        // Initialize the test extension
        ExtensionSetup.initializeLogStreamExtension(testSupport);

        ModelNode addExtension = Util.createAddOperation(PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));

        executeForResult(addExtension, masterClient);

        ModelNode addSubsystem = Util.createAddOperation(PathAddress.pathAddress(
                PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME)));
        executeForResult(addSubsystem, masterClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        ModelNode removeSubsystem = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(
                PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME)));
        executeForResult(removeSubsystem, masterClient);

        ModelNode removeExtension = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));
        executeForResult(removeExtension, masterClient);

        testSupport = null;
        masterClient = null;
        slaveClient = null;
        DomainTestSuite.stopSupport();
    }

    private String logMessageContent;

    @Before
    public void before() throws IOException {

        logMessageContent = String.valueOf(System.currentTimeMillis());
        ModelNode opNode = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
        opNode.get(VALUE).set(logMessageContent);
        Operation op = OperationBuilder.create(opNode).build();
        masterClient.executeOperation(op, OperationMessageHandler.DISCARD);
    }

    @After
    public void after() throws IOException {
        ModelNode opNode = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
        Operation op = OperationBuilder.create(opNode).build();
        masterClient.executeOperation(op, OperationMessageHandler.DISCARD);

    }

    @Test
    public void testMasterHost() throws IOException {
        PathAddress base = PathAddress.pathAddress(PROFILE, "default");
        readLogFile(createReadAttributeOp(base), masterClient, false);
    }

    @Test
    public void testSlaveHost() throws IOException {
        PathAddress base = PathAddress.pathAddress(PROFILE, "default");
        readLogFile(createReadAttributeOp(base), slaveClient, false);
    }

    @Test
    public void testMasterServer() throws IOException {
        PathAddress base = PathAddress.pathAddress(HOST, "master").append(SERVER, "main-one");
        readLogFile(createReadAttributeOp(base), masterClient, true);
        readLogFile(createOperationOp(base), masterClient, true);
    }

    @Test
    public void testSlaveServer() throws IOException {
        PathAddress base = PathAddress.pathAddress(HOST, "slave").append(SERVER, "main-three");
        readLogFile(createReadAttributeOp(base), masterClient, true);
        readLogFile(createOperationOp(base), masterClient, true);
        readLogFile(createReadAttributeOp(base), slaveClient, true);
        readLogFile(createOperationOp(base), slaveClient, true);
    }

    @Test
    public void testComposite() throws IOException {
        ModelNode composite = Util.createEmptyOperation(COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = composite.get(STEPS);
        steps.add(createReadAttributeOp(PathAddress.pathAddress(PROFILE, "default")));
        steps.add(createReadAttributeOp(PathAddress.pathAddress(HOST, "master").append(SERVER, "main-one")));
        steps.add(createReadAttributeOp(PathAddress.pathAddress(HOST, "slave").append(SERVER, "main-three")));
        Operation op = OperationBuilder.create(composite).build();
        OperationResponse response = null;
        try {
            response = masterClient.executeOperation(op, OperationMessageHandler.DISCARD);

            ModelNode respNode = response.getResponseNode();
            System.out.println(respNode.toString());
            Assert.assertEquals(respNode.toString(), "success", respNode.get("outcome").asString());
            List<? extends OperationResponse.StreamEntry> streams = response.getInputStreams();
            //Assert.assertEquals(3, streams.size());

            ModelNode result0 = respNode.get(RESULT, "step-1", RESULT);
            Assert.assertEquals(ModelType.STRING, result0.getType());
            String uuid = result0.asString();
            processResponseStream(response, uuid, false, true);

            ModelNode result1 = respNode.get(RESULT, "step-2", RESULT);
            Assert.assertEquals(ModelType.STRING, result1.getType());
            uuid = result1.asString();
            processResponseStream(response, uuid, true, false);

            ModelNode result2 = respNode.get(RESULT, "step-3", RESULT);
            Assert.assertEquals(ModelType.STRING, result2.getType());
            uuid = result2.asString();
            processResponseStream(response, uuid, true, false);

        } finally {
            StreamUtils.safeClose(response);
        }
    }

    private ModelNode createReadAttributeOp(PathAddress base) {
        PathAddress pa = base.append(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME);
        return Util.getReadAttributeOperation(pa, LogStreamExtension.LOG_FILE.getName());
    }

    private ModelNode createOperationOp(PathAddress base) {
        PathAddress pa = base.append(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME);
        return Util.createEmptyOperation(LogStreamExtension.STREAM_LOG_FILE, pa);
    }

    private void readLogFile(ModelNode opNode, ModelControllerClient client, boolean forServer) throws IOException {
        Operation op = OperationBuilder.create(opNode).build();
        OperationResponse response = null;
        try {
            response = client.executeOperation(op, OperationMessageHandler.DISCARD);

            ModelNode respNode = response.getResponseNode();
            System.out.println(respNode.toString());
            Assert.assertEquals(respNode.toString(), "success", respNode.get("outcome").asString());
            ModelNode result = respNode.get("result");
            Assert.assertEquals(respNode.toString(), ModelType.STRING, result.getType());
            List<? extends OperationResponse.StreamEntry> streams = response.getInputStreams();
            Assert.assertEquals(1, streams.size());
            processResponseStream(response, result.asString(), forServer, client == masterClient);

        } finally {
            StreamUtils.safeClose(response);
        }

    }

    private void processResponseStream(OperationResponse response, String streamUUID, boolean forServer, boolean forMaster) throws IOException {
        OperationResponse.StreamEntry se = response.getInputStream(streamUUID);
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(se.getStream()));

        String expected = LogStreamExtension.getLogMessage(logMessageContent);
        boolean readRegisteredServer = false;
        boolean readRegisteredSlave = false;
        boolean readExpected = false;
        String read;
        while ((read = reader.readLine()) != null) {
            readRegisteredServer = readRegisteredServer || read.contains("Registering server");
            readRegisteredSlave = readRegisteredSlave || read.contains("Registered remote slave host");
            readExpected = readExpected || read.contains(expected);
        }

        if (forServer) {
            Assert.assertFalse(readRegisteredServer);
        } else if (forMaster) {
            Assert.assertTrue(readRegisteredSlave);
        } else {
            Assert.assertFalse(readRegisteredSlave);
            Assert.assertTrue(readRegisteredServer);
        }
        Assert.assertTrue(readExpected);

        reader.close();
    }

    private static ModelNode executeForResult(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        try {
            return DomainTestUtils.executeForResult(op, modelControllerClient);
        } catch (MgmtOperationException e) {
            System.out.println(" Op failed:");
            System.out.println(e.getOperation());
            System.out.println("with result");
            System.out.println(e.getResult());
            throw e;
        }
    }
}
