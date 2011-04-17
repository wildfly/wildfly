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

package org.jboss.as.server;

import org.jboss.as.controller.BasicModelController;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelProvider;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContextImpl;
import org.jboss.as.controller.OperationControllerContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;
import org.jboss.as.controller.persistence.ExtensibleConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptionProviders;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.api.ContentRepository;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.DelegatingServiceRegistry;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartException;

import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ServerControllerImpl extends BasicModelController implements ServerController {

    private static final Logger log = Logger.getLogger("org.jboss.as.server");

    private final ExecutorService executorService;
    private final ServiceTarget serviceTarget;
    private final ServiceRegistry serviceRegistry;
    private final ServerEnvironment serverEnvironment;
    private final AtomicInteger stamp = new AtomicInteger(0);
    private final AtomicStampedReference<State> state = new AtomicStampedReference<State>(null, 0);
    private final ExtensibleConfigurationPersister extensibleConfigurationPersister;
    private final ContentRepository contentRepository;
    private final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);
    private final ServerStateMonitorListener serverStateMonitorListener;

    ServerControllerImpl(final ServiceContainer container, final ServiceTarget serviceTarget, final ServerEnvironment serverEnvironment,
            final ExtensibleConfigurationPersister configurationPersister, final ContentRepository contentRepository,
            final ExecutorService executorService) {
        super(ServerControllerModelUtil.createCoreModel(), configurationPersister, ServerDescriptionProviders.ROOT_PROVIDER);
        this.serviceTarget = serviceTarget;
        extensibleConfigurationPersister = configurationPersister;
        this.serverEnvironment = serverEnvironment;
        this.contentRepository = contentRepository;
        serviceRegistry = new DelegatingServiceRegistry(container);
        this.executorService = executorService;
        serverStateMonitorListener = new ServerStateMonitorListener(container);
    }

    void init() {
        state.set(State.STARTING, stamp.incrementAndGet());

        registerInternalOperations();

        // Build up the core model registry
        ServerControllerModelUtil.initOperations(getRegistry(), contentRepository, extensibleConfigurationPersister, serverEnvironment);

        deployers.clear();
        for (Phase phase : Phase.values()) {
            deployers.put(phase, new ConcurrentSkipListSet<RegisteredProcessor>());
        }
    }

    EnumMap<Phase, SortedSet<RegisteredProcessor>> finishBoot() {
        state.set(State.RUNNING, stamp.incrementAndGet());
        EnumMap<Phase, SortedSet<RegisteredProcessor>> copy = new EnumMap<Phase, SortedSet<RegisteredProcessor>>(Phase.class);
        for (Map.Entry<Phase, SortedSet<RegisteredProcessor>> entry : deployers.entrySet()) {
            copy.put(entry.getKey(), new ConcurrentSkipListSet<RegisteredProcessor>(entry.getValue()));
        }
        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public ServerEnvironment getServerEnvironment() {
        return serverEnvironment;
    }

    /**
     * Get this server's service container registry.
     *
     * @return the container registry
     */
    @Override
    public ServiceRegistry getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Get the server controller state.
     *
     * @return the state
     */
    @Override
    public State getState() {
        return state.getReference();
    }

    ServiceListener<Object> getServerStateMonitorListener() {
        return serverStateMonitorListener;
    }

    /** {@inheritDoc} */
    @Override
    protected OperationContext getOperationContext(final ModelNode subModel, final OperationHandler operationHandler, final Operation operation, final ModelProvider modelProvider) {
        if (operationHandler instanceof BootOperationHandler) {
            if (getState() == State.STARTING) {
                return new BootContextImpl(subModel, getRegistry(), deployers, modelProvider, operation);
            } else {
                state.set(State.RESTART_REQUIRED, stamp.incrementAndGet());
                return super.getOperationContext(subModel, operationHandler, operation, modelProvider);
            }
        } else if (!(getState() == State.RESTART_REQUIRED && operationHandler instanceof ModelUpdateOperationHandler)) {
            return new ServerOperationContextImpl(this, getRegistry(), subModel, modelProvider, operation);
        } else {
            return super.getOperationContext(subModel, operationHandler, operation, modelProvider);
        }
    }

    @Override
    protected OperationResult doExecute(OperationContext context, Operation operation, OperationHandler operationHandler, ResultHandler resultHandler, PathAddress address,
            final OperationControllerContext operationControllerContext) throws OperationFailedException {
        boolean rollback = isRollbackOnRuntimeFailure(context, operation.getOperation());
        RollbackAwareResultHandler rollbackAwareHandler = new RollbackAwareResultHandler(resultHandler);
        final OperationResult result = super.doExecute(context, operation, operationHandler, rollbackAwareHandler, address, operationControllerContext);

        if(context instanceof ServerOperationContextImpl) {
            if (rollback) {
                rollbackAwareHandler.setRollbackOperation(result.getCompensatingOperation());
                // TODO deal with Cancellable as well
            }
            final ServerOperationContextImpl serverOperationContext = ServerOperationContextImpl.class.cast(context);
            if(serverOperationContext.getRuntimeTask() != null) {

                // Make sure we've settled and generated a report post-boot so boot issues don't show up as op issues
                if (!serverStateMonitorListener.isFirstReportComplete() && state.getReference() != State.STARTING) {
                    serverStateMonitorListener.awaitUninterruptibly();
                }

                try {
                    serverOperationContext.getRuntimeTask().execute(new RuntimeTaskContext() {
                        @Override
                        public ServiceTarget getServiceTarget() {
                            return serviceTarget;
                        }

                        @Override
                        public ServiceRegistry getServiceRegistry() {
                            return serviceRegistry;
                        }
                    });
                } catch (OperationFailedException e) {
                    rollbackAwareHandler.handleFailed(e.getFailureDescription());
                } catch (Exception e) {
                    rollbackAwareHandler.handleFailed(new ModelNode().set(e.toString()));
                }

                ModelNode serverStateChangeReport = null;
                if (state.getReference() != State.STARTING) {
                    serverStateChangeReport = serverStateMonitorListener.awaitUninterruptibly();
                }
                if (serverStateChangeReport != null && !rollbackAwareHandler.isTerminalState()) {
                    rollbackAwareHandler.handleFailed(serverStateChangeReport);
                }
            }

            if (!rollbackAwareHandler.isTerminalState()) {
                rollbackAwareHandler.notifySuccess();
            }
        }
        // else this is a step in a composite op and the ServerMultiStepOperationController will handle it

        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void persistConfiguration(final ModelNode model, final ConfigurationPersisterProvider configurationPersisterFactory) {
        // do not persist during startup
        if (getState() != State.STARTING) {
            super.persistConfiguration(model, configurationPersisterFactory);
        }
    }

    @Override
    protected boolean isReadOnly(OperationHandler operationHandler) {
        // Minor optimization: Assume nothing is RO during boot
        if (getState() == State.STARTING) {
            return false;
        }
        return super.isReadOnly(operationHandler);
    }

    @Override
    protected MultiStepOperationController getMultiStepOperationController(final Operation operation, final ResultHandler handler,
            final OperationControllerContext operationControllerContext) throws OperationFailedException {
        return new ServerMultiStepOperationController(operation, handler, operationControllerContext);
    }

    private boolean isRollbackOnRuntimeFailure(OperationContext context, ModelNode operation) {
        return context instanceof ServerOperationContextImpl &&
            (!operation.hasDefined(OPERATION_HEADERS) || !operation.get(OPERATION_HEADERS).hasDefined(ROLLBACK_ON_RUNTIME_FAILURE)
                    || operation.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).asBoolean());
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
    }

    /**
     * A service listener to track container status.  Must be present when the service is created, or results will
     * be unpredictable.
     */
    class ServerStateMonitorListener extends AbstractServiceListener<Object> {
        private final ServiceRegistry serviceRegistry;
        private final AtomicInteger busyServiceCount = new AtomicInteger();

        // protected by "this"
        /** Failed controllers pending tick reaching zero */
        private final Map<ServiceController<?>, String> failedControllers = new IdentityHashMap<ServiceController<?>, String>();
        /** Failed controllers as of the last time tick reached zero */
        private final Map<ServiceController<?>, String> latestSettledFailedControllers = new IdentityHashMap<ServiceController<?>, String>();
        /** Failed controllers as of the last time getServerStateChangeReport() was called */
        private final Map<ServiceController<?>, String> lastReportFailedControllers = new IdentityHashMap<ServiceController<?>, String>();
        /** Services with missing deps */
        private final Set<ServiceController<?>> servicesWithMissingDeps = identitySet();
        /** Map of Services with missing deps as of the last time tick reached zero */
        private Map<ServiceName, Set<ServiceName>> previousMissingDeps = new HashMap<ServiceName, Set<ServiceName>>();
        /** Services with missing deps as of the last time getServerStateChangeReport() was called */
        private final Set<ServiceName> lastReportMissingDepSet = new TreeSet<ServiceName>();
        /** Flag indicating we've created our first post-boot report */
        private volatile boolean firstReportDone;

        ServerStateMonitorListener(final ServiceRegistry registry) {
            serviceRegistry = registry;
        }

        @Override
        public void listenerAdded(final ServiceController<?> controller) {
            if (controller.getName().equals(Services.JBOSS_SERVER_CONTROLLER)) {
                controller.removeListener(this);
            } else {
                untick();
            }
        }

        @Override
        public void serviceWaiting(final ServiceController<?> controller) {
            tick();
        }

        @Override
        public void serviceWaitingCleared(final ServiceController<?> controller) {
            untick();
        }

        @Override
        public void serviceWontStart(final ServiceController<?> controller) {
            tick();
        }

        @Override
        public void serviceWontStartCleared(final ServiceController<?> controller) {
            untick();
        }

        @Override
        public void dependencyProblem(final ServiceController<?> controller) {
            tick();
        }

        @Override
        public void dependencyProblemCleared(final ServiceController<?> controller) {
            untick();
        }

        @Override
        public void serviceStarting(final ServiceController<?> controller) {
            // no tick
        }

        @Override
        public void serviceStarted(final ServiceController<?> controller) {
            tick();
        }

        @Override
        public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
            synchronized (this) {
                failedControllers.put(controller, reason.toString());
            }
            tick();
        }

        @Override
        public void serviceRemoved(final ServiceController<?> controller) {
            synchronized (this) {
                failedControllers.remove(controller);
                servicesWithMissingDeps.remove(controller);
            }
            tick();
        }

        @Override
        public void serviceStopRequested(final ServiceController<?> controller) {
            untick();
        }

        @Override
        public void serviceStopRequestCleared(final ServiceController<?> controller) {
            tick();
        }

        @Override
        public void serviceStopping(final ServiceController<?> controller) {
            // no tick
        }

        @Override
        public void failedServiceStarting(final ServiceController<?> controller) {
            synchronized (this) {
                failedControllers.remove(controller);
            }
            untick();
        }

        @Override
        public void failedServiceStopped(final ServiceController<?> controller) {
            synchronized (this) {
                failedControllers.remove(controller);
            }
            untick();
        }

        @Override
        public void immediateDependencyAvailable(final ServiceController<?> controller) {
            synchronized (this) {
                servicesWithMissingDeps.remove(controller);
            }
        }

        @Override
        public void immediateDependencyUnavailable(final ServiceController<?> controller) {
            synchronized (this) {
                servicesWithMissingDeps.add(controller);
            }
        }

        ModelNode awaitUninterruptibly() {
            boolean intr = false;
            // todo - atomically return the last status summary, or something
            try {
                synchronized (this) {
                    while (busyServiceCount.get() > 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }

                    return getServerStateChangeReport();
                }
            } finally {
                if (intr) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Tick down the count, triggering a deployment status report when the count is zero.
         */
        private void tick() {
            int tick = busyServiceCount.decrementAndGet();
//            System.out.println("TICK -> " + tick + " (" + Thread.currentThread().getStackTrace()[2].getMethodName() + ") -> " + tickController);
            if (tick == 0) {
                synchronized (this) {
                    notifyAll();
                    final Map<ServiceName, Set<ServiceName>> missingDeps = new HashMap<ServiceName, Set<ServiceName>>();
                    for (ServiceController<?> controller : servicesWithMissingDeps) {
                        for(ServiceName missing : controller.getImmediateUnavailableDependencies()) {
                            if(!missingDeps.containsKey(missing)) {
                                missingDeps.put(missing, new HashSet<ServiceName>());
                            }
                            missingDeps.get(missing).add(controller.getName());
                        }
                    }

                    final Set<ServiceName> previousMissing = previousMissingDeps.keySet();

                    // no longer missing deps...
                    final Set<ServiceName> noLongerMissing = new TreeSet<ServiceName>();
                    for (ServiceName name : previousMissing) {
                        if (! missingDeps.containsKey(name)) {
                            noLongerMissing.add(name);
                        }
                    }

                    // newly missing deps
                    final Map<ServiceName, Set<ServiceName>> newlyMissing = new TreeMap<ServiceName, Set<ServiceName>>();
                    newlyMissing.clear();
                    for (Map.Entry<ServiceName, Set<ServiceName>> entry : missingDeps.entrySet()) {
                        if (! previousMissing.contains(entry.getKey())) {
                            newlyMissing.put(entry.getKey(), entry.getValue());
                        }
                    }

                    previousMissingDeps = missingDeps;

                    // track failed services for the change report
                    latestSettledFailedControllers.clear();
                    latestSettledFailedControllers.putAll(failedControllers);

                    final StringBuilder msg = new StringBuilder();
                    msg.append("Service status report\n");
                    boolean print = false;
                    if (! newlyMissing.isEmpty()) {
                        print = true;
                        msg.append("   New missing/unsatisfied dependencies:\n");
                        for (Map.Entry<ServiceName, Set<ServiceName>> entry : newlyMissing.entrySet()) {
                            final ServiceName name = entry.getKey();
                            ServiceController<?> controller = serviceRegistry.getService(name);
                            if (controller == null) {
                                msg.append("      ").append(name).append(" (missing)");
                            } else {
                                msg.append("      ").append(name).append(" (unavailable)");
                            }
                            msg.append(" required by [");
                            Iterator<ServiceName> it = entry.getValue().iterator();
                            while(it.hasNext()) {
                                ServiceName requiring = it.next();
                                msg.append(requiring);
                                if(it.hasNext()) {
                                    msg.append(", ");
                                }
                            }
                            msg.append("]");
                            msg.append('\n');
                        }
                    }
                    if (! noLongerMissing.isEmpty()) {
                        print = true;
                        msg.append("   Newly corrected services:\n");
                        for (ServiceName name : noLongerMissing) {
                            ServiceController<?> controller = serviceRegistry.getService(name);
                            if (controller == null) {
                                msg.append("      ").append(name).append(" (no longer required)\n");
                            } else {
                                msg.append("      ").append(name).append(" (now available)\n");
                            }
                        }
                    }
                    if (! failedControllers.isEmpty()) {
                        print = true;
                        msg.append("  Services which failed to start:\n");
                        for (Map.Entry<ServiceController<?>, String> entry : failedControllers.entrySet()) {
                            msg.append("      ").append(entry.getKey().getName()).append(": ").append(entry.getValue()).append('\n');
                        }
                        failedControllers.clear();
                    }
                    if (print) {
                        log.info(msg);
                    }
                }
            }
        }

        private void untick() {
            busyServiceCount.incrementAndGet();
//            System.out.println("UNTICK -> " + tick + " (" + Thread.currentThread().getStackTrace()[2].getMethodName() + ") -> ");
        }

        private synchronized ModelNode getServerStateChangeReport() {

            // Determine the newly failed controllers
            final Map<ServiceController<?>, String> newFailedControllers = new IdentityHashMap<ServiceController<?>, String>(latestSettledFailedControllers);
            newFailedControllers.keySet().removeAll(lastReportFailedControllers.keySet());
            // Back up current state for use in next report
            lastReportFailedControllers.clear();
            lastReportFailedControllers.putAll(latestSettledFailedControllers);
            // Determine the new missing dependencies
            final Map<ServiceName, Set<ServiceName>> newReportMissingDeps = new TreeMap<ServiceName, Set<ServiceName>>(previousMissingDeps);
            for(ServiceName dep : lastReportMissingDepSet) {
                newReportMissingDeps.remove(dep);
            }
            // Back up current state for use in next report
            lastReportMissingDepSet.clear();
            lastReportMissingDepSet.addAll(previousMissingDeps.keySet());

            ModelNode report = null;
            if (!newFailedControllers.isEmpty() || !newReportMissingDeps.isEmpty()) {
                report = new ModelNode();
                if (! newReportMissingDeps.isEmpty()) {
                    ModelNode missing = report.get("New missing/unsatisfied dependencies");
                    for (Map.Entry<ServiceName, Set<ServiceName>> entry : newReportMissingDeps.entrySet()) {
                        final ServiceName name = entry.getKey();
                        ServiceController<?> controller = serviceRegistry.getService(name);
                        StringBuilder missingText = new StringBuilder(name.toString());
                        if (controller == null) {
                            missingText.append(" (missing)");
                        } else {
                            missingText.append(" (unavailable)\n");
                        }
                        missingText.append(" required by [");
                        Iterator<ServiceName> it = entry.getValue().iterator();
                        while(it.hasNext()) {
                            ServiceName requiring = it.next();
                            missingText.append(requiring);
                            if(it.hasNext()) {
                                missingText.append(", ");
                            }
                        }
                        missingText.append("]");
                        missing.add(missingText.toString());
                    }
                }
                if (! newFailedControllers.isEmpty()) {
                    ModelNode failed = report.get("Services which failed to start:");
                    for (Map.Entry<ServiceController<?>, String> entry : newFailedControllers.entrySet()) {
                        failed.add(entry.getKey().getName().toString());
                    }
                }
            }
            firstReportDone = true;
            return report;
        }

        private boolean isFirstReportComplete() {
            return firstReportDone;
        }
    }

    private class ServerOperationContextImpl extends OperationContextImpl implements ServerOperationContext, RuntimeOperationContext {
        // -1 as initial value ensures the CAS in revertRestartRequired()
        // will never succeed unless restartRequired() is called
        private int ourStamp = -1;
        private RuntimeTask runtimeTask;

        public ServerOperationContextImpl(ModelController controller, ModelNodeRegistration registry, ModelNode subModel, ModelProvider modelProvider, OperationAttachments executionAttachments) {
            super(controller, registry, subModel, modelProvider, executionAttachments);
        }

        @Override
        public ServerController getController() {
            return (ServerController) super.getController();
        }

        @Override
        public synchronized void restartRequired() {
            AtomicStampedReference<State> stateRef = state;
            int newStamp = stamp.incrementAndGet();
            int[] receiver = new int[1];
            // Keep trying until stateRef is RESTART_REQUIRED with our stamp
            for (;;) {
                State was = stateRef.get(receiver);
                if (was == State.STARTING) {
                    break;
                }
                if (stateRef.compareAndSet(was, State.RESTART_REQUIRED, receiver[0], newStamp)) {
                    ourStamp = newStamp;
                    break;
                }
            }
        }

        @Override
        public synchronized void revertRestartRequired() {
            // If 'state' still has the state we last set in restartRequired(), change to RUNNING
            state.compareAndSet(State.RESTART_REQUIRED, State.RUNNING, ourStamp, stamp.incrementAndGet());
        }

        @Override
        public RuntimeOperationContext getRuntimeContext() {
            return this;
        }

        public RuntimeTask getRuntimeTask() {
            return runtimeTask;
        }

        @Override
        public void setRuntimeTask(RuntimeTask runtimeTask) {
            this.runtimeTask = runtimeTask;
        }
    }

    private class BootContextImpl extends ServerOperationContextImpl implements BootOperationContext {

        private final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers;

        private BootContextImpl(final ModelNode subModel, final ModelNodeRegistration registry, final EnumMap<Phase, SortedSet<RegisteredProcessor>> deployers, ModelProvider modelProvider, OperationAttachments executionAttachments) {
            super(ServerControllerImpl.this, registry, subModel, modelProvider, executionAttachments);
            this.deployers = deployers;
        }

        @Override
        public void addDeploymentProcessor(final Phase phase, final int priority, final DeploymentUnitProcessor processor) {
            if (phase == null) {
                throw new IllegalArgumentException("phase is null");
            }
            if (processor == null) {
                throw new IllegalArgumentException("processor is null");
            }
            if (priority < 0) {
                throw new IllegalArgumentException("priority is invalid (must be >= 0)");
            }
            deployers.get(phase).add(new RegisteredProcessor(priority, processor));
        }
    }

    static final class RegisteredProcessor implements Comparable<RegisteredProcessor> {
        private final int priority;
        private final DeploymentUnitProcessor processor;

        RegisteredProcessor(final int priority, final DeploymentUnitProcessor processor) {
            this.priority = priority;
            this.processor = processor;
        }

        @Override
        public int compareTo(final RegisteredProcessor o) {
            final int rel = Integer.signum(priority - o.priority);
            return rel == 0 ? processor.getClass().getName().compareTo(o.getClass().getName()) : rel;
        }

        int getPriority() {
            return priority;
        }

        DeploymentUnitProcessor getProcessor() {
            return processor;
        }

    }

    class RollbackAwareResultHandler implements ResultHandler {

        private final ResultHandler delegate;
        private volatile ModelNode rollbackOperation;
        private volatile boolean terminalState;

        public RollbackAwareResultHandler(ResultHandler resultHandler) {
            delegate = resultHandler;
        }

        @Override
        public void handleResultFragment(String[] location, ModelNode result) {
            delegate.handleResultFragment(location, result);
        }

        @Override
        public void handleResultComplete() {
            // we ignore these and wait for notifySuccess();
        }

        @Override
        public void handleFailed(final ModelNode failureDescription) {

            if (terminalState) {
                // An async call from a service listener?? Oh well, too late.
                // We're going to eliminate async handling anyway
                return;
            }

            terminalState = true;

            if (rollbackOperation == null || !rollbackOperation.isDefined()) {
                delegate.handleFailed(failureDescription);
                return;
            }

            final ResultHandler rollbackHandler = new ResultHandler() {

                @Override
                public void handleResultFragment(String[] location, ModelNode result) {
                    // ignore fragments from rollback
                }

                @Override
                public void handleResultComplete() {
                    // FIXME this will not appear in the correct location
//                    ModelNode rollbackResult = new ModelNode();
//                    rollbackResult.get("rolled-back").set(true);
//                    delegate.handleResultFragment(new String[0], rollbackResult);
                    delegate.handleFailed(failureDescription);
                }

                @Override
                public void handleFailed(ModelNode rollbackFailureDescription) {
                    // FIXME this will not appear in the correct location
                    ModelNode rollbackResult = new ModelNode();
//                    rollbackResult.get("rolled-back").set(false);
//                    delegate.handleResultFragment(new String[0], rollbackResult);
                    rollbackResult = new ModelNode();
                    rollbackResult.get("rollback-failure-description").set(rollbackFailureDescription);
                    delegate.handleFailed(failureDescription);
                }

                @Override
                public void handleCancellation() {
                    handleFailed(new ModelNode().set("Rollback was cancelled"));
                }
            };

            // Make sure the rollback op doesn't itself try and roll back
            rollbackOperation.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

            Runnable r = new Runnable() {

                @Override
                public void run() {
                    execute(OperationBuilder.Factory.create(rollbackOperation).build(), rollbackHandler);
                }

            };
            executorService.execute(r);
        }

        @Override
        public void handleCancellation() {
            terminalState = true;
            delegate.handleCancellation();
        }

        private void setRollbackOperation(ModelNode compensatingOperation) {
            rollbackOperation = compensatingOperation;
        }

        private boolean isTerminalState() {
            return terminalState;
        }

        private void notifySuccess() {
            delegate.handleResultComplete();
        }
    }

    private class ServerMultiStepOperationController extends MultiStepOperationController {

        private ServerMultiStepOperationController(final Operation operation, final ResultHandler resultHandler,
                final OperationControllerContext injectedOperationControllerContext) throws OperationFailedException {
            super(operation, resultHandler, injectedOperationControllerContext);
        }

        @Override
        public OperationContext getOperationContext(ModelProvider modelSource, PathAddress address,
                OperationHandler operationHandler, Operation operation) {
            OperationContext delegate = super.getOperationContext(modelSource, address, operationHandler, operation);
            return delegate.getRuntimeContext() == null ? delegate : new StepRuntimeOperationContext(Integer.valueOf(currentOperation), ServerOperationContext.class.cast(delegate), operation);
        }

        @Override
        protected void handleFailures() {
            if (!modelComplete.get()) {
                super.handleFailures();
            } else if (rollbackOnRuntimeFailure) {
                final ModelNode compensatingOp = getOverallCompensatingOperation();
                if (compensatingOp.isDefined()) {
                    final ResultHandler rollbackResultHandler = new RollbackResultHandler();
                    // Execute the rollback in another thread as this method may be called by an MSC thread
                    // and we don't want to risk blocking it
                    Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            execute(OperationBuilder.Factory.create(compensatingOp).build(), rollbackResultHandler);
                        }
                    };
                    executorService.execute(r);
                } else {
                    super.handleFailures();
                }
            } else if (resultHandler instanceof StepResultHandler) {
                // This is a nested compensating op so it needs to correctly record a failure

                resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, resultsNode);

                if (overallFailure != null) {
                    resultHandler.handleFailed(overallFailure);
                }
                else {
                    final ModelNode failureMsg = new ModelNode();
                    // TODO i18n
                    final String baseMsg = "Composite operation failed. Steps that failed:";
                    for (Property property : resultsNode.asPropertyList()) {
                        final ModelNode stepResult = property.getValue();
                        if (stepResult.hasDefined(FAILURE_DESCRIPTION)) {
                            failureMsg.get(baseMsg, "Operation " + property.getName()).set(stepResult.get(FAILURE_DESCRIPTION));
                        }
                    }
                    resultHandler.handleFailed(failureMsg);
                }
            } else {
                //This is the top level compensating op, which is not going to rollback on runtime failure, so we just return success
                resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, resultsNode);
                resultHandler.handleResultComplete();
            }
        }

        @Override
        protected void recordModelComplete() {

            if (isModelUpdated()) {
                updateModelAndPersist();
            }

            if (runtimeTasks.size() > 0) {
                RuntimeTaskContext rtc = new RuntimeTaskContext() {
                    @Override
                    public ServiceTarget getServiceTarget() {
                        return serviceTarget;
                    }

                    @Override
                    public ServiceRegistry getServiceRegistry() {
                        return serviceRegistry;
                    }
                };
                for(int i = 0; i < steps.size(); i++) {
                    Integer id = Integer.valueOf(i);
                    RuntimeTask runtimeTask = runtimeTasks.get(id);
                    if (runtimeTask == null) {
                        continue;
                    }
                    try {
                        runtimeTask.execute(rtc);
                    } catch (OperationFailedException e) {
                        stepResultHandlers.get(id).handleFailed(e.getFailureDescription());
                    } catch (Throwable t) {
                        stepResultHandlers.get(id).handleFailed(new ModelNode().set(t.toString()));
                    }
                }
            }

            if (state.getReference() != State.STARTING  && !(resultHandler instanceof StepResultHandler)) {
                overallFailure = serverStateMonitorListener.awaitUninterruptibly();
            }

            for(int i = 0; i < steps.size(); i++) {
                Integer id = Integer.valueOf(i);
                StepResultHandler stepHandler = stepResultHandlers.get(id);
                // doExecute will not invoke this
                if (!stepHandler.isTerminalState()) {
                    stepHandler.handleResultComplete();
                }
            }

            // If we haven't already recorded failures and we aren't a nested op
            if (!hasFailures() && overallFailure != null && !(resultHandler instanceof StepResultHandler)) {
                hasFailures = true;
            }

            modelComplete.set(true);
            processComplete();
        }

        private void rollbackComplete(final ModelNode rollbackResult) {

            // Update each of our steps to indicate what happened with the rollback
            synchronized (resultsNode) {
                for (int i = 0; i < steps.size(); i++) {
                    String stepKey = getStepKey(i);
                    ModelNode stepResult = resultsNode.get(stepKey);
                    if (stepResult.hasDefined(OUTCOME) && !CANCELLED.equals(stepResult.get(OUTCOME).asString())) {

                        ModelNode rollbackStepOutcome = null;
                        ModelNode rollbackStepResult = null;
                        String rollbackKey = rollbackStepNames.get(Integer.valueOf(i));
                        if (rollbackKey != null) {
                            rollbackStepResult = rollbackResult.get(rollbackKey);
                            rollbackStepOutcome = rollbackStepResult.isDefined() ? rollbackStepResult.get(OUTCOME) : null;
                        }

                        if (rollbackStepOutcome == null || !rollbackStepOutcome.isDefined()) {
                            stepResult.get(ROLLED_BACK).set(false);
                            stepResult.get(ROLLBACK_FAILURE_DESCRIPTION).set(new ModelNode().set("No compensating operations was available"));
                        } else if (CANCELLED.equals(rollbackStepOutcome.asString())) {
                            stepResult.get(ROLLED_BACK).set(false);
                            stepResult.get(ROLLBACK_FAILURE_DESCRIPTION).set(new ModelNode().set("Execution of the compensating operation was cancelled"));
                        } else if (SUCCESS.equals(rollbackStepOutcome.asString())) {
                            stepResult.get(ROLLED_BACK).set(true);
                        } else {
                            stepResult.get(ROLLED_BACK).set(false);
                            ModelNode rollbackFailureCause = rollbackStepResult.get(FAILURE_DESCRIPTION);
                            if (!rollbackFailureCause.isDefined()) {
                                rollbackFailureCause = new ModelNode().set("Compensating operation was reverted due to failure of other compensating operations");
                            }
                            stepResult.get(ROLLBACK_FAILURE_DESCRIPTION).set(rollbackFailureCause);
                        }
                    }
                }
            }

            // Finally, notify the end user's result handler of completion
            super.handleFailures();
        }

        /** Context that stores any registered RuntimeTask under the step's id */
        private class StepRuntimeOperationContext implements ServerOperationContext, RuntimeOperationContext {

            private final Integer id;
            private final ServerOperationContext delegate;
            private final OperationAttachments executionAttachments;

            private StepRuntimeOperationContext(final Integer id, final ServerOperationContext delegate, OperationAttachments executionAttachments) {
                this.id = id;
                this.delegate = delegate;
                this.executionAttachments = executionAttachments;
            }

            @Override
            public ModelNode getSubModel() throws IllegalArgumentException {
                return delegate.getSubModel();
            }

            @Override
            public ModelNode getSubModel(PathAddress address) throws IllegalArgumentException {
                return delegate.getSubModel(address);
            }

            @Override
            public ModelNodeRegistration getRegistry() {
                return delegate.getRegistry();
            }

            @Override
            public ServerController getController() {
                return delegate.getController();
            }

            @Override
            public void restartRequired() {
                delegate.restartRequired();
            }

            @Override
            public void revertRestartRequired() {
                delegate.revertRestartRequired();
            }

            @Override
            public RuntimeOperationContext getRuntimeContext() {
                return this;
            }

            @Override
            public void setRuntimeTask(RuntimeTask runtimeTask) {
                runtimeTasks.put(id, runtimeTask);
            }

            @Override
            public List<InputStream> getInputStreams() {
                return executionAttachments.getInputStreams();
            }
        }

        /**
         * Captures the result of executing compensating operations and then triggers
         * the final completion of the original operation.
         */
        private class RollbackResultHandler implements ResultHandler {

            private final ModelNode rollbackResult = new ModelNode();

            @Override
            public void handleResultFragment(String[] location, ModelNode result) {
                rollbackResult.get(location).set(result);
            }

            @Override
            public void handleResultComplete() {
                // TODO add an overall rollback message (needs change in ResultHandler API)
                rollbackComplete(rollbackResult);
            }

            @Override
            public void handleFailed(ModelNode failureDescription) {
                // TODO add an overall rollback message (needs change in ResultHandler API)
                rollbackComplete(rollbackResult);
            }

            @Override
            public void handleCancellation() {
                // TODO add an overall rollback message (needs change in ResultHandler API)
                rollbackComplete(rollbackResult);
            }

        }

    }
}
