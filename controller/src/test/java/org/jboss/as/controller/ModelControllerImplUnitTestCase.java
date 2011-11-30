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

/**
 *
 */
package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_UPDATE_SKIPPED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.NullConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests of {@link ModelControllerImpl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ModelControllerImplUnitTestCase {

    protected static boolean useNonRecursive;

    private ServiceContainer container;
    private ModelController controller;
    private AtomicBoolean sharedState;

    public static void toggleRuntimeState(AtomicBoolean state) {
        boolean runtimeVal = false;
        while (!state.compareAndSet(runtimeVal, !runtimeVal)) {
            runtimeVal = !runtimeVal;
        }
    }

    @Before
    public void setupController() throws InterruptedException {

        // restore default
        useNonRecursive = false;

        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        ControlledProcessState processState = new ControlledProcessState(true);
        ModelControllerService svc = new ModelControllerService(processState);
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), svc);
        builder.install();
        sharedState = svc.state;
        svc.latch.await();
        controller = svc.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);
        processState.setRunning();
    }

    @After
    public void shutdownServiceContainer() {
        if (container != null) {
            container.shutdown();
            try {
                container.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finally {
                container = null;
            }
        }
    }

    public static class ModelControllerService extends AbstractControllerService {

        final AtomicBoolean state = new AtomicBoolean(true);
        final CountDownLatch latch = new CountDownLatch(1);

        ModelControllerService(final ControlledProcessState processState) {
            this(processState, new NullConfigurationPersister());
        }

        ModelControllerService(final ControlledProcessState processState, final ConfigurationPersister configurationPersister) {
            super(ProcessType.EMBEDDED_SERVER, new RunningModeControl(RunningMode.NORMAL), configurationPersister, processState, DESC_PROVIDER, null, ExpressionResolver.DEFAULT);
        }

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {

            rootRegistration.registerOperationHandler("setup", new SetupHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("composite", CompositeOperationHandler.INSTANCE, DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("good", new ModelStageGoodHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("bad", new ModelStageFailsHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("evil", new ModelStageThrowsExceptionHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("handleFailed", new RuntimeStageFailsHandler(state), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("runtimeException", new RuntimeStageThrowsExceptionHandler(state), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("operationFailedException", new RuntimeStageThrowsOFEHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("good-service", new GoodServiceHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("bad-service", new BadServiceHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("missing-service", new MissingServiceHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("reload-required", new ReloadRequiredHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("restart-required", new RestartRequiredHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("dependent-service", new DependentServiceHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("remove-dependent-service", new RemoveDependentServiceHandler(), DESC_PROVIDER, false);

            rootRegistration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true);
            rootRegistration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true);
            rootRegistration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

            rootRegistration.registerSubModel(PathElement.pathElement("child"), DESC_PROVIDER);
        }

        @Override
        protected void finishBoot() throws ConfigurationPersistenceException {
            super.finishBoot();
            latch.countDown();
        }
    }


    @Test
    public void testGoodModelExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("good", "attr1", 5), null, null, null);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(1, result.get("result").asInt());
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(5, result.get(RESULT).asInt());
    }

    @Test
    public void testGoodModelExecutionNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodModelExecution();
    }

    /**
     * Test successfully updating the model but then having the caller roll back the transaction.
     */
    @Test
    public void testGoodModelExecutionTxRollback() {
        ModelNode result = controller.execute(getOperation("good", "attr1", 5), null, RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        // Store response data for later assertions after we check more critical stuff
        String outcome = result.get(OUTCOME).asString();
        boolean rolledback = result.get(ROLLED_BACK).asBoolean();

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(1, result.get(RESULT).asInt());

        // Assert the first response was as expected
        assertEquals(FAILED, outcome);  // TODO success may be valid???
        assertTrue(rolledback);
    }

    @Test
    public void testGoodModelExecutionTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodModelExecutionTxRollback();
    }

    @Test
    public void testModelStageFailureExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("bad", "attr1", 5), null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("this request is bad", result.get(FAILURE_DESCRIPTION).asString());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertEquals(1, result.get(RESULT).asInt());
    }

    @Test
    public void testModelStageFailureExecutionNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageFailureExecution();
    }

    @Test
    public void testModelStageUnhandledFailureExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("evil", "attr1", 5), null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(FAILURE_DESCRIPTION).toString().contains("this handler is evil"));

        // Confirm runtime state was unchanged
        assertTrue(sharedState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testModelStageUnhandledFailureExecutionNonRecursive() throws Exception {
        useNonRecursive = true;
        testModelStageUnhandledFailureExecution();
    }

    @Test
    public void testHandleFailedExecution() throws Exception {
        ModelNode result = controller.execute(getOperation("handleFailed", "attr1", 5, "good", false), null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("handleFailed", result.get("failure-description").asString());

        // Confirm runtime state was unchanged
        assertTrue(sharedState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testHandleFailedExecutionNonRecursive() throws Exception {
        useNonRecursive = true;
        testHandleFailedExecution();
    }

    @Test
    public void testRuntimeStageFailedNoRollback() throws Exception {

        ModelNode op = getOperation("handleFailed", "attr1", 5, "good");
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("handleFailed", result.get("failure-description").asString());

        // Confirm runtime state was changed
        assertFalse(sharedState.get());

        // Confirm model was changed
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testRuntimeStageFailedNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRuntimeStageFailedNoRollback();
    }

    @Test
    public void testRuntimeStageUnhandledFailureNoRollback() throws Exception {

        ModelNode op = getOperation("runtimeException", "attr1", 5, "good");
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().contains("runtime exception"));

        // Confirm runtime state was changed (handler changes it and throws exception, does not fix state)
        assertFalse(sharedState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testRuntimeStageUnhandledFailureNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRuntimeStageUnhandledFailureNoRollback();
    }

    @Test
    public void testOperationFailedExceptionNoRollback() throws Exception {

        ModelNode op = getOperation("operationFailedException", "attr1", 5, "good");
        op.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
        ModelNode result = controller.execute(op, null, null, null);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get("failure-description").toString().contains("OFE"));

        // Confirm model was changed
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        System.out.println(result);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testOperationFailedExceptionNoRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testOperationFailedExceptionNoRollback();
    }

    @Test
    public void testPathologicalRollback() throws Exception {
        ModelNode result = controller.execute(getOperation("bad", "attr1", 5), null, null, null); // don't tell it to call the 'good' op on rollback
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertEquals("this request is bad", result.get("failure-description").asString());

        // Confirm runtime state was unchanged
        assertTrue(sharedState.get());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testPathologicalRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testPathologicalRollback();
    }

    @Test
    public void testGoodService() throws Exception {
        ModelNode result = controller.execute(getOperation("good-service", "attr1", 5), null, null, null);
        System.out.println(result);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        assertNotNull(sc);
        assertEquals(ServiceController.State.UP, sc.getState());

        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testGoodServiceNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodService();
    }

    @Test
    public void testGoodServiceTxRollback() throws Exception {
        ModelNode result = controller.execute(getOperation("good-service", "attr1", 5), null, RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        // Store response data for later assertions after we check more critical stuff

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("good-service"));
        if (sc != null) {
            assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        // Confirm model was unchanged
        ModelNode result2 = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals(SUCCESS, result2.get(OUTCOME).asString());
        assertEquals(1, result2.get(RESULT).asInt());

        // Assert the first response was as expected
        assertEquals(FAILED, result.get(OUTCOME).asString());   // TODO success may be valid???
        assertTrue(result.get(ROLLED_BACK).asBoolean());
    }

    @Test
    public void testGoodServiceTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testGoodServiceTxRollback();
    }

    @Test
    public void testBadService() throws Exception {
        ModelNode result = controller.execute(getOperation("bad-service", "attr1", 5, "good"), null, null, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("bad-service"));
        if (sc != null) {
            assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testBadServiceNonRecursive() throws Exception {
        useNonRecursive = true;
        testBadService();
    }

    @Test
    public void testMissingService() throws Exception {
        ModelNode result = controller.execute(getOperation("missing-service", "attr1", 5, "good"), null, null, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("missing-service"));
        if (sc != null) {
            assertEquals(ServiceController.Mode.REMOVE, sc.getMode());
        }

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testMissingServiceNonRecursive() throws Exception {
        useNonRecursive = true;
        testMissingService();
    }

    @Test
    public void testGlobal() throws Exception {

        ModelNode operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(RECURSIVE).set(false);

        ModelNode result = controller.execute(operation, null, null, null);
        assertTrue(result.get("result").hasDefined("child"));
        assertTrue(result.get("result", "child").has("one"));
        assertFalse(result.get("result", "child").hasDefined("one"));

        operation = new ModelNode();
        operation.get(OP).set(READ_RESOURCE_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(RECURSIVE).set(true);

        result = controller.execute(operation, null, null, null);
        assertTrue(result.get("result", "child", "one").hasDefined("attribute1"));

        operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(CHILD_TYPE).set("child");

        result = controller.execute(operation, null, null, null).get("result");
        assertEquals("one", result.get(0).asString());
        assertEquals("two", result.get(1).asString());

        operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_TYPES_OPERATION);
        operation.get(OP_ADDR).setEmptyList();

        result = controller.execute(operation, null, null, null).get("result");
        assertEquals("child", result.get(0).asString());

        operation = new ModelNode();
        operation.get(OP).set(READ_CHILDREN_RESOURCES_OPERATION);
        operation.get(OP_ADDR).setEmptyList();
        operation.get(CHILD_TYPE).set("child");
    }

    @Test
    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testReloadRequired() throws Exception {
        ModelNode result = controller.execute(getOperation("reload-required", "attr1", 5), null, null, null);
        System.out.println(result);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertTrue(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean());
        assertEquals(ControlledProcessState.State.RELOAD_REQUIRED.toString(), result.get(RESPONSE_HEADERS, PROCESS_STATE).asString());

        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testReloadRequiredNonRecursive() throws Exception {
        useNonRecursive = true;
        testReloadRequired();
    }

    @Test
    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testRestartRequired() throws Exception {
        ModelNode result = controller.execute(getOperation("restart-required", "attr1", 5), null, null, null);
        System.out.println(result);
        assertEquals(SUCCESS, result.get(OUTCOME).asString());
        assertTrue(result.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertTrue(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RESTART).asBoolean());
        assertEquals(ControlledProcessState.State.RESTART_REQUIRED.toString(), result.get(RESPONSE_HEADERS, PROCESS_STATE).asString());

        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    @Ignore("AS7-1103 Fails intermittently for unknown reasons")
    public void testRestartRequiredNonRecursive() throws Exception {
        useNonRecursive = true;
        testRestartRequired();
    }

    @Test
    public void testReloadRequiredTxRollback() throws Exception {
        ModelNode result = controller.execute(getOperation("reload-required", "attr1", 5), null, RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertFalse(result.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RELOAD));
        assertFalse(result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE));

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testReloadRequiredTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testReloadRequiredTxRollback();
    }

    @Test
    public void testRestartRequiredTxRollback() throws Exception {
        ModelNode result = controller.execute(getOperation("restart-required", "attr1", 5), null, RollbackTransactionControl.INSTANCE, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).asBoolean());
        assertFalse(result.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RESTART));
        assertFalse(result.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE));

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());
    }

    @Test
    public void testRestartRequiredTxRollbackNonRecursive() throws Exception {
        useNonRecursive = true;
        testRestartRequiredTxRollback();
    }

    @Test
    public void testRemoveDependentService() throws Exception {
        ModelNode result = controller.execute(getOperation("dependent-service", "attr1", 5), null, null, null);
        System.out.println(result);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(1, result.get("result").asInt());

        ServiceController<?> sc = container.getService(ServiceName.JBOSS.append("depended-service"));
        assertNotNull(sc);
        assertEquals(ServiceController.State.UP, sc.getState());

        sc = container.getService(ServiceName.JBOSS.append("dependent-service"));
        assertNotNull(sc);
        assertEquals(ServiceController.State.UP, sc.getState());

        result = controller.execute(getOperation("remove-dependent-service", "attr1", 6, "good"), null, null, null);
        System.out.println(result);
        assertEquals(FAILED, result.get(OUTCOME).asString());
        assertTrue(result.hasDefined(FAILURE_DESCRIPTION));

        sc = container.getService(ServiceName.JBOSS.append("depended-service"));
        assertNotNull(sc);
        assertEquals(ServiceController.State.UP, sc.getState());

        sc = container.getService(ServiceName.JBOSS.append("dependent-service"));
        assertNotNull(sc);
        assertEquals(ServiceController.State.UP, sc.getState());

        // Confirm model was unchanged
        result = controller.execute(getOperation("good", "attr1", 1), null, null, null);
        assertEquals("success", result.get("outcome").asString());
        assertEquals(5, result.get("result").asInt());
    }

    @Test
    public void testRemoveDependentNonRecursive() throws Exception {
        useNonRecursive = true;
        testRemoveDependentService();
    }

    public static ModelNode getOperation(String opName, String attr, int val) {
        return getOperation(opName, attr, val, null, false);
    }
    public static ModelNode getOperation(String opName, String attr, int val, String rollbackName) {
        return getOperation(opName, attr, val, rollbackName, false);
    }

    public static ModelNode getOperation(String opName, String attr, int val, String rollbackName, boolean async) {
        ModelNode op = new ModelNode();
        op.get(OP).set(opName);
        op.get(OP_ADDR).setEmptyList();
        op.get(NAME).set(attr);
        op.get(VALUE).set(val);
        op.get("rollbackName").set(rollbackName == null ? opName : rollbackName);

        if (async) {
            op.get("async").set(true);
        }
        return op;
    }

    public static class SetupHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) {
            ModelNode model = new ModelNode();

            //Atttributes
            model.get("attr1").set(1);
            model.get("attr2").set(2);

            context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().set(model);

            final ModelNode child1 = new ModelNode();
            child1.get("attribute1").set(1);
            final ModelNode child2 = new ModelNode();
            child2.get("attribute2").set(2);

            context.createResource(PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement("child", "one"))).getModel().set(child1);
            context.createResource(PathAddress.EMPTY_ADDRESS.append(PathElement.pathElement("child", "two"))).getModel().set(child2);

            context.completeStep();
        }
    }

    public static class ModelStageGoodHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require(NAME).asString();
            ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            ModelNode attr = model.get(name);
            final int current = attr.asInt();
            attr.set(operation.require(VALUE));

            context.getResult().set(current);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class ModelStageFailsHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require(NAME).asString();
            ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            ModelNode attr = model.get(name);
            attr.set(operation.require(VALUE));

            context.getFailureDescription().set("this request is bad");

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class ModelStageThrowsExceptionHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require(NAME).asString();
            ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            ModelNode attr = model.get(name);
            attr.set(operation.require(VALUE));

            throw new RuntimeException("this handler is evil");
        }
    }

    public static class RuntimeStageFailsHandler implements OperationStepHandler {

        private final AtomicBoolean state;

        public RuntimeStageFailsHandler(AtomicBoolean state) {
            this.state = state;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));

            context.getResult().set(current);

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) {
                    toggleRuntimeState(state);
                    context.getFailureDescription().set("handleFailed");
                    if (useNonRecursive) {
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                toggleRuntimeState(state);
                            }
                        });
                    }
                    else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        toggleRuntimeState(state);
                    }
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class RuntimeStageThrowsExceptionHandler implements OperationStepHandler {

        private final AtomicBoolean state;

        public RuntimeStageThrowsExceptionHandler(AtomicBoolean state) {
            this.state = state;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));

            context.getResult().set(current);

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) {
                    toggleRuntimeState(state);
                    throw new RuntimeException("runtime exception");
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class RuntimeStageThrowsOFEHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            int current = attr.asInt();
            attr.set(operation.require("value"));

            context.getResult().set(current);

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    throw new OperationFailedException(new ModelNode().set("OFE"));
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class GoodServiceHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            final int current = attr.asInt();
            attr.set(operation.require("value"));

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, ModelNode operation) {

                    context.getResult().set(current);
                    final ServiceName svcName =  ServiceName.JBOSS.append("good-service");
                    ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    context.getServiceTarget().addService(svcName, Service.NULL)
                            .addListener(verificationHandler)
                            .install();
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    if (useNonRecursive) {
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                context.removeService(svcName);
                            }
                        });
                    } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(svcName);
                    }
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class MissingServiceHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            final int current = attr.asInt();
            attr.set(operation.require("value"));

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, ModelNode operation) {

                    context.getResult().set(current);

                    final ServiceName svcName = ServiceName.JBOSS.append("missing-service");
                    ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    context.getServiceTarget().addService(svcName, Service.NULL)
                            .addDependency(ServiceName.JBOSS.append("missing"))
                            .addListener(verificationHandler)
                            .install();
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    if (useNonRecursive) {
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                context.removeService(svcName);
                            }
                        });
                    } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(svcName);
                    }
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class BadServiceHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            final int current = attr.asInt();
            attr.set(operation.require("value"));

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, ModelNode operation) {

                    context.getResult().set(current);


                    Service<Void> bad = new Service<Void>() {

                        @Override
                        public Void getValue() throws IllegalStateException, IllegalArgumentException {
                            return null;
                        }

                        @Override
                        public void start(StartContext context) throws StartException {
                            throw new RuntimeException("Bad service!");
                        }

                        @Override
                        public void stop(StopContext context) {
                        }

                    };
                    final ServiceName svcName = ServiceName.JBOSS.append("bad-service");
                    ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    context.getServiceTarget().addService(svcName, bad)
                            .addListener(verificationHandler)
                            .install();
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    if (useNonRecursive) {
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                context.removeService(svcName);
                            }
                        });
                    } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(svcName);
                    }
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class ReloadRequiredHandler implements OperationStepHandler {
        @Override
        public void execute(final OperationContext context, ModelNode operation) {

            String name = operation.require(NAME).asString();
            ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            ModelNode attr = model.get(name);
            final int current = attr.asInt();
            attr.set(operation.require(VALUE));

            context.getResult().set(current);

            context.runtimeUpdateSkipped();
            context.reloadRequired();
            if (useNonRecursive) {
                context.completeStep(new OperationContext.RollbackHandler() {
                    @Override
                    public void handleRollback(OperationContext context, ModelNode operation) {
                        context.revertReloadRequired();
                    }
                });
            } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                context.revertReloadRequired();
            }
        }
    }

    public static class RestartRequiredHandler implements OperationStepHandler {
        @Override
        public void execute(final OperationContext context, ModelNode operation) {

            String name = operation.require(NAME).asString();
            ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
            ModelNode attr = model.get(name);
            final int current = attr.asInt();
            attr.set(operation.require(VALUE));

            context.getResult().set(current);

            context.runtimeUpdateSkipped();
            context.restartRequired();
            if (useNonRecursive) {
                context.completeStep(new OperationContext.RollbackHandler() {
                    @Override
                    public void handleRollback(OperationContext context, ModelNode operation) {
                        context.revertRestartRequired();
                    }
                });
            } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                context.revertRestartRequired();
            }
        }
    }

    public static class DependentServiceHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            final int current = attr.asInt();
            attr.set(operation.require("value"));

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, ModelNode operation) {

                    context.getResult().set(current);
                    ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    final ServiceName dependedSvcName = ServiceName.JBOSS.append("depended-service");
                    context.getServiceTarget().addService(dependedSvcName, Service.NULL)
                            .addListener(verificationHandler)
                            .install();
                    final ServiceName dependentSvcName = ServiceName.JBOSS.append("dependent-service");
                    context.getServiceTarget().addService(dependentSvcName, Service.NULL)
                            .addDependencies(dependedSvcName)
                            .addListener(verificationHandler)
                            .install();
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    if (useNonRecursive) {
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                context.removeService(dependedSvcName);
                                context.removeService(dependentSvcName);
                            }
                        });
                    } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.removeService(dependedSvcName);
                        context.removeService(dependentSvcName);
                    }
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static class RemoveDependentServiceHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) {

            String name = operation.require("name").asString();
            ModelNode attr = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel().get(name);
            final int current = attr.asInt();
            attr.set(operation.require("value"));

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, ModelNode operation) {

                    context.getResult().set(current);
                    final ServiceName dependedSvcName = ServiceName.JBOSS.append("depended-service");
                    context.removeService(dependedSvcName);
                    if (useNonRecursive) {
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                            context.getServiceTarget().addService(dependedSvcName, Service.NULL)
                                    .install();
                            }
                        });
                    } else if (context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                        context.getServiceTarget().addService(dependedSvcName, Service.NULL)
                                .install();
                    }
                }
            }, OperationContext.Stage.RUNTIME);

            if (useNonRecursive) {
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            } else {
                context.completeStep();
            }
        }
    }

    public static final DescriptionProvider DESC_PROVIDER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode();
        }
    };

    static class RollbackTransactionControl implements ModelController.OperationTransactionControl {

        static final RollbackTransactionControl INSTANCE = new RollbackTransactionControl();

        @Override
        public void operationPrepared(ModelController.OperationTransaction transaction, ModelNode result) {
            transaction.rollback();
        }
    }
}
