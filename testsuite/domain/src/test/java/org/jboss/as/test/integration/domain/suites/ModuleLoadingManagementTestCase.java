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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE_LOADING;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;

import java.io.File;
import java.util.List;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests of the core-service=module-loading resource.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class ModuleLoadingManagementTestCase {

    private static final PathElement HOST_PE = PathElement.pathElement(HOST, "master");
    private static final PathElement SERVER_PE = PathElement.pathElement(SERVER, "main-one");
    private static final PathElement RESOURCE = PathElement.pathElement(CORE_SERVICE, MODULE_LOADING);
    private static final PathAddress HOST_PA = PathAddress.pathAddress(HOST_PE, RESOURCE);
    private static final PathAddress SERVER_PA = PathAddress.pathAddress(HOST_PE, SERVER_PE, RESOURCE);

    private static final String MODULES_DIR = File.separator + "modules";
    private static final String LAYERS_BASE = MODULES_DIR + File.separator + "system" + File.separator
            + "layers" + File.separator + "base";

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ReadEnvironmentVariablesTestCase.class.getSimpleName());

        domainMasterLifecycleUtil = testSupport.getDomainMasterLifecycleUtil();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        DomainTestSuite.stopSupport();
        testSupport = null;
        domainMasterLifecycleUtil = null;
    }

    @Test
    public void testModuleRootsAttribute() throws Exception {
        moduleRootAttributeTest(true);
        moduleRootAttributeTest(false);
    }

    private void moduleRootAttributeTest(boolean forServer) throws Exception {
        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode op = Util.createEmptyOperation("read-attribute", forServer ? SERVER_PA : HOST_PA);
        op.get(NAME).set("module-roots");

        ModelNode response = domainClient.execute(op);
        List<ModelNode> result = validateResponse(response).asList();
        boolean hasModules = false;
        boolean hasBase = false;
        for (ModelNode node : result) {
            String root = node.asString();
            if (root.endsWith(MODULES_DIR)) {
                Assert.assertFalse(hasModules);
                hasModules = true;
            }
            if (root.endsWith(LAYERS_BASE)) {
                Assert.assertFalse(hasBase);
                hasBase = true;
            }
        }
        Assert.assertTrue(hasModules);
        Assert.assertTrue(hasBase);
    }

    @Test
    public void testListResourceLoaderPaths() throws Exception {

        DomainClient domainClient = domainMasterLifecycleUtil.getDomainClient();
        ModelNode op = Util.createEmptyOperation("list-resource-loader-paths", HOST_PA);
        op.get(MODULE).set("org.hibernate");

        ModelNode response = domainClient.execute(op);
        List<ModelNode> hostResult = validateResponse(response).asList();
        Assert.assertTrue(hostResult.size() > 0);
        for (ModelNode path : hostResult) {
            Assert.assertTrue(path.asString().contains(LAYERS_BASE));
        }

        op = Util.createEmptyOperation("list-resource-loader-paths", SERVER_PA);
        op.get(MODULE).set("org.hibernate");

        response = domainClient.execute(op);
        List<ModelNode> serverResult = validateResponse(response).asList();
        Assert.assertEquals(hostResult.size(), serverResult.size());
        for (ModelNode node : serverResult) {
            Assert.assertTrue(hostResult.contains(node));
        }
    }
}
