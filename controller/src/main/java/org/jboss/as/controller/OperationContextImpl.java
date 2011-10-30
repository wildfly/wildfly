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

import java.io.InputStream;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

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
final class OperationContextImpl extends AbstractOperationContext {

    private final ModelControllerImpl modelController;
    private final EnumSet<ContextFlag> contextFlags;
    private final OperationMessageHandler messageHandler;
    private final ServiceTarget serviceTarget;
    private final Map<ServiceName, ServiceController<?>> realRemovingControllers = new HashMap<ServiceName, ServiceController<?>>();
    private final boolean booting;
    private final OperationAttachments attachments;
    /** Tracks whether any steps have gotten write access to the model */
    private final Set<PathAddress> affectsModel;
    /** Tracks whether any steps have gotten write access to the management resource registration*/
    private boolean affectsResourceRegistration;

    private boolean respectInterruption = true;

    private Resource model;
    /** Tracks whether any steps have gotten write access to the runtime */
    private boolean affectsRuntime;
    /** The step that acquired the write lock */
    private Step lockStep;
    /** The step that acquired the container monitor  */
    private Step containerMonitorStep;

    OperationContextImpl(final ModelControllerImpl modelController, final Type contextType, final EnumSet<ContextFlag> contextFlags,
                            final OperationMessageHandler messageHandler, final OperationAttachments attachments,
                            final Resource model, final ModelController.OperationTransactionControl transactionControl,
                            final ControlledProcessState processState, final boolean booting) {
        super(contextType, transactionControl, processState);
        this.booting = booting;
        this.model = model;
        this.modelController = modelController;
        this.messageHandler = messageHandler;
        this.attachments = attachments;
        this.affectsModel = new HashSet<PathAddress>(1);
        this.contextFlags = contextFlags;
        this.serviceTarget = new ContextServiceTarget(modelController);
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

    @Override
    void awaitModelControllerContainerMonitor() throws InterruptedException {
        if (affectsRuntime) {
            log.debugf("Entered VERIFY stage; waiting for service container to settle");
            modelController.awaitContainerMonitor(true, 1);
        }
    }

    @Override
    ConfigurationPersister.PersistenceResource createPersistenceResource() throws ConfigurationPersistenceException {
        return modelController.writeModel(model, affectsModel);
    }

    public boolean isBooting() {
        return booting;
    }

    @Override
    public boolean isRollbackOnRuntimeFailure() {
        return contextFlags.contains(ContextFlag.ROLLBACK_ON_FAIL);
    }

    @Override
    public boolean isResourceServiceRestartAllowed() {
        return contextFlags.contains(ContextFlag.ALLOW_RESOURCE_SERVICE_RESTART);
    }


    public ManagementResourceRegistration getResourceRegistrationForUpdate() {
        final PathAddress address = activeStep.address;
        assert isControllingThread();
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
        assert isControllingThread();
        Stage currentStage = this.currentStage;
        if (currentStage == null || currentStage == Stage.DONE) {
            throw new IllegalStateException("Operation already complete");
        }
        ImmutableManagementResourceRegistration delegate = modelController.getRootRegistration().getSubModel(address);
        return delegate == null ? null : new DelegatingImmutableManagementResourceRegistration(delegate);
    }

    public ServiceRegistry getServiceRegistry(final boolean modify) throws UnsupportedOperationException {
        assert isControllingThread();
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
        assert isControllingThread();
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
        assert isControllingThread();
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

            public void transition(final ServiceController<?> controller, final ServiceController.Transition transition) {
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
        assert isControllingThread();
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
        assert isControllingThread();
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
        assert isControllingThread();
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
        assert isControllingThread();
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
        assert isControllingThread();
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
        for (PathElement element : address) {
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
        assert isControllingThread();
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
        assert isControllingThread();
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

    @Override
    void releaseStepLocks(AbstractOperationContext.Step step) {

        if (this.lockStep == step) {
            modelController.releaseLock();
            lockStep = null;
        }
        if (this.containerMonitorStep == step) {
            awaitContainerMonitor();
            modelController.releaseContainerMonitor();
            containerMonitorStep = null;
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
