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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
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
 * Test of slave hosts ability to ignore ops sent by master for certain resources (AS7-3174).
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IgnoredResourcesTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(IgnoredResourcesTestCase.class.getSimpleName());
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
    public void testProfileIgnored() throws Exception  {

        ModelNode op = createOpNode(null, "read-children-names");
        op.get("child-type").set("profile");

        // Sanity check againt domain
        ModelNode result = executeOperation(op, domainMasterLifecycleUtil.getDomainClient());
        boolean found = false;
        for (ModelNode profile : result.asList()) {
            if ("ignored".equals(profile)) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        // Resource should not exist on slave
        result = executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
        for (ModelNode profile : result.asList()) {
            if ("ignored".equals(profile)) {
                Assert.fail("found profile 'ignored'");
            }
        }

        // Modify the ignored profile
        ModelNode mod = createOpNode("/profile=ignored/subsystem=naming", "add");
        executeOperation(mod, domainMasterLifecycleUtil.getDomainClient());

        // Resource still should not exist on slave
        result = executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
        for (ModelNode profile : result.asList()) {
            if ("ignored".equals(profile)) {
                Assert.fail("found profile 'ignored'");
            }
        }
    }

    @Test
    public void testSocketBindingGroupIgnored() throws Exception  {

        ModelNode op = createOpNode(null, "read-children-names");
        op.get("child-type").set("socket-binding-group");

        // Sanity check againt domain
        ModelNode result = executeOperation(op, domainMasterLifecycleUtil.getDomainClient());
        boolean found = false;
        for (ModelNode profile : result.asList()) {
            if ("ignored".equals(profile)) {
                found = true;
                break;
            }
        }
        Assert.assertTrue(found);

        // Resource should not exist on slave
        result = executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
        for (ModelNode profile : result.asList()) {
            if ("ignored".equals(profile)) {
                Assert.fail("found socket-binding-group 'ignored'");
            }
        }

        // Modify the ignored group
        ModelNode mod = createOpNode("/socket-binding-group=ignored/socket-binding=test", "add");
        mod.get("port").set(12345);
        executeOperation(mod, domainMasterLifecycleUtil.getDomainClient());

        // Resource still should not exist on slave
        result = executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
        for (ModelNode profile : result.asList()) {
            if ("ignored".equals(profile)) {
                Assert.fail("found socket-binding-group 'ignored'");
            }
        }

    }

    @Test
    public void testExtensionIgnored() throws Exception  {

        ModelNode op = createOpNode(null, "read-children-names");
        op.get("child-type").set("extension");

        // Sanity check againt domain
        ModelNode result = executeOperation(op, domainMasterLifecycleUtil.getDomainClient());
        boolean found = false;
        for (ModelNode profile : result.asList()) {
            if ("org.jboss.as.jsr77".equals(profile)) {
                Assert.fail("found extension 'org.jboss.as.jsr77'");
            }
        }
        Assert.assertTrue(found);

        // Resource should not exist on slave
        result = executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
        for (ModelNode profile : result.asList()) {
            if ("org.jboss.as.jsr77".equals(profile)) {
                Assert.fail("found extension 'org.jboss.as.jsr77'");
            }
        }

        // Add the ignored extension
        ModelNode mod = createOpNode("/extension=org.jboss.as.jsr77", "add");
        mod.get("port").set(12345);
        executeOperation(mod, domainMasterLifecycleUtil.getDomainClient());

        // Resource still should not exist on slave
        result = executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
        for (ModelNode profile : result.asList()) {
            if ("org.jboss.as.jsr77".equals(profile)) {
                Assert.fail("found extension 'org.jboss.as.jsr77'");
            }
        }

    }

    @Test
    public void testIgnoreTypeHost() throws IOException {

        ModelNode op = createOpNode("/core-service=ignored-resources/ignored-resource-type=host", "add");
        op.get("wildcard").set(true);

        try {
            executeOperation(op, domainSlaveLifecycleUtil.getDomainClient());
            Assert.fail("should not be able to ignore type 'host'");
        } catch (MgmtOperationException good) {
            //
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
