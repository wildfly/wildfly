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

import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionProvider;

final class ConcreteDescriptionProviderRegistry extends DescriptionProviderRegistry {
    @SuppressWarnings( { "unused" })
    private volatile Map<String, DescriptionProviderSubregistry> children;
    @SuppressWarnings( { "unused" })
    private volatile ModelDescriptionProvider provider;

    private static final AtomicMapFieldUpdater<ConcreteDescriptionProviderRegistry, String, DescriptionProviderSubregistry> childrenUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(ConcreteDescriptionProviderRegistry.class, Map.class, "children"));
    private static final AtomicReferenceFieldUpdater<ConcreteDescriptionProviderRegistry, ModelDescriptionProvider> providersUpdater = AtomicReferenceFieldUpdater.newUpdater(ConcreteDescriptionProviderRegistry.class, ModelDescriptionProvider.class, "provider");

    ConcreteDescriptionProviderRegistry(final String valueString, final DescriptionProviderSubregistry parent) {
        super(valueString, parent);
        childrenUpdater.clear(this);
    }

    @Override
    ModelDescriptionProvider getModelDescriptionProvider(final ListIterator<PathElement> iterator) {
        if (! iterator.hasNext()) {
            return providersUpdater.get(this);
        }
        final PathElement next = iterator.next();
        try {
            final String key = next.getKey();
            final Map<String, DescriptionProviderSubregistry> snapshot = childrenUpdater.get(this);
            final DescriptionProviderSubregistry subregistry = snapshot.get(key);
            return subregistry == null ? null : subregistry.getModelDescriptionProvider(iterator, next.getValue());
        } finally {
            iterator.previous();
        }
    }

    @Override
    void register(final ListIterator<PathElement> iterator, final ModelDescriptionProvider provider) {
        if (! iterator.hasNext()) {
            if (providersUpdater.compareAndSet(this, null, provider)) {
                throw new IllegalArgumentException("A provider is already registered at location '" + getLocationString() + "'");
            }
        }
        final PathElement element = iterator.next();
        getSubregistry(element.getKey()).register(element.getValue(), iterator, provider);
    }

    DescriptionProviderSubregistry getSubregistry(final String key) {
        for (;;) {
            final Map<String, DescriptionProviderSubregistry> snapshot = childrenUpdater.get(this);
            final DescriptionProviderSubregistry subregistry = snapshot.get(key);
            if (subregistry != null) {
                return subregistry;
            } else {
                final DescriptionProviderSubregistry newRegistry = new DescriptionProviderSubregistry(key, this);
                final DescriptionProviderSubregistry appearing = childrenUpdater.putAtomic(this, key, newRegistry, snapshot);
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

    @Override
    void registerProxyProvider(final ListIterator<PathElement> iterator, final ModelDescriptionProvider provider) {
        if (! iterator.hasNext()) {
            throw new IllegalArgumentException("Providers have already been registered at location '" + getLocationString() + "'");
        }
        final PathElement element = iterator.next();
        getSubregistry(element.getKey()).registerProxyHandler(element.getValue(), iterator, provider);
    }
}

