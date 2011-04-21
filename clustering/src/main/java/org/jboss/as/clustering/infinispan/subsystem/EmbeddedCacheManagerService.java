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

import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;
import javax.transaction.TransactionManager;

import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.ChannelProvider;
import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.ExecutorProvider;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.util.loading.ContextClassLoaderSwitcher;
import org.jboss.util.loading.ContextClassLoaderSwitcher.SwitchContext;

/**
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerService implements Service<CacheContainer> {
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME);

    public static ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    public static ServiceName getServiceName(String name) {
        return SERVICE_NAME.append(name);
    }

    @SuppressWarnings("unchecked")
    private static final ContextClassLoaderSwitcher switcher = (ContextClassLoaderSwitcher) AccessController.doPrivileged(ContextClassLoaderSwitcher.INSTANTIATOR);

    private final EmbeddedCacheManagerConfiguration configuration;

    private volatile CacheContainer container;

    public EmbeddedCacheManagerService(EmbeddedCacheManagerConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public CacheContainer getValue() throws IllegalStateException, IllegalArgumentException {
        return this.container;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {

        EmbeddedCacheManagerDefaults defaults = this.configuration.getDefaults();
        GlobalConfiguration global = defaults.getGlobalConfiguration().clone();
        TransportConfiguration transport = this.configuration.getTransportConfiguration();
        if (transport != null) {
            Long timeout = transport.getLockTimeout();
            if (timeout != null) {
                global.setDistributedSyncTimeout(timeout.longValue());
            }
            String site = transport.getSite();
            if (site != null) {
                global.setSiteId(site);
            }
            String rack = transport.getRack();
            if (rack != null) {
                global.setRackId(rack);
            }
            String machine = transport.getMachine();
            if (machine != null) {
                global.setMachineId(machine);
            }
            String nodeName = transport.getEnvironment().getNodeName();
            global.setTransportNodeName(nodeName);
            global.setClusterName(String.format("%s-%s", nodeName, this.configuration.getName()));

            ChannelProvider.init(global, transport.getChannelFactory());

            Executor executor = transport.getExecutor();
            if (executor != null) {
                ExecutorProvider.initTransportExecutor(global, executor);
            }
        }

        global.setCacheManagerName(this.configuration.getName());

        Configuration defaultConfig = new Configuration();
        MBeanServer server = this.configuration.getMBeanServer();
        if (server != null) {
            global.setExposeGlobalJmxStatistics(true);
            global.setMBeanServerLookupInstance(new MBeanServerProvider(server));
            global.setJmxDomain(server.getDefaultDomain());
            defaultConfig.setExposeJmxStatistics(true);
        } else {
            global.setExposeGlobalJmxStatistics(false);
            defaultConfig.setExposeJmxStatistics(false);
        }

        Executor listenerExecutor = this.configuration.getListenerExecutor();
        if (listenerExecutor != null) {
            ExecutorProvider.initListenerExecutor(global, listenerExecutor);
        }
        ScheduledExecutorService evictionExecutor = this.configuration.getEvictionExecutor();
        if (evictionExecutor != null) {
            ExecutorProvider.initEvictionExecutor(global, evictionExecutor);
        }
        ScheduledExecutorService replicationQueueExecutor = this.configuration.getReplicationQueueExecutor();
        if (replicationQueueExecutor != null) {
            ExecutorProvider.initReplicationQueueExecutor(global, replicationQueueExecutor);
        }

        TransactionManager transactionManager = this.configuration.getTransactionManager();
        if (transactionManager != null) {
            defaultConfig.setTransactionManagerLookup(new TransactionManagerProvider(transactionManager));
        }

        SwitchContext switchContext = switcher.getSwitchContext(this.getClass().getClassLoader());

        try {
            EmbeddedCacheManager manager = new DefaultCacheManager(global, defaultConfig, false);

            // Add named configurations
            for (Map.Entry<String, Configuration> entry: this.configuration.getConfigurations().entrySet()) {
                Configuration overrides = entry.getValue();
                Configuration configuration = defaults.getDefaultConfiguration(overrides.getCacheMode()).clone();
                configuration.applyOverrides(overrides);
                manager.defineConfiguration(entry.getKey(), configuration);
            }

            this.container = new DefaultEmbeddedCacheManager(manager, this.configuration.getDefaultCache());
            this.container.start();
        } finally {
            switchContext.reset();
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.container.stop();
        this.container = null;
    }
}
