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

/**
 * A registry of values within a specific key type.
 */
final class Subregistry {

    private final String keyName;
    private final ConcreteOperationRegistry parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, OperationRegistry> childRegistries;

    private static final AtomicMapFieldUpdater<Subregistry, String, OperationRegistry> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(Subregistry.class, Map.class, "childRegistries"));

    Subregistry(final String keyName, final ConcreteOperationRegistry parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    void register(final String elementValue, final ListIterator<PathElement> iterator, final String operationName, final OperationHandler handler) {
        OperationRegistry registry = childRegistriesUpdater.get(this, elementValue);
        if (registry == null) {
            final OperationRegistry newRegistry = new ConcreteOperationRegistry(elementValue, this);
            final OperationRegistry appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
            registry = appearingRegistry != null ? appearingRegistry : newRegistry;
        }
        registry.register(iterator, operationName, handler);
    }

    void registerProxyHandler(final String elementValue, final ListIterator<PathElement> iterator, final OperationHandler handler) {
        OperationRegistry registry;
        registry = childRegistriesUpdater.get(this, elementValue);
        if (registry == null) {
            final boolean last = !iterator.hasNext();
            final OperationRegistry newRegistry = last ? new ProxyOperationRegistry(elementValue, this, handler) : new ConcreteOperationRegistry(elementValue, this);
            final OperationRegistry appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
            if (appearingRegistry == null && last) {
                return;
            }
            registry = appearingRegistry;
        }
        registry.registerProxyHandler(iterator, handler);
    }

    OperationHandler getHandler(final ListIterator<PathElement> iterator, final String child, final String operationName) {
        final Map<String, OperationRegistry> snapshot = childRegistriesUpdater.get(this);
        final OperationRegistry childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getHandler(iterator, operationName);
        } else {
            final OperationRegistry wildcardRegistry = snapshot.get("*");
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
