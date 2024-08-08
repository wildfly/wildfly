/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.server.group.GroupMember;
import org.wildfly.extension.clustering.server.group.legacy.LegacyGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCommandDispatcherFactory<A extends Comparable<A>, M extends GroupMember<A>> extends org.wildfly.clustering.dispatcher.CommandDispatcherFactory {

    CommandDispatcherFactory<M> unwrap();

    @Override
    LegacyGroup<A, M> getGroup();

    default <C> org.wildfly.clustering.dispatcher.CommandDispatcher<C> wrap(CommandDispatcher<M, C> dispatcher) {
        return new org.wildfly.clustering.dispatcher.CommandDispatcher<>() {
            @Override
            public C getContext() {
                return dispatcher.getContext();
            }

            @Override
            public <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException {
                try {
                    return dispatcher.dispatchToMember(command, LegacyCommandDispatcherFactory.this.getGroup().unwrap(member));
                } catch (IOException e) {
                    throw new CommandDispatcherException(e);
                }
            }

            @Override
            public <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super C> command, Node... excludedMembers) throws CommandDispatcherException {
                try {
                    Map<M, CompletionStage<R>> results = dispatcher.dispatchToGroup(command, Stream.of(excludedMembers).<M>map(LegacyCommandDispatcherFactory.this.getGroup()::unwrap).collect(Collectors.toSet()));
                    return results.entrySet().stream().collect(Collectors.toMap(entry -> LegacyCommandDispatcherFactory.this.getGroup().wrap(entry.getKey()), Map.Entry::getValue));
                } catch (IOException e) {
                    throw new CommandDispatcherException(e);
                }
            }

            @Override
            public void close() {
                dispatcher.close();
            }
        };
    }

    @Override
    default <C> org.wildfly.clustering.dispatcher.CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        return this.wrap(this.unwrap().createCommandDispatcher(id, context));
    }

    @Override
    default <C> org.wildfly.clustering.dispatcher.CommandDispatcher<C> createCommandDispatcher(Object id, C context, ClassLoader loader) {
        return this.wrap(this.unwrap().createCommandDispatcher(id, context, loader));
    }
}
