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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BOOT_TIME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_CONFIG;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

import java.io.IOException;

import org.jboss.as.arquillian.container.domain.managed.DomainLifecycleUtil;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.version.Version;
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
public class ManagementVersionTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ManagementVersionTestCase.class.getSimpleName());
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
    public void testDomainRootResource() throws Exception  {
        rootResourceTest(null, domainMasterLifecycleUtil.getDomainClient());
    }

    @Test
    public void testMasterHostRootResource() throws Exception  {
        rootResourceTest("host=master", domainMasterLifecycleUtil.getDomainClient());
    }

    @Test
    public void testSlaveHostRootResource() throws Exception  {
        rootResourceTest("host=slave", domainSlaveLifecycleUtil.getDomainClient());
    }

    @Test
    public void testDomainExtensions() throws Exception {
        extensionsTest(null, domainMasterLifecycleUtil.getDomainClient());
    }

    @Test
    public void testMasterServerExtensions() throws Exception {
        extensionsTest("host=master/server=main-one", domainMasterLifecycleUtil.getDomainClient());
    }

    @Test
    public void testSlaveServerExtensions() throws Exception {
        extensionsTest("host=slave/server=other-two", domainSlaveLifecycleUtil.getDomainClient());
    }

    private void rootResourceTest(String address, ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode(address, "read-resource");

        ModelNode result = executeOperation(op, client);
        ModelNode major = result.get("management-major-version");
        ModelNode minor = result.get("management-minor-version");
        Assert.assertEquals(ModelType.INT, major.getType());
        Assert.assertEquals(ModelType.INT, minor.getType());
        Assert.assertEquals(Version.MANAGEMENT_MAJOR_VERSION, major.asInt());
        Assert.assertEquals(Version.MANAGEMENT_MINOR_VERSION, minor.asInt());
    }

    private void extensionsTest(String address, ModelControllerClient client) throws Exception {
        ModelNode op = createOpNode(address, "read-children-resources");
        op.get("child-type").set("extension");
        op.get("recursive").set(true);
        op.get("include-runtime").set(true);

        ModelNode result = executeOperation(op, client);
        for (Property extension : result.asPropertyList()) {
            String extensionName = extension.getName();
            ModelNode subsystems = extension.getValue().get("subsystem");
            Assert.assertEquals(extensionName + " has no subsystems", ModelType.OBJECT, subsystems.getType());
            for (Property subsystem : subsystems.asPropertyList()) {
                String subsystemName = subsystem.getName();
                ModelNode value = subsystem.getValue();
                Assert.assertEquals(subsystemName + " has major version", ModelType.INT, value.get("management-major-version").getType());
                Assert.assertEquals(subsystemName + " has minor version", ModelType.INT, value.get("management-minor-version").getType());
                Assert.assertEquals(subsystemName + " has namespaces", ModelType.LIST, value.get("xml-namespaces").getType());
                Assert.assertTrue(subsystemName + " has positive major version", value.get("management-major-version").asInt() > 0);
                Assert.assertTrue(subsystemName + " has non-negative minor version", value.get("management-minor-version").asInt() >= 0);
                Assert.assertTrue(subsystemName + " has more than zero namespaces", value.get("xml-namespaces").asInt() > 0);
            }
        }
    }

    private static ModelNode createOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        ModelNode list = op.get("address").setEmptyList();
        if (address != null) {
            String [] pathSegments = address.split("/");
            for (String segment : pathSegments) {
                String[] elements = segment.split("=");
                list.add(elements[0], elements[1]);
            }
        }
        op.get("operation").set(operation);
        return op;
    }

    private ModelNode executeOperation(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        ModelNode ret = modelControllerClient.execute(op);

        if (! SUCCESS.equals(ret.get(OUTCOME).asString())) {
            throw new MgmtOperationException("Management operation failed.", op, ret);
        }
        return ret.get(RESULT);
    }
}
