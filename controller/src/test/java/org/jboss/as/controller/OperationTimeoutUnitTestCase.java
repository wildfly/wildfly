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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.BLOCKING_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of ability to time out operations. WFLY-2741.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class OperationTimeoutUnitTestCase {

    private static final Executor executor = Executors.newCachedThreadPool();

    private static CountDownLatch blockObject;
    private static CountDownLatch latch;
    private ServiceContainer container;
    private ModelControllerService controllerService;
    private ModelControllerClient client;

    @Before
    public void setupController() throws InterruptedException {

        // restore default
        blockObject = new CountDownLatch(1);
        latch = new CountDownLatch(1);

        System.out.println("=========  New Test \n");
        container = ServiceContainer.Factory.create("test");
        ServiceTarget target = container.subTarget();
        controllerService = new ModelControllerService();
        ServiceBuilder<ModelController> builder = target.addService(ServiceName.of("ModelController"), controllerService);
        builder.install();
        controllerService.awaitStartup(30, TimeUnit.SECONDS);
        ModelController controller = controllerService.getValue();
        ModelNode setup = Util.getEmptyOperation("setup", new ModelNode());
        controller.execute(setup, null, null, null);

        client = controller.createClient(executor);

        System.setProperty(BlockingTimeout.SYSTEM_PROPERTY, "1");
    }

    @After
    public void shutdownServiceContainer() {

        System.clearProperty(BlockingTimeout.SYSTEM_PROPERTY);

        releaseBlockingThreads();

        if (client != null) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

    @Test
    public void testOperationHeaderConfig() throws InterruptedException, ExecutionException, TimeoutException {
        blockInVerifyTest(new ModelNode(1));
    }

    @Test
    public void testBlockInVerify() throws InterruptedException, ExecutionException, TimeoutException {
        blockInVerifyTest(null);
    }

    private void blockInVerifyTest(ModelNode header) throws InterruptedException, ExecutionException, TimeoutException {
        ModelNode op = Util.createEmptyOperation("block", null);
        op.get("start").set(true);
        if (header != null) {
            op.get(OPERATION_HEADERS, BLOCKING_TIMEOUT).set(1);
            System.setProperty(BlockingTimeout.SYSTEM_PROPERTY, "300");
        }

        Future<ModelNode> future = client.executeAsync(op, null);

        ModelNode response = future.get(20, TimeUnit.SECONDS);
        assertEquals(response.toString(), FAILED, response.get(OUTCOME).asString());
        assertTrue(response.toString(), response.get(FAILURE_DESCRIPTION).asString().contains(ControllerLogger.MGMT_OP_LOGGER.timeoutExecutingOperation()));

        assertEquals(ControlledProcessState.State.RESTART_REQUIRED, controllerService.getCurrentProcessState());
    }

    @Test
    public void testBlockInRollback() throws InterruptedException, ExecutionException, TimeoutException {
        ModelNode op = Util.createEmptyOperation("block", null);
        op.get("fail").set(true);
        op.get("stop").set(true);

        Future<ModelNode> future = client.executeAsync(op, null);

        ModelNode response = future.get(20, TimeUnit.SECONDS);
        assertEquals(response.toString(), FAILED, response.get(OUTCOME).asString());
        assertTrue(response.toString(), response.get(FAILURE_DESCRIPTION).asString().contains("failfailfail"));

        assertEquals(ControlledProcessState.State.RESTART_REQUIRED, controllerService.getCurrentProcessState());
    }

    @Test
    public void testBlockInBoth() throws InterruptedException, ExecutionException, TimeoutException {
        ModelNode op = Util.createEmptyOperation("block", null);
        op.get("start").set(true);
        op.get("stop").set(true);

        Future<ModelNode> future = client.executeAsync(op, null);

        ModelNode response = future.get(20, TimeUnit.SECONDS);
        assertEquals(response.toString(), FAILED, response.get(OUTCOME).asString());
        assertTrue(response.toString(), response.get(FAILURE_DESCRIPTION).asString().contains(ControllerLogger.MGMT_OP_LOGGER.timeoutExecutingOperation()));

        assertEquals(ControlledProcessState.State.RESTART_REQUIRED, controllerService.getCurrentProcessState());
    }

    @Test
    public void testBlockAwaitingRuntimeLock() throws InterruptedException, ExecutionException, TimeoutException {

        // Get the service container fubar
        blockInVerifyTest(null);

        ModelNode op = Util.createEmptyOperation("block", null);
        op.get("start").set(false);
        op.get("stop").set(false);

        Future<ModelNode> future = client.executeAsync(op, null);

        ModelNode response = future.get(20, TimeUnit.SECONDS);
        assertEquals(response.toString(), FAILED, response.get(OUTCOME).asString());
        assertTrue(response.toString(), response.get(FAILURE_DESCRIPTION).asString().contains(ControllerLogger.MGMT_OP_LOGGER.timeoutAwaitingInitialStability()));

        assertEquals(ControlledProcessState.State.RESTART_REQUIRED, controllerService.getCurrentProcessState());
    }

    @Test
    public void testRepairInRollback() throws InterruptedException, ExecutionException, TimeoutException {
        ModelNode op = Util.createEmptyOperation("block", null);
        op.get("fail").set(true);
        op.get("start").set(true);
        op.get("repair").set(true);

        Future<ModelNode> future = client.executeAsync(op, null);

        ModelNode response = future.get(20, TimeUnit.SECONDS);
        assertEquals(response.toString(), FAILED, response.get(OUTCOME).asString());
        assertTrue(response.toString(), response.get(FAILURE_DESCRIPTION).asString().contains("failfailfail"));

        // We should be in RUNNING state because stability could be obtained before the op completed
        assertEquals(ControlledProcessState.State.RUNNING, controllerService.getCurrentProcessState());
    }


    private static void releaseBlockingThreads() {
        if (blockObject != null) {
            blockObject.countDown();
        }
    }

    private static void block() {
        latch.countDown();
        try {
            blockObject.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class ModelControllerService extends TestModelControllerService {

        @Override
        protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {

            rootRegistration.registerOperationHandler("setup", new SetupHandler(), DESC_PROVIDER, false);
            rootRegistration.registerOperationHandler("block", new BlockingServiceHandler(), DESC_PROVIDER, false);

            GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        }
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

            context.stepCompleted();
        }
    }

    public static class BlockingServiceHandler implements OperationStepHandler {
        @Override
        public void execute(OperationContext context, ModelNode operation) {

            context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);

            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, ModelNode operation) {

                    boolean start = operation.get("start").asBoolean(false);
                    boolean stop = operation.get("stop").asBoolean(false);
                    boolean fail = operation.get("fail").asBoolean(false);
                    final boolean repair = fail && operation.get("repair").asBoolean(false);

                    BlockingService bad = start ? (stop ? BlockingService.BOTH : BlockingService.START)
                                                : (stop ? BlockingService.STOP : BlockingService.NEITHER);

                    final ServiceName svcName = ServiceName.JBOSS.append("bad-service");
                    ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    context.getServiceTarget().addService(svcName, bad)
                            .addListener(verificationHandler)
                            .install();
                    context.addStep(verificationHandler, OperationContext.Stage.VERIFY);

                    try {
                        bad.startLatch.await(20, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    if (fail) {
                        context.setRollbackOnly();
                        context.getFailureDescription().set("failfailfail");
                    }
                    context.completeStep(new OperationContext.ResultHandler() {

                        @Override
                        public void handleResult(OperationContext.ResultAction resultAction, OperationContext context, ModelNode operation) {
                            if (repair) {
                                releaseBlockingThreads();
                            }
                            context.removeService(svcName);
                        }
                    });

                }
            }, OperationContext.Stage.RUNTIME);

            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }

    private static class BlockingService implements Service<Void> {

        private static final BlockingService START = new BlockingService(true, false);
        private static final BlockingService STOP = new BlockingService(false, true);
        private static final BlockingService BOTH = new BlockingService(true, true);
        private static final BlockingService NEITHER = new BlockingService(false, false);

        private final boolean start;
        private final boolean stop;
        private final CountDownLatch startLatch = new CountDownLatch(1);

        private BlockingService(boolean start, boolean stop) {
            this.start = start;
            this.stop = stop;
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }

        @Override
        public void start(StartContext context) throws StartException {
            startLatch.countDown();
            if (start) {
                block();
            }
        }

        @Override
        public void stop(StopContext context) {
            if (stop) {
                block();
            }
        }
    }
}
