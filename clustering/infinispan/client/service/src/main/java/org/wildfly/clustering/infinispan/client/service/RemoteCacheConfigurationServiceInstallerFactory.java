/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.infinispan.client.service;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.configuration.RemoteCacheConfiguration;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheConfigurationServiceInstallerFactory implements Function<BinaryServiceConfiguration, ServiceInstaller> {

    private final Consumer<RemoteCacheConfigurationBuilder> configurator;

    public RemoteCacheConfigurationServiceInstallerFactory(Consumer<RemoteCacheConfigurationBuilder> configurator) {
        this.configurator = configurator;
    }

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        Consumer<RemoteCacheConfigurationBuilder> configurator = this.configurator;
        ServiceDependency<RemoteCacheContainer> container = configuration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER);
        String cacheName = configuration.getChildName();
        Supplier<RemoteCacheConfiguration> cacheConfiguration = new Supplier<>() {
            @Override
            public RemoteCacheConfiguration get() {
                return container.get().getConfiguration().addRemoteCache(cacheName, configurator);
            }
        };
        Consumer<RemoteCacheConfiguration> stopTask = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfiguration config) {
                container.get().getConfiguration().removeRemoteCache(cacheName);
            }
        };
        return ServiceInstaller.builder(cacheConfiguration).blocking()
                .onStop(stopTask)
                .provides(configuration.resolveServiceName(HotRodServiceDescriptor.REMOTE_CACHE_CONFIGURATION))
                .requires(container)
                .build();
    }
}
