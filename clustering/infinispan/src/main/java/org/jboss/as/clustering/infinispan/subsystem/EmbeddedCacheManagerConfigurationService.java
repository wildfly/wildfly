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
import org.infinispan.configuration.global.GlobalJmxStatisticsConfigurationBuilder;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.marshall.Ids;
import org.jboss.as.clustering.infinispan.ChannelProvider;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.ManagedExecutorFactory;
import org.jboss.as.clustering.infinispan.ManagedScheduledExecutorFactory;
import org.jboss.as.clustering.infinispan.io.SimpleExternalizer;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerConfigurationService implements Service<EmbeddedCacheManagerConfiguration>, EmbeddedCacheManagerConfiguration {
    public static ServiceName getServiceName(String name) {
        return EmbeddedCacheManagerService.getServiceName(name).append("config");
    }

    interface TransportConfiguration {
        Long getLockTimeout();
        ChannelFactory getChannelFactory();
        Executor getExecutor();
    }

    interface Dependencies {
        ModuleLoader getModuleLoader();
        TransportConfiguration getTransportConfiguration();
        MBeanServer getMBeanServer();
        Executor getListenerExecutor();
        ScheduledExecutorService getEvictionExecutor();
        ScheduledExecutorService getReplicationQueueExecutor();
    }

    private final String name;
    private final String defaultCache;
    private final Dependencies dependencies;
    private final ModuleIdentifier moduleId;
    private volatile GlobalConfiguration config;

    public EmbeddedCacheManagerConfigurationService(String name, String defaultCache, ModuleIdentifier moduleIdentifier, Dependencies dependencies) {
        this.name = name;
        this.defaultCache = defaultCache;
        this.moduleId = moduleIdentifier;
        this.dependencies = dependencies;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDefaultCache() {
        return this.defaultCache;
    }

    @Override
    public GlobalConfiguration getGlobalConfiguration() {
        return this.config;
    }

    @Override
    public ModuleIdentifier getModuleIdentifier() {
        return this.moduleId;
    }

    @Override
    public EmbeddedCacheManagerConfiguration getValue() {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {

        GlobalConfigurationBuilder builder = new GlobalConfigurationBuilder();
        ModuleLoader moduleLoader = this.dependencies.getModuleLoader();
        builder.serialization().classResolver(ModularClassResolver.getInstance(moduleLoader));
        try {
            ClassLoader loader = (this.moduleId != null) ? moduleLoader.loadModule(this.moduleId).getClassLoader() : EmbeddedCacheManagerConfiguration.class.getClassLoader();
            builder.classLoader(loader);
            int id = Ids.MAX_ID;
            for (SimpleExternalizer<?> externalizer: ServiceLoader.load(SimpleExternalizer.class, loader)) {
                builder.serialization().addAdvancedExternalizer(id++, externalizer);
            }
        } catch (ModuleLoadException e) {
            throw new StartException(e);
        }
        builder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER);

        TransportConfiguration transport = this.dependencies.getTransportConfiguration();
        TransportConfigurationBuilder transportBuilder = builder.transport();

        if (transport != null) {
            ChannelProvider.init(transportBuilder, ChannelService.getServiceName(this.name));
            Long timeout = transport.getLockTimeout();
            if (timeout != null) {
                transportBuilder.distributedSyncTimeout(timeout.longValue());
            }
            // Topology is retrieved from the channel
            org.jboss.as.clustering.jgroups.TransportConfiguration.Topology topology = transport.getChannelFactory().getProtocolStackConfiguration().getTransport().getTopology();
            if (topology != null) {
                String site = topology.getSite();
                if (site != null) {
                    transportBuilder.siteId(site);
                }
                String rack = topology.getRack();
                if (rack != null) {
                    transportBuilder.rackId(rack);
                }
                String machine = topology.getMachine();
                if (machine != null) {
                    transportBuilder.machineId(machine);
                }
            }
            transportBuilder.clusterName(this.name);

            Executor executor = transport.getExecutor();
            if (executor != null) {
                builder.asyncTransportExecutor().factory(new ManagedExecutorFactory(executor));
            }
        }

        Executor listenerExecutor = this.dependencies.getListenerExecutor();
        if (listenerExecutor != null) {
            builder.asyncListenerExecutor().factory(new ManagedExecutorFactory(listenerExecutor));
        }
        ScheduledExecutorService evictionExecutor = this.dependencies.getEvictionExecutor();
        if (evictionExecutor != null) {
            builder.evictionScheduledExecutor().factory(new ManagedScheduledExecutorFactory(evictionExecutor));
        }
        ScheduledExecutorService replicationQueueExecutor = this.dependencies.getReplicationQueueExecutor();
        if (replicationQueueExecutor != null) {
            builder.replicationQueueScheduledExecutor().factory(new ManagedScheduledExecutorFactory(replicationQueueExecutor));
        }

        GlobalJmxStatisticsConfigurationBuilder jmxBuilder = builder.globalJmxStatistics().cacheManagerName(this.name);

        MBeanServer server = this.dependencies.getMBeanServer();
        if (server != null) {
            jmxBuilder.enable()
                .mBeanServerLookup(new MBeanServerProvider(server))
                .jmxDomain(EmbeddedCacheManagerService.getServiceName(null).getCanonicalName())
                .allowDuplicateDomains(true)
            ;
        } else {
            jmxBuilder.disable();
        }
        this.config = builder.build();
    }

    @Override
    public void stop(StopContext context) {
        // Nothing to stop
    }
}
