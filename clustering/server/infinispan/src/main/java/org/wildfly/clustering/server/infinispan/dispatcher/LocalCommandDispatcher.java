/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.dispatcher;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

    public LocalCommandDispatcher(Node node, C context) {
        this.node = node;
        this.context = context;
    }

    @Override
    public C getContext() {
        return this.context;
    }

    @Override
    public <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException {
        if (!this.node.equals(member)) {
            throw new IllegalArgumentException(member.getName());
        }
        try {
            R result = command.execute(this.context);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            CompletableFuture<R> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
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
}
