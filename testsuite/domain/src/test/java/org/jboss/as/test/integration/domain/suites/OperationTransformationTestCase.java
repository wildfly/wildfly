/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.createCompositeOperation;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.createOperation;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForFailure;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.executeForResult;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.exists;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.getRunningServerAddress;
import junit.framework.Assert;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.extension.ExtensionSetup;
import org.jboss.as.test.integration.domain.extension.VersionedExtensionCommon;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Emanuel Muckenhuber
 */
public class OperationTransformationTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil master;
    private static DomainLifecycleUtil slave;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(OperationTransformationTestCase.class.getSimpleName());

        master = testSupport.getDomainMasterLifecycleUtil();
        slave = testSupport.getDomainSlaveLifecycleUtil();
        // Initialize the extension
        ExtensionSetup.initializeTransformersExtension(testSupport);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainTestSuite.stopSupport();
        testSupport = null;
        master = null;
        slave = null;
    }

    @Test
    public void test() throws Exception {
        final PathAddress extension = PathAddress.pathAddress(PathElement.pathElement(EXTENSION, VersionedExtensionCommon.EXTENSION_NAME));
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME));
        final PathAddress ignored = PathAddress.pathAddress(PathElement.pathElement(PROFILE, "ignored"),
                PathElement.pathElement(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME));

        final ModelNode serverAddress = getRunningServerAddress("slave", "main-three");
        serverAddress.add(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME);

        final DomainClient client = master.getDomainClient();
        // Add extension
        final ModelNode extensionAdd = createAdd(extension);
        executeForResult(extensionAdd, client);
        // Add subsystem
        final ModelNode subsystemAdd = createAdd(address);
        executeForResult(subsystemAdd, client);
        // Check master version
        final ModelNode mExt = create(READ_RESOURCE_OPERATION, extension.append(PathElement.pathElement(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME)));
        assertVersion(executeForResult(mExt, client), ModelVersion.create(2));

        // Create subsystem in the ignored profile for further use
        final ModelNode newIgnored = createAdd(ignored);
        executeForResult(newIgnored, client);
        Assert.assertTrue(exists(ignored, client));

        // Check the new element
        final PathAddress newElement = address.append(PathElement.pathElement("new-element", "new1"));
        final ModelNode addNew = createAdd(newElement);
        executeForResult(addNew, client);
        Assert.assertTrue(exists(newElement, client));

        // The add operation on the slave should have been discarded
        final ModelNode newElementOnSlave = serverAddress.clone();
        newElementOnSlave.add("new-element", "new1");
        Assert.assertFalse(exists(newElementOnSlave, client));

        // Other new element
        final PathElement otherNewElementPath = PathElement.pathElement("other-new-element", "new1");
        // This needs to get rejected
        final ModelNode addOtherNew = createAdd(address.append(otherNewElementPath));
        executeForFailure(addOtherNew, client);

        // Same if wrapped in a composite
        final ModelNode cpa = createCompositeOperation(addOtherNew);
        executeForFailure(cpa, client);

        // This should work
        final ModelNode addOtherNewIgnored = createAdd(ignored.append(otherNewElementPath));
        executeForResult(addOtherNewIgnored, client);

        // Check the renamed element
        final PathAddress renamedAddress = address.append(PathElement.pathElement("renamed", "element"));
        final ModelNode renamedAdd = createAdd(renamedAddress);
        executeForResult(renamedAdd, client);
        Assert.assertTrue(exists(renamedAddress, client));


        // renamed > element
        final ModelNode renamedElementOnSlave = serverAddress.clone();
        renamedElementOnSlave.add("renamed", "element");
        Assert.assertFalse(exists(renamedElementOnSlave, client));

        // element > renamed
        final ModelNode elementRenamedOnSlave = serverAddress.clone();
        elementRenamedOnSlave.add("element", "renamed");
        Assert.assertTrue(exists(elementRenamedOnSlave, client));

//        final ModelNode op = create(READ_RESOURCE_OPERATION, PathAddress.pathAddress(PathElement.pathElement("profile", "ignored")));
//        System.out.println(executeForResult(op, slave.getDomainClient()));

        final ModelNode update = new ModelNode();
        update.get(OP).set("update");
        update.get(OP_ADDR).set(address.toModelNode());

        //
        final ModelNode updateResult = client.execute(update);
        Assert.assertEquals(updateResult.toString(), updateResult.get(OUTCOME).asString(), SUCCESS);
        // "result" => {"test-attribute" => "test"},
        Assert.assertEquals("test", updateResult.get(RESULT, "test-attribute").asString());
        // server-result
        Assert.assertEquals("test", updateResult.get(SERVER_GROUPS, "main-server-group", HOST, "slave", "main-three", "response", RESULT, "test-attribute").asString());

        //
        final ModelNode write = new ModelNode();
        write.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        write.get(OP_ADDR).set(address.toModelNode());
        write.get(NAME).set("test-attribute");
        write.get(VALUE).set("test123");

        //
        final ModelNode composite = new ModelNode();
        composite.get(OP).set(COMPOSITE);
        composite.get(OP_ADDR).setEmptyList();
        final ModelNode steps = composite.get(STEPS);

        final ModelNode test = new ModelNode();
        test.get(OP).set("test");
        test.get(OP_ADDR).set(serverAddress);

        steps.add(write);
        steps.add(test);


        final ModelNode compositeResult = client.execute(composite);
        // server-result
        Assert.assertEquals(false, compositeResult.get(SERVER_GROUPS, "main-server-group", HOST, "slave", "main-three", "response", RESULT, "step-2", RESULT).asBoolean());

        // Test expression replacement
        testPropertiesModel();
    }

    protected void testPropertiesModel() throws Exception {
        final DomainClient client = master.getDomainClient();
        final DomainClient slaveClient = slave.getDomainClient();

        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(PROFILE, "default"));

        // Test the properties model
        final PathAddress properties = address.append(PathElement.pathElement(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME));

        final ModelNode writePropertiesInt = writeAttribute(properties, "int", "${org.jboss.domain.tests.int:1}");
        executeForFailure(writePropertiesInt, client);
        // Check both master and slave
        Assert.assertFalse(executeForResult(readAttribute(properties, "int"), client).isDefined());
        Assert.assertFalse(executeForResult(readAttribute(properties, "int"), slaveClient).isDefined());

        final ModelNode writePropertiesString = writeAttribute(properties, "string", "${org.jboss.domain.tests.string:abc}");
        executeForFailure(writePropertiesString, client);
        // Check both master and slave
        Assert.assertFalse(executeForResult(readAttribute(properties, "string"), client).isDefined());
        Assert.assertFalse(executeForResult(readAttribute(properties, "string"), slaveClient).isDefined());

        // Test the ignored model
        final PathAddress ignored = PathAddress.pathAddress(PathElement.pathElement(PROFILE, "ignored"), PathElement.pathElement(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME));

        final ModelNode writeIgnoredString = writeAttribute(ignored, "string", "${org.jboss.domain.tests.string:abc}");
        executeForResult(writeIgnoredString, client);
        Assert.assertTrue(executeForResult(readAttribute(ignored, "string"), client).isDefined());
        executeForFailure(readAttribute(ignored, "string"), slaveClient);

        final ModelNode writeIgnoredInt = writeAttribute(ignored, "int", "${org.jboss.domain.tests.int:1}");
        executeForResult(writeIgnoredInt, client);
        Assert.assertTrue(executeForResult(readAttribute(ignored, "int"), client).isDefined());
        executeForFailure(readAttribute(ignored, "int"), slaveClient);
    }

    static ModelNode createAdd(PathAddress address) {
        return create(ADD, address);
    }

    static ModelNode create(String op, ModelNode address) {
        return createOperation(op, address);
    }

    static ModelNode create(String op, PathAddress address) {
        return createOperation(op, address);
    }

    static ModelNode writeAttribute(PathAddress address, String name, int value) {
        final ModelNode operation = writeAttribute(address, name);
        operation.get(VALUE).set(value);
        return operation;
    }

    static ModelNode writeAttribute(PathAddress address, String name, String value) {
        final ModelNode operation = writeAttribute(address, name);
        operation.get(VALUE).set(value);
        return operation;
    }

    static ModelNode readAttribute(PathAddress address, String name) {
        final ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, address);
        operation.get(NAME).set(name);
        return operation;
    }

    static ModelNode writeAttribute(PathAddress address, String name) {
        final ModelNode operation =  createOperation(WRITE_ATTRIBUTE_OPERATION, address);
        operation.get(NAME).set(name);
        return operation;
    }

    static void assertVersion(final ModelNode v, ModelVersion version) {
        Assert.assertEquals(version.getMajor(), v.get(MANAGEMENT_MAJOR_VERSION).asInt());
        Assert.assertEquals(version.getMinor(), v.get(MANAGEMENT_MINOR_VERSION).asInt());
        Assert.assertEquals(version.getMicro(), v.get(MANAGEMENT_MICRO_VERSION).asInt());
    }

}
