/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.service.Service;
import org.wildfly.clustering.server.provider.ServiceProviderRegistration;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationEvent;
import org.wildfly.clustering.server.provider.ServiceProviderRegistrationListener;
import org.wildfly.clustering.singleton.SingletonState;
import org.wildfly.clustering.singleton.election.SingletonElectionListener;
import org.wildfly.clustering.singleton.election.SingletonElectionPolicy;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractSingletonContext<C extends SingletonContext, S extends Service> implements SingletonContextRegistration<C>, Supplier<C>, ServiceProviderRegistrationListener<GroupMember> {

    private final ServiceName name;
    private final AtomicBoolean primary = new AtomicBoolean(false);
    private final S service;
    private final SingletonElectionListener electionListener;
    private final SingletonElectionPolicy electionPolicy;
    private final int quorum;
    private final Group<GroupMember> group;
    private final CommandDispatcher<GroupMember, C> dispatcher;
    private final ServiceProviderRegistration<ServiceName, GroupMember> registration;

    public AbstractSingletonContext(SingletonServiceContext context, S service) {
        this.name = context.getServiceName();
        this.service = service;
        this.electionListener = context.getElectionListener();
        this.electionPolicy = context.getElectionPolicy();
        this.quorum = context.getQuorum();
        this.group = context.getCommandDispatcherFactory().getGroup();
        this.dispatcher = context.getCommandDispatcherFactory().createCommandDispatcher(this.name, this.get(), WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
        this.registration = context.getServiceProviderRegistrar().register(this.name, this);
    }

    @Override
    public CommandDispatcher<GroupMember, C> getCommandDispatcher() {
        return this.dispatcher;
    }

    @Override
    public ServiceProviderRegistration<ServiceName, GroupMember> getServiceProviderRegistration() {
        return this.registration;
    }

    @Override
    public boolean isStarted() {
        return this.service.isStarted();
    }

    @Override
    public synchronized void start() {
        // If we were not already the primary node
        if (this.primary.compareAndSet(false, true)) {
            this.service.start();
        }
    }

    @Override
    public synchronized void stop() {
        // If we were the previous the primary node
        if (this.primary.compareAndSet(true, false)) {
            this.service.stop();
        }
    }

    @Override
    public void elected(List<GroupMember> candidates, GroupMember elected) {
        try {
            this.electionListener.elected(candidates, elected);
        } catch (Throwable e) {
            SingletonLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public SingletonState getSingletonState() {
        Queue<GroupMember> primaryMembers = new LinkedList<>();
        try {
            Map<GroupMember, CompletionStage<Boolean>> results = this.dispatcher.dispatchToGroup(PrimaryProviderCommand.INSTANCE);
            Iterator<Map.Entry<GroupMember, CompletionStage<Boolean>>> entries = results.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry<GroupMember, CompletionStage<Boolean>> entry = entries.next();
                try {
                    if (entry.getValue().toCompletableFuture().join()) {
                        primaryMembers.add(entry.getKey());
                    }
                } catch (CancellationException e) {
                    // Member does not provide this service
                    entries.remove();
                }
            }
            if (primaryMembers.size() > 1) {
                throw SingletonLogger.ROOT_LOGGER.multiplePrimaryProvidersDetected(this.name.getCanonicalName(), primaryMembers);
            }
            GroupMember localMember = this.group.getLocalMember();
            Optional<GroupMember> primaryMember = Optional.ofNullable(primaryMembers.peek());
            return new SingletonState() {
                @Override
                public boolean isPrimaryProvider() {
                    return primaryMember.filter(localMember::equals).isPresent();
                }

                @Override
                public Optional<GroupMember> getPrimaryProvider() {
                    return primaryMember;
                }

                @Override
                public Set<GroupMember> getProviders() {
                    return Collections.unmodifiableSet(results.keySet());
                }
            };
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isPrimaryProvider() {
        return this.primary.get();
    }

    @Override
    public void providersChanged(ServiceProviderRegistrationEvent<GroupMember> event) {
        List<GroupMember> candidates = new ArrayList<>(this.group.getMembership().getMembers());
        candidates.retainAll(event.getCurrentProviders());

        // Only run election on a single node
        if (candidates.isEmpty() || candidates.get(0).equals(this.group.getLocalMember())) {
            // First validate that quorum was met
            int size = candidates.size();
            boolean quorumMet = size >= this.quorum;

            if ((this.quorum > 1) && (size == this.quorum)) {
                // Log fragility of singleton availability
                SingletonLogger.ROOT_LOGGER.quorumJustReached(this.name.getCanonicalName(), this.quorum);
            }

            GroupMember elected = quorumMet ? this.electionPolicy.elect(candidates) : null;

            try {
                if (elected != null) {
                    // Stop service on every node except elected node
                    for (Map.Entry<GroupMember, CompletionStage<Void>> entry : this.dispatcher.dispatchToGroup(StopCommand.INSTANCE, Set.of(elected)).entrySet()) {
                        try {
                            entry.getValue().toCompletableFuture().join();
                        } catch (CancellationException e) {
                            SingletonLogger.ROOT_LOGGER.tracef("%s is not installed on %s", this.name, entry.getKey().getName());
                        } catch (CompletionException e) {
                            Throwable cause = e.getCause();
                            if ((cause instanceof IllegalStateException) && (cause.getCause() instanceof ServiceNotFoundException)) {
                                SingletonLogger.ROOT_LOGGER.debugf("%s is no longer installed on %s", this.name, entry.getKey().getName());
                            } else {
                                throw e;
                            }
                        }
                    }
                    // Start service on elected node
                    try {
                        this.dispatcher.dispatchToMember(StartCommand.INSTANCE, elected).toCompletableFuture().join();
                    } catch (CancellationException e) {
                        SingletonLogger.ROOT_LOGGER.debugf("%s could not be started on the elected primary singleton provider (%s) because it left the cluster.  A new primary provider election will take place.", this.name, elected.getName());
                    } catch (CompletionException e) {
                        Throwable cause = e.getCause();
                        if ((cause instanceof IllegalStateException) && (cause.getCause() instanceof ServiceNotFoundException)) {
                            SingletonLogger.ROOT_LOGGER.debugf("%s is no longer installed on the elected primary singleton provider (%s). A new primary provider election will take place.", this.name, elected.getName());
                        } else {
                            throw e;
                        }
                    }
                } else {
                    if (!quorumMet) {
                        SingletonLogger.ROOT_LOGGER.quorumNotReached(this.name.getCanonicalName(), this.quorum);
                    }

                    // Stop service on every node
                    for (Map.Entry<GroupMember, CompletionStage<Void>> entry : this.dispatcher.dispatchToGroup(StopCommand.INSTANCE).entrySet()) {
                        try {
                            entry.getValue().toCompletableFuture().join();
                        } catch (CancellationException e) {
                            SingletonLogger.ROOT_LOGGER.tracef("%s is not installed on %s", this.name, entry.getKey().getName());
                        } catch (CompletionException e) {
                            Throwable cause = e.getCause();
                            if ((cause instanceof IllegalStateException) && (cause.getCause() instanceof ServiceNotFoundException)) {
                                SingletonLogger.ROOT_LOGGER.debugf("%s is no longer installed on %s", this.name, entry.getKey().getName());
                            } else {
                                throw e;
                            }
                        }
                    }
                }

                if (this.electionListener != null) {
                    for (CompletionStage<Void> stage : this.dispatcher.dispatchToGroup(new SingletonElectionCommand(candidates, elected)).values()) {
                        try {
                            stage.toCompletableFuture().join();
                        } catch (CancellationException e) {
                            // Ignore
                        }
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
