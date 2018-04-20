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

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import org.wildfly.clustering.dispatcher.CommandDispatcher;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.logging.ClusteringServerLogger;

/**
 * A {@link CommandDispatcherFactory} decorator that allows multiple invocations of {@link #createCommandDispatcher(Object, Object)} for a given identifier,
 * returning the same managed {@link CommandDispatcher} instance to each caller.
 * The managed {@link CommandDispatcher} instance is only closed after {@link CommandDispatcher#close()} is invoked on all instances.
 * @author Paul Ferraro
 */
public class ManagedCommandDispatcherFactory implements AutoCloseableCommandDispatcherFactory {

    private final AutoCloseableCommandDispatcherFactory factory;
    private final Map<Object, Map.Entry<CommandDispatcher<?>, Integer>> dispatchers = new HashMap<>();

    public ManagedCommandDispatcherFactory(AutoCloseableCommandDispatcherFactory factory) {
        this.factory = factory;
    }

    @Override
    public void close() {
        this.factory.close();
    }

    @Override
    public Group getGroup() {
        return this.factory.getGroup();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C> CommandDispatcher<C> createCommandDispatcher(Object id, C context) {
        synchronized (this.dispatchers) {
            Map.Entry<CommandDispatcher<?>, Integer> existingEntry = this.dispatchers.get(id);
            if (existingEntry == null) {
                CommandDispatcher<C> dispatcher = this.factory.createCommandDispatcher(id, context);
                CommandDispatcher<C> result = new ManagedCommandDispatcher<>(dispatcher, () -> {
                    synchronized (this.dispatchers) {
                        Map.Entry<CommandDispatcher<?>, Integer> entry = this.dispatchers.get(id);
                        synchronized (entry) {
                            // Decrement reference count
                            int refs = entry.getValue() - 1;
                            // Close when the reference count reaches zero
                            if (refs == 0) {
                                dispatcher.close();
                                this.dispatchers.remove(id);
                            } else {
                                entry.setValue(refs);
                            }
                        }
                    }
                });
                this.dispatchers.put(id, new AbstractMap.SimpleEntry<>(result, 1));
                return result;
            }
            CommandDispatcher<C> result = (CommandDispatcher<C>) existingEntry.getKey();
            // Ensure contexts are the same!
            if (result.getContext() != context) {
                throw ClusteringServerLogger.ROOT_LOGGER.commandDispatcherContextMismatch(id);
            }
            synchronized (existingEntry) {
                // Increment reference count
                int refs = existingEntry.getValue() + 1;
                existingEntry.setValue(refs);
            }
            return result;
        }
    }
}
