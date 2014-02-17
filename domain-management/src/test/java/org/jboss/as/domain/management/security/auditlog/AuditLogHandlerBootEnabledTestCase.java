/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security.auditlog;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.audit.SyslogAuditLogHandler.MessageTransfer;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.audit.AuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.domain.management.audit.FileAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.JsonAuditLogFormatterResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogHandlerResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.SyslogHandler.SyslogType;
import org.junit.Assert;
import org.junit.Test;

/**
 * Don't use core-model test for this. It does not support runtime, and more importantly for backwards compatibility the audit logger cannot be used
 *
 * @author: Kabir Khan
 */
public class AuditLogHandlerBootEnabledTestCase extends AbstractAuditLogHandlerTestCase {
    public AuditLogHandlerBootEnabledTestCase() {
        super(true);
    }

    @Test
    public void testAuditLoggerBootUp() throws Exception {
        File file = new File(logDir, "test-file.log");
        List<ModelNode> bootRecords = readFile(file, 1);

        ModelNode bootRecord = bootRecords.get(0);
        List<ModelNode> bootOps = checkBootRecordHeader(bootRecord, 5, "core", false, true, true);
        for (int i = 0 ; i < 5 ; i++) {
            checkOpsEqual(bootOperations.get(i), bootOps.get(i));
        }
    }

    @Test
    public void testCannotRemoveReferencedHandler() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createRemoveFileHandlerOperation("file");
        executeForFailure(op);
        List<ModelNode> records = readFile(file, 2);
        //TODO This gets picked up as read-only since it did not actually get around to modifying the model
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", true, false, false);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testCanRemoveLoggerReference() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createRemoveHandlerReferenceOperation("test-file");
        executeForResult(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testCannotRemoveReferencedFormatter() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createRemoveJsonFormatterOperation("test-formatter");
        executeForFailure(op);
        List<ModelNode> records = readFile(file, 2);
        //TODO This gets picked up as read-only since it did not actually get around to modifying the model
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", true, false, false);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkBootRecordHeader(records.get(2), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveFileHandlerOperation("test-file");
        executeForResult(op);
        readFile(file, 3);

        op = createRemoveJsonFormatterOperation("test-formatter");
        executeForResult(op);
        readFile(file, 3);
    }

    @Test
    public void testDisableAndEnableAuditLogger() throws Exception {
        File file = new File(logDir, "test-file.log");
        readFile(file, 1);

        ModelNode op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.ENABLED.getName(), false);
        executeForResult(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(file, 2);

        op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.ENABLED.getName(), false);
        executeForResult(op);
        readFile(file, 2);

        op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.ENABLED.getName(), true);
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkBootRecordHeader(records.get(2), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));


        op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 4);
        ops = checkBootRecordHeader(records.get(3), 1, "core", true, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testToggleReadOnly() throws Exception {
        File file = new File(logDir, "test-file.log");
        readFile(file, 1);

        ModelNode op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.LOG_READ_ONLY.getName(), false);
        executeForResult(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(file, 2);

        op = createAuditLogWriteAttributeOperation(AuditLogLoggerResourceDefinition.LOG_READ_ONLY.getName(), true);
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkBootRecordHeader(records.get(2), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(READ_RESOURCE_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 4);
        ops = checkBootRecordHeader(records.get(3), 1, "core", true, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testCannotAddHandlerWithSameName() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createAddFileHandlerOperation("test-file", "test-formatter", "fail.log");
        executeForFailure(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", false, false, false);
        checkOpsEqual(op, ops.get(0));

        op = createAddSyslogHandlerTcpOperation("test-file", "test-formatter", InetAddress.getByName("localhost"), SYSLOG_PORT, null, MessageTransfer.OCTET_COUNTING);
        executeForFailure(op);
        records = readFile(file, 3);
        ops = checkBootRecordHeader(records.get(2), 1, "core", false, false, false);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testAddNonExistingHandlerReference() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createAddHandlerReferenceOperation("notthere");
        executeForFailure(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkBootRecordHeader(records.get(1), 1, "core", false, false, false);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testSyslogUdp() throws Exception {
        SimpleSyslogServer server = SimpleSyslogServer.createUdp(6666);
        runSyslogTest(server, 6666, createAddSyslogHandlerUdpOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666, null, 0));
    }

    @Test
    public void testSyslogTcpOctetCounting() throws Exception {
        SimpleSyslogServer server = SimpleSyslogServer.createTcp(6666, true);
        runSyslogTest(server, 6666, createAddSyslogHandlerTcpOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666, null, MessageTransfer.OCTET_COUNTING));
    }

    @Test
    public void testSyslogTcpNonTransparentFraming() throws Exception {
        SimpleSyslogServer server = SimpleSyslogServer.createTcp(6666, false);
        runSyslogTest(server, 6666, createAddSyslogHandlerTcpOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666, null, MessageTransfer.NON_TRANSPARENT_FRAMING));
    }

    @Test
    public void testSyslogTlsOctetCounting() throws Exception {
        File serverCertStore = new File(getClass().getResource("server-cert-store.jks").toURI());
        File clientTrustStore = new File(getClass().getResource("client-trust-store.jks").toURI());
        SimpleSyslogServer server = SimpleSyslogServer.createTls(6666, true, serverCertStore, "changeit", null, null);
        runSyslogTest(server, 6666, createAddSyslogHandlerTlsOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666,
                null, MessageTransfer.OCTET_COUNTING, clientTrustStore, "changeit", null, null));
    }

    @Test
    public void testSyslogTlsNonTransparentFraming() throws Exception {
        File serverCertStore = new File(getClass().getResource("server-cert-store.jks").toURI());
        File clientTrustStore = new File(getClass().getResource("client-trust-store.jks").toURI());
        SimpleSyslogServer server = SimpleSyslogServer.createTls(6666, false, serverCertStore, "changeit", null, null);
        runSyslogTest(server, 6666, createAddSyslogHandlerTlsOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666,
                null, MessageTransfer.NON_TRANSPARENT_FRAMING, clientTrustStore, "changeit", null, null));
    }

    @Test
    public void testSyslogTlsOctetCountingClientAuth() throws Exception {
        File serverCertStore = new File(getClass().getResource("server-cert-store.jks").toURI());
        File clientTrustStore = new File(getClass().getResource("client-trust-store.jks").toURI());
        File clientCertStore = new File(getClass().getResource("client-cert-store.jks").toURI());
        File serverTrustStore = new File(getClass().getResource("server-trust-store.jks").toURI());
        SimpleSyslogServer server = SimpleSyslogServer.createTls(6666, true, serverCertStore, "changeit", serverTrustStore, "changeit");
        runSyslogTest(server, 6666, createAddSyslogHandlerTlsOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666,
                null, MessageTransfer.OCTET_COUNTING, clientTrustStore, "changeit", clientCertStore, "changeit"));
    }

    @Test
    public void testSyslogTlsNonTransparentFramingClientAuth() throws Exception {
        File serverCertStore = new File(getClass().getResource("server-cert-store.jks").toURI());
        File clientTrustStore = new File(getClass().getResource("client-trust-store.jks").toURI());
        File clientCertStore = new File(getClass().getResource("client-cert-store.jks").toURI());
        File serverTrustStore = new File(getClass().getResource("server-trust-store.jks").toURI());
        SimpleSyslogServer server = SimpleSyslogServer.createTls(6666, false, serverCertStore, "changeit", serverTrustStore, "changeit");
        runSyslogTest(server, 6666, createAddSyslogHandlerTlsOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666,
                null, MessageTransfer.NON_TRANSPARENT_FRAMING, clientTrustStore, "changeit", clientCertStore, "changeit"));
    }


    private void runSyslogTest(SimpleSyslogServer server, int port, ModelNode handlerAddOperation) throws Exception {
        try {
            File file = new File(logDir, "test-file.log");

            executeForResult(handlerAddOperation);
            List<ModelNode> records1 = readFile(file, 2);
            List<ModelNode> ops = checkBootRecordHeader(records1.get(1), 1, "core", false, false, true);
            checkOpsEqual(handlerAddOperation, ops.get(0));

            ModelNode op = createAddHandlerReferenceOperation("syslog-test");
            executeForResult(op);
            records1 = readFile(file, 3);
            ops = checkBootRecordHeader(records1.get(2), 1, "core", false, false, true);
            checkOpsEqual(op, ops.get(0));
            byte[] receivedBytes = server.receiveData();
            List<ModelNode> syslogOps = checkBootRecordHeader(getSyslogRecord(receivedBytes), 1, "core", false, false, true);
            Assert.assertEquals(ops, syslogOps);
            //TODO check syslog format and contents

            op = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
            executeForResult(op);
            records1 = readFile(file, 4);
            ops = checkBootRecordHeader(records1.get(3), 1, "core", true, false, true);
            checkOpsEqual(op, ops.get(0));
            receivedBytes = server.receiveData();
            syslogOps = checkBootRecordHeader(getSyslogRecord(receivedBytes), 1, "core", true, false, true);
            Assert.assertEquals(ops, syslogOps);

            //Make sure that removing the syslog handler it still gets logged
            op = createRemoveHandlerReferenceOperation("syslog-test");
            executeForResult(op);
            records1 = readFile(file, 5);
            ops = checkBootRecordHeader(records1.get(4), 1, "core", false, false, true);
            checkOpsEqual(op, ops.get(0));
            receivedBytes = server.receiveData();
            syslogOps = checkBootRecordHeader(getSyslogRecord(receivedBytes), 1, "core", false, false, true);
            Assert.assertEquals(ops, syslogOps);

            op = Util.createOperation(READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
            executeForResult(op);
            records1 = readFile(file, 6);
            ops = checkBootRecordHeader(records1.get(5), 1, "core", true, false, true);
            checkOpsEqual(op, ops.get(0));
            //Should be nothing in syslog
            Assert.assertNull(server.pollData());

            //TODO remove handler and check nothing in syslog and expected in file
        } finally {
            server.close();
        }
    }


    @Test
    public void testAddRemoveFileAuditLogHandler() throws Exception {
        File file1 = new File(logDir, "test-file.log");
        File file2 = new File(logDir, "test-file2.log");
        Assert.assertFalse(file2.exists());

        ModelNode op = createAddFileHandlerOperation("file2", "test-formatter", "test-file2.log");
        executeForResult(op);
        Assert.assertFalse(file2.exists());
        List<ModelNode> records1 = readFile(file1, 2);
        List<ModelNode> ops = checkBootRecordHeader(records1.get(1), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createAddHandlerReferenceOperation("file2");
        executeForResult(op);
        records1 = readFile(file1, 3);
        List<ModelNode> records2 = readFile(file2, 1);
        Assert.assertEquals(records1.get(2), records2.get(0));
        ops = checkBootRecordHeader(records1.get(2), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveHandlerReferenceOperation("test-file");
        executeForResult(op);
        records1 = readFile(file1, 4);
        records2 = readFile(file2, 2);
        Assert.assertEquals(records1.get(3), records2.get(1));
        ops = checkBootRecordHeader(records1.get(3), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        //The file handler was closed so it creates a new file
        op = createAddHandlerReferenceOperation("test-file");
        executeForResult(op);
        records1 = readFile(file1, 1);
        records2 = readFile(file2, 3);
        Assert.assertEquals(records1.get(0), records2.get(2));
        ops = checkBootRecordHeader(records1.get(0), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveHandlerReferenceOperation("test-file");
        executeForResult(op);
        records1 = readFile(file1, 2);
        records2 = readFile(file2, 4);
        Assert.assertEquals(records1.get(1), records2.get(3));
        ops = checkBootRecordHeader(records1.get(1), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveFileHandlerOperation("test-file");
        executeForResult(op);
        records1 = readFile(file1, 2);
        records2 = readFile(file2, 5);
        ops = checkBootRecordHeader(records2.get(4), 1, "core", false, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testAddRemoveSyslogAuditLogHandler() throws Exception {
        File file1 = new File(logDir, "test-file.log");

        SimpleSyslogServer server = SimpleSyslogServer.createTcp(6666, false);
        try {
            ModelNode op = createAddSyslogHandlerTcpOperation("tcp", "test-formatter", InetAddress.getByName("localhost"), SYSLOG_PORT, null, null);
            executeForResult(op);
            List<ModelNode> records1 = readFile(file1, 2);
            List<ModelNode> ops = checkBootRecordHeader(records1.get(1), 1, "core", false, false, true);
            checkOpsEqual(op, ops.get(0));

            op = createAddHandlerReferenceOperation("tcp");
            executeForResult(op);
            records1 = readFile(file1, 3);
            ops = checkBootRecordHeader(records1.get(2), 1, "core", false, false, true);
            ModelNode syslogOp = checkBootRecordHeader(getSyslogRecord(server.receiveData()), 1, "core", false, false, true).get(0);
            checkOpsEqual(op, syslogOp);
            Assert.assertEquals(syslogOp, ops.get(0));

            op = createRemoveHandlerReferenceOperation("tcp");
            executeForResult(op);
            records1 = readFile(file1, 4);
            ops = checkBootRecordHeader(records1.get(3), 1, "core", false, false, true);
            syslogOp = checkBootRecordHeader(getSyslogRecord(server.receiveData()), 1, "core", false, false, true).get(0);
            checkOpsEqual(op, syslogOp);
            Assert.assertEquals(syslogOp, ops.get(0));
        } finally {
            //Close the dummy server since it only accepts one connection
            server.close();
        }

        server = SimpleSyslogServer.createTcp(6666, false);
        try {
            ModelNode op = createAddHandlerReferenceOperation("tcp");
            executeForResult(op);
            List<ModelNode> records1 = readFile(file1, 5);
            List<ModelNode> ops = checkBootRecordHeader(records1.get(4), 1, "core", false, false, true);
            ModelNode syslogOp = checkBootRecordHeader(getSyslogRecord(server.receiveData()), 1, "core", false, false, true).get(0);
            checkOpsEqual(op, syslogOp);
            Assert.assertEquals(syslogOp, ops.get(0));

            op = createRemoveHandlerReferenceOperation("test-file");
            executeForResult(op);
            records1 = readFile(file1, 6);
            //records2 = readFile(file2, 4);
            //Assert.assertEquals(records1.get(5), records2.get(3));
            ops = checkBootRecordHeader(records1.get(5), 1, "core", false, false, true);
            checkOpsEqual(op, ops.get(0));
        } finally {
            server.close();
        }
    }

    @Test
    public void testMessageTransfer() throws Exception {
        SimpleSyslogServer server1 = SimpleSyslogServer.createUdp(SYSLOG_PORT);
        try {
            SimpleSyslogServer server2 = SimpleSyslogServer.createUdp(SYSLOG_PORT2);
            try {
                executeForResult(createAddSyslogHandlerUdpOperation("rfc-3164", "test-formatter", InetAddress.getByName("localhost"), SYSLOG_PORT, SyslogType.RFC3164, 5000));
                executeForResult(createAddSyslogHandlerUdpOperation("rfc-5424", "test-formatter", InetAddress.getByName("localhost"), SYSLOG_PORT2, SyslogType.RFC5424, 5000));

                ModelNode composite = new ModelNode();
                composite.get(OP_ADDR).setEmptyList();
                composite.get(OP).set(COMPOSITE);
                composite.get(STEPS).add(createAddHandlerReferenceOperation("rfc-3164"));
                composite.get(STEPS).add(createAddHandlerReferenceOperation("rfc-5424"));
                executeForResult(composite);

                byte[] bytes1 = server1.receiveData();
                byte[] bytes2 = server2.receiveData();

                //The RFC-5424 format is longer than the RFC-3164 format
                Assert.assertTrue(bytes2.length > bytes1.length);

                ModelNode op1 = checkBootRecordHeader(getSyslogRecord(bytes1), 1, "core", false, false, true).get(0);
                checkOpsEqual(composite, op1);
                ModelNode op2 = checkBootRecordHeader(getSyslogRecord(bytes2), 1, "core", false, false, true).get(0);
                Assert.assertEquals(op1, op2);
            } finally {
                server2.close();
            }
        } finally {
            server1.close();
        }
    }

    @Test
    public void testUpdateFileHandlerFormatter() throws Exception {
        //testUpdateSyslogHandlerFormatter Does the same for the syslog hander
        File file = new File(logDir, "test-file.log");
        String fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{[\\s\\S]*", fullRecord)); //This regexp allows for new lines

        file.delete();
        ModelNode op = Util.getWriteAttributeOperation(createFileHandlerAddress("test-file"),
                FileAuditLogHandlerResourceDefinition.FORMATTER.getName(),
                new ModelNode("non-existent"));
        executeForFailure(op);
        fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{[\\s\\S]*", fullRecord)); //This regexp allows for new lines
        ModelNode record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
        ModelNode loggedOp = checkBootRecordHeader(record, 1, "core", false, false, false).get(0);
        checkOpsEqual(op, loggedOp);


        //Add some new formatters
        op = Util.createAddOperation(createJsonFormatterAddress("compact-formatter"));
        op.get(JsonAuditLogFormatterResourceDefinition.COMPACT.getName()).set(true);
        op.get(JsonAuditLogFormatterResourceDefinition.DATE_FORMAT.getName()).set("yyyy/MM/dd HH-mm-ss");
        op.get(JsonAuditLogFormatterResourceDefinition.DATE_SEPARATOR.getName()).set(" xxx ");
        executeForResult(op);

        op = Util.createAddOperation(createJsonFormatterAddress("escaped-formatter"));
        op.get(JsonAuditLogFormatterResourceDefinition.INCLUDE_DATE.getName()).set(false);
        op.get(JsonAuditLogFormatterResourceDefinition.ESCAPE_NEW_LINE.getName()).set(true);
        executeForResult(op);

        //Update the handler formatter to the compact version and check the logged format
        file.delete();
        op = Util.getWriteAttributeOperation(createFileHandlerAddress("test-file"), FileAuditLogHandlerResourceDefinition.FORMATTER.getName(), new ModelNode("compact-formatter"));
        executeForResult(op);
        fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d/\\d\\d/\\d\\d \\d\\d-\\d\\d-\\d\\d xxx \\{.*", fullRecord)); //This regexp checks for no new lines
        record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
        loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
        checkOpsEqual(op, loggedOp);

        //Update the handler formatter to the escaped version and check the logged format
        file.delete();
        op = Util.getWriteAttributeOperation(createFileHandlerAddress("test-file"), FileAuditLogHandlerResourceDefinition.FORMATTER.getName(), new ModelNode("escaped-formatter"));
        executeForResult(op);
        fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\{.*", fullRecord)); //This regexp checks for no new lines
        Assert.assertTrue(fullRecord.indexOf("#012") > 0);
        record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')).replace("#012", ""));
        loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
        checkOpsEqual(op, loggedOp);

        //Check removing formatter in use fails
        file.delete();
        op = Util.createRemoveOperation(createJsonFormatterAddress("escaped-formatter"));
        executeForFailure(op);

        //Check can remove unused formatter
        op = Util.createRemoveOperation(createJsonFormatterAddress("compact-formatter"));
        executeForResult(op);

        //Now try changing the used formatter at runtime
        file.delete();
        op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.ESCAPE_NEW_LINE.getName(), new ModelNode(false));
        executeForResult(op);
        fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\{[\\s\\S]*", fullRecord)); //This regexp allows for new lines
        Assert.assertTrue(fullRecord.indexOf("#012") == -1);
        record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
        loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
        checkOpsEqual(op, loggedOp);

        file.delete();
        op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.COMPACT.getName(), new ModelNode(true));
        executeForResult(op);
        fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\{.*", fullRecord)); //This regexp allows for new lines
        Assert.assertTrue(fullRecord.indexOf("#012") == -1);
        record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
        loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
        checkOpsEqual(op, loggedOp);

        op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.INCLUDE_DATE.getName(), new ModelNode(true));
        executeForResult(op);
        op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.DATE_FORMAT.getName(), new ModelNode("yyyy/MM/dd HH-mm-ss"));
        executeForResult(op);
        file.delete();
        op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.DATE_SEPARATOR.getName(), new ModelNode(" xxx "));
        executeForResult(op);
        fullRecord = readFullFileRecord(file);
        Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d/\\d\\d/\\d\\d \\d\\d-\\d\\d-\\d\\d xxx \\{.*", fullRecord)); //This regexp checks for no new lines
        record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
        loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
        checkOpsEqual(op, loggedOp);
    }


    @Test
    public void testUpdateSyslogHandlerFormatter() throws Exception {
        //testUpdateFileHandlerFormatter does the same for the file handler
        SimpleSyslogServer server = SimpleSyslogServer.createUdp(6666);

        try {
            //Set up the syslog handler
            ModelNode op = createAddSyslogHandlerUdpOperation("syslog-test", "test-formatter", InetAddress.getByName("localhost"), 6666, null, 0);
            executeForResult(op);
            op = createAddHandlerReferenceOperation("syslog-test");
            executeForResult(op);
            byte[] bytes = server.receiveData();

            op = Util.getWriteAttributeOperation(createFileHandlerAddress("test-file"),
                    FileAuditLogHandlerResourceDefinition.FORMATTER.getName(),
                    new ModelNode("non-existent"));
            executeForFailure(op);
            bytes = server.receiveData();
            String fullRecord = stripSyslogHeader(bytes);
            Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{[\\s\\S]*", fullRecord)); //This regexp allows for new lines
            ModelNode record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
            ModelNode loggedOp = checkBootRecordHeader(record, 1, "core", false, false, false).get(0);
            checkOpsEqual(op, loggedOp);

            //Add some new formatters
            op = Util.createAddOperation(createJsonFormatterAddress("compact-formatter"));
            op.get(JsonAuditLogFormatterResourceDefinition.COMPACT.getName()).set(true);
            op.get(JsonAuditLogFormatterResourceDefinition.DATE_FORMAT.getName()).set("yyyy/MM/dd HH-mm-ss");
            op.get(JsonAuditLogFormatterResourceDefinition.DATE_SEPARATOR.getName()).set(" xxx ");
            executeForResult(op);
            bytes = server.receiveData();

            op = Util.createAddOperation(createJsonFormatterAddress("escaped-formatter"));
            op.get(JsonAuditLogFormatterResourceDefinition.INCLUDE_DATE.getName()).set(false);
            op.get(JsonAuditLogFormatterResourceDefinition.ESCAPE_NEW_LINE.getName()).set(true);
            executeForResult(op);
            bytes = server.receiveData();

            //Update the handler formatter to the compact version and check the logged format
            op = Util.getWriteAttributeOperation(createSyslogHandlerAddress("syslog-test"), FileAuditLogHandlerResourceDefinition.FORMATTER.getName(), new ModelNode("compact-formatter"));
            executeForResult(op);
            bytes = server.receiveData();
            fullRecord = stripSyslogHeader(bytes);
            Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d/\\d\\d/\\d\\d \\d\\d-\\d\\d-\\d\\d xxx \\{.*", fullRecord)); //This regexp checks for no new lines
            record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
            loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
            checkOpsEqual(op, loggedOp);

            //Update the handler formatter to the escaped version and check the logged format
            op = Util.getWriteAttributeOperation(createSyslogHandlerAddress("syslog-test"), FileAuditLogHandlerResourceDefinition.FORMATTER.getName(), new ModelNode("escaped-formatter"));
            executeForResult(op);
            bytes = server.receiveData();
            fullRecord = stripSyslogHeader(bytes);
            Assert.assertTrue(Pattern.matches("\\{.*", fullRecord)); //This regexp checks for no new lines
            Assert.assertTrue(fullRecord.indexOf("#012") > 0);
            record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')).replace("#012", ""));
            loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
            checkOpsEqual(op, loggedOp);

            //Check removing formatter in use fails
            op = Util.createRemoveOperation(createJsonFormatterAddress("escaped-formatter"));
            executeForFailure(op);
            bytes = server.receiveData();

            //Check can remove unused formatter
            op = Util.createRemoveOperation(createJsonFormatterAddress("compact-formatter"));
            executeForResult(op);
            bytes = server.receiveData();

            op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.ESCAPE_NEW_LINE.getName(), new ModelNode(false));
            executeForResult(op);
            bytes = server.receiveData();
            fullRecord = stripSyslogHeader(bytes);
            Assert.assertTrue(Pattern.matches("\\{[\\s\\S]*", fullRecord)); //This regexp allows for new lines
            Assert.assertTrue(fullRecord.indexOf("#012") == -1);
            record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
            loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
            checkOpsEqual(op, loggedOp);

            op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.COMPACT.getName(), new ModelNode(true));
            executeForResult(op);
            bytes = server.receiveData();
            fullRecord = stripSyslogHeader(bytes);
            Assert.assertTrue(Pattern.matches("\\{.*", fullRecord)); //This regexp allows for new lines
            Assert.assertTrue(fullRecord.indexOf("#012") == -1);
            record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
            loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
            checkOpsEqual(op, loggedOp);

            op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.INCLUDE_DATE.getName(), new ModelNode(true));
            executeForResult(op);
            bytes = server.receiveData();
            op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.DATE_FORMAT.getName(), new ModelNode("yyyy/MM/dd HH-mm-ss"));
            executeForResult(op);
            bytes = server.receiveData();
            op = Util.getWriteAttributeOperation(createJsonFormatterAddress("escaped-formatter"), JsonAuditLogFormatterResourceDefinition.DATE_SEPARATOR.getName(), new ModelNode(" xxx "));
            executeForResult(op);
            bytes = server.receiveData();
            fullRecord = stripSyslogHeader(bytes);
            Assert.assertTrue(Pattern.matches("\\d\\d\\d\\d/\\d\\d/\\d\\d \\d\\d-\\d\\d-\\d\\d xxx \\{.*", fullRecord)); //This regexp checks for no new lines
            record = ModelNode.fromJSONString(fullRecord.substring(fullRecord.indexOf('{')));
            loggedOp = checkBootRecordHeader(record, 1, "core", false, false, true).get(0);
            checkOpsEqual(op, loggedOp);
        } finally {
            server.close();
        }
    }

    @Test
    public void testRuntimeFailureMetricsAndRecycle() throws Exception {
        ModelNode op = createAddSyslogHandlerTcpOperation("syslog", "test-formatter", InetAddress.getByName("localhost"), SYSLOG_PORT, null, MessageTransfer.OCTET_COUNTING);
        op.get(STEPS).asList().get(0).get(SyslogAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.getName()).set(2);
        executeForResult(op);

        final ModelNode readResource = Util.createOperation(READ_RESOURCE_OPERATION, AUDIT_ADDR);
        readResource.get(ModelDescriptionConstants.RECURSIVE).set(true);
        readResource.get(ModelDescriptionConstants.INCLUDE_RUNTIME).set(true);

        ModelNode result = executeForResult(readResource);
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 0, false);
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 0, false);

        //Delete the log directory so we start seeing failures in the file handler
        for (File file : logDir.listFiles()) {
            file.delete();
        }
        logDir.delete();

        executeForResult(createAddHandlerReferenceOperation("syslog"));

        result = executeForResult(readResource);
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 1, false);
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 1, false);

        result = executeForResult(readResource);
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 2, false);
        //syslog handler has been disabled after 2 failures
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 2, true);

        result = executeForResult(readResource);
        //File handler is disabled after 3 failures
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 3, true);
        //syslog handler should still be disabled
        checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 2, true);

        //Install syslog server so it is up and running
        SimpleSyslogServer server = SimpleSyslogServer.createTcp(6666, true);
        try {
            //File handler should still be disabled
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 3, true);
            //syslog handler should still be disabled
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 2, true);

            //Recycle the syslog handler so it reconnects and resets the failure count
            executeForResult(Util.createOperation(ModelDescriptionConstants.RECYCLE, createSyslogHandlerAddress("syslog")));
            server.receiveData();

            //Create the logging directory
            logDir.mkdir();

            result = executeForResult(readResource);
            server.receiveData();
            //File handler should still be disabled
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 3, true);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 0, false);

            //Recycle the file handler so it resets the failure count and starts logging again
            executeForResult(Util.createOperation(ModelDescriptionConstants.RECYCLE, createFileHandlerAddress("test-file")));
            server.receiveData();

            File file = new File(logDir, "test-file.log");
            Assert.assertTrue(file.exists());

            result = executeForResult(readResource);
            server.receiveData();
            //Both handlers should work again
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 3, 0, false);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 2, 0, false);

            //Recycle the file handler and make sure it gets backed up
            long fileSize = file.length();
            Assert.assertEquals(1, logDir.listFiles().length);
            executeForResult(Util.createOperation(ModelDescriptionConstants.RECYCLE, createFileHandlerAddress("test-file")));
            server.receiveData();
            Assert.assertEquals(2, logDir.listFiles().length);
            for (File current : logDir.listFiles()) {
                if (current.getName().equals(file.getName())){
                    Assert.assertFalse(fileSize == current.length());
                } else {
                    Assert.assertEquals(fileSize, current.length());
                }
            }

            //Finally just update the max failure counts and see that works
            op = Util.getWriteAttributeOperation(createFileHandlerAddress("test-file"), AuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.getName(), new ModelNode(7));
            executeForResult(op);
            server.receiveData();
            op = Util.getWriteAttributeOperation(createSyslogHandlerAddress("syslog"), AuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.getName(), new ModelNode(4));
            executeForResult(op);
            server.receiveData();

            result = executeForResult(readResource);
            server.receiveData();
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.FILE_HANDLER, "test-file"), 7, 0, false);
            checkHandlerRuntimeFailureMetrics(result.get(ModelDescriptionConstants.SYSLOG_HANDLER, "syslog"), 4, 0, false);
        } finally {
            server.close();
        }
    }
}
