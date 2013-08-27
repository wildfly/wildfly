/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.registry;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.registry.RegistryFactory;

/**
 * {@link Registry} service that creates a {@link Registry} from a factory and entry provider.
 * @author Paul Ferraro
 */
public class RegistryService<K, V> implements Service<Registry<K, V>> {

    @SuppressWarnings("rawtypes")
    private final Value<RegistryFactory> factory;
    @SuppressWarnings("rawtypes")
    private final Value<RegistryEntryProvider> provider;

    private volatile Registry<K, V> registry;

    @SuppressWarnings("rawtypes")
    public RegistryService(Value<RegistryFactory> factory, Value<RegistryEntryProvider> provider) {
        this.factory = factory;
        this.provider = provider;
    }

    @Override
    public Registry<K, V> getValue() {
        return this.registry;
    }

    @Override
    public void start(StartContext context) {
        this.registry = this.factory.getValue().createRegistry(this.provider.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.registry.close();
    }
}
