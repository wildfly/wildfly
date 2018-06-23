/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.group.Node;

/**
 * Non-clustered {@link CommandDispatcher} implementation
 * @author Paul Ferraro
 * @param <C> command context
 */
public class LocalCommandDispatcher<C> implements CommandDispatcher<C> {

    private final C context;
    private final Node node;
    private final Executor executor;

    public LocalCommandDispatcher(Node node, C context, Executor executor) {
        this.node = node;
        this.context = context;
        this.executor = executor;
    }

    @Override
    public C getContext() {
        return this.context;
    }

    @Override
    public <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException {
        if (!this.node.equals(this.node)) {
            throw new IllegalArgumentException(member.getName());
        }
        return CompletableFuture.supplyAsync(new CommandTask<>(command, this.context), this.executor);
    }

    @Override
    public <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super C> command, Node... excludedMembers) throws CommandDispatcherException {
        if ((excludedMembers != null) && Arrays.asList(excludedMembers).contains(this.node)) return Collections.emptyMap();
        return Collections.singletonMap(this.node, this.executeOnMember(command, this.node));
    }

    @Override
    public void close() {
        // Do nothing
    }

    private static class CommandTask<R, C> implements Supplier<R> {
        private final C context;
        private final Command<R, C> command;

        CommandTask(Command<R, C> command, C context) {
            this.command = command;
            this.context = context;
        }

        @Override
        public R get() {
            try {
                return this.command.execute(this.context);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }
    }
}
