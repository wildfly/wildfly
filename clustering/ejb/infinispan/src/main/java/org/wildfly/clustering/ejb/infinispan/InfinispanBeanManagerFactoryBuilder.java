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

import org.jboss.as.server.deployment.Services;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.threads.JBossThreadFactory;
import org.wildfly.clustering.ee.infinispan.TransactionBatch;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilder;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.infinispan.spi.service.CacheServiceNameFactory;
import org.wildfly.clustering.infinispan.spi.service.TemplateConfigurationBuilder;
import org.wildfly.clustering.service.concurrent.CachedThreadPoolExecutorServiceBuilder;
import org.wildfly.clustering.service.concurrent.RemoveOnCancelScheduledExecutorServiceBuilder;
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

    static String getCacheName(ServiceName deploymentUnitServiceName) {
        if (Services.JBOSS_DEPLOYMENT_SUB_UNIT.isParentOf(deploymentUnitServiceName)) {
            return deploymentUnitServiceName.getParent().getSimpleName() + "/" + deploymentUnitServiceName.getSimpleName();
        }
        return deploymentUnitServiceName.getSimpleName();
    }

    private final String name;
    private final BeanManagerFactoryBuilderConfiguration config;

    public InfinispanBeanManagerFactoryBuilder(String name, BeanManagerFactoryBuilderConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public void installDeploymentUnitDependencies(ServiceTarget target, ServiceName deploymentUnitServiceName) {
        String containerName = this.config.getContainerName();
        String cacheName = getCacheName(deploymentUnitServiceName);
        String templateCacheName = this.config.getCacheName();
        if (templateCacheName == null) {
            templateCacheName = CacheServiceNameFactory.DEFAULT_CACHE;
        }

        new TemplateConfigurationBuilder(containerName, cacheName, templateCacheName).build(target).install();

        new CacheBuilder<>(this.config.getContainerName(), cacheName).build(target)
                .addDependency(deploymentUnitServiceName.append("marshalling"))
                .install()
        ;
        new RemoveOnCancelScheduledExecutorServiceBuilder(deploymentUnitServiceName.append(this.name, "expiration"), EXPIRATION_THREAD_FACTORY).build(target).install();
        new CachedThreadPoolExecutorServiceBuilder(deploymentUnitServiceName.append(this.name, "eviction"), EVICTION_THREAD_FACTORY).build(target).install();
    }

    @Override
    public <T> ServiceBuilder<? extends BeanManagerFactory<G, I, T, TransactionBatch>> build(ServiceTarget target, ServiceName name, BeanContext context) {
        return InfinispanBeanManagerFactory.build(this.name, target, name, this.config, context);
    }
}
