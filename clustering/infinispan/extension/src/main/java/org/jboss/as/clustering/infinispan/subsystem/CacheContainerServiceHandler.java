/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.DEFAULT_CAPABILITIES;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.DEFAULT_CLUSTERING_CAPABILITIES;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute.MODULES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.ServiceLoader;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ServiceValueCaptorServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.ServiceValueRegistry;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.service.ServiceNameRegistry;
import org.wildfly.clustering.spi.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;

/**
 * @author Paul Ferraro
 */
public class CacheContainerServiceHandler implements ResourceServiceHandler {

    private final ServiceValueRegistry<EmbeddedCacheManager> containerRegistry;
    private final ServiceValueRegistry<Cache<?, ?>> cacheRegistry;

    public CacheContainerServiceHandler(ServiceValueRegistry<EmbeddedCacheManager> containerRegistry, ServiceValueRegistry<Cache<?, ?>> cacheRegistry) {
        this.containerRegistry = containerRegistry;
        this.cacheRegistry = cacheRegistry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        ServiceTarget target = context.getServiceTarget();

        new ModulesServiceConfigurator(CacheContainerComponent.MODULES.getServiceName(address), MODULES, Collections.singletonList(Module.forClass(CacheContainer.class))).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        GlobalConfigurationServiceConfigurator configBuilder = new GlobalConfigurationServiceConfigurator(address);
        configBuilder.configure(context, model).build(target).install();

        CacheContainerServiceConfigurator containerBuilder = new CacheContainerServiceConfigurator(address, this.cacheRegistry).configure(context, model);
        containerBuilder.build(target).install();

        new ServiceValueCaptorServiceConfigurator<>(this.containerRegistry.add(containerBuilder.getServiceName())).build(target).install();

        new KeyAffinityServiceFactoryServiceConfigurator(address).build(target).install();

        new BinderServiceConfigurator(InfinispanBindingFactory.createCacheContainerBinding(name), containerBuilder.getServiceName()).build(target).install();

        String defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asString(null);
        if (defaultCache != null) {
            for (Map.Entry<InfinispanCacheRequirement, Capability> entry : DEFAULT_CAPABILITIES.entrySet()) {
                new IdentityServiceConfigurator<>(entry.getValue().getServiceName(address), entry.getKey().getServiceName(context, name, defaultCache)).build(target).install();
            }

            if (!defaultCache.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                new BinderServiceConfigurator(InfinispanBindingFactory.createCacheBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME), DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CACHE).getServiceName(address)).build(target).install();
                new BinderServiceConfigurator(InfinispanBindingFactory.createCacheConfigurationBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME), DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CONFIGURATION).getServiceName(address)).build(target).install();
            }

            ServiceNameRegistry<ClusteringCacheRequirement> registry = new CapabilityServiceNameRegistry<>(DEFAULT_CLUSTERING_CAPABILITIES, address);

            for (IdentityCacheServiceConfiguratorProvider provider : ServiceLoader.load(IdentityCacheServiceConfiguratorProvider.class, IdentityCacheServiceConfiguratorProvider.class.getClassLoader())) {
                for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(registry, name, null, defaultCache)) {
                    configurator.configure(context).build(target).install();
                }
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        String defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asString(null);
        if (defaultCache != null) {
            ServiceNameRegistry<ClusteringCacheRequirement> registry = new CapabilityServiceNameRegistry<>(DEFAULT_CLUSTERING_CAPABILITIES, address);

            for (IdentityCacheServiceConfiguratorProvider provider : ServiceLoader.load(IdentityCacheServiceConfiguratorProvider.class, IdentityCacheServiceConfiguratorProvider.class.getClassLoader())) {
                for (ServiceNameProvider configurator : provider.getServiceConfigurators(registry, name, null, defaultCache)) {
                    context.removeService(configurator.getServiceName());
                }
            }

            if (!defaultCache.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                context.removeService(InfinispanBindingFactory.createCacheBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME).getBinderServiceName());
                context.removeService(InfinispanBindingFactory.createCacheConfigurationBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME).getBinderServiceName());
            }

            for (Capability capability : DEFAULT_CAPABILITIES.values()) {
                context.removeService(capability.getServiceName(address));
            }
        }

        context.removeService(InfinispanBindingFactory.createCacheContainerBinding(name).getBinderServiceName());

        context.removeService(CacheContainerComponent.MODULES.getServiceName(address));

        for (Capability capability : EnumSet.allOf(CacheContainerResourceDefinition.Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }

        context.removeService(new ServiceValueCaptorServiceConfigurator<>(this.containerRegistry.remove(CacheContainerResourceDefinition.Capability.CONTAINER.getServiceName(address))).getServiceName());
    }
}
