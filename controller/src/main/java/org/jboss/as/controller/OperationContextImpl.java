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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_UPDATE_SKIPPED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.registry.DelegatingImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.BatchServiceTarget;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * Operation context implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class OperationContextImpl implements OperationContext {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private final ModelControllerImpl modelController;
    private final Type contextType;
    private final EnumSet<ContextFlag> contextFlags;
    private final OperationMessageHandler messageHandler;
    private final Thread initiatingThread;
    private final EnumMap<Stage, Deque<Step>> steps;
    private final ModelController.OperationTransactionControl transactionControl;
    private final ServiceTarget serviceTarget;
    private final Map<ServiceName, ServiceController<?>> realRemovingControllers = new HashMap<ServiceName, ServiceController<?>>();
    // protected by "realRemovingControllers"
    private final Map<ServiceName, Step> removalSteps = new HashMap<ServiceName, Step>();
    private final boolean booting;
    private final OperationAttachments attachments;
    private final ControlledProcessState processState;
    /** Tracks whether any steps have gotten write access to the model */
    private final Set<PathAddress> affectsModel;
    /** Tracks whether any steps have gotten write access to the management resource registration*/
    private boolean affectsResourceRegistration;

    private boolean respectInterruption = true;
    private Stage currentStage = Stage.MODEL;

    private Resource model;
    private ResultAction resultAction;
    /** Tracks whether any steps have gotten write access to the runtime */
    private boolean affectsRuntime;
    /** Tracks whether we've detected cancellation */
    private boolean cancelled;
    /** Currently executing step */
    private Step activeStep;
    /** The step that acquired the write lock */
    private Step lockStep;
    /** The step that acquired the container monitor  */
    private Step containerMonitorStep;

    enum ContextFlag {
        ROLLBACK_ON_FAIL,
        ALLOW_RESOURCE_SERVICE_RESTART,
    }

    OperationContextImpl(final ModelControllerImpl modelController, final Type contextType, final EnumSet<ContextFlag> contextFlags,
                         final OperationMessageHandler messageHandler, final OperationAttachments attachments,
                         final Resource model, final ModelController.OperationTransactionControl transactionControl,
                         final ControlledProcessState processState, final boolean booting) {
        this.contextType = contextType;
        this.transactionControl = transactionControl;
        this.booting = booting;
        this.model = model;
        this.modelController = modelController;
        this.messageHandler = messageHandler;
        this.attachments = attachments;
        this.processState = processState;
        steps = new EnumMap<Stage, Deque<Step>>(Stage.class);
        for (Stage stage : Stage.values()) {
            steps.put(stage, new ArrayDeque<Step>());
        }
        affectsModel = new HashSet<PathAddress>(1);
        initiatingThread = Thread.currentThread();
        this.contextFlags = contextFlags;
        serviceTarget = new ContextServiceTarget(modelController);
    }

    public InputStream getAttachmentStream(final int index) {
        if (attachments == null) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return attachments.getInputStreams().get(index);
    }

    public int getAttachmentStreamCount() {
        return attachments == null ? 0 : attachments.getInputStreams().size();
    }

    public void addStep(final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        final ModelNode response = activeStep == null ? new ModelNode().setEmptyObject() : activeStep.response;
        addStep(response, activeStep.operation, activeStep.address, step, stage);
    }

    public void addStep(final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        final ModelNode response = activeStep == null ? new ModelNode().setEmptyObject() : activeStep.response;
        addStep(response, operation, null, step, stage);
    }

    public void addStep(final ModelNode response, final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        addStep(response, operation, null, step, stage);
    }

    private void addStep(final ModelNode response, final ModelNode operation, final PathAddress address,
                         final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        assert Thread.currentThread() == initiatingThread;
        if (response == null) {
            throw new IllegalArgumentException("response is null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("operation is null");
        }
        if (step == null) {
            throw new IllegalArgumentException("step is null");
        }
        if (stage == null) {
            throw new IllegalArgumentException("stage is null");
        }
        if (currentStage == Stage.DONE) {
            throw new IllegalStateException("Operation already complete");
        }
        if (stage.compareTo(currentStage) < 0 && (stage != Stage.IMMEDIATE || currentStage == Stage.DONE)) {
            throw new IllegalStateException("Stage " + stage + " is already complete");
        }
        if (contextType == Type.MANAGEMENT && stage.compareTo(Stage.MODEL) > 0) {
            if(stage != Stage.VERIFY) { // allow verification also in mgmt mode
                throw new IllegalArgumentException("Invalid step stage for this context type");
            }
        }
        if (stage == Stage.DOMAIN && contextType != Type.HOST) {
            throw new IllegalStateException("Stage " + stage + " is not valid for context type " + contextType);
        }
        if (stage == Stage.DONE) {
            throw new IllegalArgumentException("Invalid step stage specified");
        }
        if (stage == Stage.IMMEDIATE) {
            steps.get(currentStage).addFirst(new Step(step, response, operation, address));
        } else {
            steps.get(stage).addLast(new Step(step, response, operation, address));
        }
    }

    public ModelNode getFailureDescription() {
        return activeStep.response.get(FAILURE_DESCRIPTION);
    }

    public boolean hasFailureDescription() {
        return activeStep.response.has(FAILURE_DESCRIPTION);
    }

    public ResultAction completeStep() {
        try {
            doCompleteStep();
            if (resultAction == ResultAction.KEEP) {
                report(MessageSeverity.INFO, "Operation succeeded, committing");
            } else {
                report(MessageSeverity.INFO, "Operation rolling back");
            }
            return resultAction;
        } finally {
            respectInterruption = false;
        }
    }

    public void completeStep(RollbackHandler rollbackHandler) {
        if (rollbackHandler == null) {
            throw new IllegalArgumentException("rollbackHandler is null");
        }
        this.activeStep.rollbackHandler = rollbackHandler;
        // we return and executeStep picks it up
    }

    /**
     * Perform the work of completing a step.
     *
     * @return the result action for the step which has just completed
     */
    private void doCompleteStep() {

        assert Thread.currentThread() == initiatingThread;
        // If someone called this when the operation is done, fail.
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }

        // If previous steps have put us in a state where we shouldn't do any more, just stop
        if (!canContinueProcessing()) {
            respectInterruption = false;
            return;
        }

        // Locate the next step to execute.
        ModelNode response = activeStep == null ? null : activeStep.response;
        Step step = null;
        do {
            step = steps.get(currentStage).pollFirst();
            if (step == null) {
                // No steps remain in this stage; proceed to the next stage.
                if (currentStage.hasNext()) {
                    currentStage = currentStage.next();
                    if (affectsRuntime && currentStage == Stage.VERIFY) {
                        // a change was made to the runtime.  Thus, we must wait for stability before resuming in to verify.
                        ContainerStateMonitor.ContainerStateChangeReport changeReport;
                        try {
                            // First wait until any removals we've initiated have begun processing, otherwise
                            // the ContainerStateMonitor may not have gotten the notification causing it to untick
                            final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                            synchronized (map) {
                                while (!map.isEmpty()) {
                                    map.wait();
                                }
                            }
                            changeReport = modelController.awaitContainerStateChangeReport(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            cancelled = true;
                            if (response != null) {
                                response.get(OUTCOME).set(CANCELLED);
                                response.get(FAILURE_DESCRIPTION).set("Operation cancelled");
                                response.get(ROLLED_BACK).set(true);
                            }
                            resultAction = ResultAction.ROLLBACK;
                            return;
                        }
                        // If any services are missing, add a verification handler to see if we caused it
                        if (changeReport != null && !changeReport.getMissingServices().isEmpty()) {
                            ServiceRemovalVerificationHandler removalVerificationHandler = new ServiceRemovalVerificationHandler(changeReport);
                            addStep(new ModelNode(), new ModelNode(), PathAddress.EMPTY_ADDRESS, removalVerificationHandler, Stage.VERIFY);
                        }
                    }
                }
            } else {
                executeStep(step);
                if (step.rollbackHandler == null) {
                    // A recursive step executed
                    return;
                } else {
                    // A non-recursive step executed
                    // See if it put us in a state where we shouldn't do any more
                    if (!canContinueProcessing()) {
                        // We're done. Do the cleanup that would happen in executeStep's finally block
                        // if this was a recursive step
                        respectInterruption = false;
                        step.finalizeStep();
                        return;
                    } else {
                        // else move on to next step
                        response = activeStep.response;
                    }
                }
            }
        } while (currentStage != Stage.DONE);

        // All steps are completed withuout triggering rollback; time for final processing

        // Prepare persistence of any configuration changes
        ConfigurationPersister.PersistenceResource persistenceResource = null;
        if (isModelAffected() && resultAction != ResultAction.ROLLBACK) {
            try {
                persistenceResource = modelController.writeModel(model, affectsModel);
            } catch (ConfigurationPersistenceException e) {
                response.get(OUTCOME).set(FAILED);
                log.errorf(e, "Failed to persist configuration change");
                response.get(FAILURE_DESCRIPTION).set("Failed to persist configuration change: " + e);
                resultAction = ResultAction.ROLLBACK;
                return;
            }
        }

        // Allow any containing TransactionControl to vote
        final AtomicReference<ResultAction> ref = new AtomicReference<ResultAction>(transactionControl == null ? ResultAction.KEEP : ResultAction.ROLLBACK);
        if (transactionControl != null) {
            if (log.isTraceEnabled()) {
                log.trace("Prepared response is " + response);
            }
            transactionControl.operationPrepared(new ModelController.OperationTransaction() {
                public void commit() {
                    ref.set(ResultAction.KEEP);
                }

                public void rollback() {
                    ref.set(ResultAction.ROLLBACK);
                }
            }, response);
        }
        resultAction = ref.get();

        // Commit the persistence of any configuration changes
        if (persistenceResource != null) {
            if (resultAction == ResultAction.ROLLBACK) {
                persistenceResource.rollback();
            } else {
                persistenceResource.commit();
            }
        }
    }

    private boolean canContinueProcessing() {

        // Cancellation is detected via interruption.
        if (Thread.currentThread().isInterrupted()) {
            cancelled = true;
        }
        // Rollback when any of:
        // 1. operation is cancelled
        // 2. operation failed in model phase
        // 3. operation failed in runtime/verify and rollback_on_fail is set
        // 4. isRollbackOnly
        if (cancelled) {
            if (activeStep != null) {
                activeStep.response.get(OUTCOME).set(CANCELLED);
                activeStep.response.get(FAILURE_DESCRIPTION).set("Operation cancelled");
                activeStep.response.get(ROLLED_BACK).set(true);
            }
            resultAction = ResultAction.ROLLBACK;
        }
        else if (activeStep != null && activeStep.response.hasDefined(FAILURE_DESCRIPTION) && (contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL) || currentStage == Stage.MODEL)) {
            activeStep.response.get(OUTCOME).set(FAILED);
            activeStep.response.get(ROLLED_BACK).set(true);
            resultAction = ResultAction.ROLLBACK;
        }
        return resultAction != ResultAction.ROLLBACK;
    }

    private void executeStep(final Step step) {

        step.predecessor = this.activeStep;
        this.activeStep = step;

        try {
            try {
                ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(step.handler.getClass());
                try {
                    step.handler.execute(this, step.operation);
                } finally {
                    SecurityActions.setThreadContextClassLoader(oldTccl);
                }

            } catch (OperationFailedException ofe) {
                if (currentStage != Stage.DONE) {
                    // Handler threw OFE before calling completeStep(); that's equivalent to
                    // a request that we set the failure description and call completeStep()
                    step.response.get(FAILURE_DESCRIPTION).set(ofe.getFailureDescription());
                    log.errorf("Operation (%s) failed - address: (%s) - failure description: %s",
                            step.operation.get(OP), step.operation.get(OP_ADDR), step.response.get(FAILURE_DESCRIPTION));
                    completeStep();
                }
                else {
                    // Handler threw OFE after calling completeStep()
                    // Throw it on and let standard error handling deal with it
                    throw ofe;
                }
            }
        } catch (Throwable t) {
            if (t instanceof StackOverflowError) {
                log.errorf(t, "Operation (%s) failed - address: (%s) -- due to insufficient stack space for the thread used to " +
                        "execute operations. If this error is occurring during server boot, setting " +
                        "system property %s to a value higher than [%d] may resolve this problem.",
                        step.operation.get(OP), step.operation.get(OP_ADDR), AbstractControllerService.BOOT_STACK_SIZE_PROPERTY,
                        AbstractControllerService.DEFAULT_BOOT_STACK_SIZE);
            } else {
                log.errorf(t, "Operation (%s) failed - address: (%s)", step.operation.get(OP), step.operation.get(OP_ADDR));
            }
            // If this block is entered, then the step failed
            // The question is, did it fail before or after calling completeStep()?
            if (currentStage != Stage.DONE) {
                // It failed before, so consider the operation a failure.
                if (! step.response.hasDefined(FAILURE_DESCRIPTION)) {
                    step.response.get(FAILURE_DESCRIPTION).set("Operation handler failed: " + t);
                }
                step.response.get(OUTCOME).set(FAILED);
                resultAction = getFailedResultAction(t);
                if (resultAction == ResultAction.ROLLBACK) {
                    step.response.get(ROLLED_BACK).set(true);
                }
            } else {
                // It failed after!  Just return, ignore the failure
                report(MessageSeverity.WARN, "Step handler " + step.handler + " failed after completion");
            }
        } finally {

            boolean finalize = true;
            if (step.rollbackHandler != null) {
                // A non-recursive step executed
                if (!hasMoreSteps()) {
                    // this step was the last registered step;
                    // go ahead and shift back into recursive mode to wrap things up
                    completeStep();
                } else {
                    // Let doCompleteStep carry on with subsequent steps.
                    // If this step has failed in a way that will prevent subsequent steps running,
                    // doCompleteStep will finalize this step.
                    // Otherwise, some subsequent step will finalize this step
                    finalize = false;
                }
            }

            if (finalize) {
                // We're on the way out on the recursive call path. Finish off this step
                step.finalizeStep();
            }
            // else non-recursive steps get finished off by the succeeding recursive step
        }
    }

    /**
     * Decide whether failure should trigger a rollback.
     *
     * @param cause the cause of the failure, or {@code null} if failure is not the result of catching a throwable
     * @return the result action
     */
    private ResultAction getFailedResultAction(Throwable cause) {
        if (currentStage == Stage.MODEL || cancelled || contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL)
                || isRollbackOnly() || (cause != null && !(cause instanceof OperationFailedException))) {
            return ResultAction.ROLLBACK;
        }
        return ResultAction.KEEP;
    }

    public Type getType() {
        assert Thread.currentThread() == initiatingThread;
        return contextType;
    }

    public boolean isBooting() {
        return booting;
    }

    public boolean isRollbackOnly() {
        return resultAction == ResultAction.ROLLBACK;
    }

    public void setRollbackOnly() {
        resultAction = ResultAction.ROLLBACK;
    }

    private boolean isRollingBack() {
        return currentStage == Stage.DONE && resultAction == ResultAction.ROLLBACK;
    }

    @Override
    public boolean isRollbackOnRuntimeFailure() {
        return contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL);
    }

    @Override
    public boolean isResourceServiceRestartAllowed() {
        return contextFlags.contains(ContextFlag.ALLOW_RESOURCE_SERVICE_RESTART);
    }

    @Override
    public void reloadRequired() {
        if (processState.isReloadSupported()) {
            activeStep.restartStamp = processState.setReloadRequired();
            activeStep.response.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).set(true);
        } else {
            restartRequired();
        }
    }

    @Override
    public void restartRequired() {
        activeStep.restartStamp = processState.setRestartRequired();
        activeStep.response.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RESTART).set(true);
    }

    @Override
    public void revertReloadRequired() {
        if (processState.isReloadSupported()) {
            processState.revertReloadRequired(this.activeStep.restartStamp);
            if (activeStep.response.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RELOAD)) {
                activeStep.response.get(RESPONSE_HEADERS).remove(OPERATION_REQUIRES_RELOAD);
                if (activeStep.response.get(RESPONSE_HEADERS).asInt() == 0) {
                    activeStep.response.remove(RESPONSE_HEADERS);
                }
            }
        }
        else {
            revertRestartRequired();
        }
    }

    @Override
    public void revertRestartRequired() {
        processState.revertRestartRequired(this.activeStep.restartStamp);
        if (activeStep.response.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RESTART)) {
            activeStep.response.get(RESPONSE_HEADERS).remove(OPERATION_REQUIRES_RESTART);
            if (activeStep.response.get(RESPONSE_HEADERS).asInt() == 0) {
                activeStep.response.remove(RESPONSE_HEADERS);
            }
        }
    }

    @Override
    public void runtimeUpdateSkipped() {
        activeStep.response.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).set(true);
    }


    public ManagementResourceRegistration getResourceRegistrationForUpdate() {
        final PathAddress address = activeStep.address;
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (!affectsResourceRegistration) {
            takeWriteLock();
            affectsResourceRegistration = true;
        }
        return modelController.getRootRegistration().getSubModel(address);
    }


    public ImmutableManagementResourceRegistration getResourceRegistration() {
        final PathAddress address = activeStep.address;
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null || currentStage == Stage.DONE) {
            throw new IllegalStateException("Operation already complete");
        }
        ImmutableManagementResourceRegistration delegate = modelController.getRootRegistration().getSubModel(address);
        return delegate == null ? null : new DelegatingImmutableManagementResourceRegistration(delegate);
    }

    public ServiceRegistry getServiceRegistry(final boolean modify) throws UnsupportedOperationException {
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (! (!modify || currentStage == Stage.RUNTIME || currentStage == Stage.MODEL || currentStage == Stage.VERIFY || isRollingBack())) {
            throw new IllegalStateException("Get service registry only supported in runtime operations");
        }
        if (modify && !affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        return modelController.getServiceRegistry();
    }

    public ServiceController<?> removeService(final ServiceName name) throws UnsupportedOperationException {
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY && !isRollingBack()) {
            throw new IllegalStateException("Service removal only supported in runtime operations");
        }
        if (!affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        ServiceController<?> controller = modelController.getServiceRegistry().getService(name);
        if (controller != null) {
            doRemove(controller);
        }
        return controller;
    }

    public void removeService(final ServiceController<?> controller) throws UnsupportedOperationException {
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY && !isRollingBack()) {
            throw new IllegalStateException("Service removal only supported in runtime operations");
        }
        if (!affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        doRemove(controller);
    }

    private void doRemove(final ServiceController<?> controller) {
        final Step removalStep = activeStep;
        controller.addListener(new AbstractServiceListener<Object>() {
            public void listenerAdded(final ServiceController<?> controller) {
                final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                synchronized (map) {
                    map.put(controller.getName(), controller);
                    controller.setMode(ServiceController.Mode.REMOVE);
                }
            }

            public void transition(final ServiceController<?> controller, final ServiceController.Transition transition) {
                switch (transition) {
                    case REMOVING_to_REMOVED:
                    case REMOVING_to_DOWN: {
                        final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                        synchronized (map) {
                            ServiceName name = controller.getName();
                            if (map.get(name) == controller) {
                                map.remove(controller.getName());
                                removalSteps.put(name, removalStep);
                                map.notifyAll();
                            }
                        }
                        break;
                    }
                }
            }
        });
    }

    public ServiceTarget getServiceTarget() throws UnsupportedOperationException {
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY && !isRollingBack()) {
            throw new IllegalStateException("Get service target only supported in runtime operations");
        }
        if (!affectsRuntime) {
            takeWriteLock();
            affectsRuntime = true;
            acquireContainerMonitor();
            awaitContainerMonitor();
        }
        return serviceTarget;
    }

    private void takeWriteLock() {
        if (lockStep == null) {
            if (currentStage == Stage.DONE) {
                throw new IllegalStateException("Invalid modification after completed step");
            }
            try {
                modelController.acquireLock(respectInterruption);
                lockStep = activeStep;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
        }
    }

    private void acquireContainerMonitor() {
        if (containerMonitorStep == null) {
            if (currentStage == Stage.DONE) {
                throw new IllegalStateException("Invalid modification after completed step");
            }
            modelController.acquireContainerMonitor();
            containerMonitorStep = activeStep;
        }
    }

    private void awaitContainerMonitor() {
        try {
            modelController.awaitContainerMonitor(respectInterruption, 1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CancellationException("Operation cancelled asynchronously");
        }
    }

    public ModelNode readModel(final PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        Resource model = this.model;
        for (final PathElement element : address) {
            model = model.requireChild(element);
        }
        // recursively read the model
        return Resource.Tools.readModel(model);
    }

    public ModelNode readModelForUpdate(final PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (affectsModel.size() == 0) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.add(address);
        Resource model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot write to *");
            }
            if (! i.hasNext()) {
                final String key = element.getKey();
                if(! model.hasChild(element)) {
                    final PathAddress parent = address.subAddress(0, address.size() -1);
                    final Set<String> childrenNames = modelController.getRootRegistration().getChildNames(parent);
                    if(!childrenNames.contains(key)) {
                        throw new IllegalStateException("no child-type " + key);
                    }
                    final Resource newModel = Resource.Factory.create();
                    model.registerChild(element, newModel);
                    model = newModel;
                } else {
                    model = model.requireChild(element);
                }
            } else {
                model = model.requireChild(element);
            }
        }
        if(model == null) {
            throw new IllegalStateException();
        }
        return model.getModel();
    }

    public Resource readResource(PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        Resource model = this.model;
        for (final PathElement element : address) {
            model = model.requireChild(element);
        }
        return model.clone();
    }

    public Resource readResourceForUpdate(PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (affectsModel.size() == 0) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.add(address);
        Resource resource = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot write to *");
            }
            resource = resource.requireChild(element);
        }
        return resource;
    }

    public Resource createResource(PathAddress relativeAddress) {
        final Resource toAdd = Resource.Factory.create();
        addResource(relativeAddress, toAdd);
        return toAdd;
    }

    public void addResource(PathAddress relativeAddress, Resource toAdd) {
        final PathAddress absoluteAddress = activeStep.address.append(relativeAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (absoluteAddress.size() == 0) {
            throw new IllegalStateException("Duplicate resource " + absoluteAddress);
        }
        if (affectsModel.size() == 0) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.add(absoluteAddress);
        Resource model = this.model;
        final Iterator<PathElement> i = absoluteAddress.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot write to *");
            }
            if (! i.hasNext()) {
                final String key = element.getKey();
                if(model.hasChild(element)) {
                    throw new IllegalStateException("Duplicate resource " + absoluteAddress);
                } else {
                    final PathAddress parent = absoluteAddress.subAddress(0, absoluteAddress.size() -1);
                    final Set<String> childrenNames = modelController.getRootRegistration().getChildNames(parent);
                    if(!childrenNames.contains(key)) {
                        throw new IllegalStateException("no child-type " + key);
                    }
                    model.registerChild(element, toAdd);
                    model = toAdd;
                }
            } else {
                model = model.getChild(element);
                if (model == null) {
                    PathAddress ancestor = PathAddress.EMPTY_ADDRESS;
                    for (PathElement pe : absoluteAddress) {
                        ancestor = ancestor.append(pe);
                        if (element.equals(pe)) {
                            break;
                        }
                    }
                    throw new IllegalStateException(String.format("Resource %s does not exist; a resource at " +
                            "address %s cannot be created until all ancestor resources have been added", ancestor, absoluteAddress));
                }
            }
        }
    }

    public Resource removeResource(final PathAddress requestAddress) {
        final PathAddress address = activeStep.address.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (affectsModel.size() == 0) {
            takeWriteLock();
            model = model.clone();
        }
        affectsModel.add(address);
        Resource model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot remove *");
            }
            if (! i.hasNext()) {
                model = model.removeChild(element);
            } else {
                model = model.requireChild(element);
            }
        }
        return model;
    }

    public void acquireControllerLock() {
        takeWriteLock();
    }

    public Resource getRootResource() {
        final Resource readOnlyModel = this.model;
        return readOnlyModel.clone();
    }

    public boolean isModelAffected() {
        return affectsModel.size() > 0;
    }

    public boolean isRuntimeAffected() {
        return affectsRuntime;
    }

    public boolean isResourceRegistryAffected() {
        return affectsResourceRegistration;
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public void report(final MessageSeverity severity, final String message) {
        try {
            if(messageHandler != null) {
                messageHandler.handleReport(severity, message);
            }
        } catch (Throwable t) {
            // ignored
        }
    }

    public ModelNode getResult() {
        return activeStep.response.get(RESULT);
    }

    public boolean hasResult() {
        return activeStep.response.has(RESULT);
    }

    private boolean hasMoreSteps() {
        Stage stage = currentStage;
        boolean more = !steps.get(stage).isEmpty();
        while (!more && stage.hasNext()) {
            stage = stage.next();
            more = !steps.get(stage).isEmpty();
        }
        return more;
    }

    private class Step {
        private final OperationStepHandler handler;
        private final ModelNode response;
        private final ModelNode operation;
        private final PathAddress address;
        private Object restartStamp;
        private RollbackHandler rollbackHandler;
        private Step predecessor;

        private Step(final OperationStepHandler handler, final ModelNode response, final ModelNode operation, final PathAddress address) {
            this.handler = handler;
            this.response = response;
            this.operation = operation;
            this.address = address == null ? PathAddress.pathAddress(operation.get(OP_ADDR)) : address;
            // Create the outcome node early so it appears at the top of the response
            response.get(OUTCOME);
        }

        private void finalizeStep() {
            finalizeInternal();
            Step step = this.predecessor;
            while (step != null) {
                if (step.rollbackHandler != null) {
                    step.finalizeInternal();
                    step = step.predecessor;
                } else {
                    OperationContextImpl.this.activeStep = step;
                    break;
                }
            }
        }

        private void finalizeInternal() {

            OperationContextImpl.this.activeStep = this;

            try {
                handleRollback();

                if (currentStage != null && currentStage != Stage.DONE) {
                    // This is a failure because the next step failed to call completeStep().
                    // Either an exception occurred beforehand, or the implementer screwed up.
                    // If an exception occurred, then this will have no effect.
                    // If the implementer screwed up, then we're essentially fixing the context state and treating
                    // the overall operation as a failure.
                    currentStage = currentStage != null ? Stage.DONE : null;
                    if (! response.hasDefined(FAILURE_DESCRIPTION)) {
                        response.get(FAILURE_DESCRIPTION).set("Operation handler failed to complete");
                    }
                    response.get(OUTCOME).set(FAILED);
                    response.get(ROLLED_BACK).set(true);
                    resultAction = getFailedResultAction(null);
                } else if (resultAction == ResultAction.ROLLBACK) {
                    response.get(OUTCOME).set(FAILED);
                    response.get(ROLLED_BACK).set(true);
                } else {
                    response.get(OUTCOME).set(response.hasDefined(FAILURE_DESCRIPTION) ? FAILED : SUCCESS);
                }

            } finally {
                if (OperationContextImpl.this.lockStep == this) {
                    modelController.releaseLock();
                    lockStep = null;
                }
                if (OperationContextImpl.this.containerMonitorStep == this) {
                    awaitContainerMonitor();
                    modelController.releaseContainerMonitor();
                    containerMonitorStep = null;
                }

                if (predecessor == null) {
                    // We're returning from the outermost completeStep()
                    // Null out the current stage to disallow further access to the context
                    currentStage = null;
                }
            }
        }

        private void handleRollback() {
            if (rollbackHandler != null) {
                try {
                    if (resultAction == ResultAction.ROLLBACK) {
                        ClassLoader oldTccl = SecurityActions.setThreadContextClassLoader(handler.getClass());
                        try {
                            rollbackHandler.handleRollback(OperationContextImpl.this, operation);
                        } finally {
                            SecurityActions.setThreadContextClassLoader(oldTccl);
                        }
                    }
                } catch (Exception e) {
                    report(MessageSeverity.ERROR, String.format("Step handler %s for operation %s at address %s " +
                            "failed handling operation rollback -- %s", handler, operation.get(OP).asString(), address, e));
                } finally {
                    // Clear the rollback handler so we never try and finalize this step again
                    rollbackHandler = null;
                }
            }
        }

    }

    class ContextServiceTarget implements ServiceTarget {

        private final ModelControllerImpl modelController;

        ContextServiceTarget(final ModelControllerImpl modelController) {
            this.modelController = modelController;
        }

        public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) {
            final ServiceBuilder<T> realBuilder = modelController.getServiceTarget().addServiceValue(name, value);
            return new ContextServiceBuilder<T>(realBuilder, name);
        }

        public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) {
            return addServiceValue(name, new ImmediateValue<Service<T>>(service));
        }

        public ServiceTarget addListener(final ServiceListener<Object> listener) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final ServiceListener<Object>... listeners) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final Collection<ServiceListener<Object>> listeners) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final ServiceListener.Inheritance inheritance, final ServiceListener<Object> listener) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final ServiceListener.Inheritance inheritance, final ServiceListener<Object>... listeners) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addListener(final ServiceListener.Inheritance inheritance, final Collection<ServiceListener<Object>> listeners) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget removeListener(final ServiceListener<Object> listener) {
            throw new UnsupportedOperationException();
        }

        public Set<ServiceListener<Object>> getListeners() {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addDependency(final ServiceName dependency) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addDependency(final ServiceName... dependencies) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget addDependency(final Collection<ServiceName> dependencies) {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget removeDependency(final ServiceName dependency) {
            throw new UnsupportedOperationException();
        }

        public Set<ServiceName> getDependencies() {
            throw new UnsupportedOperationException();
        }

        public ServiceTarget subTarget() {
            throw new UnsupportedOperationException();
        }

        public BatchServiceTarget batchTarget() {
            throw new UnsupportedOperationException();
        }
    }

    class ContextServiceBuilder<T> implements ServiceBuilder<T> {

        private final ServiceBuilder<T> realBuilder;
        private final ServiceName name;

        ContextServiceBuilder(final ServiceBuilder<T> realBuilder, final ServiceName name) {
            this.realBuilder = realBuilder;
            this.name = name;
        }

        public ServiceBuilder<T> addAliases(final ServiceName... aliases) {
            realBuilder.addAliases(aliases);
            return this;
        }

        public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
            realBuilder.setInitialMode(mode);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final ServiceName... dependencies) {
            realBuilder.addDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final ServiceName... dependencies) {
            realBuilder.addDependencies(dependencyType, dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> dependencies) {
            realBuilder.addDependencies(dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final Iterable<ServiceName> dependencies) {
            realBuilder.addDependencies(dependencyType, dependencies);
            return this;
        }

        public ServiceBuilder<T> addDependency(final ServiceName dependency) {
            realBuilder.addDependency(dependency);
            return this;
        }

        public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency) {
            realBuilder.addDependency(dependencyType, dependency);
            return this;
        }

        public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
            realBuilder.addDependency(dependency, target);
            return this;
        }

        public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Injector<Object> target) {
            realBuilder.addDependency(dependencyType, dependency, target);
            return this;
        }

        public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            realBuilder.addDependency(dependency, type, target);
            return this;
        }

        public <I> ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            realBuilder.addDependency(dependencyType, dependency, type, target);
            return this;
        }

        public <I> ServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
            realBuilder.addInjection(target, value);
            return this;
        }

        public <I> ServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
            realBuilder.addInjectionValue(target, value);
            return this;
        }

        public ServiceBuilder<T> addInjection(final Injector<? super T> target) {
            realBuilder.addInjection(target);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener<? super T> listener) {
            realBuilder.addListener(listener);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener<? super T>... listeners) {
            realBuilder.addListener(listeners);
            return this;
        }

        public ServiceBuilder<T> addListener(final Collection<? extends ServiceListener<? super T>> listeners) {
            realBuilder.addListener(listeners);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener.Inheritance inheritance, final ServiceListener<? super T> listener) {
            realBuilder.addListener(inheritance, listener);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener.Inheritance inheritance, final ServiceListener<? super T>... listeners) {
            realBuilder.addListener(inheritance, listeners);
            return this;
        }

        public ServiceBuilder<T> addListener(final ServiceListener.Inheritance inheritance, final Collection<? extends ServiceListener<? super T>> listeners) {
            realBuilder.addListener(inheritance, listeners);
            return this;
        }

        public ServiceController<T> install() throws ServiceRegistryException, IllegalStateException {
            final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
            synchronized (map) {
                // Wait for removal to complete
                while (map.containsKey(name)) try {
                    map.wait();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new CancellationException("Service install was cancelled");
                }
                boolean intr = false;
                try {
                    while (map.containsKey(name)) try {
                        map.wait();

                        // If a step removed this ServiceName before, it's no longer responsible
                        // for any ill effect
                        removalSteps.remove(name);

                        return realBuilder.install();
                    } catch (InterruptedException e) {
                        if (respectInterruption) {
                            Thread.currentThread().interrupt();
                            throw new CancellationException("Service install was cancelled");
                        } else {
                            intr = true;
                        }
                    }
                } finally {
                    if (intr) {
                        Thread.currentThread().interrupt();
                    }
                }

                // If a step removed this ServiceName before, it's no longer responsible
                // for any ill effect
                removalSteps.remove(name);

                return realBuilder.install();
            }
        }
    }

    /** Verifies that any service removals performed by this operation did not trigger a missing dependency */
    private class ServiceRemovalVerificationHandler implements OperationStepHandler {

        private final ContainerStateMonitor.ContainerStateChangeReport containerStateChangeReport;

        private ServiceRemovalVerificationHandler(ContainerStateMonitor.ContainerStateChangeReport containerStateChangeReport) {
            this.containerStateChangeReport = containerStateChangeReport;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {


            final Map<Step, Map<ServiceName, Set<ServiceName>>> missingByStep = new HashMap<Step, Map<ServiceName, Set<ServiceName>>>();
            // The realRemovingControllers map acts as the guard for the removalSteps map
            Object mutex = realRemovingControllers;
            synchronized (mutex) {
                for (Map.Entry<ServiceName, ContainerStateMonitor.MissingDependencyInfo> entry : containerStateChangeReport.getMissingServices().entrySet()) {
                    ContainerStateMonitor.MissingDependencyInfo missingDependencyInfo = entry.getValue();
                    Step removalStep = removalSteps.get(entry.getKey());
                    if (removalStep != null) {
                        Map<ServiceName, Set<ServiceName>> stepBadRemovals = missingByStep.get(removalStep);
                        if (stepBadRemovals == null) {
                            stepBadRemovals = new HashMap<ServiceName, Set<ServiceName>>();
                            missingByStep.put(removalStep, stepBadRemovals);
                        }
                        stepBadRemovals.put(entry.getKey(), missingDependencyInfo.getDependents());
                    }
                }
            }

            for (Map.Entry<Step, Map<ServiceName, Set<ServiceName>>> entry : missingByStep.entrySet()) {
                Step step = entry.getKey();
                if (!step.response.hasDefined(FAILURE_DESCRIPTION)) {
                    StringBuilder sb = new StringBuilder("Removing services has lead to unsatisfied dependencies:");
                    for (Map.Entry<ServiceName, Set<ServiceName>> removed : entry.getValue().entrySet()) {
                        sb.append("\nService ");
                        sb.append(removed.getKey().getCanonicalName());
                        sb.append(" was depended upon by ");
                        boolean first = true;
                        for (ServiceName dependent : removed.getValue()) {
                            if (!first) {
                                sb.append(", ");
                            } else {
                                first = false;
                            }
                            sb.append(dependent);
                        }
                    }
                    step.response.get(FAILURE_DESCRIPTION).set(sb.toString());
                }
                // else a handler already recorded a failure; don't overwrite
            }

            if (!missingByStep.isEmpty() && context.isRollbackOnRuntimeFailure()) {
                context.setRollbackOnly();
            }
            context.completeStep();
        }
    }
}
