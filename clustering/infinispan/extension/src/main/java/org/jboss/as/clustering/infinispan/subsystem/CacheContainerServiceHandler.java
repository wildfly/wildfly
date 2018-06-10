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
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.MODULE;

import java.util.EnumSet;
import java.util.Map;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ModuleServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringCacheRequirement;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheContainerServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        // Handle case where ejb subsystem has already installed services for this cache-container
        // This can happen if the ejb cache-container is added to a running server
        if (context.getProcessType().isServer() && !context.isBooting() && name.equals("ejb")) {
            PathElement ejbPath = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
            Resource ejbResource = safeGetResource(context, ejbPath);
            if (ejbResource != null && ejbResource.hasChild(PathElement.pathElement("service", "remote"))) {
                // Following restart, these services will be installed by this handler, rather than the ejb remote handler
                context.addStep(new OperationStepHandler() {
                    @Override
                    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                        context.reloadRequired();
                        context.completeStep(OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER);
                    }
                }, OperationContext.Stage.RUNTIME);
                return;
            }
        }

        ServiceTarget target = context.getServiceTarget();

        new ModuleServiceConfigurator(CacheContainerComponent.MODULE.getServiceName(address), MODULE).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        GlobalConfigurationServiceConfigurator configBuilder = new GlobalConfigurationServiceConfigurator(address);
        configBuilder.configure(context, model).build(target).install();

        CacheContainerServiceConfigurator containerBuilder = new CacheContainerServiceConfigurator(address).configure(context, model);
        containerBuilder.build(target).install();

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

        context.removeService(CacheContainerComponent.MODULE.getServiceName(address));

        for (Capability capability : EnumSet.allOf(CacheContainerResourceDefinition.Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }
    }

    private static Resource safeGetResource(OperationContext context, PathElement path) {
        try {
            return context.readResourceFromRoot(PathAddress.pathAddress(path), false);
        } catch (RuntimeException e) {
            // No such resource
            return null;
        }
    }
}
