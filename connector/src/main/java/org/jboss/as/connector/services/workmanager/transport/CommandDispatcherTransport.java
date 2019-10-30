/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.services.workmanager.transport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.resource.spi.work.DistributableWork;
import javax.resource.spi.work.WorkException;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.jca.core.api.workmanager.DistributedWorkManager;
import org.jboss.jca.core.spi.workmanager.Address;
import org.jboss.jca.core.workmanager.transport.remote.AbstractRemoteTransport;
import org.jboss.jca.core.workmanager.transport.remote.ProtocolMessages.Request;
import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.GroupListener;
import org.wildfly.clustering.group.Membership;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.service.concurrent.ServiceExecutor;
import org.wildfly.clustering.service.concurrent.StampedLockServiceExecutor;
import org.wildfly.common.function.ExceptionRunnable;
import org.wildfly.common.function.ExceptionSupplier;

/**
 * {@link DistributedWorkManager}-specific transport based on a {@link CommandDispatcher}.
 * The current implementation is a direct translation of {@link org.jboss.jca.core.workmanager.transport.remote.jgroups.JGroupsTransport}.
 * @author Paul Ferraro
 */
public class CommandDispatcherTransport extends AbstractRemoteTransport<Node> implements GroupListener {

    private final ServiceExecutor executor = new StampedLockServiceExecutor();
    private final CommandDispatcherFactory dispatcherFactory;
    private final String name;

    private volatile CommandDispatcher<CommandDispatcherTransport> dispatcher;
    private volatile Registration groupListenerRegistration;
    private volatile boolean initialized = false;

    public CommandDispatcherTransport(CommandDispatcherFactory dispatcherFactory, String name) {
        this.dispatcherFactory = dispatcherFactory;
        this.name = name;
    }

    @Override
    public String getId() {
        return this.getOwnAddress().getName();
    }

    @Override
    public void startup() throws Exception {
        this.dispatcher = this.dispatcherFactory.createCommandDispatcher(this.name, this);
        this.groupListenerRegistration = this.dispatcherFactory.getGroup().register(this);
        this.broadcast(new JoinCommand());
    }

    @Override
    public void shutdown() {
        this.executor.close(() -> {
            try {
                this.broadcast(new LeaveCommand(this.getOwnAddress()));
            } catch (WorkException e) {
                ConnectorLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
            } finally {
                this.groupListenerRegistration.close();
                this.dispatcher.close();
            }
        });
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
    protected Node getOwnAddress() {
        return this.dispatcherFactory.getGroup().getLocalMember();
    }

    @Override
    protected Serializable sendMessage(Node physicalAddress, Request request, Serializable... parameters) throws WorkException {
        Command<?, CommandDispatcherTransport> command = createCommand(request, parameters);
        CommandDispatcher<CommandDispatcherTransport> dispatcher = this.dispatcher;
        ExceptionSupplier<Optional<Serializable>, WorkException> task = new ExceptionSupplier<Optional<Serializable>, WorkException>() {
            @Override
            public Optional<Serializable> get() throws WorkException {
                try {
                    CompletionStage<?> response = dispatcher.executeOnMember(command, physicalAddress);
                    return Optional.ofNullable((Serializable) response.toCompletableFuture().join());
                } catch (CancellationException e) {
                    return Optional.empty();
                } catch (CommandDispatcherException | CompletionException e) {
                    throw new WorkException(e);
                }
            }
        };
        return this.executor.execute(task).orElse(null).orElse(null);
    }

    private void broadcast(Command<Void, CommandDispatcherTransport> command) throws WorkException {
        CommandDispatcher<CommandDispatcherTransport> dispatcher = this.dispatcher;
        ExceptionRunnable<WorkException> task = new ExceptionRunnable<WorkException>() {
            @Override
            public void run() throws WorkException {
                try {
                    for (Map.Entry<Node, CompletionStage<Void>> entry : dispatcher.executeOnGroup(command).entrySet()) {
                        // Verify that command executed successfully on all nodes
                        try {
                            entry.getValue().toCompletableFuture().join();
                        } catch (CancellationException e) {
                            // Ignore
                        } catch (CompletionException e) {
                            throw new WorkException(e);
                        }
                    }
                } catch (CommandDispatcherException e) {
                    throw new WorkException(e);
                }
            }
        };
        this.executor.execute(task);
    }

    private static Command<?, CommandDispatcherTransport> createCommand(Request request, Serializable... parameters) {
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
                return new AddWorkManagerCommand(address, (Node) parameters[1]);
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
    public void membershipChanged(Membership previousMembership, Membership membership, boolean merged) {
        Runnable task = () -> {
            Set<Node> leavers = new HashSet<>(previousMembership.getMembers());
            leavers.removeAll(membership.getMembers());
            // Handle abrupt leavers
            for (Node leaver : leavers) {
                this.leave(leaver);
            }

            if (merged) {
                this.join(membership);
            }
        };
        this.executor.execute(task);
    }

    public void join() {
        this.join(this.dispatcherFactory.getGroup().getMembership());
    }

    private void join(Membership membership) {
        Map<Node, CompletionStage<Set<Address>>> futures = new HashMap<>();
        for (Node member : membership.getMembers()) {
            if (!this.getOwnAddress().equals(member) && !this.nodes.containsValue(member)) {
                try {
                    futures.put(member, this.dispatcher.executeOnMember(new GetWorkManagersCommand(), member));
                } catch (CommandDispatcherException e) {
                    ConnectorLogger.ROOT_LOGGER.warn(e.getLocalizedMessage(), e);
                }
            }
        }
        for (Map.Entry<Node, CompletionStage<Set<Address>>> entry : futures.entrySet()) {
            Node member = entry.getKey();
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
