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

import org.jboss.as.controller.ModelDescriptionProvider;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;

/**
 * A registry of providers of descriptions of portions of the model.  This registry is thread-safe.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class DescriptionProviderRegistry {

    private final String valueString;
    private final DescriptionProviderSubregistry parent;

    DescriptionProviderRegistry(final String valueString, final DescriptionProviderSubregistry parent) {
        this.valueString = valueString;
        this.parent = parent;
    }

    /**
     * Create a new empty registry.
     *
     * @return the new registry
     */
    public static DescriptionProviderRegistry create() {
        return new ConcreteDescriptionProviderRegistry(null, null);
    }

    /**
     * Get a model description provider at a specific address.
     *
     * @param pathAddress the address
     * @return the model description provider, or {@code null} if none match
     */
    public final ModelDescriptionProvider getModelDescriptionProvider(final PathAddress pathAddress) {
        return getModelDescriptionProvider(pathAddress.iterator());
    }

    abstract ModelDescriptionProvider getModelDescriptionProvider(ListIterator<PathElement> iterator);

    /**
     * Register a model description provider.
     *
     * @param pathAddress the applicable path
     * @param provider the model description provider
     * @throws IllegalArgumentException if a handler is already registered at that location
     */
    public void register(final PathAddress pathAddress, final ModelDescriptionProvider provider) {
        register(pathAddress.iterator(), provider);
    }

    abstract void register(ListIterator<PathElement> iterator, ModelDescriptionProvider provider);

    /**
     * Register a proxy provider that will handle all requests at or below the given address.
     *
     * @param pathAddress the address to proxy
     * @param provider the provider to proxy to
     */
    public void registerProxyProvider(final PathAddress pathAddress, final ModelDescriptionProvider provider) {
        registerProxyProvider(pathAddress.iterator(), provider);
    }

    abstract void registerProxyProvider(ListIterator<PathElement> iterator, ModelDescriptionProvider provider);

    final String getLocationString() {
        if (parent == null) {
            return "";
        } else {
            return parent.getLocationString() + valueString + ")";
        }
    }

}
