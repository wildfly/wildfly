/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACTIVE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXECUTION_STATUS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROFILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.test.integration.management.extension.blocker.BlockerExtension.BLOCK_POINT;
import static org.jboss.as.test.integration.management.extension.blocker.BlockerExtension.CALLER;
import static org.jboss.as.test.integration.management.extension.blocker.BlockerExtension.TARGET_HOST;
import static org.jboss.as.test.integration.management.extension.blocker.BlockerExtension.TARGET_SERVER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.client.helpers.domain.DomainClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.domain.extension.ExtensionSetup;
import org.jboss.as.test.integration.domain.management.util.DomainTestSupport;
import org.jboss.as.test.integration.domain.management.util.DomainTestUtils;
import org.jboss.as.test.integration.management.extension.blocker.BlockerExtension;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test of cancellation of running operations in a domain.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class OperationCancellationTestCase {

    private static final Logger log = Logger.getLogger(OperationCancellationTestCase.class);

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(
            PathElement.pathElement(PROFILE, "default"),
            PathElement.pathElement(SUBSYSTEM, BlockerExtension.SUBSYSTEM_NAME));
    private static final ModelNode BLOCK_OP = Util.createEmptyOperation("block", SUBSYSTEM_ADDRESS);
    private static final ModelNode WRITE_FOO_OP = Util.getWriteAttributeOperation(SUBSYSTEM_ADDRESS, BlockerExtension.FOO.getName(), true);
    private static final PathAddress MGMT_CONTROLLER = PathAddress.pathAddress(
            PathElement.pathElement(CORE_SERVICE, MANAGEMENT),
            PathElement.pathElement(SERVICE, MANAGEMENT_OPERATIONS)
    );

    private static final long GET_TIMEOUT = TimeoutUtil.adjust(10000);

    private static DomainTestSupport testSupport;
    private static DomainClient masterClient;

    @BeforeClass
    public static void setupDomain() throws Exception {
        testSupport = DomainTestSuite.createSupport(OperationCancellationTestCase.class.getSimpleName());
        masterClient = testSupport.getDomainMasterLifecycleUtil().getDomainClient();

        // Initialize the test extension
        ExtensionSetup.initializeBlockerExtension(testSupport);

        ModelNode addExtension = Util.createAddOperation(PathAddress.pathAddress(PathElement.pathElement(EXTENSION, BlockerExtension.MODULE_NAME)));

        executeForResult(addExtension, masterClient);

        ModelNode addSubsystem = Util.createAddOperation(PathAddress.pathAddress(
                PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, BlockerExtension.SUBSYSTEM_NAME)));
        executeForResult(addSubsystem, masterClient);
    }

    @AfterClass
    public static void tearDownDomain() throws Exception {
        ModelNode removeSubsystem = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(
                PathElement.pathElement(PROFILE, "default"),
                PathElement.pathElement(SUBSYSTEM, BlockerExtension.SUBSYSTEM_NAME)));
        executeForResult(removeSubsystem, masterClient);

        ModelNode removeExtension = Util.createEmptyOperation(REMOVE, PathAddress.pathAddress(PathElement.pathElement(EXTENSION, BlockerExtension.MODULE_NAME)));
        executeForResult(removeExtension, masterClient);

        testSupport = null;
        masterClient = null;
        DomainTestSuite.stopSupport();
    }

    @After
    public void awaitCompletion() throws Exception {
        // Start from the leaves of the domain process tree and work inward validating
        // that all block ops are cleared locally. This ensures that a later test doesn't
        // mistakenly cancel a completing op from an earlier test
        validateNoActiveOperation(masterClient, "master", "main-one");
        validateNoActiveOperation(masterClient, "slave", "main-three");
        validateNoActiveOperation(masterClient, "slave", null);
        validateNoActiveOperation(masterClient, "master", null);
    }

    @Test
    public void testMasterBlockModel() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.EXECUTING, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.EXECUTING, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertFailedOrCancelled(response);
        validateNoActiveOperation(masterClient, "master", null, id, false);
    }

    @Test
    public void testMasterBlockRuntime() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.EXECUTING, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.EXECUTING, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertFailedOrCancelled(response);
        validateNoActiveOperation(masterClient, "master", null, id, false);
    }

    @Test
    @Ignore("MSC-143")
    public void testMasterBlockServiceStart() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", null, id, false);
    }

    @Test
    @Ignore("MSC-143")
    public void testMasterBlockServiceStartCancelFuture() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        blockFuture.cancel(true);
        try {
            ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
            fail("not cancelled: " + response);
        } catch (CancellationException good) {
            // good
        }
        validateNoActiveOperation(masterClient, "master", null, id, false);
    }

    @Test
    public void testMasterBlockVerify() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.EXECUTING, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.EXECUTING, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertFailedOrCancelled(response);
        validateNoActiveOperation(masterClient, "master", null, id, false);
    }

    @Test
    public void testMasterBlockCompletion() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.COMMIT);
        String blockId = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.COMPLETING, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.COMPLETING, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), SUCCESS, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", null, blockId, false);
    }

    @Test
    public void testMasterBlockRollback() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.ROLLBACK);
        String blockId = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.ROLLING_BACK, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // master has already failed when we cancel, so it reports as failed
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", null, blockId, false);
    }

    @Test
    public void testMasterBlockOtherOpCancelOtherOp() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", null, BlockerExtension.BlockPoint.COMMIT);
        String blockId = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.COMPLETING, start);
        Future<ModelNode> writeFuture = masterClient.executeAsync(WRITE_FOO_OP, OperationMessageHandler.DISCARD);
        cancel(masterClient, "master", null, "write-attribute", OperationContext.ExecutionStatus.AWAITING_OTHER_OPERATION, start, false);
        ModelNode response = writeFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        blockFuture.cancel(true);
        validateNoActiveOperation(masterClient, "master", null, blockId, true);
    }

    @Test
    public void testMasterBlockOtherOpCancelBlockOp() throws Exception {
        long start = System.currentTimeMillis();
        block("master", null, BlockerExtension.BlockPoint.COMMIT);
        String blockId = findActiveOperation(masterClient, "master", null, "block", OperationContext.ExecutionStatus.COMPLETING, start);
        Future<ModelNode> writeFuture = masterClient.executeAsync(WRITE_FOO_OP, OperationMessageHandler.DISCARD);
        findActiveOperation(masterClient, "master", null, "write-attribute", OperationContext.ExecutionStatus.AWAITING_OTHER_OPERATION, start);
        cancel(masterClient, "master", null, "block", OperationContext.ExecutionStatus.COMPLETING, start, false);
        ModelNode response = writeFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), SUCCESS, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", null, blockId, true);
    }

    @Test
    public void testSlaveBlockModelCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    public void testSlaveBlockRuntimeCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveBlockServiceStartCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveBlockServiceStartCancelMasterFuture() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        blockFuture.cancel(true);
        try {
            ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
            fail("not cancelled: " + response);
        } catch (CancellationException good) {
            // good
        }
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    public void testSlaveBlockVerifyCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    public void testSlaveBlockCompletionCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.COMMIT);
        String id = findActiveOperation(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.COMPLETING, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // The master may or may not be done with DomainSlaveHandler.execute and DomainRolloutStepHandler.execute
        // when cancelled. If yes, result is SUCCESS, if not result is CANCELLED
        assertSuccessOrCancelled(response);
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    public void testSlaveBlockRollbackCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.ROLLBACK);
        String id = findActiveOperation(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // Slave fails before Stage.DONE so no normal prepared message in DONE with the failure. The block in
        // rollback prevents the failure coming via the final response. So the master doesn't know about
        // the failure and just detects the local cancellation. So, CANCELLED
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, id, true);
    }

    @Test
    public void testSlaveBlockOtherOpCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.MODEL);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", null, start);
        Future<ModelNode> writeFuture = masterClient.executeAsync(WRITE_FOO_OP, OperationMessageHandler.DISCARD);
        cancel(masterClient, "master", null, "write-attribute", OperationContext.ExecutionStatus.AWAITING_OTHER_OPERATION, start, false);
        ModelNode response = writeFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        blockFuture.cancel(true);
        validateNoActiveOperation(masterClient, "slave", null, blockId, true);
    }

    @Test
    public void testSlaveBlockModelCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.MODEL);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "slave", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, blockId, false);
    }

    @Test
    public void testSlaveBlockRuntimeCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.RUNTIME);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "slave", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, blockId, false);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveBlockServiceStartCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.SERVICE_START);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, blockId, false);
    }

    @Test
    public void testSlaveBlockVerifyCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.VERIFY);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "slave", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, blockId, false);
    }

    @Test
    public void testSlaveBlockCompletionCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.COMMIT);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.COMPLETING, start);
        cancel(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.COMPLETING , start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // cancellation any time in Stage.DONE on slave will not result in a prepare-phase failed response to
        // master, so this should always be success
        assertEquals(response.asString(), SUCCESS, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, blockId, true);
    }

    @Test
    public void testSlaveBlockRollbackCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", null, BlockerExtension.BlockPoint.ROLLBACK);
        String blockId = findActiveOperation(masterClient, "slave", null, "block", null, start);
        cancel(masterClient, "slave", null, "block", OperationContext.ExecutionStatus.ROLLING_BACK, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // Cancelling the rollback does not prevent the master learning that the slave failed.
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", null, blockId, true);
    }

    @Test
    public void testMasterServerBlockModelCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testMasterServerBlockRuntimeCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testMasterServerBlockServiceStartCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testMasterServerBlockServiceStartCancelMasterFuture() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        blockFuture.cancel(true);
        try {
            ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
            fail("not cancelled: " + response);
        } catch (CancellationException good) {
            // good
        }
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testMasterServerBlockVerifyCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testMasterServerBlockCompletionCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.COMMIT);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.COMPLETING, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // The result may be success or cancelled depending on whether the cancelled executed while
        // DomainRolloutStepHandler.execute was running (CANCELLED) or afterwards (SUCCESS)
        assertSuccessOrCancelled(response);
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testMasterServerBlockRollbackCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.ROLLBACK);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // The server op does not reach the prepare stage, so the master doesn't either and reports as cancelled
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testMasterServerBlockModelCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", "main-one", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, false);
    }

    @Test
    public void testMasterServerBlockRuntimeCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", "main-one", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, false);
    }

    @Test
    @Ignore("MSC-143")
    public void testMasterServerBlockServiceStartCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, false);
    }

    @Test
    public void testMasterServerBlockVerifyCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", null, start);
        cancel(masterClient, "master", "main-one", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, false);
    }

    @Test
    public void testMasterServerBlockCompletionCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.COMMIT);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.COMPLETING, start);
        cancel(masterClient, "master", "main-one", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // cancelling on the server during Stage.DONE should not result in a prepare-phase failure sent to master,
        // so result should always be SUCCESS
        assertEquals(response.asString(), SUCCESS, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testMasterServerBlockRollbackCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("master", "main-one", BlockerExtension.BlockPoint.ROLLBACK);
        String id = findActiveOperation(masterClient, "master", "main-one", "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        cancel(masterClient, "master", "main-one", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // Server never reaches Stage.DONE before blocking, so master is waiting for initial response.
        // Cancelling the rollback on server doesn't change that response from FAILED. So master sees and reports failure.
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "master", "main-one", id, true);
    }

    @Test
    public void testSlaveServerBlockModelCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id , true);
    }

    @Test
    public void testSlaveServerBlockRuntimeCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveServerBlockServiceStartCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveServerBlockServiceStartCancelMasterFuture() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        blockFuture.cancel(true);
        try {
            ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
            fail("not cancelled: " + response);
        } catch (CancellationException good) {
            // good
        }
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockVerifyCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockCompletionCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.COMMIT);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.COMPLETING, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // The result may be success or cancelled depending on whether the cancelled executed while
        // DomainRolloutStepHandler.execute was running (CANCELLED) or afterwards (SUCCESS)
        assertSuccessOrCancelled(response);
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockRollbackCancelMaster() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.ROLLBACK);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        cancel(masterClient, "master", null, "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // Server doesn't get to DONE so doesn't report there, so master is waiting for initial report, which is blocking
        // on server. So master detects local cancellation and reports it
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }


    // Tests of cancelling an op blocking on a slave server via an op on the slave

    // NOTE: Not the way a user should cancel a server op -- either cancel it on the coordinating HC
    // (i.e. master for a domain-wide op) or on the server itself. Otherwise it's easy to mistakenly
    // cancel the master's call to the slave HC and not its proxied call to the server.
    // But we test this for completeness.

    @Test
    public void testSlaveServerBlockModelCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        // Here we must pass 'true' to the 'serverOpOnly' param to ensure we cancel the server op, and not the non-blocking HC op
        cancel(masterClient, "slave", null, "block", null, start, true);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockRuntimeCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        // Here we must pass 'true' to the 'serverOpOnly' param to ensure we cancel the server op, and not the non-blocking HC op
        cancel(masterClient, "slave", null, "block", null, start, true);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveServerBlockServiceStartCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start);
        // Here we must pass 'true' to the 'serverOpOnly' param to ensure we cancel the server op, and not the non-blocking HC op
        cancel(masterClient, "slave", null, "block", null, start, true);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockVerifyCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        // Here we must pass 'true' to the 'serverOpOnly' param to ensure we cancel the server op, and not the non-blocking HC op
        cancel(masterClient, "slave", null, "block", null, start, true);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockCompletionCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.COMMIT);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.COMPLETING, start);
        // Here we must pass 'true' to the 'serverOpOnly' param to ensure we cancel the server op, and not the non-blocking HC op
        cancel(masterClient, "slave", null, "block", null, start, true);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // The slave will already have sent its prepare phase response to master before the slave op even gets started.
        // So the subsequent cancellation will not affect the overall result, so it's SUCCESS
        assertEquals(response.asString(), SUCCESS, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }
    @Test
    public void testSlaveServerBlockRollbackCancelSlave() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.ROLLBACK);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        // Here we must pass 'true' to the 'serverOpOnly' param to ensure we cancel the server op, and not the non-blocking HC op
        cancel(masterClient, "slave", null, "block", null, start, true);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // Server can't report it's failure as a prepared message in DONE because it doesn't get there. So master is
        // waiting for the initial report. The slave then cancelling releases the initial report but doesn't change
        // its outcome from FAILED. So master sees failure
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testSlaveServerBlockModelCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.MODEL);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "slave", "main-three", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, false);
    }

    @Test
    public void testSlaveServerBlockRuntimeCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "slave", "main-three", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, false);
    }

    @Test
    @Ignore("MSC-143")
    public void testSlaveServerBlockServiceStartCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.SERVICE_START);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.AWAITING_STABILITY, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, false);
    }

    @Test
    public void testSlaveServerBlockVerifyCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.VERIFY);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "slave", "main-three", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, false);
    }

    @Test
    public void testSlaveServerBlockCompletionCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.COMMIT);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", null, start);
        cancel(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.COMPLETING, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // cancelling on the server during Stage.DONE should not result in a prepare-phase failure sent to master,
        // so result should always be SUCCESS
        assertEquals(response.asString(), SUCCESS, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, false);
    }

    @Test
    public void testSlaveServerBlockRollbackCancelServer() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.ROLLBACK);
        String id = findActiveOperation(masterClient, "slave", "main-three", "block", OperationContext.ExecutionStatus.ROLLING_BACK, start);
        cancel(masterClient, "slave", "main-three", "block", null, start, false);
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        // Cancelling in rollback doesn't change the server's FAILED outcome, so master sees it and reports it.
        assertEquals(response.asString(), FAILED, response.get(OUTCOME).asString());
        validateNoActiveOperation(masterClient, "slave", "main-three", id, false);
    }

    // Tests of helper methods

    @Test
    public void testCancelNonProgressingOperation() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "master", null, "block", null, start);
        ModelNode op = Util.createEmptyOperation("cancel-non-progressing-operation",
                PathAddress.pathAddress(PathElement.pathElement(HOST, "master")).append(MGMT_CONTROLLER));
        op.get("timeout").set(0);
        ModelNode result = executeForResult(op, masterClient);
        assertEquals(result.toString(), id, result.asString());
        ModelNode response = blockFuture.get(GET_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(response.asString(), CANCELLED, response.get(OUTCOME).asString());
        // Bogus as "id" is not the id on slave/main-three
//        validateNoActiveOperation(masterClient, "slave", "main-three", id, true);
    }

    @Test
    public void testFindNonProgressingOperation() throws Exception {
        long start = System.currentTimeMillis();
        Future<ModelNode> blockFuture = block("slave", "main-three", BlockerExtension.BlockPoint.RUNTIME);
        String id = findActiveOperation(masterClient, "master", null, "block", null, start);
        try {
            ModelNode op = Util.createEmptyOperation("find-non-progressing-operation",
                    PathAddress.pathAddress(PathElement.pathElement(HOST, "master")).append(MGMT_CONTROLLER));
            op.get("timeout").set(0);
            ModelNode result = executeForResult(op, masterClient);
            assertEquals(result.toString(), id, result.asString());
        } finally {
            try {
                blockFuture.cancel(true);
                validateNoActiveOperation(masterClient, "master", null, id, true);
            } catch (Exception toLog) {
                log.error("Failed to cancel in testFindNonProgressingOperation" , toLog);
            }
        }
    }

    private Future<ModelNode> block(String host, String server, BlockerExtension.BlockPoint blockPoint) {
        ModelNode op = BLOCK_OP.clone();
        op.get(TARGET_HOST.getName()).set(host);
        if (server != null) {
            op.get(TARGET_SERVER.getName()).set(server);
        }
        op.get(BLOCK_POINT.getName()).set(blockPoint.toString());
        op.get(CALLER.getName()).set(getTestMethod());
        return masterClient.executeAsync(op, OperationMessageHandler.DISCARD);
    }

    private void cancel(DomainClient client, String host, String server, String opName,
                           OperationContext.ExecutionStatus targetStatus, long executionStart, boolean serverOpOnly) throws Exception {
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(HOST, host));
        if (server != null) {
            address = address.append(PathElement.pathElement(SERVER, server));
        }
        address = address.append(MGMT_CONTROLLER);

        final String opToCancel = findActiveOperation(client, address, opName, targetStatus, executionStart, serverOpOnly);
        address = address.append(PathElement.pathElement(ACTIVE_OPERATION, opToCancel));

        ModelNode result = executeForResult(Util.createEmptyOperation("cancel", address), client);

        boolean retVal = result.asBoolean();
        assertTrue(result.asString(), retVal);
    }

    private String findActiveOperation(DomainClient client, String host, String server, String opName,
                                       OperationContext.ExecutionStatus targetStatus, long executionStart) throws Exception {
        PathAddress address = getManagementControllerAddress(host, server);
        return findActiveOperation(client, address, opName, targetStatus, executionStart, false);
    }

    private String findActiveOperation(DomainClient client, PathAddress address, String opName, OperationContext.ExecutionStatus targetStatus, long executionStart, boolean serverOpOnly) throws Exception {
        ModelNode op = Util.createEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, address);
        op.get(CHILD_TYPE).set(ACTIVE_OPERATION);
        long maxTime = TimeoutUtil.adjust(5000);
        long timeout = executionStart + maxTime;
        List<Property> activeOps = new ArrayList<Property>();
        String opToCancel = null;
        do {
            ModelNode result = executeForResult(op, client);
            if (result.isDefined()) {
                assertEquals(result.asString(), ModelType.OBJECT, result.getType());
                for (Property prop : result.asPropertyList()) {
                    if (prop.getValue().get(OP).asString().equals(opName)) {
                        PathAddress pa = PathAddress.pathAddress(prop.getValue().get(OP_ADDR));
                        if (!serverOpOnly || pa.size() > 2 && pa.getElement(1).getKey().equals(SERVER)) {
                            activeOps.add(prop);
                            if (targetStatus == null || prop.getValue().get(EXECUTION_STATUS).asString().equals(targetStatus.toString())) {
                                opToCancel = prop.getName();
                                break;
                            }
                        }
                    }
                }
            }
            if (opToCancel == null) {
                activeOps.clear();
                Thread.sleep(50);
            }

        } while ((opToCancel == null || activeOps.size() > 1) && System.currentTimeMillis() <= timeout);

        assertTrue(opName + " not present after " + maxTime + " ms", activeOps.size() > 0);
        assertEquals("Multiple instances of " + opName + " present: " + activeOps, 1, activeOps.size());
        assertNotNull(opName + " not in status " + targetStatus + " after " + maxTime + " ms", opToCancel);

        return opToCancel;
    }

    private String findActiveOperation(DomainClient client, PathAddress address, String opName) throws Exception {
        ModelNode op = Util.createEmptyOperation(READ_CHILDREN_RESOURCES_OPERATION, address);
        op.get(CHILD_TYPE).set(ACTIVE_OPERATION);
        ModelNode result = executeForResult(op, client);
        if (result.isDefined()) {
            assertEquals(result.asString(), ModelType.OBJECT, result.getType());
            for (Property prop : result.asPropertyList()) {
                if (prop.getValue().get(OP).asString().equals(opName)) {
                    return prop.getName();
                }
            }
        }
        return null;
    }

    private void validateNoActiveOperation(DomainClient client, String host, String server) throws Exception {

        PathAddress baseAddress = getManagementControllerAddress(host, server);

        // The op should clear w/in a few ms but we'll wait up to 5 secs just in case
        // something strange is happening on the machine is overloaded
        long timeout = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
        MgmtOperationException failure;
        do {
            String id = findActiveOperation(client, baseAddress, "block");
            if (id == null) {
                return;
            }
            failure = null;
            PathAddress address = baseAddress.append(PathElement.pathElement(ACTIVE_OPERATION, id));
            ModelNode op = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
            op.get(NAME).set(OP);
            try {
                executeForFailure(op, client);
            } catch (MgmtOperationException moe) {
                failure = moe;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() < timeout);

        throw failure;
    }

    private void validateNoActiveOperation(DomainClient client, String host, String server, String id,
                                           boolean patient) throws Exception {
        PathAddress address = getManagementControllerAddress(host, server);
        address = address.append(PathElement.pathElement(ACTIVE_OPERATION, id));
        ModelNode op = Util.createEmptyOperation(READ_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(OP);

        // The op should clear w/in a few ms but we'll wait up to 5 secs just in case
        // something strange is happening on the machine is overloaded
        long timeout = patient ? System.currentTimeMillis() + TimeoutUtil.adjust(5000) : 0;
        MgmtOperationException failure;
        do {
            try {
                executeForFailure(op, client);
                return;
            } catch (MgmtOperationException moe) {
                if (!patient) {
                    throw moe;
                }
                failure = moe;
            }
            Thread.sleep(50);
        } while (System.currentTimeMillis() < timeout);

        throw failure;
    }

    private static PathAddress getManagementControllerAddress(String host, String server) {
        PathAddress address = PathAddress.pathAddress(PathElement.pathElement(HOST, host));
        if (server != null) {
            address = address.append(PathElement.pathElement(SERVER, server));
        }
        address = address.append(MGMT_CONTROLLER);
        return address;
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

    private static ModelNode executeForFailure(final ModelNode op, final ModelControllerClient modelControllerClient) throws IOException, MgmtOperationException {
        try {
            return DomainTestUtils.executeForFailure(op, modelControllerClient);
        } catch (MgmtOperationException e) {
            System.out.println(" Op that incorrectly succeeded:");
            System.out.println(e.getOperation());
            throw e;
        }
    }

    private static String getTestMethod() {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement ste : stack) {
            String method = ste.getMethodName();
            if (method.startsWith("test")) {
                return method;
            }
        }
        return "unknown";
    }

    private static void assertFailedOrCancelled(ModelNode response) {
        assertXorCancelled(response, FAILED);
    }

    private static void assertSuccessOrCancelled(ModelNode response) {
        assertXorCancelled(response, SUCCESS);
    }

    private static void assertXorCancelled(ModelNode response, String x) {
        String outcome = response.get(OUTCOME).asString();
        if (!CANCELLED.equals(outcome)) {
            assertEquals(response.asString(), x, response.get(OUTCOME).asString());
        }

    }
}
