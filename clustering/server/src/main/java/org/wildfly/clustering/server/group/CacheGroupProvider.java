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
package org.wildfly.clustering.server.group;

import java.util.Arrays;
import java.util.Collection;

import org.infinispan.Cache;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.subsystem.CacheService;
import org.jboss.as.clustering.infinispan.subsystem.CacheServiceProvider;
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
import org.wildfly.clustering.group.Group;

/**
 * Installs a {@link Group} service per cache.
 * @author Paul Ferraro
 */
public class CacheGroupProvider implements CacheServiceProvider {
    private static final Logger logger = Logger.getLogger(CacheServiceProvider.class);

    public static ServiceName getServiceName(String containerName, String cacheName) {
        return ServiceName.JBOSS.append("clustering", "group", containerName, cacheName);
    }

    private static ContextNames.BindInfo createBinding(String containerName, String cacheName) {
        return ContextNames.bindInfoFor(JndiNameFactory.createJndiName(JndiNameFactory.DEFAULT_JNDI_NAMESPACE, "clustering", "group", containerName, cacheName).getAbsoluteName());
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

        CacheGroupConfig config = new CacheGroupConfig();
        Service<Group> service = new CacheGroupService(config);
        ServiceBuilder<?> builder = AsynchronousService.addService(target, name, service)
                .addDependency(CacheService.getServiceName(containerName, cacheName), Cache.class, config.getCacheInjector())
                .addDependency(CacheNodeFactoryProvider.getServiceName(containerName, cacheName), CacheNodeFactory.class, config.getNodeFactoryInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;

        BinderService binder = new BinderService(bindInfo.getBindName());
        ServiceBuilder<?> binderBuilder = target.addService(bindInfo.getBinderServiceName(), binder)
                .addAliases(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(bindInfo.getBindName()))
                .addDependency(name, Group.class, new ManagedReferenceInjector<Group>(binder.getManagedObjectInjector()))
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

    @SuppressWarnings("rawtypes")
    static class CacheGroupConfig implements CacheGroupConfiguration {
        private final InjectedValue<Cache> cache = new InjectedValue<>();
        private final InjectedValue<CacheNodeFactory> factory = new InjectedValue<>();

        @Override
        public Cache<?, ?> getCache() {
            return this.cache.getValue();
        }

        @Override
        public CacheNodeFactory getNodeFactory() {
            return this.factory.getValue();
        }

        Injector<Cache> getCacheInjector() {
            return this.cache;
        }

        Injector<CacheNodeFactory> getNodeFactoryInjector() {
            return this.factory;
        }
    }
}
