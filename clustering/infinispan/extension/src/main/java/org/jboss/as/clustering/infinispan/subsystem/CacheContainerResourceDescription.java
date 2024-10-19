/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.MBeanServer;

import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.commons.jmx.MBeanServerLookup;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.internal.PrivateGlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.as.clustering.controller.EnumAttributeDefinition;
import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.controller.ModuleListAttributeDefinition;
import org.jboss.as.clustering.controller.StatisticsEnabledAttributeDefinition;
import org.jboss.as.clustering.infinispan.jmx.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.wildfly.clustering.cache.infinispan.embedded.lifecycle.WildFlyClusteringModuleLifecycle;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceAttributeDefinition;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 *
 */
public enum CacheContainerResourceDescription implements ResourceCapabilityDescription<GlobalConfiguration>, ResourceServiceConfigurator {
    INSTANCE;

    static PathElement pathElement(String containerName) {
        return PathElement.pathElement("cache-container", containerName);
    }

    private static final PathElement PATH = pathElement(PathElement.WILDCARD_VALUE);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION).build();

    static final CapabilityReferenceAttributeDefinition<Configuration> DEFAULT_CACHE = new CapabilityReferenceAttributeDefinition.Builder<>("default-cache", CapabilityReference.builder(CAPABILITY, InfinispanServiceDescriptor.CACHE_CONFIGURATION).withParentPath(PATH).build()).setRequired(false).build();
    static final StatisticsEnabledAttributeDefinition STATISTICS_ENABLED = new StatisticsEnabledAttributeDefinition.Builder().build();
    static final EnumAttributeDefinition<InfinispanMarshallerFactory> MARSHALLER = new EnumAttributeDefinition.Builder<>("marshaller", InfinispanMarshallerFactory.LEGACY).build();
    static final StringListAttributeDefinition ALIASES = new StringListAttributeDefinition.Builder("aliases").setRequired(false).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES).build();
    static final ModuleListAttributeDefinition MODULES = new ModuleListAttributeDefinition.Builder().setRequired(false).setDefaultValue(Module.forClass(WildFlyClusteringModuleLifecycle.class)).build();

    @Override
    public PathElement getPathElement() {
        return PATH;
    }

    @Override
    public UnaryServiceDescriptor<GlobalConfiguration> getServiceDescriptor() {
        return InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(DEFAULT_CACHE, STATISTICS_ENABLED, MARSHALLER, ALIASES, MODULES);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        List<ModelNode> aliases = ALIASES.resolveModelAttribute(context, model).asListOrEmpty();
        String defaultCacheName = DEFAULT_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        boolean statisticsEnabled = STATISTICS_ENABLED.resolve(context, model);
        InfinispanMarshallerFactory marshallerFactory = MARSHALLER.resolve(context, model);
        if (marshallerFactory == InfinispanMarshallerFactory.LEGACY) {
            InfinispanLogger.ROOT_LOGGER.marshallerEnumValueDeprecated(MARSHALLER.getName(), InfinispanMarshallerFactory.LEGACY, EnumSet.complementOf(EnumSet.of(InfinispanMarshallerFactory.LEGACY)));
        }
        ServiceDependency<ModuleLoader> loader = ServiceDependency.on(Services.JBOSS_SERVICE_MODULE_LOADER);
        ServiceDependency<List<Module>> containerModules = MODULES.resolve(context, model);
        ServiceDependency<TransportConfiguration> transport = ServiceDependency.on(TransportResourceDescription.SERVICE_DESCRIPTOR, name);
        ServiceDependency<ServerEnvironment> environment = ServiceDependency.on(ServerEnvironment.SERVICE_DESCRIPTOR);
        ServiceDependency<MBeanServer> mbeanServer = new MBeanServerResolver(CAPABILITY).resolve(context, model);
        Map<ThreadPool, ServiceDependency<ThreadPoolConfiguration>> pools = new EnumMap<>(ThreadPool.class);
        Map<ScheduledThreadPool, ServiceDependency<ThreadPoolConfiguration>> scheduledPools = new EnumMap<>(ScheduledThreadPool.class);
        for (ThreadPool pool : EnumSet.allOf(ThreadPool.class)) {
            pools.put(pool, ServiceDependency.on(pool.getServiceDescriptor(), name));
        }
        for (ScheduledThreadPool pool : EnumSet.allOf(ScheduledThreadPool.class)) {
            scheduledPools.put(pool, ServiceDependency.on(pool.getServiceDescriptor(), name));
        }
        Supplier<GlobalConfiguration> factory = new Supplier<>() {
            @Override
            public GlobalConfiguration get() {
                GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
                builder.cacheManagerName(name)
                        .defaultCacheName(defaultCacheName)
                        .cacheContainer().statistics(statisticsEnabled)
                ;

                builder.transport().read(transport.get());

                List<Module> modules = containerModules.get();
                Marshaller marshaller = marshallerFactory.apply(loader.get(), modules);
                InfinispanLogger.ROOT_LOGGER.debugf("%s cache-container will use %s", name, marshaller.getClass().getName());
                // Register dummy serialization context initializer, to bypass service loading in org.infinispan.marshall.protostream.impl.SerializationContextRegistryImpl
                // Otherwise marshaller auto-detection will not work
                builder.serialization().marshaller(marshaller).addContextInitializer(new SerializationContextInitializer() {
                    @Deprecated
                    @Override
                    public String getProtoFile() {
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

                builder.blockingThreadPool().read(pools.get(ThreadPool.BLOCKING).get());
                builder.listenerThreadPool().read(pools.get(ThreadPool.LISTENER).get());
                builder.nonBlockingThreadPool().read(pools.get(ThreadPool.NON_BLOCKING).get());
                builder.expirationThreadPool().read(scheduledPools.get(ScheduledThreadPool.EXPIRATION).get());

                builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
                // Disable registration of MicroProfile Metrics
                builder.metrics().gauges(false).histograms(false).accurateSize(true);

                MBeanServerLookup mbeanServerProvider = Optional.ofNullable(mbeanServer.get()).map(MBeanServerProvider::new).orElse(null);
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

                String path = InfinispanSubsystemResourceDescription.INSTANCE.getName() + File.separatorChar + name;
                builder.globalState().enable()
                        .configurationStorage(ConfigurationStorage.VOLATILE)
                        .persistentLocation(path, environment.get().getServerDataDir().getPath())
                        .temporaryLocation(path, environment.get().getServerTempDir().getPath())
                        ;
                return builder.build();
            }
        };
        CapabilityServiceInstaller.Builder<GlobalConfiguration, GlobalConfiguration> builder = CapabilityServiceInstaller.builder(CAPABILITY, factory);
        for (ModelNode alias : aliases) {
            builder.provides(ServiceNameFactory.resolveServiceName(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION, alias.asString()));
        }
        return builder.asPassive()
            .requires(List.of(mbeanServer, loader, containerModules, transport, environment))
            .requires(pools.values())
            .requires(scheduledPools.values())
            .build();
    }
}
