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

import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.cache.Contextual;
import org.jboss.as.ejb3.cache.Identifiable;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.ejb3.component.stateful.StatefulTimeoutInfo;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.ee.Batch;
import org.wildfly.clustering.ejb.StatefulBeanConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorConfiguration;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorFactory;
import org.wildfly.clustering.ejb.BeanManagerFactoryServiceConfiguratorFactoryProvider;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Service that returns a distributable {@link org.jboss.as.ejb3.cache.CacheFactoryBuilder}.
 * @author Paul Ferraro
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public class DistributableCacheFactoryBuilderServiceConfigurator<K, V extends Identifiable<K> & Contextual<Batch>> extends DistributableCacheFactoryBuilderServiceNameProvider implements ServiceConfigurator, DistributableCacheFactoryBuilder<K, V> {

    private final BeanManagerFactoryServiceConfiguratorFactory factory;
    private final BeanManagerFactoryServiceConfiguratorConfiguration config;

    public DistributableCacheFactoryBuilderServiceConfigurator(String name, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        this(name, load(), config);
    }

    private static BeanManagerFactoryServiceConfiguratorFactoryProvider load() {
        Iterator<BeanManagerFactoryServiceConfiguratorFactoryProvider> providers = load(BeanManagerFactoryServiceConfiguratorFactoryProvider.class).iterator();
        if (!providers.hasNext()) {
            throw new ServiceConfigurationError(BeanManagerFactoryServiceConfiguratorFactoryProvider.class.getName());
        }
        return providers.next();
    }

    private static <T> Iterable<T> load(Class<T> providerClass) {
        PrivilegedAction<Iterable<T>> action = new PrivilegedAction<Iterable<T>>() {
            @Override
            public Iterable<T> run() {
                return ServiceLoader.load(providerClass, providerClass.getClassLoader());
            }
        };
        return WildFlySecurityManager.doUnchecked(action);
    }

    public DistributableCacheFactoryBuilderServiceConfigurator(String name, BeanManagerFactoryServiceConfiguratorFactoryProvider provider, BeanManagerFactoryServiceConfiguratorConfiguration config) {
        super(name);
        this.config = config;
        this.factory = provider.getBeanManagerFactoryBuilder(name, config);
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<DistributableCacheFactoryBuilder<K, V>> cacheFactoryBuilder = builder.provides(name);
        Service service = Service.newInstance(cacheFactoryBuilder, this);
        return builder.setInstance(service);
    }

    @Override
    public BeanManagerFactoryServiceConfiguratorConfiguration getConfiguration() {
        return this.config;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit) {
        return this.factory.getDeploymentServiceConfigurators(unit.getServiceName());
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(DeploymentUnit unit, StatefulComponentDescription description, ComponentConfiguration configuration) {
        StatefulBeanConfiguration context = new StatefulBeanConfiguration() {
            @Override
            public String getName() {
                return configuration.getComponentName();
            }

            @Override
            public ServiceName getDeploymentUnitServiceName() {
                return unit.getServiceName();
            }

            @Override
            public Module getModule() {
                return unit.getAttachment(Attachments.MODULE);
            }

            @Override
            public Duration getTimeout() {
                StatefulTimeoutInfo info = description.getStatefulTimeout();

                // A value of -1 means the bean will never be removed due to timeout
                if (info == null || info.getValue() < 0) {
                    return null;
                }
                // TODO Once based on JDK9+, change to Duration.of(this.info.getValue(), this.info.getTimeUnit().toChronoUnit())
                return Duration.ofMillis(TimeUnit.MILLISECONDS.convert(info.getValue(), info.getTimeUnit()));
            }
        };
        CapabilityServiceConfigurator configurator = this.factory.getBeanManagerFactoryServiceConfigurator(context);
        return new DistributableCacheFactoryServiceConfigurator<K, V>(description.getCacheFactoryServiceName(), configurator);
    }

    @Override
    public boolean supportsPassivation() {
        return true;
    }
}
