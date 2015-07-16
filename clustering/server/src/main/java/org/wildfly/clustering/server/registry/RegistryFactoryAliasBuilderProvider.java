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

package org.wildfly.clustering.server.registry;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.registry.RegistryEntryProvider;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupServiceName;
import org.wildfly.clustering.spi.CacheGroupAliasBuilderProvider;
import org.wildfly.clustering.spi.GroupServiceNameFactory;

/**
 * @author Paul Ferraro
 */
public class RegistryFactoryAliasBuilderProvider implements CacheGroupAliasBuilderProvider {

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<Builder<?>> getBuilders(String containerName, String aliasCacheName, String targetCacheName) {
        Builder<RegistryFactory> factoryBuilder = new AliasServiceBuilder<>(CacheGroupServiceName.REGISTRY_FACTORY.getServiceName(containerName, aliasCacheName), CacheGroupServiceName.REGISTRY_FACTORY.getServiceName(containerName, targetCacheName), RegistryFactory.class);
        Builder<Registry> registryBuilder = new AliasServiceBuilder<>(CacheGroupServiceName.REGISTRY.getServiceName(containerName, aliasCacheName), CacheGroupServiceName.REGISTRY.getServiceName(containerName, targetCacheName), Registry.class);
        Builder<RegistryEntryProvider> entryBuilder = new AliasServiceBuilder<>(CacheGroupServiceName.REGISTRY_ENTRY.getServiceName(containerName, targetCacheName), CacheGroupServiceName.REGISTRY_ENTRY.getServiceName(containerName, aliasCacheName), RegistryEntryProvider.class);
        ContextNames.BindInfo binding = ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, GroupServiceNameFactory.BASE_NAME, CacheGroupServiceName.REGISTRY.toString(), containerName, aliasCacheName).getAbsoluteName());
        Builder<ManagedReferenceFactory> bindingBuilder = new BinderServiceBuilder<>(binding, factoryBuilder.getServiceName(), RegistryFactory.class);
        return Arrays.asList(factoryBuilder, registryBuilder, entryBuilder, bindingBuilder);
    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
