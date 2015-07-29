/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.singleton;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.msc.DelegatingServiceBuilder;
import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.as.clustering.msc.ServiceControllerFactory;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.singleton.Singleton;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonServiceBuilder;
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Decorates an MSC service ensuring that it is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class CacheSingletonServiceBuilder<T> implements SingletonServiceBuilder<T>, Service<T>, ServiceProviderRegistration.Listener, SingletonContext<T>, Singleton {

    private final InjectedValue<Group> group = new InjectedValue<>();
    private final InjectedValue<ServiceProviderRegistrationFactory> registrationFactory = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    private final Service<T> service;
    final ServiceName targetServiceName;
    final ServiceName singletonServiceName;
    private final AtomicBoolean master = new AtomicBoolean(false);
    private final SingletonContext<T> singletonDispatcher = new SingletonDispatcher();
    private final String containerName;
    private final String cacheName;

    volatile ServiceProviderRegistration registration;
    volatile CommandDispatcher<SingletonContext<T>> dispatcher;
    volatile boolean started = false;
    private volatile SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
    private volatile ServiceRegistry registry;
    volatile int quorum = 1;

    public CacheSingletonServiceBuilder(ServiceName serviceName, Service<T> service, String containerName, String cacheName) {
        this.singletonServiceName = serviceName;
        this.targetServiceName = serviceName.append("service");
        this.service = service;
        this.containerName = containerName;
        this.cacheName = cacheName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.singletonServiceName;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        final ServiceBuilder<T> serviceBuilder = target.addService(this.targetServiceName, this.service).setInitialMode(ServiceController.Mode.NEVER);
        // Remove target service when this service is removed
        final ServiceListener<T> listener = new AbstractServiceListener<T>() {
            @Override
            public void serviceRemoveRequested(ServiceController<? extends T> controller) {
                ServiceController<?> service = controller.getServiceContainer().getService(CacheSingletonServiceBuilder.this.targetServiceName);
                if (service != null) {
                    service.setMode(ServiceController.Mode.REMOVE);
                    controller.removeListener(this);
                }
            }
        };
        final ServiceBuilder<T> singletonBuilder = new AsynchronousServiceBuilder<>(this.singletonServiceName, this).build(target)
                .addAliases(this.singletonServiceName.append("singleton"))
                .addDependency(CacheGroupServiceName.GROUP.getServiceName(this.containerName, this.cacheName), Group.class, this.group)
                .addDependency(CacheGroupServiceName.SERVICE_PROVIDER_REGISTRATION.getServiceName(this.containerName, this.cacheName), ServiceProviderRegistrationFactory.class, this.registrationFactory)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(this.containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
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
            public ServiceBuilder<T> addMonitor(StabilityMonitor monitor) {
                singletonBuilder.addMonitor(monitor);
                return this;
            }

            @Override
            public ServiceBuilder<T> addMonitors(StabilityMonitor... monitors) {
                singletonBuilder.addMonitors(monitors);
                return this;
            }

            @Override
            public ServiceBuilder<T> addListener(ServiceListener<? super T> listener) {
                singletonBuilder.addListener(listener);
                return this;
            }

            @Override
            public ServiceBuilder<T> addListener(@SuppressWarnings("unchecked") ServiceListener<? super T>... listeners) {
                singletonBuilder.addListener(listeners);
                return this;
            }

            @Override
            public ServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> listeners) {
                singletonBuilder.addListener(listeners);
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
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        this.quorum = quorum;
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy electionPolicy) {
        this.electionPolicy = electionPolicy;
        return this;
    }

    @Override
    public void start(StartContext context) {
        this.registry = context.getController().getServiceContainer();
        this.dispatcher = this.dispatcherFactory.getValue().<SingletonContext<T>>createCommandDispatcher(this.singletonServiceName, this);
        this.registration = this.registrationFactory.getValue().createRegistration(this.singletonServiceName, this);
        this.started = true;
    }

    @Override
    public void stop(StopContext context) {
        this.started = false;
        this.registration.close();
        this.dispatcher.close();
    }

    @Override
    public boolean isMaster() {
        return this.master.get();
    }

    @Override
    public void providersChanged(Set<Node> nodes) {
        if (this.elected(nodes)) {
            if (!this.master.get()) {
                ClusteringServerLogger.ROOT_LOGGER.electedMaster(this.singletonServiceName.getCanonicalName());
                this.singletonDispatcher.stopOldMaster();
                this.startNewMaster();
            }
        } else if (this.master.get()) {
            ClusteringServerLogger.ROOT_LOGGER.electedSlave(this.singletonServiceName.getCanonicalName());
            this.stopOldMaster();
        }
    }

    private boolean elected(Set<Node> candidates) {
        int size = candidates.size();
        if (size < this.quorum) {
            ClusteringServerLogger.ROOT_LOGGER.quorumNotReached(this.singletonServiceName.getCanonicalName(), this.quorum);
            return false;
        } else if (size == this.quorum) {
            ClusteringServerLogger.ROOT_LOGGER.quorumJustReached(this.singletonServiceName.getCanonicalName(), this.quorum);
        }
        Node elected = this.election(candidates);
        if (elected != null) {
            ClusteringServerLogger.ROOT_LOGGER.elected(elected.getName(), this.singletonServiceName.getCanonicalName());
        }
        return (elected != null) ? elected.equals(this.group.getValue().getLocalNode()) : false;
    }

    private Node election(Set<Node> candidates) {
        SingletonElectionPolicy policy = this.electionPolicy;
        List<Node> nodes = this.group.getValue().getNodes();
        nodes.retainAll(candidates);
        return !nodes.isEmpty() ? policy.elect(nodes) : null;
    }

    private void startNewMaster() {
        this.master.set(true);
        ServiceController<?> service = this.registry.getRequiredService(this.targetServiceName);
        try {
            ServiceContainerHelper.start(service);
        } catch (StartException e) {
            ClusteringServerLogger.ROOT_LOGGER.serviceStartFailed(e, this.targetServiceName.getCanonicalName());
            ServiceContainerHelper.stop(service);
        }
    }

    @Override
    public T getValue() {
        if (!this.started) throw new IllegalStateException();
        AtomicReference<T> ref = this.getValueRef();
        if (ref == null) {
            ref = this.singletonDispatcher.getValueRef();
        }
        return ref.get();
    }

    @Override
    public AtomicReference<T> getValueRef() {
        return this.master.get() ? new AtomicReference<>(this.service.getValue()) : null;
    }

    @Override
    public void stopOldMaster() {
        if (this.master.compareAndSet(true, false)) {
            ServiceContainerHelper.stop(this.registry.getRequiredService(this.targetServiceName));
        }
    }

    class SingletonDispatcher implements SingletonContext<T> {

        @Override
        public void stopOldMaster() {
            try {
                CacheSingletonServiceBuilder.this.dispatcher.executeOnCluster(new StopSingletonCommand<T>());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public AtomicReference<T> getValueRef() {
            try {
                Map<Node, CommandResponse<AtomicReference<T>>> results = Collections.emptyMap();
                while (results.isEmpty()) {
                    if (!CacheSingletonServiceBuilder.this.started) {
                        throw new IllegalStateException(ClusteringServerLogger.ROOT_LOGGER.notStarted(CacheSingletonServiceBuilder.this.singletonServiceName.getCanonicalName()));
                    }
                    results = CacheSingletonServiceBuilder.this.dispatcher.executeOnCluster(new SingletonValueCommand<T>());
                    Iterator<CommandResponse<AtomicReference<T>>> responses = results.values().iterator();
                    while (responses.hasNext()) {
                        if (responses.next().get() == null) {
                            // Prune non-master results
                            responses.remove();
                        }
                    }
                    // We expect only 1 result
                    int count = results.size();
                    if (count > 1) {
                        // This would mean there are multiple masters!
                        throw ClusteringServerLogger.ROOT_LOGGER.unexpectedResponseCount(CacheSingletonServiceBuilder.this.singletonServiceName.getCanonicalName(), count);
                    }
                    if (count == 0) {
                        ClusteringServerLogger.ROOT_LOGGER.noResponseFromMaster(CacheSingletonServiceBuilder.this.singletonServiceName.getCanonicalName());
                        // Verify whether there is no master because a quorum was not reached during the last election
                        if (CacheSingletonServiceBuilder.this.registration.getProviders().size() < CacheSingletonServiceBuilder.this.quorum) {
                            return new AtomicReference<>();
                        }
                        // Otherwise, we're in the midst of a new master election, so just try again
                        Thread.yield();
                    }
                }
                return results.values().iterator().next().get();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
