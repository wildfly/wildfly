/*
 *
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.jboss.as.test.integration.domain.suites;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test cached-connection-manager runtime-only ops registered against domain profile resources
 */
public class JcaCCMRuntimeOnlyProfileOpsTestCase {
    private static final PathAddress MASTER = PathAddress.pathAddress(ModelDescriptionConstants.HOST, "master");
    private static final PathAddress SLAVE = PathAddress.pathAddress(ModelDescriptionConstants.HOST, "slave");
    private static final PathElement MAIN_ONE = PathElement.pathElement("server", "main-one");
    private static final PathElement MAIN_THREE = PathElement.pathElement("server", "main-three");

    private static final PathAddress PROFILE = PathAddress.pathAddress("profile", "default");
    private static final PathElement SUBSYSTEM = PathElement.pathElement("subsystem", "jca");
    private static final PathElement CCM = PathElement.pathElement("cached-connection-manager", "cached-connection-manager");

    private static DomainTestSupport testSupport;
    private static ModelControllerClient client;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(JcaCCMRuntimeOnlyProfileOpsTestCase.class.getSimpleName());
        client = testSupport.getDomainMasterLifecycleUtil().getDomainClient();
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        client.close();
        client = null;
        testSupport = null;
        DomainTestSuite.stopSupport();
    }

    @Test
    public void testGetNumberOfConnections() throws IOException {
        final String opName = "get-number-of-connections";
        ModelNode op = Util.createEmptyOperation(opName, PROFILE.append(SUBSYSTEM).append(CCM));
        ModelNode response = executeOp(op, SUCCESS);

        assertFalse(response.toString(), response.hasDefined(RESULT)); // handler doesn't set a result on profile
        assertTrue(response.toString(), response.hasDefined(SERVER_GROUPS, "main-server-group", "host", "master", "main-one", "response", "result", "TX"));
        assertTrue(response.toString(), response.hasDefined(SERVER_GROUPS, "main-server-group", "host", "master", "main-one", "response", "result", "NonTX"));
        assertEquals(0, response.get(SERVER_GROUPS, "main-server-group", "host", "master", "main-one", "response", "result", "TX").asInt());
        assertEquals(0, response.get(SERVER_GROUPS, "main-server-group", "host", "master", "main-one", "response", "result", "NonTX").asInt());
        assertTrue(response.toString(), response.hasDefined(SERVER_GROUPS, "main-server-group", "host", "slave", "main-three", "response", "result", "TX"));
        assertTrue(response.toString(), response.hasDefined(SERVER_GROUPS, "main-server-group", "host", "slave", "main-three", "response", "result", "NonTX"));
        assertEquals(0, response.get(SERVER_GROUPS, "main-server-group", "host", "slave", "main-three", "response", "result", "TX").asInt());
        assertEquals(0, response.get(SERVER_GROUPS, "main-server-group", "host", "slave", "main-three", "response", "result", "NonTX").asInt());

        // Now check direct invocation on servers
        op = Util.createEmptyOperation(opName, MASTER.append(MAIN_ONE).append(SUBSYSTEM).append(CCM));
        response = executeOp(op, SUCCESS);
        assertEquals(0, response.get(RESULT).get("TX").asInt());
        assertEquals(0, response.get(RESULT).get("NonTX").asInt());

        op = Util.createEmptyOperation(opName, SLAVE.append(MAIN_THREE).append(SUBSYSTEM).append(CCM));
        response = executeOp(op, SUCCESS);
        assertEquals(0, response.get(RESULT).get("TX").asInt());
        assertEquals(0, response.get(RESULT).get("NonTX").asInt());
    }

    @Test
    public void testListConnections() throws IOException {
        final String opName = "list-connections";
        ModelNode op = Util.createEmptyOperation(opName, PROFILE.append(SUBSYSTEM).append(CCM));
        ModelNode response = executeOp(op, SUCCESS);

        assertFalse(response.toString(), response.hasDefined(RESULT)); // handler doesn't set a result on profile
        assertTrue(response.toString(), response.has(SERVER_GROUPS, "main-server-group", "host", "master", "main-one", "response", "result", "TX"));
        assertTrue(response.toString(), response.has(SERVER_GROUPS, "main-server-group", "host", "master", "main-one", "response", "result", "NonTX"));

        assertTrue(response.toString(), response.has(SERVER_GROUPS, "main-server-group", "host", "slave", "main-three", "response", "result", "TX"));
        assertTrue(response.toString(), response.has(SERVER_GROUPS, "main-server-group", "host", "slave", "main-three", "response", "result", "NonTX"));

        // Now check direct invocation on servers
        op = Util.createEmptyOperation(opName, MASTER.append(MAIN_ONE).append(SUBSYSTEM).append(CCM));
        response = executeOp(op, SUCCESS);
        assertTrue(response.has(RESULT, "TX"));
        assertTrue(response.has(RESULT, "NonTX"));

        op = Util.createEmptyOperation(opName, SLAVE.append(MAIN_THREE).append(SUBSYSTEM).append(CCM));
        response = executeOp(op, SUCCESS);
        assertTrue(response.has(RESULT, "TX"));
        assertTrue(response.has(RESULT, "NonTX"));
    }


    private static ModelNode executeOp(ModelNode op, String outcome) throws IOException {
        ModelNode response = client.execute(op);
        assertTrue(response.toString(), response.hasDefined(OUTCOME));
        assertEquals(response.toString(), outcome, response.get(OUTCOME).asString());
        return response;
    }
}
