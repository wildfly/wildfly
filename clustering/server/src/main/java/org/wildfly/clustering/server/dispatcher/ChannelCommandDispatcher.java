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
package org.wildfly.clustering.server.dispatcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RspFilter;
import org.jgroups.util.Buffer;
import org.jgroups.util.Rsp;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.group.Group;

/**
 * MessageDispatcher-based command dispatcher.
 * @author Paul Ferraro
 *
 * @param <C> command execution context
 */
public class ChannelCommandDispatcher<C> implements CommandDispatcher<C> {

    private static final RspFilter FILTER = new RspFilter() {
        @Override
        public boolean isAcceptable(Object response, Address sender) {
            return !(response instanceof NoSuchService);
        }

        @Override
        public boolean needMoreResponses() {
            return true;
        }
    };

    private final MessageDispatcher dispatcher;
    private final CommandMarshaller<C> marshaller;
    private final Group<Address> group;
    private final long timeout;
    private final CommandDispatcher<C> localDispatcher;
    private final Runnable closeTask;

    public ChannelCommandDispatcher(MessageDispatcher dispatcher, CommandMarshaller<C> marshaller, Group<Address> group, long timeout, CommandDispatcher<C> localDispatcher, Runnable closeTask) {
        this.dispatcher = dispatcher;
        this.marshaller = marshaller;
        this.group = group;
        this.timeout = timeout;
        this.localDispatcher = localDispatcher;
        this.closeTask = closeTask;
    }

    @Override
    public C getContext() {
        return this.localDispatcher.getContext();
    }

    @Override
    public void close() {
        this.closeTask.run();
    }

    @Override
    public <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException {
        Buffer buffer = this.createBuffer(command);
        RequestOptions options = this.createRequestOptions(excludedNodes);
        try {
            Map<Address, Rsp<R>> responses = this.dispatcher.castMessage(null, buffer, options);

            Map<Node, CommandResponse<R>> results = new HashMap<>();
            for (Map.Entry<Address, Rsp<R>> entry: responses.entrySet()) {
                Address address = entry.getKey();
                Rsp<R> response = entry.getValue();
                if (response.wasReceived() && !response.wasSuspected()) {
                    results.put(this.group.createNode(address), createCommandResponse(response));
                }
            }

            return results;
        } catch (Exception e) {
            throw new CommandDispatcherException(e);
        }
    }

    @Override
    public <R> Map<Node, Future<R>> submitOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException {
        Set<Node> excluded = Stream.of(excludedNodes).collect(Collectors.toSet());
        Map<Node, Future<R>> results = new HashMap<>();
        Buffer buffer = this.createBuffer(command);
        RequestOptions options = this.createRequestOptions();
        for (Node node : this.group.getMembership().getMembers()) {
            if (!excluded.contains(node)) {
                try {
                    results.put(node, this.dispatcher.sendMessageWithFuture(this.group.getAddress(node), buffer, options));
                } catch (Exception e) {
                    throw new CommandDispatcherException(e);
                }
            }
        }
        return results;
    }

    @Override
    public <R> CommandResponse<R> executeOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException {
        // Bypass MessageDispatcher if target node is local
        if (this.isLocal(node)) {
            return this.localDispatcher.executeOnNode(command, node);
        }
        Buffer buffer = this.createBuffer(command);
        RequestOptions options = this.createRequestOptions();
        try {
            // Use sendMessageWithFuture(...) instead of sendMessage(...) since we want to differentiate between sender exceptions and receiver exceptions
            Future<R> future = this.dispatcher.sendMessageWithFuture(this.group.getAddress(node), buffer, options);
            return new SimpleCommandResponse<>(future.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SimpleCommandResponse<>(e);
        } catch (ExecutionException e) {
            return new SimpleCommandResponse<>(e);
        } catch (Exception e) {
            throw new CommandDispatcherException(e);
        }
    }

    @Override
    public <R> Future<R> submitOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException {
        // Bypass MessageDispatcher if target node is local
        if (this.isLocal(node)) {
            return this.localDispatcher.submitOnNode(command, node);
        }
        Buffer buffer = this.createBuffer(command);
        RequestOptions options = this.createRequestOptions();
        try {
            return this.dispatcher.sendMessageWithFuture(this.group.getAddress(node), buffer, options);
        } catch (Exception e) {
            throw new CommandDispatcherException(e);
        }
    }

    public <R> Future<R> submit(Node node, Buffer buffer, RequestOptions options) throws CommandDispatcherException {
        try {
            return this.dispatcher.sendMessageWithFuture(this.group.getAddress(node), buffer, options);
        } catch (Exception e) {
            throw new CommandDispatcherException(e);
        }
    }

    private <R> Buffer createBuffer(Command<R, ? super C> command) {
        try {
            return new Buffer(this.marshaller.marshal(command));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isLocal(Node node) {
        return this.getLocalAddress().equals(this.group.getAddress(node));
    }

    private RequestOptions createRequestOptions(Node... excludedNodes) {
        Address[] excludedAddresses = new Address[excludedNodes.length];
        for (int i = 0; i < excludedNodes.length; ++i) {
            excludedAddresses[i] = this.group.getAddress(excludedNodes[i]);
        }
        return this.createRequestOptions().exclusionList(excludedAddresses);
    }

    private RequestOptions createRequestOptions() {
        return new RequestOptions(ResponseMode.GET_ALL, this.timeout, false, FILTER, Message.Flag.DONT_BUNDLE, Message.Flag.OOB);
    }

    static <R> CommandResponse<R> createCommandResponse(Rsp<R> response) {
        Throwable exception = response.getException();
        return (exception != null) ? new SimpleCommandResponse<>(exception) : new SimpleCommandResponse<>(response.getValue());
    }

    private Address getLocalAddress() {
        return this.dispatcher.getChannel().getAddress();
    }
}
