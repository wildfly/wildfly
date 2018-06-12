/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonService;

/**
 * Logic common to current and legacy {@link SingletonService} implementations.
 * @author Paul Ferraro
 */
public abstract class AbstractDistributedSingletonService<C extends Lifecycle> implements SingletonService, Lifecycle, Listener, Supplier<C> {

    private final ServiceName name;
    private final Supplier<ServiceProviderRegistry<ServiceName>> registry;
    private final Supplier<CommandDispatcherFactory> dispatcherFactory;
    private final SingletonElectionPolicy electionPolicy;
    private final int quorum;
    private final Function<ServiceTarget, Lifecycle> primaryLifecycleFactory;

    private final AtomicBoolean primary = new AtomicBoolean(false);

    private volatile Lifecycle primaryLifecycle;
    private volatile CommandDispatcher<C> dispatcher;
    private volatile ServiceProviderRegistration<ServiceName> registration;

    public AbstractDistributedSingletonService(DistributedSingletonServiceContext context, Function<ServiceTarget, Lifecycle> primaryLifecycleFactory) {
        this.name = context.getServiceName();
        this.registry = context.getServiceProviderRegistry();
        this.dispatcherFactory = context.getCommandDispatcherFactory();
        this.electionPolicy = context.getElectionPolicy();
        this.quorum = context.getQuorum();
        this.primaryLifecycleFactory = primaryLifecycleFactory;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();

        this.primaryLifecycle = this.primaryLifecycleFactory.apply(target);

        this.dispatcher = this.dispatcherFactory.get().createCommandDispatcher(this.name, this.get());
        this.registration = this.registry.get().register(this.name, this);
    }

    @Override
    public void stop(StopContext context) {
        this.registration.close();
        this.dispatcher.close();
    }

    @Override
    public void providersChanged(Set<Node> nodes) {
        Group group = this.registry.get().getGroup();
        List<Node> candidates = new ArrayList<>(group.getMembership().getMembers());
        candidates.retainAll(nodes);

        // Only run election on a single node
        if (candidates.isEmpty() || candidates.get(0).equals(group.getLocalMember())) {
            // First validate that quorum was met
            int size = candidates.size();
            boolean quorumMet = size >= this.quorum;

            if ((this.quorum > 1) && (size == this.quorum)) {
                // Log fragility of singleton availability
                ClusteringServerLogger.ROOT_LOGGER.quorumJustReached(this.name.getCanonicalName(), this.quorum);
            }

            Node elected = quorumMet ? this.electionPolicy.elect(candidates) : null;

            try {
                if (elected != null) {
                    ClusteringServerLogger.ROOT_LOGGER.elected(elected.getName(), this.name.getCanonicalName());

                    // Stop service on every node except elected node
                    for (CompletionStage<Void> stage : this.dispatcher.executeOnGroup(new StopCommand(), elected).values()) {
                        try {
                            stage.toCompletableFuture().join();
                        } catch (CancellationException e) {
                            // Ignore
                        }
                    }
                    // Start service on elected node
                    this.dispatcher.executeOnMember(new StartCommand(), elected).toCompletableFuture().join();
                } else {
                    if (quorumMet) {
                        ClusteringServerLogger.ROOT_LOGGER.noPrimaryElected(this.name.getCanonicalName());
                    } else {
                        ClusteringServerLogger.ROOT_LOGGER.quorumNotReached(this.name.getCanonicalName(), this.quorum);
                    }

                    // Stop service on every node
                    for (CompletionStage<Void> stage : this.dispatcher.executeOnGroup(new StopCommand()).values()) {
                        try {
                            stage.toCompletableFuture().join();
                        } catch (CancellationException e) {
                            // Ignore
                        }
                    }
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
            ClusteringServerLogger.ROOT_LOGGER.startSingleton(this.name.getCanonicalName());
            this.primaryLifecycle.start();
        }
    }

    @Override
    public void stop() {
        // If we were the previous the primary node
        if (this.primary.compareAndSet(true, false)) {
            ClusteringServerLogger.ROOT_LOGGER.stopSingleton(this.name.getCanonicalName());
            this.primaryLifecycle.stop();
        }
    }

    @Override
    public boolean isPrimary() {
        return this.primary.get();
    }

    int getQuorum() {
        return this.quorum;
    }

    CommandDispatcher<C> getCommandDispatcher() {
        return this.dispatcher;
    }

    ServiceProviderRegistration<ServiceName> getServiceProviderRegistration() {
        return this.registration;
    }
}
