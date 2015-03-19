/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.marshall.core.Ids;
import org.jboss.as.clustering.infinispan.ChannelTransport;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.ManagedExecutorFactory;
import org.jboss.as.clustering.infinispan.ManagedScheduledExecutorFactory;
import org.jboss.as.jmx.MBeanServerService;
import org.jboss.as.server.Services;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.infinispan.spi.io.SimpleExternalizer;
import org.wildfly.clustering.infinispan.spi.service.CacheContainerServiceName;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Paul Ferraro
 */
public class CacheContainerConfigurationBuilder implements Builder<GlobalConfiguration>, Value<GlobalConfiguration> {

    private final InjectedValue<ModuleLoader> loader = new InjectedValue<>();
    private final InjectedValue<MBeanServer> server = new InjectedValue<>();
    private final String name;
    private boolean statisticsEnabled;
    private ModuleIdentifier module;
    private ValueDependency<TransportConfiguration> transport = null;
    private ValueDependency<Executor> listenerExecutor = null;
    private ValueDependency<ScheduledExecutorService> evictionExecutor = null;
    private ValueDependency<ScheduledExecutorService> replicationQueueExecutor = null;

    public CacheContainerConfigurationBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return CacheContainerServiceName.CONFIGURATION.getServiceName(this.name);
    }

    @Override
    public ServiceBuilder<GlobalConfiguration> build(ServiceTarget target) {
        ServiceBuilder<GlobalConfiguration> builder = target.addService(this.getServiceName(), new ValueService<>(this))
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, this.loader)
                .addDependency(MBeanServerService.SERVICE_NAME, MBeanServer.class, this.server)
        ;
        if (this.transport != null) {
            this.transport.register(builder);
        }
        if (this.listenerExecutor != null) {
            this.listenerExecutor.register(builder);
        }
        if (this.evictionExecutor != null) {
            this.evictionExecutor.register(builder);
        }
        if (this.replicationQueueExecutor != null) {
            this.replicationQueueExecutor.register(builder);
        }
        return builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public GlobalConfiguration getValue() {

        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
        ModuleLoader moduleLoader = this.loader.getValue();
        builder.serialization().classResolver(ModularClassResolver.getInstance(moduleLoader));
        try {
            ClassLoader loader = (this.module != null) ? moduleLoader.loadModule(this.module).getClassLoader() : CacheContainerConfiguration.class.getClassLoader();
            builder.classLoader(loader);
            int id = Ids.MAX_ID;
            for (SimpleExternalizer<?> externalizer: ServiceLoader.load(SimpleExternalizer.class, loader)) {
                InfinispanLogger.ROOT_LOGGER.debugf("Cache container %s will use an externalizer for %s", this.name, externalizer.getTargetClass().getName());
                builder.serialization().addAdvancedExternalizer(id++, externalizer);
            }
        } catch (ModuleLoadException e) {
            throw new IllegalStateException(e);
        }
        builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);

        TransportConfiguration transport = (this.transport != null) ? this.transport.getValue() : null;

        if (transport != null) {
            org.infinispan.configuration.global.TransportConfigurationBuilder transportBuilder = builder.transport()
                    .clusterName(this.name)
                    .transport(new ChannelTransport(transport.getChannel(), transport.getChannelFactory()))
                    .distributedSyncTimeout(transport.getLockTimeout())
            ;

            // Topology is retrieved from the channel
            ProtocolStackConfiguration stack = transport.getChannelFactory().getProtocolStackConfiguration();
            org.wildfly.clustering.jgroups.spi.TransportConfiguration.Topology topology = stack.getTransport().getTopology();
            if (topology != null) {
                transportBuilder.siteId(topology.getSite()).rackId(topology.getRack()).machineId(topology.getMachine());
            }

            Executor executor = transport.getExecutor();
            if (executor != null) {
                transportBuilder.transportThreadPool().threadPoolFactory(new ManagedExecutorFactory(executor));
            }

            RelayConfiguration relay = stack.getRelay();
            if (relay != null) {
                builder.site().localSite(relay.getSiteName());
            }
        }

        Executor listenerExecutor = (this.listenerExecutor != null) ? this.listenerExecutor.getValue() : null;
        if (listenerExecutor != null) {
            builder.listenerThreadPool().threadPoolFactory(new ManagedExecutorFactory(listenerExecutor));
        }
        ScheduledExecutorService evictionExecutor = (this.evictionExecutor != null) ? this.evictionExecutor.getValue() : null;
        if (evictionExecutor != null) {
            builder.evictionThreadPool().threadPoolFactory(new ManagedScheduledExecutorFactory(evictionExecutor));
        }
        ScheduledExecutorService replicationQueueExecutor = (this.replicationQueueExecutor != null) ? this.replicationQueueExecutor.getValue() : null;
        if (replicationQueueExecutor != null) {
            builder.replicationQueueThreadPool().threadPoolFactory(new ManagedExecutorFactory(replicationQueueExecutor));
        }

        builder.globalJmxStatistics()
                .enabled(this.statisticsEnabled)
                .cacheManagerName(this.name)
                .mBeanServerLookup(new MBeanServerProvider(this.server.getValue()))
                .jmxDomain(CacheContainerServiceName.CACHE_CONTAINER.getServiceName(CacheServiceNameFactory.DEFAULT_CACHE).getParent().getCanonicalName())
                .allowDuplicateDomains(true);

        return builder.build();
    }

    public CacheContainerConfigurationBuilder setModule(ModuleIdentifier module) {
        this.module = module;
        return this;
    }

    public CacheContainerConfigurationBuilder setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
        return this;
    }

    public TransportConfigurationBuilder setTransport() {
        TransportConfigurationBuilder builder = new TransportConfigurationBuilder(this.name);
        this.transport = new InjectedValueDependency<>(builder, TransportConfiguration.class);
        return builder;
    }

    public CacheContainerConfigurationBuilder setListenerExecutor(String executorName) {
        if (executorName != null) {
            this.listenerExecutor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), Executor.class);
        }
        return this;
    }

    public CacheContainerConfigurationBuilder setEvictionExecutor(String executorName) {
        if (executorName != null) {
            this.evictionExecutor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), ScheduledExecutorService.class);
        }
        return this;
    }

    public CacheContainerConfigurationBuilder setReplicationQueueExecutor(String executorName) {
        if (executorName != null) {
            this.replicationQueueExecutor = new InjectedValueDependency<>(ThreadsServices.executorName(executorName), ScheduledExecutorService.class);
        }
        return this;
    }
}
