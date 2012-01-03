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

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.GlobalJmxStatisticsConfigurationBuilder;
import org.infinispan.configuration.global.ShutdownHookBehavior;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.jboss.as.clustering.infinispan.ChannelProvider;
import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.ExecutorProvider;
import org.jboss.as.clustering.infinispan.InfinispanLogger;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;

/**
 * @author Paul Ferraro
 */
@Listener
public class EmbeddedCacheManagerService implements Service<EmbeddedCacheManager> {

    private static final Logger log = Logger.getLogger(EmbeddedCacheManagerService.class.getPackage().getName());
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME);

    public static ServiceName getServiceName(String name) {
        return (name != null) ? SERVICE_NAME.append(name) : SERVICE_NAME;
    }

    public static ServiceName getTransportServiceName(String name) {
        return getServiceName(name).append("transport");
    }

    public static ServiceName getTransportRequiredServiceName(String name) {
        return getTransportServiceName(name).append("required");
    }

    interface TransportConfiguration {
        Long getLockTimeout();
        String getSite();
        String getRack();
        String getMachine();

        Value<Channel> getChannel();
        Executor getExecutor();
    }

    interface Dependencies {
        TransportConfiguration getTransportConfiguration();
        MBeanServer getMBeanServer();
        Executor getListenerExecutor();
        ScheduledExecutorService getEvictionExecutor();
        ScheduledExecutorService getReplicationQueueExecutor();
    }

    private final String name;
    private final String defaultCache;
    private final Dependencies dependencies;

    private volatile EmbeddedCacheManager container;

    public EmbeddedCacheManagerService(String name, String defaultCache, Dependencies dependencies) {
        this.name = name;
        this.defaultCache = defaultCache;
        this.dependencies = dependencies;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public EmbeddedCacheManager getValue() throws IllegalStateException, IllegalArgumentException {
        return this.container;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {

        GlobalConfigurationBuilder globalBuilder = new GlobalConfigurationBuilder();
        globalBuilder.shutdown().hookBehavior(ShutdownHookBehavior.DONT_REGISTER).transport().strictPeerToPeer(false);

        // set up transport only if transport is required by some cache in the cache manager
        TransportConfiguration transport = this.dependencies.getTransportConfiguration();
        TransportConfigurationBuilder transportBuilder = globalBuilder.transport();

        // If our transport service is running, configure Infinispan to use it
        if (context.getController().getServiceContainer().getRequiredService(getTransportServiceName(this.name)).getState() == ServiceController.State.UP) {
            // See ISPN-1675
            // transportBuilder.transport(new ChannelTransport(transport.getChannel()));
            ChannelProvider.init(transportBuilder, transport.getChannel());
            Long timeout = transport.getLockTimeout();
            transportBuilder.distributedSyncTimeout((timeout != null) ? timeout.longValue() : 60000);
            String site = transport.getSite();
            if (site != null) {
                transportBuilder.siteId(site);
            }
            String rack = transport.getRack();
            if (rack != null) {
                transportBuilder.rackId(rack);
            }
            String machine = transport.getMachine();
            if (machine != null) {
                transportBuilder.machineId(machine);
            }
            transportBuilder.clusterName(this.name);

            Executor executor = transport.getExecutor();
            if (executor != null) {
                // See ISPN-1675
                // globalBuilder.asyncTransportExecutor().factory(new ManagedExecutorFactory(executor));
                ExecutorProvider.initTransportExecutor(globalBuilder, executor);
            }
        } else {
            transportBuilder.clearTransport();
        }

        Executor listenerExecutor = this.dependencies.getListenerExecutor();
        if (listenerExecutor != null) {
            // See ISPN-1675
            // globalBuilder.asyncListenerExecutor().factory(new ManagedExecutorFactory(listenerExecutor));
            ExecutorProvider.initListenerExecutor(globalBuilder, listenerExecutor);
        }
        ScheduledExecutorService evictionExecutor = this.dependencies.getEvictionExecutor();
        if (evictionExecutor != null) {
            // See ISPN-1675
            // globalBuilder.evictionScheduledExecutor().factory(new ManagedScheduledExecutorFactory(evictionExecutor));
            ExecutorProvider.initEvictionExecutor(globalBuilder, evictionExecutor);
        }
        ScheduledExecutorService replicationQueueExecutor = this.dependencies.getReplicationQueueExecutor();
        if (replicationQueueExecutor != null) {
            // See ISPN-1675
            // globalBuilder.replicationQueueScheduledExecutor().factory(new ManagedScheduledExecutorFactory(replicationQueueExecutor));
            ExecutorProvider.initReplicationQueueExecutor(globalBuilder, replicationQueueExecutor);
        }

        GlobalJmxStatisticsConfigurationBuilder jmxBuilder = globalBuilder.globalJmxStatistics().cacheManagerName(this.name);

        MBeanServer server = this.dependencies.getMBeanServer();
        if (server != null) {
            jmxBuilder.enable().mBeanServerLookup(new MBeanServerProvider(server)).jmxDomain(SERVICE_NAME.getCanonicalName());
        } else {
            jmxBuilder.disable();
        }

        // create the cache manager
        EmbeddedCacheManager manager = new DefaultCacheManager(globalBuilder.build(), false);
        this.container = new DefaultEmbeddedCacheManager(manager, this.defaultCache);
        this.container.addListener(this);
        this.container.start();
        log.debugf("%s cache container started", this.name);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.container.stop();
        this.container.removeListener(this);
        log.debugf("%s cache container stopped", this.name);
        this.container = null;
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
        InfinispanLogger.ROOT_LOGGER.cacheStarted(event.getCacheName(), event.getCacheManager().getGlobalConfiguration().getCacheManagerName());
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
        InfinispanLogger.ROOT_LOGGER.cacheStopped(event.getCacheName(), event.getCacheManager().getGlobalConfiguration().getCacheManagerName());
    }
}
