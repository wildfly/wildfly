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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INET_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SCHEMA_LOCATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateFailedResponse;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;

import java.io.IOException;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.SchemaLocationAddHandler;
import org.jboss.as.controller.operations.common.SchemaLocationRemoveHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of various management operations to confirm they are or are not allowed access.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementAccessTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static final String TEST = "test";
    private static final ModelNode ROOT_ADDRESS = new ModelNode().setEmptyList();
    private static final ModelNode MASTER_ROOT_ADDRESS = new ModelNode().add(HOST, "master");
    private static final ModelNode SLAVE_ROOT_ADDRESS = new ModelNode().add(HOST, "slave");
    private static final ModelNode ROOT_PROP_ADDRESS = new ModelNode().add(SYSTEM_PROPERTY, TEST);
    private static final ModelNode OTHER_SERVER_GROUP_ADDRESS = new ModelNode().add(SERVER_GROUP, "other-server-group");
    private static final ModelNode TEST_SERVER_GROUP_ADDRESS = new ModelNode().add(SERVER_GROUP, "test-server-group");
    private static final ModelNode MASTER_INTERFACE_ADDRESS = new ModelNode().set(MASTER_ROOT_ADDRESS).add(INTERFACE, "public");
    private static final ModelNode SLAVE_INTERFACE_ADDRESS = new ModelNode().set(SLAVE_ROOT_ADDRESS).add(INTERFACE, "public");
    private static final ModelNode MAIN_RUNNING_SERVER_ADDRESS = new ModelNode().add(HOST, "master").add(SERVER, "main-one");
    private static final ModelNode MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS = new ModelNode().set(MAIN_RUNNING_SERVER_ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN).add(TYPE, "class-loading");
    private static final ModelNode OTHER_RUNNING_SERVER_ADDRESS = new ModelNode().add(HOST, "slave").add(SERVER, "other-two");
    private static final ModelNode OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS = new ModelNode().set(OTHER_RUNNING_SERVER_ADDRESS).add(CORE_SERVICE, PLATFORM_MBEAN).add(TYPE, "class-loading");

    static {
        ROOT_ADDRESS.protect();
        MASTER_ROOT_ADDRESS.protect();
        SLAVE_ROOT_ADDRESS.protect();
        ROOT_PROP_ADDRESS.protect();
        OTHER_SERVER_GROUP_ADDRESS.protect();
        MASTER_INTERFACE_ADDRESS.protect();
        SLAVE_INTERFACE_ADDRESS.protect();
        MAIN_RUNNING_SERVER_ADDRESS.protect();
        MAIN_RUNNING_SERVER_CLASSLOADING_ADDRESS.protect();
        OTHER_RUNNING_SERVER_ADDRESS.protect();
        OTHER_RUNNING_SERVER_CLASSLOADING_ADDRESS.protect();
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ManagementAccessTestCase.class.getSimpleName());
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

    private DomainClient masterClient;
    private DomainClient slaveClient;

    @Before
    public void setup() throws Exception {
        masterClient = domainMasterLifecycleUtil.getDomainClient();
        slaveClient = domainSlaveLifecycleUtil.getDomainClient();
    }

    @After
    public void teardown() throws Exception {
        masterClient.execute(Util.getEmptyOperation(REMOVE, TEST_SERVER_GROUP_ADDRESS));
        masterClient.execute(SchemaLocationRemoveHandler.getRemoveSchemaLocationOperation(ROOT_ADDRESS, "uri"));
        masterClient.execute(SchemaLocationRemoveHandler.getRemoveSchemaLocationOperation(MASTER_ROOT_ADDRESS, "uri"));
        masterClient.execute(SchemaLocationRemoveHandler.getRemoveSchemaLocationOperation(SLAVE_ROOT_ADDRESS, "uri"));
    }

    @Test
    public void testDomainReadAccess() throws IOException {

        // Start with reads of the root resource
        ModelNode response = masterClient.execute(getReadAttributeOperation(ROOT_ADDRESS, SCHEMA_LOCATIONS));
        ModelNode returnVal = validateResponse(response);

        response = slaveClient.execute(getReadAttributeOperation(ROOT_ADDRESS, SCHEMA_LOCATIONS));
        ModelNode slaveReturnVal = validateResponse(response);

        Assert.assertEquals(returnVal, slaveReturnVal);

        // Now try a resource below root
        response = masterClient.execute(getReadAttributeOperation(OTHER_SERVER_GROUP_ADDRESS, PROFILE));
        returnVal = validateResponse(response);
        Assert.assertEquals("osgi", returnVal.asString());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_SERVER_GROUP_ADDRESS, PROFILE));
        slaveReturnVal = validateResponse(response);

        Assert.assertEquals(returnVal, slaveReturnVal);
    }

    @Test
    public void testCompositeDomainReadAccess() throws IOException {

        ModelNode request = getEmptyOperation(COMPOSITE, null);
        ModelNode steps = request.get(STEPS);
        steps.add(getReadAttributeOperation(ROOT_ADDRESS, SCHEMA_LOCATIONS));
        steps.add(getReadAttributeOperation(OTHER_SERVER_GROUP_ADDRESS, PROFILE));
        request.protect();

        ModelNode response = masterClient.execute(request);
        System.out.println(response);
        ModelNode returnVal = validateResponse(response);
        validateResponse(returnVal.get("step-1"));
        ModelNode profile = validateResponse(returnVal.get("step-2"));
        Assert.assertEquals("osgi", profile.asString());

        response = slaveClient.execute(request);
        System.out.println(response);
        ModelNode slaveReturnVal = validateResponse(response);
        Assert.assertEquals(returnVal, slaveReturnVal);
    }

    @Test
    public void testHostReadAccess() throws IOException {

        // Start with reads of the root resource
        ModelNode response = masterClient.execute(getReadAttributeOperation(MASTER_ROOT_ADDRESS, NAME));
        ModelNode returnVal = validateResponse(response);
        Assert.assertEquals("master", returnVal.asString());

        response = slaveClient.execute(getReadAttributeOperation(SLAVE_ROOT_ADDRESS, NAME));
        ModelNode slaveReturnVal = validateResponse(response);
        Assert.assertEquals("slave", slaveReturnVal.asString());

        // Now try a resource below root
        response = masterClient.execute(getReadAttributeOperation(MASTER_INTERFACE_ADDRESS, INET_ADDRESS));
        returnVal = validateResponse(response);
        Assert.assertEquals(ModelType.EXPRESSION, returnVal.getType());

        response = slaveClient.execute(getReadAttributeOperation(SLAVE_INTERFACE_ADDRESS, INET_ADDRESS));
        slaveReturnVal = validateResponse(response);
        Assert.assertEquals(ModelType.EXPRESSION, slaveReturnVal.getType());

        response = masterClient.execute(getReadAttributeOperation(SLAVE_INTERFACE_ADDRESS, INET_ADDRESS));
        returnVal = validateResponse(response);
        Assert.assertEquals(ModelType.EXPRESSION, returnVal.getType());

        Assert.assertEquals(returnVal, slaveReturnVal);

        // Can't access the master via the slave
        response = slaveClient.execute(getReadAttributeOperation(MASTER_ROOT_ADDRESS, NAME));
        validateFailedResponse(response);
        response = slaveClient.execute(getReadAttributeOperation(MASTER_INTERFACE_ADDRESS, INET_ADDRESS));
        validateFailedResponse(response);
    }

    @Test
    public void testCompositeHostReadAccess() throws IOException {

        ModelNode masterRequest = getEmptyOperation(COMPOSITE, null);
        ModelNode steps = masterRequest.get(STEPS);
        steps.add(getReadAttributeOperation(MASTER_ROOT_ADDRESS, NAME));
        steps.add(getReadAttributeOperation(MASTER_INTERFACE_ADDRESS, INET_ADDRESS));
        masterRequest.protect();

        ModelNode response = masterClient.execute(masterRequest);
        System.out.println(response);
        ModelNode returnVal = validateResponse(response);
        ModelNode name = validateResponse(returnVal.get("step-1"));
        Assert.assertEquals("master", name.asString());
        ModelNode inetAddress = validateResponse(returnVal.get("step-2"));
        Assert.assertEquals(ModelType.EXPRESSION, inetAddress.getType());

        ModelNode slaveRequest = getEmptyOperation(COMPOSITE, null);
        steps = slaveRequest.get(STEPS);
        steps.add(getReadAttributeOperation(SLAVE_ROOT_ADDRESS, NAME));
        steps.add(getReadAttributeOperation(SLAVE_INTERFACE_ADDRESS, INET_ADDRESS));
        masterRequest.protect();

        response = slaveClient.execute(slaveRequest);
        System.out.println(response);
        ModelNode slaveReturnVal = validateResponse(response);
        name = validateResponse(slaveReturnVal.get("step-1"));
        Assert.assertEquals("slave", name.asString());
        inetAddress = validateResponse(slaveReturnVal.get("step-2"));
        Assert.assertEquals(ModelType.EXPRESSION, inetAddress.getType());

        // Check we get the same thing via the master
        response = masterClient.execute(slaveRequest);
        returnVal = validateResponse(response);
        Assert.assertEquals(returnVal, slaveReturnVal);

        // Can't access the master via the slave
        response = slaveClient.execute(masterRequest);
        validateFailedResponse(response);
    }

    @Test
    public void testCompositeCrossHostReadAccess() throws IOException {

        ModelNode masterRequest = getEmptyOperation(COMPOSITE, null);
        ModelNode steps = masterRequest.get(STEPS);
        steps.add(getReadAttributeOperation(MASTER_ROOT_ADDRESS, NAME));
        steps.add(getReadAttributeOperation(MASTER_INTERFACE_ADDRESS, INET_ADDRESS));
        steps.add(getReadAttributeOperation(SLAVE_ROOT_ADDRESS, NAME));
        steps.add(getReadAttributeOperation(SLAVE_INTERFACE_ADDRESS, INET_ADDRESS));
        steps.add(getReadAttributeOperation(MAIN_RUNNING_SERVER_ADDRESS, NAME));
        steps.add(getReadAttributeOperation(OTHER_RUNNING_SERVER_ADDRESS, NAME));
        masterRequest.protect();

        System.out.println(masterRequest);
        ModelNode response = masterClient.execute(masterRequest);
        System.out.println(response);
        ModelNode returnVal = validateResponse(response);
        ModelNode name = validateResponse(returnVal.get("step-1"));
        Assert.assertEquals("master", name.asString());
        ModelNode inetAddress = validateResponse(returnVal.get("step-2"));
        Assert.assertEquals(ModelType.EXPRESSION, inetAddress.getType());
        name = validateResponse(returnVal.get("step-3"));
        Assert.assertEquals("slave", name.asString());
        inetAddress = validateResponse(returnVal.get("step-4"));
        Assert.assertEquals(ModelType.EXPRESSION, inetAddress.getType());
        name = validateResponse(returnVal.get("step-5"));
        Assert.assertEquals("main-one", name.asString());
        name = validateResponse(returnVal.get("step-6"));
        Assert.assertEquals("other-two", name.asString());

        // Can't access the master via the slave
        response = slaveClient.execute(masterRequest);
        validateFailedResponse(response);
    }

    @Test
    public void testDomainWriteAccess() throws IOException {

        // Start with writes of the root resource
        final ModelNode addSchemaLocRequest = SchemaLocationAddHandler.getAddSchemaLocationOperation(ROOT_ADDRESS, "uri", "location");
        ModelNode response = masterClient.execute(addSchemaLocRequest);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(ROOT_ADDRESS, SCHEMA_LOCATIONS));
        ModelNode returnVal = validateResponse(response);
        Assert.assertTrue(hasTestSchemaLocation(returnVal));

        // Now try a resource below root
        final ModelNode addServerGroupRequest = Util.getEmptyOperation(ADD, TEST_SERVER_GROUP_ADDRESS);
        addServerGroupRequest.get(PROFILE).set("default");
        addServerGroupRequest.get(SOCKET_BINDING_GROUP).set("standard-sockets");

        response = masterClient.execute(addServerGroupRequest);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(TEST_SERVER_GROUP_ADDRESS, PROFILE));
        returnVal = validateResponse(response);
        Assert.assertEquals("default", returnVal.asString());

        // Slave can't write
        response = slaveClient.execute(addSchemaLocRequest);
        validateFailedResponse(response);

        response = slaveClient.execute(addServerGroupRequest);
        validateFailedResponse(response);
    }

    @Test
    public void testCompositeDomainWriteAccess() throws IOException {

        ModelNode masterRequest = getEmptyOperation(COMPOSITE, null);
        ModelNode steps = masterRequest.get(STEPS);
        steps.add(SchemaLocationAddHandler.getAddSchemaLocationOperation(ROOT_ADDRESS, "uri", "location"));

        // Now try a resource below root
        final ModelNode addServerGroupRequest = Util.getEmptyOperation(ADD, TEST_SERVER_GROUP_ADDRESS);
        addServerGroupRequest.get(PROFILE).set("default");
        addServerGroupRequest.get(SOCKET_BINDING_GROUP).set("standard-sockets");

        steps.add(addServerGroupRequest);
        masterRequest.protect();

        ModelNode response = masterClient.execute(masterRequest);
        System.out.println(response);
        validateResponse(response);

        response = masterClient.execute(getReadAttributeOperation(ROOT_ADDRESS, SCHEMA_LOCATIONS));
        ModelNode returnVal = validateResponse(response);
        Assert.assertTrue(hasTestSchemaLocation(returnVal));

        response = masterClient.execute(getReadAttributeOperation(TEST_SERVER_GROUP_ADDRESS, PROFILE));
        returnVal = validateResponse(response);
        Assert.assertEquals("default", returnVal.asString());

        // Slave can't write
        response = slaveClient.execute(masterRequest);
        validateFailedResponse(response);
    }

    private boolean hasTestSchemaLocation(ModelNode returnVal) {

        Assert.assertEquals(ModelType.LIST, returnVal.getType());
        for (Property prop : returnVal.asPropertyList()) {
            if ("uri".equals(prop.getName()) && "location".equals(prop.getValue().asString())) {
                return true;
            }
        }

        return false;
    }

    private static ModelNode getReadAttributeOperation(ModelNode address, String attribute) {
        ModelNode result = getEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
        result.get(NAME).set(attribute);
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
