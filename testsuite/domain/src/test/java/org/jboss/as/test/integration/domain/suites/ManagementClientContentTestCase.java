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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.IN_SERIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_CLIENT_CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ACROSS_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLOUT_PLANS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateFailedResponse;
import static org.jboss.as.test.integration.domain.DomainTestSupport.validateResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.jboss.as.arquillian.container.domain.managed.DomainLifecycleUtil;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of using the domain content repo for storing and accessing management client content.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ManagementClientContentTestCase {

    private static DomainTestSupport testSupport;
    private static DomainLifecycleUtil domainMasterLifecycleUtil;
    private static DomainLifecycleUtil domainSlaveLifecycleUtil;

    private static long key = System.currentTimeMillis();

    private static final ModelNode ROLLOUT_PLANS_ADDRESS = new ModelNode().add(MANAGEMENT_CLIENT_CONTENT, ROLLOUT_PLANS);
    private static final ModelNode ROLLOUT_PLAN_A = new ModelNode();
    private static final ModelNode ROLLOUT_PLAN_B = new ModelNode();
    private static final ModelNode ROLLOUT_PLAN_C = new ModelNode();

    static {
        ROLLOUT_PLANS_ADDRESS.protect();

        ROLLOUT_PLAN_A.get(ROLLOUT_PLAN, IN_SERIES).add("main-server-group", new ModelNode());
        ROLLOUT_PLAN_A.protect();
        ROLLOUT_PLAN_B.get(ROLLOUT_PLAN, IN_SERIES).add("other-server-group", new ModelNode());
        ROLLOUT_PLAN_B.protect();
        ROLLOUT_PLAN_C.get(ROLLOUT_PLAN, IN_SERIES).add("main-server-group", new ModelNode());
        ROLLOUT_PLAN_C.get(ROLLOUT_PLAN, ROLLBACK_ACROSS_GROUPS).set(false);
        ROLLOUT_PLAN_C.protect();
    }

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(ManagementClientContentTestCase.class.getSimpleName());
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
    public void testRolloutPlans() throws IOException {

        final String planA = getContentName();
        final ModelNode addressA = getContentAddress(ROLLOUT_PLANS_ADDRESS, ROLLOUT_PLAN, planA);
        final String planB = getContentName();
        final ModelNode addressB = getContentAddress(ROLLOUT_PLANS_ADDRESS, ROLLOUT_PLAN, planB);

        // Check overall hashes match on master and slave
        ModelNode overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, null, false);

        // Add content

        ModelNode op = Util.getEmptyOperation(ADD, addressA);
        op.get(CONTENT).set(ROLLOUT_PLAN_A);
        ModelNode response = masterClient.execute(op);
        validateResponse(response);
        response = masterClient.execute(getReadAttributeOperation(addressA, CONTENT));
        ModelNode returnVal = validateResponse(response);
        assertEquals(ROLLOUT_PLAN_A, returnVal);

        // Confirm plan hashes match on master and slave
        validateHashes(addressA, new ModelNode(), true);
        // Check overall hashes match on master and slave
        overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, overallHash, true);

        // Add another

        op = Util.getEmptyOperation(ADD, addressB);
        op.get(CONTENT).set(ROLLOUT_PLAN_B);
        response = masterClient.execute(op);
        validateResponse(response);
        response = masterClient.execute(getReadAttributeOperation(addressB, CONTENT));
        returnVal = validateResponse(response);
        assertEquals(ROLLOUT_PLAN_B, returnVal);

        // Confirm plan hashes match on master and slave
        validateHashes(addressB, new ModelNode(), true);
        // Check overall hashes match on master and slave
        overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, overallHash, true);

        // Validate read-children names

        op = Util.getEmptyOperation(READ_CHILDREN_NAMES_OPERATION, ROLLOUT_PLANS_ADDRESS);
        op.get(CHILD_TYPE).set(ROLLOUT_PLAN);
        response = masterClient.execute(op);
        returnVal = validateResponse(response);
        List<ModelNode> plans = returnVal.asList();
        assertEquals(2, plans.size());
        for (ModelNode node : plans) {
            if (!planA.equals(node.asString())) {
                assertEquals(planB, node.asString());
            }
        }

        // Simple write-attribute

        op = Util.getEmptyOperation(WRITE_ATTRIBUTE_OPERATION, addressB);
        op.get(NAME).set(CONTENT);
        op.get(VALUE).set(ROLLOUT_PLAN_C);

        response = masterClient.execute(op);
        validateResponse(response);
        response = masterClient.execute(getReadAttributeOperation(addressB, CONTENT));
        returnVal = validateResponse(response);
        assertEquals(ROLLOUT_PLAN_C, returnVal);

        // Confirm plan hashes match on master and slave
        ModelNode planBHash = validateHashes(addressB, new ModelNode(), true);
        // Check overall hashes match on master and slave
        overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, overallHash, true);

        // Store op

        op = Util.getEmptyOperation("store", addressB);
        op.get(HASH).set(planBHash);
        op.get(CONTENT).set(ROLLOUT_PLAN_B);

        response = masterClient.execute(op);
        validateResponse(response);
        response = masterClient.execute(getReadAttributeOperation(addressB, CONTENT));
        returnVal = validateResponse(response);
        assertEquals(ROLLOUT_PLAN_B, returnVal);

        // Confirm plan hashes match on master and slave
        planBHash = validateHashes(addressB, planBHash, true);
        // Check overall hashes match on master and slave
        overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, overallHash, true);

        // Failed store op (wrong value in hash param)

        op = Util.getEmptyOperation("store", addressB);
        op.get(HASH).set(new byte[20]); // incorrect value
        op.get(CONTENT).set(ROLLOUT_PLAN_B);

        response = masterClient.execute(op);
        validateFailedResponse(response);
        response = masterClient.execute(getReadAttributeOperation(addressB, CONTENT));
        returnVal = validateResponse(response);
        assertEquals(ROLLOUT_PLAN_B, returnVal);

        // Confirm plan hashes match on master and slave
        validateHashes(addressB, planBHash, false);
        // Check overall hashes match on master and slave
        overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, overallHash, false);

        // Remove plan

        op = Util.getEmptyOperation(REMOVE, addressB);
        response = masterClient.execute(op);
        validateResponse(response);

        // Check overall hashes match on master and slave
        overallHash = validateHashes(ROLLOUT_PLANS_ADDRESS, overallHash, true);
    }

    private ModelNode validateHashes(ModelNode address, ModelNode currentHash, boolean expectChange) throws IOException {

        // Start with reads of the root resource
        ModelNode response = masterClient.execute(getReadAttributeOperation(address, HASH));
        ModelNode overallHash = validateResponse(response);

        response = slaveClient.execute(getReadAttributeOperation(address, HASH));
        ModelNode slaveOverallHash = validateResponse(response);

        Assert.assertEquals(overallHash, slaveOverallHash);

        if (currentHash != null) {
            if (expectChange) {
                assertFalse(overallHash.equals(currentHash));
            } else {
                assertTrue(overallHash.equals(currentHash));
            }
        }

        return overallHash;
    }

    private ModelNode getContentAddress(final ModelNode parentAddress, final String type, final String name) {
        return parentAddress.clone().add(type, name);
    }

    private String getContentName() {
        final String result = getClass().getSimpleName() + key;
        key++;
        return result;
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
