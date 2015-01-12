/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.CacheBuilderFactory;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.GroupServiceNameFactory;

/**
 * Provides the requisite builders for a clustered {@link RegistryFactory} created from the specified factory.
 * @author Paul Ferraro
 */
public class RegistryFactoryBuilderProvider implements CacheGroupBuilderProvider {

    private final CacheBuilderFactory<RegistryFactory<Object, Object>> factory;

    /**
     * Constructs a new provider for {@link RegistryFactory} service builders.
     */
    public RegistryFactoryBuilderProvider(CacheBuilderFactory<RegistryFactory<Object, Object>> factory) {
        this.factory = factory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Builder<?>> getBuilders(String containerName, String cacheName) {
        Builder<RegistryFactory<Object, Object>> builder = this.factory.createBuilder(containerName, cacheName);
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, CacheGroupServiceName.REGISTRY.toString(), containerName, cacheName).getAbsoluteName());
        Builder<ManagedReferenceFactory> bindingBuilder = new BinderServiceBuilder<>(binding, builder.getServiceName(), RegistryFactory.class);
        Builder<Registry<Object, Object>> registryBuilder = new RegistryBuilder<>(containerName, cacheName);
        return Arrays.asList(builder, bindingBuilder, registryBuilder);
    }
}
