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

import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Attribute.*;
import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.*;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.ModuleBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.spi.service.CacheBuilder;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CacheGroupBuilderProvider;

/**
 * @author Paul Ferraro
 */
public class CacheServiceHandler implements ResourceServiceHandler {

    private final ResourceServiceBuilderFactory<Configuration> builderFactory;
    private final Class<? extends CacheGroupBuilderProvider> providerClass;

    CacheServiceHandler(ResourceServiceBuilderFactory<Configuration> builderFactory, Class<? extends CacheGroupBuilderProvider> providerClass) {
        this.builderFactory = builderFactory;
        this.providerClass = providerClass;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress();
        PathAddress containerAddress = cacheAddress.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();
        CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        ServiceName moduleServiceName = CacheComponent.MODULE.getServiceName(cacheAddress);
        if (model.hasDefined(MODULE.getName())) {
            new ModuleBuilder(moduleServiceName, MODULE).configure(context, model).build(target).install();
        } else {
            new AliasServiceBuilder<>(moduleServiceName, CacheContainerComponent.MODULE.getServiceName(containerAddress), Module.class).build(target).install();
        }

        this.builderFactory.createBuilder(cacheAddress).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        new CacheBuilder<>(CACHE.getServiceName(cacheAddress), containerName, cacheName).configure(context).build(target).install();
        new XAResourceRecoveryBuilder(cacheAddress).build(target).install();

        new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheConfigurationBinding(containerName, cacheName), CONFIGURATION.getServiceName(cacheAddress), Configuration.class).build(target).install();
        BinderServiceBuilder<?> bindingBuilder = new BinderServiceBuilder<>(InfinispanBindingFactory.createCacheBinding(containerName, cacheName), CACHE.getServiceName(cacheAddress), Cache.class);
        String jndiName = ModelNodes.asString(CacheResourceDefinition.Attribute.JNDI_NAME.resolveModelAttribute(context, model));
        if (jndiName != null) {
            bindingBuilder.alias(ContextNames.bindInfoFor(JndiNameFactory.parse(jndiName).getAbsoluteName()));
        }
        bindingBuilder.build(target).install();

        for (CacheGroupBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(support, containerName, cacheName)) {
                builder.build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress cacheAddress = context.getCurrentAddress();
        PathAddress containerAddress = cacheAddress.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();
        CapabilityServiceSupport support = context.getCapabilityServiceSupport();

        for (CacheGroupBuilderProvider provider : ServiceLoader.load(this.providerClass, this.providerClass.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(support, containerName, cacheName)) {
                context.removeService(builder.getServiceName());
            }
        }

        context.removeService(InfinispanBindingFactory.createCacheBinding(containerName, cacheName).getBinderServiceName());
        context.removeService(InfinispanBindingFactory.createCacheConfigurationBinding(containerName, cacheName).getBinderServiceName());

        context.removeService(new XAResourceRecoveryBuilder(cacheAddress).getServiceName());

        EnumSet.allOf(CacheResourceDefinition.Capability.class).stream().map(capability -> capability.getServiceName(cacheAddress)).forEach(serviceName -> context.removeService(serviceName));
    }
}
