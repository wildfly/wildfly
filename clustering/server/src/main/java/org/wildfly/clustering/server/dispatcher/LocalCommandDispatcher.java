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

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jboss.threads.JBossThreadFactory;
import org.jgroups.Address;
import org.jgroups.UnreachableException;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandResponse;
import org.wildfly.clustering.group.Node;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Non-clustered {@link CommandDispatcher} implementation
 * @author Paul Ferraro
 * @param <C> command context
 */
public class LocalCommandDispatcher<C> implements CommandDispatcher<C> {

    private final C context;
    private final Node node;
    private final ExecutorService executor;

    public LocalCommandDispatcher(Node node, C context) {
        this(node, context, Executors.newCachedThreadPool(createThreadFactory()));
    }

    private static ThreadFactory createThreadFactory() {
        PrivilegedAction<ThreadFactory> action = () -> new JBossThreadFactory(new ThreadGroup(LocalCommandDispatcher.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
        return WildFlySecurityManager.doUnchecked(action);
    }

    public LocalCommandDispatcher(Node node, C context, ExecutorService executor) {
        this.node = node;
        this.context = context;
        this.executor = executor;
    }

    @Override
    public C getContext() {
        return this.context;
    }

    @Override
    public <R> CommandResponse<R> executeOnNode(Command<R, ? super C> command, Node node) {
        if (!this.node.equals(node)) {
            throw new UnreachableException((Address) null);
        }
        try {
            return new SimpleCommandResponse<>(command.execute(this.context));
        } catch (Throwable e) {
            return new SimpleCommandResponse<>(e);
        }
    }

    @Override
    public <R> Map<Node, CommandResponse<R>> executeOnCluster(Command<R, ? super C> command, Node... excludedNodes) {
        Map<Node, CommandResponse<R>> results = new HashMap<>();
        if ((excludedNodes == null) || (excludedNodes.length == 0) || !Arrays.asList(excludedNodes).contains(this.node)) {
            results.put(this.node, this.executeOnNode(command, this.node));
        }
        return results;
    }

    @Override
    public <R> Future<R> submitOnNode(final Command<R, ? super C> command, Node node) {
        Callable<R> task = () -> command.execute(this.context);
        return this.executor.submit(task);
    }

    @Override
    public <R> Map<Node, Future<R>> submitOnCluster(Command<R, ? super C> command, Node... excludedNodes) {
        Map<Node, Future<R>> results = new HashMap<>();
        if ((excludedNodes == null) || (excludedNodes.length == 0) || !Arrays.asList(excludedNodes).contains(this.node)) {
            results.put(this.node, this.submitOnNode(command, this.node));
        }
        return results;
    }

    @Override
    public void close() {
        PrivilegedAction<Void> action = () -> {
            this.executor.shutdown();
            return null;
        };
        WildFlySecurityManager.doUnchecked(action);
    }
}
