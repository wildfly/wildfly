/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.registry;

import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathElement;

final class ConcreteOperationRegistry extends OperationRegistry {
    @SuppressWarnings( { "unused" })
    private volatile Map<String, OperationSubregistry> children;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, OperationHandler> handlers;

    private static final AtomicMapFieldUpdater<ConcreteOperationRegistry, String, OperationSubregistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteOperationRegistry.class, Map.class, "children"));
    private static final AtomicMapFieldUpdater<ConcreteOperationRegistry, String, OperationHandler> handlersUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteOperationRegistry.class, Map.class, "handlers"));

    ConcreteOperationRegistry(final String valueString, final OperationSubregistry parent) {
        super(valueString, parent);
        childrenUpdater.clear(this);
        handlersUpdater.clear(this);
    }

    OperationHandler getHandler(ListIterator<PathElement> iterator, String operationName) {
        if (! iterator.hasNext()) {
            return handlersUpdater.get(this, operationName);
        }
        final PathElement next = iterator.next();
        try {
            final String key = next.getKey();
            final Map<String, OperationSubregistry> snapshot = childrenUpdater.get(this);
            final OperationSubregistry subregistry = snapshot.get(key);
            return subregistry == null ? null : subregistry.getHandler(iterator, next.getValue(), operationName);
        } finally {
            iterator.previous();
        }
    }

    Map<String, OperationHandler> getHandlers(ListIterator<PathElement> iterator) {
        if (! iterator.hasNext()) {
            return handlersUpdater.getReadOnly(this);
        }
        final PathElement next = iterator.next();
        try {
            final String key = next.getKey();
            final Map<String, OperationSubregistry> snapshot = childrenUpdater.get(this);
            final OperationSubregistry subregistry = snapshot.get(key);
            return subregistry == null ? Collections.<String, OperationHandler>emptyMap() : subregistry.getHandlers(iterator, next.getValue());
        } finally {
            iterator.previous();
        }
    }

    void register(final ListIterator<PathElement> iterator, final String operationName, final OperationHandler handler) {
        if (! iterator.hasNext()) {
            if (handlersUpdater.putIfAbsent(this, operationName, handler) != null) {
                throw new IllegalArgumentException("A handler named '" + operationName + "' is already registered at location '" + getLocationString() + "'");
            }
        }
        final PathElement element = iterator.next();
        getSubregistry(element.getKey()).register(element.getValue(), iterator, operationName, handler);
    }

    OperationSubregistry getSubregistry(String key) {
        for (;;) {
            final Map<String, OperationSubregistry> snapshot = childrenUpdater.get(this);
            final OperationSubregistry subregistry = snapshot.get(key);
            if (subregistry != null) {
                return subregistry;
            } else {
                final OperationSubregistry newRegistry = new OperationSubregistry(key, this);
                final OperationSubregistry appearing = childrenUpdater.putAtomic(this, key, newRegistry, snapshot);
                if (appearing == null) {
                    return newRegistry;
                } else if (appearing != newRegistry) {
                    // someone else added one
                    return appearing;
                }
                // otherwise, retry the loop because the map changed
            }
        }
    }

    void registerProxyHandler(final ListIterator<PathElement> iterator, final OperationHandler handler) {
        if (! iterator.hasNext()) {
            throw new IllegalArgumentException("Handlers have already been registered at location '" + getLocationString() + "'");
        }
        final PathElement element = iterator.next();
        getSubregistry(element.getKey()).registerProxyHandler(element.getValue(), iterator, handler);
    }
}

