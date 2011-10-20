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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.operations.common.ExtensionAddHandler;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ModelControllerImpl implements ModelController {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private final ServiceRegistry serviceRegistry;
    private final ServiceTarget serviceTarget;
    private final ManagementResourceRegistration rootRegistration;
    private final Lock writeLock = new ReentrantLock();
    private final ContainerStateMonitor stateMonitor;
    private final RootResource model = new RootResource();
    private final ConfigurationPersister persister;
    private final OperationContext.Type controllerType;
    private final AtomicBoolean bootingFlag = new AtomicBoolean(true);
    private final OperationStepHandler prepareStep;
    private final ControlledProcessState processState;

    ModelControllerImpl(final ServiceRegistry serviceRegistry, final ServiceTarget serviceTarget, final ManagementResourceRegistration rootRegistration,
                        final ContainerStateMonitor stateMonitor, final ConfigurationPersister persister, final OperationContext.Type controllerType,
                        final OperationStepHandler prepareStep, final ControlledProcessState processState) {
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

    public ModelNode execute(final ModelNode operation, final OperationMessageHandler handler, final OperationTransactionControl control, final OperationAttachments attachments) {
        final ModelNode headers = operation.has(OPERATION_HEADERS) ? operation.get(OPERATION_HEADERS) : null;
        final boolean rollbackOnFailure = headers == null || !headers.hasDefined(ROLLBACK_ON_RUNTIME_FAILURE) || headers.get(ROLLBACK_ON_RUNTIME_FAILURE).asBoolean();
        final EnumSet<OperationContextImpl.ContextFlag> contextFlags = rollbackOnFailure ? EnumSet.of(OperationContextImpl.ContextFlag.ROLLBACK_ON_FAIL) : EnumSet.noneOf(OperationContextImpl.ContextFlag.class);
        final boolean restartResourceServices = headers != null && headers.hasDefined(ALLOW_RESOURCE_SERVICE_RESTART) && headers.get(ALLOW_RESOURCE_SERVICE_RESTART).asBoolean();
        if (restartResourceServices) {
            contextFlags.add(OperationContextImpl.ContextFlag.ALLOW_RESOURCE_SERVICE_RESTART);
        }
        OperationContextImpl context = new OperationContextImpl(this, controllerType, contextFlags, handler, attachments, model, control, processState, bootingFlag.get());
        ModelNode response = new ModelNode();
        context.addStep(response, operation, prepareStep, OperationContext.Stage.MODEL);

        context.completeStep();

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

        // Execute all ops prior to the first ExtensionAddHandler as well as all ExtensionAddHandlers; save the rest.
        // This gets extensions registered before proceeding to other ops that count on these registrations
        final OperationContextImpl context = new OperationContextImpl(this, controllerType, EnumSet.noneOf(OperationContextImpl.ContextFlag.class),
                handler, null, model, control, processState, bootingFlag.get());
        final ModelNode result = new ModelNode().setEmptyList();
        result.setEmptyList();
        boolean sawExtensionAdd = false;
        List<ParsedOp> postExtensionOps = null;
        for (ModelNode bootOp : bootList) {
            final ParsedOp parsedOp = new ParsedOp(bootOp);
            if (postExtensionOps != null) {
                // Handle cases like AppClient where extension adds are interleaved with subsystem ops
                if (parsedOp.isExtensionAdd()) {
                    final OperationStepHandler stepHandler = rootRegistration.getOperationHandler(parsedOp.address, parsedOp.operationName);
                    context.addStep(result.add(), bootOp, stepHandler, OperationContext.Stage.MODEL);
                } else {
                    postExtensionOps.add(parsedOp);
                }
            } else {
                final OperationStepHandler stepHandler = rootRegistration.getOperationHandler(parsedOp.address, parsedOp.operationName);
                if (!sawExtensionAdd && stepHandler == null) {
                    // Odd case. An op prior to the first extension add where there is no handler. This would really
                    // only happen during AS development
                    String msg = String.format("No handler for operation %s at address %s", parsedOp.operationName, parsedOp.address);
                    log.error(msg);
                    context.setRollbackOnly();
                    // stop
                    break;
                } else if (stepHandler instanceof ExtensionAddHandler) {
                    context.addStep(result.add(), bootOp, stepHandler, OperationContext.Stage.MODEL);
                    sawExtensionAdd = true;
                } else if (!sawExtensionAdd) {
                    // An operation prior to the first Extension Add
                    context.addStep(result.add(), bootOp, stepHandler, OperationContext.Stage.MODEL);
                } else {
                    // Start the postExtension list
                    postExtensionOps = new ArrayList<ParsedOp>();
                    postExtensionOps.add(parsedOp);
                }
            }
        }

        // Run the steps up to the last ExtensionAddHandler
        if (context.completeStep() == OperationContext.ResultAction.KEEP && postExtensionOps != null) {

            // Success. Now any extension handlers are registered. Continue with remaining ops
            final OperationContextImpl postExtContext = new OperationContextImpl(this, controllerType, EnumSet.noneOf(OperationContextImpl.ContextFlag.class),
                    handler, null, model, control, processState, bootingFlag.get());
            final ModelNode postExtResult = new ModelNode().setEmptyList();
            postExtResult.setEmptyList();

            for (ParsedOp parsedOp : postExtensionOps) {
                final OperationStepHandler stepHandler = rootRegistration.getOperationHandler(parsedOp.address, parsedOp.operationName);
                if (stepHandler == null) {
                    String msg = String.format("No handler for operation %s at address %s", parsedOp.operationName, parsedOp.address);
                    log.error(msg);
                    postExtContext.setRollbackOnly();
                    // stop
                    break;
                } else {
                    final ModelNode response = postExtResult.add();
                    postExtContext.addStep(response, parsedOp.operation, stepHandler, OperationContext.Stage.MODEL);
                }
            }

            postExtContext.completeStep();
        }
    }

    void finshBoot() {
        bootingFlag.set(false);
    }

    public Resource getRootResource() {
        return model;
    }

    ManagementResourceRegistration getRootRegistration() {
        return rootRegistration;
    }

    public ModelControllerClient createClient(final Executor executor) {
        return new ModelControllerClient() {

            @Override
            public void close() throws IOException {
                // whatever
            }

            @Override
            public ModelNode execute(ModelNode operation) throws IOException {
                return execute(operation, null);
            }

            @Override
            public ModelNode execute(Operation operation) throws IOException {
                return execute(operation, null);
            }

            @Override
            public ModelNode execute(final ModelNode operation, final OperationMessageHandler messageHandler) {
                return ModelControllerImpl.this.execute(operation, messageHandler, OperationTransactionControl.COMMIT, null);
            }

            @Override
            public ModelNode execute(Operation operation, OperationMessageHandler messageHandler) throws IOException {
                return ModelControllerImpl.this.execute(operation.getOperation(), messageHandler, OperationTransactionControl.COMMIT, operation);
            }

            @Override
            public AsyncFuture<ModelNode> executeAsync(ModelNode operation, OperationMessageHandler messageHandler) {
                return executeAsync(operation, messageHandler, null);
            }

            @Override
            public AsyncFuture<ModelNode> executeAsync(final Operation operation, final OperationMessageHandler messageHandler) {
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
                            opTask.handleResult(ModelControllerImpl.this.execute(operation, messageHandler, OperationTransactionControl.COMMIT, attachments));
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
                model.set(resource);
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

    private class DefaultPrepareStepHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (log.isTraceEnabled()) {
                log.trace("Executing " + operation.get(OP) + " " + operation.get(OP_ADDR));
            }
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String operationName =  operation.require(OP).asString();
            final OperationStepHandler stepHandler = rootRegistration.getOperationHandler(address, operationName);
            if(stepHandler != null) {
                context.addStep(stepHandler, OperationContext.Stage.MODEL);
            } else {
                context.getFailureDescription().set(String.format("No handler for operation %s at address %s", operationName, address));
            }
            context.completeStep();
        }
    }

    /**
     * The root resource, maintains a read-only reference to the current model. All write operations have to performed
     * after acquiring the write lock on a clone of the underlying model.
     */
    private class RootResource implements Resource {

        private final AtomicReference<Resource> modelReference = new AtomicReference<Resource>(Resource.Factory.create());

        void set(Resource resource){
            modelReference.set(resource);
        }

        @SuppressWarnings({"CloneDoesntCallSuperClone"})
        @Override
        public Resource clone() {
            return getDelegate().clone();
        }

        public Resource getChild(PathElement element) {
            return getDelegate().getChild(element);
        }

        public Set<Resource.ResourceEntry> getChildren(String childType) {
            return getDelegate().getChildren(childType);
        }

        public Set<String> getChildrenNames(String childType) {
            return getDelegate().getChildrenNames(childType);
        }

        public Set<String> getChildTypes() {
            return getDelegate().getChildTypes();
        }

        public ModelNode getModel() {
            return getDelegate().getModel();
        }

        public boolean hasChild(PathElement element) {
            return getDelegate().hasChild(element);
        }

        public boolean hasChildren(String childType) {
            return getDelegate().hasChildren(childType);
        }

        public boolean isModelDefined() {
            return getDelegate().isModelDefined();
        }

        public boolean isProxy() {
            return getDelegate().isProxy();
        }

        public boolean isRuntime() {
            return getDelegate().isRuntime();
        }

        public Resource navigate(PathAddress address) {
            return getDelegate().navigate(address);
        }

        public void registerChild(PathElement address, Resource resource) {
            getDelegate().registerChild(address, resource);
        }

        public Resource removeChild(PathElement address) {
            return getDelegate().removeChild(address);
        }

        public Resource requireChild(PathElement element) {
            return getDelegate().requireChild(element);
        }

        public void writeModel(ModelNode newModel) {
            getDelegate().writeModel(newModel);
        }

        private Resource getDelegate() {
            return this.modelReference.get();
        }

    }

    private static class ParsedOp {
        private final ModelNode operation;
        private final String operationName;
        private final PathAddress address;

        private ParsedOp(final ModelNode operation) {
            this.operation = operation;
            this.address = PathAddress.pathAddress(operation.get(OP_ADDR));
            this.operationName = operation.require(OP).asString();
        }

        private boolean isExtensionAdd() {
            return address.size() == 1 && address.getElement(0).getKey().equals(EXTENSION)
                        && operationName.equals(ADD);
        }
    }

}
