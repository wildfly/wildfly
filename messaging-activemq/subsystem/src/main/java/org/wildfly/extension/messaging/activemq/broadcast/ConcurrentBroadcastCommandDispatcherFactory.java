/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.broadcast;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.wildfly.clustering.function.Consumer;
import org.wildfly.clustering.server.Group;
import org.wildfly.clustering.server.GroupMember;
import org.wildfly.clustering.server.Registration;
import org.wildfly.clustering.server.cache.Cache;
import org.wildfly.clustering.server.cache.CacheStrategy;
import org.wildfly.clustering.server.dispatcher.Command;
import org.wildfly.clustering.server.dispatcher.CommandDispatcher;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactory;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link BroadcastCommandDispatcherFactory} that returns the same {@link CommandDispatcher} instance for a given identifier.
 * @author Paul Ferraro
 */
public class ConcurrentBroadcastCommandDispatcherFactory implements BroadcastCommandDispatcherFactory {

    private final Set<BroadcastReceiver> receivers = ConcurrentHashMap.newKeySet();
    private final CommandDispatcherFactory<GroupMember> dispatcherFactory;
    private final Cache<Object, CachedCommandDispatcher<?>> cache = CacheStrategy.CONCURRENT.createCache(Consumer.of(), Consumer.close().compose(CachedCommandDispatcher::get));

    public ConcurrentBroadcastCommandDispatcherFactory(CommandDispatcherFactory<GroupMember> dispatcherFactory) {
        this.dispatcherFactory = dispatcherFactory;
    }

    @Override
    public void receive(byte[] data) {
        for (BroadcastReceiver receiver : this.receivers) {
            receiver.receive(data);
        }
    }

    @Override
    public Registration register(BroadcastReceiver receiver) {
        this.receivers.add(receiver);
        return () -> this.receivers.remove(receiver);
    }

    @Override
    public Group<GroupMember> getGroup() {
        return this.dispatcherFactory.getGroup();
    }

    @Override
    public <C> CommandDispatcher<GroupMember, C> createCommandDispatcher(Object id, C context) {
        return this.createCommandDispatcher(id, context, WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> CommandDispatcher<GroupMember, C> createCommandDispatcher(Object id, C context, ClassLoader loader) {
        CommandDispatcherFactory<GroupMember> dispatcherFactory = this.dispatcherFactory;
        BiFunction<Object, Runnable, CachedCommandDispatcher<?>> factory = new BiFunction<>() {
            @Override
            public CachedCommandDispatcher<?> apply(Object id, Runnable closeTask) {
                return new CachedCommandDispatcher<>(dispatcherFactory.createCommandDispatcher(id, context, loader), closeTask);
            }
        };
        return (CommandDispatcher<GroupMember, C>) this.cache.computeIfAbsent(id, factory);
    }

    private static class CachedCommandDispatcher<C> implements CommandDispatcher<GroupMember, C>, Supplier<CommandDispatcher<GroupMember, C>> {

        private final CommandDispatcher<GroupMember, C> dispatcher;
        private final Runnable closeTask;

        CachedCommandDispatcher(CommandDispatcher<GroupMember, C> dispatcher, Runnable closeTask) {
            this.dispatcher = dispatcher;
            this.closeTask = closeTask;
        }

        @Override
        public CommandDispatcher<GroupMember, C> get() {
            return this.dispatcher;
        }

        @Override
        public C getContext() {
            return this.dispatcher.getContext();
        }

        @Override
        public <R, E extends Exception> CompletionStage<R> dispatchToMember(Command<R, ? super C, E> command, GroupMember member) throws IOException {
            return this.dispatcher.dispatchToMember(command, member);
        }

        @Override
        public <R, E extends Exception> Map<GroupMember, CompletionStage<R>> dispatchToGroup(Command<R, ? super C, E> command, Set<GroupMember> excluding) throws IOException {
            return this.dispatcher.dispatchToGroup(command, excluding);
        }

        @Override
        public void close() {
            this.closeTask.run();
        }
    }
}
