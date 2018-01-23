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

package org.wildfly.clustering.server.dispatcher;

import java.util.Map;
import java.util.concurrent.Future;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;

/**
 * Decorates a command dispatcher with custom {@link #close()} logic.
 * @author Paul Ferraro
 */
public class ManagedCommandDispatcher<C> implements CommandDispatcher<C> {
    private final CommandDispatcher<C> dispatcher;
    private final Runnable closeTask;

    ManagedCommandDispatcher(CommandDispatcher<C> dispatcher, Runnable closeTask) {
        this.dispatcher = dispatcher;
        this.closeTask = closeTask;
    }

    @Override
    public C getContext() {
        return this.dispatcher.getContext();
    }

    @Override
    public <R> CommandResponse<R> executeOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException {
        return this.dispatcher.executeOnNode(command, node);
    }

    @Override
    public <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException {
        return this.dispatcher.executeOnCluster(command, excludedNodes);
    }

    @Override
    public <R> Future<R> submitOnNode(Command<R, ? super C> command, Node node) throws CommandDispatcherException {
        return this.dispatcher.submitOnNode(command, node);
    }

    @Override
    public <R> Map<Node, Future<R>> submitOnCluster(Command<R, ? super C> command, Node... excludedNodes) throws CommandDispatcherException {
        return this.dispatcher.submitOnCluster(command, excludedNodes);
    }

    @Override
    public void close() {
        this.closeTask.run();
    }
}