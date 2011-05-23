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

package org.jboss.as.test.integration.domain;

import org.jboss.as.arquillian.container.domain.managed.DomainLifecycleUtil;
import org.jboss.as.arquillian.container.domain.managed.JBossAsManagedConfiguration;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;

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
    private static final ModelNode SERVER_PROP_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode MAIN_RUNNING_SERVER_PROP_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_ADDRESS = new ModelNode();
    private static final ModelNode OTHER_RUNNING_SERVER_PROP_ADDRESS = new ModelNode();

    static {
        ROOT_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        ROOT_PROP_ADDRESS.protect();
        SERVER_GROUP_PROP_ADDRESS.add(SERVER_GROUP, "other-server-group");
        SERVER_GROUP_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        SERVER_GROUP_PROP_ADDRESS.protect();
        HOST_PROP_ADDRESS.add(HOST, "slave");
        HOST_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        HOST_PROP_ADDRESS.protect();
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
        OTHER_RUNNING_SERVER_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_ADDRESS.protect();
        OTHER_RUNNING_SERVER_PROP_ADDRESS.add(HOST, "slave");
        OTHER_RUNNING_SERVER_PROP_ADDRESS.add(SERVER, "other-two");
        OTHER_RUNNING_SERVER_PROP_ADDRESS.add(SYSTEM_PROPERTY, TEST);
        OTHER_RUNNING_SERVER_PROP_ADDRESS.protect();

    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = new DomainTestSupport(CoreResourceManagementTestCase.class.getSimpleName(), "domain-configs/domain-standard.xml", "host-configs/host-master.xml", "host-configs/host-slave.xml");
        testSupport.start();
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport.stop();
    }

    @Test
    public void testSystemPropertyManagement() throws IOException {
        DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();
        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();

        ModelNode request = getSystemPropertyAddOperation(ROOT_PROP_ADDRESS, "domain", Boolean.FALSE);
        ModelNode response = masterClient.execute(request);
        validateResponse(response);

        // TODO validate response structure

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        ModelNode returnVal = validateResponse(response);
        Assert.assertEquals(2, returnVal.asList().size());

        response = masterClient.execute(getReadChildrenNamesOperation(OTHER_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(2, returnVal.asList().size());

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
        request = Util.getResourceRemoveOperation(ROOT_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(1, returnVal.asList().size());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        request = Util.getResourceRemoveOperation(SERVER_GROUP_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(1, returnVal.asList().size());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        request = Util.getResourceRemoveOperation(HOST_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(1, returnVal.asList().size());

        response = slaveClient.execute(getReadAttributeOperation(OTHER_RUNNING_SERVER_PROP_ADDRESS, VALUE));
        returnVal = validateResponse(response);
        Assert.assertEquals("server1", returnVal.asString());

        request = Util.getResourceRemoveOperation(SERVER_PROP_ADDRESS);
        response = masterClient.execute(request);
        validateResponse(response);

        response = masterClient.execute(getReadChildrenNamesOperation(MAIN_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(1, returnVal.asList().size());

        response = slaveClient.execute(getReadChildrenNamesOperation(OTHER_RUNNING_SERVER_ADDRESS, SYSTEM_PROPERTY));
        returnVal = validateResponse(response);
        Assert.assertEquals(1, returnVal.asList().size());
    }

    private static ModelNode getSystemPropertyAddOperation(ModelNode address, String value, Boolean boottime) {
        ModelNode result = Util.getEmptyOperation(ADD, address);
        if (value != null) {
            result.get(VALUE).set(value);
        }
        if (boottime != null) {
            result.get(BOOT_TIME).set(boottime.booleanValue());
        }
        return result;
    }

    private static ModelNode getReadAttributeOperation(ModelNode address, String attribute) {
        ModelNode result = Util.getEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
        result.get(NAME).set(attribute);
        return result;
    }

    private static ModelNode getWriteAttributeOperation(ModelNode address, String value) {
        ModelNode result = Util.getEmptyOperation(WRITE_ATTRIBUTE_OPERATION, address);
        result.get(NAME).set(VALUE);
        result.get(VALUE).set(value);
        return result;
    }

    private static ModelNode getReadChildrenNamesOperation(ModelNode address, String type) {
        ModelNode result = Util.getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, address);
        result.get(CHILD_TYPE).set(type);
        return result;
    }
}
