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
import org.jboss.as.controller.PathElement;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ProxyDescriptionProviderRegistry extends DescriptionProviderRegistry {
    private final ModelDescriptionProvider provider;

    ProxyDescriptionProviderRegistry(final String valueString, final DescriptionProviderSubregistry parent, final ModelDescriptionProvider provider) {
        super(valueString, parent);
        this.provider = provider;
    }

    @Override
    ModelDescriptionProvider getModelDescriptionProvider(final ListIterator<PathElement> iterator) {
        return provider;
    }

    @Override
    void register(final ListIterator<PathElement> iterator, final ModelDescriptionProvider provider) {
        throw new IllegalArgumentException("A proxy provider is already registered at location '" + getLocationString() + "'");
    }

    @Override
    void registerProxyProvider(final ListIterator<PathElement> iterator, final ModelDescriptionProvider provider) {
        throw new IllegalArgumentException("A proxy provider is already registered at location '" + getLocationString() + "'");
    }
}
