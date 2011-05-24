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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicReference;
import org.jboss.as.controller.client.MessageSeverity;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.dmr.ModelNode;
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Operation context implementation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class NewOperationContextImpl implements NewOperationContext {

    private final NewModelControllerImpl modelController;
    private final Type contextType;
    private final EnumSet<ContextFlag> contextFlags;
    private final OperationMessageHandler messageHandler;
    private final Thread initiatingThread;
    private final EnumMap<Stage, Deque<Step>> steps;
    private final NewModelController.OperationTransactionControl transactionControl;
    private final ServiceTarget serviceTarget;
    private final Map<ServiceName, ServiceController<?>> realRemovingControllers = new HashMap<ServiceName, ServiceController<?>>();
    private boolean respectInterruption = true;
    private PathAddress modelAddress;
    private Stage currentStage = Stage.MODEL;
    private EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
    /**
     * The result of the current operation.
     */
    private ModelNode response;
    /**
     * The current operation body being executed.
     */
    private ModelNode operation;
    /**
     * The model.  If {@code affectsModel} is {@code true}, this is a clone of the model.
     */
    private ModelNode model;
    private ModelNode readOnlyModel;
    private ResultAction resultAction = ResultAction.ROLLBACK;

    enum ContextFlag {
        ROLLBACK_ON_FAIL,
    }

    enum Flag {
        AFFECTS_MODEL,
        AFFECTS_RUNTIME,
        WRITE_LOCK_TAKEN,
        CANCELLED,
    }

    NewOperationContextImpl(final NewModelControllerImpl modelController, final Type contextType, final EnumSet<ContextFlag> contextFlags, final OperationMessageHandler messageHandler, final ModelNode model, final NewModelController.OperationTransactionControl transactionControl) {
        this.contextType = contextType;
        this.transactionControl = transactionControl;
        this.model = readOnlyModel = model;
        this.modelController = modelController;
        this.messageHandler = messageHandler;
        response = new ModelNode().setEmptyObject();
        steps = new EnumMap<Stage, Deque<Step>>(Stage.class);
        for (Stage stage : Stage.values()) {
            steps.put(stage, new ArrayDeque<Step>());
        }
        initiatingThread = Thread.currentThread();
        this.contextFlags = contextFlags;
        serviceTarget = new ContextServiceTarget(modelController);
    }

    public InputStream getAttachmentStream(final int index) throws IOException {
        throw new UnsupportedOperationException("Not yet");
    }

    public void addStep(final NewStepHandler step, final Stage stage) throws IllegalArgumentException {
        addStep(response, operation, step, stage);
    }

    public void addStep(final ModelNode response, final ModelNode operation, final NewStepHandler step, final Stage stage) throws IllegalArgumentException {
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
        if (stage.compareTo(currentStage) > 0) {
            throw new IllegalStateException("Stage " + stage + " is already complete");
        }
        if (contextType == Type.MANAGEMENT && stage.compareTo(Stage.MODEL) > 0) {
            throw new IllegalArgumentException("Invalid step stage for this context type");
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
    private ResultAction doCompleteStep() {
        assert Thread.currentThread() == initiatingThread;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (Thread.currentThread().isInterrupted()) {
            flags.add(Flag.CANCELLED);
        }
        // Rollback when any of:
        // 1. operation is cancelled
        // 2. operation failed in model phase
        // 3. operation failed in runtime/verify and rollback_on_fail is set
        ModelNode response = this.response;
        if (flags.contains(Flag.CANCELLED)) {
            response.get(OUTCOME).set(CANCELLED);
            response.get(FAILURE_DESCRIPTION).set("Operation cancelled");
            response.set(ROLLED_BACK).set(true);
            return ResultAction.ROLLBACK;
        }
        if (response.hasDefined(FAILURE_DESCRIPTION) && (contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL) || currentStage == Stage.MODEL)) {
            response.get(OUTCOME).set(FAILED);
            response.set(ROLLED_BACK).set(true);
            return ResultAction.ROLLBACK;
        }
        do {
            Step step = steps.get(currentStage).pollFirst();
            if (step == null) {
                currentStage = currentStage.next();
                if (contextType == Type.MANAGEMENT && currentStage == Stage.MODEL.next()) {
                    currentStage = null;
                }
                if (flags.contains(Flag.AFFECTS_RUNTIME) && currentStage == Stage.VERIFY) {
                    // a change was made to the runtime
                    modelController.releaseContainerMonitor();
                    try {
                        modelController.awaitContainerMonitor(true, 0);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        flags.add(Flag.CANCELLED);
                        response.get(OUTCOME).set(CANCELLED);
                        response.get(FAILURE_DESCRIPTION).set("Operation cancelled");
                        response.set(ROLLED_BACK).set(true);
                        return ResultAction.ROLLBACK;
                    }
                }
            } else {
                PathAddress oldModelAddress = modelAddress;
                EnumSet<Flag> oldFlags = flags;
                ModelNode oldOperation = operation;
                ModelNode oldResponse = response;
                try {
                    flags = EnumSet.noneOf(Flag.class);
                    response = step.response;
                    ModelNode newOperation = operation = step.operation;
                    modelAddress = PathAddress.pathAddress(newOperation.get(ADDRESS));
                    step.handler.execute(this, newOperation);
                    return resultAction;
                } catch (Throwable t) {
                    // If this block is entered, then the next step failed
                    // The question is, did it fail before or after calling completeStep()?
                    if (currentStage != null) {
                        // It failed before, so consider the operation a failure.
                        if (! response.hasDefined(FAILURE_DESCRIPTION)) {
                            response.get(FAILURE_DESCRIPTION).set("Operation handler failed: " + t);
                        }
                        response.get(OUTCOME).set(FAILED);
                        response.set(ROLLED_BACK).set(true);
                        // this result action will be overwritten in finally, but whatever
                        return resultAction = ResultAction.ROLLBACK;
                    } else {
                        if (resultAction == ResultAction.ROLLBACK) {
                            response.set(ROLLED_BACK).set(true);
                        }
                        response.get(OUTCOME).set(response.hasDefined(FAILURE_DESCRIPTION) ? FAILED : SUCCESS);
                        // It failed after!  Just return, ignore the failure
                        report(MessageSeverity.WARN, "Step handler " + step.handler + " failed after completion");
                        return resultAction;
                    }
                } finally {
                    try {
                        if (currentStage != null) {
                            // This is a failure because the next step failed to call completeStep().
                            // Either an exception occurred beforehand, or the implementer screwed up.
                            // If an exception occurred, then this will have no effect.
                            // If the implementer screwed up, then we're essentially fixing the context state and treating
                            // the overall operation as a failure.
                            currentStage = null;
                            if (! response.hasDefined(FAILURE_DESCRIPTION)) {
                                response.get(FAILURE_DESCRIPTION).set("Operation handler failed to complete");
                            }
                            response.get(OUTCOME).set(FAILED);
                            response.set(ROLLED_BACK).set(true);
                            // We deliberately override the result to always roll back in this case!
                            //noinspection ReturnInsideFinallyBlock
                            return resultAction = ResultAction.ROLLBACK;
                        } else {
                            if (resultAction == ResultAction.ROLLBACK) {
                                response.get(ROLLED_BACK).set(true);
                            }
                            response.get(OUTCOME).set(response.hasDefined(FAILURE_DESCRIPTION) ? FAILED : SUCCESS);
                        }
                    } finally {
                        modelAddress = oldModelAddress;
                        flags = oldFlags;
                        operation = oldOperation;
                        this.response = response = oldResponse;
                        if (flags.contains(Flag.WRITE_LOCK_TAKEN)) {
                            modelController.releaseLock();
                        }
                    }
                }
                // -- not reached --
            }
        } while (currentStage != Stage.DONE);
        // No more steps, verified operation is a success!
        if (isModelAffected()) try {
            modelController.writeModel(model);
        } catch (ConfigurationPersistenceException e) {
            response.get(OUTCOME).set(FAILED);
            response.get(FAILURE_DESCRIPTION).set("Failed to persist configuration change: " + e);
            return resultAction = ResultAction.ROLLBACK;
        }
        final AtomicReference<ResultAction> ref = new AtomicReference<ResultAction>(ResultAction.ROLLBACK);
        transactionControl.operationPrepared(new NewModelController.OperationTransaction() {
            public void commit() {
                ref.set(ResultAction.KEEP);
            }

            public void rollback() {
                ref.set(ResultAction.ROLLBACK);
            }
        }, response);
        return resultAction = ref.get();
    }

    public Type getType() {
        assert Thread.currentThread() == initiatingThread;
        return contextType;
    }

    public ServiceRegistry getServiceRegistry(final boolean modify) throws UnsupportedOperationException {
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (! (currentStage == Stage.RUNTIME || currentStage == Stage.VERIFY && ! modify)) {
            throw new IllegalStateException("Get service registry only supported in runtime operations");
        }
        if (modify && flags.add(Flag.AFFECTS_RUNTIME)) {
            takeWriteLock();
            try {
                modelController.awaitContainerMonitor(respectInterruption, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
            modelController.acquireContainerMonitor();
        }
        return modelController.getServiceRegistry();
    }

    public ServiceController<?> removeService(final ServiceName name) throws UnsupportedOperationException {
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY) {
            throw new IllegalStateException("Service removal only supported in runtime operations");
        }
        if (flags.add(Flag.AFFECTS_RUNTIME)) {
            takeWriteLock();
            try {
                modelController.awaitContainerMonitor(respectInterruption, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
            modelController.acquireContainerMonitor();
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
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY) {
            throw new IllegalStateException("Service removal only supported in runtime operations");
        }
        if (flags.add(Flag.AFFECTS_RUNTIME)) {
            takeWriteLock();
            try {
                modelController.awaitContainerMonitor(respectInterruption, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
            modelController.acquireContainerMonitor();
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
        if (currentStage != Stage.RUNTIME && currentStage != Stage.VERIFY) {
            throw new IllegalStateException("Get service target only supported in runtime operations");
        }
        if (flags.add(Flag.AFFECTS_RUNTIME)) {
            takeWriteLock();
            try {
                modelController.awaitContainerMonitor(respectInterruption, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
            modelController.acquireContainerMonitor();
        }
        return serviceTarget;
    }

    private void takeWriteLock() {
        if (flags.add(Flag.WRITE_LOCK_TAKEN)) {
            try {
                modelController.acquireLock(respectInterruption);
            } catch (InterruptedException e) {
                flags.remove(Flag.WRITE_LOCK_TAKEN);
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
            try {
                modelController.awaitContainerMonitor(respectInterruption, 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CancellationException("Operation cancelled asynchronously");
            }
        }
    }

    public ModelNode readModel(final PathAddress requestAddress) {
        final PathAddress address = modelAddress.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        ModelNode model = this.model;
        for (final PathElement element : address) {
            model = model.require(element.getKey()).require(element.getValue());
        }
        return model;
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
        if (flags.add(Flag.AFFECTS_MODEL)) {
            takeWriteLock();
            model = model.clone();
            readOnlyModel = null;
        }
        ModelNode model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot write to *");
            }
            if (! i.hasNext()) {
                model = model.require(element.getKey()).get(element.getValue());
            } else {
                model = model.require(element.getKey()).require(element.getValue());
            }
        }
        return model;
    }

    public ModelNode getModel() {
        final ModelNode readOnlyModel = this.readOnlyModel;
        if (readOnlyModel == null) {
            final ModelNode newModel = model.clone();
            newModel.protect();
            return this.readOnlyModel = newModel;
        } else {
            return readOnlyModel;
        }
    }

    public ModelNode writeModel(final PathAddress requestAddress, final ModelNode newData) throws UnsupportedOperationException {
        final PathAddress address = modelAddress.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (flags.add(Flag.AFFECTS_MODEL)) {
            takeWriteLock();
            model = model.clone();
            readOnlyModel = null;
        }
        ModelNode model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot write to *");
            }
            if (! i.hasNext()) {
                model = model.require(element.getKey()).get(element.getValue());
            } else {
                model = model.require(element.getKey()).require(element.getValue());
            }
        }
        return model;
    }

    public ModelNode removeModel(final PathAddress requestAddress) throws UnsupportedOperationException {
        final PathAddress address = modelAddress.append(requestAddress);
        assert Thread.currentThread() == initiatingThread;
        Stage currentStage = this.currentStage;
        if (currentStage == null) {
            throw new IllegalStateException("Operation already complete");
        }
        if (currentStage != Stage.MODEL) {
            throw new IllegalStateException("Stage MODEL is already complete");
        }
        if (flags.add(Flag.AFFECTS_MODEL)) {
            takeWriteLock();
            model = model.clone();
        }
        ModelNode model = this.model;
        final Iterator<PathElement> i = address.iterator();
        while (i.hasNext()) {
            final PathElement element = i.next();
            if (element.isMultiTarget()) {
                throw new IllegalArgumentException("Cannot write to *");
            }
            if (! i.hasNext()) {
                model = model.require(element.getKey()).remove(element.getValue());
            } else {
                model = model.require(element.getKey()).require(element.getValue());
            }
        }
        return model;
    }

    public boolean isModelAffected() {
        return flags.contains(Flag.AFFECTS_MODEL);
    }

    public boolean isRuntimeAffected() {
        return flags.contains(Flag.AFFECTS_RUNTIME);
    }

    public Stage getCurrentStage() {
        return currentStage;
    }

    public ModelNode getCompensatingOperation() {
        if (operation == null) {
            throw new NullPointerException("operation is null");
        }
        return response.get(COMPENSATING_OPERATION);
    }

    public void report(final MessageSeverity severity, final String message) {
        try {
            messageHandler.handleReport(severity, message);
        } catch (Throwable t) {
            // ignored
        }
    }

    public ModelNode getResult() {
        return response.get("result");
    }

    static class Step {
        private final NewStepHandler handler;
        private final ModelNode response;
        private final ModelNode operation;

        private Step(final NewStepHandler handler, final ModelNode response, final ModelNode operation) {
            this.handler = handler;
            this.response = response;
            this.operation = operation;
        }
    }

    class ContextServiceTarget implements ServiceTarget {

        private final NewModelControllerImpl modelController;

        ContextServiceTarget(final NewModelControllerImpl modelController) {
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
