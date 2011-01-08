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

import org.jboss.as.controller.ModelDescriptionProvider;
import org.jboss.as.controller.PathElement;

/**
 * A registry of values within a specific key type.
 */
final class DescriptionProviderSubregistry {

    private final String keyName;
    private final ConcreteDescriptionProviderRegistry parent;
    @SuppressWarnings( { "unused" })
    private volatile Map<String, DescriptionProviderRegistry> childRegistries;

    private static final AtomicMapFieldUpdater<DescriptionProviderSubregistry, String, DescriptionProviderRegistry> childRegistriesUpdater = AtomicMapFieldUpdater.newMapUpdater(AtomicReferenceFieldUpdater.newUpdater(DescriptionProviderSubregistry.class, Map.class, "childRegistries"));

    DescriptionProviderSubregistry(final String keyName, final ConcreteDescriptionProviderRegistry parent) {
        this.keyName = keyName;
        this.parent = parent;
        childRegistriesUpdater.clear(this);
    }

    void register(final String elementValue, final ListIterator<PathElement> iterator, final ModelDescriptionProvider provider) {
        DescriptionProviderRegistry registry = childRegistriesUpdater.get(this, elementValue);
        if (registry == null) {
            final DescriptionProviderRegistry newRegistry = new ConcreteDescriptionProviderRegistry(elementValue, this);
            final DescriptionProviderRegistry appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
            registry = appearingRegistry != null ? appearingRegistry : newRegistry;
        }
        registry.register(iterator, provider);
    }

    void registerProxyHandler(final String elementValue, final ListIterator<PathElement> iterator, final ModelDescriptionProvider provider) {
        DescriptionProviderRegistry registry;
        registry = childRegistriesUpdater.get(this, elementValue);
        if (registry == null) {
            final boolean last = !iterator.hasNext();
            final DescriptionProviderRegistry newRegistry = last ? new ProxyDescriptionProviderRegistry(elementValue, this, provider) : new ConcreteDescriptionProviderRegistry(elementValue, this);
            final DescriptionProviderRegistry appearingRegistry = childRegistriesUpdater.putIfAbsent(this, elementValue, newRegistry);
            if (appearingRegistry == null && last) {
                return;
            }
            registry = appearingRegistry;
        }
        registry.registerProxyProvider(iterator, provider);
    }

    ModelDescriptionProvider getModelDescriptionProvider(final ListIterator<PathElement> iterator, final String child) {
        final Map<String, DescriptionProviderRegistry> snapshot = childRegistriesUpdater.get(this);
        final DescriptionProviderRegistry childRegistry = snapshot.get(child);
        if (childRegistry != null) {
            return childRegistry.getModelDescriptionProvider(iterator);
        } else {
            final DescriptionProviderRegistry wildcardRegistry = snapshot.get("*");
            if (wildcardRegistry != null) {
                return wildcardRegistry.getModelDescriptionProvider(iterator);
            } else {
                return null;
            }
        }
    }

    String getLocationString() {
        return parent.getLocationString() + "(" + keyName + " => ";
    }
}
