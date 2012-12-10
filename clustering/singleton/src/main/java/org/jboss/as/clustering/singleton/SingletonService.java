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

package org.jboss.as.clustering.singleton;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.ClusterNode;
import org.jboss.as.clustering.GroupRpcDispatcher;
import org.jboss.as.clustering.ResponseFilter;
import org.jboss.as.clustering.impl.CoreGroupCommunicationService;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.clustering.msc.DelegatingServiceBuilder;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.clustering.msc.ServiceControllerFactory;
import org.jboss.as.clustering.service.ServiceProviderRegistry;
import org.jboss.as.clustering.service.ServiceProviderRegistryService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Decorates an MSC service ensuring that it is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
public class SingletonService<T> implements Service<T>, ServiceProviderRegistry.Listener, SingletonRpcHandler<T>, Singleton {

    public static final String DEFAULT_CONTAINER = "singleton";

    private final InjectedValue<ServiceProviderRegistry> registryRef = new InjectedValue<ServiceProviderRegistry>();
    private final InjectedValue<GroupRpcDispatcher> dispatcherRef = new InjectedValue<GroupRpcDispatcher>();
    private final Service<T> service;
    final ServiceName targetServiceName;
    private final ServiceName singletonServiceName;
    private final AtomicBoolean master = new AtomicBoolean(false);

    volatile ServiceProviderRegistry registry;
    volatile GroupRpcDispatcher dispatcher;
    volatile boolean started = false;
    private volatile SingletonElectionPolicy electionPolicy;
    private volatile SingletonRpcHandler<T> handler;
    private volatile ServiceRegistry container;
    private volatile boolean restartOnMerge = true;

    public SingletonService(Service<T> service, ServiceName serviceName) {
        this.service = service;
        this.singletonServiceName = serviceName;
        this.targetServiceName = serviceName.append("service");
    }

    public ServiceBuilder<T> build(ServiceTarget target) {
        return this.build(target, DEFAULT_CONTAINER);
    }

    public ServiceBuilder<T> build(ServiceTarget target, String container) {
        final ServiceBuilder<T> serviceBuilder = target.addService(this.targetServiceName, this.service).setInitialMode(ServiceController.Mode.NEVER);
        // Remove target service when this service is removed
        final ServiceListener<T> listener = new AbstractServiceListener<T>() {
            @Override
            public void serviceRemoveRequested(ServiceController<? extends T> controller) {
                ServiceController<?> service = controller.getServiceContainer().getService(SingletonService.this.targetServiceName);
                if (service != null) {
                    service.setMode(ServiceController.Mode.REMOVE);
                }
            }
        };
        final ServiceBuilder<T> singletonBuilder = AsynchronousService.addService(target, this.singletonServiceName, this)
                .addAliases(this.singletonServiceName.append("singleton"))
                .addDependency(ServiceProviderRegistryService.getServiceName(container), ServiceProviderRegistry.class, this.registryRef)
                .addDependency(CoreGroupCommunicationService.getServiceName(container), GroupRpcDispatcher.class, this.dispatcherRef)
                .addListener(listener)
        ;
        // Add dependencies to the target service builder, but install should return the installed singleton controller
        return new DelegatingServiceBuilder<T>(serviceBuilder, ServiceControllerFactory.SIMPLE) {
            @Override
            public ServiceBuilder<T> addAliases(ServiceName... aliases) {
                singletonBuilder.addAliases(aliases);
                return this;
            }

            @Override
            public ServiceBuilder<T> setInitialMode(ServiceController.Mode mode) {
                singletonBuilder.setInitialMode(mode);
                return this;
            }

            @Override
            public ServiceController<T> install() {
                super.install();
                return singletonBuilder.install();
            }
        };
    }

    @Override
    public void start(final StartContext context) {
        this.container = context.getController().getServiceContainer();
        this.dispatcher = this.dispatcherRef.getValue();
        this.registry = this.registryRef.getValue();
        final String name = this.singletonServiceName.getCanonicalName();
        this.handler = new RpcHandler(this.dispatcher, name);
        this.dispatcher.registerRPCHandler(name, this);
        this.registry.register(name, this);
        this.started = true;
    }

    @Override
    public void stop(StopContext context) {
        this.started = false;
        String name = this.singletonServiceName.getCanonicalName();
        this.registry.unregister(name);
        this.dispatcher.unregisterRPCHandler(name, this);
        // Make sure our service is stopped before returning
        this.stopOldMaster();
    }

    @Override
    public boolean isMaster() {
        return this.master.get();
    }

    public void setElectionPolicy(SingletonElectionPolicy electionPolicy) {
        this.electionPolicy = electionPolicy;
    }

    public void setRestartOnMerge(boolean restart) {
        this.restartOnMerge = restart;
    }

    @Override
    public void serviceProvidersChanged(Set<ClusterNode> nodes, boolean merge) {
        if (this.elected(nodes)) {
            if (this.master.get()) {
                // If already master, don't bother re-electing, just restart if necessary
                if (this.restartOnMerge && merge) {
                    this.stopOldMaster();
                    this.startNewMaster();
                }
            } else {
                SingletonLogger.ROOT_LOGGER.electedMaster(this.singletonServiceName.getCanonicalName());
                this.handler.stopOldMaster();
                this.startNewMaster();
            }
        } else if (this.master.get()) {
            SingletonLogger.ROOT_LOGGER.electedSlave(this.singletonServiceName.getCanonicalName());
            this.stopOldMaster();
        }
    }

    private boolean elected(Set<ClusterNode> candidates) {
        ClusterNode elected = this.election(candidates);
        if (elected != null) {
            SingletonLogger.ROOT_LOGGER.elected(elected.getName(), this.singletonServiceName.getCanonicalName());
        }
        return (elected != null) ? elected.equals(this.dispatcher.getClusterNode()) : false;
    }

    private ClusterNode election(Set<ClusterNode> candidates) {
        List<ClusterNode> nodes = this.dispatcher.getClusterNodes();

        nodes.retainAll(candidates);

        if (nodes.isEmpty()) return null;

        return (this.electionPolicy == null) ? nodes.get(0) : this.electionPolicy.elect(nodes);
    }

    private void startNewMaster() {
        this.master.set(true);
        final ServiceController<?> controller = this.container.getRequiredService(this.targetServiceName);
        final Queue<ServiceController<?>> installedControllers = new ConcurrentLinkedQueue<ServiceController<?>>();
        final ServiceListener<Object> listener = new AbstractServiceListener<Object>() {
            @Override
            public void transition(ServiceController<? extends Object> serviceController, ServiceController.Transition transition) {
                // We only want to track services installed by the target service
                if (serviceController != controller) {
                    if (serviceController.getState() == ServiceController.State.STARTING) {
                        installedControllers.add(serviceController);
                    }
                }
            }
        };
        controller.addListener(ServiceListener.Inheritance.ALL, listener);
        try {
            ServiceContainerHelper.start(controller);
            while (!installedControllers.isEmpty()) {
                ServiceController<?> installedController = installedControllers.remove();
                if (!ServiceContainerHelper.wait(installedController, EnumSet.of(ServiceController.State.STARTING), ServiceController.State.UP)) {
                    throw installedController.getStartException();
                }
            }
        } catch (StartException e) {
            SingletonLogger.ROOT_LOGGER.serviceStartFailed(e, this.targetServiceName.getCanonicalName());
            ServiceContainerHelper.stop(controller);
        } finally {
            controller.removeListener(listener);
        }
    }

    @Override
    public T getValue() {
        AtomicReference<T> ref = this.getValueRef();
        if (ref == null) {
            ref = this.handler.getValueRef();
        }
        return ref.get();
    }

    @Override
    public AtomicReference<T> getValueRef() {
        return this.master.get() ? new AtomicReference<T>(this.service.getValue()) : null;
    }

    @Override
    public void stopOldMaster() {
        if (this.master.compareAndSet(true, false)) {
            ServiceContainerHelper.stop(this.container.getRequiredService(this.targetServiceName));
        }
    }

    private class RpcHandler implements SingletonRpcHandler<T>, ResponseFilter {
        private final GroupRpcDispatcher dispatcher;
        private String name;

        RpcHandler(GroupRpcDispatcher dispatcher, String name) {
            this.dispatcher = dispatcher;
            this.name = name;
        }

        @Override
        public void stopOldMaster() {
            try {
                this.dispatcher.callMethodOnCluster(this.name, "stopOldMaster", new Object[0], new Class<?>[0], true);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public AtomicReference<T> getValueRef() {
            try {
                List<AtomicReference<T>> results = Collections.emptyList();
                while (results.isEmpty()) {
                    if (!SingletonService.this.started) {
                        throw new IllegalStateException(SingletonMessages.MESSAGES.notStarted(this.name));
                    }
                    results = this.dispatcher.callMethodOnCluster(this.name, "getValueRef", new Object[0], new Class<?>[0], false, this);
                    Iterator<AtomicReference<T>> refs = results.iterator();
                    while (refs.hasNext()) {
                        // Prune non-master results
                        if (refs.next() == null) {
                            refs.remove();
                        }
                    }
                    // We expect only 1 result
                    int count = results.size();
                    if (count > 1) {
                        // This would mean there are multiple masters!
                        throw SingletonMessages.MESSAGES.unexpectedResponseCount(this.name, count);
                    }
                    if (count == 0) {
                        // This can happen If we're in the middle of a new master election, so just try again
                        SingletonLogger.ROOT_LOGGER.noResponseFromMaster(this.name);
                        Thread.yield();
                    }
                }
                return results.get(0);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public boolean isAcceptable(Object response, ClusterNode sender) {
            return (response == null) || !(response instanceof IllegalStateException);
        }

        @Override
        public boolean needMoreResponses() {
            return true;
        }
    }
}
