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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.provider.ServiceProviderRegistry;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;
import org.wildfly.clustering.singleton.SingletonElectionListener;
import org.wildfly.clustering.singleton.SingletonElectionPolicy;
import org.wildfly.clustering.singleton.service.SingletonService;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Logic common to current and legacy {@link SingletonService} implementations.
 * @author Paul Ferraro
 */
public abstract class AbstractDistributedSingletonService<C extends SingletonContext> implements SingletonService, SingletonContext, Listener, Supplier<C> {

    private final ServiceName name;
    private final Supplier<ServiceProviderRegistry<ServiceName>> registry;
    private final Supplier<CommandDispatcherFactory> dispatcherFactory;
    private final SingletonElectionPolicy electionPolicy;
    private final SingletonElectionListener electionListener;
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
        this.electionListener = context.getElectionListener();
        this.quorum = context.getQuorum();
        this.primaryLifecycleFactory = primaryLifecycleFactory;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ServiceTarget target = context.getChildTarget();

        this.primaryLifecycle = this.primaryLifecycleFactory.apply(target);

        this.dispatcher = this.dispatcherFactory.get().createCommandDispatcher(this.name, this.get(), WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
        this.registration = this.registry.get().register(this.name, this);
    }

    @Override
    public void stop(StopContext context) {
        this.registration.close();
        this.dispatcher.close();
    }

    @Override
    public synchronized void providersChanged(Set<Node> nodes) {
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
                    // Stop service on every node except elected node
                    for (Map.Entry<Node, CompletionStage<Void>> entry : this.dispatcher.executeOnGroup(new StopCommand(), elected).entrySet()) {
                        try {
                            entry.getValue().toCompletableFuture().join();
                        } catch (CancellationException e) {
                            ClusteringServerLogger.ROOT_LOGGER.tracef("Singleton service %s is not installed on %s", this.name.getCanonicalName(), entry.getKey().getName());
                        } catch (CompletionException e) {
                            Throwable cause = e.getCause();
                            if ((cause instanceof IllegalStateException) && (cause.getCause() instanceof ServiceNotFoundException)) {
                                ClusteringServerLogger.ROOT_LOGGER.debugf("Singleton service %s is no longer installed on %s", this.name.getCanonicalName(), entry.getKey().getName());
                            } else {
                                throw e;
                            }
                        }
                    }
                    // Start service on elected node
                    try {
                        this.dispatcher.executeOnMember(new StartCommand(), elected).toCompletableFuture().join();
                    } catch (CancellationException e) {
                        ClusteringServerLogger.ROOT_LOGGER.debugf("Singleton service %s could not be started on the elected primary singleton provider (%s) because it left the cluster.  A new primary provider election will take place.", this.name.getCanonicalName(), elected.getName());
                    } catch (CompletionException e) {
                        Throwable cause = e.getCause();
                        if ((cause instanceof IllegalStateException) && (cause.getCause() instanceof ServiceNotFoundException)) {
                            ClusteringServerLogger.ROOT_LOGGER.debugf("Service % is no longer installed on the elected primary singleton provider (%s). A new primary provider election will take place.", this.name.getCanonicalName(), elected.getName());
                        } else {
                            throw e;
                        }
                    }
                } else {
                    if (!quorumMet) {
                        ClusteringServerLogger.ROOT_LOGGER.quorumNotReached(this.name.getCanonicalName(), this.quorum);
                    }

                    // Stop service on every node
                    for (Map.Entry<Node, CompletionStage<Void>> entry : this.dispatcher.executeOnGroup(new StopCommand()).entrySet()) {
                        try {
                            entry.getValue().toCompletableFuture().join();
                        } catch (CancellationException e) {
                            ClusteringServerLogger.ROOT_LOGGER.tracef("Singleton service %s is not installed on %s", this.name.getCanonicalName(), entry.getKey().getName());
                        } catch (CompletionException e) {
                            Throwable cause = e.getCause();
                            if ((cause instanceof IllegalStateException) && (cause.getCause() instanceof ServiceNotFoundException)) {
                                ClusteringServerLogger.ROOT_LOGGER.debugf("Singleton service %s is no longer installed on %s", this.name.getCanonicalName(), entry.getKey().getName());
                            } else {
                                throw e;
                            }
                        }
                    }
                }

                if (this.electionListener != null) {
                    for (CompletionStage<Void> stage : this.dispatcher.executeOnGroup(new SingletonElectionCommand(candidates, elected)).values()) {
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
    public synchronized void start() {
        // If we were not already the primary node
        if (this.primary.compareAndSet(false, true)) {
            this.primaryLifecycle.start();
        }
    }

    @Override
    public synchronized void stop() {
        // If we were the previous the primary node
        if (this.primary.compareAndSet(true, false)) {
            this.primaryLifecycle.stop();
        }
    }

    @Override
    public void elected(List<Node> candidates, Node elected) {
        try {
            this.electionListener.elected(candidates, elected);
        } catch (Throwable e) {
            ClusteringServerLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public boolean isPrimary() {
        return this.primary.get();
    }

    @Override
    public Node getPrimaryProvider() {
        if (this.isPrimary()) return this.registry.get().getGroup().getLocalMember();

        List<Node> primaryMembers = new LinkedList<>();
        try {
            for (Map.Entry<Node, CompletionStage<Boolean>> entry : this.dispatcher.executeOnGroup(new PrimaryProviderCommand()).entrySet()) {
                try {
                    if (entry.getValue().toCompletableFuture().join()) {
                        primaryMembers.add(entry.getKey());
                    }
                } catch (CancellationException e) {
                    // Ignore
                }
            }
            if (primaryMembers.size() > 1) {
                throw ClusteringServerLogger.ROOT_LOGGER.multiplePrimaryProvidersDetected(this.name.getCanonicalName(), primaryMembers);
            }
            return !primaryMembers.isEmpty() ? primaryMembers.get(0) : null;
        } catch (CommandDispatcherException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<Node> getProviders() {
        return this.registration.getProviders();
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
