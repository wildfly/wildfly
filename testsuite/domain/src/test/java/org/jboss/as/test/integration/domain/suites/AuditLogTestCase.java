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
package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_UUID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.JSON_FORMATTER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROTOCOL;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSLOG_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UDP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.audit.JsonAuditLogItemFormatter;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xnio.IoUtils;

/**
 * @author: Kabir Khan
 */
public class AuditLogTestCase {
    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil masterLifecycleUtil;
    private static DomainLifecycleUtil slaveLifecycleUtil;

    private static final int SYSLOG_PORT = 10514;

    private static final int ADJUSTED_SECOND = TimeoutUtil.adjust(1000);


    private PathAddress masterAuditAddress = PathAddress.pathAddress(
            PathElement.pathElement(HOST, "master"),
            CoreManagementResourceDefinition.PATH_ELEMENT,
            AccessAuditResourceDefinition.PATH_ELEMENT);

    private PathAddress masterCoreLogggerAddress = masterAuditAddress.append(AuditLogLoggerResourceDefinition.PATH_ELEMENT);

    private PathAddress masterServerLoggerAddress = masterAuditAddress.append(AuditLogLoggerResourceDefinition.HOST_SERVER_PATH_ELEMENT);

    private PathAddress slaveAuditAddress = PathAddress.pathAddress(
            PathElement.pathElement(HOST, "slave"),
            CoreManagementResourceDefinition.PATH_ELEMENT,
            AccessAuditResourceDefinition.PATH_ELEMENT);


    private PathAddress slaveCoreLogggerAddress = slaveAuditAddress.append(AuditLogLoggerResourceDefinition.PATH_ELEMENT);

    private PathAddress slaveServerLoggerAddress = slaveAuditAddress.append(AuditLogLoggerResourceDefinition.HOST_SERVER_PATH_ELEMENT);

    private static File masterAuditLog;
    private static File masterServerAuditLog;
    private static File slaveAuditLog;
    private static File slaveServerAuditLog;
    private static SimpleSyslogServer server;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(AuditLogTestCase.class.getSimpleName());
        masterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        slaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();

        File file = new File(testSupport.getDomainMasterConfiguration().getDomainDirectory());
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        masterAuditLog = file;

        file = new File(testSupport.getDomainMasterConfiguration().getDomainDirectory());
        file = new File(file, "servers");
        file = new File(file, "main-one");
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        masterServerAuditLog = file;

        file = new File(testSupport.getDomainSlaveConfiguration().getDomainDirectory());
        file = new File(file, "data");
        file = new File(file, "audit-log.log");
        slaveAuditLog = file;

        file = new File(testSupport.getDomainSlaveConfiguration().getDomainDirectory());
        file = new File(file, "data");
        file = new File(file, "servers");
        file = new File(file, "main-three");
        file = new File(file, "audit-log.log");
        slaveServerAuditLog = file;

        //Start up syslog server
        server = SimpleSyslogServer.createUdp(SYSLOG_PORT);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport = null;
        masterLifecycleUtil = null;
        slaveLifecycleUtil = null;
        DomainTestSuite.stopSupport();

        //Stop syslog server
        server.close();
    }

    @Before
    public void before() {
        masterAuditLog.delete();
        masterServerAuditLog.delete();
        slaveAuditLog.delete();
        slaveServerAuditLog.delete();
    }

    @After
    public void turnOffSysylog() throws Exception {
        ModelNode op = Util.getWriteAttributeOperation(masterCoreLogggerAddress, ENABLED, new ModelNode(false));
        masterLifecycleUtil.executeForResult(op);

        op = Util.getWriteAttributeOperation(slaveCoreLogggerAddress, ENABLED, new ModelNode(false));
        slaveLifecycleUtil.executeForResult(op);

        op = Util.getWriteAttributeOperation(masterServerLoggerAddress, ENABLED, new ModelNode(false));
        masterLifecycleUtil.executeForResult(op);

        op = Util.getWriteAttributeOperation(slaveServerLoggerAddress, ENABLED, new ModelNode(false));
        slaveLifecycleUtil.executeForResult(op);
    }

    @Test
    public void testFileAuditLogInDomain() throws Exception {
        Assert.assertFalse(masterAuditLog.exists());
        Assert.assertFalse(masterServerAuditLog.exists());
        Assert.assertFalse(slaveAuditLog.exists());
        Assert.assertFalse(slaveServerAuditLog.exists());

        ModelNode op = Util.getWriteAttributeOperation(masterCoreLogggerAddress, ENABLED, new ModelNode(true));
        masterLifecycleUtil.executeForResult(op);
        Assert.assertTrue(masterAuditLog.exists());
        Assert.assertFalse(masterServerAuditLog.exists());
        Assert.assertFalse(slaveAuditLog.exists());
        Assert.assertFalse(slaveServerAuditLog.exists());

        op = Util.getWriteAttributeOperation(slaveCoreLogggerAddress, ENABLED, new ModelNode(true));
        slaveLifecycleUtil.executeForResult(op);
        Assert.assertTrue(masterAuditLog.exists());
        Assert.assertFalse(masterServerAuditLog.exists());
        Assert.assertTrue(slaveAuditLog.exists());
        Assert.assertFalse(slaveServerAuditLog.exists());

        op = Util.getWriteAttributeOperation(masterServerLoggerAddress, ENABLED, new ModelNode(true));
        masterLifecycleUtil.executeForResult(op);
        Assert.assertTrue(masterAuditLog.exists());
        Assert.assertTrue(masterServerAuditLog.exists());
        Assert.assertTrue(slaveAuditLog.exists());
        Assert.assertFalse(slaveServerAuditLog.exists());

        op = Util.getWriteAttributeOperation(slaveServerLoggerAddress, ENABLED, new ModelNode(true));
        slaveLifecycleUtil.executeForResult(op);
        Assert.assertTrue(masterAuditLog.exists());
        Assert.assertTrue(masterServerAuditLog.exists());
        Assert.assertTrue(slaveAuditLog.exists());
        Assert.assertTrue(slaveServerAuditLog.exists());

        masterAuditLog.delete();
        slaveAuditLog.delete();
        masterServerAuditLog.delete();
        slaveServerAuditLog.delete();

        String propertyName = "test" + System.currentTimeMillis();
        ModelNode addOp = Util.createAddOperation(PathAddress.pathAddress(SYSTEM_PROPERTY, propertyName));
        addOp.get(VALUE).set("abc");
        masterLifecycleUtil.executeForResult(addOp);
        Assert.assertTrue(masterAuditLog.exists());
        Assert.assertTrue(masterServerAuditLog.exists());
        Assert.assertTrue(slaveAuditLog.exists());
        Assert.assertTrue(slaveServerAuditLog.exists());

        ModelNode masterRecord = readFile(masterAuditLog, 1).get(0);
        Assert.assertFalse(masterRecord.get(DOMAIN_UUID).isDefined());
        ModelNode masterOp = getOp(masterRecord);
        compareOpsWithoutHeaders(addOp, masterOp);
        Assert.assertTrue(masterOp.get(OPERATION_HEADERS, DOMAIN_UUID).isDefined());
        String domainUUID = masterOp.get(OPERATION_HEADERS, DOMAIN_UUID).asString();


        ModelNode masterServerRecord = readFile(masterServerAuditLog, 1).get(0);
        Assert.assertEquals(domainUUID, masterServerRecord.get(JsonAuditLogItemFormatter.DOMAIN_UUID).asString());
        ModelNode masterServerOp = getOp(masterServerRecord);
        Assert.assertEquals(domainUUID, masterServerOp.get(OPERATION_HEADERS, DOMAIN_UUID).asString());
        compareOpsWithoutHeaders(addOp, masterServerOp, BOOT_TIME);

        boolean mainThree = false;
        boolean otherTwo = false;
        boolean domainCopy = false;
        List<ModelNode> slaveRecords = readFile(slaveAuditLog, 3);
        for (ModelNode slaveRecord : slaveRecords){
            Assert.assertEquals(domainUUID, slaveRecord.get(JsonAuditLogItemFormatter.DOMAIN_UUID).asString());
            ModelNode slaveOp = getOp(slaveRecord);
            Assert.assertEquals(domainUUID, slaveOp.get(OPERATION_HEADERS, DOMAIN_UUID).asString());

            //The addresses will be different for the different ops so compare a copy with the address adjusted
            ModelNode addOpClone = addOp.clone();
            String address = slaveOp.get(OP_ADDR).asString();
            if (address.contains("main-three")){
                Assert.assertFalse(mainThree);
                mainThree = true;
                addOpClone.get(OP_ADDR).set(
                        PathAddress.pathAddress(
                                PathElement.pathElement(HOST, "slave"),
                                PathElement.pathElement(SERVER, "main-three"),
                                PathElement.pathElement(SYSTEM_PROPERTY, propertyName)).toModelNode());
            } else if (address.contains("other-two")){
                Assert.assertFalse(otherTwo);
                otherTwo = true;
                addOpClone.get(OP_ADDR).set(
                        PathAddress.pathAddress(
                                PathElement.pathElement(HOST, "slave"),
                                PathElement.pathElement(SERVER, "other-two"),
                                PathElement.pathElement(SYSTEM_PROPERTY, propertyName)).toModelNode());
            } else {
                Assert.assertFalse(domainCopy);
                domainCopy = true;
            }

            compareOpsWithoutHeaders(addOpClone, slaveOp, BOOT_TIME);
        }
        Assert.assertTrue(mainThree);
        Assert.assertTrue(otherTwo);
        Assert.assertTrue(domainCopy);

        ModelNode slaveServerRecord = readFile(slaveServerAuditLog, 1).get(0);
        Assert.assertEquals(domainUUID, slaveServerRecord.get(JsonAuditLogItemFormatter.DOMAIN_UUID).asString());
        ModelNode slaveServerOp = getOp(slaveServerRecord);
        Assert.assertEquals(domainUUID, slaveServerOp.get(OPERATION_HEADERS, DOMAIN_UUID).asString());
        compareOpsWithoutHeaders(addOp, slaveServerOp, BOOT_TIME);
    }

    @Test
    public void testCanAddSyslogServerToMaster() throws Exception {
        testCanAddSyslogServer(masterAuditAddress);
    }

    @Test
    public void testCanAddSyslogServerToSlave() throws Exception {
        testCanAddSyslogServer(slaveAuditAddress);
    }

    private void testCanAddSyslogServer(PathAddress baseAddress) throws Exception {
        final PathAddress handlerAddress = PathAddress.pathAddress(baseAddress.append(SYSLOG_HANDLER, "test-syslog"));

        //First enable all the loggers
        ModelNode op = Util.getWriteAttributeOperation(masterCoreLogggerAddress, ENABLED, new ModelNode(true));
        masterLifecycleUtil.executeForResult(op);
        op = Util.getWriteAttributeOperation(slaveCoreLogggerAddress, ENABLED, new ModelNode(true));
        slaveLifecycleUtil.executeForResult(op);
        op = Util.getWriteAttributeOperation(masterServerLoggerAddress, ENABLED, new ModelNode(true));
        masterLifecycleUtil.executeForResult(op);
        op = Util.getWriteAttributeOperation(slaveServerLoggerAddress, ENABLED, new ModelNode(true));
        slaveLifecycleUtil.executeForResult(op);



        final ModelNode compositeAdd = new ModelNode();
        compositeAdd.get(OP).set(COMPOSITE);
        ModelNode add = Util.createAddOperation(handlerAddress);
        add.get(FORMATTER).set(JSON_FORMATTER);
        compositeAdd.get(STEPS).add(add);

        add = Util.createAddOperation(handlerAddress.append(PathElement.pathElement(PROTOCOL, UDP)));
        add.get(HOST).set("localhost");
        add.get(PORT).set(SYSLOG_PORT);
        compositeAdd.get(STEPS).add(add);

        masterLifecycleUtil.executeForResult(compositeAdd);

        try {
            expectNoSyslogData();

            //Add handler reference to server logger and check it gets logged, then remove handler reference when done
            final PathElement testHandlerReference = PathElement.pathElement(HANDLER, "test-syslog");
            final PathAddress serverLoggerHandlerReferenceAddress = baseAddress.append(AuditLogLoggerResourceDefinition.HOST_SERVER_PATH_ELEMENT).append(testHandlerReference);
            final ModelNode addServerLoggerHandlerReference = Util.createAddOperation(serverLoggerHandlerReferenceAddress);
            masterLifecycleUtil.executeForResult(addServerLoggerHandlerReference);
            boolean removed = false;
            try {
                //Master has one server, slave has two
                final List<String> servers = new ArrayList<String>();
                if (baseAddress.getElement(0).getValue().equals("master")) {
                    servers.add("main-one");
                } else {
                    servers.add("main-three");
                    servers.add("other-two");
                }

                final PathAddress serverLoggerAddressOnServer = PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuditResourceDefinition.PATH_ELEMENT, AuditLogLoggerResourceDefinition.PATH_ELEMENT);
                for (int i = 0 ; i < servers.size() ; i++) {
                    expectSyslogData(serverLoggerAddressOnServer.append(testHandlerReference), addServerLoggerHandlerReference, false);
                }
                expectNoSyslogData();

                //Now disable the server logger
                PathAddress serverLoggerAddress = baseAddress.append(AuditLogLoggerResourceDefinition.HOST_SERVER_PATH_ELEMENT);
                final ModelNode writeFalseEnabled = Util.getWriteAttributeOperation(serverLoggerAddress, ENABLED, new ModelNode(false));
                masterLifecycleUtil.executeForResult(writeFalseEnabled);
                for (int i = 0 ; i < servers.size() ; i++) {
                    expectSyslogData(serverLoggerAddressOnServer, writeFalseEnabled, false);
                }
                expectNoSyslogData();

                //Restart a server
                final PathAddress serverAddress = PathAddress.pathAddress(baseAddress.getElement(0)).append(SERVER_CONFIG, servers.get(0));
                final ModelNode restartOp = Util.createEmptyOperation("reload", serverAddress);
                restartOp.get(BLOCKING).set(true);
                expectNoSyslogData();

                //Now enable the server logger again
                final ModelNode writeTrueEnabled = Util.getWriteAttributeOperation(serverLoggerAddress, ENABLED, new ModelNode(true));
                masterLifecycleUtil.executeForResult(writeTrueEnabled);
                for (int i = 0 ; i < servers.size() ; i++) {
                    expectSyslogData(serverLoggerAddressOnServer, writeTrueEnabled, false);
                }
                expectNoSyslogData();


                //Remove handler address
                final ModelNode removeServerLoggerHandlerReference = Util.createRemoveOperation(serverLoggerHandlerReferenceAddress);
                masterLifecycleUtil.executeForResult(removeServerLoggerHandlerReference);
                removed = true;
                for (int i = 0 ; i < servers.size() ; i++) {
                    expectSyslogData(serverLoggerAddressOnServer.append(testHandlerReference), removeServerLoggerHandlerReference, false);
                }
                expectNoSyslogData();
            } finally {
                if (!removed) {
                    masterLifecycleUtil.executeForResult(Util.createRemoveOperation(serverLoggerHandlerReferenceAddress));
                }
            }


            //Add handler reference to host logger and check it gets logged, then remove handler reference when done
            final PathAddress hostLoggerHandlerReferenceAddress = baseAddress.append(AuditLogLoggerResourceDefinition.PATH_ELEMENT).append(PathElement.pathElement(HANDLER, "test-syslog"));
            final ModelNode addHostLoggerHandlerReference = Util.createAddOperation(hostLoggerHandlerReferenceAddress);
            masterLifecycleUtil.executeForResult(addHostLoggerHandlerReference);
            removed = false;
            try {
                boolean master = baseAddress.getElement(0).getValue().equals("master");
                expectSyslogData(hostLoggerHandlerReferenceAddress, addHostLoggerHandlerReference, master);
                expectNoSyslogData();
                final ModelNode removeHostLoggerHandlerReference = Util.createRemoveOperation(hostLoggerHandlerReferenceAddress);
                masterLifecycleUtil.executeForResult(removeHostLoggerHandlerReference);
                removed = true;
                expectSyslogData(hostLoggerHandlerReferenceAddress, removeHostLoggerHandlerReference, master);
                expectNoSyslogData();
            } finally {
                if (!removed) {
                    masterLifecycleUtil.executeForResult(Util.createRemoveOperation(hostLoggerHandlerReferenceAddress));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            masterLifecycleUtil.executeForResult(Util.createRemoveOperation(handlerAddress));
        }
    }

    private ModelNode expectSyslogData(PathAddress pathAddress, ModelNode op, boolean masterOnlyOp) throws Exception {
        byte[] data = server.receiveData(5 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(data);
        String msg = new String(data, StandardCharsets.UTF_8);
        msg = msg.substring(msg.indexOf('{')).replace("#012", "\n");
        ModelNode syslogData =  ModelNode.fromJSONString(msg);

        Assert.assertEquals(!masterOnlyOp, syslogData.hasDefined("domainUUID"));
        Assert.assertEquals(AccessMechanism.NATIVE.toString(), syslogData.get(ACCESS).asString());
        Assert.assertTrue(syslogData.hasDefined("remote-address") && syslogData.get("remote-address").asString().length() > 0);
        Assert.assertFalse(syslogData.hasDefined("r/o") && syslogData.get("r/o").asBoolean());
        Assert.assertFalse(syslogData.hasDefined("booting") && syslogData.get("booting").asBoolean());
        Assert.assertEquals("$local", syslogData.get(USER).asString());
        Assert.assertTrue(syslogData.hasDefined(SUCCESS) && syslogData.get(SUCCESS).asBoolean());

        List<ModelNode> ops = syslogData.get("ops").asList();
        Assert.assertEquals(1, ops.size());
        ModelNode loggedOp = ops.get(0);
        loggedOp.remove(OPERATION_HEADERS);

        ModelNode expectedOperation = op.clone();
        expectedOperation.get(OP_ADDR).set(pathAddress.toModelNode());
        //Do this to make the format of the address the same as for the one from the syslog
        expectedOperation = ModelNode.fromJSONString(expectedOperation.toJSONString(true));

        Assert.assertEquals(expectedOperation, loggedOp);

        return syslogData;
    }

    private void expectNoSyslogData() throws InterruptedException {
        byte[] data = server.receiveData(1 * ADJUSTED_SECOND, TimeUnit.MILLISECONDS);
        Assert.assertNull(data);
    }

    private ModelNode getOp(ModelNode record){
        List<ModelNode> ops = record.get("ops").asList();
        Assert.assertEquals(1, ops.size());
        return ops.get(0);
    }

    private void compareOpsWithoutHeaders(ModelNode expected, ModelNode actual, String...undefinedAttributesToRemoveFromActual){
        ModelNode expectedClone = expected.clone();
        expectedClone.remove(OPERATION_HEADERS);
        ModelNode actualClone = actual.clone();
        actualClone.remove(OPERATION_HEADERS);

        for (String removal : undefinedAttributesToRemoveFromActual){
            if (!actualClone.hasDefined(removal)){
                actualClone.remove(removal);
            }
        }

        //Ops marshalled from json handle the addresses slightly differently.
        //The 'raw' dmr op will have the address as a list of property model values
        //The one marshalled from json will have it as a list of object model values
        //So marshall/unmarshal both ops to json for this comparison
        expectedClone = ModelNode.fromJSONString(expectedClone.toJSONString(true));
        actualClone = ModelNode.fromJSONString(actualClone.toJSONString(true));

        Assert.assertEquals(expectedClone, actualClone);
    }

    private final Pattern DATE_STAMP_PATTERN = Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d - \\{");

    private List<ModelNode> readFile(File file, int expectedRecords) throws IOException {
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
}
