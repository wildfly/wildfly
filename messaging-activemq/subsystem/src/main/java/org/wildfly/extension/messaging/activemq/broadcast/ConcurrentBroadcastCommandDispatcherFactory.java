/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.broadcast;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.wildfly.clustering.Registration;
import org.wildfly.clustering.dispatcher.Command;
import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherException;
import org.wildfly.clustering.ee.Manager;
import org.wildfly.clustering.ee.cache.ConcurrentManager;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.spi.dispatcher.CommandDispatcherFactory;
import org.wildfly.common.function.Functions;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A {@link BroadcastCommandDispatcherFactory} that returns the same {@link CommandDispatcher} instance for a given identifier.
 * @author Paul Ferraro
 */
public class ConcurrentBroadcastCommandDispatcherFactory implements BroadcastCommandDispatcherFactory {

    private final Set<BroadcastReceiver> receivers = ConcurrentHashMap.newKeySet();
    private final Manager<Object, CommandDispatcher<?>> dispatchers = new ConcurrentManager<>(Functions.discardingConsumer(), new Consumer<CommandDispatcher<?>>() {
        @Override
        public void accept(CommandDispatcher<?> dispatcher) {
            ((ConcurrentCommandDispatcher<?>) dispatcher).closeDispatcher();
        }
    });
    private final CommandDispatcherFactory factory;

    public ConcurrentBroadcastCommandDispatcherFactory(CommandDispatcherFactory factory) {
        this.factory = factory;
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
    public Group getGroup() {
        return this.factory.getGroup();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        CommandDispatcherFactory dispatcherFactory = this.factory;
        Function<Runnable, CommandDispatcher<?>> factory = new Function<Runnable, CommandDispatcher<?>>() {
            @Override
            public CommandDispatcher<C> apply(Runnable closeTask) {
                CommandDispatcher<C> dispatcher = dispatcherFactory.createCommandDispatcher(id, context, WildFlySecurityManager.getClassLoaderPrivileged(this.getClass()));
                return new ConcurrentCommandDispatcher<>(dispatcher, closeTask);
            }
        };
        return (CommandDispatcher<C>) this.dispatchers.apply(id, factory);
    }

    private static class ConcurrentCommandDispatcher<C> implements CommandDispatcher<C> {

        private final CommandDispatcher<C> dispatcher;
        private final Runnable closeTask;

        ConcurrentCommandDispatcher(CommandDispatcher<C> dispatcher, Runnable closeTask) {
            this.dispatcher = dispatcher;
            this.closeTask = closeTask;
        }

        void closeDispatcher() {
            this.dispatcher.close();
        }

        @Override
        public C getContext() {
            return this.dispatcher.getContext();
        }

        @Override
        public <R> CompletionStage<R> executeOnMember(Command<R, ? super C> command, Node member) throws CommandDispatcherException {
            return this.dispatcher.executeOnMember(command, member);
        }

        @Override
        public <R> Map<Node, CompletionStage<R>> executeOnGroup(Command<R, ? super C> command, Node... excludedMembers) throws CommandDispatcherException {
            return this.dispatcher.executeOnGroup(command, excludedMembers);
        }

        @Override
        public void close() {
            this.closeTask.run();
        }
    }
}
