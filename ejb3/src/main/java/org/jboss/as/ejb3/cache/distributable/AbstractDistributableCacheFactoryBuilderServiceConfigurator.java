/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import java.util.function.Consumer;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.cache.CacheFactoryBuilder;
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
import org.wildfly.clustering.ejb.BeanManagementProvider;
import org.wildfly.clustering.ejb.StatefulBeanConfiguration;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Service that returns a distributable {@link org.jboss.as.ejb3.cache.CacheFactoryBuilder} using a beam management provider
 * from the distributable-ejb subsystem.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 * @param <K> the cache key type
 * @param <V> the cache value type
 */
public abstract class AbstractDistributableCacheFactoryBuilderServiceConfigurator<K, V extends Identifiable<K> & Contextual<Batch>> extends DistributableCacheFactoryBuilderServiceNameProvider implements ResourceServiceConfigurator, CacheFactoryBuilder<K, V>, Consumer<SupplierDependency<BeanManagementProvider>> {

    private volatile SupplierDependency<BeanManagementProvider> provider;

    public AbstractDistributableCacheFactoryBuilderServiceConfigurator(PathAddress address) {
        super(address.getLastElement().getValue());
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<CacheFactoryBuilder<K, V>> cacheFactoryBuilder = this.provider.register(builder).provides(name);
        Service service = Service.newInstance(cacheFactoryBuilder, this);
        return builder.setInstance(service);
    }

    @Override
    public void accept(SupplierDependency<BeanManagementProvider> provider) {
        this.provider = provider;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(DeploymentUnit unit) {
        return this.provider.get().getDeploymentServiceConfigurators(unit.getServiceName());
    }

    @Override
    public CapabilityServiceConfigurator getServiceConfigurator(DeploymentUnit unit, StatefulComponentDescription description, ComponentConfiguration configuration) {
        StatefulBeanConfiguration statefulBeanConfiguration = new StatefulBeanConfiguration() {
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
                return (info != null && info.getValue() >= 0) ? Duration.of(info.getValue(), info.getTimeUnit().toChronoUnit()) : null;
            }
        };
        CapabilityServiceConfigurator configurator = this.provider.get().getBeanManagerFactoryServiceConfigurator(statefulBeanConfiguration);
        // name vs description.getCacheFactoryServiceName()
        return new DistributableCacheFactoryServiceConfigurator<K, V>(description.getCacheFactoryServiceName(), configurator);
    }

    @Override
    public boolean supportsPassivation() {
        return true;
    }
}
