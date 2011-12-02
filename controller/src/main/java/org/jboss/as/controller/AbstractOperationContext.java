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

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
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

    boolean respectInterruption = true;

    Stage currentStage = Stage.MODEL;

    ResultAction resultAction;
    /** Tracks whether we've detected cancellation */
    private boolean cancelled;
    /** Currently executing step */
    Step activeStep;

    enum ContextFlag {
        ROLLBACK_ON_FAIL,
        ALLOW_RESOURCE_SERVICE_RESTART,
    }

    AbstractOperationContext(final Type contextType,
                             final ModelController.OperationTransactionControl transactionControl,
                             final ControlledProcessState processState) {
        this.contextType = contextType;
        this.transactionControl = transactionControl;
        this.processState = processState;
        steps = new EnumMap<Stage, Deque<Step>>(Stage.class);
        for (Stage stage : Stage.values()) {
            steps.put(stage, new ArrayDeque<Step>());
        }
        initiatingThread = Thread.currentThread();
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

    void addStep(final ModelNode response, final ModelNode operation, final PathAddress address,
                         final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
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
        if (stage == Stage.IMMEDIATE) {
            steps.get(currentStage).addFirst(new Step(step, response, operation, address));
        } else {
            steps.get(stage).addLast(new Step(step, response, operation, address));
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
                            Thread.currentThread().interrupt();
                            cancelled = true;
                            if (response != null) {
                                response.get(OUTCOME).set(CANCELLED);
                                response.get(FAILURE_DESCRIPTION).set(MESSAGES.operationCancelled());
                                response.get(ROLLED_BACK).set(true);
                            }
                            resultAction = ResultAction.ROLLBACK;
                            return;
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
                persistenceResource = createPersistenceResource();
            } catch (ConfigurationPersistenceException e) {
                ROOT_LOGGER.failedToPersistConfigurationChange(e);
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
            if (ROOT_LOGGER.isTraceEnabled()) {
                ROOT_LOGGER.trace("Prepared response is " + response);
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

            } catch (OperationFailedException ofe) {
                if (currentStage != Stage.DONE) {
                    // Handler threw OFE before calling completeStep(); that's equivalent to
                    // a request that we set the failure description and call completeStep()
                    step.response.get(FAILURE_DESCRIPTION).set(ofe.getFailureDescription());
                    if (isBooting()) {
                        // An OFE on boot needs to be logged as an ERROR
                        ROOT_LOGGER.operationFailed(step.operation.get(OP), step.operation.get(OP_ADDR), step.response.get(FAILURE_DESCRIPTION));
                    } else {
                        // An OFE post-boot is a client-side mistake and is logged at DEBUG
                        ROOT_LOGGER.operationFailedOnClientError(step.operation.get(OP), step.operation.get(OP_ADDR), step.response.get(FAILURE_DESCRIPTION));
                    }
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
                ROOT_LOGGER.operationFailed(t, step.operation.get(OP), step.operation.get(OP_ADDR), AbstractControllerService.BOOT_STACK_SIZE_PROPERTY,
                        AbstractControllerService.DEFAULT_BOOT_STACK_SIZE);
            } else {
                ROOT_LOGGER.operationFailed(t, step.operation.get(OP), step.operation.get(OP_ADDR));
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
        if (currentStage == Stage.MODEL || cancelled || isRollbackOnRuntimeFailure()
                || isRollbackOnly() || (cause != null && !(cause instanceof OperationFailedException))) {
            return ResultAction.ROLLBACK;
        }
        return ResultAction.KEEP;
    }

    public final Type getType() {
        assert isControllingThread();
        return contextType;
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

        private void finalizeStep() {
            finalizeInternal();
            Step step = this.predecessor;
            while (step != null) {
                if (step.rollbackHandler != null) {
                    step.finalizeInternal();
                    step = step.predecessor;
                } else {
                    AbstractOperationContext.this.activeStep = step;
                    break;
                }
            }
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
