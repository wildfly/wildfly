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
package org.wildfly.clustering.dispatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

import org.wildfly.clustering.group.Node;

/**
 * Dispatches commands for execution on a group.
 *
 * @param <C> the command context type
 * @author Paul Ferraro
 */
public interface CommandDispatcher<C> extends AutoCloseable {

    /**
     * Returns the context with which this dispatcher was created.
     * @return a command execution context
     */
    C getContext();

    /**
     * Executes the specified command on the specified group member.
     * If the member has no corresponding dispatcher, the returned completion stage throws a {@link java.util.concurrent.CancellationException}.
     *
     * @param <R> the command execution return type
     * @param command the command to execute
     * @param member the group member on which to execute the command
     * @return the future result of the command execution
     * @throws CommandDispatcherException if the command could not be sent
     */
    <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException;

    /**
     * Executes the specified command on all members of the group, optionally excluding some members.
     * If a given member has no corresponding dispatcher, its completion stage throws a {@link java.util.concurrent.CancellationException}.
     *
     * @param <R> the command execution return type
     * @param command the command to execute
     * @param excludedMembers the members to be excluded from group command execution
     * @return a completion stage per member of the group on which the command was executed
     * @throws CommandDispatcherException if the command could not be sent
     */
    <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super C> command, Node... excludedMembers) throws CommandDispatcherException;

    /**
     * Execute the specified command on the specified node.
     *
     * @param <R>     the return value type
     * @param command the command to execute
     * @param node    the node to execute the command on
     * @return the result of the command execution
     * @throws CommandDispatcherException if the command could not be sent
     * @deprecated Replaced by {@link #executeOnMember(Command, Node)}.
     */
    @Deprecated default <R> CommandResponse<R> executeOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException {
        try {
            return CommandResponse.of(this.executeOnMember(command, node).toCompletableFuture().join());
        } catch (CompletionException e) {
            return CommandResponse.of(e);
        }
    }

    /**
     * Execute the specified command on all nodes in the group, excluding the specified nodes
     *
     * @param <R>           the return value type
     * @param command       the command to execute
     * @param excludedNodes the set of nodes to exclude
     * @return a map of command execution results per node
     * @throws CommandDispatcherException if the command could not be broadcast
     * @deprecated Replaced by {@link #executeOnGroup(Command, Node...)}.
     */
    @Deprecated default <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException {
        Map<Node, CommandResponse<R>> result = new HashMap<>();
        for (Map.Entry<Node, CompletionStage<R>> entry : this.executeOnGroup(command, excludedNodes).entrySet()) {
            try {
                result.put(entry.getKey(), CommandResponse.of(entry.getValue().toCompletableFuture().join()));
            } catch (CancellationException e) {
                // Prune
            } catch (CompletionException e) {
                result.put(entry.getKey(), CommandResponse.of(e));
            }
        }
        return result;
    }

    /**
     * Submits the specified command on the specified node for execution.
     *
     * @param <R>     the return value type
     * @param command the command to execute
     * @param node    the node to execute the command on
     * @return the result of the command execution
     * @throws CommandDispatcherException if the command could not be sent
     * @deprecated Replaced by {@link #executeOnMember(Command, Node)}.
     */
    @Deprecated default <R> Future<R> submitOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException {
        return this.executeOnMember(command, node).toCompletableFuture();
    }

    /**
     * Submits the specified command on all nodes in the group, excluding the specified nodes.
     *
     * @param <R>           the return value type
     * @param command       the command to execute
     * @param excludedNodes the set of nodes to exclude
     * @return a map of command execution results per node.
     * @throws CommandDispatcherException if the command could not be broadcast
     * @deprecated Replaced by {@link #executeOnGroup(Command, Node...)}.
     */
    @SuppressWarnings("unchecked")
    @Deprecated default <R> Map<Node, Future<R>> submitOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException {
        return (Map<Node, Future<R>>) (Map<?, ?>) this.executeOnGroup(command, excludedNodes);
    }

    /**
     * Closes any resources used by this dispatcher.
     * Once closed, a dispatcher can no longer execute commands.
     */
    @Override
    void close();
}
