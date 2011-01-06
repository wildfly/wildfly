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

package org.jboss.as.controller;

import java.util.Collections;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A registry of model operations.  This registry is thread-safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class OperationRegistry {
    private final String valueString;
    private final OperationSubregistry parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, OperationSubregistry> children;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, OperationHandler> handlers;

    private static final AtomicMapFieldUpdater<OperationRegistry, String, OperationSubregistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationRegistry.class, Map.class, "children"));
    private static final AtomicMapFieldUpdater<OperationRegistry, String, OperationHandler> handlersUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationRegistry.class, Map.class, "handlers"));

    /**
     * Create a new empty registry.
     *
     * @return the new registry
     */
    public static OperationRegistry create() {
        return new OperationRegistry(null, null);
    }

    OperationRegistry(final String valueString, final OperationSubregistry parent) {
        this.valueString = valueString;
        this.parent = parent;
        childrenUpdater.clear(this);
        handlersUpdater.clear(this);
    }

    /**
     * Get a handler at a specific address.
     *
     * @param pathAddress the address
     * @param operationName the operation name
     * @return the operation handler, or {@code null} if none match
     */
    public OperationHandler getHandler(PathAddress pathAddress, String operationName) {
        return getHandler(pathAddress.iterator(), operationName);
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

    /**
     * Get all the handlers at a specific address.
     *
     * @param address the address
     * @return the handlers
     */
    public Map<String, OperationHandler> getHandlers(PathAddress address) {
        // might be a direct view, might be a copy - so just be safe
        return Collections.unmodifiableMap(getHandlers(address.iterator()));
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

    /**
     * Register an operation.
     *
     * @param pathAddress the applicable path
     * @param operationName the operation name
     * @param handler the handler for the operation
     * @throws IllegalArgumentException if a handler is already registered at that location
     */
    public void register(PathAddress pathAddress, String operationName, OperationHandler handler) {
        register(pathAddress.iterator(), operationName, handler);
    }

    void register(final ListIterator<PathElement> iterator, final String operationName, final OperationHandler handler) {
        if (! iterator.hasNext()) {
            if (handlersUpdater.putIfAbsent(this, operationName, handler) != null) {
                throw new IllegalArgumentException("A handler named '" + operationName + "' is already registered at location '" + getLocationString() + "'");
            }
        }
        final PathElement element = iterator.next();
        final String key = element.getKey();
        final Map<String, OperationSubregistry> snapshot = childrenUpdater.get(this);
        final OperationSubregistry subregistry = snapshot.get(key);
        if (subregistry != null) {
            subregistry.register(element.getValue(), iterator, operationName, handler);
        }
    }

    String getLocationString() {
        if (parent == null) {
            return "";
        } else {
            return parent.getLocationString() + valueString + ")";
        }
    }
}

/**
 * A registry of values within a specific key type.
 */
final class OperationSubregistry {

    private final String keyName;
    private final OperationRegistry parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, OperationRegistry> childRegistries;

    private static final AtomicMapFieldUpdater<OperationSubregistry, String, OperationRegistry> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(OperationSubregistry.class, Map.class, "childRegistries"));

    protected OperationSubregistry(final String keyName, final OperationRegistry parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    void register(final String elementValue, final ListIterator<PathElement> iterator, final String operationName, final OperationHandler handler) {
        OperationRegistry registry = childRegistriesUpdater.get(this, elementValue);
        if (registry == null) {
            final OperationRegistry newRegistry = new OperationRegistry(elementValue, this);
            final OperationRegistry appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
            registry = appearingRegistry != null ? appearingRegistry : newRegistry;
        }
        registry.register(iterator, operationName, handler);
    }

    OperationHandler getHandler(final ListIterator<PathElement> iterator, final String child, final String operationName) {
        final OperationRegistry childRegistry = childRegistriesUpdater.get(this, child);
        if (childRegistry != null) {
            return childRegistry.getHandler(iterator, operationName);
        } else {
            final OperationRegistry wildcardRegistry = childRegistriesUpdater.get(this, "*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getHandler(iterator, operationName);
            } else {
                return null;
            }
        }
    }

    Map<String, OperationHandler> getHandlers(final ListIterator<PathElement> iterator, final String child) {
        final Map<String, OperationRegistry> snapshot = childRegistriesUpdater.get(this);
        final OperationRegistry childRegistry = snapshot.get(child);
        final OperationRegistry wildcardRegistry = snapshot.get(this);
        if (wildcardRegistry == null) {
            if (childRegistry == null) {
                return Collections.emptyMap();
            } else {
                return childRegistry.getHandlers(iterator);
            }
        } else {
            if (childRegistry == null) {
                return wildcardRegistry.getHandlers(iterator);
            } else {
                final Map<String, OperationHandler> wildcardHandlers = wildcardRegistry.getHandlers(iterator);
                final Map<String, OperationHandler> childHandlers = childRegistry.getHandlers(iterator);
                final FastCopyHashMap<String, OperationHandler> combined = new FastCopyHashMap<String, OperationHandler>(childHandlers);
                combined.putAll(wildcardHandlers);
                return combined;
            }
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }
}
