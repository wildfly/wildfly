/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan;

import java.security.AccessController;
import java.util.concurrent.ThreadFactory;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.concurrent.CachedThreadPoolExecutorService;
import org.jboss.as.clustering.concurrent.RemoveOnCancelScheduledExecutorService;
import org.jboss.as.clustering.infinispan.subsystem.CacheConfigurationService;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.EmbeddedCacheManagerService;
import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.JBossThreadFactory;
import org.jboss.tm.XAResourceRecoveryRegistry;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilder;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.security.manager.action.GetAccessControlContextAction;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 */
public class InfinispanBeanManagerFactoryBuilder<G, I> implements BeanManagerFactoryBuilder<G, I, TransactionBatch> {

    private static final ThreadFactory EXPIRATION_THREAD_FACTORY = new JBossThreadFactory(new ThreadGroup(BeanExpirationScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    private static final ThreadFactory EVICTION_THREAD_FACTORY = new JBossThreadFactory(new ThreadGroup(BeanEvictionScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));

    private final String name;
    private final BeanManagerFactoryBuilderConfiguration config;

    public InfinispanBeanManagerFactoryBuilder(String name, BeanManagerFactoryBuilderConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public void installDeploymentUnitDependencies(ServiceTarget target, ServiceName deploymentUnitServiceName) {
        String cacheName = BeanCacheConfigurationService.getCacheName(deploymentUnitServiceName);
        ServiceName configurationServiceName = CacheConfigurationService.getServiceName(this.config.getContainerName(), cacheName);
        final InjectedValue<EmbeddedCacheManager> container = new InjectedValue<>();
        InjectedValue<Configuration> configuration = new InjectedValue<>();
        target.addService(configurationServiceName, new BeanCacheConfigurationService(cacheName, container, configuration))
                .addDependency(EmbeddedCacheManagerService.getServiceName(this.config.getContainerName()), EmbeddedCacheManager.class, container)
                .addDependency(CacheConfigurationService.getServiceName(this.config.getContainerName(), this.config.getCacheName()), Configuration.class, configuration)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;

        ServiceName cacheServiceName = CacheService.getServiceName(this.config.getContainerName(), cacheName);
        CacheService.Dependencies dependencies = new CacheService.Dependencies() {
            @Override
            public EmbeddedCacheManager getCacheContainer() {
                return container.getValue();
            }

            @Override
            public XAResourceRecoveryRegistry getRecoveryRegistry() {
                return null;
            }
        };
        AsynchronousService.addService(target, cacheServiceName, new CacheService<>(cacheName, dependencies))
                .addDependency(GlobalComponentRegistryService.getServiceName(this.config.getContainerName()))
                .addDependency(configurationServiceName)
                .addDependency(deploymentUnitServiceName.append("marshalling"))
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        RemoveOnCancelScheduledExecutorService.build(target, deploymentUnitServiceName.append(this.name, "expiration"), EXPIRATION_THREAD_FACTORY)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        CachedThreadPoolExecutorService.build(target, deploymentUnitServiceName.append(this.name, "eviction"), EVICTION_THREAD_FACTORY)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }

    @Override
    public <T> ServiceBuilder<? extends BeanManagerFactory<G, I, T, TransactionBatch>> build(ServiceTarget target, ServiceName name, BeanContext context) {
        return InfinispanBeanManagerFactory.build(this.name, target, name, this.config, context);
    }
}
