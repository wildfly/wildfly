/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.infinispan.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Configures a {@link org.jboss.msc.Service} providing a cache configuration based on a configuration template.
 * @author Paul Ferraro
 */
public class TemplateConfigurationServiceInstallerFactory implements BiFunction<BinaryServiceConfiguration, BinaryServiceConfiguration, ServiceInstaller> {

    private final Consumer<ConfigurationBuilder> configurator;

    /**
     * Constructs a new cache configuration builder.
     * @param containerName the name of the cache container
     * @param templateName the name of the template cache
     */
    public TemplateConfigurationServiceInstallerFactory() {
        this(Functions.discardingConsumer());
    }

    /**
     * Constructs a new cache configuration builder.
     * @param containerName the name of the cache container
     * @param templateName the name of the template cache
     * @param configurator provides additional configuration of the target cache
     */
    public TemplateConfigurationServiceInstallerFactory(Consumer<ConfigurationBuilder> configurator) {
        this.configurator = configurator;
    }

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration templateConfiguration, BinaryServiceConfiguration configuration) {
        ServiceDependency<Configuration> template = templateConfiguration.getServiceDependency(InfinispanServiceDescriptor.CACHE_CONFIGURATION);
        Consumer<ConfigurationBuilder> configurator = new Consumer<>() {
            @Override
            public void accept(ConfigurationBuilder builder) {
                builder.read(template.get());
            }
        };
        return new ConfigurationServiceInstallerFactory(configurator.andThen(this.configurator), List.of(template)).apply(configuration);
    }
}
