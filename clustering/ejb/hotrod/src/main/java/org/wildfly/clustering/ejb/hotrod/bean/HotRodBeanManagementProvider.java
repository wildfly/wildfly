/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.ejb.hotrod.bean;

import java.util.List;
import java.util.function.Consumer;

import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.configuration.NearCacheMode;
import org.infinispan.client.hotrod.configuration.RemoteCacheConfigurationBuilder;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.FunctionalCapabilityServiceConfigurator;
import org.jboss.as.controller.ServiceNameFactory;
import org.wildfly.clustering.ejb.bean.BeanConfiguration;
import org.wildfly.clustering.ejb.bean.BeanDeploymentConfiguration;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.infinispan.client.service.InfinispanClientRequirement;
import org.wildfly.clustering.infinispan.client.service.RemoteCacheServiceConfigurator;
import org.wildfly.clustering.marshalling.spi.ByteBufferMarshaller;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.common.function.Functions;

/**
 * Provides service installation mechanics for components of a bean deployment.
 * @author Paul Ferraro
 */
public class HotRodBeanManagementProvider implements BeanManagementProvider {

    private final String name;
    private final HotRodBeanManagementConfiguration config;

    public HotRodBeanManagementProvider(String name, HotRodBeanManagementConfiguration config) {
        this.name = name;
        this.config = config;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Iterable<CapabilityServiceConfigurator> getDeploymentServiceConfigurators(BeanDeploymentConfiguration configuration) {
        Integer maxActiveBeans = this.config.getMaxActiveBeans();
        String containerName = this.config.getContainerName();
        String configurationName = this.config.getConfigurationName();
        String cacheName = configuration.getDeploymentName();
        String templateName = (configurationName != null) ? configurationName : DefaultTemplate.DIST_SYNC.getTemplateName();

        CapabilityServiceConfigurator marshallerConfigurator = new FunctionalCapabilityServiceConfigurator<>(configuration.getDeploymentServiceName().append("marshaller"), this.config.getMarshallerFactory(), Functions.constantSupplier(configuration));
        SupplierDependency<ByteBufferMarshaller> marshaller = new ServiceSupplierDependency<>(marshallerConfigurator);
        CapabilityServiceConfigurator cacheConfigurator = new RemoteCacheServiceConfigurator<>(ServiceNameFactory.parseServiceName(InfinispanClientRequirement.REMOTE_CONTAINER.getName()).append(containerName, cacheName), containerName, cacheName, new Consumer<RemoteCacheConfigurationBuilder>() {
            @Override
            public void accept(RemoteCacheConfigurationBuilder builder) {
                builder.forceReturnValues(false).nearCacheMode(NearCacheMode.INVALIDATED).templateName(templateName).nearCacheFactory(new BeanManagerNearCacheFactory<>(maxActiveBeans, marshaller));
            }
        });
        CapabilityServiceConfigurator groupManagerConfigurator = new HotRodBeanGroupManagerServiceConfigurator<>(configuration, new ServiceSupplierDependency<>(cacheConfigurator), marshaller);
        return List.of(marshallerConfigurator, cacheConfigurator, groupManagerConfigurator);
    }

    @Override
    public CapabilityServiceConfigurator getBeanManagerFactoryServiceConfigurator(BeanConfiguration config) {
        return new HotRodBeanManagerFactoryServiceConfigurator<>(config, this.config);
    }
}
