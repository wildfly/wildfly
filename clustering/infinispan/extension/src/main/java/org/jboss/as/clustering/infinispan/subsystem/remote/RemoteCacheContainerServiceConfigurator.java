/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
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
import org.wildfly.service.NonBlockingLifecycle;
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

        Supplier<CompletionStage<RemoteCacheManager>> factory = new Supplier<>() {
            @Override
            public CompletionStage<RemoteCacheManager> get() {
                return CompletableFuture.completedStage(new RemoteCacheManager(configuration.get(), false));
            }
        };
        Function<RemoteCacheManager, NonBlockingLifecycle> lifecycle = new Function<>() {
            @Override
            public NonBlockingLifecycle apply(RemoteCacheManager manager) {
                return new NonBlockingLifecycle() {
                    @Override
                    public boolean isStarted() {
                        return manager.isStarted();
                    }

                    @Override
                    public CompletionStage<Void> start() {
                        CompletionStage<Void> result = manager.startAsync();
                        result.thenRun(() -> InfinispanLogger.ROOT_LOGGER.remoteCacheContainerStarted(name));
                        return result;
                    }

                    @Override
                    public CompletionStage<Void> stop() {
                        CompletionStage<Void> result = manager.stopAsync();
                        result.thenRun(() -> InfinispanLogger.ROOT_LOGGER.remoteCacheContainerStopped(name));
                        return result;
                    }
                };
            }
        };
        Function<RemoteCacheManager, RemoteCacheContainer> wrapper = new Function<>() {
            @Override
            public RemoteCacheContainer apply(RemoteCacheManager manager) {
                return new ManagedRemoteCacheContainer(manager, name, loader.get(), registrar);
            }
        };
        return CapabilityServiceInstaller.NonBlockingBuilder.of(CAPABILITY, factory).map(wrapper)
                .withLifecycle(lifecycle)
                .requires(List.of(configuration, loader))
                .build();
    }

    static RemoteCacheManager createRemoteCacheManager(Configuration configuration) {
        return new RemoteCacheManager(configuration, false);
    }
}
