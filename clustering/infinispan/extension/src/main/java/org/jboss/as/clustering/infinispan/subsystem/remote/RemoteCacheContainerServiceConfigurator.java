/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.Configuration;
import org.jboss.as.clustering.infinispan.client.ManagedRemoteCacheContainer;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.clustering.server.Registrar;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Configures a service providing a {@link RemoteCacheContainer}.
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public enum RemoteCacheContainerServiceConfigurator implements ResourceServiceConfigurator {
    INSTANCE;

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER).build();

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        ServiceDependency<Configuration> configuration = ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION, name);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);

        Registrar<String> registrar = (RemoteCacheContainerResource) context.readResource(PathAddress.EMPTY_ADDRESS);

        Supplier<RemoteCacheManager> factory = configuration.map(RemoteCacheContainerServiceConfigurator::createRemoteCacheManager);
        Consumer<RemoteCacheManager> start = new Consumer<>() {
            @Override
            public void accept(RemoteCacheManager manager) {
                manager.start();
                InfinispanLogger.ROOT_LOGGER.remoteCacheContainerStarted(name);
            }
        };
        Consumer<RemoteCacheManager> stop = new Consumer<>() {
            @Override
            public void accept(RemoteCacheManager manager) {
                manager.stop();
                InfinispanLogger.ROOT_LOGGER.remoteCacheContainerStopped(name);
            }
        };
        Function<RemoteCacheManager, RemoteCacheContainer> wrapper = new Function<>() {
            @Override
            public RemoteCacheContainer apply(RemoteCacheManager manager) {
                return new ManagedRemoteCacheContainer(manager, name, loader.get(), registrar);
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, wrapper, factory).blocking()
                .onStart(start)
                .onStop(stop)
                .requires(List.of(configuration, loader))
                .build();
    }

    static RemoteCacheManager createRemoteCacheManager(Configuration configuration) {
        return new RemoteCacheManager(configuration, false);
    }
}
