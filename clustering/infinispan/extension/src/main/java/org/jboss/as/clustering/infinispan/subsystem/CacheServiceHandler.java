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

import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability.CONFIGURATION;
import static org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.ListAttribute.MODULES;
import static org.jboss.as.clustering.infinispan.subsystem.TransactionResourceDefinition.TransactionRequirement.XA_RESOURCE_RECOVERY_REGISTRY;

import java.util.Collections;
import java.util.EnumSet;

import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.infinispan.subsystem.CacheResourceDefinition.Capability;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.service.CacheServiceConfigurator;
import org.wildfly.clustering.server.service.CacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.ProvidedCacheServiceConfigurator;
import org.wildfly.clustering.service.IdentityServiceConfigurator;

/**
 * @author Paul Ferraro
 */
public class CacheServiceHandler<P extends CacheServiceConfiguratorProvider> implements ResourceServiceHandler {

    private final ResourceServiceConfiguratorFactory configuratorFactory;
    private final Class<P> providerClass;

    CacheServiceHandler(ResourceServiceConfiguratorFactory configuratorFactory, Class<P> providerClass) {
        this.configuratorFactory = configuratorFactory;
        this.providerClass = providerClass;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress cacheAddress = context.getCurrentAddress();
        PathAddress containerAddress = cacheAddress.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        ServiceName moduleServiceName = CacheComponent.MODULES.getServiceName(cacheAddress);
        if (model.hasDefined(MODULES.getName())) {
            new ModulesServiceConfigurator(moduleServiceName, MODULES, Collections.emptyList()).configure(context, model).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        } else {
            new IdentityServiceConfigurator<>(moduleServiceName, CacheContainerComponent.MODULES.getServiceName(containerAddress)).build(target).install();
        }

        this.configuratorFactory.createServiceConfigurator(cacheAddress).configure(context, model).build(target).install();

        ServiceName cacheServiceName = CACHE.getServiceName(cacheAddress);
        new CacheServiceConfigurator<>(cacheServiceName, containerName, cacheName).configure(context).build(target).install();
        if (context.hasOptionalCapability(XA_RESOURCE_RECOVERY_REGISTRY.getName(), null, null)) {
            new XAResourceRecoveryServiceConfigurator(cacheAddress).configure(context).build(target).install();
        }

        ServiceName lazyCacheServiceName = cacheServiceName.append("lazy");
        new LazyCacheServiceConfigurator(lazyCacheServiceName, containerName, cacheName).configure(context).build(target).install();

        new BinderServiceConfigurator(InfinispanBindingFactory.createCacheConfigurationBinding(containerName, cacheName), CONFIGURATION.getServiceName(cacheAddress)).build(target).install();
        new BinderServiceConfigurator(InfinispanBindingFactory.createCacheBinding(containerName, cacheName), lazyCacheServiceName).build(target).install();

        new ProvidedCacheServiceConfigurator<>(this.providerClass, containerName, cacheName).configure(context).build(target).install();
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) {
        PathAddress cacheAddress = context.getCurrentAddress();
        PathAddress containerAddress = cacheAddress.getParent();

        String containerName = containerAddress.getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        new ProvidedCacheServiceConfigurator<>(this.providerClass, containerName, cacheName).remove(context);

        context.removeService(InfinispanBindingFactory.createCacheBinding(containerName, cacheName).getBinderServiceName());
        context.removeService(InfinispanBindingFactory.createCacheConfigurationBinding(containerName, cacheName).getBinderServiceName());

        context.removeService(new XAResourceRecoveryServiceConfigurator(cacheAddress).getServiceName());
        context.removeService(CacheComponent.MODULES.getServiceName(cacheAddress));

        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(cacheAddress));
        }
    }
}
