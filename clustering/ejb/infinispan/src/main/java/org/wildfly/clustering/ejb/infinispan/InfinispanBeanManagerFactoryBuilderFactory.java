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
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfiguration;
import org.infinispan.configuration.cache.ExpirationConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.jboss.as.clustering.controller.BuilderAdapter;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderFactory;
import org.wildfly.clustering.ejb.infinispan.logging.InfinispanEjbLogger;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.concurrent.CachedThreadPoolExecutorServiceBuilder;
import org.wildfly.clustering.service.concurrent.RemoveOnCancelScheduledExecutorServiceBuilder;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 */
public class InfinispanBeanManagerFactoryBuilderFactory<I> implements BeanManagerFactoryBuilderFactory<I, TransactionBatch> {

    private static final ThreadFactory EXPIRATION_THREAD_FACTORY = createThreadFactory();
    private static final ThreadFactory EVICTION_THREAD_FACTORY = createThreadFactory();

    private static ThreadFactory createThreadFactory() {
        return AccessController.doPrivileged(new PrivilegedAction<ThreadFactory>() {
            @Override
            public ThreadFactory run() {
                return new JBossThreadFactory(new ThreadGroup(InfinispanBeanManager.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null);
            }
        });
    }

    static String getCacheName(ServiceName deploymentUnitServiceName) {
        if (Services.JBOSS_DEPLOYMENT_SUB_UNIT.isParentOf(deploymentUnitServiceName)) {
            return deploymentUnitServiceName.getParent().getSimpleName() + "/" + deploymentUnitServiceName.getSimpleName();
        }
        return deploymentUnitServiceName.getSimpleName();
    }

    private final CapabilityServiceSupport support;
    private final String name;
    private final BeanManagerFactoryBuilderConfiguration config;

    public InfinispanBeanManagerFactoryBuilderFactory(CapabilityServiceSupport support, String name, BeanManagerFactoryBuilderConfiguration config) {
        this.support = support;
        this.name = name;
        this.config = config;
    }

    @Override
    public Collection<CapabilityServiceBuilder<?>> getDeploymentBuilders(final ServiceName name) {
        String cacheName = getCacheName(name);
        String containerName = this.config.getContainerName();
        String templateCacheName = this.config.getCacheName();

        // Ensure eviction and expiration are disabled
        Consumer<ConfigurationBuilder> configurator = builder -> {
            // Ensure expiration is not enabled on cache
            ExpirationConfiguration expiration = builder.expiration().create();
            if ((expiration.lifespan() >= 0) || (expiration.maxIdle() >= 0)) {
                builder.expiration().lifespan(-1).maxIdle(-1);
                InfinispanEjbLogger.ROOT_LOGGER.expirationDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, templateCacheName));
            }
            // Ensure eviction is not enabled on cache
            EvictionConfiguration eviction = builder.eviction().create();
            if (eviction.strategy().isEnabled()) {
                builder.eviction().size(-1L).strategy(EvictionStrategy.MANUAL);
                InfinispanEjbLogger.ROOT_LOGGER.evictionDisabled(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, templateCacheName));
            }
        };

        List<CapabilityServiceBuilder<?>> builders = new ArrayList<>(4);
        builders.add(new TemplateConfigurationBuilder(ServiceName.parse(InfinispanCacheRequirement.CONFIGURATION.resolve(containerName, cacheName)), containerName, cacheName, templateCacheName, configurator));
        builders.add(new CacheBuilder<Object, Object>(ServiceName.parse(InfinispanCacheRequirement.CACHE.resolve(containerName, cacheName)), containerName, cacheName) {
            @Override
            public ServiceBuilder<Cache<Object, Object>> build(ServiceTarget target) {
                return super.build(target).addDependency(name.append("marshalling"));
            }
        });
        builders.add(new BuilderAdapter<>(new RemoveOnCancelScheduledExecutorServiceBuilder(name.append(this.name, "expiration"), EXPIRATION_THREAD_FACTORY)));
        builders.add(new BuilderAdapter<>(new CachedThreadPoolExecutorServiceBuilder(name.append(this.name, "eviction"), EVICTION_THREAD_FACTORY)));
        return builders;
    }

    @Override
    public <T> Builder<? extends BeanManagerFactory<I, T, TransactionBatch>> getBeanManagerFactoryBuilder(BeanContext context) {
        return new InfinispanBeanManagerFactoryBuilder<>(this.support, this.name, context, this.config);
    }
}
