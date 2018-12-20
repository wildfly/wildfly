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

import java.time.Duration;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.cache.CacheFactoryBuilderService;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.BeanContext;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorFactoryProvider;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.SimpleServiceNameProvider;

/**
 * Service that returns a distributable {@link org.jboss.as.ejb3.cache.CacheFactoryBuilder}.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryBuilderService<K, V extends Identifiable<K> & Contextual<Batch>> extends SimpleServiceNameProvider implements ServiceConfigurator, DistributableCacheFactoryBuilder<K, V> {

    public static ServiceName getServiceName(String name) {
        return CacheFactoryBuilderService.BASE_CACHE_FACTORY_SERVICE_NAME.append("distributable", name);
    }

    private final BeanManagerFactoryServiceConfiguratorFactory builder;
    private final BeanManagerFactoryServiceConfiguratorConfiguration config;

    public DistributableCacheFactoryBuilderService(CapabilityServiceSupport support, String name, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        this(support, name, load(), config);
    }

    private static BeanManagerFactoryServiceConfiguratorFactoryProvider load() {
        for (BeanManagerFactoryServiceConfiguratorFactoryProvider provider: ServiceLoader.load(BeanManagerFactoryServiceConfiguratorFactoryProvider.class, BeanManagerFactoryServiceConfiguratorFactoryProvider.class.getClassLoader())) {
            return provider;
        }
        return null;
    }

    public DistributableCacheFactoryBuilderService(CapabilityServiceSupport support, String name, BeanManagerFactoryServiceConfiguratorFactoryProvider provider, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        super(getServiceName(name));
        this.config = config;
        this.builder = provider.getBeanManagerFactoryBuilder(support, name, config);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<DistributableCacheFactoryBuilder<K, V>> cacheFactoryBuilder = builder.provides(this.getServiceName());
        Service service = Service.newInstance(cacheFactoryBuilder, this);
        return builder.setInstance(service);
    }

    @Override
    public BeanManagerFactoryServiceConfiguratorConfiguration getConfiguration() {
        return this.config;
    }

    @Override
    public void installDeploymentUnitDependencies(CapabilityServiceSupport support, ServiceTarget target, ServiceName deploymentUnitServiceName) {
        for (CapabilityServiceConfigurator configurator : this.builder.getDeploymentServiceConfigurators(deploymentUnitServiceName)) {
            configurator.configure(support).build(target).install();
        }
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target, ServiceName serviceName, StatefulComponentDescription description, ComponentConfiguration configuration) {
        BeanContext context = new BeanContext() {
            @Override
            public String getBeanName() {
                return configuration.getComponentName();
            }

            @Override
            public ServiceName getDeploymentUnitServiceName() {
                return description.getDeploymentUnitServiceName();
            }

            @Override
            public ModuleLoader getModuleLoader() {
                return configuration.getModuleLoader();
            }

            @Override
            public ClassLoader getClassLoader() {
                return configuration.getModuleClassLoader();
            }

            @Override
            public Duration getTimeout() {
                StatefulTimeoutInfo info = description.getStatefulTimeout();
                // TODO Once based on JDK9+, change to Duration.of(this.info.getValue(), this.info.getTimeUnit().toChronoUnit())
                return (info != null) ? Duration.ofMillis(TimeUnit.MILLISECONDS.convert(info.getValue(), info.getTimeUnit())) : null;
            }
        };
        ServiceConfigurator configurator = this.builder.getBeanManagerFactoryServiceConfigurator(context);
        return new DistributableCacheFactoryService<K, V>(serviceName, configurator).build(target);
    }

    @Override
    public boolean supportsPassivation() {
        return true;
    }
}
