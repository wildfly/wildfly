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

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
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
public class CacheSingletonServiceBuilder<T> implements SingletonServiceBuilder<T>, Service<T>, ServiceProviderRegistration.Listener, SingletonContext<T>, Singleton {

    @SuppressWarnings("rawtypes")
    private final InjectedValue<ServiceProviderRegistry> registry = new InjectedValue<>();
    private final InjectedValue<CommandDispatcherFactory> dispatcherFactory = new InjectedValue<>();
    private final InjectedValue<ServiceProviderRegistration<ServiceName>> registration = new InjectedValue<>();
    private final InjectedValue<CommandDispatcher<SingletonContext<T>>> dispatcher = new InjectedValue<>();
    private final Service<T> primaryService;
    private final ServiceName serviceName;
    private final AtomicBoolean primary = new AtomicBoolean(false);
    private final String containerName;
    private final String cacheName;
    private final AtomicInteger quorum = new AtomicInteger(1);

    private volatile Service<T> backupService;
    private volatile SingletonElectionPolicy electionPolicy = new SimpleSingletonElectionPolicy();
    private volatile ServiceController<T> primaryController;
    private volatile ServiceController<T> backupController;

    public CacheSingletonServiceBuilder(ServiceName serviceName, Service<T> service, String containerName, String cacheName) {
        this.serviceName = serviceName;
        this.primaryService = service;
        this.containerName = containerName;
        this.cacheName = cacheName;
        this.backupService = new PrimaryProxyService<>(this.serviceName, this.dispatcher, this.registration, this.quorum);
    }

    @Override
    public ServiceName getServiceName() {
        return this.serviceName;
    }

    @Override
    public ServiceBuilder<T> build(ServiceTarget target) {
        return new AsynchronousServiceBuilder<>(this.serviceName, this).build(target)
                .addDependency(CacheGroupServiceName.SERVICE_PROVIDER_REGISTRY.getServiceName(this.containerName, this.cacheName), ServiceProviderRegistry.class, this.registry)
                .addDependency(GroupServiceName.COMMAND_DISPATCHER.getServiceName(this.containerName), CommandDispatcherFactory.class, this.dispatcherFactory)
                .setInitialMode(ServiceController.Mode.ACTIVE)
        ;
    }

    @Override
    public SingletonServiceBuilder<T> requireQuorum(int quorum) {
        this.quorum.set(quorum);
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> electionPolicy(SingletonElectionPolicy electionPolicy) {
        this.electionPolicy = electionPolicy;
        return this;
    }

    @Override
    public SingletonServiceBuilder<T> backupService(Service<T> backupService) {
        this.backupService = backupService;
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();
        this.primaryController = target.addService(this.serviceName.append("primary"), this.primaryService).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        this.backupController = target.addService(this.serviceName.append("backup"), this.backupService).setInitialMode(ServiceController.Mode.PASSIVE).install();
        this.dispatcher.inject(this.dispatcherFactory.getValue().<SingletonContext<T>>createCommandDispatcher(this.serviceName, this));
        this.registration.inject(this.registry.getValue().register(this.serviceName, this));
    }

    @Override
    public void stop(StopContext context) {
        this.registration.getValue().close();
        this.registration.uninject();
        this.dispatcher.getValue().close();
        this.dispatcher.uninject();
    }

    @Override
    public boolean isPrimary() {
        return this.primary.get();
    }

    @Override
    public void providersChanged(Set<Node> nodes) {
        Group group = this.registry.getValue().getGroup();
        List<Node> candidates = group.getNodes();
        candidates.retainAll(nodes);

        // Only run election on a single node
        if (candidates.isEmpty() || candidates.get(0).equals(group.getLocalNode())) {
            Node elected = null;

            // First validate that quorum was met
            int size = candidates.size();
            int quorum = this.quorum.intValue();
            if (size >= quorum) {
                if ((quorum > 1) && (size == quorum)) {
                    ClusteringServerLogger.ROOT_LOGGER.quorumJustReached(this.serviceName.getCanonicalName(), quorum);
                }

                if (!candidates.isEmpty()) {
                    elected = this.electionPolicy.elect(candidates);

                    ClusteringServerLogger.ROOT_LOGGER.elected(elected.getName(), this.serviceName.getCanonicalName());
                }
            } else if (quorum > 1) {
                ClusteringServerLogger.ROOT_LOGGER.quorumNotReached(this.serviceName.getCanonicalName(), quorum);
            }

            CommandDispatcher<SingletonContext<T>> dispatcher = this.dispatcher.getValue();
            try {
                if (elected != null) {
                    // Stop service on every node except elected node
                    dispatcher.executeOnCluster(new StopCommand<>(), elected);
                    // Start service on elected node
                    dispatcher.executeOnNode(new StartCommand<>(), elected);
                } else {
                    // Stop service on every node
                    dispatcher.executeOnCluster(new StopCommand<>());
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void start() {
        // If we were not already the primary node
        if (this.primary.compareAndSet(false, true)) {
            ClusteringServerLogger.ROOT_LOGGER.startSingleton(this.serviceName.getCanonicalName());
            ServiceContainerHelper.stop(this.backupController);
            start(this.primaryController);
        }
    }

    @Override
    public void stop() {
        // If we were the previous the primary node
        if (this.primary.compareAndSet(true, false)) {
            ClusteringServerLogger.ROOT_LOGGER.stopSingleton(this.serviceName.getCanonicalName());
            ServiceContainerHelper.stop(this.primaryController);
            start(this.backupController);
        }
    }

    private static void start(ServiceController<?> controller) {
        try {
            ServiceContainerHelper.start(controller);
        } catch (StartException e) {
            ClusteringServerLogger.ROOT_LOGGER.serviceStartFailed(e, controller.getName().getCanonicalName());
            ServiceContainerHelper.stop(controller);
        }
    }

    @Override
    public T getValue() {
        return (this.primary.get() ? this.primaryController : this.backupController).getValue();
    }

    @Override
    public AtomicReference<T> getValueRef() {
        try {
            return this.primary.get() ? new AtomicReference<>(this.primaryController.getValue()) : null;
        } catch (IllegalStateException e) {
            // This might happen if primary service has not yet started, or if node is no longer the primary node
            return null;
        }
    }
}
