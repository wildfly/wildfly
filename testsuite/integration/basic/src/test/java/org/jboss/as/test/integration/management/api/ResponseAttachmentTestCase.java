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

package org.jboss.as.test.integration.management.api;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.extension.EmptySubsystemParser;
import org.jboss.as.test.integration.management.extension.ExtensionUtils;
import org.jboss.as.test.integration.management.extension.streams.LogStreamExtension;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test of streams attached to a standalone server management op response.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ResponseAttachmentTestCase extends ContainerResourceMgmtTestBase {

    private String logMessageContent;

    @Before
    public void before() throws IOException, MgmtOperationException {
        // Install the log-stream extension and subsystem
        ExtensionUtils.createExtensionModule(LogStreamExtension.MODULE_NAME, LogStreamExtension.class,
                EmptySubsystemParser.class.getPackage());

        ModelNode op = Util.createAddOperation(PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));
        executeOperation(op);

        op = Util.createAddOperation(PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
        executeOperation(op);

        logMessageContent = String.valueOf(System.currentTimeMillis());
        op = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
        op.get(VALUE).set(logMessageContent);
        executeOperation(op);
    }

    @After
    public void after() throws IOException, MgmtOperationException {
        try {
            ModelNode op = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(SYSTEM_PROPERTY, LogStreamExtension.LOG_MESSAGE_PROP));
            executeOperation(op);
        } finally {
            try {
                ModelNode op = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
                executeOperation(op);
            } finally {
                try {
                    ModelNode op = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(EXTENSION, LogStreamExtension.MODULE_NAME));
                    executeOperation(op);
                } finally {
                    // Remove the log-stream extension and subsystem
                    ExtensionUtils.deleteExtensionModule(LogStreamExtension.MODULE_NAME);
                }
            }
        }
    }

    @Test
    public void testReadLogAsStream() throws Exception {
        ModelNode opNode = Util.getReadAttributeOperation(PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME), "log-file");
        readLogFile(opNode);

        opNode = Util.createEmptyOperation(LogStreamExtension.STREAM_LOG_FILE, PathAddress.pathAddress(SUBSYSTEM, LogStreamExtension.SUBSYSTEM_NAME));
        readLogFile(opNode);
    }

    private void readLogFile(ModelNode opNode) throws IOException {

        Operation op = OperationBuilder.create(opNode).build();
        OperationResponse response = null;
        try {
            response = getModelControllerClient().executeOperation(op, OperationMessageHandler.DISCARD);

            ModelNode respNode = response.getResponseNode();
            Assert.assertEquals(respNode.toString(), "success", respNode.get("outcome").asString());
            Assert.assertEquals(respNode.toString(), ModelType.STRING, respNode.get("result").getType());
            String uuid = respNode.get("result").asString();
            List<? extends OperationResponse.StreamEntry> streams = response.getInputStreams();
            Assert.assertEquals(1, streams.size());
            OperationResponse.StreamEntry se = streams.get(0);
            Assert.assertEquals(uuid, se.getUUID());

            LineNumberReader reader = new LineNumberReader(new InputStreamReader(se.getStream()));
            String read;
            boolean readMessage = false;
            String expected = LogStreamExtension.getLogMessage(logMessageContent);
            while ((read = reader.readLine()) != null) {
                readMessage = readMessage || read.contains(expected);
            }

            Assert.assertTrue("Did not see " + expected, readMessage);

        } finally {
            StreamUtils.safeClose(response);
        }

    }
}
