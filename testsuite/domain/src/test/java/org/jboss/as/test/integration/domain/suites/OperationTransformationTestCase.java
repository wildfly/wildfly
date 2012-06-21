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
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.extension.ExtensionSetup;
import org.jboss.as.test.integration.domain.extension.VersionedExtensionCommon;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import static org.jboss.as.test.integration.domain.management.util.DomainTestUtils.*;
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
        final ModelNode address = PathAddress.pathAddress(PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME)).toModelNode();

        final DomainClient client = master.getDomainClient();
        // Add extension
        final ModelNode extensionAdd = new ModelNode();
        extensionAdd.get(OP).set(ADD);
        extensionAdd.get(OP_ADDR).add(EXTENSION, VersionedExtensionCommon.EXTENSION_NAME);
        executeForResult(extensionAdd, client);
        // Add subsystem
        final ModelNode subsystemAdd = new ModelNode();
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(address);
        executeForResult(subsystemAdd, client);
        // Check master version
        final ModelNode mExt = new ModelNode();
        mExt.get(OP).set(READ_RESOURCE_OPERATION);
        mExt.get(OP_ADDR).add(EXTENSION, VersionedExtensionCommon.EXTENSION_NAME).add(SUBSYSTEM, VersionedExtensionCommon.SUBSYSTEM_NAME);

        assertVersion(executeForResult(mExt, client), ModelVersion.create(2));

        //


        final ModelNode update = new ModelNode();
        update.get(OP).set("update");
        update.get(OP_ADDR).set(address);

        executeForResult(update, client);

    }

    static void assertVersion(final ModelNode v, ModelVersion version) {
        Assert.assertEquals(version.getMajor(), v.get(MANAGEMENT_MAJOR_VERSION).asInt());
        Assert.assertEquals(version.getMinor(), v.get(MANAGEMENT_MINOR_VERSION).asInt());
        Assert.assertEquals(version.getMicro(), v.get(MANAGEMENT_MICRO_VERSION).asInt());
    }

}
