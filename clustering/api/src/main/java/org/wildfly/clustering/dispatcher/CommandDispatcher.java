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
import java.util.concurrent.CompletionStage;

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
     * Closes any resources used by this dispatcher.
     * Once closed, a dispatcher can no longer execute commands.
     */
    @Override
    void close();
}
