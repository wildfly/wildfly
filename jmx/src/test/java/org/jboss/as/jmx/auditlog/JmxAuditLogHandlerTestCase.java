/*
 *
 *  * JBoss, Home of Professional Open Source.
 *  * Copyright 2013, Red Hat, Inc., and individual contributors
 *  * as indicated by the @author tags. See the copyright.txt file in the
 *  * distribution for a full listing of individual contributors.
 *  *
 *  * This is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * This software is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package org.jboss.as.jmx.auditlog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.QueryExp;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.RunningModeControl;
import org.jboss.as.controller.access.management.DelegatingConfigurableAuthorizer;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLoggerImpl;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.extension.ExtensionRegistry;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.domain.management.CoreManagementResourceDefinition;
import org.jboss.as.domain.management.audit.AccessAuditResourceDefinition;
import org.jboss.as.domain.management.audit.AuditLogLoggerResourceDefinition;
import org.jboss.as.domain.management.audit.EnvironmentNameReader;
import org.jboss.as.jmx.ExposeModelResourceResolved;
import org.jboss.as.jmx.JMXExtension;
import org.jboss.as.jmx.JMXSubsystemRootResource;
import org.jboss.as.jmx.JmxAuditLogHandlerReferenceResourceDefinition;
import org.jboss.as.jmx.JmxAuditLoggerResourceDefinition;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.jmx.test.util.AbstractControllerTestBase;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Don't use core-model test for this. It does not support runtime, and more importantly for backwards compatibility the audit logger cannot be used
 *
 * @author: Kabir Khan
 */
public class JmxAuditLogHandlerTestCase extends AbstractControllerTestBase {

    volatile PathManagerService pathManagerService;
    volatile ManagedAuditLogger auditLogger;
    volatile File logDir;
    volatile MBeanServer server;

    private static final String LAUNCH_TYPE = "launch-type";
    private static final String TYPE_STANDALONE = "STANDALONE";
    private final static String ANY_PLACEHOLDER = "$$$ ANY $$$ ANY $$$";
    private final static ObjectName OBJECT_NAME;
    static {
        try {
            OBJECT_NAME = new ObjectName("test:name=bean");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final List<ModelNode> bootOperations = new ArrayList<ModelNode>();

    public JmxAuditLogHandlerTestCase() {
        bootOperations.add(Util.createAddOperation(PathAddress.pathAddress(CoreManagementResourceDefinition.PATH_ELEMENT, AccessAuditResourceDefinition.PATH_ELEMENT)));
        ModelNode add = Util.createAddOperation(
                PathAddress.pathAddress(
                        CoreManagementResourceDefinition.PATH_ELEMENT,
                        AccessAuditResourceDefinition.PATH_ELEMENT,
                        PathElement.pathElement(ModelDescriptionConstants.JSON_FORMATTER, "test-formatter")));
        bootOperations.add(add);

        bootOperations.add(createAddFileHandlerOperation("test-file", "test-formatter", "test-file.log"));

        add = Util.createAddOperation(
                PathAddress.pathAddress(
                        CoreManagementResourceDefinition.PATH_ELEMENT,
                        AccessAuditResourceDefinition.PATH_ELEMENT,
                        AuditLogLoggerResourceDefinition.PATH_ELEMENT));
        add.get(ModelDescriptionConstants.LOG_READ_ONLY).set(true);
        bootOperations.add(add);

        PathAddress address = PathAddress.pathAddress(JMXSubsystemRootResource.PATH_ELEMENT);
        add = Util.createAddOperation(address);
        bootOperations.add(add);

        add = Util.createAddOperation(address.append(ExposeModelResourceResolved.PATH_ELEMENT));
        add.get(ExposeModelResourceResolved.DOMAIN_NAME.getName()).set("wildfly.test");
        bootOperations.add(add);

        address = address.append(JmxAuditLoggerResourceDefinition.PATH_ELEMENT);
        add = Util.createAddOperation(address);
        add.get(ModelDescriptionConstants.LOG_READ_ONLY).set(true);
        bootOperations.add(add);

        bootOperations.add(createAddJmxHandlerReferenceOperation("test-file"));
    }

    @Before
    public void installMBeans() throws Exception {
        server = getMBeanServer();
        server.registerMBean(new Bean(), OBJECT_NAME);
    }

    @After
    public void clearDependencies() throws Exception {
        auditLogger = null;
        logDir = null;

        if (server.isRegistered(OBJECT_NAME)) {
            server.unregisterMBean(OBJECT_NAME);
        }
        server = null;
    }

    protected ManagedAuditLogger getAuditLogger(){
        if (auditLogger == null){
            auditLogger = new ManagedAuditLoggerImpl("8.0.0", true);
        }
        return auditLogger;
    }

    @Test
    public void testAuditLoggerBootUp() throws Exception {
        File file = new File(logDir, "test-file.log");
        List<ModelNode> bootRecords = readFile(file, 1);

        ModelNode bootRecord = bootRecords.get(0);
        bootRecord = bootRecords.get(0);
        checkJmxBootRecordHeader(bootRecord, false, new String[] {Object.class.getName(), ObjectName.class.getName()}, new String[] {ANY_PLACEHOLDER, OBJECT_NAME.toString()});
    }

    @Test
    public void testAuditLoggerAddAndRemoveJmxReference() throws Exception {
        File file = new File(logDir, "test-file.log");
        readFile(file, 1);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        List<ModelNode> records = readFile(file, 2);
        checkJmxBootRecordHeader(records.get(1), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        ModelNode op = createRemoveJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        readFile(file, 2);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 2);

        op = createAddJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        readFile(file, 2);

        //File gets recreated and backed up here
        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 1);
        checkJmxBootRecordHeader(records.get(0), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});
    }

    @Test
    public void testSameFileCoreAndJmxAuditLog() throws Exception {
        File file = new File(logDir, "test-file.log");
        readFile(file, 1); //Tested in boo

        ModelNode op = createAddCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkCoreBootRecordHeader(records.get(1), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkCoreBootRecordHeader(records.get(2), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 4);
        checkJmxBootRecordHeader(records.get(3), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        //Remove and add the jmx handler reference, making sure that the core stuff still gets logged
        op = createRemoveJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 5);
        ops = checkCoreBootRecordHeader(records.get(4), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 5);

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 6);
        ops = checkCoreBootRecordHeader(records.get(5), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createAddJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 7);
        ops = checkCoreBootRecordHeader(records.get(6), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Remove and add the core handler reference, making sure that the jmx stuff still gets logged
        op = createRemoveCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 8);
        ops = checkCoreBootRecordHeader(records.get(7), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 8);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 9);
        checkJmxBootRecordHeader(records.get(8), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = createAddCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 10);
        ops = checkCoreBootRecordHeader(records.get(9), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Now remove all handler references and make sure that the file gets recycled when adding the jmx reference
        op = createRemoveJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 11);
        ops = checkCoreBootRecordHeader(records.get(10), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 12);
        ops = checkCoreBootRecordHeader(records.get(11), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 12);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 12);

        op = createAddJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 12);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 1); //File has been recycled
        checkJmxBootRecordHeader(records.get(0), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = createAddCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 2);
        ops = checkCoreBootRecordHeader(records.get(1), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Now remove all handler references and make sure that the file gets recycled when adding the jmx reference
        op = createRemoveCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkCoreBootRecordHeader(records.get(2), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = createRemoveJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 3);

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 3);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 3);

        op = createAddJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 3);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 1); //File has been recycled
        checkJmxBootRecordHeader(records.get(0), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = createAddCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        records = readFile(file, 2);
        ops = checkCoreBootRecordHeader(records.get(1), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 3);
        ops = checkCoreBootRecordHeader(records.get(2), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testSeparateFileCoreAndJmxAuditLog() throws Exception {
        File fileJmx = new File(logDir, "test-file.log");
        File fileCore = new File(logDir, "test-file2.log");
        readFile(fileJmx, 1);

        ModelNode op = createAddFileHandlerOperation("test-file2", "test-formatter", "test-file2.log");
        executeForResult(op);
        readFile(fileJmx, 1);
        Assert.assertFalse(fileCore.exists());

        op = createAddCoreHandlerReferenceOperation("test-file2");
        executeForResult(op);
        readFile(fileJmx, 1);
        List<ModelNode> records = readFile(fileCore, 1);
        List<ModelNode> ops = checkCoreBootRecordHeader(records.get(0), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Remove and add the jmx handler reference, making sure that core logging still gets logged
        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(fileJmx, 1);
        records = readFile(fileCore, 2);
        ops = checkCoreBootRecordHeader(records.get(1), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        readFile(fileCore, 2);
        records = readFile(fileJmx, 2);
        checkJmxBootRecordHeader(records.get(1), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = createRemoveJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        readFile(fileJmx, 2);
        records = readFile(fileCore, 3);
        ops = checkCoreBootRecordHeader(records.get(2), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        readFile(fileJmx, 2);

        op = createAddJmxHandlerReferenceOperation("test-file");
        executeForResult(op);
        readFile(fileJmx, 2);
        records = readFile(fileCore, 4);
        ops = checkCoreBootRecordHeader(records.get(3), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        readFile(fileCore, 4);
        records = readFile(fileJmx, 1); //File has been recycled
        checkJmxBootRecordHeader(records.get(0), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        //Remove and add the core handler reference, making sure that jmx logging still gets logged
        op = createRemoveCoreHandlerReferenceOperation("test-file2");
        executeForResult(op);
        readFile(fileJmx, 1);
        records = readFile(fileCore, 5);
        ops = checkCoreBootRecordHeader(records.get(4), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        readFile(fileCore, 5);
        records = readFile(fileJmx, 2); //File has been recycled
        checkJmxBootRecordHeader(records.get(1), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(fileJmx, 2);
        records = readFile(fileCore, 5);

        op = createAddCoreHandlerReferenceOperation("test-file2");
        executeForResult(op);
        readFile(fileJmx, 2);
        records = readFile(fileCore, 1); //File has been recycled
        ops = checkCoreBootRecordHeader(records.get(0), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));
    }

    @Test
    public void testDisableAndEnableAuditLog() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createAddCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkCoreBootRecordHeader(records.get(1), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 3);
        checkJmxBootRecordHeader(records.get(2), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        //Disable and reenable the core logger, making sure that jmx still gets logged
        op = createCoreAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, false);
        executeForResult(op);
        records = readFile(file, 4);
        ops = checkCoreBootRecordHeader(records.get(3), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(file, 4);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 5);
        checkJmxBootRecordHeader(records.get(4), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = createCoreAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, true);
        executeForResult(op);
        records = readFile(file, 6);
        ops = checkCoreBootRecordHeader(records.get(5), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Disable and reenable the jmx logger, making sure that core still gets logged
        op = createJMXAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, false);
        executeForResult(op);
        records = readFile(file, 7);
        ops = checkCoreBootRecordHeader(records.get(6), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 8);
        ops = checkCoreBootRecordHeader(records.get(7), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 8);

        op = createJMXAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, true);
        executeForResult(op);
        records = readFile(file, 9);
        ops = checkCoreBootRecordHeader(records.get(8), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 10);
        ops = checkCoreBootRecordHeader(records.get(9), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 11);
        checkJmxBootRecordHeader(records.get(10), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});
    }

    @Test
    public void testToggleReadOnly() throws Exception {
        File file = new File(logDir, "test-file.log");

        ModelNode op = createAddCoreHandlerReferenceOperation("test-file");
        executeForResult(op);
        List<ModelNode> records = readFile(file, 2);
        List<ModelNode> ops = checkCoreBootRecordHeader(records.get(1), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 3);
        checkJmxBootRecordHeader(records.get(2), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 4);
        ops = checkCoreBootRecordHeader(records.get(3), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        //Make the core logger not log read-only, making sure the jmx logger still does
        op = createCoreAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, false);
        executeForResult(op);
        records = readFile(file, 5);
        ops = checkCoreBootRecordHeader(records.get(4), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        readFile(file, 5);

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 6);
        checkJmxBootRecordHeader(records.get(5), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = createCoreAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, true);
        executeForResult(op);
        records = readFile(file, 7);
        ops = checkCoreBootRecordHeader(records.get(6), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Now read-only should be logged again for both
        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 8);
        checkJmxBootRecordHeader(records.get(7), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 9);
        ops = checkCoreBootRecordHeader(records.get(8), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        //Make the jmx logger not log read-only, making sure the core logger still does
        op = createJMXAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, false);
        executeForResult(op);
        records = readFile(file, 10);
        ops = checkCoreBootRecordHeader(records.get(9), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 11);
        ops = checkCoreBootRecordHeader(records.get(10), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 11);

        op = createJMXAuditLogWriteAttributeOperation(ModelDescriptionConstants.ENABLED, true);
        executeForResult(op);
        records = readFile(file, 12);
        ops = checkCoreBootRecordHeader(records.get(11), 1, false, false, true);
        checkOpsEqual(op, ops.get(0));

        //Now read-only should be logged again for both
        Assert.assertTrue(server.queryNames(OBJECT_NAME, null).contains(OBJECT_NAME));
        records = readFile(file, 13);
        checkJmxBootRecordHeader(records.get(12), true, new String[] {ObjectName.class.getName(), QueryExp.class.getName()}, new String[] {OBJECT_NAME.toString(), null});

        op = Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION, PathAddress.EMPTY_ADDRESS);
        executeForResult(op);
        records = readFile(file, 14);
        ops = checkCoreBootRecordHeader(records.get(13), 1, true, false, true);
        checkOpsEqual(op, ops.get(0));

    }


    private void checkOpsEqual(ModelNode rawDmr, ModelNode fromLog) {
        ModelNode expected = ModelNode.fromJSONString(rawDmr.toJSONString(true));
        Assert.assertEquals(expected, fromLog);

    }

    private final Pattern DATE_STAMP_PATTERN = Pattern.compile("[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9] [0-9][0-9]:[0-9][0-9]:[0-9][0-9] - \\{");

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
            reader.close();
        }
        Assert.assertEquals(list.toString(), expectedRecords, list.size());
        return list;
    }

    private MBeanServer getMBeanServer() throws Exception {
        ServiceController controller = getContainer().getRequiredService(MBeanServerService.SERVICE_NAME);
        return (PluggableMBeanServer)controller.getValue();
    }

    private ModelNode createAddFileHandlerOperation(String handlerName, String formatterName, String fileName) {
        ModelNode op = Util.createAddOperation(createFileHandlerAddress(handlerName));
        op.get(ModelDescriptionConstants.RELATIVE_TO).set("log.dir");
        op.get(ModelDescriptionConstants.PATH).set(fileName);
        op.get(ModelDescriptionConstants.FORMATTER).set(formatterName);
        return op;
    }

    private PathAddress createFileHandlerAddress(String handlerName){
        return PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                AccessAuditResourceDefinition.PATH_ELEMENT,
                PathElement.pathElement(ModelDescriptionConstants.FILE_HANDLER, handlerName));
    }

    private ModelNode createAddCoreHandlerReferenceOperation(String name){
        return Util.createAddOperation(createCoreHandlerReferenceAddress(name));
    }

    private ModelNode createRemoveCoreHandlerReferenceOperation(String name){
        return Util.createRemoveOperation(createCoreHandlerReferenceAddress(name));
    }

    private PathAddress createCoreHandlerReferenceAddress(String name){
        return PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                        AccessAuditResourceDefinition.PATH_ELEMENT,
                        AuditLogLoggerResourceDefinition.PATH_ELEMENT,
                        PathElement.pathElement(ModelDescriptionConstants.HANDLER, name));
    }

    private ModelNode createAddJmxHandlerReferenceOperation(String name){
        return Util.createAddOperation(createJmxHandlerReferenceAddress(name));
    }

    private ModelNode createRemoveJmxHandlerReferenceOperation(String name){
        return Util.createRemoveOperation(createJmxHandlerReferenceAddress(name));
    }

    private PathAddress createJmxHandlerReferenceAddress(String name){
        return PathAddress.pathAddress(
                JMXSubsystemRootResource.PATH_ELEMENT,
                JmxAuditLoggerResourceDefinition.PATH_ELEMENT,
                PathElement.pathElement(JmxAuditLogHandlerReferenceResourceDefinition.PATH_ELEMENT.getKey(), "test-file"));
    }

    private ModelNode createCoreAuditLogWriteAttributeOperation(String attr, boolean value) {
        return Util.getWriteAttributeOperation(PathAddress.pathAddress(
                CoreManagementResourceDefinition.PATH_ELEMENT,
                AccessAuditResourceDefinition.PATH_ELEMENT,
                AuditLogLoggerResourceDefinition.PATH_ELEMENT), attr, new ModelNode(value));
    }

    private ModelNode createJMXAuditLogWriteAttributeOperation(String attr, boolean value) {
        return Util.getWriteAttributeOperation(PathAddress.pathAddress(JMXSubsystemRootResource.PATH_ELEMENT, JmxAuditLoggerResourceDefinition.PATH_ELEMENT), attr, new ModelNode(value));
    }


    private List<ModelNode> checkCoreBootRecordHeader(ModelNode bootRecord, int ops, boolean readOnly, boolean booting, boolean success) {
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

    private void checkJmxBootRecordHeader(ModelNode bootRecord, boolean readOnly, String[] sig, String[] args) {
        Assert.assertEquals("jmx", bootRecord.get("type").asString());
        Assert.assertEquals(readOnly, bootRecord.get("r/o").asBoolean());
        Assert.assertFalse(bootRecord.get("booting").asBoolean());
        Assert.assertFalse(bootRecord.get("user").isDefined());
        Assert.assertFalse(bootRecord.get("domainUUID").isDefined());
        Assert.assertFalse(bootRecord.get("access").isDefined());
        Assert.assertFalse(bootRecord.get("remote-address").isDefined());
        //Assert.assertTrue(success, bootRecord.get("success").asBoolean());

        List<ModelNode> sigs = bootRecord.get("sig").asList();
        Assert.assertEquals(sig.length, sigs.size());
        for (int i = 0 ; i < sig.length ; i++) {
            Assert.assertEquals(sig[i], sigs.get(i).asString());
        }

        List<ModelNode> params = bootRecord.get("params").asList();
        Assert.assertEquals(args.length, params.size());
        for (int i = 0 ; i < args.length ; i++) {
            if (args[i] == null) {
                Assert.assertFalse(params.get(i).isDefined());
            } else if (!args[i].equals(ANY_PLACEHOLDER)) {
                Assert.assertEquals(args[i], params.get(i).asString());
            }
        }
    }



    @Override
    protected void addBootOperations(List<ModelNode> bootOperations) {
        bootOperations.addAll(this.bootOperations);
    }

    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        if (logDir == null){
            logDir = new File(".");
            logDir = new File(logDir, "target");
            logDir = new File(logDir, "audit-log-test-log-dir").getAbsoluteFile();
            if (!logDir.exists()){
                logDir.mkdirs();
            }
        }

        for (File file : logDir.listFiles()){
            file.delete();
        }

        pathManagerService = new PathManagerService() {
            {
                super.addHardcodedAbsolutePath(getContainer(), "log.dir", logDir.getAbsolutePath());
            }
        };
        GlobalOperationHandlers.registerGlobalOperations(registration, processType);
        registration.registerOperationHandler(CompositeOperationHandler.DEFINITION, CompositeOperationHandler.INSTANCE);

        registration.registerReadOnlyAttribute(LAUNCH_TYPE, new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                context.getResult().set(TYPE_STANDALONE);
                context.stepCompleted();
            }
        }, AttributeAccess.Storage.RUNTIME);


        TestServiceListener listener = new TestServiceListener();
        listener.reset(1);
        getContainer().addService(PathManagerService.SERVICE_NAME, pathManagerService)
                .addListener(listener)
                .install();

        try {
            listener.latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        registration.registerSubModel(PathResourceDefinition.createSpecified(pathManagerService));
        registration.registerSubModel(CoreManagementResourceDefinition.forStandaloneServer(new DelegatingConfigurableAuthorizer(), getAuditLogger(), pathManagerService, new EnvironmentNameReader() {
            public boolean isServer() {
                return true;
            }

            public String getServerName() {
                return "Test";
            }

            public String getHostName() {
                return null;
            }

            public String getProductName() {
                return null;
            }
        }));


        pathManagerService.addPathManagerResources(rootResource);


        ExtensionRegistry extensionRegistry = new ExtensionRegistry(ProcessType.STANDALONE_SERVER, new RunningModeControl(RunningMode.NORMAL), auditLogger, null);
        extensionRegistry.setPathManager(pathManagerService);
        extensionRegistry.setWriterRegistry(new NullConfigurationPersister());
        extensionRegistry.setSubsystemParentResourceRegistrations(registration, null);
        JMXExtension extension = new JMXExtension();
        extension.initialize(extensionRegistry.getExtensionContext("xxxx", false));

        rootResource.registerChild(CoreManagementResourceDefinition.PATH_ELEMENT, Resource.Factory.create());

        //registration.registerSubModel(JMXSubsystemRootResource.create(auditLogger));
    }


    private class TestServiceListener extends AbstractServiceListener<Object> {

        volatile CountDownLatch latch;
        Map<ServiceController.Transition, ServiceName> services = Collections.synchronizedMap(new LinkedHashMap<ServiceController.Transition, ServiceName>());


        void reset(int count) {
            latch = new CountDownLatch(count);
            services.clear();
        }

        public void transition(ServiceController<? extends Object> controller, ServiceController.Transition transition) {
            if (transition == ServiceController.Transition.STARTING_to_UP || transition == ServiceController.Transition.REMOVING_to_REMOVED) {
                services.put(transition, controller.getName());
                latch.countDown();
            }
        }
    }

    public interface BeanMBean {
        int getAttr();
        void setAttr(int i);
    }

    public static class Bean implements BeanMBean {
        volatile int attr;

        @Override
        public int getAttr() {
            return attr;
        }

        @Override
        public void setAttr(int i) {
            attr = i;
        }


    }
}
