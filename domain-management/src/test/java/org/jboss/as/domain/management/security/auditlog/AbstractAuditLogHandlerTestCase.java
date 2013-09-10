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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MESSAGE_TRANSFER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_FORMAT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLoggerImpl;
import org.jboss.as.controller.audit.SyslogAuditLogHandler;
import org.jboss.as.controller.audit.SyslogAuditLogHandler.Transport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.domain.management.audit.FileAuditLogHandlerResourceDefinition;
import org.jboss.as.domain.management.audit.SyslogAuditLogProtocolResourceDefinition;
import org.jboss.as.domain.management.security.util.ManagementControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.handlers.SyslogHandler;
import org.junit.After;
import org.junit.Assert;
import org.xnio.IoUtils;

/**
 * Don't use core-model test for this. It does not support runtime, and more importantly for backwards compatibility the audit logger cannot be used
 *
 * @author: Kabir Khan
 */
public class AbstractAuditLogHandlerTestCase extends ManagementControllerTestBase {
    protected static final PathAddress AUDIT_ADDR = PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuditResourceDefinition.PATH_ELEMENT);
    protected static final int SYSLOG_PORT = 6666;
    protected static final int SYSLOG_PORT2 = 6667;

    protected final List<ModelNode> bootOperations = new ArrayList<ModelNode>();

    public AbstractAuditLogHandlerTestCase(boolean enabled) {

        bootOperations.add(Util.createAddOperation(AUDIT_ADDR));
        ModelNode add = Util.createAddOperation(createJsonFormatterAddress("test-formatter"));
        bootOperations.add(add);

        add = createAddFileHandlerOperation("test-file", "test-formatter", "test-file.log");
        add.get(FileAuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.getName()).set(3);
        bootOperations.add(add);

        add = Util.createAddOperation(
                AUDIT_ADDR.append(AuditLogLoggerResourceDefinition.PATH_ELEMENT));
        add.get(ModelDescriptionConstants.ENABLED).set(enabled);
        add.get(ModelDescriptionConstants.LOG_READ_ONLY).set(true);
        bootOperations.add(add);
        bootOperations.add(createAddHandlerReferenceOperation("test-file"));
    }


    @After
    public void clearDependencies(){
        auditLogger = null;
        logDir = null;
    }

    protected ManagedAuditLogger getAuditLogger(){
        if (auditLogger == null){
            auditLogger = new ManagedAuditLoggerImpl("8.0.0", true);
        }
        return auditLogger;
    }

    protected void checkHandlerRuntimeFailureMetrics(ModelNode handler, int maxFailureCount, int failureCount, boolean disabled) {
        Assert.assertEquals(maxFailureCount, handler.get(AuditLogHandlerResourceDefinition.MAX_FAILURE_COUNT.getName()).asInt());
        Assert.assertEquals(failureCount, handler.get(AuditLogHandlerResourceDefinition.FAILURE_COUNT.getName()).asInt());
        Assert.assertEquals(disabled, handler.get(AuditLogHandlerResourceDefinition.DISABLED_DUE_TO_FAILURE.getName()).asBoolean());
    }

    protected String stripSyslogHeader(byte[] bytes) throws UnsupportedEncodingException {
        String s = new String(bytes, "utf-8");
        int i = s.indexOf(" - - ");
        return s.substring(i + 6);
    }

    protected ModelNode getSyslogRecord(byte[] bytes) throws UnsupportedEncodingException {
        String msg = new String(bytes, "utf-8");
        return getSyslogRecord(msg);
    }

    protected ModelNode getSyslogRecord(String msg) {
        msg = msg.substring(msg.indexOf('{')).replace("#012", "\n");
        return ModelNode.fromJSONString(msg);
    }

    protected void checkOpsEqual(ModelNode rawDmr, ModelNode fromLog) {
        ModelNode expected = ModelNode.fromJSONString(rawDmr.toJSONString(true));
        Assert.assertEquals(expected, fromLog);

    }

    private final Pattern DATE_STAMP_PATTERN = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{");

    protected List<ModelNode> readFile(File file, int expectedRecords) throws IOException {
        List<ModelNode> list = new ArrayList<ModelNode>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            StringWriter writer = null;
            String line = reader.readLine();
            while (line != null) {
                if (DATE_STAMP_PATTERN.matcher(line).matches()) {
                    if (writer != null) {
                        list.add(ModelNode.fromJSONString(writer.getBuffer().toString()));
                    }
                    writer = new StringWriter();
                    writer.append("{");
                } else {
                    writer.append("\n" + line);
                }
                line = reader.readLine();
            }
            if (writer != null) {
                list.add(ModelNode.fromJSONString(writer.getBuffer().toString()));
            }
        } finally {
            IoUtils.safeClose(reader);
        }
        Assert.assertEquals(list.toString(), expectedRecords, list.size());
        return list;
    }

    protected String readFullFileRecord(File file) throws IOException {
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            boolean firstLine = true;
            StringWriter writer = new StringWriter();
            String line = reader.readLine();
            while (line != null) {
                if (!firstLine) {
                    writer.append("\n");
                } else {
                    firstLine = false;
                }
                writer.append(line);
                line = reader.readLine();
            }
            return writer.toString();
        } finally {
            IoUtils.safeClose(reader);
        }
    }

    protected ModelNode createAuditLogWriteAttributeOperation(String attr, boolean value) {
        return Util.getWriteAttributeOperation(AUDIT_ADDR.append(AuditLogLoggerResourceDefinition.PATH_ELEMENT), attr, new ModelNode(value));
    }

    protected ModelNode createAddFileHandlerOperation(String handlerName, String formatterName, String fileName) {
        ModelNode op = Util.createAddOperation(createFileHandlerAddress(handlerName));
        op.get(ModelDescriptionConstants.RELATIVE_TO).set("log.dir");
        op.get(ModelDescriptionConstants.PATH).set(fileName);
        op.get(ModelDescriptionConstants.FORMATTER).set(formatterName);
        return op;
    }

    protected ModelNode createRemoveFileHandlerOperation(String handlerName) {
        return Util.createRemoveOperation(createFileHandlerAddress(handlerName));
    }

    protected PathAddress createFileHandlerAddress(String handlerName){
        return AUDIT_ADDR.append(PathElement.pathElement(ModelDescriptionConstants.FILE_HANDLER, handlerName));
    }

    protected ModelNode createRemoveJsonFormatterOperation(String formatterName) {
        return Util.createRemoveOperation(createJsonFormatterAddress(formatterName));
    }

    protected PathAddress createJsonFormatterAddress(String formatterName) {
        return AUDIT_ADDR.append(
                PathElement.pathElement(ModelDescriptionConstants.JSON_FORMATTER, formatterName));
    }


    protected ModelNode createAddSyslogHandlerUdpOperation(String handlerName, String formatterName, InetAddress addr, int port, SyslogHandler.SyslogType syslogFormat, int maxLength){
        ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(STEPS).setEmptyList();

        ModelNode handler = Util.createAddOperation(createSyslogHandlerAddress(handlerName));
        if (syslogFormat != null){
            handler.get(SYSLOG_FORMAT).set(syslogFormat.toString());
        }
        if (maxLength > 0) {
            handler.get(MAX_LENGTH).set(maxLength);
        }
        handler.get(ModelDescriptionConstants.FORMATTER).set(formatterName);
        composite.get(STEPS).add(handler);

        ModelNode protocol = Util.createAddOperation(createSyslogHandlerProtocolAddress(handlerName, SyslogAuditLogHandler.Transport.UDP));
        protocol.get(HOST).set(addr.getHostName());
        protocol.get(PORT).set(port);
        composite.get(STEPS).add(protocol);

        return composite;
    }

    protected ModelNode createAddSyslogHandlerTcpOperation(String handlerName, String formatterName, InetAddress addr, int port, SyslogHandler.SyslogType syslogFormat, SyslogAuditLogHandler.MessageTransfer transfer){
        ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(STEPS).setEmptyList();

        ModelNode handler = Util.createAddOperation(createSyslogHandlerAddress(handlerName));
        if (syslogFormat != null){
            handler.get(SYSLOG_FORMAT).set(syslogFormat.toString());
        }
        handler.get(ModelDescriptionConstants.FORMATTER).set(formatterName);
        composite.get(STEPS).add(handler);

        ModelNode protocol = Util.createAddOperation(createSyslogHandlerProtocolAddress(handlerName, SyslogAuditLogHandler.Transport.TCP));
        protocol.get(HOST).set(addr.getHostName());
        protocol.get(PORT).set(port);
        if (transfer != null) {
            protocol.get(MESSAGE_TRANSFER).set(transfer.name());
        }
        composite.get(STEPS).add(protocol);

        return composite;
    }

    protected ModelNode createAddSyslogHandlerTlsOperation(String handlerName, String formatterName, InetAddress addr, int port, SyslogHandler.SyslogType syslogFormat,
            SyslogAuditLogHandler.MessageTransfer transfer, File truststorePath, String trustPwd, File clientCertPath, String clientCertPwd){
        ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(STEPS).setEmptyList();

        ModelNode handler = Util.createAddOperation(createSyslogHandlerAddress(handlerName));
        if (syslogFormat != null){
            handler.get(SYSLOG_FORMAT).set(syslogFormat.toString());
        }
        handler.get(ModelDescriptionConstants.FORMATTER).set(formatterName);
        composite.get(STEPS).add(handler);

        ModelNode protocol = Util.createAddOperation(createSyslogHandlerProtocolAddress(handlerName, SyslogAuditLogHandler.Transport.TLS));
        protocol.get(HOST).set(addr.getHostName());
        protocol.get(PORT).set(port);
        if (transfer != null) {
            protocol.get(MESSAGE_TRANSFER).set(transfer.name());
        }
        composite.get(STEPS).add(protocol);

        ModelNode truststore = Util.createAddOperation(
                createSyslogHandlerProtocolAddress("syslog-test", Transport.TLS).append(
                        PathElement.pathElement(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.TRUSTSTORE)));
        truststore.get(SyslogAuditLogProtocolResourceDefinition.TlsKeyStore.KEYSTORE_PATH.getName()).set(truststorePath.getAbsolutePath());
        truststore.get(SyslogAuditLogProtocolResourceDefinition.TlsKeyStore.KEYSTORE_PASSWORD.getName()).set(trustPwd);
        composite.get(STEPS).add(truststore);

        if (clientCertPath != null) {
            ModelNode clientCert = Util.createAddOperation(createSyslogHandlerProtocolAddress("syslog-test", Transport.TLS).append(
                    PathElement.pathElement(ModelDescriptionConstants.AUTHENTICATION, ModelDescriptionConstants.CLIENT_CERT_STORE)));
            clientCert.get(SyslogAuditLogProtocolResourceDefinition.TlsKeyStore.KEYSTORE_PATH.getName()).set(clientCertPath.getAbsolutePath());
            clientCert.get(SyslogAuditLogProtocolResourceDefinition.TlsKeyStore.KEYSTORE_PASSWORD.getName()).set(clientCertPwd);
            composite.get(STEPS).add(clientCert);
        }
        return composite;
    }

    protected PathAddress createSyslogHandlerAddress(String handlerName){
        return AUDIT_ADDR.append(PathElement.pathElement(ModelDescriptionConstants.SYSLOG_HANDLER, handlerName));
    }

    protected PathAddress createSyslogHandlerProtocolAddress(String handlerName, SyslogAuditLogHandler.Transport transport){
        return AUDIT_ADDR.append(
                PathElement.pathElement(ModelDescriptionConstants.SYSLOG_HANDLER, handlerName),
                PathElement.pathElement(PROTOCOL, transport.name().toLowerCase()));
    }

    protected ModelNode createAddHandlerReferenceOperation(String name){
        return Util.createAddOperation(createHandlerReferenceAddress(name));
    }

    protected ModelNode createRemoveHandlerReferenceOperation(String name){
        return Util.createRemoveOperation(createHandlerReferenceAddress(name));
    }

    protected PathAddress createHandlerReferenceAddress(String name){
        return AUDIT_ADDR.append(
                        AuditLogLoggerResourceDefinition.PATH_ELEMENT,
                        PathElement.pathElement(ModelDescriptionConstants.HANDLER, name));
    }

    protected List<ModelNode> checkBootRecordHeader(ModelNode bootRecord, int ops, String type, boolean readOnly, boolean booting, boolean success) {
        Assert.assertEquals("core", bootRecord.get("type").asString());
        Assert.assertEquals(readOnly, bootRecord.get("r/o").asBoolean());
        Assert.assertEquals(booting, bootRecord.get("booting").asBoolean());
        Assert.assertFalse(bootRecord.get("user").isDefined());
        Assert.assertFalse(bootRecord.get("domainUUID").isDefined());
        Assert.assertFalse(bootRecord.get("access").isDefined());
        Assert.assertFalse(bootRecord.get("remote-address").isDefined());
        Assert.assertEquals(success, bootRecord.get("success").asBoolean());
        List<ModelNode> operations = bootRecord.get("ops").asList();
        Assert.assertEquals(ops, operations.size());
        return operations;
    }


    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) {
        bootOperations.addAll(this.bootOperations);
    }

}
