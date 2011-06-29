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
    private final boolean booting;
    private final OperationAttachments attachments;
    private final ControlledProcessState processState;
    /** Tracks whether any steps have gotten write access to the model */
    private final Set<PathAddress> affectsModel;
    /** Tracks whether any steps have gotten write access to the management resource registration*/
    private boolean affectsResourceRegistration;

    private boolean respectInterruption = true;
    private PathAddress modelAddress;
    private Stage currentStage = Stage.MODEL;
    /**
     * The result of the current operation.
     */
    private ModelNode response;
    /**
     * The current operation body being executed.
     */
    private ModelNode operation;

    private Resource model;
    private ResultAction resultAction;
    /** Tracks whether any steps have gotten write access to the runtime */
    private boolean affectsRuntime;
    /** Tracks whether we've detected cancellation */
    private boolean cancelled;
    /** Current number of nested levels of completeStep() calls */
    private int depth;
    /** Write lock acquisition depth */
    private int lockDepth;
    /** Container monitor acquisition depth */
    private int containerMonitorDepth;
    /** Stamp to hand back to revert a reload/restartRequired call */
    private StampHolder restartStampHolder;

    enum ContextFlag {
        ROLLBACK_ON_FAIL,
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
        response = new ModelNode().setEmptyObject();
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
        addStep(response, operation, step, stage);
    }

    public void addStep(final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
        addStep(response, operation, step, stage);
    }

    public void addStep(final ModelNode response, final ModelNode operation, final OperationStepHandler step, final Stage stage) throws IllegalArgumentException {
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
            throw new IllegalArgumentException("Invalid step stage for this context type");
        }
        if (stage == Stage.DOMAIN && contextType != Type.HOST) {
            throw new IllegalStateException("Stage " + stage + " is not valid for context type " + contextType);
        }
        if (stage == Stage.DONE) {
            throw new IllegalArgumentException("Invalid step stage specified");
        }
        if (stage == Stage.IMMEDIATE) {
            steps.get(currentStage).addFirst(new Step(step, response, operation));
        } else {
            steps.get(stage).addLast(new Step(step, response, operation));
        }
    }

    public ModelNode getFailureDescription() {
        return response.get(FAILURE_DESCRIPTION);
    }

    public boolean hasFailureDescription() {
        return response.has(FAILURE_DESCRIPTION);
    }

    public ResultAction completeStep() {
        try {
            ResultAction action = doCompleteStep();
            if (action == ResultAction.KEEP) {
                report(MessageSeverity.INFO, "Operation succeeded, committing");
            } else {
                report(MessageSeverity.INFO, "Operation rolling back");
            }
            return action;
        } finally {
            respectInterruption = false;
        }
    }

    /**
     * Perform the work of completing a step.
     *
     * @return the result action for the step which has just completed
     */
    private ResultAction doCompleteStep() {
        assert Thread.currentThread() == initiatingThread;
        // If the operation is done, fail.
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        // Cancellation is detected via interruption.
        if (Thread.currentThread().isInterrupted()) {
            cancelled = true;
        }
        // Rollback when any of:
        // 1. operation is cancelled
        // 2. operation failed in model phase
        // 3. operation failed in runtime/verify and rollback_on_fail is set
        // 4. isRollbackOnly
        ModelNode response = this.response;
        if (cancelled) {
            response.get(OUTCOME).set(CANCELLED);
            response.get(FAILURE_DESCRIPTION).set("Operation cancelled");
            response.get(ROLLED_BACK).set(true);
            resultAction = ResultAction.ROLLBACK;
            return resultAction;
        }
        if (response.hasDefined(FAILURE_DESCRIPTION) && (contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL) || currentStage == Stage.MODEL)) {
            response.get(OUTCOME).set(FAILED);
            response.get(ROLLED_BACK).set(true);
            resultAction = ResultAction.ROLLBACK;
            return resultAction;
        }
        if (resultAction == ResultAction.ROLLBACK) {
            return ResultAction.ROLLBACK;
        }
        // Locate the next step to execute.
        Step step = null;
        do {
            step = steps.get(currentStage).pollFirst();
            if (step == null) {
                // No steps remain in this stage; proceed to the next stage.
                if (currentStage.hasNext()) {
                    currentStage = currentStage.next();
                    if (contextType == Type.MANAGEMENT && currentStage == Stage.MODEL.next()) {
                        // Management mode; we do not proceed past the MODEL stage.
                        currentStage = Stage.DONE;
                    } else if (affectsRuntime && currentStage == Stage.VERIFY) {
                        // a change was made to the runtime.  Thus, we must wait for stability before resuming in to verify.
                        try {
                            modelController.awaitContainerMonitor(true, 1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            cancelled = true;
                            response.get(OUTCOME).set(CANCELLED);
                            response.get(FAILURE_DESCRIPTION).set("Operation cancelled");
                            response.get(ROLLED_BACK).set(true);
                            return ResultAction.ROLLBACK;
                        }
                    }
                }
            } else {
                return executeStep(step);
            }
        } while (currentStage != Stage.DONE);
        final AtomicReference<ResultAction> ref = new AtomicReference<ResultAction>(transactionControl == null ? ResultAction.KEEP : ResultAction.ROLLBACK);
        // No more steps, verified operation is a success!
        ConfigurationPersister.PersistenceResource persistenceResource = null;
        if (isModelAffected() && resultAction != ResultAction.ROLLBACK) try {
            persistenceResource = modelController.writeModel(model, affectsModel);
        } catch (ConfigurationPersistenceException e) {
            response.get(OUTCOME).set(FAILED);
            response.get(FAILURE_DESCRIPTION).set("Failed to persist configuration change: " + e);
            return resultAction = ResultAction.ROLLBACK;
        }
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
        if (persistenceResource != null) {
            if (resultAction == ResultAction.ROLLBACK) {
                persistenceResource.rollback();
            } else {
                persistenceResource.commit();
            }
        }
        return resultAction;
    }

    private ResultAction executeStep(final Step step) {
        PathAddress oldModelAddress = modelAddress;
        ModelNode oldOperation = operation;
        ModelNode oldResponse = this.response;
        StampHolder oldRestartStamp = restartStampHolder;
        Stage stepStage = null;
        ModelNode response = null;
        try {
            // next step runs at the next depth level
            depth++;
            response = this.response = step.response;
            this.restartStampHolder = step.restartStamp;
            ModelNode newOperation = operation = step.operation;
            modelAddress = PathAddress.pathAddress(newOperation.get(OP_ADDR));
            try {
                step.handler.execute(this, newOperation);
            } catch (OperationFailedException ofe) {
                if (currentStage != Stage.DONE) {
                    // Handler threw OFE before calling completeStep(); that's equivalent to
                    // a request that we set the failure description and call completeStep()
                    response.get(FAILURE_DESCRIPTION).set(ofe.getFailureDescription());
                    log.warnf("Operation (%s) failed - address: (%s)", operation.get(OP), operation.get(OP_ADDR));
                    completeStep();
                }
                else {
                    // Handler threw OFE after calling completeStep()
                    // Throw it on and let standard error handling deal with it
                    throw ofe;
                }
            }
            assert resultAction != null;
        } catch (Throwable t) {
            log.errorf(t, "Operation (%s) failed - address: (%s)", operation.get(OP), operation.get(OP_ADDR));
            // If this block is entered, then the next step failed
            // The question is, did it fail before or after calling completeStep()?
            if (currentStage != Stage.DONE) {
                // It failed before, so consider the operation a failure.
                if (! response.hasDefined(FAILURE_DESCRIPTION)) {
                    response.get(FAILURE_DESCRIPTION).set("Operation handler failed: " + t);
                }
                response.get(OUTCOME).set(FAILED);
                resultAction = getFailedResultAction(t);
                if (resultAction == ResultAction.ROLLBACK) {
                    response.get(ROLLED_BACK).set(true);
                }
                return resultAction;
            } else {
                if (resultAction != ResultAction.KEEP) {
                    response.get(ROLLED_BACK).set(true);
                }
                response.get(OUTCOME).set(response.hasDefined(FAILURE_DESCRIPTION) ? FAILED : SUCCESS);
                // It failed after!  Just return, ignore the failure
                report(MessageSeverity.WARN, "Step handler " + step.handler + " failed after completion");
                return resultAction;
            }
        } finally {
            modelAddress = oldModelAddress;
            operation = oldOperation;
            this.response = oldResponse;
            this.restartStampHolder = oldRestartStamp;
            if (lockDepth == depth) {
                modelController.releaseLock();
                lockDepth = 0;
            }
            if (containerMonitorDepth == depth) {
                awaitContainerMonitor();
                modelController.releaseContainerMonitor();
                containerMonitorDepth = 0;
            }
            stepStage = currentStage;
            if (--depth == 0) {
                // We're returning from the outermost completeStep()
                // Null out the current stage to disallow further access to the context
                currentStage = null;
            }
        }

        if (stepStage != Stage.DONE) {
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
            return resultAction;
        } else {
            response.get(OUTCOME).set(response.hasDefined(FAILURE_DESCRIPTION) ? FAILED : SUCCESS);
        }
        if (resultAction == ResultAction.ROLLBACK) {
            response.get(OUTCOME).set(FAILED);
            response.get(ROLLED_BACK).set(true);
        }
        return resultAction;
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
    public void reloadRequired() {
        if (processState.isReloadSupported()) {
            this.restartStampHolder.restartStamp = processState.setReloadRequired();
            this.response.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).set(true);
        } else {
            restartRequired();
        }
    }

    @Override
    public void restartRequired() {
        this.restartStampHolder.restartStamp = processState.setRestartRequired();
        this.response.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RESTART).set(true);
    }

    @Override
    public void revertReloadRequired() {
        if (processState.isReloadSupported()) {
            processState.revertReloadRequired(this.restartStampHolder.restartStamp);
            if (this.response.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RELOAD)) {
                this.response.get(RESPONSE_HEADERS).remove(OPERATION_REQUIRES_RELOAD);
                if (this.response.get(RESPONSE_HEADERS).asInt() == 0) {
                    this.response.remove(RESPONSE_HEADERS);
                }
            }
        }
        else {
            revertRestartRequired();
        }
    }

    @Override
    public void revertRestartRequired() {
        processState.revertRestartRequired(this.restartStampHolder.restartStamp);
        if (this.response.get(RESPONSE_HEADERS).hasDefined(OPERATION_REQUIRES_RESTART)) {
            this.response.get(RESPONSE_HEADERS).remove(OPERATION_REQUIRES_RESTART);
            if (this.response.get(RESPONSE_HEADERS).asInt() == 0) {
                this.response.remove(RESPONSE_HEADERS);
            }
        }
    }

    @Override
    public void runtimeUpdateSkipped() {
        this.response.get(RESPONSE_HEADERS, RUNTIME_UPDATE_SKIPPED).set(true);
    }


    public ManagementResourceRegistration getResourceRegistrationForUpdate() {
        final PathAddress address = modelAddress;
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
        final PathAddress address = modelAddress;
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
        if (! (currentStage == Stage.RUNTIME || currentStage == Stage.VERIFY || isRollingBack() && ! modify)) {
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
        controller.addListener(new AbstractServiceListener<Object>() {
            public void listenerAdded(final ServiceController<?> controller) {
                final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                synchronized (map) {
                    map.put(controller.getName(), controller);
                    controller.setMode(ServiceController.Mode.REMOVE);
                }
            }

            public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                switch (transition) {
                    case REMOVING_to_REMOVED:
                    case REMOVING_to_DOWN: {
                        final Map<ServiceName, ServiceController<?>> map = realRemovingControllers;
                        synchronized (map) {
                            if (map.get(controller.getName()) == controller) {
                                map.remove(controller.getName());
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
        if (lockDepth == 0) {
            if (currentStage == Stage.DONE) {
                throw new IllegalStateException("Invalid modification after completed step");
            }
            try {
                modelController.acquireLock(respectInterruption);
                lockDepth = depth;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
        }
    }

    private void acquireContainerMonitor() {
        if (containerMonitorDepth == 0) {
            if (currentStage == Stage.DONE) {
                throw new IllegalStateException("Invalid modification after completed step");
            }
            modelController.acquireContainerMonitor();
            containerMonitorDepth = depth;
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
        final PathAddress address = modelAddress.append(requestAddress);
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
        final PathAddress address = modelAddress.append(requestAddress);
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
                    // TODO check cardinality
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
        final PathAddress address = modelAddress.append(requestAddress);
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
        final PathAddress address = modelAddress.append(requestAddress);
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

    public Resource createResource(PathAddress requestAddress) {
        final PathAddress address = modelAddress.append(requestAddress);
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
                if(model.hasChild(element)) {
                    throw new IllegalStateException("duplicate resource " + address);
                } else {
                    final PathAddress parent = address.subAddress(0, address.size() -1);
                    final Set<String> childrenNames = modelController.getRootRegistration().getChildNames(parent);
                    if(!childrenNames.contains(key)) {
                        throw new IllegalStateException("no child-type " + key);
                    }
                    // TODO check cardinality
                    final Resource newModel = Resource.Factory.create();
                    model.registerChild(element, newModel);
                    model = newModel;
                }
            } else {
                model = model.requireChild(element);
            }
        }
        return model;
    }

    public Resource removeResource(final PathAddress requestAddress) {
        final PathAddress address = modelAddress.append(requestAddress);
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
            messageHandler.handleReport(severity, message);
        } catch (Throwable t) {
            // ignored
        }
    }

    public ModelNode getResult() {
        return response.get(RESULT);
    }

    public boolean hasResult() {
        return response.has(RESULT);
    }

    static class Step {
        private final OperationStepHandler handler;
        private final ModelNode response;
        private final ModelNode operation;
        private final StampHolder restartStamp  = new StampHolder();

        private Step(final OperationStepHandler handler, final ModelNode response, final ModelNode operation) {
            this.handler = handler;
            this.response = response;
            this.operation = operation;
            // Create the outcome node early so it appears at the top of the response
            response.get(OUTCOME);
        }
    }

    /**
     *  Simple wrapper object to allow the context and the current Step to share a reference to the object returned by
     *  {@link ModelControllerImpl#setReloadRequired()} or
     *  {@link ModelControllerImpl#setRestartRequired()}
     */
    static class StampHolder {
        private Object restartStamp;
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
                return realBuilder.install();
            }
        }
    }
}
