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
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.*;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.clustering.controller.ModuleBuilder;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.CacheContainer;
import org.wildfly.clustering.infinispan.spi.InfinispanCacheRequirement;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupAliasBuilderProvider;

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
            Resource rootResource = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS);
            PathElement ejbPath = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, "ejb3");
            if (rootResource.hasChild(ejbPath) && rootResource.getChild(ejbPath).hasChild(PathElement.pathElement("service", "remote"))) {
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
        CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        new ModuleBuilder(CacheContainerComponent.MODULE.getServiceName(address), MODULE).configure(context, model).build(target).install();
        new GlobalConfigurationBuilder(address).configure(context, model).build(target).install();

        CacheContainerBuilder containerBuilder = new CacheContainerBuilder(address).configure(context, model);
        containerBuilder.build(target).install();

        new KeyAffinityServiceFactoryBuilder(address).build(target).install();

        BinderServiceBuilder<?> bindingBuilder = new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheContainerBinding(name), containerBuilder.getServiceName(), CacheContainer.class);
        ModelNodes.optionalString(JNDI_NAME.resolveModelAttribute(context, model)).map(jndiName -> ContextNames.bindInfoFor(JndiNameFactory.parse(jndiName).getAbsoluteName())).ifPresent(aliasBinding -> bindingBuilder.alias(aliasBinding));
        bindingBuilder.build(target).install();

        String defaultCache = containerBuilder.getDefaultCache();
        if (defaultCache != null) {
            DEFAULT_CAPABILITIES.entrySet().forEach(entry -> new AliasServiceBuilder<>(entry.getValue().getServiceName(address), entry.getKey().getServiceName(context, name, defaultCache), entry.getKey().getType()).build(target).install());

            if (!defaultCache.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME), DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CACHE).getServiceName(address), Cache.class).build(target).install();
                new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheConfigurationBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME), DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CONFIGURATION).getServiceName(address), Configuration.class).build(target).install();

                for (CacheGroupAliasBuilderProvider provider : ServiceLoader.load(CacheGroupAliasBuilderProvider.class, CacheGroupAliasBuilderProvider.class.getClassLoader())) {
                    for (Builder<?> builder : provider.getBuilders(support, name, null, defaultCache)) {
                        builder.build(target).install();
                    }
                }
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        ModelNodes.optionalString(DEFAULT_CACHE.resolveModelAttribute(context, model)).ifPresent(defaultCache -> {
            if (!defaultCache.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                for (CacheGroupAliasBuilderProvider provider : ServiceLoader.load(CacheGroupAliasBuilderProvider.class, CacheGroupAliasBuilderProvider.class.getClassLoader())) {
                    for (Builder<?> builder : provider.getBuilders(support, name, null, defaultCache)) {
                        context.removeService(builder.getServiceName());
                    }
                }

                context.removeService(InfinispanBindingFactory.createCacheBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME).getBinderServiceName());
                context.removeService(InfinispanBindingFactory.createCacheConfigurationBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME).getBinderServiceName());
            }

            DEFAULT_CAPABILITIES.values().forEach(capability -> context.removeService(capability.getServiceName(address)));
        });

        context.removeService(InfinispanBindingFactory.createCacheContainerBinding(name).getBinderServiceName());

        EnumSet.allOf(CacheContainerResourceDefinition.Capability.class).stream().map(component -> component.getServiceName(address)).forEach(serviceName -> context.removeService(serviceName));
    }
}
