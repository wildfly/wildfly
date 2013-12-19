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
package org.wildfly.clustering.server.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.infinispan.Cache;
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.provider.ServiceProviderRegistrationFactory;
import org.wildfly.clustering.server.dispatcher.CommandDispatcherFactoryProvider;
import org.wildfly.clustering.server.group.CacheGroupProvider;

/**
 * Installs a ServiceProviderRegistry service per cache.
 * @author Paul Ferraro
 */
public class ServiceProviderRegistrationFactoryProvider implements CacheServiceProvider {
    private static final Logger logger = Logger.getLogger(ChannelServiceProvider.class);

    public static ServiceName getServiceName(String containerName, String cacheName) {
        return ServiceName.JBOSS.append("clustering", "providers", containerName, cacheName);
    }

    private static ContextNames.BindInfo createBinding(String containerName, String cacheName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "providers", containerName, cacheName).getAbsoluteName());
    }

    @Override
    public Collection<ServiceName> getServiceNames(String containerName, String cacheName, boolean defaultCache) {
        return Arrays.asList(getServiceName(containerName, cacheName), createBinding(containerName, cacheName).getBinderServiceName());
    }

    @Override
    public Collection<ServiceController<?>> install(ServiceTarget target, String containerName, String cacheName, boolean defaultCache, ModuleIdentifier moduleId) {
        ServiceName name = getServiceName(containerName, cacheName);
        ContextNames.BindInfo bindInfo = createBinding(containerName, cacheName);

        logger.debugf("Installing %s service, bound to ", name.getCanonicalName(), bindInfo.getAbsoluteJndiName());

        ServiceProviderRegistrationFactoryConfig config = new ServiceProviderRegistrationFactoryConfig(name);
        Service<ServiceProviderRegistrationFactory> service = new ServiceProviderRegistrationFactoryService(config);
        ServiceBuilder<?> builder = AsynchronousService.addService(target, name, service)
                .addDependency(CacheService.getServiceName(containerName, cacheName), Cache.class, config.getCacheInjector())
                .addDependency(CacheGroupProvider.getServiceName(containerName, cacheName), Group.class, config.getGroupInjector())
                .addDependency(CommandDispatcherFactoryProvider.getServiceName(containerName), CommandDispatcherFactory.class, config.getCommandDispatcherFactoryInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, ServiceProviderRegistrationFactory.class, new ManagedReferenceInjector<ServiceProviderRegistrationFactory>(binder.getManagedObjectInjector()))
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binder.getNamingStoreInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
        ;

        if (defaultCache) {
            ContextNames.BindInfo info = createBinding(containerName, CacheContainer.DEFAULT_CACHE_ALIAS);
            binderBuilder.addAliases(info.getBinderServiceName(), ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(info.getBindName()));
            builder.addAliases(getServiceName(containerName, CacheContainer.DEFAULT_CACHE_ALIAS));
        }

        return Arrays.asList(builder.install(), binderBuilder.install());
    }

    private static class ServiceProviderRegistrationFactoryConfig implements ServiceProviderRegistrationFactoryConfiguration {
        private final InjectedValue<Group> group = new InjectedValue<>();
        private final InjectedValue<Cache<Object, Set<Node>>> cache = new InjectedValue<>();
        private final InjectedValue<CommandDispatcherFactory> factory = new InjectedValue<>();
        private final Object id;
        private final CacheInvoker invoker = new BatchCacheInvoker();

        ServiceProviderRegistrationFactoryConfig(Object id) {
            this.id = id;
        }

        @Override
        public Object getId() {
            return this.id;
        }

        @Override
        public Group getGroup() {
            return this.group.getValue();
        }

        @Override
        public Cache<Object, Set<Node>> getCache() {
            return this.cache.getValue();
        }

        @Override
        public CommandDispatcherFactory getCommandDispatcherFactory() {
            return this.factory.getValue();
        }

        @Override
        public CacheInvoker getCacheInvoker() {
            return this.invoker;
        }

        Injector<Group> getGroupInjector() {
            return this.group;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        Injector<Cache> getCacheInjector() {
            return (Injector) this.cache;
        }

        Injector<CommandDispatcherFactory> getCommandDispatcherFactoryInjector() {
            return this.factory;
        }
    }
}
