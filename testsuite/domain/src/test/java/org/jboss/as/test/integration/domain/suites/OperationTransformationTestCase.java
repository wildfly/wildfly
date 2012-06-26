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

import junit.framework.Assert;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.extension.ExtensionSetup;
import org.jboss.as.test.integration.domain.extension.VersionedExtensionCommon;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.*;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

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

        // Check the new element
        final PathAddress newElement = address.append(PathElement.pathElement("new-element", "new1"));
        final ModelNode addNew = createAdd(newElement);
        executeForResult(addNew, client);
        Assert.assertTrue(exists(newElement, client));

        // The add operation on the slave should have been discarded
        final ModelNode newElementOnSlave = serverAddress.clone();
        newElementOnSlave.add("new-element", "new1");
        Assert.assertFalse(exists(newElementOnSlave, client));

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

        // Update
        executeForResult(create("update", address), client);

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

    static void assertVersion(final ModelNode v, ModelVersion version) {
        Assert.assertEquals(version.getMajor(), v.get(MANAGEMENT_MAJOR_VERSION).asInt());
        Assert.assertEquals(version.getMinor(), v.get(MANAGEMENT_MINOR_VERSION).asInt());
        Assert.assertEquals(version.getMicro(), v.get(MANAGEMENT_MICRO_VERSION).asInt());
    }

}
