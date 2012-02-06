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

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ANY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DIRECTORY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_RESULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateFailedResponse;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.CompositeOperationHandler;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.SnapshotDeleteHandler;
import org.jboss.as.controller.operations.common.SnapshotListHandler;
import org.jboss.as.controller.operations.common.SnapshotTakeHandler;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of various management operations involving core resources like system properties, paths, interfaces, socket bindings.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class CoreResourceManagementTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static final String TEST = "test";
    private static final ModelNode ROOT_PROP_ADDRESS = new ModelNode();
    private static final ModelNode SERVER_GROUP_PROP_ADDRESS = new ModelNode();
    private static final ModelNode HOST_PROP_ADDRESS = new ModelNode();
    private static final ModelNode HOST_CLASSLOADING_ADDRESS = new ModelNode();
    private static final ModelNode SERVER_PROP_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_PROP_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_PROP_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS = new ModelNode();

    static {
        ROOT_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        ROOT_PROP_ADDRESS.protect();
        SERVER_GROUP_PROP_ADDRESS.add(SERVER_GROUP, "other-server-group");
        SERVER_GROUP_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        SERVER_GROUP_PROP_ADDRESS.protect();
        HOST_PROP_ADDRESS.add(HOST, "slave");
        HOST_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        HOST_PROP_ADDRESS.protect();
        HOST_CLASSLOADING_ADDRESS.add(HOST, "slave");
        HOST_CLASSLOADING_ADDRESS.add(CORE_SERVICE, PLATFORM_MBEAN);
        HOST_CLASSLOADING_ADDRESS.add(TYPE, "class-loading");
        HOST_CLASSLOADING_ADDRESS.protect();
        SERVER_PROP_ADDRESS.add(HOST, "slave");
        SERVER_PROP_ADDRESS.add(SERVER_CONFIG, "other-two");
        SERVER_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        SERVER_PROP_ADDRESS.protect();
        MAIN_RUNNING_SERVER_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_ADDRESS.protect();
        MAIN_RUNNING_SERVER_PROP_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_PROP_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        MAIN_RUNNING_SERVER_PROP_ADDRESS.protect();
        MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(HOST, "master");
        MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(SERVER, "main-one");
        MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(CORE_SERVICE, PLATFORM_MBEAN);
        MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(TYPE, "class-loading");
        MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS.protect();
        OTHER_RUNNING_SERVER_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_ADDRESS.protect();
        OTHER_RUNNING_SERVER_PROP_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_PROP_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        OTHER_RUNNING_SERVER_PROP_ADDRESS.protect();
        OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(CORE_SERVICE, PLATFORM_MBEAN);
        OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS.add(TYPE, "class-loading");
        OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS.protect();

    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(CoreResourceManagementTestCase.class.getSimpleName());
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testSystemPropertyManagement() throws IOException {
        DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();
        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();

        ModelNode response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        ModelNode returnVal = validateResponse(response);
        int origPropCount = returnVal.asInt();

        ModelNode request = getSystemPropertyAddOperation(ROOT_PROP_ADDRESS, "domain", Boolean.FALSE);
        response = masterClient.execute(request);
        validateResponse(response);

        // TODO validate response structure

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount + 1, returnVal.asList().size());

        response = masterClient.execute(getReadChildrenNamesOperation(OTHER_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount + 1, returnVal.asList().size());

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain", returnVal.asString());

        // Override at server-group
        request = getSystemPropertyAddOperation(SERVER_GROUP_PROP_ADDRESS, "group", Boolean.FALSE);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("group", returnVal.asString());

        // Change the domain level value, confirm it does not replace override
        request = getWriteAttributeOperation(ROOT_PROP_ADDRESS, "domain2");
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain2", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("group", returnVal.asString());

        // Override at the host level
        request = getSystemPropertyAddOperation(HOST_PROP_ADDRESS, "host", Boolean.FALSE);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain2", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("host", returnVal.asString());

        // Change the domain level value, confirm it does not replace override
        request = getWriteAttributeOperation(ROOT_PROP_ADDRESS, "domain3");
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain3", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("host", returnVal.asString());

        // Change the server group level value, confirm it does not replace override
        request = getWriteAttributeOperation(SERVER_GROUP_PROP_ADDRESS, "group2");
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain3", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("host", returnVal.asString());

        // Override at the server level
        request = getSystemPropertyAddOperation(SERVER_PROP_ADDRESS, "server", Boolean.FALSE);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain3", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server", returnVal.asString());

        // Change the server group level value, confirm it does not replace override
        request = getWriteAttributeOperation(HOST_PROP_ADDRESS, "host2");
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain3", returnVal.asString());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server", returnVal.asString());

        // Modify the server level value
        request = getWriteAttributeOperation(SERVER_PROP_ADDRESS, "server1");
        response = slaveClient.execute(request);   // Start using the slave client
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("domain3", returnVal.asString());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        // Remove from top down
        request = getEmptyOperation(REMOVE, ROOT_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount, returnVal.asList().size());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        request = getEmptyOperation(REMOVE, SERVER_GROUP_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount, returnVal.asList().size());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        request = getEmptyOperation(REMOVE, HOST_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount, returnVal.asList().size());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        request = getEmptyOperation(REMOVE, SERVER_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount, returnVal.asList().size());

        response = slaveClient.execute(getReadChildrenNamesOperation(OTHER_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(origPropCount, returnVal.asList().size());
    }

    @Test
    public void testPlatformMBeanManagement() throws Exception {

        // Just validate that the resources exist at the expected location
        DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode response = masterClient.execute(getReadAttributeOperation(HOST_CLASSLOADING_ADDRESS, "loaded-class-count"));
        ModelNode returnVal = validateResponse(response);
        Assert.assertEquals(ModelType.INT, returnVal.getType());

        response = masterClient.execute(getReadAttributeOperation(MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS, "loaded-class-count"));
        returnVal = validateResponse(response);
        Assert.assertEquals(ModelType.INT, returnVal.getType());

        response = masterClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS, "loaded-class-count"));
        returnVal = validateResponse(response);
        Assert.assertEquals(ModelType.INT, returnVal.getType());

    }

    @Test
    public void testUndefineSocketBindingPortOffset() throws IOException {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        final ModelNode address = new ModelNode();
        address.add("server-group", "other-server-group");
        address.protect();
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get(NAME).set("socket-binding-port-offset");
            operation.get(INCLUDE_DEFAULTS).set(false);

            final ModelNode response = masterClient.execute(operation);
            validateResponse(response);
            Assert.assertFalse(response.get(RESULT).isDefined());
        }
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get(NAME).set("socket-binding-port-offset");
            operation.get(VALUE).set(0);

            final ModelNode response = masterClient.execute(operation);
            validateResponse(response);
        }
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get(NAME).set("socket-binding-port-offset");
            operation.get(INCLUDE_DEFAULTS).set(false);

            final ModelNode response = masterClient.execute(operation);
            validateResponse(response);
            Assert.assertTrue(response.get(RESULT).isDefined());
            Assert.assertEquals(0, response.get(RESULT).asInt());
        }
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(UNDEFINE_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get(NAME).set("socket-binding-port-offset");

            final ModelNode response = masterClient.execute(operation);
            validateResponse(response);
        }
        {
            final ModelNode operation = new ModelNode();
            operation.get(OP).set(READ_ATTRIBUTE_OPERATION);
            operation.get(OP_ADDR).set(address);
            operation.get(NAME).set("socket-binding-port-offset");
            operation.get(INCLUDE_DEFAULTS).set(false);

            final ModelNode response = masterClient.execute(operation);
            validateResponse(response);
            Assert.assertFalse(response.get(RESULT).isDefined());
        }
    }

    @Test
    public void testDomainSnapshot() throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode snapshotOperation = new ModelNode();
        snapshotOperation.get(OP).set(SnapshotTakeHandler.OPERATION_NAME);
        snapshotOperation.get(OP_ADDR).setEmptyList();
        final String snapshot = validateResponse(masterClient.execute(snapshotOperation)).asString();
        Assert.assertNotNull(snapshot);
        Assert.assertFalse(snapshot.isEmpty());

        ModelNode listSnapshotOperation = new ModelNode();
        listSnapshotOperation.get(OP).set(SnapshotListHandler.OPERATION_NAME);
        listSnapshotOperation.get(OP_ADDR).setEmptyList();
        ModelNode listResult = validateResponse(masterClient.execute(listSnapshotOperation));
        Set<String> snapshots = new HashSet<String>();
        for (ModelNode curr : listResult.get(NAMES).asList()) {
            snapshots.add(listResult.get(DIRECTORY).asString() + "/" + curr.asString());
        }

        Assert.assertTrue(snapshots.contains(snapshot));

        ModelNode deleteSnapshotOperation = new ModelNode();
        deleteSnapshotOperation.get(OP).set(SnapshotDeleteHandler.OPERATION_NAME);
        deleteSnapshotOperation.get(OP_ADDR).setEmptyList();
        deleteSnapshotOperation.get(NAME).set(snapshot.substring(snapshot.lastIndexOf("/")  + 1));
        validateResponse(masterClient.execute(deleteSnapshotOperation), false);

        listResult = validateResponse(masterClient.execute(listSnapshotOperation));
        snapshots = new HashSet<String>();
        for (ModelNode curr : listResult.get(NAMES).asList()) {
            snapshots.add(listResult.get(DIRECTORY).asString() + "/" + curr.asString());
        }

        Assert.assertFalse(snapshots.contains(snapshot));
    }

    @Test
    public void testMasterSnapshot() throws Exception {
        testSnapshot(new ModelNode().add(HOST, "master"));
    }

    @Test
    public void testSlaveSnapshot() throws Exception {
        testSnapshot(new ModelNode().add(HOST, "slave"));
    }

    @Test
    public void testCannotInvokeManagedServerOperations() throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode serverAddTf = getAddThreadFactoryOperation(
                new ModelNode().add("host", "master").add("server", "main-one").add("subsystem", "threads").add("thread-factory", "test-pool-123abc"));

        ModelNode desc = validateFailedResponse(masterClient.execute(serverAddTf));
        String errorCode = getNotAuthorizedErrorCode();
        Assert.assertTrue(desc.toString() + " does not contain " + errorCode, desc.toString().contains(errorCode));

        ModelNode slaveThreeAddress = new ModelNode().add("host", "slave").add("server", "main-three").add("subsystem", "threads").add("thread-factory", "test-pool-123abc");
        serverAddTf = getAddThreadFactoryOperation(slaveThreeAddress);

        desc = validateFailedResponse(masterClient.execute(serverAddTf));
        Assert.assertTrue(desc.toString() + " does not contain " + errorCode, desc.toString().contains(errorCode));
    }

    @Test
    public void testCannotInvokeManagedMasterServerOperationsInDomainComposite() throws Exception {
        testCannotInvokeManagedServerOperationsComposite(new ModelNode().setEmptyList(), new ModelNode().add("host", "master").add("server", "main-one").add("subsystem", "threads"));
    }

    @Test
    public void testCannotInvokeManagedSlaveServerOperationsInDomainComposite() throws Exception {
        testCannotInvokeManagedServerOperationsComposite(new ModelNode().setEmptyList(), new ModelNode().add("host", "slave").add("server", "main-three").add("subsystem", "threads"));
    }

    @Test
    public void testCannotInvokeManagedMasterServerOperationsInServerComposite() throws Exception {
        testCannotInvokeManagedServerOperationsComposite(new ModelNode().add("host", "master").add("server", "main-one"), new ModelNode().add("subsystem", "threads"));
    }

    @Test
    public void testCannotInvokeManagedSlaveServerOperationsInServerComposite() throws Exception {
        testCannotInvokeManagedServerOperationsComposite(new ModelNode().add("host", "slave").add("server", "main-three"), new ModelNode().add("subsystem", "threads"));
    }

    @Test
    public void testReadSystemPropertyResourceOnServerFromComposite() throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode composite = new ModelNode();
        composite.get(OP).set(CompositeOperationHandler.NAME);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        ModelNode server1 = new ModelNode();
        server1.get(OP).set(READ_RESOURCE_OPERATION);
        server1.get(OP_ADDR).add("host", "master").add("server", "main-one");
        ModelNode server3 = new ModelNode();
        server3.get(OP).set(READ_RESOURCE_OPERATION);
        server3.get(OP_ADDR).add("host", "slave").add("server", "main-three");
        composite.get(STEPS).add(server1);
        composite.get(STEPS).add(server3);

        ModelNode result = masterClient.execute(composite);
        validateResponse(result);
    }

    @Test
    public void testSetSystemPropertyOnServerFromComposite() throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode composite = new ModelNode();
        composite.get(OP).set(CompositeOperationHandler.NAME);
        composite.get(OP_ADDR).setEmptyList();
        composite.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        ModelNode server1 = new ModelNode();
        server1.get(OP).set(ADD);
        server1.get(OP_ADDR).add("host", "master").add("server", "main-one").add("system-property", "domain-test-property");
        ModelNode server3 = new ModelNode();
        server3.get(OP).set(ADD);
        server3.get(OP_ADDR).add("host", "slave").add("server", "main-three").add("system-property", "domain-test-property");
        composite.get(STEPS).add(server1);
        composite.get(STEPS).add(server3);

        ModelNode result = masterClient.execute(composite);

        String errorCode = getNotAuthorizedErrorCode();

        validateFailedResponse(result);

        List<Property> steps = result.get(RESULT, SERVER_GROUPS, "main-server-group").asPropertyList();
        Assert.assertEquals(2, steps.size());
        int i = 0;
        for (Property property : steps) {
            ModelNode stepResult = property.getValue().get("response");
            ModelNode desc = validateFailedResponse(stepResult);
            Assert.assertTrue(desc.toString() + " does not contain " + errorCode, desc.toString().contains(errorCode));
            i++;
        }
    }

    /**
     * Test for AS7-3600
     */
    @Test
    public void testAddRemoveHostInterface() throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();
        final String ifaceName = "testing-interface";

        ModelNode add = new ModelNode();
        add.get(OP).set(ADD);
        add.get(OP_ADDR).add(HOST, "master").add(INTERFACE, ifaceName);
        add.get(ANY_ADDRESS).set(true);

        validateResponse(masterClient.execute(add));

        ModelNode read = new ModelNode();
        read.get(OP).set(READ_RESOURCE_OPERATION);
        read.get(OP_ADDR).add(HOST, "master").add(SERVER, "main-one").add(INTERFACE, ifaceName);
        validateResponse(masterClient.execute(read));

        ModelNode remove = new ModelNode();
        remove.get(OP).set(REMOVE);
        remove.get(OP_ADDR).add(HOST, "master").add(INTERFACE, ifaceName);
        validateResponse(masterClient.execute(remove));

        validateFailedResponse(masterClient.execute(read));
    }

    /**
     * Test for AS7-3643
     */
    @Test
    public void testCanFindServerRestartRequiredAfterChangingSocketBindingPortOffset() throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode read = new ModelNode();
        read.get(OP).set(READ_ATTRIBUTE_OPERATION);
        read.get(OP_ADDR).add(HOST, "master").add(SERVER_CONFIG, "main-one");
        read.get(NAME).set("socket-binding-port-offset");
        ModelNode result = validateResponse(masterClient.execute(read));
        int original = result.isDefined() ? result.asInt() : 0;

        //The bug causing AS7-3643 caused execution of this op to fail
        ModelNode write = new ModelNode();
        write.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        write.get(OP_ADDR).add(HOST, "master").add(SERVER_CONFIG, "main-one");
        write.get(NAME).set("socket-binding-port-offset");
        write.get(VALUE).set(original + 1);
        result = validateResponse(masterClient.execute(write));

        //TODO investigate the extra quotes around main-server-group
        final String mainServerGroup = "\"main-server-group\"";
        Assert.assertEquals(SUCCESS, result.get(SERVER_GROUPS, mainServerGroup, "main-one", RESPONSE, OUTCOME).asString());
        ModelNode headers = result.get(SERVER_GROUPS, "\"main-server-group\"", "main-one", RESPONSE, RESPONSE_HEADERS);
        Assert.assertEquals(RESTART_REQUIRED, headers.get(PROCESS_STATE).asString());
        Assert.assertTrue(RESTART_REQUIRED, headers.get(OPERATION_REQUIRES_RESTART).asBoolean());


        //Now just set back to the original
        write.get(VALUE).set(original);
        validateResponse(masterClient.execute(write));
    }

    private void testCannotInvokeManagedServerOperationsComposite(ModelNode compositeAddress, ModelNode stepAddress) throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode composite = new ModelNode();
        composite.get(OP).set(CompositeOperationHandler.NAME);
        composite.get(OP_ADDR).set(compositeAddress);
        composite.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

        ModelNode goodServerOp = new ModelNode();
        goodServerOp.get(OP).set(READ_RESOURCE_OPERATION);
        goodServerOp.get(OP_ADDR).set(stepAddress);
        composite.get(STEPS).add(goodServerOp);
        composite.get(STEPS).add(getAddThreadFactoryOperation(stepAddress.clone().add("thread-factory", "test-pool-123abc")));

        ModelNode result = masterClient.execute(composite);


        String errorCode = getNotAuthorizedErrorCode();

        ModelNode desc = validateFailedResponse(result);
        Assert.assertTrue(desc.toString() + " does not contain " + errorCode, desc.toString().contains(errorCode));

        List<Property> steps = result.get(RESULT).asPropertyList();
        Assert.assertEquals(2, steps.size());
        int i = 0;
        for (Property property : steps) {
            ModelNode stepResult = property.getValue();
            Assert.assertEquals(FAILED, stepResult.get(OUTCOME).asString());
            if (i == 0) {
                Assert.assertFalse(stepResult.hasDefined(FAILURE_DESCRIPTION));
            }
            if (i++ == 1) {
                desc = validateFailedResponse(stepResult);
                Assert.assertTrue(desc.toString() + " does not contain " + errorCode, desc.toString().contains(errorCode));
            }
            i++;
        }
    }

    private ModelNode getAddThreadFactoryOperation(ModelNode address) {


        ModelNode serverTf = new ModelNode();
        serverTf.get(OP).set("add");
        serverTf.get(OP_ADDR).set(address);
        serverTf.get("group-name").set("AAA");
        serverTf.get("name").set("BBB");
        serverTf.get("priority").set(6);
        serverTf.get("thread-name-pattern").set("Test-ThreadA");

        return serverTf;
    }

    private String getNotAuthorizedErrorCode() {
        try {
            throw ControllerMessages.MESSAGES.modelUpdateNotAuthorized("", PathAddress.EMPTY_ADDRESS);
        }catch(Exception e) {
            String msg = e.getMessage();
            return msg.substring(0, msg.indexOf(":"));
        }
    }

    private void testSnapshot(ModelNode addr) throws Exception {
        final DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();

        ModelNode snapshotOperation = new ModelNode();
        snapshotOperation.get(OP).set(SnapshotTakeHandler.OPERATION_NAME);
        snapshotOperation.get(OP_ADDR).set(addr);
        final String snapshot = validateResponse(masterClient.execute(snapshotOperation)).get(DOMAIN_RESULTS).asString();
        Assert.assertNotNull(snapshot);
        Assert.assertFalse(snapshot.isEmpty());

        ModelNode listSnapshotOperation = new ModelNode();
        listSnapshotOperation.get(OP).set(SnapshotListHandler.OPERATION_NAME);
        listSnapshotOperation.get(OP_ADDR).set(addr);
        ModelNode listResult = validateResponse(masterClient.execute(listSnapshotOperation)).get(DOMAIN_RESULTS);
        Set<String> snapshots = new HashSet<String>();
        for (ModelNode curr : listResult.get(NAMES).asList()) {
            snapshots.add(listResult.get(DIRECTORY).asString() + "/" + curr.asString());
        }

        Assert.assertTrue(snapshots.contains(snapshot));

        ModelNode deleteSnapshotOperation = new ModelNode();
        deleteSnapshotOperation.get(OP).set(SnapshotDeleteHandler.OPERATION_NAME);
        deleteSnapshotOperation.get(OP_ADDR).set(addr);
        deleteSnapshotOperation.get(NAME).set(snapshot.substring(snapshot.lastIndexOf("/")  + 1));
        validateResponse(masterClient.execute(deleteSnapshotOperation));

        listResult = validateResponse(masterClient.execute(listSnapshotOperation)).get(DOMAIN_RESULTS);
        snapshots = new HashSet<String>();
        for (ModelNode curr : listResult.get(NAMES).asList()) {
            snapshots.add(listResult.get(DIRECTORY).asString() + "/" + curr.asString());
        }

        Assert.assertFalse(snapshots.contains(snapshot));
    }


    private static ModelNode getSystemPropertyAddOperation(ModelNode address, String value, Boolean boottime) {
        ModelNode result = getEmptyOperation(ADD, address);
        if (value != null) {
            result.get(VALUE).set(value);
        }
        if (boottime != null) {
            result.get(BOOT_TIME).set(boottime.booleanValue());
        }
        return result;
    }

    private static ModelNode getReadAttributeOperation(ModelNode address, String attribute) {
        ModelNode result = getEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
        result.get(NAME).set(attribute);
        return result;
    }

    private static ModelNode getWriteAttributeOperation(ModelNode address, String value) {
        ModelNode result = getEmptyOperation(WRITE_ATTRIBUTE_OPERATION, address);
        result.get(NAME).set(VALUE);
        result.get(VALUE).set(value);
        return result;
    }

    private static ModelNode getReadChildrenNamesOperation(ModelNode address, String type) {
        ModelNode result = getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, address);
        result.get(CHILD_TYPE).set(type);
        return result;
    }

    private static ModelNode getEmptyOperation(String operationName, ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        if (address != null) {
            op.get(OP_ADDR).set(address);
        }
        else {
            // Just establish the standard structure; caller can fill in address later
            op.get(OP_ADDR);
        }
        return op;
    }
}
