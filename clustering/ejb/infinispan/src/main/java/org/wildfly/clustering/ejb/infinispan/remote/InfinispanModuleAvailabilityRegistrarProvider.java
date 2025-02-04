/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.remote;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.wildfly.clustering.ejb.remote.ModuleAvailabilityRegistrarProvider;
import org.wildfly.clustering.infinispan.service.CacheServiceInstallerFactory;
import org.wildfly.clustering.infinispan.service.TemplateConfigurationServiceInstallerFactory;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.ClusteringServiceDescriptor;
import org.wildfly.clustering.server.service.FilteredBinaryServiceInstallerProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceInstaller;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The non-legacy version of the module availability registrar provider, used when the distributable-ejb subsystem is present.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanModuleAvailabilityRegistrarProvider implements ModuleAvailabilityRegistrarProvider {

    private final BinaryServiceConfiguration configuration;
    private final Consumer<ConfigurationBuilder> configurator;

    /**
     * Creates an instance of the Infinispan-based service provider registrar provider.
     * @param configuration a cache configuration
     */
    public InfinispanModuleAvailabilityRegistrarProvider(BinaryServiceConfiguration configuration) {
        this(configuration, Functions.discardingConsumer());
    }

    InfinispanModuleAvailabilityRegistrarProvider(BinaryServiceConfiguration configuration, Consumer<ConfigurationBuilder> configurator) {
        this.configuration = configuration;
        this.configurator = configurator;
    }

    @Override
    public Iterable<ServiceInstaller> getServiceInstallers(CapabilityServiceSupport support) {
        BinaryServiceConfiguration configuration = this.configuration.withChildName("module-availability");
        List<ServiceInstaller> installers = new LinkedList<>();
        // add in installers for cache service
        installers.add(new TemplateConfigurationServiceInstallerFactory(this.configurator).apply(this.configuration, configuration));
        installers.add(CacheServiceInstallerFactory.INSTANCE.apply(configuration));
        // add in installer for service provider registry
        new FilteredBinaryServiceInstallerProvider(Set.of(ClusteringServiceDescriptor.SERVICE_PROVIDER_REGISTRAR)).apply(support, configuration).forEach(installers::add);
        return installers;
    }
}
