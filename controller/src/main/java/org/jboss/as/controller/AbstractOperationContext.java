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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CALLER_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_UPDATE_SKIPPED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.dmr.ModelNode;

/**
 * Operation context implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class AbstractOperationContext implements OperationContext {

    static final ThreadLocal<Thread> controllingThread = new ThreadLocal<Thread>();

    private final Type contextType;
    final Thread initiatingThread;
    private final EnumMap<Stage, Deque<Step>> steps;
    private final ModelController.OperationTransactionControl transactionControl;
    private final ControlledProcessState processState;
    private final boolean booting;
    private final ProcessType processType;
    private final RunningMode runningMode;

    // We only respect interruption on the way in; once we complete all steps and begin
    // returning, any calls that can throw InterruptedException are converted to
    // an uninterruptible form. This is to ensure rollback changes are not interrupted
    boolean respectInterruption = true;

    Stage currentStage = Stage.MODEL;

    ResultAction resultAction;
    /** Tracks whether we've detected cancellation */
    boolean cancelled;
    /** Currently executing step */
    Step activeStep;

    enum ContextFlag {
        ROLLBACK_ON_FAIL,
        ALLOW_RESOURCE_SERVICE_RESTART,
    }

    AbstractOperationContext(final ProcessType processType,
                             final RunningMode runningMode,
                             final ModelController.OperationTransactionControl transactionControl,
                             final ControlledProcessState processState, boolean booting) {
        this.processType = processType;
        this.runningMode = runningMode;
        this.contextType = Type.getType(processType, runningMode);
        this.transactionControl = transactionControl;
        this.processState = processState;
        this.booting = booting;
        steps = new EnumMap<Stage, Deque<Step>>(Stage.class);
        for (Stage stage : Stage.values()) {
            if (booting && stage == Stage.VERIFY) {
                // Use a concurrent structure as the parallel boot threads will concurrently add steps
                steps.put(stage, new LinkedBlockingDeque<Step>());
            } else {
                steps.put(stage, new ArrayDeque<Step>());
            }
        }
        initiatingThread = Thread.currentThread();
    }

    @Override
    public boolean isBooting() {
        return booting;
    }

    public void addStep(final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        addStep(step, stage, false);
    }

    public void addStep(final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        final ModelNode response = activeStep == null ? new ModelNode().setEmptyObject() : activeStep.response;
        addStep(response, operation, null, step, stage);
    }

    public void addStep(final ModelNode response, final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        addStep(response, operation, null, step, stage);
    }

    @Override
    public void addStep(OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
        final ModelNode response = activeStep == null ? new ModelNode().setEmptyObject() : activeStep.response;
        addStep(response, activeStep.operation, activeStep.address, step, stage, addFirst);
    }

    void addStep(final ModelNode response, final ModelNode operation, final PathAddress address,
                         final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        addStep(response, operation, address, step, stage, false);
    }

    @Override
    public void addStep(ModelNode response, ModelNode operation, OperationStepHandler step, Stage stage, boolean addFirst) throws IllegalArgumentException {
        addStep(response, operation, null, step, stage, addFirst);
    }

    void addStep(final ModelNode response, final ModelNode operation, final PathAddress address,
                         final OperationStepHandler step, final Stage stage, boolean addFirst) throws IllegalArgumentException {
        assert isControllingThread();
        if (response == null) {
            throw MESSAGES.nullVar("response");
        }
        if (operation == null) {
            throw MESSAGES.nullVar("operation");
        }
        if (step == null) {
            throw MESSAGES.nullVar("step");
        }
        if (stage == null) {
            throw MESSAGES.nullVar("stage");
        }
        if (currentStage == Stage.DONE) {
            throw MESSAGES.operationAlreadyComplete();
        }
        if (stage.compareTo(currentStage) < 0 && (stage != Stage.IMMEDIATE || currentStage == Stage.DONE)) {
            throw MESSAGES.stageAlreadyComplete(stage);
        }
        if (stage == Stage.DOMAIN && contextType != Type.HOST) {
            throw MESSAGES.invalidStage(stage, contextType);
        }
        if (stage == Stage.DONE) {
            throw MESSAGES.invalidStepStage();
        }
        if (!booting && activeStep != null) {
            // Added steps inherit the caller type of their parent
            if (activeStep.operation.hasDefined(OPERATION_HEADERS) && activeStep.operation.get(OPERATION_HEADERS).hasDefined(CALLER_TYPE)) {
                operation.get(OPERATION_HEADERS, CALLER_TYPE).set(activeStep.operation.get(OPERATION_HEADERS, CALLER_TYPE));
            }
        }
        if (stage == Stage.IMMEDIATE) {
            steps.get(currentStage).addFirst(new Step(step, response, operation, address));
        } else {
            final Deque<Step> deque = steps.get(stage);
            if(addFirst) {
                deque.addFirst(new Step(step, response, operation, address));
            } else {
                deque.addLast(new Step(step, response, operation, address));
            }
        }
    }

    public final ModelNode getFailureDescription() {
        return activeStep.response.get(FAILURE_DESCRIPTION);
    }

    public final boolean hasFailureDescription() {
        return activeStep.response.has(FAILURE_DESCRIPTION);
    }

    public final ResultAction completeStep() {
        try {
            doCompleteStep();
            if (resultAction == ResultAction.KEEP) {
                report(MessageSeverity.INFO, MESSAGES.operationSucceeded());
            } else {
                report(MessageSeverity.INFO, MESSAGES.operationRollingBack());
            }
            return resultAction;
        } finally {
            respectInterruption = false;
        }
    }

    public final void completeStep(RollbackHandler rollbackHandler) {
        if (rollbackHandler == null) {
            throw MESSAGES.nullVar("rollbackHandler");
        }
        this.activeStep.rollbackHandler = rollbackHandler;
        // we return and executeStep picks it up
    }

    /**
     * Perform the work of completing a step.
     */
    private void doCompleteStep() {

        assert isControllingThread();
        // If someone called this when the operation is done, fail.
        if (currentStage == null) {
            throw MESSAGES.operationAlreadyComplete();
        }

        // If previous steps have put us in a state where we shouldn't do any more, just stop
        if (!canContinueProcessing()) {
            respectInterruption = false;
            return;
        }

        // Locate the next step to execute.
        ModelNode response = activeStep == null ? null : activeStep.response;
        Step step;
        do {
            step = steps.get(currentStage).pollFirst();
            if (step == null) {
                // No steps remain in this stage; proceed to the next stage.
                if (currentStage.hasNext()) {
                    currentStage = currentStage.next();
                    if (currentStage == Stage.VERIFY) {
                        // a change was made to the runtime.  Thus, we must wait for stability before resuming in to verify.
                        try {
                            awaitModelControllerContainerMonitor();
                        } catch (InterruptedException e) {
                            cancelled = true;
                            if (response != null) {
                                response.get(OUTCOME).set(CANCELLED);
                                response.get(FAILURE_DESCRIPTION).set(MESSAGES.operationCancelled());
                                response.get(ROLLED_BACK).set(true);
                            }
                            resultAction = ResultAction.ROLLBACK;
                            respectInterruption = false;
                            Thread.currentThread().interrupt();
                            if (activeStep != null && activeStep.rollbackHandler != null) {
                                // Finalize
                                activeStep.finalizeStep(null);
                            }
                            return;
                        }
                    }
                }
            } else {
                // Execute the step, but make sure we always finalize any steps
                Throwable toThrow = null;
                try {
                    executeStep(step);
                } catch (RuntimeException re) {
                    toThrow = re;
                } catch (Error e) {
                    toThrow = e;
                } finally {
                    if (step.rollbackHandler == null) {
                        // A recursive step executed
                        throwThrowable(toThrow);
                        return;
                    } else {
                        // A non-recursive step executed
                        // See if it put us in a state where we shouldn't do any more
                        if (!canContinueProcessing()) {
                            // We're done. Do the cleanup that would happen in executeStep's finally block
                            // if this was a recursive step
                            respectInterruption = false;
                            step.finalizeStep(toThrow);
                            return;
                        } else {
                            throwThrowable(toThrow);
                            // else move on to next step
                            response = activeStep.response;
                        }
                    }
                }
            }
        } while (currentStage != Stage.DONE);

        // All steps are completed withuout triggering rollback; time for final processing

        // Prepare persistence of any configuration changes
        ConfigurationPersister.PersistenceResource persistenceResource = null;
        if (isModelAffected() && resultAction != ResultAction.ROLLBACK) {
            try {
                persistenceResource = createPersistenceResource();
            } catch (ConfigurationPersistenceException e) {
                MGMT_OP_LOGGER.failedToPersistConfigurationChange(e);
                if (response != null) {
                    response.get(OUTCOME).set(FAILED);
                    response.get(FAILURE_DESCRIPTION).set(MESSAGES.failedToPersistConfigurationChange(e.getLocalizedMessage()));
                }
                resultAction = ResultAction.ROLLBACK;
                return;
            }
        }

        // Allow any containing TransactionControl to vote
        final AtomicReference<ResultAction> ref = new AtomicReference<ResultAction>(transactionControl == null ? ResultAction.KEEP : ResultAction.ROLLBACK);
        if (transactionControl != null) {
            if (MGMT_OP_LOGGER.isTraceEnabled()) {
                MGMT_OP_LOGGER.trace("Prepared response is " + response);
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

    abstract void awaitModelControllerContainerMonitor() throws InterruptedException;
    abstract ConfigurationPersister.PersistenceResource createPersistenceResource() throws ConfigurationPersistenceException;

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
                activeStep.response.get(FAILURE_DESCRIPTION).set(MESSAGES.operationCancelled());
                activeStep.response.get(ROLLED_BACK).set(true);
            }
            resultAction = ResultAction.ROLLBACK;
        }
        else if (activeStep != null && activeStep.response.hasDefined(FAILURE_DESCRIPTION) && (isRollbackOnRuntimeFailure() || currentStage == Stage.MODEL)) {
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

            } catch (Throwable t) {
                // Special handling for OperationClientException marker interface
                if (! (t instanceof OperationClientException)) {
                    throw t;
                } else if (currentStage != Stage.DONE) {
                    // Handler threw OCE before calling completeStep(); that's equivalent to
                    // a request that we set the failure description and call completeStep()
                    final ModelNode failDesc = OperationClientException.class.cast(t).getFailureDescription();
                    step.response.get(FAILURE_DESCRIPTION).set(failDesc);
                    if (isBooting()) {
                        // An OCE on boot needs to be logged as an ERROR
                        MGMT_OP_LOGGER.operationFailed(step.operation.get(OP), step.operation.get(OP_ADDR), step.response.get(FAILURE_DESCRIPTION));
                    } else {
                        // An OCE post-boot is a client-side mistake and is logged at DEBUG
                        MGMT_OP_LOGGER.operationFailedOnClientError(step.operation.get(OP), step.operation.get(OP_ADDR), step.response.get(FAILURE_DESCRIPTION));
                    }
                    completeStep();
                } else {
                    // Handler threw OCE after calling completeStep()
                    // Throw it on and let standard error handling deal with it
                    throw t;
                }
            }
        } catch (Throwable t) {
            if (t instanceof StackOverflowError) {
                MGMT_OP_LOGGER.operationFailed(t, step.operation.get(OP), step.operation.get(OP_ADDR), AbstractControllerService.BOOT_STACK_SIZE_PROPERTY,
                        AbstractControllerService.DEFAULT_BOOT_STACK_SIZE);
            } else {
                MGMT_OP_LOGGER.operationFailed(t, step.operation.get(OP), step.operation.get(OP_ADDR));
            }
            // If this block is entered, then the step failed
            // The question is, did it fail before or after calling completeStep()?
            if (currentStage != Stage.DONE) {
                // It failed before, so consider the operation a failure.
                if (! step.response.hasDefined(FAILURE_DESCRIPTION)) {
                    step.response.get(FAILURE_DESCRIPTION).set(MESSAGES.operationHandlerFailed(t.getLocalizedMessage()));
                }
                step.response.get(OUTCOME).set(FAILED);
                resultAction = getFailedResultAction(t);
                if (resultAction == ResultAction.ROLLBACK) {
                    step.response.get(ROLLED_BACK).set(true);
                }
            } else {
                // It failed after!  Just return, ignore the failure
                report(MessageSeverity.WARN, MESSAGES.stepHandlerFailed(step.handler));
            }
        } finally {

            finishStep(step);
        }
    }

    private void finishStep(Step step) {
        boolean finalize = true;
        Throwable toThrow = null;
        try {
            if (step.rollbackHandler != null) {
                // A non-recursive step executed
                if (!hasMoreSteps()) {
                    // this step was the last registered step;
                    // go ahead and shift back into recursive mode to wrap things up
                    completeStep();
                } else {
                    // Let doCompleteStep carry on with subsequent steps.
                    // If this step has failed in a way that will prevent subsequent steps running,
                    // and doCompleteStep will finalize this step.
                    // Otherwise, some subsequent step will finalize this step
                    finalize = false;
                }
            }
        } catch (RuntimeException re) {
            toThrow = re;
        } catch (Error e) {
            toThrow = e;
        } finally {
            if (finalize) {
                // We're on the way out on the recursive call path. Finish off this step.
                // Any throwable we caught will get rethrown by this call
                step.finalizeStep(toThrow);
            } else {
                // non-recursive steps get finished off by the succeeding recursive step
                // Throw on toThrow if it's not null; otherwise just return
                throwThrowable(toThrow);
            }
        }
    }

    private static void throwThrowable(Throwable toThrow) {
        if (toThrow != null) {
            if (toThrow instanceof RuntimeException) {
                throw (RuntimeException) toThrow;
            } else {
                throw (Error) toThrow;
            }
        }
    }

    /**
     * Decide whether failure should trigger a rollback.
     *
     * @param cause the cause of the failure, or {@code null} if failure is not the result of catching a throwable
     * @return the result action
     */
    private ResultAction getFailedResultAction(Throwable cause) {
        if (currentStage == Stage.MODEL || cancelled || isRollbackOnRuntimeFailure()
                || isRollbackOnly() || (cause != null && !(cause instanceof OperationFailedException))) {
            return ResultAction.ROLLBACK;
        }
        return ResultAction.KEEP;
    }

    public final ProcessType getProcessType() {
        return processType;
    }

    public final RunningMode getRunningMode() {
        return runningMode;
    }

    @SuppressWarnings("deprecation")
    public final Type getType() {
        return contextType;
    }

    public final boolean isNormalServer() {
        return processType.isServer() && runningMode == RunningMode.NORMAL;
    }

    public final boolean isRollbackOnly() {
        return resultAction == ResultAction.ROLLBACK;
    }

    public final void setRollbackOnly() {
        resultAction = ResultAction.ROLLBACK;
    }

    final boolean isRollingBack() {
        return currentStage == Stage.DONE && resultAction == ResultAction.ROLLBACK;
    }

    @Override
    public final void reloadRequired() {
        if (processState.isReloadSupported()) {
            activeStep.restartStamp = processState.setReloadRequired();
            activeStep.response.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).set(true);
        } else {
            restartRequired();
        }
    }

    @Override
    public final void restartRequired() {
        activeStep.restartStamp = processState.setRestartRequired();
        activeStep.response.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RESTART).set(true);
    }

    @Override
    public final void revertReloadRequired() {
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
    public final void revertRestartRequired() {
        processState.revertRestartRequired(this.activeStep.restartStamp);
        if (activeStep.response.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RESTART)) {
            activeStep.response.get(RESPONSE_HEADERS).remove(OPERATION_REQUIRES_RESTART);
            if (activeStep.response.get(RESPONSE_HEADERS).asInt() == 0) {
                activeStep.response.remove(RESPONSE_HEADERS);
            }
        }
    }

    @Override
    public final void runtimeUpdateSkipped() {
        activeStep.response.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).set(true);
    }

    public final ModelNode getResult() {
        return activeStep.response.get(RESULT);
    }

    public final boolean hasResult() {
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

    boolean isControllingThread() {
        return Thread.currentThread() == initiatingThread || controllingThread.get() == initiatingThread;
    }

    abstract void releaseStepLocks(Step step);

    class Step {
        private final OperationStepHandler handler;
        final ModelNode response;
        final ModelNode operation;
        final PathAddress address;
        private Object restartStamp;
        private RollbackHandler rollbackHandler;
        Step predecessor;

        private Step(final OperationStepHandler handler, final ModelNode response, final ModelNode operation, final PathAddress address) {
            this.handler = handler;
            this.response = response;
            this.operation = operation;
            this.address = address == null ? PathAddress.pathAddress(operation.get(OP_ADDR)) : address;
            // Create the outcome node early so it appears at the top of the response
            response.get(OUTCOME);
        }

        /**
         * Perform any rollback needed to reverse this step (if this context is rolling back),
         * and release any locks taken by this step.
         *
         * @param toThrow  RuntimeException or Error to throw when done; may be {@code null}
         */
        private void finalizeStep(Throwable toThrow) {
            try {
                finalizeInternal();
            } catch (RuntimeException t) {
                if (toThrow == null) {
                    toThrow = t;
                }
            } catch (Error t) {
                if (toThrow == null) {
                    toThrow = t;
                }
            }

            Step step = this.predecessor;
            while (step != null) {
                if (step.rollbackHandler != null) {
                    try {
                        step.finalizeInternal();
                    } catch (RuntimeException t) {
                        if (toThrow == null) {
                            toThrow = t;
                        }
                    } catch (Error t) {
                        if (toThrow == null) {
                            toThrow = t;
                        }
                    }
                    step = step.predecessor;
                } else {
                    AbstractOperationContext.this.activeStep = step;
                    break;
                }
            }

            throwThrowable(toThrow);
        }

        private void finalizeInternal() {

            AbstractOperationContext.this.activeStep = this;

            try {
                handleRollback();

                if (currentStage != null && currentStage != Stage.DONE) {
                    // This is a failure because the next step failed to call completeStep().
                    // Either an exception occurred beforehand, or the implementer screwed up.
                    // If an exception occurred, then this will have no effect.
                    // If the implementer screwed up, then we're essentially fixing the context state and treating
                    // the overall operation as a failure.
                    currentStage = Stage.DONE;
                    if (! response.hasDefined(FAILURE_DESCRIPTION)) {
                        response.get(FAILURE_DESCRIPTION).set(MESSAGES.operationHandlerFailedToComplete());
                    }
                    response.get(OUTCOME).set(cancelled ? CANCELLED : FAILED);
                    response.get(ROLLED_BACK).set(true);
                    resultAction = getFailedResultAction(null);
                } else if (resultAction == ResultAction.ROLLBACK) {
                    response.get(OUTCOME).set(cancelled ? CANCELLED : FAILED);
                    response.get(ROLLED_BACK).set(true);
                } else {
                    response.get(OUTCOME).set(response.hasDefined(FAILURE_DESCRIPTION) ? FAILED : SUCCESS);
                }

            } finally {
                releaseStepLocks(this);

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
                            rollbackHandler.handleRollback(AbstractOperationContext.this, operation);
                        } finally {
                            SecurityActions.setThreadContextClassLoader(oldTccl);
                        }
                    }
                } catch (Exception e) {
                    report(MessageSeverity.ERROR, MESSAGES.stepHandlerFailedRollback(handler, operation.get(OP).asString(), address, e.getLocalizedMessage()));
                } finally {
                    // Clear the rollback handler so we never try and finalize this step again
                    rollbackHandler = null;
                }
            }
        }

    }
}
