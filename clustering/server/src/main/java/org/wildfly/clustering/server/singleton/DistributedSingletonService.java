/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.as.clustering.msc.ServiceContainerHelper;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.SingletonService;

/**
 * Decorates an MSC service ensuring that it is only started on one node in the cluster at any given time.
 * @author Paul Ferraro
 */
public class DistributedSingletonService<T> implements SingletonService<T>, SingletonContext<T>, ServiceProviderRegistration.Listener, PrimaryProxyContext<T> {

    private final Value<ServiceProviderRegistry<ServiceName>> registry;
    private final Value<CommandDispatcherFactory> dispatcherFactory;
    private final ServiceName serviceName;
    private final Service<T> primaryService;
    private final Service<T> backupService;
    private final SingletonElectionPolicy electionPolicy;
    private final int quorum;

    private final AtomicBoolean primary = new AtomicBoolean(false);

    private volatile ServiceController<T> primaryController;
    private volatile ServiceController<T> backupController;
    private volatile CommandDispatcher<SingletonContext<T>> dispatcher;
    private volatile ServiceProviderRegistration<ServiceName> registration;
    private volatile boolean started = false;

    public DistributedSingletonService(DistributedSingletonServiceContext<T> context) {
        this.registry = context.getServiceProviderRegistry();
        this.dispatcherFactory = context.getCommandDispatcherFactory();
        this.serviceName = context.getServiceName();
        this.primaryService = context.getPrimaryService();
        this.backupService = context.getBackupService().orElse(new PrimaryProxyService<>(this));
        this.electionPolicy = context.getElectionPolicy();
        this.quorum = context.getQuorum();
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();
        this.primaryController = target.addService(this.serviceName.append("primary"), this.primaryService).setInitialMode(ServiceController.Mode.NEVER).install();
        this.backupController = target.addService(this.serviceName.append("backup"), this.backupService).setInitialMode(ServiceController.Mode.ACTIVE).install();
        this.dispatcher = this.dispatcherFactory.getValue().<SingletonContext<T>>createCommandDispatcher(this.serviceName, this);
        this.registration = this.registry.getValue().register(this.serviceName, this);
        this.started = true;
    }

    @Override
    public void stop(StopContext context) {
        this.started = false;
        this.registration.close();
        this.dispatcher.close();
    }

    @Override
    public boolean isPrimary() {
        return this.primary.get();
    }

    @Override
    public void providersChanged(Set<Node> nodes) {
        Group group = this.registry.getValue().getGroup();
        List<Node> candidates = new ArrayList<>(group.getMembership().getMembers());
        candidates.retainAll(nodes);

        // Only run election on a single node
        if (candidates.isEmpty() || candidates.get(0).equals(group.getLocalMember())) {
            // First validate that quorum was met
            int size = candidates.size();
            boolean quorumMet = size >= this.quorum;

            if ((this.quorum > 1) && (size == this.quorum)) {
                // Log fragility of singleton availability
                ClusteringServerLogger.ROOT_LOGGER.quorumJustReached(this.serviceName.getCanonicalName(), this.quorum);
            }

            Node elected = quorumMet ? this.electionPolicy.elect(candidates) : null;

            try {
                if (elected != null) {
                    ClusteringServerLogger.ROOT_LOGGER.elected(elected.getName(), this.serviceName.getCanonicalName());

                    // Stop service on every node except elected node
                    this.dispatcher.executeOnCluster(new StopCommand<>(), elected);
                    // Start service on elected node
                    this.dispatcher.executeOnNode(new StartCommand<>(), elected);
                } else {
                    if (quorumMet) {
                        ClusteringServerLogger.ROOT_LOGGER.noPrimaryElected(this.serviceName.getCanonicalName());
                    } else {
                        ClusteringServerLogger.ROOT_LOGGER.quorumNotReached(this.serviceName.getCanonicalName(), this.quorum);
                    }

                    // Stop service on every node
                    this.dispatcher.executeOnCluster(new StopCommand<>());
                }
            } catch (CommandDispatcherException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @Override
    public void start() {
        // If we were not already the primary node
        if (this.primary.compareAndSet(false, true)) {
            ClusteringServerLogger.ROOT_LOGGER.startSingleton(this.serviceName.getCanonicalName());
            toggle(this.backupController, this.primaryController);
        }
    }

    @Override
    public void stop() {
        // If we were the previous the primary node
        if (this.primary.compareAndSet(true, false)) {
            ClusteringServerLogger.ROOT_LOGGER.stopSingleton(this.serviceName.getCanonicalName());
            toggle(this.primaryController, this.backupController);
        }
    }

    private static synchronized void toggle(ServiceController<?> controllerToStop, ServiceController<?> controllerToStart) {
        ServiceContainerHelper.stop(controllerToStop);
        try {
            ServiceContainerHelper.start(controllerToStart);
        } catch (StartException e) {
            ClusteringServerLogger.ROOT_LOGGER.serviceStartFailed(e, controllerToStart.getName().getCanonicalName());
            ServiceContainerHelper.stop(controllerToStart);
        }
    }

    @Override
    public T getValue() {
        while (this.started) {
            try {
                return (this.primary.get() ? this.primaryController : this.backupController).getValue();
            } catch (IllegalStateException e) {
                // Verify whether ISE is due to unmet quorum in the previous election
                if (this.registration.getProviders().size() < this.quorum) {
                    throw ClusteringServerLogger.ROOT_LOGGER.notStarted(this.serviceName.getCanonicalName());
                }
                if (Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                // Otherwise, we're in the midst of a new election, so just try again
                Thread.yield();
            }
        }
        throw ClusteringServerLogger.ROOT_LOGGER.notStarted(this.serviceName.getCanonicalName());
    }

    @Override
    public Optional<T> getLocalValue() {
        try {
            return this.primary.get() ? Optional.ofNullable(this.primaryController.getValue()) : null;
        } catch (IllegalStateException e) {
            // This might happen if primary service has not yet started, or if node is no longer the primary node
            return null;
        }
    }

    @Override
    public CommandDispatcher<SingletonContext<T>> getCommandDispatcher() {
        return this.dispatcher;
    }

    @Override
    public ServiceName getServiceName() {
        return this.serviceName;
    }
}
