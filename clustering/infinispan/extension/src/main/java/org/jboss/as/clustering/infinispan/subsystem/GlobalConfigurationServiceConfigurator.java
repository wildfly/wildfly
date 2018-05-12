/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.STATISTICS_ENABLED;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.CONFIGURATION;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import org.infinispan.commons.marshall.Ids;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.SerializationConfigurationBuilder;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.SiteConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.internal.PrivateGlobalConfigurationBuilder;
import org.infinispan.globalstate.ConfigurationStorage;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.infinispan.AdvancedExternalizerAdapter;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
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

    private final SupplierDependency<Module> module;
    private final SupplierDependency<TransportConfiguration> transport;
    private final SupplierDependency<SiteConfiguration> site;
    private final Map<ThreadPoolResourceDefinition, SupplierDependency<ThreadPoolConfiguration>> pools = new EnumMap<>(ThreadPoolResourceDefinition.class);
    private final Map<ScheduledThreadPoolResourceDefinition, SupplierDependency<ThreadPoolConfiguration>> schedulers = new EnumMap<>(ScheduledThreadPoolResourceDefinition.class);
    private final String name;

    private volatile SupplierDependency<MBeanServer> server;
    private volatile Supplier<ModuleLoader> loader;
    private volatile String defaultCache;
    private volatile boolean statisticsEnabled;

    GlobalConfigurationServiceConfigurator(PathAddress address) {
        super(CONFIGURATION, address);
        this.name = address.getLastElement().getValue();
        this.module = new ServiceSupplierDependency<>(CacheContainerComponent.MODULE.getServiceName(address));
        this.transport = new ServiceSupplierDependency<>(CacheContainerComponent.TRANSPORT.getServiceName(address));
        this.site = new ServiceSupplierDependency<>(CacheContainerComponent.SITE.getServiceName(address));
        for (ThreadPoolResourceDefinition pool : EnumSet.complementOf(EnumSet.of(ThreadPoolResourceDefinition.CLIENT))) {
            this.pools.put(pool, new ServiceSupplierDependency<>(pool.getServiceName(address)));
        }
        for (ScheduledThreadPoolResourceDefinition pool : EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class)) {
            this.schedulers.put(pool, new ServiceSupplierDependency<>(pool.getServiceName(address)));
        }
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), null, null) ? new ServiceSupplierDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context)) : null;
        this.defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asStringOrNull();
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        return this;
    }

    @Override
    public GlobalConfiguration get() {
        org.infinispan.configuration.global.GlobalConfigurationBuilder builder = new org.infinispan.configuration.global.GlobalConfigurationBuilder();
        if (this.defaultCache != null) {
            builder.defaultCacheName(this.defaultCache);
        }
        TransportConfiguration transport = this.transport.get();
        // This fails due to ISPN-4755 !!
        // this.builder.transport().read(this.transport.getValue());
        // Workaround this by copying relevant fields individually
        builder.transport().transport(transport.transport())
                .distributedSyncTimeout(transport.distributedSyncTimeout())
                .clusterName(transport.clusterName())
                .machineId(transport.machineId())
                .rackId(transport.rackId())
                .siteId(transport.siteId())
        ;

        Module module = this.module.get();
        builder.serialization().classResolver(ModularClassResolver.getInstance(this.loader.get()));
        builder.classLoader(module.getClassLoader());
        int id = Ids.MAX_ID;
        SerializationConfigurationBuilder serialization = builder.serialization();
        for (Externalizer<?> externalizer : EnumSet.allOf(DefaultExternalizer.class)) {
            serialization.addAdvancedExternalizer(new AdvancedExternalizerAdapter<>(id++, externalizer));
        }
        for (Externalizer<?> externalizer : module.loadService(Externalizer.class)) {
            InfinispanLogger.ROOT_LOGGER.debugf("Cache container %s will use an externalizer for %s", this.name, externalizer.getTargetClass().getName());
            serialization.addAdvancedExternalizer(new AdvancedExternalizerAdapter<>(id++, externalizer));
        }

        builder.transport().transportThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.TRANSPORT).get());
        builder.transport().remoteCommandThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.REMOTE_COMMAND).get());

        builder.asyncThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.ASYNC_OPERATIONS).get());
        builder.expirationThreadPool().read(this.schedulers.get(ScheduledThreadPoolResourceDefinition.EXPIRATION).get());
        builder.listenerThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.LISTENER).get());
        builder.stateTransferThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.STATE_TRANSFER).get());
        builder.persistenceThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.PERSISTENCE).get());

        builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
        builder.globalJmxStatistics()
                .enabled(this.statisticsEnabled)
                .cacheManagerName(this.name)
                .mBeanServerLookup(new MBeanServerProvider((this.server != null) ? this.server.get() : null))
                .jmxDomain("org.wildfly.clustering.infinispan")
                .allowDuplicateDomains(true);

        builder.site().read(this.site.get());

        // Disable triangle algorithm
        // We optimize for originator as primary owner
        builder.addModule(PrivateGlobalConfigurationBuilder.class).serverMode(true);
        // Disable configuration storage
        builder.globalState().configurationStorage(ConfigurationStorage.IMMUTABLE).disable();

        return builder.build();
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<GlobalConfiguration> global = new CompositeDependency(this.module, this.transport, this.site, this.server).register(builder).provides(this.getServiceName());
        this.loader = builder.requires(Services.JBOSS_SERVICE_MODULE_LOADER);
        for (Dependency dependency: this.pools.values()) {
            dependency.register(builder);
        }
        for (Dependency dependency : this.schedulers.values()) {
            dependency.register(builder);
        }
        Service service = new FunctionalService<>(global, Function.identity(), this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }
}
