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

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.*;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Capability.*;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.management.MBeanServer;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.SiteConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.marshall.core.Ids;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
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
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.marshalling.infinispan.AdvancedExternalizerAdapter;
import org.wildfly.clustering.marshalling.spi.DefaultExternalizer;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class GlobalConfigurationBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<GlobalConfiguration>, Value<GlobalConfiguration> {

    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final ValueDependency<Module> module;
    private final ValueDependency<TransportConfiguration> transport;
    private final ValueDependency<SiteConfiguration> site;
    private final Map<ThreadPoolResourceDefinition, ValueDependency<ThreadPoolConfiguration>> pools = new EnumMap<>(ThreadPoolResourceDefinition.class);
    private final Map<ScheduledThreadPoolResourceDefinition, ValueDependency<ThreadPoolConfiguration>> schedulers = new EnumMap<>(ScheduledThreadPoolResourceDefinition.class);
    private final String name;

    private volatile ValueDependency<MBeanServer> server;
    private volatile boolean statisticsEnabled;

    GlobalConfigurationBuilder(PathAddress address) {
        super(CONFIGURATION, address);
        this.name = address.getLastElement().getValue();
        this.module = new InjectedValueDependency<>(CacheContainerComponent.MODULE.getServiceName(address), Module.class);
        this.transport = new InjectedValueDependency<>(CacheContainerComponent.TRANSPORT.getServiceName(address), TransportConfiguration.class);
        this.site = new InjectedValueDependency<>(CacheContainerComponent.SITE.getServiceName(address), SiteConfiguration.class);
        EnumSet.allOf(ThreadPoolResourceDefinition.class).forEach(pool -> this.pools.put(pool, new InjectedValueDependency<>(pool.getServiceName(address), ThreadPoolConfiguration.class)));
        EnumSet.allOf(ScheduledThreadPoolResourceDefinition.class).forEach(pool -> this.schedulers.put(pool, new InjectedValueDependency<>(pool.getServiceName(address), ThreadPoolConfiguration.class)));
    }

    @Override
    public Builder<GlobalConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), null, null) ? new InjectedValueDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context), MBeanServer.class) : null;
        this.statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        return this;
    }

    @Override
    public GlobalConfiguration getValue() {
        org.infinispan.configuration.global.GlobalConfigurationBuilder builder = new org.infinispan.configuration.global.GlobalConfigurationBuilder();
        TransportConfiguration transport = this.transport.getValue();
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

        Module module = this.module.getValue();
        builder.serialization().classResolver(ModularClassResolver.getInstance(this.loader.getValue()));
        builder.classLoader(module.getClassLoader());
        int id = Ids.MAX_ID;
        Stream<? extends Externalizer<?>> commonExternalizers = EnumSet.allOf(DefaultExternalizer.class).stream();
        @SuppressWarnings("unchecked")
        Stream<Externalizer<?>> moduleExternalizers = (Stream<Externalizer<?>>) (Stream<?>) StreamSupport.stream(module.loadService(Externalizer.class).spliterator(), false);
        Iterable<Externalizer<?>> externalizers = Stream.concat(commonExternalizers, moduleExternalizers)::iterator;
        for (Externalizer<?> externalizer : externalizers) {
            InfinispanLogger.ROOT_LOGGER.debugf("Cache container %s will use an externalizer for %s", this.name, externalizer.getTargetClass().getName());
            builder.serialization().addAdvancedExternalizer(id++, new AdvancedExternalizerAdapter<>(externalizer));
        }

        builder.transport().transportThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.TRANSPORT).getValue());
        builder.transport().remoteCommandThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.REMOTE_COMMAND).getValue());

        builder.asyncThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.ASYNC_OPERATIONS).getValue());
        builder.expirationThreadPool().read(this.schedulers.get(ScheduledThreadPoolResourceDefinition.EXPIRATION).getValue());
        builder.listenerThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.LISTENER).getValue());
        builder.stateTransferThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.STATE_TRANSFER).getValue());
        builder.persistenceThreadPool().read(this.pools.get(ThreadPoolResourceDefinition.PERSISTENCE).getValue());

        builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);
        builder.globalJmxStatistics()
                .enabled(this.statisticsEnabled)
                .cacheManagerName(this.name)
                .mBeanServerLookup(new MBeanServerProvider((this.server != null) ? this.server.getValue() : null))
                .jmxDomain("org.wildfly.clustering.infinispan")
                .allowDuplicateDomains(true);

        builder.site().read(this.site.getValue());

        return builder.build();
    }

    @Override
    public ServiceBuilder<GlobalConfiguration> build(ServiceTarget target) {
        ServiceBuilder<GlobalConfiguration> builder = target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                ;
        this.pools.values().forEach(dependency -> dependency.register(builder));
        this.schedulers.values().forEach(dependency -> dependency.register(builder));
        Stream.of(this.module, this.transport, this.site, this.server).filter(Objects::nonNull).forEach(dependency -> dependency.register(builder));
        return builder;
    }
}
