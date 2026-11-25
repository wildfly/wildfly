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
import org.infinispan.client.hotrod.configuration.TransactionMode;
import org.jboss.as.controller.management.Capabilities;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.service.BlockingLifecycle;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class RemoteCacheConfigurationServiceInstallerFactory implements Function<BinaryServiceConfiguration, ServiceInstaller> {
    private static final Consumer<RemoteCacheConfigurationBuilder> DISABLE_TRANSACTIONS = builder -> builder.transactionMode(TransactionMode.NONE);

    private final Consumer<RemoteCacheConfigurationBuilder> configurator;

    public RemoteCacheConfigurationServiceInstallerFactory(Consumer<RemoteCacheConfigurationBuilder> configurator) {
        this.configurator = configurator;
    }

    @Override
    public ServiceInstaller apply(BinaryServiceConfiguration configuration) {
        java.util.function.Consumer<RemoteCacheConfigurationBuilder> configurator = this.configurator;
        ServiceDependency<RemoteCacheContainer> container = configuration.getServiceDependency(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER);
        String cacheName = configuration.getChildName();
        Supplier<RemoteCacheConfiguration> cacheConfiguration = new Supplier<>() {
            @Override
            public RemoteCacheConfiguration get() {
                RemoteCacheContainer manager = container.get();
                // Disable client transactions if remote cache already exists but is not transactional.
                return container.get().getConfiguration().addRemoteCache(cacheName, manager.getCacheNames().contains(cacheName) && !manager.isTransactional(cacheName) ? configurator.andThen(DISABLE_TRANSACTIONS) : configurator);
            }
        };
        Consumer<RemoteCacheConfiguration> remove = new Consumer<>() {
            @Override
            public void accept(RemoteCacheConfiguration config) {
                container.get().getConfiguration().removeRemoteCache(cacheName);
            }
        };
        return ServiceInstaller.BlockingBuilder.of(cacheConfiguration, ServiceDependency.on(Capabilities.MANAGEMENT_EXECUTOR))
                .provides(configuration.resolveServiceName(HotRodServiceDescriptor.REMOTE_CACHE_CONFIGURATION))
                .requires(container)
                .withLifecycle(BlockingLifecycle.compose(remove))
                .build();
    }
}
