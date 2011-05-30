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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.controller.client.NewModelControllerClient;
import org.jboss.as.controller.client.NewOperation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.operations.common.AbstractExtensionAddHandler;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class NewModelControllerImpl implements NewModelController {

    private static final ModelNode EMPTY;

    static {
        ModelNode empty = new ModelNode();
        empty.protect();
        EMPTY = empty;
    }

    private final ServiceRegistry serviceRegistry;
    private final ServiceTarget serviceTarget;
    private final ModelNodeRegistration rootRegistration;
    private final Lock writeLock = new ReentrantLock();
    private final ContainerStateMonitor stateMonitor;
    private final AtomicReference<Resource> modelReference = new AtomicReference<Resource>(Resource.Factory.create());
    private final ConfigurationPersister persister;
    private final NewOperationContext.Type controllerType;
    private final AtomicBoolean bootingFlag = new AtomicBoolean(true);
    private final NewStepHandler prepareStep;
    private final ControlledProcessState processState;

    @Deprecated
    NewModelControllerImpl(final ModelNode model, final ServiceRegistry serviceRegistry, final ServiceTarget serviceTarget, final ModelNodeRegistration rootRegistration,
                       final ContainerStateMonitor stateMonitor, final ConfigurationPersister persister, final NewOperationContext.Type controllerType,
                       final NewStepHandler prepareStep,final ControlledProcessState processState) {
        this(serviceRegistry, serviceTarget, rootRegistration, stateMonitor, persister, controllerType, prepareStep, processState);
    }

    NewModelControllerImpl(final ServiceRegistry serviceRegistry, final ServiceTarget serviceTarget, final ModelNodeRegistration rootRegistration,
                           final ContainerStateMonitor stateMonitor, final ConfigurationPersister persister, final NewOperationContext.Type controllerType,
                           final NewStepHandler prepareStep,final ControlledProcessState processState) {
        this.serviceRegistry = serviceRegistry;
        this.serviceTarget = serviceTarget;
        this.rootRegistration = rootRegistration;
        this.stateMonitor = stateMonitor;
        this.persister = persister;
        this.controllerType = controllerType;
        this.prepareStep = prepareStep == null ? new DefaultPrepareStepHandler() : prepareStep;
        this.processState = processState;
        this.serviceTarget.addListener(ServiceListener.Inheritance.ALL, stateMonitor);
    }

    static final ThreadLocal<Boolean> RB_ON_RT_FAILURE = new ThreadLocal<Boolean>();

    public ModelNode execute(final ModelNode operation, final OperationMessageHandler handler, final OperationTransactionControl control, final OperationAttachments attachments) {
        final ModelNode headers = operation.has(OPERATION_HEADERS) ? operation.get(OPERATION_HEADERS) : null;
        final boolean rollbackOnFailure = headers == null || !headers.hasDefined(ROLLBACK_ON_RUNTIME_FAILURE) || headers.get(ROLLBACK_ON_RUNTIME_FAILURE).asBoolean();
        final EnumSet<NewOperationContextImpl.ContextFlag> contextFlags = rollbackOnFailure ? EnumSet.of(NewOperationContextImpl.ContextFlag.ROLLBACK_ON_FAIL) : EnumSet.noneOf(NewOperationContextImpl.ContextFlag.class);
        NewOperationContextImpl context = new NewOperationContextImpl(this, controllerType, contextFlags, handler, attachments, modelReference.get(), control, processState, bootingFlag.get());
        ModelNode response = new ModelNode();
        context.addStep(response, operation, prepareStep, NewOperationContext.Stage.MODEL);
        RB_ON_RT_FAILURE.set(Boolean.valueOf(rollbackOnFailure));
        try {
            context.completeStep();
        } finally {
            RB_ON_RT_FAILURE.set(null);
        }
        ControlledProcessState.State state = processState.getState();
        switch (state) {
            case RELOAD_REQUIRED:
            case RESTART_REQUIRED:
                response.get(RESPONSE_HEADERS, PROCESS_STATE).set(state.toString());
                break;
            default:
                break;
        }
        return response;
    }

    void boot(final List<ModelNode> bootList, final OperationMessageHandler handler, final OperationTransactionControl control) {
        NewOperationContextImpl context = new NewOperationContextImpl(this, controllerType, EnumSet.noneOf(NewOperationContextImpl.ContextFlag.class), handler, null, modelReference.get(), control, processState, bootingFlag.get());
        ModelNode result = context.getResult();
        result.setEmptyList();
        for (ModelNode bootOp : bootList) {
            final ModelNode response = result.add();
            context.addStep(response, bootOp, new BootStepHandler(bootOp, response), NewOperationContext.Stage.MODEL);
        }
        context.completeStep();
    }

    void finshBoot() {
        bootingFlag.set(false);
    }

    ModelNodeRegistration getRootRegistration() {
        return rootRegistration;
    }

    class BootStepHandler implements NewStepHandler {
        private final ModelNode operation;
        private final ModelNode response;

        BootStepHandler(final ModelNode operation, final ModelNode response) {
            this.operation = operation;
            this.response = response;
        }

        public void execute(final NewOperationContext context, final ModelNode operation) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.require(ADDRESS));
            final String operationName = operation.require(OP).asString();
            final NewStepHandler stepHandler = rootRegistration.getOperationHandler(address, operationName);
            if (stepHandler == null) {
                context.getFailureDescription().set(String.format("No handler for operation %s at address %s", operationName, address));
            } else {
                NewOperationContext.Stage stage = NewOperationContext.Stage.MODEL;
                if(stepHandler instanceof AbstractExtensionAddHandler) {
                    stage = NewOperationContext.Stage.IMMEDIATE;
                }
                context.addStep(response, this.operation, stepHandler, stage);
            }
            context.completeStep();
        }
    }

    public NewModelControllerClient createClient(final Executor executor) {
        return new NewModelControllerClient() {

            @Override
            public void close() throws IOException {
                // whatever
            }

            @Override
            public ModelNode execute(ModelNode operation) throws IOException {
                return execute(operation, null);
            }

            @Override
            public ModelNode execute(NewOperation operation) throws IOException {
                return execute(operation, null);
            }

            @Override
            public ModelNode execute(final ModelNode operation, final OperationMessageHandler messageHandler) {
                return NewModelControllerImpl.this.execute(operation, messageHandler, OperationTransactionControl.COMMIT, null);
            }

            @Override
            public ModelNode execute(NewOperation operation, OperationMessageHandler messageHandler) throws IOException {
                return NewModelControllerImpl.this.execute(operation.getOperation(), messageHandler, OperationTransactionControl.COMMIT, operation);
            }

            @Override
            public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
                return executeAsync(operation, messageHandler, null);
            }

            @Override
            public AsyncFuture<ModelNode> executeAsync(final NewOperation operation, final OperationMessageHandler messageHandler) {
                return executeAsync(operation.getOperation(), messageHandler, operation);
            }

            private AsyncFuture<ModelNode> executeAsync(final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) {
                if (executor == null) {
                    throw new IllegalStateException("Cannot execute asynchronous operation without an executor");
                }
                final AtomicReference<Thread> opThread = new AtomicReference<Thread>();
                class OpTask extends AsyncFutureTask<ModelNode> {
                    OpTask() {
                        super(executor);
                    }

                    public void asyncCancel(final boolean interruptionDesired) {
                        Thread thread = opThread.get();
                        if (thread != null) {
                            thread.interrupt();
                        }
                    }

                    void handleResult(final ModelNode result) {
                        setResult(result);
                    }
                }
                final OpTask opTask = new OpTask();
                executor.execute(new Runnable() {
                    public void run() {
                        opThread.set(Thread.currentThread());
                        try {
                            opTask.handleResult(NewModelControllerImpl.this.execute(operation, messageHandler, OperationTransactionControl.COMMIT, attachments));
                        } finally {
                            opThread.set(null);
                        }
                    }
                });
                return opTask;
            }
        };
    }

    ConfigurationPersister.PersistenceResource writeModel(final Resource resource, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
        final ModelNode newModel = Resource.Tools.readModel(resource);  // Get the model representation
        final ConfigurationPersister.PersistenceResource delegate = persister.store(newModel, affectedAddresses);
        return new ConfigurationPersister.PersistenceResource() {

            @Override
            public void commit() {
                modelReference.set(resource);
                delegate.commit();
            }

            @Override
            public void rollback() {
                delegate.rollback();
            }
        };
    }

    void acquireLock(final boolean interruptibly) throws InterruptedException {
        if (interruptibly) {
            //noinspection LockAcquiredButNotSafelyReleased
            writeLock.lockInterruptibly();
        } else {
            //noinspection LockAcquiredButNotSafelyReleased
            writeLock.lock();
        }
    }

    void releaseLock() {
        writeLock.unlock();
    }

    void acquireContainerMonitor() {
        stateMonitor.acquire();
    }

    void releaseContainerMonitor() {
        stateMonitor.release();
    }

    void awaitContainerMonitor(final boolean interruptibly, final int count) throws InterruptedException {
        if (interruptibly) {
            stateMonitor.await(count);
        } else {
            stateMonitor.awaitUninterruptibly(count);
        }
    }

    ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    ServiceTarget getServiceTarget() {
        return serviceTarget;
    }

    ControlledProcessState.State getState() {
        return processState.getState();
    }

    Object setReloadRequired() {
        return processState.setReloadRequired();
    }

    Object setRestartRequired() {
        return processState.setRestartRequired();
    };

    void revertReloadRequired(Object stamp) {
        processState.revertReloadRequired(stamp);
    }

    void revertRestartRequired(Object stamp) {
        processState.revertRestartRequired(stamp);
    }

    private class DefaultPrepareStepHandler implements NewStepHandler {

        @Override
        public void execute(NewOperationContext context, ModelNode operation) throws OperationFailedException {
            System.out.println("Executing " + operation.get(OP) + " " + operation.get(OP_ADDR));
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String operationName =  operation.require(OP).asString();
            final NewStepHandler stepHandler = rootRegistration.getOperationHandler(address, operationName);
            if(stepHandler != null) {
                context.addStep(stepHandler, NewOperationContext.Stage.MODEL);
            } else {
                context.getFailureDescription().set(String.format("No handler for operation %s at address %s", operationName, address));
            }
            context.completeStep();
        }
    }
}
