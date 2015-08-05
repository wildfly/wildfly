/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.server.provider;

import java.util.Set;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistration;
import org.wildfly.clustering.provider.ServiceProviderRegistration.Listener;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.provider.ServiceProviderRegistry;

/**
 * Adapts a {@link ServiceProviderRegistry} to the deprecated {@link ServiceProviderRegistrationFactory} interface.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
public class ServiceProviderRegistrationFactoryAdapter<T> implements ServiceProviderRegistrationFactory<T> {

    private final ServiceProviderRegistry<T> registry;

    public ServiceProviderRegistrationFactoryAdapter(ServiceProviderRegistry<T> registry) {
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Group getGroup() {
        return this.registry.getGroup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceProviderRegistration<T> register(T service) {
        return this.registry.register(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceProviderRegistration<T> register(T service, Listener listener) {
        return this.registry.register(service, listener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Node> getProviders(T service) {
        return this.registry.getProviders(service);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<T> getServices() {
        return this.registry.getServices();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceProviderRegistration<T> createRegistration(T service, Listener listener) {
        return this.registry.register(service, listener);
    }
}
