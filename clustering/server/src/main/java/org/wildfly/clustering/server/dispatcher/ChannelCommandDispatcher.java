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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.clustering.jgroups.Addressable;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RspFilter;
import org.jgroups.util.Rsp;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;

/**
 * MessageDispatcher-based command dispatcher.
 * @author Paul Ferraro
 *
 * @param <C> command execution context
 */
public abstract class ChannelCommandDispatcher<C> implements CommandDispatcher<C> {

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
    private final NodeFactory<Address> factory;
    private final long timeout;
    private final CommandDispatcher<C> localDispatcher;

    public ChannelCommandDispatcher(MessageDispatcher dispatcher, CommandMarshaller<C> marshaller, NodeFactory<Address> factory, long timeout, CommandDispatcher<C> localDispatcher) {
        this.dispatcher = dispatcher;
        this.marshaller = marshaller;
        this.factory = factory;
        this.timeout = timeout;
        this.localDispatcher = localDispatcher;
    }

    @Override
    public <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, C> command, Node... excludedNodes) {
        try {
            RequestOptions options = this.createRequestOptions(excludedNodes);
            Map<Address, Rsp<R>> responses = this.dispatcher.castMessage(null, this.createMessage(command), options);
            if (responses == null) return Collections.emptyMap();

            Map<Node, CommandResponse<R>> results = new HashMap<>();
            for (Map.Entry<Address, Rsp<R>> entry: responses.entrySet()) {
                Address address = entry.getKey();
                Rsp<R> response = entry.getValue();
                if (response.wasReceived()) {
                    results.put(this.factory.createNode(address), createCommandResponse(response));
                }
            }

            return results;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public <R> Map<Node, Future<R>> submitOnCluster(Command<R, C> command, Node... excludedNodes) {
        try {
            final Future<? extends Map<Address, Rsp<R>>> responses = this.dispatcher.castMessageWithFuture(null, this.createMessage(command), this.createRequestOptions(excludedNodes));
            if (responses == null) return Collections.emptyMap();

            Map<Node, Future<R>> results = new HashMap<>();
            Set<Node> excluded = (excludedNodes != null) ? new HashSet<>(Arrays.asList(excludedNodes)) : Collections.<Node>emptySet();
            for (Address address: this.dispatcher.getChannel().getView().getMembers()) {
                final Node node = this.factory.createNode(address);
                if (!excluded.contains(node)) {
                    Future<R> future = new Future<R>() {
                        @Override
                        public boolean cancel(boolean mayInterruptIfRunning) {
                            return responses.cancel(mayInterruptIfRunning);
                        }

                        @Override
                        public R get() throws InterruptedException, ExecutionException {
                            return createCommandResponse(responses.get().get(node)).get();
                        }

                        @Override
                        public R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                            return createCommandResponse(responses.get(timeout, unit).get(node)).get();
                        }

                        @Override
                        public boolean isCancelled() {
                            return responses.isCancelled();
                        }

                        @Override
                        public boolean isDone() {
                            return responses.isDone();
                        }
                    };
                    results.put(node, future);
                }
            }
            return results;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Override
    public <R> CommandResponse<R> executeOnNode(Command<R, C> command, Node node) {
        // Bypass MessageDispatcher if target node is local
        if (this.isLocal(node)) {
            return this.localDispatcher.executeOnNode(command, node);
        }
        try {
            R result = this.dispatcher.sendMessage(this.createMessage(command, node), this.createRequestOptions());
            return new SimpleCommandResponse<>(result);
        } catch (Throwable e) {
            return new SimpleCommandResponse<>(e);
        }
    }

    @Override
    public <R> Future<R> submitOnNode(Command<R, C> command, Node node) {
        // Bypass MessageDispatcher if target node is local
        if (this.isLocal(node)) {
            return this.localDispatcher.submitOnNode(command, node);
        }
        try {
            return this.dispatcher.sendMessageWithFuture(this.createMessage(command, node), this.createRequestOptions());
        } catch (Throwable e) {
            return new SimpleFuture<>(e);
        }
    }

    private <R> Message createMessage(Command<R, C> command) {
        return this.createMessage(command, null);
    }

    private <R> Message createMessage(Command<R, C> command, Node node) {
        try {
            return new Message(getAddress(node), this.getLocalAddress(), this.marshaller.marshal(command));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private boolean isLocal(Node node) {
        return this.getLocalAddress().equals(getAddress(node));
    }

    private static Address getAddress(Node node) {
        return (node instanceof Addressable) ? ((Addressable) node).getAddress() : null;
    }

    private RequestOptions createRequestOptions(Node... excludedNodes) {
        RequestOptions options = this.createRequestOptions();
        if ((excludedNodes != null) && (excludedNodes.length > 0)) {
            Address[] addresses = new Address[excludedNodes.length];
            for (int i = 0; i < excludedNodes.length; ++i) {
                addresses[i] = getAddress(excludedNodes[i]);
            }
            options.setExclusionList(addresses);
        }
        return options;
    }

    private RequestOptions createRequestOptions() {
        return new RequestOptions(ResponseMode.GET_ALL, this.timeout, false, FILTER, Message.Flag.DONT_BUNDLE, Message.Flag.OOB);
    }

    static <R> CommandResponse<R> createCommandResponse(Rsp<R> response) {
        Throwable exception = response.getException();
        return (exception != null) ? new SimpleCommandResponse<R>(exception) : new SimpleCommandResponse<>(response.wasReceived() ? response.getValue() : null);
    }

    private Address getLocalAddress() {
        return this.dispatcher.getChannel().getAddress();
    }
}
