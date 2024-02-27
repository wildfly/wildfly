/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.MARSHALLER;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.CONFIGURATION;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.management.MBeanServer;

import org.infinispan.commands.module.ModuleCommandExtensions;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.internal.PrivateGlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.infinispan.jmx.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.marshall.InfinispanMarshallerFactory;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class GlobalConfigurationServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<GlobalConfiguration> {

    private final SupplierDependency<ModuleLoader> loader;
    private final SupplierDependency<List<Module>> modules;
    private final SupplierDependency<TransportConfiguration> transport;
    private final SupplierDependency<ServerEnvironment> environment;
    private final Map<ThreadPoolResourceDefinition, SupplierDependency<ThreadPoolConfiguration>> pools = new EnumMap<>(ThreadPoolResourceDefinition.class);
    private final Map<ScheduledThreadPoolResourceDefinition, SupplierDependency<ThreadPoolConfiguration>> scheduledPools = new EnumMap<>(ScheduledThreadPoolResourceDefinition.class);
    private final String name;

    private volatile SupplierDependency<MBeanServer> server;
    private volatile String defaultCache;
    private volatile boolean statisticsEnabled;
    private volatile InfinispanMarshallerFactory marshallerFactory;

    GlobalConfigurationServiceConfigurator(PathAddress address) {
        super(CONFIGURATION, address);
        this.name = address.getLastElement().getValue();
        this.loader = new ServiceSupplierDependency<>(Services.JBOSS_SERVICE_MODULE_LOADER);
        this.modules = new ServiceSupplierDependency<>(CacheContainerComponent.MODULES.getServiceName(address));
        this.transport = new ServiceSupplierDependency<>(CacheContainerComponent.TRANSPORT.getServiceName(address));
        this.environment = new ServiceSupplierDependency<>(ServerEnvironmentService.SERVICE_NAME);
        for (ThreadPoolResourceDefinition pool : EnumSet.of(ThreadPoolResourceDefinition.LISTENER, ThreadPoolResourceDefinition.BLOCKING, ThreadPoolResourceDefinition.NON_BLOCKING)) {
            this.pools.put(pool, new ServiceSupplierDependency<>(pool.getServiceName(address)));
        }
        for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
            this.scheduledPools.put(pool, new ServiceSupplierDependency<>(pool.getServiceName(address)));
        }
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), CacheContainerResourceDefinition.Capability.CONFIGURATION.getDefinition().getDynamicName(context.getCurrentAddress()), null) ? new ServiceSupplierDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context)) : null;
        this.defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        this.marshallerFactory = InfinispanMarshallerFactory.valueOf(MARSHALLER.resolveModelAttribute(context, model).asString());
        return this;
    }

    @Override
    public GlobalConfiguration get() {
        org.infinispan.configuration.global.GlobalConfigurationBuilder builder = new org.infinispan.configuration.global.GlobalConfigurationBuilder();
        builder.cacheManagerName(this.name)
                .defaultCacheName(this.defaultCache)
                .cacheContainer().statistics(this.statisticsEnabled)
        ;

        builder.transport().read(this.transport.get());

        List<Module> modules = this.modules.get();
        Marshaller marshaller = this.marshallerFactory.apply(this.loader.get(), modules);
        InfinispanLogger.ROOT_LOGGER.debugf("%s cache-container will use %s", this.name, marshaller.getClass().getName());
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

        builder.blockingThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.BLOCKING).get());
        builder.listenerThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.LISTENER).get());
        builder.nonBlockingThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.NON_BLOCKING).get());
        builder.expirationThreadPool().read(this.scheduledPools.get(ScheduledThreadPoolResourceDefinition.EXPIRATION).get());

        builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
        // Disable registration of MicroProfile Metrics
        builder.metrics().gauges(false).histograms(false).accurateSize(true);
        builder.jmx().domain("org.wildfly.clustering.infinispan")
                .mBeanServerLookup((this.server != null) ? new MBeanServerProvider(this.server.get()) : null)
                .enabled(this.server != null)
                ;

        // Disable triangle algorithm - we optimize for originator as primary owner
        // Do not enable server-mode for the Hibernate 2LC use case:
        // * The 2LC stack already overrides the interceptor for distribution caches
        // * This renders Infinispan default 2LC configuration unusable as it results in a default media type of application/unknown for keys and values
        // See ISPN-12252 for details
        builder.addModule(PrivateGlobalConfigurationBuilder.class).serverMode(!ServiceLoader.load(ModuleCommandExtensions.class, loader).iterator().hasNext());

        String path = InfinispanExtension.SUBSYSTEM_NAME + File.separatorChar + this.name;
        ServerEnvironment environment = this.environment.get();
        builder.globalState().enable()
                .configurationStorage(ConfigurationStorage.VOLATILE)
                .persistentLocation(path, environment.getServerDataDir().getPath())
                .temporaryLocation(path, environment.getServerTempDir().getPath())
                ;
        return builder.build();
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<GlobalConfiguration> global = new CompositeDependency(this.loader, this.modules, this.transport, this.server, this.environment).register(builder).provides(this.getServiceName());
        for (Dependency dependency: this.pools.values()) {
            dependency.register(builder);
        }
        for (Dependency dependency: this.scheduledPools.values()) {
            dependency.register(builder);
        }
        Service service = new FunctionalService<>(global, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
