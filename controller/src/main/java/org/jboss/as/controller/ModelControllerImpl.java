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

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.controller.ControllerLogger.MGMT_OP_LOGGER;
import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS_MECHANISM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DOMAIN_UUID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;

import java.io.IOException;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.access.Authorizer;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.extension.ExtensionAddHandler;
import org.jboss.as.controller.extension.ParallelExtensionAddHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.AsyncFutureTask;
import org.wildfly.security.manager.action.GetAccessControlContextAction;


/**
 * Default {@link ModelController} implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ModelControllerImpl implements ModelController {

    private final ServiceRegistry serviceRegistry;
    private final ServiceTarget serviceTarget;
    private final ManagementResourceRegistration rootRegistration;
    private final ModelControllerLock controllerLock = new ModelControllerLock();
    private final ContainerStateMonitor stateMonitor;
    private final RootResource model = new RootResource();
    private final ConfigurationPersister persister;
    private final ProcessType processType;
    private final RunningModeControl runningModeControl;
    private final AtomicBoolean bootingFlag = new AtomicBoolean(true);
    private final OperationStepHandler prepareStep;
    private final ControlledProcessState processState;
    private final ExecutorService executorService;
    private final ExpressionResolver expressionResolver;
    private final Authorizer authorizer;

    private final ConcurrentMap<Integer, AbstractOperationContext> activeOperations = new ConcurrentHashMap<>();
    private final ManagedAuditLogger auditLogger;

    /** Tracks the relationship between domain resources and hosts and server groups */
    private final HostServerGroupTracker hostServerGroupTracker;

    ModelControllerImpl(final ServiceRegistry serviceRegistry, final ServiceTarget serviceTarget, final ManagementResourceRegistration rootRegistration,
                        final ContainerStateMonitor stateMonitor, final ConfigurationPersister persister,
                        final ProcessType processType, final RunningModeControl runningModeControl,
                        final OperationStepHandler prepareStep, final ControlledProcessState processState, final ExecutorService executorService,
                        final ExpressionResolver expressionResolver, final Authorizer authorizer,
                        final ManagedAuditLogger auditLogger) {
        this.serviceRegistry = serviceRegistry;
        this.serviceTarget = serviceTarget;
        this.rootRegistration = rootRegistration;
        this.stateMonitor = stateMonitor;
        this.persister = persister;
        this.processType = processType;
        this.runningModeControl = runningModeControl;
        this.prepareStep = prepareStep == null ? new DefaultPrepareStepHandler() : prepareStep;
        this.processState = processState;
        this.serviceTarget.addListener(stateMonitor);
        this.executorService = executorService;
        this.expressionResolver = expressionResolver;
        this.authorizer = authorizer;
        this.auditLogger = auditLogger;
        this.hostServerGroupTracker = processType.isManagedDomain() ? new HostServerGroupTracker() : null;
        auditLogger.startBoot();
    }

    /**
     * Executes an operation on the controller
     * @param operation the operation
     * @param handler the handler
     * @param control the transaction control
     * @param attachments the operation attachments
     * @return the result of the operation
     */
    public ModelNode execute(final ModelNode operation, final OperationMessageHandler handler, final OperationTransactionControl control, final OperationAttachments attachments) {
        return internalExecute(operation, handler, control, attachments, prepareStep, false);
    }

    /**
     * Executes an operation on the controller latching onto an existing transaction
     *
     * @param operation the operation
     * @param handler the handler
     * @param control the transaction control
     * @param attachments the operation attachments
     * @param prepareStep the prepare step to be executed before any other steps
     * @param operationId the id of the current transaction
     * @return the result of the operation
     */
    protected ModelNode executeReadOnlyOperation(final ModelNode operation, final OperationMessageHandler handler, final OperationTransactionControl control, final OperationAttachments attachments, final OperationStepHandler prepareStep, final int operationId) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ModelController.ACCESS_PERMISSION);
        }

        // Get the primary context to delegate the reads to
        final AbstractOperationContext delegateContext = activeOperations.get(operationId);
        if(delegateContext == null) {
            // TODO we might just allow this case too, but for now it's just wrong (internal) usage
            throw MESSAGES.noContextToDelegateTo(operationId);
        }
        final ModelNode response = new ModelNode();
        final OperationTransactionControl originalResultTxControl = control == null ? null : new OperationTransactionControl() {
            @Override
            public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                control.operationPrepared(transaction, response);
            }
        };
        // Use a read-only context
        final ReadOnlyContext context = new ReadOnlyContext(processType, runningModeControl.getRunningMode(), originalResultTxControl, processState, false, delegateContext, this, operationId);
        context.addStep(response, operation, prepareStep, OperationContext.Stage.MODEL);
        CurrentOperationIdHolder.setCurrentOperationID(operationId);
        try {
            context.executeOperation();
        } finally {
            CurrentOperationIdHolder.setCurrentOperationID(null);
        }

        if (!response.hasDefined(RESPONSE_HEADERS) || !response.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE)) {
            ControlledProcessState.State state = processState.getState();
            switch (state) {
                case RELOAD_REQUIRED:
                case RESTART_REQUIRED:
                    response.get(RESPONSE_HEADERS, PROCESS_STATE).set(state.toString());
                    break;
                default:
                    break;
            }
        }
        return response;
    }

    /**
     * Executes an operation on the controller
     * @param operation the operation
     * @param handler the handler
     * @param control the transaction control
     * @param attachments the operation attachments
     * @param prepareStep the prepare step to be executed before any other steps
     * @param attemptLock set to {@code true} to try to obtain the controller lock
     * @return the result of the operation
     */
    protected ModelNode internalExecute(final ModelNode operation, final OperationMessageHandler handler, final OperationTransactionControl control,
                                        final OperationAttachments attachments, final OperationStepHandler prepareStep, final boolean attemptLock) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ModelController.ACCESS_PERMISSION);
        }

        final ModelNode headers = operation.has(OPERATION_HEADERS) ? operation.get(OPERATION_HEADERS) : null;
        final boolean rollbackOnFailure = headers == null || !headers.hasDefined(ROLLBACK_ON_RUNTIME_FAILURE) || headers.get(ROLLBACK_ON_RUNTIME_FAILURE).asBoolean();
        final EnumSet<OperationContextImpl.ContextFlag> contextFlags = rollbackOnFailure ? EnumSet.of(OperationContextImpl.ContextFlag.ROLLBACK_ON_FAIL) : EnumSet.noneOf(OperationContextImpl.ContextFlag.class);
        final boolean restartResourceServices = headers != null && headers.hasDefined(ALLOW_RESOURCE_SERVICE_RESTART) && headers.get(ALLOW_RESOURCE_SERVICE_RESTART).asBoolean();
        if (restartResourceServices) {
            contextFlags.add(OperationContextImpl.ContextFlag.ALLOW_RESOURCE_SERVICE_RESTART);
        }
        final ModelNode response = new ModelNode();
        // Report the correct operation response, otherwise the preparedResult would only contain
        // the result of the last active step in a composite operation
        final OperationTransactionControl originalResultTxControl = control == null ? null : new OperationTransactionControl() {
            @Override
            public void operationPrepared(OperationTransaction transaction, ModelNode result) {
                control.operationPrepared(transaction, response);
            }
        };

        AccessAuditContext accessContext = SecurityActions.currentAccessAuditContext();
        if (accessContext != null) {
            if (operation.hasDefined(OPERATION_HEADERS)) {
                ModelNode operationHeaders = operation.get(OPERATION_HEADERS);
                if (operationHeaders.hasDefined(DOMAIN_UUID)) {
                    accessContext.setDomainUuid(operationHeaders.get(DOMAIN_UUID).asString());
                }
                if (operationHeaders.hasDefined(ACCESS_MECHANISM)) {
                    accessContext
                            .setAccessMechanism(AccessMechanism.valueOf(operationHeaders.get(ACCESS_MECHANISM).asString()));
                }
            }
        }

        for (;;) {
            // Create a random operation-id
            final Integer operationID = new Random(new SecureRandom().nextLong()).nextInt();
            final OperationContextImpl context = new OperationContextImpl(this, processType, runningModeControl.getRunningMode(),
                    contextFlags, handler, attachments, model, originalResultTxControl, processState, auditLogger,
                    bootingFlag.get(), operationID, hostServerGroupTracker);
            // Try again if the operation-id is already taken
            if(activeOperations.putIfAbsent(operationID, context) == null) {
                CurrentOperationIdHolder.setCurrentOperationID(operationID);
                boolean shouldUnlock = false;
                try {
                    if (attemptLock) {
                        if (!controllerLock.detectDeadlockAndGetLock(operationID)) {
                            response.get(OUTCOME).set(FAILED);
                            response.get(FAILURE_DESCRIPTION).set(MESSAGES.cannotGetControllerLock());
                            return response;
                        }
                        shouldUnlock = true;
                    }

                    context.addStep(response, operation, prepareStep, OperationContext.Stage.MODEL);
                    context.executeOperation();
                } finally {
                    if (shouldUnlock) {
                        controllerLock.unlock(operationID);
                    }
                    activeOperations.remove(operationID);
                    CurrentOperationIdHolder.setCurrentOperationID(null);
                }
                break;
            }
        }

        if (!response.hasDefined(RESPONSE_HEADERS) || !response.get(RESPONSE_HEADERS).hasDefined(PROCESS_STATE)) {
            ControlledProcessState.State state = processState.getState();
            switch (state) {
                case RELOAD_REQUIRED:
                case RESTART_REQUIRED:
                    response.get(RESPONSE_HEADERS, PROCESS_STATE).set(state.toString());
                    break;
                default:
                    break;
            }
        }
        return response;
    }

    boolean boot(final List<ModelNode> bootList, final OperationMessageHandler handler, final OperationTransactionControl control,
              final boolean rollbackOnRuntimeFailure) {

        final Integer operationID = new Random(new SecureRandom().nextLong()).nextInt();

        EnumSet<OperationContextImpl.ContextFlag> contextFlags = rollbackOnRuntimeFailure
                ? EnumSet.of(OperationContextImpl.ContextFlag.ROLLBACK_ON_FAIL)
                : EnumSet.noneOf(OperationContextImpl.ContextFlag.class);
        final OperationContextImpl context = new OperationContextImpl(this, processType, runningModeControl.getRunningMode(),
                contextFlags, handler, null, model, control, processState, auditLogger, bootingFlag.get(), operationID, hostServerGroupTracker);

        // Add to the context all ops prior to the first ExtensionAddHandler as well as all ExtensionAddHandlers; save the rest.
        // This gets extensions registered before proceeding to other ops that count on these registrations
        List<ParsedBootOp> postExtensionOps = organizeBootOperations(bootList, context, operationID);

        // Run the steps up to the last ExtensionAddHandler
        OperationContext.ResultAction resultAction = context.executeOperation();
        if (resultAction == OperationContext.ResultAction.KEEP && postExtensionOps != null) {

            // Success. Now any extension handlers are registered. Continue with remaining ops
            final OperationContextImpl postExtContext = new OperationContextImpl(this, processType, runningModeControl.getRunningMode(),
                    contextFlags, handler, null, model, control, processState, auditLogger, bootingFlag.get(), operationID, hostServerGroupTracker);

            for (ParsedBootOp parsedOp : postExtensionOps) {
                final OperationStepHandler stepHandler = parsedOp.handler == null ? rootRegistration.getOperationHandler(parsedOp.address, parsedOp.operationName) : parsedOp.handler;
                if (stepHandler == null) {
                    logNoHandler(parsedOp);
                    postExtContext.setRollbackOnly();
                    // stop
                    break;
                } else {
                    postExtContext.addStep(parsedOp.response, parsedOp.operation, stepHandler, OperationContext.Stage.MODEL);
                }
            }

            resultAction = postExtContext.executeOperation();
        }

        return  resultAction == OperationContext.ResultAction.KEEP;
    }

    /**
     * Organizes the list of boot operations such that all extension add operations are executed in the given context,
     * while all non-extension add operations found after the first extension add are stored for subsequent invocation
     * in a separate context. Also:
     * <ol>
     *     <li>Ensures that any operations affecting interfaces or sockets are run before any operations affecting
     *     subsystems. This improves boot performance by ensuring required services are available as soon as possible.
     *     </li>
     *     <li>If an executor service is available, organizes all extension add ops so the extension initialization
     *      can be done in parallel by the executor service.
     *     </li>
     *     <li>If an executor service is available and the controller type is SERVER, organizes all subsystem ops so
     *     they can be done in parallel by the executor service.
     *     </li>
     * </ol>
     *
     * @param bootList the list of boot operations
     * @param context operation context to use for all ops prior to the last extension add operation.
     *
     * @return a list of operations to execute in a separate context, or {@code null} if there are no such ops
     */
    private List<ParsedBootOp> organizeBootOperations(List<ModelNode> bootList, OperationContextImpl context, final int lockPermit) {

        final ModelNode result = new ModelNode().setEmptyList();

        boolean sawExtensionAdd = false;
        List<ParsedBootOp> postExtensionOps = null;
        ParallelExtensionAddHandler parallelExtensionAddHandler = executorService == null ? null : new ParallelExtensionAddHandler(executorService);
        ParallelBootOperationStepHandler parallelSubsystemHandler = (executorService != null && processType.isServer() && runningModeControl.getRunningMode() == RunningMode.NORMAL)
                ? new ParallelBootOperationStepHandler(executorService, rootRegistration, processState, this, lockPermit) : null;
        boolean registeredParallelSubsystemHandler = false;
        int subsystemIndex = 0;
        for (ModelNode bootOp : bootList) {
            final ParsedBootOp parsedOp = new ParsedBootOp(bootOp, result.add());
            if (postExtensionOps != null) {
                // Handle cases like AppClient where extension adds are interleaved with subsystem ops
                if (parsedOp.isExtensionAdd()) {
                    final ExtensionAddHandler stepHandler = (ExtensionAddHandler) rootRegistration.getOperationHandler(parsedOp.address, parsedOp.operationName);
                    if (parallelExtensionAddHandler != null) {
                        parallelExtensionAddHandler.addParsedOp(parsedOp, stepHandler);
                    } else {
                        context.addStep(parsedOp.response, parsedOp.operation, stepHandler, OperationContext.Stage.MODEL);
                    }
                } else {
                    if (parallelSubsystemHandler == null || !parallelSubsystemHandler.addSubsystemOperation(parsedOp)) {
                        // Put any interface/socket op before the subsystem op
                        if (registeredParallelSubsystemHandler && (parsedOp.isInterfaceOperation() || parsedOp.isSocketOperation())) {
                            postExtensionOps.add(subsystemIndex++, parsedOp);
                        } else {
                            postExtensionOps.add(parsedOp);
                        }
                    } else if (!registeredParallelSubsystemHandler) {
                        ModelNode op = Util.getEmptyOperation("parallel-subsystem-boot", new ModelNode().setEmptyList());
                        postExtensionOps.add(new ParsedBootOp(op, parallelSubsystemHandler, result.add()));
                        subsystemIndex = postExtensionOps.size() - 1;
                        registeredParallelSubsystemHandler = true;
                    }
                }
            } else {
                final OperationStepHandler stepHandler = rootRegistration.getOperationHandler(parsedOp.address, parsedOp.operationName);
                if (!sawExtensionAdd && stepHandler == null) {
                    // Odd case. An op prior to the first extension add where there is no handler. This would really
                    // only happen during AS development
                    logNoHandler(parsedOp);
                    context.setRollbackOnly();
                    // stop
                    break;
                } else if (stepHandler instanceof ExtensionAddHandler) {
                    if (parallelExtensionAddHandler != null) {
                        parallelExtensionAddHandler.addParsedOp(parsedOp, (ExtensionAddHandler) stepHandler);
                        if (!sawExtensionAdd) {
                            ModelNode op = Util.getEmptyOperation("parallel-extension-add", new ModelNode().setEmptyList());
                            context.addStep(result.add(), op, parallelExtensionAddHandler, OperationContext.Stage.MODEL);
                        }
                    } else {
                        context.addStep(parsedOp.response, parsedOp.operation, stepHandler, OperationContext.Stage.MODEL);
                    }
                    sawExtensionAdd = true;
                } else if (!sawExtensionAdd) {
                    // An operation prior to the first Extension Add
                    context.addStep(result.add(), bootOp, stepHandler, OperationContext.Stage.MODEL);
                } else {
                    // Start the postExtension list
                    postExtensionOps = new ArrayList<ParsedBootOp>();
                    if (parallelSubsystemHandler == null || !parallelSubsystemHandler.addSubsystemOperation(parsedOp)) {
                        postExtensionOps.add(parsedOp);
                    } else {
                        // First subsystem op; register the parallel handler and add the op to it
                        ModelNode op = Util.getEmptyOperation("parallel-subsystem-boot", new ModelNode().setEmptyList());
                        postExtensionOps.add(new ParsedBootOp(op, parallelSubsystemHandler, result.add()));
                        registeredParallelSubsystemHandler = true;
                    }
                }
            }
        }
        return postExtensionOps;
    }

    void finishBoot() {
        // Notify the audit logger that we're done booting
        auditLogger.bootDone();
        bootingFlag.set(false);
    }

    public Resource getRootResource() {
        return model;
    }

    ManagementResourceRegistration getRootRegistration() {
        return rootRegistration;
    }

    public ModelControllerClient createClient(final Executor executor) {

        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(ModelController.ACCESS_PERMISSION);
        }

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
                    throw MESSAGES.nullAsynchronousExecutor();
                }
                final AtomicReference<Thread> opThread = new AtomicReference<Thread>();
                class OpTask extends AsyncFutureTask<ModelNode> {
                    OpTask() {
                        super(executor);
                    }

                    public void asyncCancel(final boolean interruptionDesired) {
                        Thread thread = opThread.getAndSet(Thread.currentThread());
                        if (thread == null) {
                            setCancelled();
                        } else {
                            // Interrupt the request execution
                            thread.interrupt();
                            // Wait for the cancellation to clear opThread
                            boolean interrupted = false;
                            synchronized (opThread) {
                                while (opThread.get() != null) {
                                    try {
                                        opThread.wait();
                                    } catch (InterruptedException ie) {
                                        interrupted = true;
                                    }
                                }
                            }
                            setCancelled();
                            if (interrupted) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }

                    void handleResult(final ModelNode result) {
                        if (result != null && result.hasDefined(OUTCOME) && CANCELLED.equals(result.get(OUTCOME).asString())) {
                            setCancelled();
                        } else {
                            setResult(result);
                        }
                    }
                }
                final OpTask opTask = new OpTask();
                final AccessControlContext acc = doPrivileged(GetAccessControlContextAction.getInstance());
                executor.execute(new Runnable() {
                    public void run() {
                        try {
                            if (opThread.compareAndSet(null, Thread.currentThread())) {
                                ModelNode response = doPrivileged(new PrivilegedAction<ModelNode>() {

                                    @Override
                                    public ModelNode run() {
                                        return ModelControllerImpl.this.execute(operation, messageHandler,
                                                OperationTransactionControl.COMMIT, attachments);
                                    }
                                }, acc);
                                opTask.handleResult(response);
                            }
                        } finally {
                            synchronized (opThread) {
                                opThread.set(null);
                                opThread.notifyAll();
                            }
                        }
                    }
                });
                return opTask;
            }
        };
    }

    ConfigurationPersister.PersistenceResource writeModel(final Resource resource, Set<PathAddress> affectedAddresses) throws ConfigurationPersistenceException {
        final ModelNode newModel = Resource.Tools.readModel(resource);
        final ConfigurationPersister.PersistenceResource delegate = persister.store(newModel, affectedAddresses);
        return new ConfigurationPersister.PersistenceResource() {

            @Override
            public void commit() {
                // Discard the tracker first, so if there's any race the new OperationContextImpl
                // gets a cleared tracker
                if (hostServerGroupTracker != null) {
                    hostServerGroupTracker.invalidate();
                }
                model.set(resource);
                delegate.commit();
            }

            @Override
            public void rollback() {
                delegate.rollback();
            }
        };
    }

    void acquireLock(Integer permit, final boolean interruptibly, OperationContext context) throws InterruptedException {
        if (interruptibly) {
            //noinspection LockAcquiredButNotSafelyReleased
            controllerLock.lockInterruptibly(permit);
        } else {
            //noinspection LockAcquiredButNotSafelyReleased
            controllerLock.lock(permit);
        }
    }

    void releaseLock(Integer permit) {
        controllerLock.unlock(permit);
    }

    void acquireContainerMonitor() {
        stateMonitor.acquire();
    }

    void releaseContainerMonitor() {
        stateMonitor.release();
    }

    void awaitContainerMonitor(final boolean interruptibly) throws InterruptedException {
        if (interruptibly) {
            stateMonitor.await();
        } else {
            stateMonitor.awaitUninterruptibly();
        }
    }

    ContainerStateMonitor.ContainerStateChangeReport awaitContainerStateChangeReport() throws InterruptedException {
        return stateMonitor.awaitContainerStateChangeReport();
    }

    ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    ServiceTarget getServiceTarget() {
        return serviceTarget;
    }

    ModelNode resolveExpressions(ModelNode node) throws OperationFailedException {
        return expressionResolver.resolveExpressions(node);
    }

    Authorizer getAuthorizer() {
        return authorizer;
    }

    private void logNoHandler(ParsedBootOp parsedOp) {
        ImmutableManagementResourceRegistration child = rootRegistration.getSubModel(parsedOp.address);
        if (child == null) {
            ROOT_LOGGER.noSuchResourceType(parsedOp.address);
        } else {
            ROOT_LOGGER.noHandlerForOperation(parsedOp.operationName, parsedOp.address);
        }

    }

    AuditLogger getAuditLogger() {
        return auditLogger;
    }

    private class DefaultPrepareStepHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if (MGMT_OP_LOGGER.isTraceEnabled()) {
                MGMT_OP_LOGGER.tracef("Executing %s %s", operation.get(OP), operation.get(OP_ADDR));
            }
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String operationName =  operation.require(OP).asString();
            final OperationStepHandler stepHandler = resolveOperationHandler(address, operationName);
            if(stepHandler != null) {
                context.addStep(stepHandler, OperationContext.Stage.MODEL);
            } else {

                ImmutableManagementResourceRegistration child = rootRegistration.getSubModel(address);
                if (child == null) {
                    context.getFailureDescription().set(MESSAGES.noSuchResourceType(address));
                } else {
                    context.getFailureDescription().set(MESSAGES.noHandlerForOperation(operationName, address));
                }
            }
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }

    private OperationStepHandler resolveOperationHandler(final PathAddress address, final String operationName) {
        OperationStepHandler result = rootRegistration.getOperationHandler(address, operationName);
        if (result == null && address.size() > 0) {
            // For wildcard elements, check specific registrations where the same OSH is used
            // for all such registrations
            PathElement pe = address.getLastElement();
            if (pe.isWildcard()) {
                String type = pe.getKey();
                PathAddress parent = address.subAddress(0, address.size() - 1);
                Set<PathElement> children = rootRegistration.getChildAddresses(parent);
                if (children != null) {
                    OperationStepHandler found = null;
                    for (PathElement child : children) {
                        if (type.equals(child.getKey())) {
                            OperationEntry oe = rootRegistration.getOperationEntry(parent.append(child), operationName);
                            OperationStepHandler osh = oe == null ? null : oe.getOperationHandler();
                            if (osh == null || (found != null && !found.equals(osh))) {
                                // Not all children have the same handler; give up
                                found = null;
                                break;
                            }
                            // We have a candidate OSH
                            found = osh;
                        }
                    }
                    if (found != null) {
                        result = found;
                    }
                }
            }
        }
        return result;
    }

    /**
     * The root resource, maintains a read-only reference to the current model. All write operations have to performed
     * after acquiring the write lock on a clone of the underlying model.
     */
    private final class RootResource implements Resource {

        private final AtomicReference<Resource> modelReference = new AtomicReference<Resource>(Resource.Factory.create());

        /**
         * Publishes the new version of the model to any handlers that have a reference to this object.
         * Thereafter any calls to the methods of this object will delegate to the new version.
         * TODO handlers with a local variable reference to children of this resource will see the old model.
         */
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



}
