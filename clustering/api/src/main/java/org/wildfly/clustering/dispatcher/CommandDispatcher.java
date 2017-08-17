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

import java.util.Map;
import java.util.concurrent.Future;

import org.wildfly.clustering.group.Node;

/**
 * A dispatcher for remote invocation of commands.
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
     * Execute the specified command on the specified node.
     *
     * @param <R>     the return value type
     * @param command the command to execute
     * @param node    the node to execute the command on
     * @return the result of the command execution
     * @throws CommandDispatcherException if the command could not be sent
     */
    <R> CommandResponse<R> executeOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException;

    /**
     * Execute the specified command on all nodes in the group, excluding the specified nodes
     *
     * @param <R>           the return value type
     * @param command       the command to execute
     * @param excludedNodes the set of nodes to exclude
     * @return a map of command execution results per node
     * @throws CommandDispatcherException if the command could not be broadcast
     */
    <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException;

    /**
     * Submits the specified command on the specified node for execution.
     *
     * @param <R>     the return value type
     * @param command the command to execute
     * @param node    the node to execute the command on
     * @return the result of the command execution
     * @throws CommandDispatcherException if the command could not be sent
     */
    <R> Future<R> submitOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException;

    /**
     * Submits the specified command on all nodes in the group, excluding the specified nodes.
     *
     * @param <R>           the return value type
     * @param command       the command to execute
     * @param excludedNodes the set of nodes to exclude
     * @return a map of command execution results per node.
     * @throws CommandDispatcherException if the command could not be broadcast
     */
    <R> Map<Node, Future<R>> submitOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException;

    /**
     * Closes any resources used by this dispatcher.
     * Once closed, a dispatcher can no longer execute commands.
     */
    @Override
    void close();
}
