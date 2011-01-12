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
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;

/**
 * A registry of values within a specific key type.
 */
final class NodeSubregistry {

    private final String keyName;
    private final ConcreteNodeRegistration parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, AbstractNodeRegistration> childRegistries;

    private static final AtomicMapFieldUpdater<NodeSubregistry, String, AbstractNodeRegistration> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(NodeSubregistry.class, Map.class, "childRegistries"));

    NodeSubregistry(final String keyName, final ConcreteNodeRegistration parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    ModelNodeRegistration register(final String elementValue, final DescriptionProvider provider) {
        final AbstractNodeRegistration newRegistry = new ConcreteNodeRegistration(elementValue, this, provider);
        final AbstractNodeRegistration appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (appearingRegistry == null) {
            return newRegistry;
        } else {
            throw new IllegalArgumentException("A node is already registered at '" + getLocationString() + elementValue + ")'");
        }
    }

    void registerProxySubModel(final String elementValue, final OperationHandler handler) {
        final AbstractNodeRegistration newRegistry = new ProxyNodeRegistration(elementValue, this, handler);
        final AbstractNodeRegistration appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
        if (appearingRegistry != null) {
            throw new IllegalArgumentException("A node is already registered at '" + getLocationString() + elementValue + ")'");
        }
    }

    OperationHandler getHandler(final ListIterator<PathElement> iterator, final String child, final String operationName) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getHandler(iterator, operationName);
        } else {
            final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getHandler(iterator, operationName);
            } else {
                return null;
            }
        }
    }

    Map<String, DescriptionProvider> getHandlers(final ListIterator<PathElement> iterator, final String child) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistriesUpdater.get(this);
        final AbstractNodeRegistration childRegistry = snapshot.get(child);
        final AbstractNodeRegistration wildcardRegistry = snapshot.get("*");
        if (wildcardRegistry == null) {
            if (childRegistry == null) {
                return Collections.emptyMap();
            } else {
                return childRegistry.getOperationDescriptions(iterator);
            }
        } else {
            if (childRegistry == null) {
                return wildcardRegistry.getOperationDescriptions(iterator);
            } else {
                final Map<String, DescriptionProvider> wildcardHandlers = wildcardRegistry.getOperationDescriptions(iterator);
                final Map<String, DescriptionProvider> childHandlers = childRegistry.getOperationDescriptions(iterator);
                final FastCopyHashMap<String, DescriptionProvider> combined = new FastCopyHashMap<String, DescriptionProvider>(childHandlers);
                combined.putAll(wildcardHandlers);
                return combined;
            }
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }

    DescriptionProvider getOperationDescription(final Iterator<PathElement> iterator, final String child, final String operationName) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getOperationDescription(iterator, operationName);
    }

    DescriptionProvider getModelDescription(final Iterator<PathElement> iterator, final String child) {
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getModelDescription(iterator);
    }

    Set<String> getChildNames(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getChildNames(iterator);
    }

    Set<String> getAttributeNames(final Iterator<PathElement> iterator, final String child){
        final Map<String, AbstractNodeRegistration> snapshot = childRegistries;
        AbstractNodeRegistration childRegistry = snapshot.get(child);
        if (childRegistry == null) {
            childRegistry = snapshot.get("*");
            if (childRegistry == null) {
                return null;
            }
        }
        return childRegistry.getAttributeNames(iterator);
    }
}
