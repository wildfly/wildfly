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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.test.integration.domain.management.util.DomainTestSupport.validateResponse;

import java.io.IOException;

import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.test.integration.domain.management.util.DomainLifecycleUtil;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Basic tests of datasource support in a managed domain.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public class DatasourceTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static final ModelNode ROOT_ADDRESS = new ModelNode().setEmptyList();
    private static final ModelNode MASTER_ROOT_ADDRESS = new ModelNode().add(HOST, "master");
    private static final ModelNode SLAVE_ROOT_ADDRESS = new ModelNode().add(HOST, "slave");
    private static final ModelNode MAIN_RUNNING_SERVER_ADDRESS = new ModelNode().add(HOST, "master").add(SERVER, "main-one");
    private static final ModelNode MAIN_RUNNING_SERVER_DS_ADDRESS = new ModelNode().add(HOST, "master")
            .add(SERVER, "main-one").add(SUBSYSTEM, "datasources").add("data-source", "ExampleDS");

    static {
        ROOT_ADDRESS.protect();
        MASTER_ROOT_ADDRESS.protect();
        SLAVE_ROOT_ADDRESS.protect();
        MAIN_RUNNING_SERVER_ADDRESS.protect();
        MAIN_RUNNING_SERVER_DS_ADDRESS.protect();
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(DatasourceTestCase.class.getSimpleName());
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

    @Test
    public void testDatasourceConnection() throws IOException {

        // AS7-6062 -- validate that  ExampleDS works on a domain server
        ModelNode response = masterClient.execute(getEmptyOperation("test-connection-in-pool", MAIN_RUNNING_SERVER_DS_ADDRESS));
        validateResponse(response);
    }

    private static ModelNode getEmptyOperation(String operationName, ModelNode address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        if (address != null) {
            op.get(OP_ADDR).set(address);
        }
        else {
            // Just establish the standard structure; caller can fill in address later
            op.get(OP_ADDR).setEmptyList();
        }
        return op;
    }
}
