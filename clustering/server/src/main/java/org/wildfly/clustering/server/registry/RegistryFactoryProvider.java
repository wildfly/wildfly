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
package org.wildfly.clustering.server.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.invoker.BatchCacheInvoker;
import org.jboss.as.clustering.infinispan.invoker.CacheInvoker;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.CacheServiceProvider;
import org.jboss.as.clustering.jgroups.subsystem.ChannelServiceProvider;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.clustering.naming.JndiNameFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.logging.Logger;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.registry.RegistryFactory;
import org.wildfly.clustering.server.group.CacheGroupProvider;
import org.wildfly.clustering.server.group.CacheNodeFactory;
import org.wildfly.clustering.server.group.CacheNodeFactoryProvider;

/**
 * Installs a {@link RegistryFactory} service per cache.
 * @author Paul Ferraro
 */
public class RegistryFactoryProvider implements CacheServiceProvider {
    private static final Logger logger = Logger.getLogger(ChannelServiceProvider.class);

    public static ServiceName getServiceName(String containerName, String cacheName) {
        return ServiceName.JBOSS.append("clustering", "registry", containerName, cacheName);
    }

    public static ServiceName getFactoryServiceName(String containerName, String cacheName) {
        return getServiceName(containerName, cacheName).append("factory");
    }

    public static ServiceName getEntryProviderServiceName(String containerName, String cacheName) {
        return getServiceName(containerName, cacheName).append("entry");
    }

    private static ContextNames.BindInfo createBinding(String containerName, String cacheName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "registry", containerName, cacheName).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String containerName, String cacheName, boolean defaultCache) {
        List<ServiceName> result = new ArrayList<>(4);
        result.add(getServiceName(containerName, cacheName));
        result.add(getFactoryServiceName(containerName, cacheName));
        result.add(createBinding(containerName, cacheName).getBinderServiceName());
        if (defaultCache && !cacheName.equals(CacheContainer.DEFAULT_CACHE_ALIAS)) {
            result.add(getServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String containerName, String cacheName, boolean defaultCache, ModuleIdentifier moduleId) {
        ServiceName name = getFactoryServiceName(containerName, cacheName);
        ContextNames.BindInfo bindInfo = createBinding(containerName, cacheName);

        logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        RegistryFactoryConfig<Object, Object> registryConfig = new RegistryFactoryConfig<>();
        RegistryFactoryService<Object, Object> registryService = new RegistryFactoryService<>(registryConfig);
        ServiceBuilder<?> factoryBuilder = AsynchronousService.addService(target, name, registryService)
                .addDependency(CacheNodeFactoryProvider.getServiceName(containerName, cacheName), CacheNodeFactory.class, registryConfig.getNodeFactoryInjector())
                .addDependency(CacheGroupProvider.getServiceName(containerName, cacheName), Group.class, registryConfig.getGroupInjector())
                .addDependency(CacheService.getServiceName(containerName, cacheName), Cache.class, registryConfig.getCacheInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        ServiceBuilder<?> builder = RegistryService.build(target, containerName, cacheName).setInitialMode(ServiceController.Mode.ON_DEMAND);

        // Bind just the factory to JNDI
        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, RegistryFactory.class, new ManagedReferenceInjector<RegistryFactory>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        boolean addDefaultCacheAlias = defaultCache && !cacheName.equals(CacheContainer.DEFAULT_CACHE_ALIAS);

        if (addDefaultCacheAlias) {
            ContextNames.BindInfo info = createBinding(containerName, CacheContainer.DEFAULT_CACHE_ALIAS);
            binderBuilder.addAliases(info.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(info.getBindName()));
            factoryBuilder.addAliases(getFactoryServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }

        List<ServiceController<?>> result = new ArrayList<>(4);
        result.add(builder.install());
        result.add(factoryBuilder.install());
        result.add(binderBuilder.install());

        if (addDefaultCacheAlias) {
            ServiceBuilder<?> defaultBuilder = RegistryService.build(target, containerName, CacheContainer.DEFAULT_CACHE_ALIAS).setInitialMode(ServiceController.Mode.ON_DEMAND);
            result.add(defaultBuilder.install());
        }

        return result;
    }

    static class RegistryFactoryConfig<K, V> implements RegistryFactoryConfiguration<K, V> {
        private final CacheInvoker invoker = new BatchCacheInvoker();
        private final InjectedValue<Group> group = new InjectedValue<>();
        private final InjectedValue<Cache<Node, Map.Entry<K, V>>> cache = new InjectedValue<>();
        private final InjectedValue<CacheNodeFactory> factory = new InjectedValue<>();

        @Override
        public CacheInvoker getCacheInvoker() {
            return this.invoker;
        }

        @Override
        public Group getGroup() {
            return this.group.getValue();
        }

        @Override
        public Cache<Node, Map.Entry<K, V>> getCache() {
            return this.cache.getValue();
        }

        @Override
        public NodeFactory<Address> getNodeFactory() {
            return this.factory.getValue();
        }

        Injector<Group> getGroupInjector() {
            return this.group;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Injector<Cache> getCacheInjector() {
            return (Injector) this.cache;
        }

        Injector<CacheNodeFactory> getNodeFactoryInjector() {
            return this.factory;
        }
    }
}
