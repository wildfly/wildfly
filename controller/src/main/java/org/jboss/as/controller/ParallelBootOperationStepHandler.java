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

package org.jboss.as.controller;

import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Special handler that executes subsystem boot operations in parallel.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParallelBootOperationStepHandler implements OperationStepHandler {

    private final Executor executor;
    private final ImmutableManagementResourceRegistration rootRegistration;
    private final ControlledProcessState processState;

    private final Map<String, List<ParsedBootOp>> opsBySubsystem = new LinkedHashMap<String, List<ParsedBootOp>>();

    ParallelBootOperationStepHandler(final ExecutorService executorService, final ImmutableManagementResourceRegistration rootRegistration,
                                     final ControlledProcessState processState) {
        this.executor = executorService;
        this.rootRegistration = rootRegistration;
        this.processState = processState;
    }

    boolean addSubsystemOperation(final ParsedBootOp parsedOp) {
        final String subsystemName = getSubsystemName(parsedOp.address);
        if (subsystemName != null) {
            List<ParsedBootOp> list = opsBySubsystem.get(subsystemName);
            if (list == null) {
                list = new ArrayList<ParsedBootOp>();
                opsBySubsystem.put(subsystemName, list);
            }
            list.add(parsedOp);
        }
        return subsystemName != null;
    }

    private String getSubsystemName(final PathAddress address) {
        String key = null;
        if (address.size() > 0 && ModelDescriptionConstants.SUBSYSTEM.equals(address.getElement(0).getKey())) {
            key = address.getElement(0).getValue();
        }
        return key;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (context.getType() != OperationContext.Type.SERVER) {
            throw MESSAGES.fullServerBootRequired(getClass());
        }

        long start = System.currentTimeMillis();

        // Make sure the lock has been taken
        context.getResourceRegistrationForUpdate();
        final Resource rootResource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        context.acquireControllerLock();

        final Map<String, List<ParsedBootOp>> runtimeOpsBySubsystem = new LinkedHashMap<String, List<ParsedBootOp>>();
        final Map<String, ParallelBootTransactionControl> transactionControls = new LinkedHashMap<String, ParallelBootTransactionControl>();

        final CountDownLatch preparedLatch = new CountDownLatch(opsBySubsystem.size());
        final CountDownLatch committedLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(opsBySubsystem.size());
        final Thread controllingThread = Thread.currentThread();

        for (Map.Entry<String, List<ParsedBootOp>> entry : opsBySubsystem.entrySet()) {
            String subsystemName = entry.getKey();
            List<ParsedBootOp> subsystemRuntimeOps = new ArrayList<ParsedBootOp>();
            runtimeOpsBySubsystem.put(subsystemName, subsystemRuntimeOps);

            final ParallelBootTransactionControl txControl = new ParallelBootTransactionControl(subsystemName, preparedLatch, committedLatch, completeLatch);
            transactionControls.put(entry.getKey(), txControl);

            // Execute the subsystem's ops in another thread
            ParallelBootTask subsystemTask = new ParallelBootTask(subsystemName, entry.getValue(), context, txControl, subsystemRuntimeOps, controllingThread);
            executor.execute(subsystemTask);
        }

        // Wait for all subsystem ops to complete
        try {
            preparedLatch.await();

            // See if all subsystems succeeded; if not report a failure to context
            checkForSubsystemFailures(context, transactionControls, OperationContext.Stage.MODEL);

            // Add any logging subsystem steps so we get logging early in the boot
            List<ParsedBootOp> loggingOps = runtimeOpsBySubsystem.remove("logging");
            if (loggingOps != null) {
                for (ParsedBootOp loggingOp : loggingOps) {
                    context.addStep(loggingOp.response, loggingOp.operation, loggingOp.handler, OperationContext.Stage.RUNTIME);
                }
            }

            // AS7-2561
            // The parallel execution will have added the subsystems to their parent resource in random order.
            // We need to restore the order that came in the XML.
            final Map<String, Resource> subsystemResources = new LinkedHashMap<String, Resource>();
            for (String subsystemName : opsBySubsystem.keySet()) {
                final Resource resource = rootResource.removeChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, subsystemName));
                if (resource != null) {
                    subsystemResources.put(subsystemName, resource);
                }
            }
            for (Map.Entry<String, Resource> entry : subsystemResources.entrySet()) {
                rootResource.registerChild(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, entry.getKey()), entry.getValue());
            }

            // Add step to execute all the runtime ops recorded by the other subsystem tasks
            context.addStep(getRuntimeStep(runtimeOpsBySubsystem), OperationContext.Stage.RUNTIME);

        } catch (InterruptedException e) {
            context.getFailureDescription().set(new ModelNode().set(MESSAGES.subsystemBootInterrupted()));
            Thread.currentThread().interrupt();
        }

        if (MGMT_OP_LOGGER.isDebugEnabled()) {
            long elapsed = System.currentTimeMillis() - start;
            MGMT_OP_LOGGER.debugf("Ran subsystem model operations in [%d] ms", elapsed);
        }

        // Continue boot
        OperationContext.ResultAction resultAction = context.completeStep();

        // Tell all the subsystem tasks the result of the operations
        notifySubsystemTransactions(transactionControls, resultAction, committedLatch, OperationContext.Stage.RUNTIME);

        // Make sure all the subsystems have completed the out path before we return
        try {
            completeLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

    }

    private void checkForSubsystemFailures(OperationContext context, Map<String, ParallelBootTransactionControl> transactionControls, OperationContext.Stage stage) {
        boolean failureRecorded = false;
        for (Map.Entry<String, ParallelBootTransactionControl> entry : transactionControls.entrySet()) {
            ParallelBootTransactionControl txControl = entry.getValue();
            if (txControl.transaction == null) {
                String failureDesc;
                if (txControl.response.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION)) {
                    failureDesc = txControl.response.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString();
                } else {
                    failureDesc = MESSAGES.subsystemBootOperationFailed(entry.getKey());
                }
                MGMT_OP_LOGGER.error(failureDesc);
                if (!failureRecorded) {
                    context.getFailureDescription().set(failureDesc);
                    failureRecorded = true;
                }
            } else {
                MGMT_OP_LOGGER.debugf("Stage %s boot ops for subsystem %s succeeded", stage, entry.getKey());
            }
        }
    }

    private void notifySubsystemTransactions(final Map<String, ParallelBootTransactionControl> transactionControls,
                                             final OperationContext.ResultAction resultAction,
                                             final CountDownLatch committedLatch,
                                             final OperationContext.Stage stage) {
        for (Map.Entry<String, ParallelBootTransactionControl> entry : transactionControls.entrySet()) {
            ParallelBootTransactionControl txControl = entry.getValue();
            if (txControl.transaction != null) {
                if (resultAction == OperationContext.ResultAction.KEEP) {
                    txControl.transaction.commit();
                    MGMT_OP_LOGGER.debugf("Committed transaction for %s subsystem %s stage boot operations", entry.getKey(), stage);
                } else {
                    txControl.transaction.rollback();
                    MGMT_OP_LOGGER.debugf("Rolled back transaction for %s subsystem %s stage boot operations", entry.getKey(), stage);
                }
            }
        }
        committedLatch.countDown();
    }

    private OperationStepHandler getRuntimeStep(final Map<String, List<ParsedBootOp>> runtimeOpsBySubsystem) {

        return new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

                long start = System.currentTimeMillis();
                // make sure the registry lock is held
                context.getServiceRegistry(true);

                final Map<String, ParallelBootTransactionControl> transactionControls = new LinkedHashMap<String, ParallelBootTransactionControl>();

                final CountDownLatch preparedLatch = new CountDownLatch(runtimeOpsBySubsystem.size());
                final CountDownLatch committedLatch = new CountDownLatch(1);
                final CountDownLatch completeLatch = new CountDownLatch(runtimeOpsBySubsystem.size());
                final Thread controllingThread = Thread.currentThread();

                for (Map.Entry<String, List<ParsedBootOp>> entry : runtimeOpsBySubsystem.entrySet()) {
                    String subsystemName = entry.getKey();
                    final ParallelBootTransactionControl txControl = new ParallelBootTransactionControl(subsystemName, preparedLatch, committedLatch, completeLatch);
                    transactionControls.put(subsystemName, txControl);

                    // Execute the subsystem's ops in another thread
                    ParallelBootTask subsystemTask = new ParallelBootTask(subsystemName, entry.getValue(), context, txControl, null, controllingThread);
                    executor.execute(subsystemTask);
                }

                // Wait for all subsystem ops to complete
                try {
                    preparedLatch.await();

                    // See if all subsystems succeeded; if not report a failure to context
                    checkForSubsystemFailures(context, transactionControls, OperationContext.Stage.RUNTIME);

                } catch (InterruptedException e) {
                    context.getFailureDescription().set(new ModelNode().set(MESSAGES.subsystemBootInterrupted()));
                    Thread.currentThread().interrupt();
                }

                if (MGMT_OP_LOGGER.isDebugEnabled()) {
                    long elapsed = System.currentTimeMillis() - start;
                    MGMT_OP_LOGGER.debugf("Ran subsystem runtime operations in [%d] ms", elapsed);
                }

                // Continue boot
                OperationContext.ResultAction resultAction = context.completeStep();

                // Tell all the subsystem tasks the result of the operations
                notifySubsystemTransactions(transactionControls, resultAction, committedLatch, OperationContext.Stage.MODEL);

                // Make sure all the subsystems have completed the out path before we return
                try {
                    completeLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    private class ParallelBootTask implements Runnable {

        private final String subsystemName;
        private final List<ParsedBootOp> bootOperations;
        private final OperationContext primaryContext;
        private final OperationContext.Stage executionStage;
        private final ParallelBootTransactionControl transactionControl;
        private final List<ParsedBootOp> runtimeOps;
        private final Thread controllingThread;

        public ParallelBootTask(final String subsystemName,
                                final List<ParsedBootOp> bootOperations,
                                final OperationContext primaryContext,
                                final ParallelBootTransactionControl transactionControl,
                                final List<ParsedBootOp> runtimeOps,
                                final Thread controllingThread) {
            this.subsystemName = subsystemName;
            this.bootOperations = bootOperations;
            this.primaryContext = primaryContext;
            this.executionStage = primaryContext.getCurrentStage();
            this.transactionControl = transactionControl;
            this.runtimeOps = runtimeOps;
            this.controllingThread = controllingThread;
        }

        @Override
        public void run() {
            try {
                final OperationContext operationContext = new ParallelBootOperationContext(transactionControl, processState,
                        primaryContext, runtimeOps, controllingThread);
                for (ParsedBootOp op : bootOperations) {
                    final OperationStepHandler osh = op.handler == null ? rootRegistration.getOperationHandler(op.address, op.operationName) : op.handler;
                    operationContext.addStep(op.response, op.operation, osh, executionStage);
                }
                operationContext.completeStep();
            } catch (Exception e) {
                MGMT_OP_LOGGER.failedSubsystemBootOperations(e, subsystemName);
                if (!transactionControl.signalled) {
                    ModelNode failure = new ModelNode();
                    failure.get(ModelDescriptionConstants.SUCCESS).set(false);
                    failure.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).set(e.toString());
                    transactionControl.operationFailed(failure);
                }
            } finally {
                if (!transactionControl.signalled) {

                    for (ParsedBootOp op : bootOperations) {
                        if (op.response.hasDefined(ModelDescriptionConstants.SUCCESS) && !op.response.get(ModelDescriptionConstants.SUCCESS).asBoolean()) {
                            transactionControl.operationFailed(op.response);
                            break;
                        }
                    }
                    if (!transactionControl.signalled) {
                        // TODO this is really debugging
                        ModelNode failure = new ModelNode();
                        failure.get(ModelDescriptionConstants.SUCCESS).set(false);
                        failure.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).set(MESSAGES.subsystemBootOperationFailedExecuting(subsystemName));
                    }
                }
                transactionControl.operationCompleted(transactionControl.response);
            }
        }
    }

    private static class ParallelBootTransactionControl implements ProxyController.ProxyOperationControl {

        private final String subsystemName;
        private final CountDownLatch preparedLatch;
        private final CountDownLatch committedLatch;
        private final CountDownLatch completeLatch;
        private ModelNode response;
        private ModelController.OperationTransaction transaction;
        private boolean signalled;

        public ParallelBootTransactionControl(String subsystemName, CountDownLatch preparedLatch, CountDownLatch committedLatch, CountDownLatch completeLatch) {
            this.preparedLatch = preparedLatch;
            this.committedLatch = committedLatch;
            this.completeLatch = completeLatch;
            this.subsystemName = subsystemName;
        }

        @Override
        public void operationFailed(ModelNode response) {
            if (!signalled) {
                this.response = response;
                preparedLatch.countDown();
                completeLatch.countDown();
                signalled = true;
            }
        }

        @Override
        public void operationPrepared(ModelController.OperationTransaction transaction, ModelNode result) {
            if (!signalled) {
                this.transaction = transaction;
                preparedLatch.countDown();
                signalled = true;

                try {
                    committedLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw MESSAGES.transactionInterrupted();
                }
            }
        }

        @Override
        public void operationCompleted(ModelNode response) {
            this.response = response;
            completeLatch.countDown();
        }
    }
}
