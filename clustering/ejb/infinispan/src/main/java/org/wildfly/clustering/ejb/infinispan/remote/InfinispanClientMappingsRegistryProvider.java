/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.ejb.infinispan.network.ClientMappingsRegistryEntryServiceInstallerFactory;
import org.wildfly.clustering.ejb.remote.ClientMappingsRegistryProvider;
import org.wildfly.clustering.infinispan.service.CacheServiceInstallerFactory;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceInstallerFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.FilteredBinaryServiceInstallerProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * The non-legacy version of the client mappings registry provider, used when the distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanClientMappingsRegistryProvider implements ClientMappingsRegistryProvider {

    private final BinaryServiceConfiguration configuration;
    private final Consumer<ConfigurationBuilder> configurator;

    /**
     * Creates an instance of the Infinispan-based client mappings registry provider, for local or distribute use, based on a cache-service abstraction.
     * @param configuration a cache configuration
     */
    public InfinispanClientMappingsRegistryProvider(BinaryServiceConfiguration configuration) {
        this(configuration, Functions.discardingConsumer());
    }

    InfinispanClientMappingsRegistryProvider(BinaryServiceConfiguration configuration, Consumer<ConfigurationBuilder> configurator) {
        this.configuration = configuration;
        this.configurator = configurator;
    }

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(String connectorName, ServiceDependency<List<ClientMapping>> clientMappings) {
        BinaryServiceConfiguration configuration = this.configuration.withChildName(connectorName);
        List<ServiceInstaller> installers = new LinkedList<>();
        installers.add(new TemplateConfigurationServiceInstallerFactory(this.configurator).apply(this.configuration, configuration));
        installers.add(CacheServiceInstallerFactory.INSTANCE.apply(configuration));
        installers.add(new ClientMappingsRegistryEntryServiceInstallerFactory(clientMappings).apply(configuration));
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.REGISTRY, ClusteringServiceDescriptor.REGISTRY_FACTORY)).apply(configuration).forEach(installers::add);
        return installers;
    }
}
