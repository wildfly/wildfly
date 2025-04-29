/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.MARSHALLER;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;

import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.commons.jmx.MBeanServerLookup;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.UncleanShutdownAction;
import org.infinispan.configuration.internal.PrivateGlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.infinispan.jmx.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class GlobalConfigurationServiceConfigurator implements ResourceServiceConfigurator {
    private final RuntimeCapability<Void> capability;

    GlobalConfigurationServiceConfigurator(RuntimeCapability<Void> capability) {
        this.capability = capability;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String containerName = address.getLastElement().getValue();

        ServiceDependency<MBeanServer> server = new MBeanServerResolver(this.capability).resolve(context, model);
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        ServiceDependency<List<Module>> containerModules = ServiceDependency.on(CacheContainerResourceDefinition.CACHE_CONTAINER_MODULES, containerName);
        ServiceDependency<TransportConfiguration> transport = ServiceDependency.on(TransportResourceDefinition.SERVICE_DESCRIPTOR, containerName);
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        Map<ThreadPoolResourceDefinition, ServiceDependency<ThreadPoolConfiguration>> pools = new EnumMap<>(ThreadPoolResourceDefinition.class);
        for (ThreadPoolResourceDefinition pool : EnumSet.of(ThreadPoolResourceDefinition.LISTENER, ThreadPoolResourceDefinition.BLOCKING, ThreadPoolResourceDefinition.NON_BLOCKING)) {
            pools.put(pool, ServiceDependency.on(pool, containerName));
        }
        Map<ScheduledThreadPoolResourceDefinition, ServiceDependency<ThreadPoolConfiguration>> scheduledPools = new EnumMap<>(ScheduledThreadPoolResourceDefinition.class);
        for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
            scheduledPools.put(pool, ServiceDependency.on(pool, containerName));
        }

        String defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        BiFunction<ModuleLoader, List<Module>, Marshaller> marshallerFactory = InfinispanMarshallerFactory.valueOf(MARSHALLER.resolveModelAttribute(context, model).asString());

        Supplier<GlobalConfiguration> global = new Supplier<>() {
            @Override
            public GlobalConfiguration get() {
                org.infinispan.configuration.global.GlobalConfigurationBuilder builder = new org.infinispan.configuration.global.GlobalConfigurationBuilder();
                builder.cacheManagerName(containerName)
                        .defaultCacheName(defaultCache)
                        .cacheContainer().statistics(statisticsEnabled)
                ;

                builder.transport().read(transport.get());

                List<Module> modules = containerModules.get();
                Marshaller marshaller = marshallerFactory.apply(loader.get(), modules);
                InfinispanLogger.ROOT_LOGGER.debugf("%s cache-container will use %s", containerName, marshaller.getClass().getName());
                // Register dummy serialization context initializer, to bypass service loading in org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl
                // Otherwise marshaller auto-detection will not work
                builder.serialization().marshaller(marshaller).addContextInitializer(new SerializationContextInitializer() {
                    @Deprecated
                    @Override
                    public String getProtoFile() throws UncheckedIOException {
                        return null;
                    }

                    @Deprecated
                    @Override
                    public String getProtoFileName() {
                        return null;
                    }

                    @Override
                    public void registerMarshallers(SerializationContext context) {
                    }

                    @Override
                    public void registerSchema(SerializationContext context) {
                    }
                });

                ClassLoader loader = modules.size() > 1 ? new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toList())) : modules.get(0).getClassLoader();
                builder.classLoader(loader);

                builder.blockingThreadPool().read(pools.get(ThreadPoolResourceDefinition.BLOCKING).get());
                builder.listenerThreadPool().read(pools.get(ThreadPoolResourceDefinition.LISTENER).get());
                builder.nonBlockingThreadPool().read(pools.get(ThreadPoolResourceDefinition.NON_BLOCKING).get());
                builder.expirationThreadPool().read(scheduledPools.get(ScheduledThreadPoolResourceDefinition.EXPIRATION).get());

                builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
                // Disable registration of MicroProfile Metrics
                builder.metrics().gauges(false).histograms(false);

                MBeanServerLookup mbeanServerProvider = Optional.ofNullable(server.get()).map(MBeanServerProvider::new).orElse(null);
                builder.jmx().domain("org.wildfly.clustering.infinispan")
                        .mBeanServerLookup(mbeanServerProvider)
                        .enabled(mbeanServerProvider != null)
                        ;

                // Disable triangle algorithm - we optimize for originator as primary owner
                // Do not enable server-mode for the Hibernate 2LC use case:
                // * The 2LC stack already overrides the interceptor for distribution caches
                // * This renders Infinispan default 2LC configuration unusable as it results in a default media type of application/unknown for keys and values
                // See ISPN-12252 for details
                builder.addModule(PrivateGlobalConfigurationBuilder.class).serverMode(!ServiceLoader.load(ModuleCommandExtensions.class, loader).iterator().hasNext());

                String path = InfinispanExtension.SUBSYSTEM_NAME + File.separatorChar + containerName;
                builder.globalState().enable()
                        .configurationStorage(ConfigurationStorage.VOLATILE)
                        .persistentLocation(path, environment.get().getServerDataDir().getPath())
                        .temporaryLocation(path, environment.get().getServerTempDir().getPath())
                        .uncleanShutdownAction(UncleanShutdownAction.PURGE)
                        ;
                return builder.build();
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, global)
                .requires(List.of(server, loader, containerModules, transport, environment))
                .requires(pools.values())
                .requires(scheduledPools.values())
                .asPassive()
                .build();
    }
}
