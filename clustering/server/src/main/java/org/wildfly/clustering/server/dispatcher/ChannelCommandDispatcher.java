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
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RspFilter;
import org.jgroups.util.Buffer;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
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
    private final Duration timeout;
    private final CommandDispatcher<C> localDispatcher;
    private final Runnable closeTask;
    private final Address localAddress;
    private final RequestOptions options;

    public ChannelCommandDispatcher(MessageDispatcher dispatcher, CommandMarshaller<C> marshaller, Group<Address> group, Duration timeout, CommandDispatcher<C> localDispatcher, Runnable closeTask) {
        this.dispatcher = dispatcher;
        this.marshaller = marshaller;
        this.group = group;
        this.timeout = timeout;
        this.localDispatcher = localDispatcher;
        this.closeTask = closeTask;
        this.localAddress = dispatcher.getChannel().getAddress();
        this.options = new RequestOptions(ResponseMode.GET_ALL, this.timeout.toMillis(), false, FILTER, Message.Flag.DONT_BUNDLE, Message.Flag.OOB);
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
    public <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException {
        // Bypass MessageDispatcher if target node is local
        Address address = this.group.getAddress(member);
        if (this.localAddress.equals(address)) {
            return this.localDispatcher.executeOnMember(command, member);
        }
        Buffer buffer = this.createBuffer(command);
        ServiceRequest<R> request = new ServiceRequest<>(this.dispatcher.getCorrelator(), this.group.getAddress(member), this.options);
        return request.send(buffer);
    }

    @Override
    public <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super C> command, Node... excludedMembers) throws CommandDispatcherException {
        Set<Node> excluded = (excludedMembers != null) ? new HashSet<>(Arrays.asList(excludedMembers)) : Collections.emptySet();
        Map<Node, CompletionStage<R>> results = new ConcurrentHashMap<>();
        Buffer buffer = this.createBuffer(command);
        for (Node member : this.group.getMembership().getMembers()) {
            if (!excluded.contains(member)) {
                Address address = this.group.getAddress(member);
                if (this.localAddress.equals(address)) {
                    results.put(member, this.localDispatcher.executeOnMember(command, member));
                } else {
                    try {
                        ServiceRequest<R> request = new ServiceRequest<>(this.dispatcher.getCorrelator(), this.group.getAddress(member), this.options);
                        CompletionStage<R> future = request.send(buffer);
                        results.put(member, future);
                        future.whenComplete(new PruneCancellationTask<>(results, member));
                    } catch (CommandDispatcherException e) {
                        // Cancel previously dispatched messages
                        for (CompletionStage<R> result : results.values()) {
                            result.toCompletableFuture().cancel(true);
                        }
                        throw e;
                    }
                }
            }
        }
        return results;
    }

    private <R> Buffer createBuffer(Command<R, ? super C> command) {
        try {
            return new Buffer(this.marshaller.marshal(command));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static class PruneCancellationTask<T> implements BiConsumer<T, Throwable> {
        private final Map<Node, CompletionStage<T>> results;
        private final Node member;

        PruneCancellationTask(Map<Node, CompletionStage<T>> results, Node member) {
            this.results = results;
            this.member = member;
        }

        @Override
        public void accept(T result, Throwable exception) {
            if (exception instanceof CancellationException) {
                this.results.remove(this.member);
            }
        }
    }
}
