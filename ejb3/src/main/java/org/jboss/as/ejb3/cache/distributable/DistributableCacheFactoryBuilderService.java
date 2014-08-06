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
package org.jboss.as.ejb3.cache.distributable;

import java.util.ServiceLoader;
import java.util.UUID;

import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ejb.Batch;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilder;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryBuilderProvider;

/**
 * Service that returns a distributable {@link org.jboss.as.ejb3.cache.CacheFactoryBuilder}.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryBuilderService<K, V extends Identifiable<K> & Contextual<Batch>> extends AbstractService<DistributableCacheFactoryBuilder<K, V>> implements DistributableCacheFactoryBuilder<K, V> {

    public static ServiceName getServiceName(String name) {
        return CacheFactoryBuilderService.BASE_CACHE_FACTORY_SERVICE_NAME.append("distributable", name);
    }

    private final String name;
    private final BeanManagerFactoryBuilder<UUID, K, Batch> builder;
    private final BeanManagerFactoryBuilderConfiguration config;

    public DistributableCacheFactoryBuilderService(String name, BeanManagerFactoryBuilderConfiguration config) {
        this(name, load(), config);
    }

    private static BeanManagerFactoryBuilderProvider<Batch> load() {
        for (BeanManagerFactoryBuilderProvider<Batch> provider: ServiceLoader.load(BeanManagerFactoryBuilderProvider.class, BeanManagerFactoryBuilderProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    public DistributableCacheFactoryBuilderService(String name, BeanManagerFactoryBuilderProvider<Batch> provider, BeanManagerFactoryBuilderConfiguration config) {
        this.name = name;
        this.config = config;
        this.builder = provider.<UUID, K>getBeanManagerFactoryBuilder(name, config);
    }

    public ServiceBuilder<DistributableCacheFactoryBuilder<K, V>> build(ServiceTarget target) {
        return target.addService(getServiceName(this.name), this);
    }

    @Override
    public DistributableCacheFactoryBuilder<K, V> getValue() {
        return this;
    }

    @Override
    public BeanManagerFactoryBuilderConfiguration getConfiguration() {
        return this.config;
    }

    @Override
    public void installDeploymentUnitDependencies(ServiceTarget target, ServiceName deploymentUnitServiceName) {
        this.builder.installDeploymentUnitDependencies(target, deploymentUnitServiceName);
    }

    @Override
    public ServiceBuilder<? extends CacheFactory<K, V>> build(ServiceTarget target, ServiceName serviceName, BeanContext context, StatefulTimeoutInfo statefulTimeout) {
        ServiceName factoryServiceName = serviceName.append(this.name, "bean-manager");
        this.builder.build(target, factoryServiceName, context)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
        return DistributableCacheFactoryService.build(target, serviceName, factoryServiceName);
    }

    @Override
    public boolean supportsPassivation() {
        return true;
    }
}
