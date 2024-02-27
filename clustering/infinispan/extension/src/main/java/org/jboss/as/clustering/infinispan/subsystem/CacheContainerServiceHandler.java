/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.DEFAULT_CAPABILITIES;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE;
import static org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute.MODULES;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.ModulesServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.infinispan.lifecycle.WildFlyInfinispanModuleLifecycle;
import org.wildfly.clustering.infinispan.service.InfinispanCacheRequirement;
import org.wildfly.clustering.server.service.ProvidedIdentityCacheServiceConfigurator;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * @author Paul Ferraro
 */
public class CacheContainerServiceHandler implements ResourceServiceHandler {

    private final ServiceValueRegistry<EmbeddedCacheManager> containerRegistry;
    private final ServiceValueRegistry<Cache<?, ?>> cacheRegistry;
    private final Map<PathAddress, Consumer<OperationContext>> removers = new ConcurrentHashMap<>();

    public CacheContainerServiceHandler(ServiceValueRegistry<EmbeddedCacheManager> containerRegistry, ServiceValueRegistry<Cache<?, ?>> cacheRegistry) {
        this.containerRegistry = containerRegistry;
        this.cacheRegistry = cacheRegistry;
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        ServiceTarget target = context.getServiceTarget();

        new ModulesServiceConfigurator(CacheContainerComponent.MODULES.getServiceName(address), MODULES, Collections.singletonList(Module.forClass(WildFlyInfinispanModuleLifecycle.class))).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        GlobalConfigurationServiceConfigurator configBuilder = new GlobalConfigurationServiceConfigurator(address);
        configBuilder.configure(context, model).build(target).install();

        ServiceName containerServiceName = CacheContainerResourceDefinition.Capability.CONTAINER.getDefinition().getCapabilityServiceName(address);
        this.removers.put(address, this.containerRegistry.capture(ServiceDependency.on(containerServiceName)).install(context));

        new CacheContainerServiceConfigurator(address, this.cacheRegistry).configure(context, model).build(target).install();

        new KeyAffinityServiceFactoryServiceConfigurator(address).build(target).install();

        new BinderServiceConfigurator(InfinispanBindingFactory.createCacheContainerBinding(name), containerServiceName).build(target).install();

        String defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asString(null);
        if (defaultCache != null) {
            for (Map.Entry<InfinispanCacheRequirement, Capability> entry : DEFAULT_CAPABILITIES.entrySet()) {
                new IdentityServiceConfigurator<>(entry.getValue().getServiceName(address), entry.getKey().getServiceName(context, name, defaultCache)).build(target).install();
            }

            if (!defaultCache.equals(JndiNameFactory.DEFAULT_LOCAL_NAME)) {
                ServiceName lazyCacheServiceName = DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CACHE).getServiceName(address).append("lazy");
                new LazyCacheServiceConfigurator(lazyCacheServiceName, name, defaultCache).configure(context).build(target).install();
                new BinderServiceConfigurator(InfinispanBindingFactory.createCacheBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME), lazyCacheServiceName).build(target).install();
                new BinderServiceConfigurator(InfinispanBindingFactory.createCacheConfigurationBinding(name, JndiNameFactory.DEFAULT_LOCAL_NAME), DEFAULT_CAPABILITIES.get(InfinispanCacheRequirement.CONFIGURATION).getServiceName(address)).build(target).install();
            }

            new ProvidedIdentityCacheServiceConfigurator(name, null, defaultCache).configure(context).build(target).install();
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        String defaultCache = DEFAULT_CACHE.resolveModelAttribute(context, model).asString(null);
        if (defaultCache != null) {
            new ProvidedIdentityCacheServiceConfigurator(name, null, defaultCache).remove(context);

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

        Consumer<OperationContext> remover = this.removers.remove(address);
        if (remover != null) {
            remover.accept(context);
        }
    }
}
