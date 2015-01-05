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

import java.io.Serializable;
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
import org.wildfly.clustering.singleton.election.SimpleSingletonElectionPolicy;
import org.wildfly.clustering.spi.CacheGroupServiceNameFactory;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceName;

/**
 * Decorates an MSC service ensuring that it is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class SingletonService<T extends Serializable> implements Service<T>, ServiceProviderRegistration.Listener, SingletonContext<T>, Singleton {

    public static final String DEFAULT_CONTAINER = "server";

    private final InjectedValue<Group> group = new InjectedValue<>();
    private final InjectedValue<ServiceProviderRegistrationFactory> registrationFactory = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    private final Service<T> service;
    final ServiceName targetServiceName;
    final ServiceName singletonServiceName;
    private final AtomicBoolean master = new AtomicBoolean(false);
    private final SingletonContext<T> singletonDispatcher = new SingletonDispatcher();

    volatile ServiceProviderRegistration registration;
    volatile CommandDispatcher<SingletonContext<T>> dispatcher;
    volatile boolean started = false;
    private volatile SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
    private volatile ServiceRegistry container;
    volatile int quorum = 1;

    public SingletonService(ServiceName serviceName, Service<T> service) {
        this.singletonServiceName = serviceName;
        this.targetServiceName = serviceName.append("service");
        this.service = service;
    }

    public ServiceBuilder<T> build(ServiceTarget target) {
        return this.build(target, DEFAULT_CONTAINER);
    }

    public ServiceBuilder<T> build(ServiceTarget target, String containerName) {
        return this.build(target, containerName, CacheGroupServiceNameFactory.DEFAULT_CACHE);
    }

    public ServiceBuilder<T> build(ServiceTarget target, String containerName, String cacheName) {
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
        final ServiceBuilder<T> singletonBuilder = new AsynchronousServiceBuilder<>(this.singletonServiceName, this).build(target)
                .addAliases(this.singletonServiceName.append("singleton"))
                .addDependency(CacheGroupServiceName.GROUP.getServiceName(containerName, cacheName), Group.class, this.group)
                .addDependency(CacheGroupServiceName.SERVICE_PROVIDER_REGISTRATION.getServiceName(containerName, cacheName), ServiceProviderRegistrationFactory.class, this.registrationFactory)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
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
    public void start(StartContext context) {
        this.container = context.getController().getServiceContainer();
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

    public void setElectionPolicy(SingletonElectionPolicy electionPolicy) {
        this.electionPolicy = electionPolicy;
    }

    public void setQuorum(int quorum) {
        this.quorum = quorum;
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
        ServiceController<?> service = this.container.getRequiredService(this.targetServiceName);
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
            ServiceContainerHelper.stop(this.container.getRequiredService(this.targetServiceName));
        }
    }

    class SingletonDispatcher implements SingletonContext<T> {

        @Override
        public void stopOldMaster() {
            try {
                SingletonService.this.dispatcher.executeOnCluster(new StopSingletonCommand<T>());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public AtomicReference<T> getValueRef() {
            try {
                Map<Node, CommandResponse<AtomicReference<T>>> results = Collections.emptyMap();
                while (results.isEmpty()) {
                    if (!SingletonService.this.started) {
                        throw new IllegalStateException(ClusteringServerLogger.ROOT_LOGGER.notStarted(SingletonService.this.singletonServiceName.getCanonicalName()));
                    }
                    results = SingletonService.this.dispatcher.executeOnCluster(new SingletonValueCommand<T>());
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
                        throw ClusteringServerLogger.ROOT_LOGGER.unexpectedResponseCount(SingletonService.this.singletonServiceName.getCanonicalName(), count);
                    }
                    if (count == 0) {
                        ClusteringServerLogger.ROOT_LOGGER.noResponseFromMaster(SingletonService.this.singletonServiceName.getCanonicalName());
                        // Verify whether there is no master because a quorum was not reached during the last election
                        if (SingletonService.this.registration.getProviders().size() < SingletonService.this.quorum) {
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
