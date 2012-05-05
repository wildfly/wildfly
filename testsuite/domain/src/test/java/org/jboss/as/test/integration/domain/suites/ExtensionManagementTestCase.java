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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.as.test.integration.domain.extension.TestExtension;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.StreamExporter;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests management of extensions.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ExtensionManagementTestCase {

    private static final String ADDRESS = "extension=" + TestExtension.MODULE_NAME;

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ExtensionManagementTestCase.class.getSimpleName());
        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
        domainSlaveLifecycleUtil = testSupport.getDomainSlaveLifecycleUtil();

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        InputStream moduleXml = tccl.getResourceAsStream("extension/module.xml");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class);
        URL url = tccl.getResource("extension/META-INF/services/org.jboss.as.controller.Extension");
        jar.addAsManifestResource(url, "services/org.jboss.as.controller.Extension");
        jar.addPackage(TestExtension.class.getPackage());
        StreamExporter exporter = jar.as(ZipExporter.class);
        Map<String, StreamExporter> content = Collections.singletonMap("test-extension.jar", exporter);

        testSupport.addTestModule(TestExtension.MODULE_NAME, moduleXml, content);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        testSupport = null;
        domainMasterLifecycleUtil = null;
        domainSlaveLifecycleUtil = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testAddRemoveExtension() throws Exception  {
        ModelNode op = createOpNode(ADDRESS, "add");
        DomainClient masterClient = domainMasterLifecycleUtil.getDomainClient();
        executeForResult(op, masterClient);
        extensionVersionTest(masterClient, null);
        extensionVersionTest(masterClient, "host=master/server=main-one");
        extensionVersionTest(masterClient, "host=slave/server=main-three");
        DomainClient slaveClient = domainSlaveLifecycleUtil.getDomainClient();
        extensionVersionTest(slaveClient, null);

        op = createOpNode(ADDRESS, "remove");
        executeForResult(op, masterClient);
        extensionRemovalTest(masterClient, null);
        extensionRemovalTest(masterClient, "host=master/server=main-one");
        extensionRemovalTest(masterClient, "host=slave/server=main-three");
        extensionRemovalTest(slaveClient, null);
    }

    private void extensionVersionTest(ModelControllerClient client, String addressPrefix) throws Exception {

        String address = addressPrefix == null ? ADDRESS : addressPrefix + '/' + ADDRESS;
        ModelNode op = createOpNode(address, "read-resource");
        op.get("recursive").set(true);
        op.get("include-runtime").set(true);

        ModelNode result = executeForResult(op, client);
        ModelNode subsystems = result.get("subsystem");
        Assert.assertEquals("extension has no subsystems", ModelType.OBJECT, subsystems.getType());
        for (Property subsystem : subsystems.asPropertyList()) {
            String subsystemName = subsystem.getName();
            int version = Integer.parseInt(subsystemName);
            ModelNode value = subsystem.getValue();
            Assert.assertEquals(subsystemName + " has major version", ModelType.INT, value.get("management-major-version").getType());
            Assert.assertEquals(subsystemName + " has minor version", ModelType.INT, value.get("management-minor-version").getType());
            Assert.assertEquals(subsystemName + " has namespaces", ModelType.LIST, value.get("xml-namespaces").getType());
            Assert.assertEquals(subsystemName + " has correct major version", version, value.get("management-major-version").asInt());
            Assert.assertEquals(subsystemName + " has correct minor version", version, value.get("management-minor-version").asInt());
            Assert.assertTrue(subsystemName + " has more than zero namespaces", value.get("xml-namespaces").asInt() > 0);
        }
    }

    private void extensionRemovalTest(ModelControllerClient client, String addressPrefix) throws Exception {
        ModelNode op = createOpNode(addressPrefix, "read-children-names");
        op.get("child-type").set("extension");
        List<ModelNode> result = executeForResult(op, client).asList();
        for (ModelNode extension : result) {
            Assert.assertFalse(TestExtension.MODULE_NAME.equals(extension.asString()));
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

    private static ModelNode executeForResult(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        try {
            return DomainTestUtils.executeForResult(op, modelControllerClient);
        } catch (MgmtOperationException e) {
            System.out.println(" Op failed:");
            System.out.println(e.getOperation());
            System.out.println("with result");
            System.out.println(e.getResult());
            throw e;
        }
    }
}
