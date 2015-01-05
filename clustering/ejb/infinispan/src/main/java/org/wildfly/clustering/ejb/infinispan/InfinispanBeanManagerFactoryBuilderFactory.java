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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import org.infinispan.Cache;
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
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.concurrent.CachedThreadPoolExecutorServiceBuilder;
import org.wildfly.clustering.service.concurrent.RemoveOnCancelScheduledExecutorServiceBuilder;
import org.wildfly.clustering.spi.CacheGroupServiceNameFactory;
import org.wildfly.security.manager.action.GetAccessControlContextAction;

/**
 * Builds an infinispan-based {@link BeanManagerFactory}.
 *
 * @author Paul Ferraro
 *
 * @param <G> the group identifier type
 * @param <I> the bean identifier type
 */
public class InfinispanBeanManagerFactoryBuilderFactory<G, I> implements BeanManagerFactoryBuilderFactory<G, I, TransactionBatch> {

    private static final ThreadFactory EXPIRATION_THREAD_FACTORY = new JBossThreadFactory(new ThreadGroup(BeanExpirationScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));
    private static final ThreadFactory EVICTION_THREAD_FACTORY = new JBossThreadFactory(new ThreadGroup(BeanEvictionScheduler.class.getSimpleName()), Boolean.FALSE, null, "%G - %t", null, null, AccessController.doPrivileged(GetAccessControlContextAction.getInstance()));

    static String getCacheName(ServiceName deploymentUnitServiceName) {
        if (Services.JBOSS_DEPLOYMENT_SUB_UNIT.isParentOf(deploymentUnitServiceName)) {
            return deploymentUnitServiceName.getParent().getSimpleName() + "/" + deploymentUnitServiceName.getSimpleName();
        }
        return deploymentUnitServiceName.getSimpleName();
    }

    private final String name;
    private final BeanManagerFactoryBuilderConfiguration config;

    public InfinispanBeanManagerFactoryBuilderFactory(String name, BeanManagerFactoryBuilderConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public Collection<Builder<?>> getDeploymentBuilders(final ServiceName name) {
        String cacheName = getCacheName(name);
        String containerName = this.config.getContainerName();
        String templateCacheName = this.config.getCacheName();
        if (templateCacheName == null) {
            templateCacheName = CacheGroupServiceNameFactory.DEFAULT_CACHE;
        }

        List<Builder<?>> builders = new ArrayList<>(4);
        builders.add(new TemplateConfigurationBuilder(containerName, cacheName, templateCacheName));
        builders.add(new CacheBuilder<Object, Object>(containerName, cacheName) {
            @Override
            public ServiceBuilder<Cache<Object, Object>> build(ServiceTarget target) {
                return super.build(target).addDependency(name.append("marshalling"));
            }
        });
        builders.add(new RemoveOnCancelScheduledExecutorServiceBuilder(name.append(this.name, "expiration"), EXPIRATION_THREAD_FACTORY));
        builders.add(new CachedThreadPoolExecutorServiceBuilder(name.append(this.name, "eviction"), EVICTION_THREAD_FACTORY));
        return builders;
    }

    @Override
    public <T> Builder<? extends BeanManagerFactory<G, I, T, TransactionBatch>> getBeanManagerFactoryBuilder(BeanContext context) {
        return new InfinispanBeanManagerFactoryBuilder<>(this.name, context, this.config);
    }
}
