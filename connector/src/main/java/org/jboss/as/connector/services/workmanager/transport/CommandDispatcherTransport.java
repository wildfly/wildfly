/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.workmanager.transport;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.resource.spi.work.DistributableWork;
import jakarta.resource.spi.work.WorkException;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.jca.core.api.workmanager.DistributedWorkManager;
import org.jboss.jca.core.spi.workmanager.Address;
import org.jboss.jca.core.workmanager.transport.remote.AbstractRemoteTransport;
import org.jboss.jca.core.workmanager.transport.remote.ProtocolMessages.Request;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.GroupMembership;
import org.wildfly.clustering.server.GroupMembershipEvent;
import org.wildfly.clustering.server.GroupMembershipListener;
import org.wildfly.clustering.server.GroupMembershipMergeEvent;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.util.BlockingExecutor;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * {@link DistributedWorkManager}-specific transport based on a {@link CommandDispatcher}.
 * The current implementation is a direct translation of {@link org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport}.
 * @author Paul Ferraro
 */
public class CommandDispatcherTransport extends AbstractRemoteTransport<GroupMember> implements GroupMembershipListener<GroupMember> {

    private final BlockingExecutor executor;
    private final CommandDispatcherFactory<GroupMember> dispatcherFactory;
    private final String name;

    private volatile CommandDispatcher<GroupMember, CommandDispatcherTransport> dispatcher;
    private volatile Registration groupListenerRegistration;
    private volatile boolean initialized = false;

    public CommandDispatcherTransport(CommandDispatcherFactory<GroupMember> dispatcherFactory, String name) {
        this.dispatcherFactory = dispatcherFactory;
        this.name = name;
        this.executor = BlockingExecutor.newInstance(() -> {
            try {
                CommandDispatcherTransport.this.broadcast(new LeaveCommand(this.getOwnAddress()));
            } catch (WorkException e) {
                ConnectorLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            } finally {
                this.groupListenerRegistration.close();
                this.dispatcher.close();
            }
        });
    }

    @Override
    public String getId() {
        return this.getOwnAddress().getName();
    }

    @Override
    public void startup() throws Exception {
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.name, this, WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
        this.groupListenerRegistration = this.dispatcherFactory.getGroup().register(this);
        this.broadcast(new JoinCommand());
    }

    @Override
    public void shutdown() {
        this.executor.close();
    }

    @Override
    public void initialize() throws Exception {
        this.initialized = true;
    }

    @Override
    public boolean isInitialized() {
        return this.initialized;
    }

    @Override
    protected GroupMember getOwnAddress() {
        return this.dispatcherFactory.getGroup().getLocalMember();
    }

    @Override
    public void register(Address address) {
        // We need to override this method, since base implementation throws ClassCastException
        this.nodes.put(address, null);

        if (address.getTransportId() == null || address.getTransportId().equals(this.getId())) {
            Set<GroupMember> sent = new HashSet<>();
            for (GroupMember member : this.nodes.values()) {
                if (member != null && !sent.contains(member)) {
                    sent.add(member);
                    try {
                        this.sendMessage(member, Request.WORKMANAGER_ADD, address, this.getOwnAddress());
                    } catch (WorkException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
    }

    @Override
    protected Serializable sendMessage(GroupMember physicalAddress, Request request, Serializable... parameters) throws WorkException {
        return (Serializable) this.sendMessage(physicalAddress, request, (Object[]) parameters);
    }

    private Object sendMessage(GroupMember physicalAddress, Request request, Object... parameters) throws WorkException {
        TransportCommand<?> command = createCommand(request, parameters);
        CommandDispatcher<GroupMember, CommandDispatcherTransport> dispatcher = this.dispatcher;
        Supplier<Optional<Object>> task = new Supplier<>() {
            @Override
            public Optional<Object> get() {
                try {
                    CompletionStage<?> response = dispatcher.dispatchToMember(command, physicalAddress);
                    return Optional.ofNullable(response.toCompletableFuture().join());
                } catch (CancellationException e) {
                    return Optional.empty();
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        };
        try {
            Optional<Object> value = this.executor.execute(task).orElse(null);
            return value != null ? value.orElse(null) : null;
        } catch (CompletionException e) {
            throw new WorkException(e.getCause());
        }
    }

    private void broadcast(TransportCommand<Void> command) throws WorkException {
        CommandDispatcher<GroupMember, CommandDispatcherTransport> dispatcher = this.dispatcher;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    for (Map.Entry<GroupMember, CompletionStage<Void>> entry : dispatcher.dispatchToGroup(command).entrySet()) {
                        // Verify that command executed successfully on all nodes
                        try {
                            entry.getValue().toCompletableFuture().join();
                        } catch (CancellationException e) {
                            // Ignore
                        }
                    }
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        };
        try {
            this.executor.execute(task);
        } catch (CompletionException e) {
            throw new WorkException(e.getCause());
        }
    }

    private static TransportCommand<?> createCommand(Request request, Object... parameters) {
        Address address = (parameters.length > 0) ? (Address) parameters[0] : null;
        switch (request) {
            case CLEAR_DISTRIBUTED_STATISTICS: {
                return new ClearDistributedStatisticsCommand(address);
            }
            case DELTA_DOWORK_ACCEPTED: {
                return new DeltaDoWorkAcceptedCommand(address);
            }
            case DELTA_DOWORK_REJECTED: {
                return new DeltaDoWorkRejectedCommand(address);
            }
            case DELTA_SCHEDULEWORK_ACCEPTED: {
                return new DeltaScheduleWorkAcceptedCommand(address);
            }
            case DELTA_SCHEDULEWORK_REJECTED: {
                return new DeltaScheduleWorkRejectedCommand(address);
            }
            case DELTA_STARTWORK_ACCEPTED: {
                return new DeltaStartWorkAcceptedCommand(address);
            }
            case DELTA_STARTWORK_REJECTED: {
                return new DeltaStartWorkRejectedCommand(address);
            }
            case DELTA_WORK_FAILED: {
                return new DeltaWorkFailedCommand(address);
            }
            case DELTA_WORK_SUCCESSFUL: {
                return new DeltaWorkSuccessfulCommand(address);
            }
            case DO_WORK: {
                return new DoWorkCommand(address, (DistributableWork) parameters[2]);
            }
            case GET_DISTRIBUTED_STATISTICS: {
                return new DistributedStatisticsCommand(address);
            }
            case GET_LONGRUNNING_FREE: {
                return new LongRunningFreeCommand(address);
            }
            case GET_SHORTRUNNING_FREE: {
                return new ShortRunningFreeCommand(address);
            }
            case PING: {
                return new PingCommand();
            }
            case SCHEDULE_WORK: {
                return new ScheduleWorkCommand(address, (DistributableWork) parameters[2]);
            }
            case START_WORK: {
                return new StartWorkCommand(address, (DistributableWork) parameters[2]);
            }
            case UPDATE_LONGRUNNING_FREE: {
                return new UpdateLongRunningFreeCommand(address, (Long) parameters[1]);
            }
            case UPDATE_SHORTRUNNING_FREE: {
                return new UpdateShortRunningFreeCommand(address, (Long) parameters[1]);
            }
            case WORKMANAGER_ADD: {
                return new AddWorkManagerCommand(address, (GroupMember) parameters[1]);
            }
            case WORKMANAGER_REMOVE: {
                return new RemoveWorkManagerCommand(address);
            }
            default: {
                throw new IllegalStateException(request.name());
            }
        }
    }

    @Override
    public void updated(GroupMembershipEvent<GroupMember> event) {
        Runnable task = () -> {
            // Handle abrupt leavers
            for (GroupMember leaver : event.getLeavers()) {
                this.leave(leaver);
            }
        };
        this.executor.execute(task);
    }

    @Override
    public void merged(GroupMembershipMergeEvent<GroupMember> event) {
        Runnable task = () -> this.join(event.getCurrentMembership());
        this.executor.execute(task);
    }

    public void join() {
        this.join(this.dispatcherFactory.getGroup().getMembership());
    }

    private void join(GroupMembership<GroupMember> membership) {
        Map<GroupMember, CompletionStage<Set<Address>>> futures = new HashMap<>();
        for (GroupMember member : membership.getMembers()) {
            if (!this.getOwnAddress().equals(member) && !this.nodes.containsValue(member)) {
                try {
                    futures.put(member, this.dispatcher.dispatchToMember(new GetWorkManagersCommand(), member));
                } catch (IOException e) {
                    ConnectorLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
        }
        for (Map.Entry<GroupMember, CompletionStage<Set<Address>>> entry : futures.entrySet()) {
            GroupMember member = entry.getKey();
            try {
                Set<Address> addresses = entry.getValue().toCompletableFuture().join();
                for (Address address : addresses) {
                    this.join(address, member);

                    this.localUpdateLongRunningFree(address, this.getShortRunningFree(address));
                    this.localUpdateShortRunningFree(address, this.getShortRunningFree(address));
                }
            } catch (CancellationException e) {
                // Ignore
            } catch (CompletionException e) {
                ConnectorLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            }
        }
    }
}
